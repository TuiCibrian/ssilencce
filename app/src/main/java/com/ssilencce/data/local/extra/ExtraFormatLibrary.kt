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

import android.content.Context
import android.net.Uri
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.ExtraFormatUriStore
import com.ssilencce.data.model.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A file discovered by the supplementary scanner. Stored in a small JSON cache so every song
 * query stays a memory lookup instead of walking the filesystem.
 */
@Serializable
data class ExtraFormatTrack(
    val id: Long,
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String? = null,
    val genre: String? = null,
    val composer: String? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val year: Int = 0,
    val durationMs: Long,
    val size: Long,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val sampleRate: Int = 0,
    val channels: Int = 0,
) {
    fun toSong(): Song {
        val albumName = album.ifBlank { Album.UNKNOWN_ALBUM_DISPLAY_NAME }
        return Song(
            id = id,
            data = path,
            title = title,
            trackNumber = trackNumber,
            year = year,
            size = size,
            duration = durationMs,
            dateAdded = dateAddedSeconds,
            rawDateModified = dateModifiedSeconds,
            albumId = if (album.isBlank()) -1 else syntheticId("album:$album"),
            albumName = albumName,
            artistId = if (artist.isBlank()) -1 else syntheticId("artist:$artist"),
            artistName = artist,
            albumArtistName = albumArtist,
            genreName = genre,
            volumeName = null,
            composerName = composer,
            discNumber = discNumber,
        )
    }

    private fun syntheticId(value: String): Long = ExtraFormatTrackIds.forValue(value)
}

@Serializable
private data class ExtraFormatCache(
    val version: Int = EXTRA_FORMAT_CACHE_VERSION,
    val tracks: List<ExtraFormatTrack> = emptyList(),
)

private const val EXTRA_FORMAT_CACHE_VERSION = 1

/** Deterministic negative ids shared by the scanner, the cache and the URI resolver. */
object ExtraFormatTrackIds {
    fun forPath(path: String): Long = forValue("path:${canonicalPath(path)}")

    /** Resolves storage-root symlinks (for example /sdcard and /storage/emulated/0). */
    fun canonicalPath(path: String): String = runCatching { File(path).canonicalPath }.getOrDefault(path)

    fun forValue(value: String): Long {
        var hash = -0x340d631b7bdddcdbL
        for (character in value) {
            hash = hash xor character.code.toLong()
            hash *= 0x100000001b3L
        }
        return -((hash and 0x3FFFFFFFFFFFFFFFL) + 2L)
    }
}

/**
 * Process-wide view of the supplementary scan cache.
 *
 * The cache is loaded before any list can show an extra-format song, which also fills
 * [ExtraFormatUriStore] so `Song.uri` can return `file://` URIs without storing a second field in
 * every song subclass (queue, expanded, favorites and history copies all share the same id).
 */
object ExtraFormatLibrary {
    private val cacheFile = "extra_formats/library.json"
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Volatile
    private var tracks: List<ExtraFormatTrack>? = null

    fun ensureLoaded(context: Context) {
        if (tracks != null) return
        synchronized(this) {
            if (tracks != null) return
            val loaded =
                runCatching {
                    val file = File(context.filesDir, cacheFile)
                    if (!file.isFile) return@runCatching emptyList()
                    json.decodeFromString<ExtraFormatCache>(file.readText()).tracks
                }.getOrDefault(emptyList())
            apply(normalizeTracks(loaded))
        }
    }

    fun songs(context: Context): List<Song> {
        ensureLoaded(context)
        return tracks.orEmpty().map { it.toSong() }
    }

    fun songById(
        context: Context,
        songId: Long,
    ): Song? {
        ensureLoaded(context)
        return tracks.orEmpty().firstOrNull { it.id == songId }?.toSong()
    }

    fun songByPath(
        context: Context,
        path: String,
    ): Song? {
        ensureLoaded(context)
        val canonical = ExtraFormatTrackIds.canonicalPath(path)
        return tracks.orEmpty().firstOrNull { ExtraFormatTrackIds.canonicalPath(it.path) == canonical }?.toSong()
    }

    fun isEmpty(context: Context): Boolean {
        ensureLoaded(context)
        return tracks.orEmpty().isEmpty()
    }

    fun cacheLastModified(context: Context): Long = File(context.filesDir, cacheFile).lastModified()

    fun update(
        context: Context,
        newTracks: List<ExtraFormatTrack>,
    ) {
        synchronized(this) {
            val normalized = normalizeTracks(newTracks)
            val cache = ExtraFormatCache(tracks = normalized)
            runCatching {
                val file = File(context.filesDir, cacheFile)
                file.parentFile?.mkdirs()
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(json.encodeToString(cache))
                if (file.exists() && !file.delete()) {
                    file.writeText(json.encodeToString(cache))
                } else if (!temp.renameTo(file)) {
                    file.writeText(json.encodeToString(cache))
                }
                temp.delete()
            }
            apply(normalized)
        }
    }

    fun clear(context: Context) {
        synchronized(this) {
            runCatching { File(context.filesDir, cacheFile).delete() }
            apply(emptyList())
        }
    }

    private fun normalizeTracks(tracks: List<ExtraFormatTrack>): List<ExtraFormatTrack> {
        val seen = HashSet<Triple<String, Long, Long>>()
        return tracks.mapNotNull { track ->
            val canonicalPath = ExtraFormatTrackIds.canonicalPath(track.path)
            val normalized =
                if (canonicalPath == track.path) {
                    track
                } else {
                    track.copy(
                        id = ExtraFormatTrackIds.forPath(canonicalPath),
                        path = canonicalPath,
                    )
                }
            val identity = Triple(normalized.path, normalized.size, normalized.dateModifiedSeconds)
            normalized.takeIf { seen.add(identity) }
        }
    }

    private fun apply(newTracks: List<ExtraFormatTrack>) {
        tracks = newTracks
        ExtraFormatUriStore.replace(
            newTracks.associate { track ->
                track.id to Uri.fromFile(File(track.path)).toString()
            },
        )
    }

    private const val CACHE_VERSION = 1
}
