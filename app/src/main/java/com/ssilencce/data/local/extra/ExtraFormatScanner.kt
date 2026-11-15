/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.ssilencce.data.local.extra

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.ssilencce.data.local.MetadataReader
import com.ssilencce.data.local.room.InclExclDao
import com.ssilencce.extensions.hasT
import com.ssilencce.playback.extractor.ExtraFormatKind
import com.ssilencce.playback.extractor.ExtraFormatSupport
import com.ssilencce.util.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Finds audio files that MediaStore does not index (DSF, DSDIFF and AIFF on most devices) inside
 * the folders that already form the user's library.
 *
 * The scan is opt-in, runs on Dispatchers.IO and only reads tags/headers; audio data is never
 * decoded here. Results are cached in [ExtraFormatLibrary] so list queries never touch the disk.
 */
object ExtraFormatScanner : KoinComponent {
    private const val TAG = "ExtraFormatScanner"
    private const val MAX_DEPTH = 8
    private const val MAX_FILES = 20_000
    private const val MAX_KNOWN_PATHS = 200_000

    private val context: Context by inject()
    private val inclExclDao: InclExclDao by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanMutex = Mutex()
    private var scheduledJob: Job? = null

    fun scanAsync(onFinished: ((Int) -> Unit)? = null) {
        val appContext = context.applicationContext
        scope.launch {
            val count =
                try {
                    scan(appContext)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Extra-format scan failed", e)
                    0
                }
            onFinished?.let { withContext(Dispatchers.Main) { it(count) } }
        }
    }

    fun scheduleIfNeeded() {
        if (!Preferences.extraFormatsScanEnabled) return
        if (!hasScanPermission(context)) return
        if (System.currentTimeMillis() - ExtraFormatLibrary.cacheLastModified(context) < FRESH_AFTER_MS) return
        scheduledJob?.cancel()
        scheduledJob =
            scope.launch {
                delay(STARTUP_DELAY_MS)
                if (!Preferences.extraFormatsScanEnabled) return@launch
                runCatching { scan(context) }
                    .onFailure { Log.e(TAG, "Scheduled extra-format scan failed", it) }
            }
    }

    suspend fun scan(context: Context): Int =
        scanMutex.withLock {
            withContext(Dispatchers.IO) {
                if (!Preferences.extraFormatsScanEnabled) {
                    ExtraFormatLibrary.update(context, emptyList())
                    return@withContext 0
                }
                if (!hasScanPermission(context)) return@withContext ExtraFormatLibrary.songs(context).size
                val knownPaths = mediaStorePaths(context)
                val roots = scanRoots(context)
                val blacklist =
                    if (Preferences.blacklistEnabled) {
                        inclExclDao.blackListPaths().map { it.path }
                    } else {
                        emptyList()
                    }
                val discovered = ArrayList<ExtraFormatTrack>()
                var visitedEntries = 0
                for (root in roots) {
                    if (visitedEntries >= MAX_FILES) break
                    val queue = ArrayDeque<Pair<File, Int>>()
                    queue.add(root to 0)
                    while (queue.isNotEmpty() && visitedEntries < MAX_FILES) {
                        ensureActive()
                        val (directory, depth) = queue.removeFirst()
                        val children = runCatching { directory.listFiles() }.getOrNull() ?: continue
                        for (child in children) {
                            if (visitedEntries >= MAX_FILES) break
                            visitedEntries++
                            if (child.isDirectory) {
                                if (depth >= MAX_DEPTH) continue
                                val name = child.name
                                if (name.startsWith('.') || name.equals("Android", true)) continue
                                if (isBlacklisted(child.path, blacklist)) continue
                                queue.add(child to depth + 1)
                                continue
                            }
                            if (!child.isFile || child.length() <= 0) continue
                            val extension = child.extension.lowercase()
                            if (extension !in ExtraFormatSupport.scannableExtensions) continue
                            val path = child.path
                            if (path in knownPaths) continue
                            if (isBlacklisted(path, blacklist)) continue
                            readTrack(child, extension)?.let { discovered.add(it) }
                        }
                    }
                }
                val sorted =
                    discovered
                        .distinctBy { Triple(it.path, it.size, it.dateModifiedSeconds) }
                        .sortedBy { it.path }
                ExtraFormatLibrary.update(context, sorted)
                Log.i(TAG, "Indexed ${sorted.size} extra-format file(s)")
                sorted.size
            }
        }

    fun hasScanPermission(context: Context): Boolean =
        if (hasT()) {
            ContextCompat.checkSelfPermission(context, READ_MEDIA_AUDIO) == PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
        }

    private fun readTrack(
        file: File,
        extension: String,
    ): ExtraFormatTrack? {
        val kind = ExtraFormatKind.entries.firstOrNull { extension in it.extensions } ?: return null
        if (!ExtraFormatSupport.isPlayable(kind)) return null
        val metadata = runCatching { MetadataReader(Uri.fromFile(file)) }.getOrNull()
        val duration = metadata?.durationMs() ?: ExtraFormatFileProbe.durationMs(file, kind) ?: return null
        if (duration <= 0) return null
        val minDuration = Preferences.minimumSongDuration * 1000L
        val maxDuration = Preferences.maximumSongDuration * 1000L
        if (duration < minDuration) return null
        if (maxDuration > 0 && duration > maxDuration) return null
        val storedPath = ExtraFormatTrackIds.canonicalPath(file.path)

        val title =
            metadata?.first(MetadataReader.TITLE)?.trim()?.takeIf { it.isNotEmpty() }
                ?: file.nameWithoutExtension
        val artist = metadata?.merge(MetadataReader.ARTIST)?.trim().orEmpty()
        val album = metadata?.first(MetadataReader.ALBUM)?.trim().orEmpty()
        val albumArtist = metadata?.first(MetadataReader.ALBUM_ARTIST)?.trim()?.takeIf { it.isNotEmpty() }
        val genre = metadata?.genre()?.takeIf { it.isNotBlank() }
        val composer = metadata?.merge(MetadataReader.COMPOSER)?.trim()?.takeIf { it.isNotEmpty() }
        val trackNumber = metadata?.value(MetadataReader.TRACK_NUMBER)?.leadingNumber() ?: 0
        val discNumber = metadata?.value(MetadataReader.DISC_NUMBER)?.leadingNumber() ?: 0
        val year = metadata?.first(MetadataReader.YEAR)?.take(4)?.toIntOrNull() ?: 0
        val modified = file.lastModified().coerceAtLeast(0) / 1000

        return ExtraFormatTrack(
            id = ExtraFormatTrackIds.forPath(storedPath),
            path = storedPath,
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            composer = composer,
            trackNumber = trackNumber,
            discNumber = discNumber,
            year = year,
            durationMs = duration,
            size = file.length(),
            dateAddedSeconds = modified,
            dateModifiedSeconds = modified,
            sampleRate = metadata?.sampleRateHz() ?: 0,
            channels = metadata?.channelCount() ?: 0,
        )
    }

    private fun mediaStorePaths(context: Context): Set<String> {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val paths = HashSet<String>()
        runCatching {
            context.contentResolver
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext() && paths.size < MAX_KNOWN_PATHS) {
                        cursor.getString(0)?.let { paths.add(it) }
                    }
                }
        }.onFailure { Log.w(TAG, "Could not read MediaStore paths", it) }
        return paths
    }

    private fun scanRoots(context: Context): List<File> {
        val configured =
            if (Preferences.whitelistEnabled) {
                runCatching { inclExclDao.whitelistPaths().map { it.path } }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        val candidates =
            buildList {
                if (configured.isNotEmpty()) {
                    addAll(configured)
                } else {
                    addAll(mediaStoreFolders(context))
                    Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                        ?.path
                        ?.let { add(it) }
                }
            }
        val directories =
            candidates
                .map { File(it) }
                .filter { it.isDirectory }
                .distinctBy { ExtraFormatTrackIds.canonicalPath(it.path) }
                .sortedBy { it.absolutePath.length }
        val roots =
            if (configured.isNotEmpty()) {
                directories
            } else {
                // MediaStore returns every song folder; keep only top-level roots so a deep
                // library is not walked once per album folder.
                val pruned = ArrayList<File>()
                for (directory in directories) {
                    if (pruned.none { directory.absolutePath.startsWith(it.absolutePath + File.separator) }) {
                        pruned.add(directory)
                    }
                }
                pruned
            }
        return roots.take(MAX_ROOTS)
    }

    private fun mediaStoreFolders(context: Context): List<String> {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val folders = HashSet<String>()
        runCatching {
            context.contentResolver
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val data = cursor.getString(0) ?: continue
                        File(data).parent?.let { folders.add(it) }
                        if (folders.size >= MAX_ROOTS) break
                    }
                }
        }
        return folders.toList()
    }

    private fun isBlacklisted(
        path: String,
        blacklist: List<String>,
    ): Boolean = blacklist.any { path == it || path.startsWith("$it${File.separator}") }

    private fun String.leadingNumber(): Int = takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    private const val STARTUP_DELAY_MS = 4_000L
    private const val FRESH_AFTER_MS = 6 * 60 * 60 * 1000L
    private const val MAX_ROOTS = 256
}
