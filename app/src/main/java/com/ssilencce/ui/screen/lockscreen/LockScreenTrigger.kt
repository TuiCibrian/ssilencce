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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.ssilencce.util.Preferences

/**
 * In-memory playback state shared by [com.ssilencce.playback.PlaybackService] and the lock
 * screen player. It lets the player honor the "skip when paused for a minute" option without
 * persisting anything or polling the service.
 */
object LockScreenPlaybackClock {
    private var active = false
    private var pausedSinceMillis = UNKNOWN

    @Synchronized
    fun onPlaybackIntentChanged(playWhenReady: Boolean) {
        if (playWhenReady) {
            active = true
            pausedSinceMillis = UNKNOWN
        } else {
            if (active || pausedSinceMillis == UNKNOWN) {
                pausedSinceMillis = SystemClock.elapsedRealtime()
            }
            active = false
        }
    }

    @Synchronized
    fun pausedForMillis(now: Long = SystemClock.elapsedRealtime()): Long = if (active || pausedSinceMillis == UNKNOWN) 0L else (now - pausedSinceMillis).coerceAtLeast(0L)

    @get:Synchronized
    val isActive: Boolean
        get() = active

    private const val UNKNOWN = -1L
}

/**
 * Decides whether a lock screen player launch should be allowed to continue. Preview launches
 * bypass every condition so the user can inspect the screen from Settings.
 */
object LockScreenLaunchPolicy {
    const val PAUSED_GRACE_MS = 60_000L

    fun shouldShow(
        context: Context,
        preview: Boolean,
    ): Boolean {
        if (preview) return true
        if (!Preferences.lockScreenPlayerEnabled) return false
        if (Preferences.lockScreenSkipWhenUnlocked && !context.isKeyguardLocked()) return false
        if (Preferences.lockScreenSkipWhenPaused &&
            !LockScreenPlaybackClock.isActive &&
            LockScreenPlaybackClock.pausedForMillis() >= PAUSED_GRACE_MS
        ) {
            return false
        }
        return true
    }

    private fun Context.isKeyguardLocked(): Boolean = (getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isKeyguardLocked == true
}

/**
 * Registered by the playback service only while playback is active, so the screen-off
 * broadcast costs nothing when the queue is idle or paused.
 */
class LockScreenScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != Intent.ACTION_SCREEN_OFF) return
        if (!Preferences.lockScreenPlayerEnabled) return
        if (LockScreenPlayerActivity.isShowing) return
        LockScreenPlayerActivity.launch(context, preview = false)
    }
}
