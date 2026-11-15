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
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.room.LyricsDao
import com.ssilencce.data.local.room.LyricsEntity
import com.ssilencce.data.local.room.NamedQueueDao
import com.ssilencce.data.local.room.NamedQueueEntity
import com.ssilencce.data.local.room.NamedQueueItemEntity
import com.ssilencce.data.local.room.PlayCountDao
import com.ssilencce.data.local.room.PlaylistEntity
import com.ssilencce.data.local.room.TrackPositionDao
import com.ssilencce.data.mapper.toSongEntity
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.zipOutputStream
import com.ssilencce.extensions.showToast
import com.ssilencce.util.backup.BackupArchivePolicy
import com.ssilencce.util.backup.NamedQueueBackupEntry
import com.ssilencce.util.backup.PlayCountBackupEntry
import com.ssilencce.util.backup.TrackPositionBackupEntry
import com.ssilencce.util.backup.toBackupEntry
import com.ssilencce.util.backup.toEntity
import com.ssilencce.util.m3u.M3UConstants
import com.ssilencce.util.m3u.M3UWriter
import com.ssilencce.util.m3u.PlaylistExportFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FilterInputStream
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
    private val playCountDao by inject<PlayCountDao>()
    private val trackPositionDao by inject<TrackPositionDao>()
    private val namedQueueDao by inject<NamedQueueDao>()

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
                zipItems.addAll(getFavoriteZipItems(context))
                zipItems.addAll(getPlayCountZipItems())
                zipItems.addAll(getNamedQueueZipItems())
                zipItems.addAll(getTrackPositionZipItems())
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
                // Clean cache playlist and favorites directories.
                File(context.filesDir, PLAYLISTS_PATH).deleteRecursively()
                File(context.filesDir, FAVORITES_PATH).deleteRecursively()
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
        val favoritesName = context.getString(R.string.favorites_label)
        for (playlist in repository.playlistsWithSongs()) {
            if (BackupArchivePolicy.isFavoritesPlaylist(playlist.playlistEntity.playlistName, favoritesName)) {
                continue
            }
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

    private suspend fun getFavoriteZipItems(context: Context): List<ZipItem> {
        val favoritesName = context.getString(R.string.favorites_label)
        val favorites =
            repository.playlistsWithSongs().firstOrNull {
                BackupArchivePolicy.isFavoritesPlaylist(it.playlistEntity.playlistName, favoritesName)
            } ?: return emptyList()
        if (favorites.songs.isEmpty()) {
            return listOf(
                ZipItem(FAVORITES_PATH.child(FAVORITES_FILE), fileContent = M3UConstants.HEADER),
            )
        }
        val favoritesFolder = File(context.filesDir, FAVORITES_PATH)
        if (!favoritesFolder.exists() && !favoritesFolder.mkdirs()) {
            throw IOException("Could not create favorites backup directory")
        }
        val favoritesFile = M3UWriter.writeToDirectory(favoritesFolder, favorites)
        if (!favoritesFile.isFile) {
            throw IOException("Favorites backup file was not created: ${favoritesFile.absolutePath}")
        }
        return listOf(ZipItem(FAVORITES_PATH.child(FAVORITES_FILE), favoritesFile.absolutePath))
    }

    private suspend fun getPlayCountZipItems(): List<ZipItem> {
        val playCounts = playCountDao.allPlayCounts()
        if (playCounts.isEmpty()) return emptyList()
        val serialized = json.encodeToString(playCounts.map { it.toBackupEntry() })
        return listOf(ZipItem(PLAY_COUNTS_PATH.child(PLAY_COUNTS_FILE), fileContent = serialized))
    }

    private suspend fun getNamedQueueZipItems(): List<ZipItem> {
        val queues = namedQueueDao.queuesWithItems()
        if (queues.isEmpty()) return emptyList()
        val serialized = json.encodeToString(queues.map { it.toBackupEntry() })
        return listOf(ZipItem(NAMED_QUEUES_PATH.child(NAMED_QUEUES_FILE), fileContent = serialized))
    }

    private suspend fun getTrackPositionZipItems(): List<ZipItem> {
        val positions = trackPositionDao.all()
        if (positions.isEmpty()) return emptyList()
        val serialized = json.encodeToString(positions.map { it.toBackupEntry() })
        return listOf(ZipItem(TRACK_POSITIONS_PATH.child(TRACK_POSITIONS_FILE), fileContent = serialized))
    }

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
    ): Boolean {
        if (uri == null) return false
        return withContext(Dispatchers.IO) {
            val summary = RestoreSummary(selectionRequested = contents.isNotEmpty())
            var settingsRestored = false
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
                        val entryLimit =
                            when (kind) {
                                RestoreEntryKind.Lyrics,
                                RestoreEntryKind.PlayCounts,
                                RestoreEntryKind.Queues,
                                RestoreEntryKind.PerFileSettings,
                                -> MAX_JSON_ENTRY_BYTES

                                else -> MAX_SINGLE_ENTRY_BYTES
                            }
                        entryStream.beginEntry(entryLimit)
                        if (!entry.isDirectory) {
                            when (kind) {
                                RestoreEntryKind.Playlists -> {
                                    if (selected) {
                                        if (BackupArchivePolicy.isLegacyFavoritesEntry(entry.name)) {
                                            // Old archives stored Favorites under Playlists/; merge
                                            // it into the current localized favorites playlist.
                                            summary.recordRestoreResult(restoreFavorites(entryStream))
                                        } else {
                                            restorePlaylists(entryStream, entry, summary)
                                        }
                                    }
                                }

                                RestoreEntryKind.Favorites -> {
                                    if (selected) {
                                        summary.recordRestoreResult(restoreFavorites(entryStream))
                                    }
                                }

                                RestoreEntryKind.PlayCounts -> {
                                    if (selected) {
                                        summary.recordRestoreResult(restorePlayCounts(entryStream))
                                    }
                                }

                                RestoreEntryKind.Queues -> {
                                    if (selected) {
                                        summary.recordRestoreResult(restoreNamedQueues(entryStream))
                                    }
                                }

                                RestoreEntryKind.PerFileSettings -> {
                                    if (selected) {
                                        summary.recordRestoreResult(restoreTrackPositions(entryStream))
                                    }
                                }

                                RestoreEntryKind.Settings -> {
                                    if (selected) {
                                        val imported =
                                            entry.getFileName() == "${context.packageName}_preferences.xml" &&
                                                restorePreferences(context, entryStream)
                                        if (imported) {
                                            summary.recordImport()
                                            settingsRestored = true
                                        } else {
                                            summary.recordFailure()
                                        }
                                    }
                                }

                                RestoreEntryKind.Lyrics -> {
                                    // An empty lyrics cache is a valid backup no-op, not a failure.
                                    if (selected && restoreLyrics(entryStream)) {
                                        summary.recordImport()
                                    }
                                }

                                RestoreEntryKind.ArtistImages -> {
                                    if (selected) {
                                        val imported =
                                            if (BackupArchivePolicy.isCustomArtistPreferenceEntry(entry.name) &&
                                                entry.getFileName() == CUSTOM_ARTIST_PREFS_FILE
                                            ) {
                                                restoreCustomArtistPrefs(context, entryStream, entry)
                                            } else if (BackupArchivePolicy.isCustomArtistImageEntry(entry.name)) {
                                                restoreCustomArtistImages(context, entryStream, entry)
                                            } else {
                                                false
                                            }
                                        if (imported) {
                                            summary.recordImport()
                                        } else {
                                            summary.recordFailure()
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
                return@withContext settingsRestored
            }
            withContext(Dispatchers.Main) {
                summary.showResult(context)
            }
            settingsRestored
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
    ): Boolean {
        val values = parsePreferencesXml(zipIn.withMaxBytes(MAX_SETTINGS_ENTRY_BYTES)) ?: return false
        currentCoroutineContext().ensureActive()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val previous = preferences.all
        val editor = preferences.edit().clear()
        if (!putPreferenceValues(editor, values)) {
            return false
        }
        currentCoroutineContext().ensureActive()
        val persisted =
            try {
                editor.commit()
            } catch (error: Exception) {
                Log.e(TAG, "Could not commit restored preferences", error)
                false
            }
        if (persisted) {
            return true
        }
        Log.e(TAG, "Restored preferences were not persisted; rolling back live values")
        restorePreferenceSnapshot(preferences, previous)
        return false
    }

    private fun InputStream.withMaxBytes(maxBytes: Long): InputStream =
        object : FilterInputStream(this) {
            private var bytesRead = 0L

            override fun read(): Int {
                if (bytesRead > maxBytes) throw IOException("Settings entry exceeds limit")
                val value = super.read()
                if (value >= 0) {
                    bytesRead++
                    if (bytesRead > maxBytes) throw IOException("Settings entry exceeds limit")
                }
                return value
            }

            override fun read(
                b: ByteArray,
                off: Int,
                len: Int,
            ): Int {
                if (bytesRead > maxBytes) throw IOException("Settings entry exceeds limit")
                val allowed = minOf(len.toLong(), maxBytes - bytesRead + 1).toInt()
                val read = super.read(b, off, allowed)
                if (read > 0) {
                    bytesRead += read
                    if (bytesRead > maxBytes) throw IOException("Settings entry exceeds limit")
                }
                return read
            }
        }

    /**
     * Parses the untrusted SharedPreferences XML completely before any preference mutation.
     * Only the five SharedPreferences value types are accepted; unknown tags, duplicate keys,
     * empty names, malformed values and trailing content reject the whole entry.
     */
    private suspend fun parsePreferencesXml(zipIn: InputStream): Map<String, Any>? {
        try {
            // KXmlParser defaults to not processing DOCDECL; do not request the
            // implementation-specific DOCDECL feature (some parsers reject it).
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(zipIn, null)
            var prologEvent = parser.nextToken()
            while (prologEvent != XmlPullParser.START_TAG) {
                currentCoroutineContext().ensureActive()
                if (prologEvent == XmlPullParser.DOCDECL ||
                    prologEvent == XmlPullParser.END_DOCUMENT
                ) {
                    return null
                }
                prologEvent = parser.nextToken()
            }
            if (parser.name != "map") return null
            val values = LinkedHashMap<String, Any>()
            while (true) {
                currentCoroutineContext().ensureActive()
                when (parser.next()) {
                    XmlPullParser.START_TAG -> {
                        if (parser.depth != 2) return null
                        val name = parser.getAttributeValue(null, "name") ?: return null
                        if (name.isEmpty() ||
                            name.length > MAX_PREFERENCE_KEY_CHARS ||
                            values.size >= MAX_PREFERENCE_KEYS ||
                            values.containsKey(name)
                        ) {
                            return null
                        }
                        when (parser.name) {
                            "string" -> {
                                val text = parser.nextText()
                                if (text.length > MAX_PREFERENCE_VALUE_CHARS) return null
                                values[name] = text
                            }

                            "int" -> {
                                val raw = parser.getAttributeValue(null, "value") ?: return null
                                values[name] = raw.toIntOrNull() ?: return null
                            }

                            "long" -> {
                                val raw = parser.getAttributeValue(null, "value") ?: return null
                                values[name] = raw.toLongOrNull() ?: return null
                            }

                            "float" -> {
                                val raw = parser.getAttributeValue(null, "value") ?: return null
                                values[name] = raw.toFloatOrNull() ?: return null
                            }

                            "boolean" -> {
                                val raw = parser.getAttributeValue(null, "value") ?: return null
                                values[name] =
                                    when (raw) {
                                        "true" -> true
                                        "false" -> false
                                        else -> return null
                                    }
                            }

                            "set" -> {
                                val set = LinkedHashSet<String>()
                                var event = parser.next()
                                while (event != XmlPullParser.END_TAG || parser.depth > 2) {
                                    currentCoroutineContext().ensureActive()
                                    when (event) {
                                        XmlPullParser.START_TAG -> {
                                            if (parser.depth != 3 || parser.name != "string") {
                                                return null
                                            }
                                            val text = parser.nextText()
                                            if (text.length > MAX_PREFERENCE_VALUE_CHARS ||
                                                set.size >= MAX_PREFERENCE_SET_VALUES
                                            ) {
                                                return null
                                            }
                                            set.add(text)
                                        }

                                        XmlPullParser.DOCDECL,
                                        XmlPullParser.END_DOCUMENT,
                                        -> {
                                            return null
                                        }

                                        XmlPullParser.TEXT -> {
                                            if (!parser.isWhitespace) return null
                                        }

                                        else -> {
                                            Unit
                                        }
                                    }
                                    event = parser.next()
                                }
                                values[name] = set
                            }

                            else -> {
                                return null
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.depth == 1) {
                            if (!parser.consumeDocumentEnd()) return null
                            return values
                        }
                        if (parser.depth != 2) return null
                    }

                    XmlPullParser.DOCDECL,
                    XmlPullParser.END_DOCUMENT,
                    -> {
                        return null
                    }

                    XmlPullParser.TEXT -> {
                        if (!parser.isWhitespace) return null
                    }

                    else -> {
                        Unit
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "Invalid preferences XML", error)
            return null
        }
    }

    private fun putPreferenceValues(
        editor: SharedPreferences.Editor,
        values: Map<String, *>,
    ): Boolean {
        for ((key, value) in values) {
            when (value) {
                is String -> {
                    editor.putString(key, value)
                }

                is Int -> {
                    editor.putInt(key, value)
                }

                is Long -> {
                    editor.putLong(key, value)
                }

                is Float -> {
                    editor.putFloat(key, value)
                }

                is Boolean -> {
                    editor.putBoolean(key, value)
                }

                is Set<*> -> {
                    if (value.any { it !is String }) return false
                    editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }

                else -> {
                    return false
                }
            }
        }
        return true
    }

    private fun restorePreferenceSnapshot(
        preferences: SharedPreferences,
        snapshot: Map<String, *>,
    ) {
        val editor = preferences.edit().clear()
        if (!putPreferenceValues(editor, snapshot)) {
            Log.e(TAG, "Could not restore live preference snapshot")
            return
        }
        val restored =
            try {
                editor.commit()
            } catch (error: Exception) {
                Log.e(TAG, "Could not commit live preference rollback", error)
                false
            }
        if (!restored) {
            Log.e(TAG, "Live preference rollback was not persisted")
        }
    }

    private suspend fun XmlPullParser.consumeDocumentEnd(): Boolean {
        var event = next()
        while (event != XmlPullParser.END_DOCUMENT) {
            currentCoroutineContext().ensureActive()
            if (event == XmlPullParser.START_TAG) return false
            if (event == XmlPullParser.TEXT && !isWhitespace) return false
            event = next()
        }
        return true
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
        val songs = readM3uSongs(zipIn)
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
        // A valid empty playlist is a no-op when it already exists; it is not a failure.
        summary.recordPlaylistRestore(
            playlistCreated = playlistEntity == null,
            resolvedSongs = songs.size,
        )
    }

    private suspend fun readM3uSongs(zipIn: InputStream): List<Song> {
        val songs = mutableListOf<Song>()
        val reader = zipIn.bufferedReader()
        while (true) {
            currentCoroutineContext().ensureActive()
            val line = reader.readLine() ?: break
            if (line.startsWith(File.separator) && File(line).exists()) {
                val song = repository.songByFilePath(line, true)
                if (song != Song.emptySong) {
                    songs.add(song)
                }
            }
        }
        return songs
    }

    private suspend fun restoreFavorites(zipIn: InputStream): RestoreApplyResult {
        val songs = readM3uSongs(zipIn)
        val existingFavorites = repository.checkFavoritePlaylist()
        if (existingFavorites == null && songs.isEmpty()) return RestoreApplyResult()
        val favorites = existingFavorites ?: repository.favoritePlaylist()
        val existingIds = repository.findSongsInFavorites(songs).map { it.id }.toSet()
        val newSongs = songs.distinctBy { it.id }.filterNot { it.id in existingIds }
        if (newSongs.isNotEmpty()) {
            repository.insertSongsInPlaylist(newSongs.map { it.toSongEntity(favorites.playListId) })
        }
        return if (existingFavorites == null || newSongs.isNotEmpty()) {
            RestoreApplyResult(imported = true)
        } else {
            RestoreApplyResult()
        }
    }

    private suspend fun restorePlayCounts(zipIn: InputStream): RestoreApplyResult {
        val entries =
            try {
                json.decodeFromString<List<PlayCountBackupEntry>>(zipIn.bufferedReader().readText())
            } catch (error: SerializationException) {
                Log.e(TAG, "Invalid play-count backup entry", error)
                return RestoreApplyResult(failed = true)
            }
        if (entries.isEmpty()) return RestoreApplyResult()
        val existing = playCountDao.allPlayCounts().associateBy { it.id }
        val merged =
            entries.distinctBy { it.id }.map { entry ->
                val current = existing[entry.id]
                val counters =
                    BackupArchivePolicy.mergePlayCounts(
                        current =
                            current?.let {
                                BackupArchivePolicy.PlayCountCounters(
                                    timePlayed = it.timePlayed,
                                    playCount = it.playCount,
                                    skipCount = it.skipCount,
                                )
                            },
                        incoming =
                            BackupArchivePolicy.PlayCountCounters(
                                timePlayed = entry.timePlayed,
                                playCount = entry.playCount,
                                skipCount = entry.skipCount,
                            ),
                    )
                if (current == null) {
                    entry.toEntity()
                } else {
                    current.copy(
                        timePlayed = counters.timePlayed,
                        playCount = counters.playCount,
                        skipCount = counters.skipCount,
                    )
                }
            }
        playCountDao.upsertSongsInPlayCount(merged)
        return RestoreApplyResult(imported = true)
    }

    private suspend fun restoreNamedQueues(zipIn: InputStream): RestoreApplyResult {
        val entries =
            try {
                json.decodeFromString<List<NamedQueueBackupEntry>>(zipIn.bufferedReader().readText())
            } catch (error: SerializationException) {
                Log.e(TAG, "Invalid named-queue backup entry", error)
                return RestoreApplyResult(failed = true)
            }
        if (entries.isEmpty()) return RestoreApplyResult()
        val namesById =
            namedQueueDao
                .queuesWithItems()
                .associate { it.queue.id to it.queue.name }
                .toMutableMap()
        var imported = false
        var failed = false
        for (entry in entries) {
            val plan =
                BackupArchivePolicy.planNamedQueueRestore(
                    id = entry.id,
                    requestedName = entry.name,
                    namesById = namesById,
                )
            if (plan == null) {
                Log.w(TAG, "Skipping named queue restore: invalid id or queue limit reached")
                failed = true
                continue
            }
            val queue =
                NamedQueueEntity(
                    id = plan.id,
                    name = plan.name,
                    createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt,
                    resumeIndex = entry.resumeIndex,
                    resumePositionMs = entry.resumePositionMs,
                    repeatMode = entry.repeatMode,
                    shuffleEnabled = entry.shuffleEnabled,
                )
            val items =
                entry.mediaIds.mapIndexed { index, mediaId ->
                    NamedQueueItemEntity(queueId = plan.id, order = index, mediaId = mediaId)
                }
            try {
                namedQueueDao.saveQueue(queue, items)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Log.e(TAG, "Could not restore named queue ${entry.id}", error)
                failed = true
                continue
            }
            namesById[plan.id] = plan.name
            imported = true
        }
        return RestoreApplyResult(imported = imported, failed = failed)
    }

    private suspend fun restoreTrackPositions(zipIn: InputStream): RestoreApplyResult {
        val entries =
            try {
                json.decodeFromString<List<TrackPositionBackupEntry>>(zipIn.bufferedReader().readText())
            } catch (error: SerializationException) {
                Log.e(TAG, "Invalid track-position backup entry", error)
                return RestoreApplyResult(failed = true)
            }
        if (entries.isEmpty()) return RestoreApplyResult()
        val existing = trackPositionDao.all().associateBy { it.mediaId }
        val positions =
            entries
                .distinctBy { it.mediaId }
                .mapNotNull { entry ->
                    val shouldApply =
                        BackupArchivePolicy.shouldApplyTrackPosition(
                            currentUpdatedAt = existing[entry.mediaId]?.updatedAt,
                            incomingUpdatedAt = entry.updatedAt,
                        )
                    if (shouldApply) entry.toEntity() else null
                }
        if (positions.isEmpty()) return RestoreApplyResult()
        trackPositionDao.upsertAll(positions)
        return RestoreApplyResult(imported = true)
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
    private const val PLAYLISTS_PATH = BackupArchivePolicy.PLAYLISTS_PATH
    private const val FAVORITES_PATH = BackupArchivePolicy.FAVORITES_PATH
    private const val FAVORITES_FILE = BackupArchivePolicy.FAVORITES_FILE
    private const val SETTINGS_PATH = BackupArchivePolicy.SETTINGS_PATH
    private const val LYRICS_PATH = BackupArchivePolicy.LYRICS_PATH
    private const val CUSTOM_ARTISTS_PATH = BackupArchivePolicy.CUSTOM_ARTISTS_PATH
    private const val CUSTOM_ARTIST_PREFS_FILE = BackupArchivePolicy.CUSTOM_ARTIST_PREFS_FILE
    private const val PLAY_COUNTS_PATH = BackupArchivePolicy.PLAY_COUNTS_PATH
    private const val PLAY_COUNTS_FILE = BackupArchivePolicy.PLAY_COUNTS_FILE
    private const val NAMED_QUEUES_PATH = BackupArchivePolicy.NAMED_QUEUES_PATH
    private const val NAMED_QUEUES_FILE = BackupArchivePolicy.NAMED_QUEUES_FILE
    private const val TRACK_POSITIONS_PATH = BackupArchivePolicy.TRACK_POSITIONS_PATH
    private const val TRACK_POSITIONS_FILE = BackupArchivePolicy.TRACK_POSITIONS_FILE
    private const val MAX_BACKUP_ENTRIES = 1000
    private const val MAX_BACKUP_UNCOMPRESSED_BYTES = 100L * 1024 * 1024
    private const val MAX_SINGLE_ENTRY_BYTES = MAX_BACKUP_UNCOMPRESSED_BYTES
    private const val MAX_JSON_ENTRY_BYTES = 10L * 1024 * 1024
    private const val MAX_SETTINGS_ENTRY_BYTES = 1L * 1024 * 1024
    private const val MAX_PREFERENCE_KEYS = 1000
    private const val MAX_PREFERENCE_KEY_CHARS = 256
    private const val MAX_PREFERENCE_VALUE_CHARS = 64 * 1024
    private const val MAX_PREFERENCE_SET_VALUES = 4096

    private fun ZipEntry.getFileName(): String = name.substring(name.lastIndexOf(File.separator) + 1)

    private fun ZipEntry.restoreEntryKind(): RestoreEntryKind = BackupArchivePolicy.entryKind(name, isDirectory)

    private fun RestoreEntryKind.isSelected(contents: List<BackupContent>): Boolean =
        when (this) {
            RestoreEntryKind.Settings -> BackupContent.Settings in contents
            RestoreEntryKind.Lyrics -> BackupContent.Lyrics in contents
            RestoreEntryKind.ArtistImages -> BackupContent.ArtistImages in contents
            RestoreEntryKind.Playlists -> BackupContent.Playlists in contents
            RestoreEntryKind.Favorites -> BackupContent.Favorites in contents
            RestoreEntryKind.PlayCounts -> BackupContent.PlayCounts in contents
            RestoreEntryKind.Queues -> BackupContent.Queues in contents
            RestoreEntryKind.PerFileSettings -> BackupContent.PerFileSettings in contents
            RestoreEntryKind.Unrecognized -> false
        }

    private fun RestoreSummary.recordRestoreResult(result: RestoreApplyResult) {
        if (result.imported) recordImport()
        if (result.failed) recordFailure()
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

            RestoreOutcome.Partial -> {
                context.showToast(R.string.restore_partial_success)
            }
        }
    }
}

private data class RestoreApplyResult(
    val imported: Boolean = false,
    val failed: Boolean = false,
)

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
    Playlists(R.string.backup_playlists),
    Favorites(R.string.backup_favorites),
    PlayCounts(R.string.backup_play_counts),
    Queues(R.string.backup_queues),
    PerFileSettings(R.string.backup_per_file_settings),
    Lyrics(R.string.backup_synced_lyrics),
    ArtistImages(R.string.backup_artist_images),
}
