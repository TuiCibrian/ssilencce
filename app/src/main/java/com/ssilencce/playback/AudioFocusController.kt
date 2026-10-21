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

package com.ssilencce.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.media3.common.Player
import com.ssilencce.util.AudioFocusMode
import com.ssilencce.util.IGNORE_AUDIO_FOCUS
import com.ssilencce.util.Preferences
import java.util.UUID

/**
 * Single owner of audio focus for playback. ExoPlayer's built-in focus handling is disabled so
 * the four user-visible modes can be implemented deterministically, including the ask-before-
 * taking mode and resume-after-call.
 */
class AudioFocusController(
    context: Context,
    private val player: Player,
    private val preferences: SharedPreferences,
    private val onPendingAskChanged: (String?) -> Unit,
) {
    private val audioManager = requireNotNull(context.getSystemService<AudioManager>())
    private val audioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private var hasFocus = false
    var pendingAskId: String? = null
        private set
    private val pendingAsk get() = pendingAskId != null
    private var transientLoss = false
    private var resumeOnGain = false
    private var internalFocusPause = false
    private var ducked = false

    private val focusRequest =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { change -> onFocusChanged(change) }
            .build()

    val isInTransientLoss: Boolean
        get() = transientLoss

    fun onPlayWhenReadyChanged(
        playWhenReady: Boolean,
        reason: Int,
    ) {
        if (playWhenReady) {
            internalFocusPause = false
            requestFocusIfNeeded()
        } else {
            // Consume an internal-pause marker on the next state callback, regardless of the
            // reported reason, so a marker can never survive into a later user pause.
            val wasInternalPause = internalFocusPause
            internalFocusPause = false
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST && !wasInternalPause) {
                resumeOnGain = false
            }
            if (!transientLoss && !pendingAsk) {
                abandonFocus()
            }
        }
    }

    fun onUserPause() {
        resumeOnGain = false
        internalFocusPause = false
        clearPendingAsk()
        // An already-paused player emits no playWhenReady callback, so explicit no-op pause
        // must release focus itself when no automatic resume remains armed.
        if (!player.playWhenReady) abandonFocus()
    }

    fun onPlaybackIntent(playWhenReady: Boolean): Boolean {
        if (!playWhenReady) onUserPause()
        if (playWhenReady && transientLoss) resumeOnGain = false
        // A repeated play keeps the original request alive, including queued controller plays.
        return !playWhenReady || !pendingAsk
    }

    fun confirmPendingAsk(requestId: String): Boolean {
        if (requestId != pendingAskId) return false
        clearPendingAsk()
        if (requestFocus()) {
            player.play()
            return true
        }
        return false
    }

    fun cancelPendingAsk(requestId: String): Boolean {
        if (requestId != pendingAskId) return false
        clearPendingAsk()
        resumeOnGain = false
        player.pause()
        return true
    }

    fun onModeChanged() {
        val mode = Preferences.audioFocusMode
        if (mode != AudioFocusMode.Ask) {
            clearPendingAsk()
        }
        if (mode == AudioFocusMode.Ignore) {
            abandonFocus()
        } else if (player.playWhenReady && !hasFocus) {
            requestFocusIfNeeded()
        }
    }

    fun release() {
        abandonFocus()
    }

    private fun requestFocusIfNeeded() {
        val mode = Preferences.audioFocusMode
        if (mode == AudioFocusMode.Ignore) {
            abandonFocus()
            return
        }
        if (pendingAsk) {
            // A queued user play must not bypass the existing confirmation or produce a second
            // prompt. Keep playback paused until the original Ask is confirmed or cancelled.
            if (player.playWhenReady) {
                player.pause()
            }
            return
        }
        if (hasFocus && !transientLoss) return
        if (mode == AudioFocusMode.Ask && audioManager.isMusicActive()) {
            pendingAskId = UUID.randomUUID().toString()
            player.pause()
            onPendingAskChanged(pendingAskId)
            return
        }
        if (!requestFocus()) {
            player.pause()
        }
    }

    private fun requestFocus(): Boolean {
        val granted =
            audioManager.requestAudioFocus(focusRequest) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        hasFocus = granted
        if (granted) {
            transientLoss = false
        } else {
            resumeOnGain = false
        }
        return granted
    }

    private fun abandonFocus() {
        if (hasFocus) {
            audioManager.abandonAudioFocusRequest(focusRequest)
            hasFocus = false
        }
        clearPendingAsk()
        restoreVolume()
        transientLoss = false
        resumeOnGain = false
        internalFocusPause = false
    }

    private fun clearPendingAsk() {
        if (pendingAskId != null) {
            pendingAskId = null
            onPendingAskChanged(null)
        }
    }

    private fun onFocusChanged(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                transientLoss = false
                restoreVolume()
                if (resumeOnGain) {
                    resumeOnGain = false
                    player.play()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false
                transientLoss = false
                resumeOnGain = false
                restoreVolume()
                player.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                transientLoss = true
                val mode = Preferences.audioFocusMode
                val wasPlayWhenReady = player.playWhenReady
                resumeOnGain = wasPlayWhenReady && mode != AudioFocusMode.Ignore
                if (wasPlayWhenReady) {
                    internalFocusPause = true
                    player.pause()
                    // No state change means no callback will consume the marker.
                    if (player.playWhenReady) {
                        internalFocusPause = false
                    }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (!ducked) {
                    ducked = true
                    player.volume = DUCK_VOLUME
                }
            }
        }
    }

    private fun restoreVolume() {
        if (ducked) {
            ducked = false
            player.volume = 1f
        }
    }

    companion object {
        private const val DUCK_VOLUME = 0.2f

        fun isIgnored(preferences: SharedPreferences): Boolean =
            preferences.getBoolean(IGNORE_AUDIO_FOCUS, false) ||
                Preferences.audioFocusMode == AudioFocusMode.Ignore
    }
}
