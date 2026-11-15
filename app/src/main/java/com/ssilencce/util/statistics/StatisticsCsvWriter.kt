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

import com.ssilencce.data.model.ArtistPlayStat
import com.ssilencce.data.model.SongPlayStat

object StatisticsCsvWriter {
    private const val TOP_TRACKS_HEADER = "Rank,Title,Artist,Album,DurationMs,Plays"
    private const val TOP_ARTISTS_HEADER = "Rank,Artist,Plays,Percentage"
    private const val LINE_ENDING = "\r\n"
    private val FORMULA_PREFIXES = setOf('=', '+', '-', '@')

    fun topTracks(stats: List<SongPlayStat>): String =
        buildString {
            append(TOP_TRACKS_HEADER).append(LINE_ENDING)
            stats.forEachIndexed { index, stat ->
                appendRow(
                    (index + 1).toString(),
                    escapeText(stat.song.title),
                    escapeText(stat.song.artistName),
                    escapeText(stat.song.albumName),
                    stat.song.duration.toString(),
                    stat.playCount.toString(),
                )
            }
        }

    fun topArtists(stats: List<ArtistPlayStat>): String =
        buildString {
            append(TOP_ARTISTS_HEADER).append(LINE_ENDING)
            stats.forEachIndexed { index, stat ->
                appendRow(
                    (index + 1).toString(),
                    escapeText(stat.artistName),
                    stat.playCount.toString(),
                    stat.percentage.toString(),
                )
            }
        }

    private fun StringBuilder.appendRow(vararg fields: String) {
        fields.forEachIndexed { index, field ->
            if (index > 0) append(',')
            append(field)
        }
        append(LINE_ENDING)
    }

    private fun escapeText(value: String): String {
        val firstNonSpace = value.firstOrNull { !it.isWhitespace() }
        val neutralized =
            if (firstNonSpace != null && firstNonSpace in FORMULA_PREFIXES) {
                "'$value"
            } else {
                value
            }
        return if (neutralized.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
            "\"${neutralized.replace("\"", "\"\"")}\""
        } else {
            neutralized
        }
    }
}
