package com.ssilencce.playback

import android.os.SystemClock
import com.ssilencce.util.AppVisibility
import com.ssilencce.util.Preferences

/**
 * Tracks the pause cause and recent audio-route connections so connect/auto-play policies can
 * be evaluated without guessing from transient player callbacks. All methods run on the
 * playback service main thread.
 */
class ResumeAutoplayPolicy(
    private val isUiVisible: () -> Boolean = { AppVisibility.isUiVisible },
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) {
    private var pausedByDisconnect = false
    private var lastRouteConnectedAt = Long.MIN_VALUE

    fun onPlaybackStarted() {
        pausedByDisconnect = false
    }

    fun onUserPause() {
        pausedByDisconnect = false
    }

    fun onDisconnectPause(playbackWasActive: Boolean) {
        pausedByDisconnect = playbackWasActive
    }

    fun onRouteConnected() {
        lastRouteConnectedAt = elapsedRealtime()
    }

    fun canResumeOnConnect(): Boolean = !Preferences.isResumeOnlyIfPausedByDisconnect() || pausedByDisconnect

    fun consumeResumeOnConnect() {
        pausedByDisconnect = false
    }

    /**
     * "Prevent unwanted autoplay" rejects play requests from non-app media controllers. The
     * standard variant only covers the short window after a device connects (the usual source
     * of spurious car/Bluetooth play commands); strict mode also blocks them while the app UI
     * is not visible, leaving only the app and its notification able to start playback.
     */
    fun shouldBlockExternalPlay(): Boolean =
        when {
            !Preferences.preventUnwantedAutoplay -> false
            Preferences.preventUnwantedAutoplayStrict -> !isUiVisible()
            lastRouteConnectedAt == Long.MIN_VALUE -> false
            else -> elapsedRealtime() - lastRouteConnectedAt <= AUTOPLAY_GUARD_WINDOW_MS
        }

    companion object {
        const val AUTOPLAY_GUARD_WINDOW_MS = 5_000L
    }
}
