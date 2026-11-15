package com.ssilencce.playback

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs short volume ramps for fade-in on play and fade-out on pause. All player commands
 * stay on the application thread (ExoPlayer's threading requirement); the coroutine only
 * updates the shared [PlaybackVolumeController] multiplier.
 */
class PlaybackFadeController(
    private val scope: CoroutineScope,
    private val volumeController: PlaybackVolumeController,
    private val fadeInDurationMs: () -> Long,
    private val fadeOutDurationMs: () -> Long,
) {
    private var fadeJob: Job? = null
    private var pendingPause: (() -> Unit)? = null
    private var fadeInPending = false

    /**
     * Called before a play command is applied.
     *
     * @param alreadyPlaying true when the player is still producing audio (for example while
     * a pause fade-out is pending); the volume is ramped back up instead of starting silent.
     */
    fun onPlayRequested(alreadyPlaying: Boolean) {
        val durationMs = fadeInDurationMs()
        val from = volumeController.currentFadeMultiplier
        stopRamp()
        if (durationMs <= 0L) {
            volumeController.clearFade()
            return
        }
        if (alreadyPlaying) {
            if (from < 1f) {
                ramp(from = from, to = 1f, durationMs = durationMs, onEnd = null)
            } else {
                volumeController.clearFade()
            }
            return
        }
        fadeInPending = true
        volumeController.setFadeMultiplier(0f)
    }

    /** Called when the player actually starts producing audio. */
    fun onPlaybackStarted() {
        if (!fadeInPending) return
        fadeInPending = false
        val durationMs = fadeInDurationMs()
        if (durationMs <= 0L) {
            volumeController.clearFade()
            return
        }
        ramp(
            from = volumeController.currentFadeMultiplier,
            to = 1f,
            durationMs = durationMs,
            onEnd = null,
        )
    }

    /**
     * Called before a pause command is applied. Returns true when the pause is deferred
     * (or committed synchronously) by this controller.
     */
    fun requestPause(commit: () -> Unit): Boolean {
        val durationMs = fadeOutDurationMs()
        if (durationMs <= 0L || volumeController.currentFadeMultiplier <= 0f) {
            stopRamp()
            commit()
            volumeController.clearFade()
            return true
        }
        // Repeated pause requests must not restart the ramp or commit early.
        if (pendingPause != null) return true
        fadeInPending = false
        val from = volumeController.currentFadeMultiplier
        pendingPause = commit
        ramp(from = from, to = 0f, durationMs = durationMs) {
            val action = pendingPause
            pendingPause = null
            action?.invoke()
            volumeController.clearFade()
        }
        return true
    }

    /** Fade-out used by the sleep timer; the caller supplies its own final pause action. */
    fun fadeOut(
        durationMs: Long,
        commit: () -> Unit,
    ) {
        cancel()
        if (durationMs <= 0L) {
            commit()
            return
        }
        ramp(
            from = volumeController.currentFadeMultiplier,
            to = 0f,
            durationMs = durationMs,
            onEnd = {
                commit()
                volumeController.clearFade()
            },
        )
    }

    /** Commits a deferred pause immediately, e.g. when the media item changes. */
    fun commitPendingPause() {
        val action = pendingPause ?: return
        stopRamp()
        action.invoke()
        volumeController.clearFade()
    }

    /** Cancels any running ramp and restores the non-faded volume. */
    fun cancel() {
        stopRamp()
        volumeController.clearFade()
    }

    /** Commits a deferred pause, if any, and stops ramping. Used when the service dies. */
    fun release() {
        val action = pendingPause
        stopRamp()
        action?.invoke()
        volumeController.clearFade()
    }

    private fun stopRamp() {
        fadeJob?.cancel()
        fadeJob = null
        pendingPause = null
        fadeInPending = false
    }

    private fun ramp(
        from: Float,
        to: Float,
        durationMs: Long,
        onEnd: (() -> Unit)?,
    ) {
        fadeJob?.cancel()
        fadeJob =
            scope.launch {
                val start = SystemClock.elapsedRealtime()
                while (true) {
                    val elapsed = SystemClock.elapsedRealtime() - start
                    val fraction = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    volumeController.setFadeMultiplier(from + (to - from) * fraction)
                    if (fraction >= 1f) break
                    delay(FADE_STEP_MS)
                }
                onEnd?.invoke()
            }
    }

    private companion object {
        const val FADE_STEP_MS = 40L
    }
}
