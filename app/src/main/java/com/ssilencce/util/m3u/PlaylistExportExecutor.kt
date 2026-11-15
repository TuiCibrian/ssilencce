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

package com.ssilencce.util.m3u

import android.content.Context
import android.widget.Toast
import com.ssilencce.R
import com.ssilencce.data.local.repository.PlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Process-owned runner for dialog-initiated playlist exports.
 *
 * The dialog is only the command surface; dismissing or recreating it must not cancel the export or
 * let a restored copy start the same request twice. The token is carried in dialog arguments and
 * remains reserved until the operation completes.
 */
object PlaylistExportExecutor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeTokens = ConcurrentHashMap.newKeySet<String>()

    fun start(
        token: String,
        playlistIds: List<Long>,
        format: PlaylistExportFormat,
        context: Context,
    ): Boolean {
        if (!activeTokens.add(token)) return false
        val appContext = context.applicationContext
        scope.launch {
            try {
                val repository = GlobalContext.get().get<PlaylistRepository>()
                val playlists = withContext(IO) { playlistIds.map { repository.playlistWithSongs(it) } }
                if (playlists.isEmpty()) {
                    Toast
                        .makeText(
                            appContext,
                            R.string.failed_to_save_playlist,
                            Toast.LENGTH_SHORT,
                        ).show()
                } else if (playlists.size == 1) {
                    M3UWriter.export(appContext, playlists.first(), format)
                } else {
                    M3UWriter.export(appContext, playlists, format)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Toast
                    .makeText(
                        appContext,
                        R.string.failed_to_save_playlist,
                        Toast.LENGTH_SHORT,
                    ).show()
            } finally {
                activeTokens.remove(token)
            }
        }
        return true
    }
}
