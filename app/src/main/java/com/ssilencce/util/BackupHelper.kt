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

package com.ssilencce.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import com.ssilencce.R
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.room.LyricsDao
import com.ssilencce.data.local.room.LyricsEntity
import com.ssilencce.data.local.room.PlaylistEntity
import com.ssilencce.data.mapper.toSongEntity
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.zipOutputStream
import com.ssilencce.extensions.showToast
import com.ssilencce.util.m3u.M3UConstants
import com.ssilencce.util.m3u.M3UWriter
import com.ssilencce.util.m3u.PlaylistExportFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object BackupHelper : KoinComponent {
    private val repository by inject<Repository>()
    private val lyricsDao by inject<LyricsDao>()

    // Backups may come from other builds of the app; unknown fields must not abort a restore.
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createBackup(
        context: Context,
        uri: Uri?,
    ) {
        if (uri == null) return
        when (val result = createBackupFile(context, uri)) {
            BackupCreationResult.Success -> {
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.backup_successful)
                }
            }

            is BackupCreationResult.Failure -> {
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.backup_failed)
                }
            }
        }
    }

    /**
     * Writes the backup archive to [uri] and returns the outcome without showing toasts.
     * Cancellation is rethrown so callers (including WorkManager workers) can clean up.
     */
    suspend fun createBackupFile(
        context: Context,
        uri: Uri,
    ): BackupCreationResult =
        withContext(Dispatchers.IO) {
            try {
                val zipItems = mutableListOf<ZipItem>()
                zipItems.addAll(getPlaylistZipItems(context))
                zipItems.addAll(getSettingsZipItems(context))
                zipItems.addAll(getLyricsZipItems())
                zipItems.addAll(getCustomArtistZipItems(context))
                val outputStream = context.contentResolver.openOutputStream(uri) ?: throw FileNotFoundException()
                zipAll(context.getString(R.string.app_name), zipItems, outputStream)
                BackupCreationResult.Success
            } catch (cancellation: CancellationException) {
                cleanupBackupTarget(context, uri)
                throw cancellation
            } catch (throwable: Throwable) {
                cleanupBackupTarget(context, uri)
                Log.e(TAG, "Failed to create backup", throwable)
                BackupCreationResult.Failure(throwable)
            } finally {
                // Clean Cache Playlist Directory
                File(context.filesDir, PLAYLISTS_PATH).deleteRecursively()
            }
        }

    private suspend fun zipAll(
        comment: String,
        zipItems: List<ZipItem>,
        output: OutputStream,
    ) {
        output.zipOutputStream().use { out ->
            out.setComment(comment)
            for (zipItem in zipItems) {
                currentCoroutineContext().ensureActive()
                if (zipItem.filePath != null) {
                    File(zipItem.filePath).inputStream().buffered().use { origin ->
                        val entry = ZipEntry(zipItem.zipPath)
                        out.putNextEntry(entry)
                        origin.copyToWithCancellation(out)
                        out.closeEntry()
                    }
                } else if (!zipItem.fileContent.isNullOrEmpty()) {
                    val entry = ZipEntry(zipItem.zipPath)
                    out.putNextEntry(entry)
                    // The writer must not close the archive: further entries follow.
                    out.write(zipItem.fileContent.toByteArray(Charsets.UTF_8))
                    out.closeEntry()
                }
            }
        }
    }

    private suspend fun getPlaylistZipItems(context: Context): List<ZipItem> {
        val playlistZipItems = mutableListOf<ZipItem>()
        // Cache Playlist files in App storage
        val playlistFolder = File(context.filesDir, PLAYLISTS_PATH)
        if (!playlistFolder.exists()) {
            playlistFolder.mkdirs()
        }
        for (playlist in repository.playlistsWithSongs()) {
            if (playlist.songs.isEmpty()) {
                // Keep legitimate empty playlist snapshots as a valid M3U header entry.
                val zipPath =
                    PLAYLISTS_PATH.child(
                        "${playlist.playlistEntity.safeBackupPlaylistName()}.${PlaylistExportFormat.M3U.extension}",
                    )
                playlistZipItems.add(ZipItem(zipPath, fileContent = M3UConstants.HEADER))
                continue
            }
            val playlistFile = M3UWriter.writeToDirectory(playlistFolder, playlist)
            if (!playlistFile.isFile) {
                throw IOException("Playlist backup file was not created: ${playlistFile.absolutePath}")
            }
            playlistZipItems.add(
                ZipItem(
                    PLAYLISTS_PATH.child(playlistFile.name),
                    playlistFile.absolutePath,
                ),
            )
        }
        return playlistZipItems
    }

    private fun PlaylistEntity.safeBackupPlaylistName(): String =
        playlistName
            .replace('/', '_')
            .replace('\\', '_')
            .trim()
            .ifBlank { "playlist" }

    private suspend fun getLyricsZipItems(): List<ZipItem> {
        val allLyrics = lyricsDao.getAllLyrics()
        if (allLyrics.isNotEmpty()) {
            return listOf(ZipItem(LYRICS_PATH.child("lyrics.json"), fileContent = json.encodeToString(allLyrics)))
        }
        return emptyList()
    }

    private fun getSettingsZipItems(context: Context): List<ZipItem> {
        val sharedPrefPath = File(context.filesDir.parentFile, "shared_prefs")
        return listOf("${context.packageName}_preferences.xml")
            .map {
                ZipItem(SETTINGS_PATH.child(it), File(sharedPrefPath, it).absolutePath)
            }
    }

    private fun getCustomArtistZipItems(context: Context): List<ZipItem> {
        val zipItemList = mutableListOf<ZipItem>()
        val sharedPrefPath = File(context.filesDir.parentFile, "shared_prefs")
        val customArtistImagesDir = FileUtil.customArtistImagesDirectory()
        if (customArtistImagesDir != null) {
            zipItemList.addAll(
                customArtistImagesDir
                    .listFiles()
                    ?.map {
                        ZipItem(
                            CUSTOM_ARTISTS_PATH.child("custom_artist_images").child(it.name),
                            it.absolutePath,
                        )
                    }?.toList() ?: listOf(),
            )

            File(sharedPrefPath, "custom_artist_images.xml").let {
                if (it.exists()) {
                    zipItemList.add(
                        ZipItem(
                            CUSTOM_ARTISTS_PATH.child("prefs").child("custom_artist_images.xml"),
                            it.absolutePath,
                        ),
                    )
                }
            }
        }
        return zipItemList
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri?,
        contents: List<BackupContent>,
    ) {
        if (uri == null) return
        withContext(Dispatchers.IO) {
            val summary = RestoreSummary(selectionRequested = contents.isNotEmpty())
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    throw FileNotFoundException()
                }
                ZipInputStream(inputStream).use { zipIn ->
                    val entryStream = BoundedEntryInputStream(zipIn)
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        currentCoroutineContext().ensureActive()
                        if (summary.totalEntries >= MAX_BACKUP_ENTRIES) {
                            throw IOException("Backup contains too many entries")
                        }
                        val kind =
                            if (entry.isDirectory) {
                                RestoreEntryKind.Unrecognized
                            } else {
                                entry.restoreEntryKind()
                            }
                        val selected = kind.isSelected(contents)
                        summary.recordEntry(kind, selected)
                        // Declared ZIP sizes are unreliable (streamed entries report -1),
                        // so limits are enforced against the bytes actually read.
                        entryStream.beginEntry(
                            if (kind == RestoreEntryKind.Lyrics) MAX_LYRICS_ENTRY_BYTES else MAX_SINGLE_ENTRY_BYTES,
                        )
                        if (!entry.isDirectory) {
                            when (kind) {
                                RestoreEntryKind.Playlists -> {
                                    if (selected) {
                                        restorePlaylists(entryStream, entry, summary)
                                    }
                                }

                                RestoreEntryKind.Settings -> {
                                    if (selected &&
                                        entry.getFileName() == "${context.packageName}_preferences.xml" &&
                                        restorePreferences(context, entryStream, entry)
                                    ) {
                                        summary.recordImport()
                                    }
                                }

                                RestoreEntryKind.Lyrics -> {
                                    if (selected && restoreLyrics(entryStream)) {
                                        summary.recordImport()
                                    }
                                }

                                RestoreEntryKind.ArtistImages -> {
                                    if (selected) {
                                        if (entry.isCustomArtistPrefEntry() &&
                                            entry.getFileName() == CUSTOM_ARTIST_PREFS_FILE &&
                                            restoreCustomArtistPrefs(context, entryStream, entry)
                                        ) {
                                            summary.recordImport()
                                        } else if (entry.isCustomArtistImageEntry() &&
                                            restoreCustomArtistImages(context, entryStream, entry)
                                        ) {
                                            summary.recordImport()
                                        }
                                    }
                                }

                                RestoreEntryKind.Unrecognized -> {
                                    Unit
                                }
                            }
                        }
                        // Unselected and unrecognized entries are drained through the counting
                        // stream so their actual bytes count towards the limits and cancellation
                        // stays responsive: ZipInputStream.nextEntry would inflate them silently.
                        entryStream.drain()
                        entry = zipIn.nextEntry
                    }
                }
                if (summary.totalEntries == 0) {
                    throw IOException("Backup archive contains no entries")
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to restore backup", throwable)
                withContext(Dispatchers.Main) {
                    context.showToast(R.string.could_not_restore_data)
                }
                return@withContext
            }
            withContext(Dispatchers.Main) {
                summary.showResult(context)
            }
        }
    }

    private suspend fun restoreLyrics(zipIn: InputStream): Boolean {
        val serializedLyrics = zipIn.bufferedReader().readText()
        val lyrics = json.decodeFromString<List<LyricsEntity>>(serializedLyrics)
        if (lyrics.isEmpty()) return false
        lyricsDao.insertLyrics(lyrics)
        return true
    }

    private suspend fun restorePreferences(
        context: Context,
        zipIn: InputStream,
        zipEntry: ZipEntry,
    ): Boolean {
        val file = File(context.filesDir.parentFile, "shared_prefs${File.separator}${zipEntry.getFileName()}")
        return restoreFileAtomically(file, zipIn, zipEntry.name)
    }

    /**
     * Stages one restored file beside its destination and replaces the live file only after a
     * complete, closed, synced write. A failed or cancelled restore leaves the old file intact.
     */
    private suspend fun restoreFileAtomically(
        target: File,
        zipIn: InputStream,
        entryName: String,
    ): Boolean {
        val parent = target.parentFile
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            Log.e(TAG, "Could not prepare restore target for $entryName")
            return false
        }
        val temporary =
            try {
                File.createTempFile(".${target.name}.", ".tmp", parent)
            } catch (error: IOException) {
                Log.e(TAG, "Could not create restore temp for $entryName", error)
                return false
            }
        return try {
            FileOutputStream(temporary).use { fileOut ->
                val buffered = BufferedOutputStream(fileOut)
                zipIn.copyToWithCancellation(buffered)
                buffered.flush()
                fileOut.fd.sync()
            }
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "Failed to restore $entryName; existing file kept", error)
            false
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun cleanupBackupTarget(
        context: Context,
        uri: Uri,
    ) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (error: Exception) {
            Log.e(TAG, "Could not clean partial backup target", error)
        }
    }

    private suspend fun restorePlaylists(
        zipIn: InputStream,
        zipEntry: ZipEntry,
        summary: RestoreSummary,
    ) {
        val playlistName = zipEntry.getFileName().substringBeforeLast(".")
        val songs = mutableListOf<Song>()

        // Get songs from m3u playlist files
        val reader = zipIn.bufferedReader()
        while (true) {
            currentCoroutineContext().ensureActive()
            val line = reader.readLine() ?: break
            if (line.startsWith(File.separator)) {
                if (File(line).exists()) {
                    val song = repository.songByFilePath(line, true)
                    if (song != Song.emptySong) {
                        songs.add(song)
                    }
                }
            }
        }
        val playlistEntity = repository.checkPlaylistExists(playlistName).firstOrNull()
        val playListId =
            if (playlistEntity != null) {
                playlistEntity.playListId
            } else {
                repository.createPlaylist(PlaylistEntity(playlistName = playlistName))
            }
        if (songs.isNotEmpty()) {
            val songEntities =
                songs.map {
                    it.toSongEntity(playListId)
                }
            repository.insertSongsInPlaylist(songEntities)
        }
        // A valid empty playlist still round-trips its entity and counts as an import when created.
        summary.recordPlaylistRestore(
            playlistCreated = playlistEntity == null,
            resolvedSongs = songs.size,
        )
    }

    private suspend fun restoreCustomArtistImages(
        context: Context,
        zipIn: InputStream,
        zipEntry: ZipEntry,
    ): Boolean {
        val parentFolder = File(context.filesDir, "custom_artist_images")
        if (!parentFolder.exists() && !parentFolder.mkdirs()) {
            Log.e(TAG, "Could not create custom artist image directory")
            return false
        }
        val file = File(parentFolder, zipEntry.requireSafeFileName())
        return restoreFileAtomically(file, zipIn, zipEntry.name)
    }

    private suspend fun restoreCustomArtistPrefs(
        context: Context,
        zipIn: InputStream,
        zipEntry: ZipEntry,
    ): Boolean {
        val file = File(context.filesDir.parentFile, "shared_prefs".child(zipEntry.requireSafeFileName()))
        return restoreFileAtomically(file, zipIn, zipEntry.name)
    }

    private suspend fun InputStream.copyToWithCancellation(out: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = read(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
        }
    }

    private suspend fun InputStream.drain() {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (read(buffer) != -1) {
            currentCoroutineContext().ensureActive()
        }
    }

    /**
     * Counts the bytes actually read from the current ZIP entry and enforces per-entry and
     * total limits while reading. Streamed ZIP entries declare no usable size (the declared
     * size is -1), so limits cannot be validated from the entry header.
     */
    private class BoundedEntryInputStream(
        private val delegate: ZipInputStream,
    ) : InputStream() {
        private var entryLimitBytes = Long.MAX_VALUE
        private var entryBytes = 0L
        private var totalBytes = 0L

        fun beginEntry(limitBytes: Long) {
            entryLimitBytes = limitBytes
            entryBytes = 0L
        }

        override fun read(): Int {
            val value = delegate.read()
            if (value != -1) {
                accountRead(1)
            }
            return value
        }

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
            val read = delegate.read(b, off, len)
            if (read > 0) {
                accountRead(read)
            }
            return read
        }

        private fun accountRead(bytes: Int) {
            entryBytes += bytes
            totalBytes += bytes
            if (entryBytes > entryLimitBytes) {
                throw IOException("Backup entry exceeds its size limit")
            }
            if (totalBytes > MAX_BACKUP_UNCOMPRESSED_BYTES) {
                throw IOException("Backup exceeds total size limit")
            }
        }

        // The ZipInputStream is owned by the caller; this counting view must not close it.
        override fun close() = Unit
    }

    private fun ZipEntry.requireSafeFileName(): String {
        val fileName = getFileName()
        if (fileName.isEmpty() ||
            fileName == "." ||
            fileName == ".." ||
            fileName.contains('/') ||
            fileName.contains('\\')
        ) {
            throw IOException("Backup entry has an unsafe file name")
        }
        return fileName
    }

    const val BACKUP_EXTENSION = "bmgbak"
    const val APPEND_EXTENSION = ".$BACKUP_EXTENSION"
    private const val TAG = "BackupHelper"
    private const val PLAYLISTS_PATH = "Playlists"
    private const val SETTINGS_PATH = "prefs"
    private const val LYRICS_PATH = "lyrics"
    private const val CUSTOM_ARTISTS_PATH = "artistImages"
    private const val CUSTOM_ARTIST_PREFS_FILE = "custom_artist_images.xml"
    private const val MAX_BACKUP_ENTRIES = 1000
    private const val MAX_BACKUP_UNCOMPRESSED_BYTES = 100L * 1024 * 1024
    private const val MAX_SINGLE_ENTRY_BYTES = MAX_BACKUP_UNCOMPRESSED_BYTES
    private const val MAX_LYRICS_ENTRY_BYTES = 10L * 1024 * 1024

    private fun ZipEntry.isPlaylistEntry(): Boolean = hasPrefix(PLAYLISTS_PATH)

    private fun ZipEntry.isPreferenceEntry(): Boolean = hasPrefix(SETTINGS_PATH)

    private fun ZipEntry.isLyricsEntry(): Boolean = hasPrefix(LYRICS_PATH)

    private fun ZipEntry.isCustomArtistEntry(): Boolean = hasPrefix(CUSTOM_ARTISTS_PATH)

    private fun ZipEntry.isCustomArtistImageEntry(): Boolean = hasPrefix(CUSTOM_ARTISTS_PATH) && name.contains("custom_artist_images")

    private fun ZipEntry.isCustomArtistPrefEntry(): Boolean = hasPrefix(CUSTOM_ARTISTS_PATH) && name.contains("prefs")

    private fun ZipEntry.hasPrefix(prefix: String): Boolean = name == prefix || name.startsWith("$prefix/")

    private fun ZipEntry.getFileName(): String = name.substring(name.lastIndexOf(File.separator) + 1)

    private fun ZipEntry.restoreEntryKind(): RestoreEntryKind =
        when {
            isPlaylistEntry() -> {
                RestoreEntryKind.Playlists
            }

            isPreferenceEntry() -> {
                RestoreEntryKind.Settings
            }

            isLyricsEntry() -> {
                RestoreEntryKind.Lyrics
            }

            isCustomArtistEntry() && (isCustomArtistPrefEntry() || isCustomArtistImageEntry()) -> {
                RestoreEntryKind.ArtistImages
            }

            else -> {
                RestoreEntryKind.Unrecognized
            }
        }

    private fun RestoreEntryKind.isSelected(contents: List<BackupContent>): Boolean =
        when (this) {
            RestoreEntryKind.Settings -> BackupContent.Settings in contents
            RestoreEntryKind.Lyrics -> BackupContent.Lyrics in contents
            RestoreEntryKind.ArtistImages -> BackupContent.ArtistImages in contents
            RestoreEntryKind.Playlists -> BackupContent.Playlists in contents
            RestoreEntryKind.Unrecognized -> false
        }

    private fun RestoreSummary.showResult(context: Context) {
        when (outcome()) {
            RestoreOutcome.Success,
            RestoreOutcome.SuccessWithCount,
            -> {
                context.showToast(R.string.data_restored_successfully)
            }

            RestoreOutcome.NothingSelected -> {
                context.showToast(R.string.restore_nothing_selected)
            }

            RestoreOutcome.NoRecognizedContent -> {
                context.showToast(R.string.restore_no_recognized_content)
            }

            RestoreOutcome.NothingImported -> {
                context.showToast(R.string.restore_nothing_restored)
            }
        }
    }
}

sealed interface BackupCreationResult {
    data object Success : BackupCreationResult

    data class Failure(
        val throwable: Throwable,
    ) : BackupCreationResult
}

data class ZipItem(
    val zipPath: String,
    val filePath: String? = null,
    val fileContent: String? = null,
)

fun String.child(child: String): String = this + File.separator + child

enum class BackupContent(
    @StringRes val titleRes: Int,
) {
    Settings(R.string.backup_settings),
    Lyrics(R.string.backup_synced_lyrics),
    ArtistImages(R.string.backup_artist_images),
    Playlists(R.string.backup_playlists),
}
