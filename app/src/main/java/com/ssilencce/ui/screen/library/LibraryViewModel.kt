/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.screen.library

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.animation.doOnEnd
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.ssilencce.R
import com.ssilencce.coil.CustomPlaylistImageManager
import com.ssilencce.core.model.CategoryInfo
import com.ssilencce.core.model.LibraryMargin
import com.ssilencce.core.model.filesystem.FileSystemItem
import com.ssilencce.core.model.filesystem.FileSystemQuery
import com.ssilencce.data.SongProvider
import com.ssilencce.data.local.repository.PlaylistImportNameResolver
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.room.InclExclDao
import com.ssilencce.data.local.room.InclExclEntity
import com.ssilencce.data.local.room.PlaylistEntity
import com.ssilencce.data.local.room.PlaylistWithSongs
import com.ssilencce.data.local.room.SongEntity
import com.ssilencce.data.mapper.toSongEntity
import com.ssilencce.data.mapper.toSongs
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.ArtistPlayStat
import com.ssilencce.data.model.ContentType
import com.ssilencce.data.model.Folder
import com.ssilencce.data.model.Genre
import com.ssilencce.data.model.ListeningPeriod
import com.ssilencce.data.model.Playlist
import com.ssilencce.data.model.ReleaseYear
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.network.LoginParams
import com.ssilencce.data.model.network.ScrobblingService
import com.ssilencce.extensions.files.getCanonicalPathSafe
import com.ssilencce.extensions.media.indexOfSong
import com.ssilencce.ui.dialogs.playlists.AddToPlaylistUiState
import com.ssilencce.ui.screen.library.home.SuggestedResult
import com.ssilencce.util.Preferences
import com.ssilencce.util.StorageUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class LibraryViewModel(
    private val repository: Repository,
    private val inclExclDao: InclExclDao,
    private val customPlaylistImageManager: CustomPlaylistImageManager,
) : ViewModel() {
    init {
        viewModelScope.launch(IO) {
            initializeBlacklist()
            deleteMissingContent()
        }
    }

    private val suggestions = MutableLiveData(SuggestedResult.Idle)
    private val songs = MutableLiveData<List<Song>>()
    private val albums = MutableLiveData<List<Album>>()
    private val artists = MutableLiveData<List<Artist>>()
    private val albumArtists = MutableLiveData<List<Artist>>()
    private val composers = MutableLiveData<List<Artist>>()
    private val playlists = MutableLiveData<List<PlaylistWithSongs>>()
    private val genres = MutableLiveData<List<Genre>>()
    private val years = MutableLiveData<List<ReleaseYear>>()
    private val fileSystem = MutableLiveData<FileSystemQuery>()
    private val fabMargin = MutableLiveData(LibraryMargin(0))
    private val miniPlayerMargin = MutableLiveData(LibraryMargin(0))
    private val songHistory = MutableLiveData<List<Song>>()
    private val notRecentlyPlayed = MutableLiveData<List<Song>>()

    fun getSuggestions(): LiveData<SuggestedResult> = suggestions

    fun getSongs(): LiveData<List<Song>> = songs

    fun getAlbums(): LiveData<List<Album>> = albums

    fun getArtists(): LiveData<List<Artist>> = artists

    fun getAlbumArtists(): LiveData<List<Artist>> = albumArtists

    fun getComposers(): LiveData<List<Artist>> = composers

    fun getPlaylists(): LiveData<List<PlaylistWithSongs>> = playlists

    fun getGenres(): LiveData<List<Genre>> = genres

    fun getYears(): LiveData<List<ReleaseYear>> = years

    fun getFileSystem(): LiveData<FileSystemQuery> = fileSystem

    fun getFabMargin(): LiveData<LibraryMargin> = fabMargin

    fun getMiniPlayerMargin(): LiveData<LibraryMargin> = miniPlayerMargin

    private fun createValueAnimator(
        oldValue: Int,
        newValue: Int,
        setter: (Int) -> Unit,
    ): Animator =
        ValueAnimator.ofInt(oldValue, newValue).apply {
            addUpdateListener { setter(it.animatedValue as Int) }
            doOnEnd { setter(newValue) }
            start()
        }

    fun setLibraryMargins(
        fabBottomMargin: LibraryMargin,
        bottomSheetMargin: LibraryMargin,
    ) {
        val fabAnimator =
            createValueAnimator(
                oldValue = fabMargin.value!!.margin,
                newValue = fabBottomMargin.margin,
            ) {
                fabMargin.postValue(fabBottomMargin.copy(margin = it))
            }
        val miniPlayerAnimator =
            createValueAnimator(
                oldValue = miniPlayerMargin.value!!.margin,
                newValue = bottomSheetMargin.margin,
            ) {
                miniPlayerMargin.postValue(bottomSheetMargin.copy(margin = it))
            }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fabAnimator, miniPlayerAnimator)
        animatorSet.start()
    }

    suspend fun albumById(id: Long) = repository.albumById(id)

    fun artistById(id: Long) = repository.artistById(id)

    suspend fun devicePlaylistById(id: Long) = repository.devicePlaylist(id)

    fun genreBySong(song: Song): LiveData<Genre> =
        liveData(IO) {
            emit(repository.genreBySong(song))
        }

    fun allSongs() =
        liveData(IO) {
            emit(repository.allSongs())
        }

    fun forceReload(reloadType: ReloadType) =
        viewModelScope.launch(IO) {
            when (reloadType) {
                ReloadType.Songs -> fetchSongs()
                ReloadType.Albums -> fetchAlbums()
                ReloadType.Artists -> fetchArtists()
                ReloadType.AlbumArtists -> fetchAlbumArtists()
                ReloadType.Composers -> fetchComposers()
                ReloadType.Playlists -> fetchPlaylists()
                ReloadType.Genres -> fetchGenres()
                ReloadType.Folders -> fetchFolders()
                ReloadType.Years -> fetchYears()
                ReloadType.Suggestions -> fetchSuggestions()
            }
        }

    /**
     * Refreshes every library surface after a filter change (for example ghost folders).
     */
    fun reloadAll() {
        viewModelScope.launch(IO) {
            fetchSongs()
            fetchAlbums()
            fetchArtists()
            fetchAlbumArtists()
            fetchComposers()
            fetchPlaylists()
            fetchGenres()
            fetchYears()
            fetchSuggestions()
            fetchFolders()
        }
    }

    private suspend fun fetchSuggestions() {
        val currentValue =
            suggestions.value?.copy(state = SuggestedResult.State.Loading)
                ?: SuggestedResult(SuggestedResult.State.Loading)
        suggestions.postValue(currentValue)

        val data = repository.homeSuggestions()
        suggestions.postValue(SuggestedResult(SuggestedResult.State.Ready, data))
    }

    private suspend fun fetchSongs() {
        songs.postValue(repository.allSongs())
    }

    private suspend fun fetchAlbums() {
        albums.postValue(repository.allAlbums())
    }

    private suspend fun fetchArtists() {
        val hasAlbumArtistsTab =
            Preferences.libraryCategories.any {
                it.category == CategoryInfo.Category.AlbumArtists && it.visible
            }
        if (!hasAlbumArtistsTab && Preferences.onlyAlbumArtists) {
            artists.postValue(repository.allAlbumArtists())
        } else {
            artists.postValue(repository.allArtists())
        }
    }

    private suspend fun fetchAlbumArtists() {
        albumArtists.postValue(repository.allAlbumArtists())
    }

    private suspend fun fetchComposers() {
        composers.postValue(repository.allComposers())
    }

    private suspend fun fetchPlaylists() {
        playlists.postValue(repository.playlistsWithSongs(true))
    }

    private suspend fun fetchGenres() {
        genres.postValue(repository.allGenres())
    }

    private suspend fun fetchYears() {
        years.postValue(repository.allYears())
    }

    private fun fetchFolders() {
        navigateToPath()
    }

    private suspend fun filesToSongs(
        files: List<FileSystemItem>,
        includeFolders: Boolean,
        deepListing: Boolean,
    ): List<Song> =
        buildList {
            if (includeFolders) {
                val songs =
                    files.filterIsInstance<Folder>().flatMap {
                        if (deepListing) {
                            repository.songsByFolder(it.filePath, true)
                        } else {
                            it.songs
                        }
                    }
                addAll(songs)
            }
            addAll(files.filterIsInstance<Song>())
        }

    fun navigateToPath(
        navigateToPath: String? = null,
        hierarchyView: Boolean = Preferences.hierarchyFolderView,
    ) = viewModelScope.launch(IO) {
        if (hierarchyView) {
            val path =
                if (navigateToPath.isNullOrEmpty()) {
                    fileSystem.value?.path ?: Preferences.startDirectory.getCanonicalPathSafe()
                } else {
                    navigateToPath
                }
            fileSystem.postValue(repository.filesInPath(path))
        } else {
            fileSystem.postValue(repository.allFolders())
        }
    }

    fun scanPaths(
        context: Context,
        paths: Array<String>,
    ): LiveData<Int> =
        liveData(IO) {
            val scanResult =
                runCatching {
                    suspendCancellableCoroutine { continuation ->
                        var progress = 0
                        val total = paths.size

                        MediaScannerConnection.scanFile(context, paths, null) { _, _ ->
                            progress++
                            if (progress == total && continuation.isActive) {
                                continuation.resume(total)
                            }
                        }
                    }
                }
            emit(scanResult.getOrElse { 0 })
        }

    fun scanAllPaths(context: Context): LiveData<Int> {
        // We attempt to retrieve all storage roots using our StorageManager-based utility.
        // If that fails for some reason, we fall back to Environment.getExternalStorageDirectory()
        // to scan at least the device's primary storage root.
        val storageRoots =
            StorageUtil
                .refreshStorageVolumes()
                .map { it.filePath }
                .plus(Environment.getExternalStorageDirectory().path)
                .distinct()
                .toTypedArray()

        return scanPaths(context, storageRoots)
    }

    fun blacklistPath(file: File) =
        viewModelScope.launch(IO) {
            inclExclDao.insertPath(InclExclEntity(file.getCanonicalPathSafe(), InclExclDao.BLACKLIST))
            forceReload(ReloadType.Folders)
        }

    fun listSongsFromFiles(
        song: Song,
        files: List<FileSystemItem>?,
    ) = liveData(IO) {
        if (!files.isNullOrEmpty()) {
            val currentFolder = fileSystem.value
            val songs =
                if (currentFolder != null) {
                    filesToSongs(files, includeFolders = false, deepListing = false)
                } else {
                    emptyList()
                }
            val startPos = songs.indexOfSong(song.id).coerceAtLeast(0)
            emit(songs to startPos)
        }
    }

    fun songs(providers: List<Any>): LiveData<List<Song>> =
        liveData(IO) {
            val songs =
                providers
                    .filterIsInstance<SongProvider>()
                    .flatMap { it.songs }
            emit(songs)
        }

    fun songs(
        files: List<FileSystemItem>,
        includeFolders: Boolean,
        deepListing: Boolean,
    ): LiveData<List<Song>> =
        liveData(IO) {
            val songs = filesToSongs(files, includeFolders, deepListing)
            emit(songs)
        }

    fun artists(type: ContentType): LiveData<List<Artist>> =
        liveData(IO) {
            when (type) {
                ContentType.TopArtists -> emit(repository.topArtists())
                ContentType.RecentArtists -> emit(repository.recentArtists())
                else -> emit(arrayListOf())
            }
        }

    fun albums(type: ContentType): LiveData<List<Album>> =
        liveData(IO) {
            when (type) {
                ContentType.TopAlbums -> emit(repository.topAlbums())
                ContentType.RecentAlbums -> emit(repository.recentAlbums())
                else -> emit(arrayListOf())
            }
        }

    fun clearHistory() {
        viewModelScope.launch(IO) {
            repository.clearSongHistory()
        }
        songHistory.value = emptyList()
    }

    fun playCountSongs(period: ListeningPeriod): LiveData<List<Song>> =
        liveData(IO) {
            emit(repository.playCountSongs(period))
        }

    fun artistPlayStats(period: ListeningPeriod): LiveData<List<ArtistPlayStat>> =
        liveData(IO) {
            emit(repository.artistPlayStats(period))
        }

    fun playCountSongsFlow(period: ListeningPeriod): Flow<List<Song>> = repository.playCountSongsFlow(period)

    fun artistPlayStatsFlow(period: ListeningPeriod): Flow<List<ArtistPlayStat>> = repository.artistPlayStatsFlow(period)

    fun deleteSongsFromHistory(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch(IO) {
            repository.deleteSongsInHistory(songIds)
        }
    }

    suspend fun shortcutTargetExists(
        type: String,
        id: String,
        path: String?,
    ): Boolean? =
        withContext(IO) {
            runCatching {
                when (type) {
                    "Album" -> {
                        if (!isExternalStorageAvailable()) return@runCatching null
                        id.toLongOrNull()?.let { repository.albumById(it).songs.isNotEmpty() } ?: false
                    }

                    "Artist" -> {
                        if (!isExternalStorageAvailable()) return@runCatching null
                        id.toLongOrNull()?.let { repository.artistById(it).songs.isNotEmpty() } ?: false
                    }

                    "Playlist" -> {
                        id.toLongOrNull()?.let { playlistId ->
                            repository.playlistsWithSongs().any { it.playlistEntity.playListId == playlistId }
                        } ?: false
                    }

                    "Folder" -> {
                        val folderPath = path ?: return@runCatching false
                        val folder = File(folderPath)
                        if (folder.exists()) {
                            true
                        } else {
                            // A missing folder is only definitive while its volume is mounted and
                            // its parent is still visible.
                            val state = Environment.getExternalStorageState(folder)
                            val parentVisible = folder.parentFile?.exists() == true
                            if ((state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) &&
                                parentVisible
                            ) {
                                false
                            } else {
                                null
                            }
                        }
                    }

                    else -> {
                        false
                    }
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                null
            }
        }

    private fun isExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }

    fun lastAddedSongs(): LiveData<List<Song>> =
        liveData(IO) {
            emit(repository.recentSongs())
        }

    fun favoriteSongsFlow() = repository.favoriteSongsFlow()

    fun playCountSongsFlow() = repository.playCountSongsFlow()

    fun historySongsFlow() = repository.historySongsFlow()

    suspend fun shortcutSongs(
        type: String,
        id: String,
        path: String?,
    ): List<Song> =
        withContext(IO) {
            runCatching {
                when (type) {
                    "Album" -> id.toLongOrNull()?.let { repository.albumById(it).songs } ?: emptyList()
                    "Artist" -> id.toLongOrNull()?.let { repository.artistById(it).songs } ?: emptyList()
                    "Playlist" -> id.toLongOrNull()?.let { repository.playlistSongs(it).toSongs() } ?: emptyList()
                    "Folder" -> path?.let { repository.songsByFolder(it, includeSubfolders = true) } ?: emptyList()
                    else -> emptyList()
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                emptyList()
            }
        }

    fun notRecentlyPlayedSongs(): LiveData<List<Song>> {
        loadNotRecentlyPlayedSongs()
        return notRecentlyPlayed
    }

    fun loadNotRecentlyPlayedSongs() =
        viewModelScope.launch(IO) {
            notRecentlyPlayed.postValue(repository.notRecentlyPlayedSongs())
        }

    private val _addToPlaylistUiState = MutableStateFlow<AddToPlaylistUiState?>(null)
    val addToPlaylistUiState = _addToPlaylistUiState.asStateFlow()

    fun prepareToAddToPlaylist(searchQuery: String? = null) =
        viewModelScope.launch(IO) {
            _addToPlaylistUiState.update { it ?: AddToPlaylistUiState.Loading }

            val playlists =
                if (searchQuery.isNullOrBlank()) {
                    repository.playlistsWithSongs()
                } else {
                    repository.searchPlaylists(searchQuery)
                }

            _addToPlaylistUiState.value =
                if (playlists.isEmpty()) {
                    AddToPlaylistUiState.Empty(searchQuery)
                } else {
                    AddToPlaylistUiState.Ready(playlists)
                }
        }

    fun addToPlaylists(
        playlistsIds: List<Long>,
        songs: List<Song>,
    ) = viewModelScope.launch(IO) {
        val state = addToPlaylistUiState.value ?: return@launch
        if (state is AddToPlaylistUiState.Ready && state.playlists.isNotEmpty()) {
            _addToPlaylistUiState.value = state.copy(isLoading = true)

            var success = true
            val playlists = state.playlists.filter { playlistsIds.contains(it.playlistEntity.playListId) }
            for (playlist in playlists) {
                val checkedSongs =
                    songs.filterNot {
                        repository.checkSongExistInPlaylist(playlist.playlistEntity, it)
                    }
                val result =
                    runCatching {
                        insertSongs(
                            songs =
                                checkedSongs.map {
                                    it.toSongEntity(playListId = playlist.playlistEntity.playListId)
                                },
                        )
                    }
                success = success && result.isSuccess
            }

            _addToPlaylistUiState.value = AddToPlaylistUiState.Completed(success)
            forceReload(ReloadType.Playlists)
        }
    }

    fun finishAddingToPlaylists() {
        _addToPlaylistUiState.value = null
    }

    fun updatePlaylist(
        playlist: PlaylistEntity,
        newName: String,
        newImageUri: String?,
        newDescription: String?,
    ) = viewModelScope.launch(IO) {
        var imageUri = playlist.customCoverUri
        if (newImageUri != imageUri) {
            if (!imageUri.isNullOrEmpty()) {
                customPlaylistImageManager.deleteImage(imageUri.toUri())
            }
            imageUri = customPlaylistImageManager.createPlaylistImage(newImageUri)?.toString()
        }
        repository.updatePlaylist(
            playlist.copy(
                playlistName = newName,
                customCoverUri = imageUri,
                description = newDescription,
            ),
        )
    }

    fun deleteSongsInPlaylist(songs: List<SongEntity>) =
        viewModelScope.launch(IO) {
            repository.deleteSongsInPlaylist(songs)
            forceReload(ReloadType.Playlists)
        }

    fun deletePlaylists(playlists: List<PlaylistEntity>) =
        viewModelScope.launch(IO) {
            for (playlist in playlists) {
                playlist.customCoverUri?.let {
                    customPlaylistImageManager.deleteImage(it.toUri())
                }
            }
            repository.deletePlaylists(playlists)
            forceReload(ReloadType.Playlists)
        }

    fun createCustomPlaylist(
        playlistName: String,
        customCoverUri: String? = null,
        description: String? = null,
        songs: List<Song> = emptyList(),
    ): LiveData<AddToPlaylistResult> =
        liveData(IO) {
            emit(AddToPlaylistResult(playlistName, isWorking = true))

            val playlists = checkPlaylistExists(playlistName)
            if (playlists.isEmpty()) {
                val playlistImageUri = customPlaylistImageManager.createPlaylistImage(customCoverUri)
                val playlistEntity =
                    PlaylistEntity(
                        playlistName = playlistName,
                        customCoverUri = playlistImageUri?.toString(),
                        description = description,
                    )
                val playlistId: Long = createPlaylist(playlistEntity)
                if (songs.isNotEmpty()) {
                    insertSongs(songs.map { it.toSongEntity(playlistId) })
                }
                val playlistCreated = (playlistId != -1L)
                val isFavoritePlaylist = repository.checkFavoritePlaylist()?.playListId == playlistId
                emit(
                    AddToPlaylistResult(
                        playlistName,
                        playlistCreated = playlistCreated,
                        isFavoritePlaylist = isFavoritePlaylist,
                        insertedSongs = songs.size,
                    ),
                )
            } else {
                // Playlist already exists
                emit(AddToPlaylistResult(playlistName, playlistCreated = false))
            }
            forceReload(ReloadType.Playlists)
        }

    fun favoritePlaylist(): LiveData<PlaylistEntity> =
        liveData(IO) {
            emit(repository.favoritePlaylist())
        }

    fun isSongFavorite(song: Song): LiveData<Boolean> =
        liveData(IO) {
            emit(repository.isSongFavorite(song.id))
        }

    suspend fun insertSongs(songs: List<SongEntity>) = repository.insertSongsInPlaylist(songs)

    private suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> = repository.checkPlaylistExists(playlistName)

    private suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long = repository.createPlaylist(playlistEntity)

    private suspend fun deleteMissingContent() {
        repository.deleteMissingContent()
    }

    private val playlistImportMutex = Mutex()
    private var pendingPlaylistImport: PendingPlaylistImport? = null
    private val _playlistImportConflict = MutableStateFlow<PlaylistImportConflictState?>(null)
    val playlistImportConflict = _playlistImportConflict.asStateFlow()
    val playlistImportConflictLiveData = _playlistImportConflict.asLiveData()
    private val _playlistImportResult = MutableLiveData<ImportResult?>()
    val playlistImportResult: LiveData<ImportResult?> = _playlistImportResult

    fun getDevicePlaylists(): LiveData<List<ImportablePlaylistResult>> =
        liveData(IO) {
            val devicePlaylists = repository.devicePlaylists()
            val importablePlaylists =
                devicePlaylists
                    .map {
                        ImportablePlaylistResult(it.name, it.getSongs())
                    }.filter {
                        it.songs.isNotEmpty()
                    }
            emit(importablePlaylists)
        }

    fun startPlaylistImport(
        context: Context,
        playlist: ImportablePlaylistResult,
        selectedSongs: List<Song>,
    ) = viewModelScope.launch(IO) {
        val songs = selectedSongs.distinctBy { it.id }
        if (songs.isEmpty()) return@launch
        playlistImportMutex.withLock {
            val pending = PendingPlaylistImport(playlist.playlistName, songs)
            pendingPlaylistImport = pending
            _playlistImportConflict.value = null
            val exactMatches = repository.checkPlaylistExists(pending.sourceName)
            if (exactMatches.isNotEmpty()) {
                _playlistImportConflict.value = buildConflictState(context, pending, exactMatches)
                return@withLock
            }
            when (createImportedPlaylist(pending, pending.sourceName)) {
                CreateResult.Created -> {
                    finishCreated(context, pending, pending.sourceName)
                }

                CreateResult.NameTaken -> {
                    val refreshed = repository.checkPlaylistExists(pending.sourceName)
                    if (refreshed.isNotEmpty()) {
                        _playlistImportConflict.value = buildConflictState(context, pending, refreshed)
                    } else {
                        failImport(context, pending.sourceName)
                    }
                }

                CreateResult.Failed -> {
                    failImport(context, pending.sourceName)
                }
            }
        }
    }

    fun applyKeepBoth(context: Context) {
        val pending = pendingPlaylistImport ?: return
        val current = _playlistImportConflict.value ?: return
        viewModelScope.launch(IO) {
            playlistImportMutex.withLock {
                _playlistImportConflict.value = current.copy(applying = true, renameError = null, generalError = null)
                var attempts = 0
                while (attempts <= PlaylistImportNameResolver.MAX_SUFFIX) {
                    val names = repository.playlists().map { it.playlistName }
                    val name = PlaylistImportNameResolver.resolveName(pending.sourceName, names)
                    if (name == null) {
                        _playlistImportConflict.value =
                            current.copy(
                                applying = false,
                                defaultName = null,
                                existingNames = names,
                                generalError = PlaylistImportGeneralError.SuffixExhausted,
                            )
                        return@withLock
                    }
                    when (createImportedPlaylist(pending, name)) {
                        CreateResult.Created -> {
                            finishCreated(context, pending, name)
                            return@withLock
                        }

                        CreateResult.NameTaken -> {
                            attempts++
                        }

                        CreateResult.Failed -> {
                            _playlistImportConflict.value =
                                current.copy(
                                    applying = false,
                                    generalError = PlaylistImportGeneralError.DatabaseFailure,
                                )
                            return@withLock
                        }
                    }
                }
                val names = repository.playlists().map { it.playlistName }
                _playlistImportConflict.value =
                    current.copy(
                        applying = false,
                        defaultName = PlaylistImportNameResolver.resolveName(pending.sourceName, names),
                        existingNames = names,
                        generalError = PlaylistImportGeneralError.SuffixExhausted,
                    )
            }
        }
    }

    fun applyReplace(context: Context) {
        val pending = pendingPlaylistImport ?: return
        val current = _playlistImportConflict.value ?: return
        val target = current.existing.singleOrNull() ?: return
        if (!current.canReplace) return
        viewModelScope.launch(IO) {
            playlistImportMutex.withLock {
                _playlistImportConflict.value = current.copy(applying = true, renameError = null, generalError = null)
                val replaced =
                    try {
                        repository.replacePlaylistContentsIfEligible(
                            playlistId = target.playListId,
                            expectedName = current.sourceName,
                            favoritesName = context.getString(R.string.favorites_label),
                            songs = pending.songs,
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        null
                    }
                when (replaced) {
                    true -> {
                        finishReplaced(context, pending, target.playlistName)
                    }

                    false -> {
                        val refreshed = repository.checkPlaylistExists(current.sourceName)
                        if (refreshed.isEmpty()) {
                            failImport(context, current.sourceName)
                        } else {
                            _playlistImportConflict.value =
                                buildConflictState(context, pending, refreshed)
                                    .copy(generalError = PlaylistImportGeneralError.ConflictChanged)
                        }
                    }

                    null -> {
                        _playlistImportConflict.value =
                            current.copy(
                                applying = false,
                                generalError = PlaylistImportGeneralError.DatabaseFailure,
                            )
                    }
                }
            }
        }
    }

    fun applyRename(
        context: Context,
        rawName: String,
    ) {
        val pending = pendingPlaylistImport ?: return
        val current = _playlistImportConflict.value ?: return
        viewModelScope.launch(IO) {
            playlistImportMutex.withLock {
                _playlistImportConflict.value = current.copy(applying = true, renameError = null, generalError = null)
                val names = repository.playlists().map { it.playlistName }
                when (val validation = PlaylistImportNameResolver.validateRename(rawName, names)) {
                    PlaylistImportNameResolver.RenameValidation.Blank -> {
                        _playlistImportConflict.value =
                            current.copy(
                                applying = false,
                                existingNames = names,
                                renameError = PlaylistImportRenameError.Blank,
                            )
                    }

                    is PlaylistImportNameResolver.RenameValidation.Duplicate -> {
                        _playlistImportConflict.value =
                            current.copy(
                                applying = false,
                                existingNames = names,
                                renameError = PlaylistImportRenameError.Duplicate,
                            )
                    }

                    is PlaylistImportNameResolver.RenameValidation.Valid -> {
                        when (createImportedPlaylist(pending, validation.name)) {
                            CreateResult.Created -> {
                                finishCreated(context, pending, validation.name)
                            }

                            CreateResult.NameTaken -> {
                                val refreshed = repository.playlists().map { it.playlistName }
                                _playlistImportConflict.value =
                                    current.copy(
                                        applying = false,
                                        existingNames = refreshed,
                                        renameError = PlaylistImportRenameError.Duplicate,
                                    )
                            }

                            CreateResult.Failed -> {
                                _playlistImportConflict.value =
                                    current.copy(
                                        applying = false,
                                        generalError = PlaylistImportGeneralError.DatabaseFailure,
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearPlaylistImportRenameError() {
        _playlistImportConflict.update { it?.copy(renameError = null) }
    }

    fun cancelPlaylistImport() {
        pendingPlaylistImport = null
        _playlistImportConflict.value = null
    }

    fun consumePlaylistImportResult() {
        _playlistImportResult.value = null
    }

    private suspend fun createImportedPlaylist(
        pending: PendingPlaylistImport,
        name: String,
    ): CreateResult =
        try {
            val playlistId =
                repository.createPlaylistWithSongsIfNameFree(
                    playlist = PlaylistEntity(playlistName = name),
                    songs = pending.songs,
                )
            if (playlistId == -1L) CreateResult.NameTaken else CreateResult.Created
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            CreateResult.Failed
        }

    private suspend fun buildConflictState(
        context: Context,
        pending: PendingPlaylistImport,
        existing: List<PlaylistEntity>,
    ): PlaylistImportConflictState {
        val names = repository.playlists().map { it.playlistName }
        val single = existing.singleOrNull()
        val favoritesName = context.getString(R.string.favorites_label)
        val replaceBlock =
            when {
                existing.size > 1 -> PlaylistImportReplaceBlock.MultipleMatches
                single?.playlistName == favoritesName -> PlaylistImportReplaceBlock.Favorites
                else -> null
            }
        return PlaylistImportConflictState(
            sourceName = pending.sourceName,
            selectedSongCount = pending.songs.size,
            existing = existing,
            existingSongCount = single?.let { repository.playlistSongs(it.playListId).size } ?: 0,
            defaultName = PlaylistImportNameResolver.resolveName(pending.sourceName, names),
            existingNames = names,
            replaceBlock = replaceBlock,
        )
    }

    private fun finishCreated(
        context: Context,
        pending: PendingPlaylistImport,
        actualName: String,
    ) {
        pendingPlaylistImport = null
        _playlistImportConflict.value = null
        _playlistImportResult.postValue(ImportResult.created(context, actualName, pending.songs.size))
        forceReload(ReloadType.Playlists)
    }

    private fun finishReplaced(
        context: Context,
        pending: PendingPlaylistImport,
        actualName: String,
    ) {
        pendingPlaylistImport = null
        _playlistImportConflict.value = null
        _playlistImportResult.postValue(ImportResult.replaced(context, actualName, pending.songs.size))
        forceReload(ReloadType.Playlists)
    }

    private fun failImport(
        context: Context,
        sourceName: String,
    ) {
        pendingPlaylistImport = null
        _playlistImportConflict.value = null
        _playlistImportResult.postValue(ImportResult.error(context, sourceName))
    }

    private data class PendingPlaylistImport(
        val sourceName: String,
        val songs: List<Song>,
    )

    private enum class CreateResult {
        Created,
        NameTaken,
        Failed,
    }

    fun deleteSongs(songs: List<Song>) =
        viewModelScope.launch(IO) {
            repository.deleteSongs(songs)
        }

    private suspend fun initializeBlacklist() {
        if (!Preferences.initializedBlacklist) {
            repository.initializeBlacklist()
            Preferences.initializedBlacklist = true
        }
    }

    fun getLoginState(service: ScrobblingService) = repository.getLoginState(service)

    fun logInToService(
        service: ScrobblingService,
        params: LoginParams,
    ) = viewModelScope.launch(IO) {
        repository.loginToService(service, params)
    }

    fun logoutFromService(service: ScrobblingService) =
        viewModelScope.launch(IO) {
            repository.logoutFromService(service)
        }

    @Suppress("DEPRECATION")
    fun handleIntent(intent: Intent): LiveData<HandleIntentResult> =
        liveData(IO) {
            val result = HandleIntentResult(handled = true)
            val uri = intent.data
            if (uri == null || uri.scheme == "glance-action") {
                emit(result.copy(handled = false))
            } else {
                try {
                    if (uri.toString().isNotEmpty()) {
                        val songs = repository.songsByUri(uri)
                        emit(result.copy(songs = songs, failed = songs.isEmpty()))
                    } else {
                        when (intent.type) {
                            MediaStore.Audio.Playlists.CONTENT_TYPE -> {
                                val id = parseIdFromIntent(intent, "playlistId", "playlist")
                                if (id >= 0) {
                                    val position = intent.getIntExtra("position", 0)
                                    val playlist = devicePlaylistById(id)
                                    if (playlist != Playlist.EmptyPlaylist) {
                                        emit(result.copy(songs = playlist.getSongs(), position = position))
                                    } else {
                                        emit(result)
                                    }
                                }
                            }

                            MediaStore.Audio.Albums.CONTENT_TYPE -> {
                                val id = parseIdFromIntent(intent, "albumId", "album")
                                if (id >= 0) {
                                    val position = intent.getIntExtra("position", 0)
                                    emit(result.copy(songs = albumById(id).songs, position = position))
                                }
                            }

                            MediaStore.Audio.Artists.CONTENT_TYPE -> {
                                val id = parseIdFromIntent(intent, "artistId", "artist")
                                if (id >= 0) {
                                    val position = intent.getIntExtra("position", 0)
                                    emit(result.copy(songs = artistById(id).songs, position = position))
                                }
                            }

                            else -> {
                                emit(result.copy(handled = false))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LibraryViewModel", "handleIntent() failed; intent=$intent", e)
                }
            }
        }

    private fun parseIdFromIntent(
        intent: Intent,
        longKey: String,
        stringKey: String,
    ): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            id = intent.getStringExtra(stringKey)?.toLongOrNull() ?: -1
        }
        return id
    }
}

enum class ReloadType {
    Songs,
    Albums,
    Artists,
    AlbumArtists,
    Composers,
    Playlists,
    Genres,
    Folders,
    Years,
    Suggestions,
}
