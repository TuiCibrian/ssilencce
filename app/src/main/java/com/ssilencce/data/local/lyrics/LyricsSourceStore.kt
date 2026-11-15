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

package com.ssilencce.data.local.lyrics

import android.content.Context
import android.util.Log
import com.ssilencce.data.model.lyrics.LyricsSourceChoice
import com.ssilencce.data.model.lyrics.LyricsSourceSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Small per-song store for the user's lyrics source choice.
 *
 * A dedicated Room table would force a schema migration for a few bytes per song, so the entries
 * live in a single JSON document inside app storage instead. The document is loaded lazily and
 * written atomically (temporary file + rename) to survive process death during a write.
 */
class LyricsSourceStore(
    context: Context,
) {
    private val directory = File(context.applicationContext.filesDir, "lyrics")
    private val file = File(directory, "lyrics_sources.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private var entries: MutableMap<Long, LyricsSourceSelection>? = null

    suspend fun get(songId: Long): LyricsSourceSelection =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                loadEntries()[songId] ?: LyricsSourceSelection.Automatic
            }
        }

    suspend fun put(
        songId: Long,
        selection: LyricsSourceSelection,
    ) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadEntries()
            if (selection == LyricsSourceSelection.Automatic) {
                current.remove(songId)
            } else {
                current[songId] = selection
            }
            persist(current)
        }
    }

    private fun loadEntries(): MutableMap<Long, LyricsSourceSelection> {
        entries?.let { return it }
        val loaded =
            runCatching {
                if (file.exists()) {
                    json
                        .decodeFromString<StoredDocument>(file.readText())
                        .selections
                        .mapNotNull { (key, value) ->
                            key.toLongOrNull()?.let { songId ->
                                songId to value.toSelection()
                            }
                        }.toMap()
                        .toMutableMap()
                } else {
                    mutableMapOf()
                }
            }.onFailure {
                Log.e(TAG, "Could not read lyrics source store", it)
            }.getOrDefault(mutableMapOf())
        entries = loaded
        return loaded
    }

    private fun persist(entries: Map<Long, LyricsSourceSelection>) {
        try {
            directory.mkdirs()
            val document =
                StoredDocument(
                    selections =
                        entries
                            .mapKeys { (songId, _) -> songId.toString() }
                            .mapValues { (_, selection) -> StoredSelection.from(selection) },
                )
            val temporary = File(directory, "${file.name}.tmp")
            temporary.writeText(json.encodeToString(document))
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText())
                temporary.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not write lyrics source store", e)
        }
    }

    @Serializable
    private class StoredDocument(
        val selections: Map<String, StoredSelection> = emptyMap(),
    )

    @Serializable
    private class StoredSelection(
        val source: String,
        val fileUri: String? = null,
        val fileName: String? = null,
    ) {
        fun toSelection(): LyricsSourceSelection {
            val choice = LyricsSourceChoice.entries.firstOrNull { it.name == source }
            return if (choice == null) {
                LyricsSourceSelection.Automatic
            } else {
                LyricsSourceSelection(choice, fileUri, fileName)
            }
        }

        companion object {
            fun from(selection: LyricsSourceSelection) =
                StoredSelection(
                    source = selection.source.name,
                    fileUri = selection.fileUri,
                    fileName = selection.fileName,
                )
        }
    }

    companion object {
        private const val TAG = "LyricsSourceStore"
    }
}
