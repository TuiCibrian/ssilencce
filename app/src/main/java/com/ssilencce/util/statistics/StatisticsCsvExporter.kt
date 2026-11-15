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

package com.ssilencce.util.statistics

import android.content.ContentResolver
import android.net.Uri
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.ContentType
import com.ssilencce.data.model.ListeningPeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.time.ZoneId
import java.util.Locale

data class StatisticsCsvDocument(
    val fileName: String,
    val content: String,
)

object StatisticsCsvExporter : KoinComponent {
    private const val MAX_ROWS = 100

    private val repository: Repository by inject()

    suspend fun prepare(
        contentType: ContentType,
        period: ListeningPeriod,
    ): StatisticsCsvDocument? =
        withContext(Dispatchers.IO) {
            when (contentType) {
                ContentType.TopTracks -> {
                    val stats = repository.songPlayStats(period).take(MAX_ROWS)
                    if (stats.isEmpty()) {
                        null
                    } else {
                        StatisticsCsvDocument(
                            fileName = fileName("top-tracks", period),
                            content = StatisticsCsvWriter.topTracks(stats),
                        )
                    }
                }

                ContentType.TopArtists -> {
                    val stats = repository.artistPlayStats(period).take(MAX_ROWS)
                    if (stats.isEmpty()) {
                        null
                    } else {
                        StatisticsCsvDocument(
                            fileName = fileName("top-artists", period),
                            content = StatisticsCsvWriter.topArtists(stats),
                        )
                    }
                }

                else -> {
                    null
                }
            }
        }

    /**
     * Prepares a completed-period document for the automatic worker. Manual [prepare] keeps its
     * current-period semantics and file names unchanged.
     */
    suspend fun prepareAutomatic(
        contentType: ContentType,
        window: StatisticsCsvWindow,
    ): StatisticsCsvDocument? =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = window.startMillis(zone)
            val end = window.endExclusiveMillis(zone)
            when (contentType) {
                ContentType.TopTracks -> {
                    val stats = repository.songPlayStats(start, end).take(MAX_ROWS)
                    if (stats.isEmpty()) {
                        null
                    } else {
                        StatisticsCsvDocument(
                            fileName = automaticFileName(contentType, window)!!,
                            content = StatisticsCsvWriter.topTracks(stats),
                        )
                    }
                }

                ContentType.TopArtists -> {
                    val stats = repository.artistPlayStats(start, end).take(MAX_ROWS)
                    if (stats.isEmpty()) {
                        null
                    } else {
                        StatisticsCsvDocument(
                            fileName = automaticFileName(contentType, window)!!,
                            content = StatisticsCsvWriter.topArtists(stats),
                        )
                    }
                }

                else -> {
                    null
                }
            }
        }

    internal fun automaticFileName(
        contentType: ContentType,
        window: StatisticsCsvWindow,
    ): String? =
        when (contentType) {
            ContentType.TopTracks -> StatisticsCsvFiles.tracksName(window)
            ContentType.TopArtists -> StatisticsCsvFiles.artistsName(window)
            else -> null
        }

    suspend fun write(
        resolver: ContentResolver,
        uri: Uri,
        document: StatisticsCsvDocument,
    ) {
        withContext(Dispatchers.IO) {
            var outputOpened = false
            try {
                val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Unable to open statistics document")
                outputOpened = true
                stream.use { output ->
                    output.write(document.content.toByteArray(Charsets.UTF_8))
                }
            } catch (cancellation: CancellationException) {
                if (outputOpened) deleteQuietly(resolver, uri)
                throw cancellation
            } catch (error: Exception) {
                if (outputOpened) deleteQuietly(resolver, uri)
                throw error
            }
        }
    }

    private fun fileName(
        type: String,
        period: ListeningPeriod,
    ): String = "ssilencce-$type-${period.name.lowercase(Locale.ROOT)}.csv"

    private fun deleteQuietly(
        resolver: ContentResolver,
        uri: Uri,
    ) {
        runCatching { resolver.delete(uri, null, null) }
    }
}
