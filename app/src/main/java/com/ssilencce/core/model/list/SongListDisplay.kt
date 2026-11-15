package com.ssilencce.core.model.list

import com.ssilencce.core.model.sort.SortKey
import com.ssilencce.data.model.Song

enum class SongTitleMode {
    FollowSort,
    Title,
    FileName,
}

/**
 * Pure display rules shared by every song list.
 *
 * [SongTitleMode.FollowSort] preserves the historical behavior: the file name is shown only while
 * the list is sorted by [SortKey.FileName]. The date suffix is date-only and is appended to the
 * existing secondary text; layouts without a secondary text view are unchanged.
 */
object SongListDisplayPolicy {
    const val INFO_DELIMITER = " • "

    fun title(
        song: Song,
        sortKey: SortKey?,
        mode: SongTitleMode,
    ): String =
        when (mode) {
            SongTitleMode.FollowSort -> if (sortKey == SortKey.FileName) song.fileName else song.title
            SongTitleMode.Title -> song.title
            SongTitleMode.FileName -> song.fileName
        }

    fun appendDateModified(
        base: String?,
        dateModified: String?,
    ): String? =
        when {
            dateModified.isNullOrBlank() -> base
            base.isNullOrBlank() -> dateModified
            else -> "$base$INFO_DELIMITER$dateModified"
        }
}
