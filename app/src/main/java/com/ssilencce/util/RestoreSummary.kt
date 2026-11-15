/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.util

/**
 * Content categories recognized inside a backup archive. Kept independent from
 * [BackupContent] so the summary logic can run without an Android runtime.
 */
enum class RestoreEntryKind {
    Settings,
    Lyrics,
    ArtistImages,
    Playlists,
    Favorites,
    PlayCounts,
    Queues,
    PerFileSettings,
    SongPlaybackParams,
    Notes,
    Unrecognized,
}

/**
 * Honest outcome of a restore attempt. [Success] means exactly one selected content item was
 * imported; [SuccessWithCount] means more than one was imported; [Partial] means at least one
 * selected entry failed while another selected entry was imported.
 */
enum class RestoreOutcome {
    Success,
    SuccessWithCount,
    NothingSelected,
    NoRecognizedContent,
    NothingImported,
    Partial,
}

/**
 * Pure bookkeeping for [BackupHelper.restoreBackup]. It counts every ZIP entry, the entries that
 * belong to a known backup category, the recognized entries whose category was selected, and the
 * content items that were actually written or inserted, and selected entries that failed.
 */
class RestoreSummary(
    private val selectionRequested: Boolean,
) {
    var totalEntries: Int = 0
        private set

    var recognizedEntries: Int = 0
        private set

    var selectedEntries: Int = 0
        private set

    var importedItems: Int = 0
        private set

    var failedSelectedEntries: Int = 0
        private set

    fun recordEntry(
        kind: RestoreEntryKind,
        selected: Boolean,
    ) {
        totalEntries++
        if (kind == RestoreEntryKind.Unrecognized) return
        recognizedEntries++
        if (selected) selectedEntries++
    }

    fun recordImport(count: Int = 1) {
        if (count > 0) importedItems += count
    }

    /** Records a selected entry that was recognized but could not be imported. */
    fun recordFailure() {
        failedSelectedEntries++
    }

    /**
     * Records a valid playlist entry. Creating the playlist entity is a real import even when the
     * M3U contains no resolvable songs; an already existing playlist is only an import when songs
     * were added to it.
     */
    fun recordPlaylistRestore(
        playlistCreated: Boolean,
        resolvedSongs: Int,
    ): Boolean {
        val imported = playlistCreated || resolvedSongs > 0
        if (imported) recordImport()
        return imported
    }

    fun outcome(): RestoreOutcome =
        when {
            !selectionRequested -> RestoreOutcome.NothingSelected
            recognizedEntries == 0 -> RestoreOutcome.NoRecognizedContent
            importedItems == 0 -> RestoreOutcome.NothingImported
            failedSelectedEntries > 0 -> RestoreOutcome.Partial
            importedItems == 1 -> RestoreOutcome.Success
            else -> RestoreOutcome.SuccessWithCount
        }
}
