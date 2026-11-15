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

package com.ssilencce.data.local.lyrics.lrc

import java.util.Locale

/**
 * Intermediate line used by the in-app sync editor. A null timestamp means the line still needs
 * to be stamped.
 */
data class StampedLyricLine(
    val text: String,
    val timestamp: Long? = null,
)

/**
 * Helpers to turn plain lyrics into LRC timestamps and back. The parsing side is intentionally
 * lenient so an existing LRC document can be re-opened for re-stamping without losing its line
 * text.
 */
object LrcTools {
    private val LINE_WITH_TIMESTAMPS =
        Regex("""^\s*((?:\[\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?\])+)(.*)$""")
    private val LINE_TIMESTAMP = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val WORD_TIMESTAMP = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val METADATA_ATTRIBUTE = Regex("""^\s*\[[a-zA-Z#]+:[^]]*]\s*$""")

    fun formatTimestamp(positionMs: Long): String {
        val clamped = positionMs.coerceAtLeast(0)
        val minutes = clamped / 60_000
        val seconds = (clamped / 1_000) % 60
        val centiseconds = (clamped % 1_000) / 10
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centiseconds)
    }

    fun parseTimestamp(value: String): Long? {
        val match = LINE_TIMESTAMP.find(value) ?: return null
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val fraction =
            match.groupValues[3].takeIf { it.isNotEmpty() }?.let { text ->
                when (text.length) {
                    1 -> text.toLongOrNull()?.times(100)
                    2 -> text.toLongOrNull()?.times(10)
                    else -> text.padEnd(3, '0').take(3).toLongOrNull()
                }
            } ?: 0
        return (minutes * 60 + seconds) * 1_000 + fraction
    }

    /**
     * Splits [content] into editable lines. Blank lines and LRC metadata attributes are dropped;
     * existing line timestamps (and enhanced word timestamps) are removed from the text.
     */
    fun parseLines(content: String): List<StampedLyricLine> {
        // TTML is markup, not plain lyrics; showing its raw tags as stampable lines would be noise.
        if (content.trimStart().startsWith("<")) {
            return emptyList()
        }
        val result = mutableListOf<StampedLyricLine>()
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.isBlank() || METADATA_ATTRIBUTE.matches(line)) {
                return@forEach
            }
            val timestamped = LINE_WITH_TIMESTAMPS.matchEntire(line)
            val timestamp = timestamped?.let { parseTimestamp(it.groupValues[1]) }
            val text =
                (timestamped?.groupValues?.getOrNull(2) ?: line)
                    .replace(WORD_TIMESTAMP, "")
                    .trim()
            if (text.isNotEmpty()) {
                result.add(StampedLyricLine(text, timestamp))
            }
        }
        return result
    }

    /**
     * Serializes stamped lines as a standard LRC document. Out-of-order stamps are written in
     * timestamp order so third-party players can consume the result.
     */
    fun buildLrc(lines: List<StampedLyricLine>): String {
        val stamped = lines.filter { it.timestamp != null }.sortedBy { it.timestamp }
        return buildString {
            stamped.forEach { line ->
                append(formatTimestamp(line.timestamp ?: 0L))
                append(line.text)
                append('\n')
            }
        }.trimEnd()
    }
}
