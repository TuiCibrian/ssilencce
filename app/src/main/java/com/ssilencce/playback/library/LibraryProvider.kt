package com.ssilencce.playback.library

import android.content.Context
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.ssilencce.R
import com.ssilencce.coil.CoverProvider.Companion.ALBUM_ARTIST_COVER_PATH
import com.ssilencce.coil.CoverProvider.Companion.ALBUM_COVER_PATH
import com.ssilencce.coil.CoverProvider.Companion.ARTIST_COVER_PATH
import com.ssilencce.coil.CoverProvider.Companion.GENRE_COVER_PATH
import com.ssilencce.coil.CoverProvider.Companion.PLAYLIST_COVER_PATH
import com.ssilencce.coil.CoverProvider.Companion.getImageUri
import com.ssilencce.core.model.CategoryInfo
import com.ssilencce.core.model.filesystem.FileSystemItem
import com.ssilencce.core.sort.FileSortMode
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.mapper.toSongs
import com.ssilencce.data.model.Folder
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.albumInfo
import com.ssilencce.extensions.media.artistInfo
import com.ssilencce.extensions.media.asNumberOfSongs
import com.ssilencce.extensions.media.songCountStr
import com.ssilencce.playback.toMediaItems
import com.ssilencce.util.Preferences

class LibraryProvider(
    private val repository: Repository,
) {
    private val _searchResult = mutableListOf<MediaItem>()
    val searchResult: List<MediaItem> get() = _searchResult

    suspend fun getMediaItemsForPlayback(
        mediaItems: List<MediaItem>,
        tryToResolveComplexPaths: Boolean = false,
    ): List<MediaItem> {
        val resolvedMediaItems =
            mediaItems
                .filter { item -> item.localConfiguration != null }
                .toMutableList()
        if (resolvedMediaItems.size == mediaItems.size) {
            return resolvedMediaItems
        }
        val (songs, missingMediaItems) =
            (mediaItems - resolvedMediaItems.toSet()).let { invalidItems ->
                repository.songsByMediaItems(invalidItems)
            }
        if (songs.isNotEmpty()) {
            resolvedMediaItems.addAll(songs.toMediaItems())
        }
        if (missingMediaItems.isNotEmpty()) {
            val complexMediaItems =
                if (tryToResolveComplexPaths) {
                    missingMediaItems.filter { item -> item.mediaId.contains(":") }
                } else {
                    emptyList()
                }
            if (complexMediaItems.isNotEmpty()) {
                getMediaItemsForAAOSPlayback(complexMediaItems)?.first?.forEach {
                    resolvedMediaItems.add(it)
                }
            } else {
                missingMediaItems.forEach {
                    getPlayableSongs(it.mediaId).let { playableSongs ->
                        if (playableSongs.isNotEmpty()) {
                            resolvedMediaItems.addAll(playableSongs.toMediaItems())
                        }
                    }
                }
            }
        }
        return resolvedMediaItems
    }

    suspend fun getMediaItemsForAAOSPlayback(
        mediaItems: List<MediaItem>,
        currentQueue: List<MediaItem> = emptyList(),
    ): Pair<List<MediaItem>, Int>? {
        val single = mediaItems.singleOrNull()
        return if (single != null) {
            val path = MediaIDs.splitPath(single.mediaId)
            when (path.firstOrNull()) {
                SEARCH -> {
                    val id = path.getOrNull(1)
                    if (id == null || searchResult.isEmpty()) return null
                    val transformedMediaItems = searchResult.map { it.buildUpon().setMediaId(id).build() }
                    Pair(
                        transformedMediaItems,
                        transformedMediaItems.indexOfFirst { it.mediaId == id }.coerceAtLeast(0),
                    )
                }

                MediaIDs.SONGS -> {
                    val id = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val allSongs = repository.allSongs()
                    Pair(
                        allSongs.map { it.toAutoMediaItem() },
                        allSongs.indexOfFirst { it.id == id }.coerceAtLeast(0),
                    )
                }

                MediaIDs.ALBUMS -> {
                    val albumId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val album = repository.albumById(albumId)
                    Pair(
                        album.songs.map { it.toAutoMediaItem() },
                        album.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val artistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val artistSongs = repository.artistById(artistId).sortedSongs
                    Pair(
                        artistSongs.map { it.toAutoMediaItem() },
                        artistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.ALBUM_ARTISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val albumArtistName = path.getOrNull(1) ?: return null
                    val albumArtistSongs = repository.albumArtistByName(albumArtistName).sortedSongs
                    Pair(
                        albumArtistSongs.map { it.toAutoMediaItem() },
                        albumArtistSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.COMPOSERS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val composerId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val composerSongs =
                        repository
                            .allComposers()
                            .firstOrNull { it.id == composerId }
                            ?.sortedSongs
                            .orEmpty()
                    Pair(
                        composerSongs.map { it.toAutoMediaItem() },
                        composerSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.FOLDERS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val folderPath =
                        path
                            .getOrNull(1)
                            ?.let(MediaPathCodec::decode)
                            ?: return null
                    val folderSongs = repository.songsByFolder(folderPath, includeSubfolders = false)
                    Pair(
                        folderSongs.map { it.toAutoMediaItem() },
                        folderSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.CURRENT_QUEUE -> {
                    val originalMediaId =
                        path
                            .getOrNull(1)
                            ?.let(MediaPathCodec::decode)
                            ?: return null
                    val startIndex = currentQueue.indexOfFirst { it.mediaId == originalMediaId }
                    if (startIndex < 0) return null
                    Pair(currentQueue, startIndex)
                }

                MediaIDs.PLAYLISTS -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val playlistId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playlist = repository.playlistWithSongs(playlistId)
                    Pair(
                        playlist.songs.toSongs().map { it.toAutoMediaItem() },
                        playlist.songs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.GENRES -> {
                    val songId = path.getOrNull(2)?.toLongOrNull() ?: return null
                    val genreId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val songsByGenre = repository.songsByGenre(genreId)
                    Pair(
                        songsByGenre.map { it.toAutoMediaItem() },
                        songsByGenre.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.TOP_TRACKS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val playCountSongs = repository.playCountSongs()
                    Pair(
                        playCountSongs.map { it.toAutoMediaItem() },
                        playCountSongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                MediaIDs.RECENT_SONGS -> {
                    val songId = path.getOrNull(1)?.toLongOrNull() ?: return null
                    val historySongs = repository.historySongs()
                    Pair(
                        historySongs.map { it.toAutoMediaItem() },
                        historySongs.indexOfFirst { it.id == songId }.coerceAtLeast(0),
                    )
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }
    }

    suspend fun getChildren(
        context: Context,
        parentId: String,
        currentQueue: List<MediaItem> = emptyList(),
    ): List<MediaItem> =
        if (MediaIDs.isPath(parentId)) {
            val parts = MediaIDs.splitPath(parentId)
            when {
                parts.firstOrNull() == MediaIDs.FOLDERS -> {
                    val path =
                        if (parts.size == 2) {
                            parts[1].let(MediaPathCodec::decode)
                        } else {
                            null
                        }
                    if (path == null) emptyList() else getFolderChildren(context, parentId, path)
                }

                parts.size < 2 -> {
                    listOf(MediaItem.EMPTY)
                }

                else -> {
                    getPlayableMediaItems(parts[0], parts[1])
                }
            }
        } else {
            when (parentId) {
                MediaIDs.ROOT -> {
                    getRootChildren(context)
                }

                MediaIDs.ALBUMS -> {
                    repository.allAlbums().map { album ->
                        MediaItem
                            .Builder()
                            .setMediaId(MediaIDs.getPathId(parentId, album.id))
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                                    .setArtworkUri(getImageUri(ALBUM_COVER_PATH, album.id))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle(album.name)
                                    .setSubtitle(album.albumInfo())
                                    .build(),
                            ).build()
                    }
                }

                MediaIDs.ALBUM_ARTISTS -> {
                    repository.allAlbumArtists().map { albumArtist ->
                        MediaItem
                            .Builder()
                            .setMediaId(MediaIDs.getPathId(parentId, albumArtist.name))
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                    .setArtworkUri(getImageUri(ALBUM_ARTIST_COVER_PATH, albumArtist.name))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle(albumArtist.name)
                                    .setSubtitle(albumArtist.artistInfo(context))
                                    .build(),
                            ).build()
                    }
                }

                MediaIDs.ARTISTS -> {
                    repository.allArtists().map { artist ->
                        MediaItem
                            .Builder()
                            .setMediaId(MediaIDs.getPathId(parentId, artist.id))
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                    .setArtworkUri(getImageUri(ARTIST_COVER_PATH, artist.id))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle(artist.name)
                                    .setSubtitle(artist.artistInfo(context))
                                    .build(),
                            ).build()
                    }
                }

                MediaIDs.COMPOSERS -> {
                    repository.allComposers().map { composer ->
                        MediaItem
                            .Builder()
                            .setMediaId(MediaIDs.getPathId(parentId, composer.id))
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                                    .setArtworkUri(getImageUri(ARTIST_COVER_PATH, composer.id))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle(composer.name)
                                    .setSubtitle(composer.songCount.asNumberOfSongs(context))
                                    .build(),
                            ).build()
                    }
                }

                MediaIDs.PLAYLISTS -> {
                    repository.playlistsWithSongs(sorted = true).map { playlistWithSongs ->
                        MediaItem
                            .Builder()
                            .setMediaId(MediaIDs.getPathId(parentId, playlistWithSongs.playlistEntity.playListId))
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                    .setArtworkUri(getImageUri(PLAYLIST_COVER_PATH, playlistWithSongs.playlistEntity.playListId))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle(playlistWithSongs.playlistEntity.playlistName)
                                    .setSubtitle(playlistWithSongs.songCount.asNumberOfSongs(context))
                                    .build(),
                            ).build()
                    }
                }

                MediaIDs.GENRES -> {
                    repository.allGenres().map { genre ->
                        MediaItem
                            .Builder()
                            .setMediaId(MediaIDs.getPathId(parentId, genre.id))
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_GENRE)
                                    .setArtworkUri(getImageUri(GENRE_COVER_PATH, genre.id))
                                    .setIsBrowsable(true)
                                    .setIsPlayable(false)
                                    .setTitle(genre.name)
                                    .setSubtitle(genre.songCount.asNumberOfSongs(context))
                                    .build(),
                            ).build()
                    }
                }

                MediaIDs.FOLDERS -> {
                    getFolderRootChildren(context)
                }

                MediaIDs.CURRENT_QUEUE -> {
                    currentQueue.map { mediaItem ->
                        mediaItem
                            .buildUpon()
                            .setMediaId(
                                MediaIDs.getPathId(
                                    MediaIDs.CURRENT_QUEUE,
                                    MediaPathCodec.encode(mediaItem.mediaId),
                                ),
                            ).build()
                    }
                }

                // SONGS, TOP_TRACKS, RECENT_SONGS
                else -> {
                    getPlayableMediaItems(parentId)
                }
            }
        }

    fun getItem(itemId: String): MediaItem {
        val songId = itemId.toLongOrNull() ?: return MediaItem.EMPTY
        return repository.songById(songId).toAutoMediaItem()
    }

    suspend fun search(query: String): List<MediaItem> {
        _searchResult.clear()
        _searchResult.addAll(repository.searchSongs(query).map { it.toAutoMediaItem(SEARCH) })
        return _searchResult
    }

    private suspend fun getRootChildren(context: Context): List<MediaItem> {
        val resources = context.resources
        val mediaItems: MutableList<MediaItem> = ArrayList()
        val libraryCategories = Preferences.libraryCategories
        libraryCategories.forEach { categoryInfo ->
            if (categoryInfo.visible) {
                when (categoryInfo.category) {
                    CategoryInfo.Category.Songs -> {
                        mediaItems.add(
                            MediaItem
                                .Builder()
                                .setMediaId(MediaIDs.SONGS)
                                .setMediaMetadata(
                                    MediaMetadata
                                        .Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build(),
                                ).build(),
                        )
                    }

                    CategoryInfo.Category.Albums -> {
                        mediaItems.add(
                            MediaItem
                                .Builder()
                                .setMediaId(MediaIDs.ALBUMS)
                                .setMediaMetadata(
                                    MediaMetadata
                                        .Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build(),
                                ).build(),
                        )
                    }

                    CategoryInfo.Category.Artists -> {
                        if (Preferences.onlyAlbumArtists) {
                            mediaItems.add(
                                MediaItem
                                    .Builder()
                                    .setMediaId(MediaIDs.ALBUM_ARTISTS)
                                    .setMediaMetadata(
                                        MediaMetadata
                                            .Builder()
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setTitle(resources.getString(R.string.album_artists_label))
                                            .build(),
                                    ).build(),
                            )
                        } else {
                            mediaItems.add(
                                MediaItem
                                    .Builder()
                                    .setMediaId(MediaIDs.ARTISTS)
                                    .setMediaMetadata(
                                        MediaMetadata
                                            .Builder()
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setTitle(resources.getString(R.string.artists_label))
                                            .build(),
                                    ).build(),
                            )
                        }
                    }

                    CategoryInfo.Category.Genres -> {
                        mediaItems.add(
                            MediaItem
                                .Builder()
                                .setMediaId(MediaIDs.GENRES)
                                .setMediaMetadata(
                                    MediaMetadata
                                        .Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_GENRES)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build(),
                                ).build(),
                        )
                    }

                    CategoryInfo.Category.Playlists -> {
                        mediaItems.add(
                            MediaItem
                                .Builder()
                                .setMediaId(MediaIDs.PLAYLISTS)
                                .setMediaMetadata(
                                    MediaMetadata
                                        .Builder()
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setTitle(resources.getString(categoryInfo.category.titleRes))
                                        .build(),
                                ).build(),
                        )
                    }

                    CategoryInfo.Category.AlbumArtists -> {
                        mediaItems.add(
                            rootMediaItem(
                                context = context,
                                mediaId = MediaIDs.ALBUM_ARTISTS,
                                titleRes = R.string.album_artists_label,
                                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                            ),
                        )
                    }

                    CategoryInfo.Category.Folders -> {
                        mediaItems.add(
                            rootMediaItem(
                                context = context,
                                mediaId = MediaIDs.FOLDERS,
                                titleRes = R.string.folders_label,
                                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                        )
                    }

                    CategoryInfo.Category.Composers -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            mediaItems.add(
                                rootMediaItem(
                                    context = context,
                                    mediaId = MediaIDs.COMPOSERS,
                                    titleRes = R.string.composers_label,
                                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                                ),
                            )
                        }
                    }

                    CategoryInfo.Category.Queues -> {
                        mediaItems.add(
                            rootMediaItem(
                                context = context,
                                mediaId = MediaIDs.CURRENT_QUEUE,
                                titleRes = R.string.playing_queue_label,
                                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                        )
                    }

                    else -> { /*no-op*/ }
                }
            }
        }

        // Auto is a separate browsing surface: keep these roots available even when the
        // matching phone library category is hidden. Playlists already had a root above.
        mediaItems.addRootIfMissing(
            rootMediaItem(
                context = context,
                mediaId = MediaIDs.FOLDERS,
                titleRes = R.string.folders_label,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            ),
        )
        mediaItems.addRootIfMissing(
            rootMediaItem(
                context = context,
                mediaId = MediaIDs.ALBUM_ARTISTS,
                titleRes = R.string.album_artists_label,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mediaItems.addRootIfMissing(
                rootMediaItem(
                    context = context,
                    mediaId = MediaIDs.COMPOSERS,
                    titleRes = R.string.composers_label,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                ),
            )
        }
        mediaItems.addRootIfMissing(
            rootMediaItem(
                context = context,
                mediaId = MediaIDs.CURRENT_QUEUE,
                titleRes = R.string.playing_queue_label,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            ),
        )

        mediaItems.add(
            MediaItem
                .Builder()
                .setMediaId(MediaIDs.TOP_TRACKS)
                .setMediaMetadata(
                    MediaMetadata
                        .Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle(resources.getString(R.string.top_tracks_label))
                        .setSubtitle(repository.playCountSongs().songCountStr(context))
                        .build(),
                ).build(),
        )

        return mediaItems
    }

    private suspend fun getFolderRootChildren(context: Context): List<MediaItem> {
        if (Preferences.hierarchyFolderView) {
            val storageRoots = repository.filesInPath("").children
            if (storageRoots.isNotEmpty()) {
                return storageRoots.map { item -> item.toFolderMediaItem(context) }
            }
        }
        return repository
            .allFolders()
            .getSortedChildren(FileSortMode.AllFolders)
            .filterIsInstance<Folder>()
            .map { folder -> folder.toFolderMediaItem(context) }
    }

    private suspend fun getFolderChildren(
        context: Context,
        parentMediaId: String,
        path: String,
    ): List<MediaItem> =
        if (Preferences.hierarchyFolderView) {
            repository
                .filesInPath(path)
                .getSortedChildren(FileSortMode.AllFiles)
                .map { item ->
                    when (item) {
                        is Song -> item.toAutoMediaItem(parentMediaId)
                        else -> item.toFolderMediaItem(context)
                    }
                }
        } else {
            repository
                .folderByPath(path)
                .songs
                .map { song -> song.toAutoMediaItem(parentMediaId) }
        }

    private fun FileSystemItem.toFolderMediaItem(context: Context): MediaItem {
        val metadataBuilder =
            MediaMetadata
                .Builder()
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setTitle(fileName)
        (this as? Folder)?.songCount?.let { songCount ->
            metadataBuilder.setSubtitle(songCount.asNumberOfSongs(context))
        }
        return MediaItem
            .Builder()
            .setMediaId(MediaIDs.getPathId(MediaIDs.FOLDERS, MediaPathCodec.encode(filePath)))
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun rootMediaItem(
        context: Context,
        mediaId: String,
        titleRes: Int,
        mediaType: Int,
    ): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setMediaType(mediaType)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(context.getString(titleRes))
                    .build(),
            ).build()

    private fun MutableList<MediaItem>.addRootIfMissing(item: MediaItem) {
        if (none { existing -> existing.mediaId == item.mediaId }) {
            add(item)
        }
    }

    private suspend fun getPlayableSongs(
        parentId: String,
        childId: String? = null,
    ): List<Song> =
        if (childId == null) {
            when (parentId) {
                MediaIDs.SONGS -> repository.allSongs()
                MediaIDs.TOP_TRACKS -> repository.playCountSongs()
                MediaIDs.LAST_ADDED -> repository.recentSongs()
                MediaIDs.RECENT_SONGS -> repository.historySongs()
                MediaIDs.FAVORITES -> repository.favoriteSongs()
                else -> emptyList()
            }
        } else {
            val childIdLong = childId.toLongOrNull()
            if (childIdLong == null) {
                if (parentId == MediaIDs.ALBUM_ARTISTS) {
                    repository.albumArtistByName(childId).sortedSongs
                } else {
                    emptyList()
                }
            } else {
                when (parentId) {
                    MediaIDs.ALBUMS -> {
                        repository.albumById(childIdLong).songs
                    }

                    MediaIDs.ARTISTS -> {
                        repository.artistById(childIdLong).sortedSongs
                    }

                    MediaIDs.COMPOSERS -> {
                        repository
                            .allComposers()
                            .firstOrNull { it.id == childIdLong }
                            ?.sortedSongs
                            .orEmpty()
                    }

                    MediaIDs.PLAYLISTS -> {
                        repository.playlistWithSongs(childIdLong).songs.toSongs()
                    }

                    MediaIDs.GENRES -> {
                        repository.songsByGenre(childIdLong)
                    }

                    else -> {
                        emptyList()
                    }
                }
            }
        }

    private suspend fun getPlayableMediaItems(
        parentId: String,
        childId: String? = null,
    ) = getPlayableSongs(parentId, childId)
        .filterNot { it == Song.emptySong }
        .map { song ->
            song.toAutoMediaItem(
                if (childId.isNullOrEmpty()) parentId else MediaIDs.getPathId(parentId, childId),
            )
        }

    private fun Song.toAutoMediaItem(parent: String? = null): MediaItem = toMediaItem(if (parent.isNullOrEmpty()) id.toString() else MediaIDs.getPathId(parent, id))

    companion object {
        // Internal ID for search requests
        private const val SEARCH = "SEARCH"
    }
}
