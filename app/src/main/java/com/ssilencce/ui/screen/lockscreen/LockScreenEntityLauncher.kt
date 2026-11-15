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

package com.ssilencce.ui.screen.lockscreen

import android.content.Context
import android.content.Intent
import com.ssilencce.data.model.Genre
import com.ssilencce.data.model.Song
import com.ssilencce.ui.screen.MainActivity

enum class LockScreenEntityType {
    Artist,
    Folder,
    Genre,
    Playlist,
    GhostSearch,
}

/**
 * Opens a library detail screen from the lock screen player. The lock screen itself has no
 * navigation host, so the request travels to [MainActivity], which resolves it once the player
 * session is connected.
 */
object LockScreenEntityLauncher {
    const val ACTION_OPEN_ENTITY = "com.ssilencce.lockscreen.action.OPEN_ENTITY"
    const val EXTRA_ENTITY_TYPE = "lockscreen_entity_type"
    const val EXTRA_SONG = "lockscreen_entity_song"
    const val EXTRA_GENRE = "lockscreen_entity_genre"
    const val EXTRA_FOLDER_PATH = "lockscreen_entity_folder_path"
    const val EXTRA_PLAYLIST_ID = "lockscreen_entity_playlist_id"

    fun openArtist(
        context: Context,
        song: Song,
    ): Boolean = launch(context, LockScreenEntityType.Artist) { putExtra(EXTRA_SONG, song) }

    fun openFolder(
        context: Context,
        folderPath: String,
    ): Boolean = launch(context, LockScreenEntityType.Folder) { putExtra(EXTRA_FOLDER_PATH, folderPath) }

    fun openGenre(
        context: Context,
        genre: Genre,
    ): Boolean = launch(context, LockScreenEntityType.Genre) { putExtra(EXTRA_GENRE, genre) }

    fun openPlaylist(
        context: Context,
        playlistId: Long,
    ): Boolean = launch(context, LockScreenEntityType.Playlist) { putExtra(EXTRA_PLAYLIST_ID, playlistId) }

    fun openGhostSearch(context: Context): Boolean = launch(context, LockScreenEntityType.GhostSearch) {}

    private fun launch(
        context: Context,
        type: LockScreenEntityType,
        extras: Intent.() -> Unit,
    ): Boolean {
        val intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_ENTITY)
                .putExtra(EXTRA_ENTITY_TYPE, type.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply(extras)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
