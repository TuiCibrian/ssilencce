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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.screen.library

import android.content.Context
import com.ssilencce.R
import com.ssilencce.data.local.room.PlaylistEntity
import com.ssilencce.data.model.Song

data class HandleIntentResult(
    val handled: Boolean,
    val songs: List<Song> = emptyList(),
    val position: Int = 0,
    val failed: Boolean = false,
)

data class AddToPlaylistResult(
    val playlistName: String,
    val isWorking: Boolean = false,
    val playlistCreated: Boolean = false,
    val isFavoritePlaylist: Boolean = false,
    val insertedSongs: Int = 0,
)

class ImportablePlaylistResult(
    val playlistName: String,
    val songs: List<Song>,
)

enum class ImportOutcome {
    Created,
    Replaced,
}

data class ImportResult(
    val resultMessage: String,
    val actualName: String?,
    val outcome: ImportOutcome?,
    val importedSongCount: Int,
    val success: Boolean,
) {
    companion object {
        fun created(
            ctx: Context,
            actualName: String,
            importedSongCount: Int,
        ): ImportResult =
            ImportResult(
                resultMessage = ctx.getString(R.string.import_applied_as, actualName, importedSongCount),
                actualName = actualName,
                outcome = ImportOutcome.Created,
                importedSongCount = importedSongCount,
                success = true,
            )

        fun replaced(
            ctx: Context,
            actualName: String,
            importedSongCount: Int,
        ): ImportResult =
            ImportResult(
                resultMessage = ctx.getString(R.string.import_replaced, actualName, importedSongCount),
                actualName = actualName,
                outcome = ImportOutcome.Replaced,
                importedSongCount = importedSongCount,
                success = true,
            )

        fun error(
            ctx: Context,
            sourceName: String,
        ): ImportResult =
            ImportResult(
                resultMessage = ctx.getString(R.string.could_not_import_the_playlist_x, sourceName),
                actualName = null,
                outcome = null,
                importedSongCount = 0,
                success = false,
            )
    }
}

enum class PlaylistImportReplaceBlock {
    Favorites,
    MultipleMatches,
}

enum class PlaylistImportRenameError {
    Blank,
    Duplicate,
}

enum class PlaylistImportGeneralError {
    SuffixExhausted,
    NameTaken,
    ConflictChanged,
    DatabaseFailure,
}

data class PlaylistImportConflictState(
    val sourceName: String,
    val selectedSongCount: Int,
    val existing: List<PlaylistEntity>,
    val existingSongCount: Int,
    val defaultName: String?,
    val existingNames: List<String>,
    val replaceBlock: PlaylistImportReplaceBlock?,
    val applying: Boolean = false,
    val renameError: PlaylistImportRenameError? = null,
    val generalError: PlaylistImportGeneralError? = null,
) {
    val canReplace: Boolean
        get() = existing.size == 1 && replaceBlock == null
}
