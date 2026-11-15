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

/** A timestamped marker inside a song. [text] may be empty for a quick bookmark. */
data class SongBookmark(
    val timestampMs: Long,
    val text: String = "",
) {
    val positionMs: Long
        get() = timestampMs.coerceAtLeast(0L)
}

/** Everything stored for a song in its plain-text notes file. */
data class SongNotes(
    val songId: Long,
    val notes: String = "",
    val bookmarks: List<SongBookmark> = emptyList(),
) {
    val hasContent: Boolean
        get() = notes.isNotBlank() || bookmarks.isNotEmpty()
}
