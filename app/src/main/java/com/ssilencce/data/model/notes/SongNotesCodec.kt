/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.data.model.notes

/**
 * Plain-text format for [SongNotes]. Files stay readable and editable outside the app:
 *
 * ```
 * # ssilencce song notes
 * # Song ID: 42
 * # Title: Example
 * # Artist: Artist
 *
 * [Notes]
 * Free text.
 *
 * [Bookmarks]
 * 1:23.500 Intro
 * 12:04 Guitar solo
 * ```
 *
 * Missing sections are tolerated. A file without any section marker is treated as plain notes, so
 * importing an arbitrary `.txt` file works too.
 */
object SongNotesCodec {
    const val NOTES_SECTION = "[Notes]"
    const val BOOKMARKS_SECTION = "[Bookmarks]"

    data class Content(
        val notes: String = "",
        val bookmarks: List<SongBookmark> = emptyList(),
    )

    private val notesSectionRegex = Regex("""^\[notes]$""", RegexOption.IGNORE_CASE)
    private val bookmarksSectionRegex = Regex("""^\[bookmarks]$""", RegexOption.IGNORE_CASE)
    private val bookmarkLineRegex =
        Regex("""^(\d{1,3}):(\d{2})(?::(\d{2}))?(?:[.,](\d{1,3}))?(?:\s+(.*))?$""")

    fun decode(text: String): Content {
        val lines =
            text
                .removePrefix("\uFEFF")
                .lineSequence()
                .map { it.trimEnd('\r') }
                .toList()
        var section = Section.Preamble
        var sawSection = false
        val notes = mutableListOf<String>()
        val bookmarks = mutableListOf<SongBookmark>()
        for (line in lines) {
            val marker = line.trim()
            when {
                notesSectionRegex.matches(marker) -> {
                    section = Section.Notes
                    sawSection = true
                    continue
                }

                bookmarksSectionRegex.matches(marker) -> {
                    section = Section.Bookmarks
                    sawSection = true
                    continue
                }
            }
            when (section) {
                Section.Preamble -> Unit
                Section.Notes -> notes += line
                Section.Bookmarks -> parseBookmarkLine(line)?.let { bookmarks += it }
            }
        }
        if (!sawSection) {
            val firstContentLine = lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("#") }
            val plainNotes =
                if (firstContentLine < 0) {
                    ""
                } else {
                    lines.drop(firstContentLine).joinToString("\n").trim('\n', '\r')
                }
            return Content(notes = plainNotes)
        }
        return Content(
            notes = notes.joinToString("\n").trim('\n', '\r'),
            bookmarks = bookmarks.sortedBy { it.positionMs },
        )
    }

    fun encode(
        songId: Long,
        title: String,
        artist: String,
        notes: String,
        bookmarks: List<SongBookmark>,
    ): String =
        buildString {
            appendLine("# ssilencce song notes")
            appendLine("# Song ID: $songId")
            appendLine("# Title: ${title.singleLine()}")
            appendLine("# Artist: ${artist.singleLine()}")
            appendLine()
            appendLine(NOTES_SECTION)
            if (notes.isNotBlank()) {
                appendLine(notes.trim('\n', '\r'))
            }
            appendLine()
            appendLine(BOOKMARKS_SECTION)
            bookmarks
                .sortedBy { it.positionMs }
                .forEach { bookmark ->
                    val text = bookmark.text.singleLine()
                    if (text.isEmpty()) {
                        appendLine(formatTimestamp(bookmark.positionMs))
                    } else {
                        appendLine("${formatTimestamp(bookmark.positionMs)} $text")
                    }
                }
        }

    fun shareText(
        title: String,
        artist: String,
        notes: String,
        bookmarks: List<SongBookmark>,
    ): String =
        buildString {
            append(title.trim())
            if (artist.isNotBlank()) {
                append("\n")
                append(artist.trim())
            }
            if (notes.isNotBlank()) {
                append("\n\n")
                append(notes.trim())
            }
            if (bookmarks.isNotEmpty()) {
                append("\n\n")
                bookmarks
                    .sortedBy { it.positionMs }
                    .forEach { bookmark ->
                        append("\n")
                        append(formatTimestamp(bookmark.positionMs))
                        if (bookmark.text.isNotBlank()) {
                            append(" ")
                            append(bookmark.text.trim())
                        }
                    }
            }
        }

    fun formatTimestamp(timestampMs: Long): String {
        val total = timestampMs.coerceAtLeast(0L)
        val hours = total / 3_600_000L
        val minutes = (total / 60_000L) % 60L
        val seconds = (total / 1_000L) % 60L
        val millis = total % 1_000L
        val base =
            if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        return if (millis == 0L) base else "%s.%03d".format(base, millis)
    }

    /** Parses `m:ss`, `h:mm:ss` and the same forms with an optional `.mmm` fraction. */
    fun parseTimestamp(input: String): Long? = bookmarkLineRegex.matchEntire(input.trim())?.toTimestampMsOrNull()

    private fun parseBookmarkLine(line: String): SongBookmark? {
        val match = bookmarkLineRegex.matchEntire(line.trim()) ?: return null
        val timestampMs = match.toTimestampMsOrNull() ?: return null
        return SongBookmark(
            timestampMs = timestampMs,
            text = match.groupValues[5].trim(),
        )
    }

    private fun MatchResult.toTimestampMsOrNull(): Long? {
        val first = groupValues[1].toLongOrNull() ?: return null
        val second = groupValues[2].toLongOrNull() ?: return null
        val third = groupValues[3]
        val hours = if (third.isNullOrEmpty()) 0L else first
        val minutes = if (third.isNullOrEmpty()) first else second
        val seconds = if (third.isNullOrEmpty()) second else third.toLongOrNull() ?: return null
        val millis =
            groupValues[4]
                .takeIf { it.isNotEmpty() }
                ?.padEnd(3, '0')
                ?.take(3)
                ?.toLongOrNull() ?: 0L
        return (hours * 3_600L + minutes * 60L + seconds) * 1_000L + millis
    }

    private fun String.singleLine(): String =
        replace('\r', ' ')
            .replace('\n', ' ')
            .trim()

    private enum class Section {
        Preamble,
        Notes,
        Bookmarks,
    }
}
