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

package com.ssilencce.data.model.lyrics

import androidx.compose.runtime.Immutable
import com.ssilencce.R

/**
 * Which lyrics source a single song should use.
 *
 * [Automatic] keeps the legacy priority (file, embedded, downloaded). The other values force one
 * source; [File] points at a document picked by the user through SAF.
 */
@Immutable
data class LyricsSourceSelection(
    val source: LyricsSourceChoice = LyricsSourceChoice.Automatic,
    val fileUri: String? = null,
    val fileName: String? = null,
) {
    companion object {
        val Automatic = LyricsSourceSelection()
    }
}

enum class LyricsSourceChoice(
    val titleRes: Int,
    val summaryRes: Int,
) {
    Automatic(R.string.lyrics_source_automatic, R.string.lyrics_source_automatic_summary),
    Embedded(R.string.embedded_lyrics, R.string.lyrics_source_embedded_summary),
    Downloaded(R.string.downloaded_lyrics, R.string.lyrics_source_downloaded_summary),
    File(R.string.file_lyrics, R.string.lyrics_source_file_summary),
}
