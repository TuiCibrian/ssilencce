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

package com.ssilencce.playback

import android.content.ComponentName
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.ssilencce.R

/**
 * Quick Settings playback toggle backed by the existing [PlaybackService] MediaSession.
 *
 * The controller is connected only while the tile is listening and released as soon as it stops,
 * so the tile never polls and does not keep a session connection alive in the background.
 */
class PlaybackTileService : TileService() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var listening = false
    private var pendingToggle = false

    private val playerListener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                updateTile()
            }
        }

    private val controllerListener =
        object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) {
                if (this@PlaybackTileService.controller === controller) {
                    this@PlaybackTileService.controller = null
                    controller.removeListener(playerListener)
                    controller.release()
                }
                updateTile()
            }
        }

    override fun onStartListening() {
        super.onStartListening()
        listening = true
        updateTile()
        if (controller != null || controllerFuture != null) return
        connectController()
    }

    override fun onStopListening() {
        super.onStopListening()
        listening = false
        releaseController()
        updateTile()
    }

    override fun onDestroy() {
        listening = false
        releaseController()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val player = controller
        if (player == null) {
            if (controllerFuture == null && listening) {
                connectController()
            }
            pendingToggle = true
            updateTile()
            return
        }
        togglePlayback(player)
    }

    private fun togglePlayback(player: MediaController) {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        updateTile()
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future =
            MediaController
                .Builder(this, token)
                .setListener(controllerListener)
                .buildAsync()
        controllerFuture = future
        future.addListener(
            { onControllerReady(future) },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun onControllerReady(future: ListenableFuture<MediaController>) {
        val instance =
            try {
                future.get()
            } catch (throwable: Throwable) {
                if (controllerFuture === future) {
                    controllerFuture = null
                }
                pendingToggle = false
                updateTile()
                return
            }
        if (controllerFuture !== future || !listening) {
            pendingToggle = false
            instance.release()
            return
        }
        controllerFuture = null
        controller = instance
        instance.addListener(playerListener)
        if (pendingToggle) {
            pendingToggle = false
            togglePlayback(instance)
        } else {
            updateTile()
        }
    }

    private fun releaseController() {
        pendingToggle = false
        val future = controllerFuture
        controllerFuture = null
        future?.let { MediaController.releaseFuture(it) }
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val player = controller
        val state =
            when {
                player == null -> Tile.STATE_UNAVAILABLE
                player.isPlaying -> Tile.STATE_ACTIVE
                player.mediaItemCount > 0 -> Tile.STATE_INACTIVE
                else -> Tile.STATE_UNAVAILABLE
            }
        val statusRes =
            when (state) {
                Tile.STATE_ACTIVE -> R.string.playback_tile_playing
                Tile.STATE_INACTIVE -> R.string.playback_tile_paused
                else -> R.string.playback_tile_unavailable
            }
        tile.state = state
        tile.label = getString(R.string.playback_tile_label)
        tile.contentDescription = getString(statusRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(statusRes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = getString(statusRes)
        }
        tile.updateTile()
    }
}
