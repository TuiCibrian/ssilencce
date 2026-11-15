package com.ssilencce.playback.crossfade

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ssilencce.data.model.replaygain.ReplayGain
import com.ssilencce.playback.PlaybackVolumeController
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Immutable snapshot of the crossfade settings, read on demand so preference changes apply
 * without recreating the controller.
 */
data class CrossfadeConfig(
    val durationMs: Long,
    val manualSkip: Boolean,
    val respectGaplessAlbums: Boolean,
)

/**
 * Runs a dual-player crossfade on top of the single logical main player.
 *
 * While the outgoing track is still playing, a secondary player pre-buffers the next queue
 * item. At the configured boundary both players ramp in opposite, equal-power curves. The
 * main player then commits the queue transition (seek to the next item at the position the
 * secondary already reached), the secondary is faded out and released for reuse. The media
 * session, queue, persistence, widgets and scrobbling only ever see the main player.
 *
 * All methods must be called on the main thread, which is the application looper of both
 * players. The controller only schedules short one-shot messages; it never polls.
 */
@OptIn(UnstableApi::class)
class CrossfadeController(
    private val mainPlayer: ExoPlayer,
    private val volumeController: PlaybackVolumeController,
    private val configProvider: () -> CrossfadeConfig,
    private val isCrossfadeAllowed: () -> Boolean,
    private val isGaplessPlaybackEnabled: () -> Boolean,
    private val baseVolumeProvider: () -> Float,
    private val secondaryPlayerFactory: (audioSessionId: Int) -> CrossfadePlayer,
    private val configureSecondaryProcessors: (CrossfadePlayer) -> Unit,
    private val resolveIncomingGain: (MediaItem) -> Unit,
    private val onCommitRequested: (mediaId: String) -> Unit,
    private val onCrossfadeStateChanged: (active: Boolean) -> Unit = {},
) : Player.Listener {
    private enum class Phase {
        Idle,
        Preloading,
        Fading,
        Committing,
        Handoff,
    }

    private val handler = Handler(Looper.getMainLooper())
    private val incomingGains = HashMap<String, ReplayGain?>()

    private var phase = Phase.Idle
    private var secondary: CrossfadePlayer? = null
    private var secondaryAudioSessionId = C.AUDIO_SESSION_ID_UNSET
    private var targetIndex = C.INDEX_UNSET
    private var targetMediaId: String? = null
    private var fadeOverrideMs: Long? = null
    private var fadeStartTimeMs = 0L
    private var fadeDurationMs = 0L
    private var commitTransitionSeen = false

    private val scheduleRunnable = Runnable { beginPreload() }
    private val prepareTimeoutRunnable = Runnable { onPreparedForFade() }
    private val handoffFallbackRunnable = Runnable { maybeStartHandoff(force = true) }
    private val fadeRunnable =
        object : Runnable {
            override fun run() = tickFade()
        }
    private val handoffRunnable =
        object : Runnable {
            override fun run() = tickHandoff()
        }

    init {
        mainPlayer.addListener(this)
    }

    /** Stops an in-flight crossfade and restores the main player's normal volume. */
    fun cancel() {
        abort()
    }

    fun refresh() {
        if (phase != Phase.Idle) {
            abort()
        }
        scheduleNext()
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        secondaryAudioSessionId = audioSessionId
        secondary?.player?.setAudioSessionId(audioSessionId)
    }

    /**
     * Intercepts a "next" command. Returns true when this controller owns the command and
     * will perform the transition itself; false means the caller must skip normally.
     */
    fun onNextCommand(): Boolean {
        if (Looper.myLooper() != handler.looper) return false
        if (phase != Phase.Idle) {
            // A second skip must respond immediately; drop the pending overlap first.
            abort()
            return false
        }
        val config = configProvider()
        if (!config.manualSkip || config.durationMs <= 0L) return false
        if (!mainPlayer.isPlaying || !isCrossfadeAllowed()) return false
        val currentItem = mainPlayer.currentMediaItem ?: return false
        val nextIndex = mainPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex == mainPlayer.currentMediaItemIndex) return false
        val nextItem = mainPlayer.getMediaItemAt(nextIndex)
        if (shouldSkipForGapless(config, currentItem, nextItem)) return false

        fadeOverrideMs = config.durationMs.coerceAtMost(MAX_MANUAL_SKIP_FADE_MS)
        val started = beginPreload(manual = true, forcedNextIndex = nextIndex)
        if (!started) {
            fadeOverrideMs = null
            return false
        }
        return true
    }

    fun onIncomingGain(
        mediaId: String,
        gain: ReplayGain?,
    ) {
        if (Looper.myLooper() != handler.looper) {
            handler.post { onIncomingGain(mediaId, gain) }
            return
        }
        incomingGains[mediaId] = gain
        if (incomingGains.size > MAX_CACHED_GAINS) {
            incomingGains.clear()
            incomingGains[mediaId] = gain
        }
        val crossfadePlayer = secondary ?: return
        if (targetMediaId == mediaId && (phase == Phase.Preloading || phase == Phase.Fading)) {
            crossfadePlayer.replayGainProcessor.currentGain = gain
        }
    }

    fun release() {
        abort()
        mainPlayer.removeListener(this)
        handler.removeCallbacksAndMessages(null)
        secondary?.release()
        secondary = null
        secondaryAudioSessionId = C.AUDIO_SESSION_ID_UNSET
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (phase == Phase.Committing) {
                maybeStartHandoff(force = false)
            }
            scheduleNext()
            return
        }
        when (phase) {
            Phase.Committing, Phase.Handoff -> {
                // A seek to the incoming item can briefly report not-playing while buffering.
                if (!mainPlayer.playWhenReady) abort()
            }

            Phase.Idle -> {
                Unit
            }

            else -> {
                abort()
                scheduleNext()
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when {
            phase == Phase.Committing -> {
                if (playbackState == Player.STATE_READY) {
                    maybeStartHandoff(force = false)
                }
            }

            phase == Phase.Fading && playbackState == Player.STATE_ENDED -> {
                abort()
                scheduleNext()
            }

            phase == Phase.Preloading && playbackState == Player.STATE_ENDED -> {
                abort()
                scheduleNext()
            }

            phase == Phase.Idle -> {
                scheduleNext()
            }
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (phase == Phase.Committing || phase == Phase.Handoff) {
            if (mediaItem?.mediaId == targetMediaId) {
                commitTransitionSeen = true
                if (phase == Phase.Committing) maybeStartHandoff(force = false)
                return
            }
            abort()
            scheduleNext()
            return
        }
        if (phase != Phase.Idle) {
            abort()
        }
        scheduleNext()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (phase == Phase.Committing || phase == Phase.Handoff) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                newPosition.mediaItem?.mediaId == targetMediaId
            ) {
                commitTransitionSeen = true
                return
            }
            abort()
            scheduleNext()
            return
        }
        if (phase != Phase.Idle) {
            abort()
        }
        scheduleNext()
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        if (phase != Phase.Idle) {
            abort()
        }
        scheduleNext()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        if (phase != Phase.Idle) {
            abort()
        }
        scheduleNext()
    }

    private fun scheduleNext() {
        handler.removeCallbacks(scheduleRunnable)
        if (phase != Phase.Idle) return
        if (!mainPlayer.isPlaying) return
        val config = configProvider()
        if (config.durationMs <= 0L || !isCrossfadeAllowed()) return
        val currentItem = mainPlayer.currentMediaItem ?: return
        val nextIndex = mainPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex == mainPlayer.currentMediaItemIndex) return
        if (shouldSkipForGapless(config, currentItem, mainPlayer.getMediaItemAt(nextIndex))) return
        val duration = mainPlayer.duration
        if (duration <= 0L) return
        val remaining = remainingWallMs()
        if (remaining <= 0L) return
        if (remaining < MIN_REMAINING_MS) return
        val lead = scheduledLeadMs(config, remaining)
        val delayMs = (remaining - config.durationMs - lead).coerceAtLeast(0L)
        handler.postDelayed(scheduleRunnable, delayMs)
    }

    /** Time left before the current item ends, expressed in wall-clock milliseconds. */
    private fun remainingWallMs(): Long {
        val duration = mainPlayer.duration
        if (duration <= 0L) return 0L
        val speed = mainPlayer.playbackParameters.speed.takeIf { it > 0f } ?: 1f
        return ((duration - mainPlayer.currentPosition) / speed).toLong()
    }

    private fun scheduledLeadMs(
        config: CrossfadeConfig,
        remaining: Long,
    ): Long {
        val fade = fadeOverrideMs ?: config.durationMs
        return if (remaining > fade + GUARD_MS) PREPARE_LEAD_MS else 0L
    }

    private fun beginPreload(
        manual: Boolean = false,
        forcedNextIndex: Int = C.INDEX_UNSET,
    ): Boolean {
        if (phase != Phase.Idle) return false
        if (!mainPlayer.isPlaying) return false
        val config = configProvider()
        if (config.durationMs <= 0L || !isCrossfadeAllowed()) return false
        val currentItem = mainPlayer.currentMediaItem ?: return false
        val nextIndex = if (forcedNextIndex != C.INDEX_UNSET) forcedNextIndex else mainPlayer.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex == mainPlayer.currentMediaItemIndex) return false
        val nextItem = mainPlayer.getMediaItemAt(nextIndex)
        if (shouldSkipForGapless(config, currentItem, nextItem)) return false
        if (remainingWallMs() < MIN_REMAINING_MS) return false

        val crossfadePlayer =
            secondary
                ?: try {
                    createSecondaryPlayer()
                } catch (e: RuntimeException) {
                    null
                } ?: return false

        phase = Phase.Preloading
        if (!manual) fadeOverrideMs = null
        targetIndex = nextIndex
        targetMediaId = nextItem.mediaId
        commitTransitionSeen = false
        onCrossfadeStateChanged(true)

        configureSecondaryProcessors(crossfadePlayer)
        val hasResolvedGain = incomingGains.containsKey(nextItem.mediaId)
        crossfadePlayer.replayGainProcessor.currentGain = incomingGains[nextItem.mediaId]
        crossfadePlayer.player.volume = 0f
        crossfadePlayer.player.playbackParameters = mainPlayer.playbackParameters
        crossfadePlayer.player.setMediaItem(nextItem)
        crossfadePlayer.player.prepare()
        crossfadePlayer.player.play()
        if (!hasResolvedGain) {
            resolveIncomingGain(nextItem)
        }
        handler.postDelayed(prepareTimeoutRunnable, PREPARE_TIMEOUT_MS)
        return true
    }

    private fun createSecondaryPlayer(): CrossfadePlayer {
        val crossfadePlayer = secondaryPlayerFactory(secondaryAudioSessionId)
        secondary = crossfadePlayer
        crossfadePlayer.player.addListener(secondaryListener)
        if (secondaryAudioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            val mainSessionId = mainPlayer.audioSessionId
            if (mainSessionId != C.AUDIO_SESSION_ID_UNSET) {
                crossfadePlayer.player.setAudioSessionId(mainSessionId)
                secondaryAudioSessionId = mainSessionId
            }
        }
        return crossfadePlayer
    }

    private fun onPreparedForFade() {
        if (phase != Phase.Preloading) return
        handler.removeCallbacks(prepareTimeoutRunnable)
        if (!mainPlayer.isPlaying) {
            abort()
            scheduleNext()
            return
        }
        val config = configProvider()
        if (config.durationMs <= 0L || !isCrossfadeAllowed()) {
            abort()
            scheduleNext()
            return
        }
        val expectedIndex = mainPlayer.nextMediaItemIndex
        if (expectedIndex != targetIndex ||
            mainPlayer.getMediaItemAt(expectedIndex).mediaId != targetMediaId
        ) {
            abort()
            scheduleNext()
            return
        }
        val remaining = remainingWallMs()
        val requestedFade = fadeOverrideMs ?: config.durationMs
        val fadeMs = requestedFade.coerceAtMost(remaining - GUARD_MS)
        if (fadeMs < MIN_FADE_MS) {
            abort()
            scheduleNext()
            return
        }
        phase = Phase.Fading
        fadeStartTimeMs = SystemClock.elapsedRealtime()
        fadeDurationMs = fadeMs
        fadeRunnable.run()
    }

    private fun tickFade() {
        if (phase != Phase.Fading) return
        val crossfadePlayer = secondary
        if (crossfadePlayer == null) {
            abort()
            scheduleNext()
            return
        }
        val elapsed = SystemClock.elapsedRealtime() - fadeStartTimeMs
        val fraction = (elapsed.toFloat() / fadeDurationMs).coerceIn(0f, 1f)
        val outgoing = cos(fraction * (PI / 2)).toFloat()
        val incoming = sin(fraction * (PI / 2)).toFloat()
        volumeController.setCrossfadeMultiplier(outgoing)
        crossfadePlayer.player.volume = (incoming * baseVolumeProvider()).coerceIn(0f, 1f)
        if (fraction >= 1f) {
            commit()
        } else {
            handler.postDelayed(fadeRunnable, FADE_TICK_MS)
        }
    }

    private fun commit() {
        if (phase != Phase.Fading) return
        val crossfadePlayer =
            secondary ?: run {
                abort()
                scheduleNext()
                return
            }
        val mediaId =
            targetMediaId ?: run {
                abort()
                scheduleNext()
                return
            }
        if (!mainPlayer.isPlaying) {
            abort()
            scheduleNext()
            return
        }
        if (targetIndex == C.INDEX_UNSET ||
            targetIndex != mainPlayer.nextMediaItemIndex ||
            mainPlayer.getMediaItemAt(targetIndex).mediaId != mediaId
        ) {
            abort()
            scheduleNext()
            return
        }
        phase = Phase.Committing
        commitTransitionSeen = false
        volumeController.setCrossfadeMultiplier(0f)
        val positionMs = crossfadePlayer.player.currentPosition.coerceAtLeast(0L)
        onCommitRequested(mediaId)
        mainPlayer.seekTo(targetIndex, positionMs)
        handler.postDelayed(handoffFallbackRunnable, COMMIT_TIMEOUT_MS)
    }

    private fun maybeStartHandoff(force: Boolean) {
        if (phase != Phase.Committing) return
        if (mainPlayer.currentMediaItem?.mediaId != targetMediaId) {
            if (force) {
                abort()
                scheduleNext()
            }
            return
        }
        if (!force && (!commitTransitionSeen || mainPlayer.playbackState != Player.STATE_READY)) {
            return
        }
        handler.removeCallbacks(handoffFallbackRunnable)
        phase = Phase.Handoff
        fadeStartTimeMs = SystemClock.elapsedRealtime()
        handoffRunnable.run()
    }

    private fun tickHandoff() {
        if (phase != Phase.Handoff) return
        val crossfadePlayer = secondary
        val elapsed = SystemClock.elapsedRealtime() - fadeStartTimeMs
        val fraction = (elapsed.toFloat() / HANDOFF_MS).coerceIn(0f, 1f)
        volumeController.setCrossfadeMultiplier(fraction)
        crossfadePlayer?.player?.volume = ((1f - fraction) * baseVolumeProvider()).coerceIn(0f, 1f)
        if (fraction >= 1f) {
            finishCrossfade()
        } else {
            handler.postDelayed(handoffRunnable, HANDOFF_TICK_MS)
        }
    }

    private fun finishCrossfade() {
        handler.removeCallbacks(handoffRunnable)
        handler.removeCallbacks(handoffFallbackRunnable)
        secondary?.player?.pause()
        secondary?.player?.clearMediaItems()
        secondary?.player?.volume = 0f
        volumeController.clearCrossfade()
        phase = Phase.Idle
        targetIndex = C.INDEX_UNSET
        targetMediaId = null
        fadeOverrideMs = null
        commitTransitionSeen = false
        onCrossfadeStateChanged(false)
        scheduleNext()
    }

    private fun abort() {
        if (phase == Phase.Idle) return
        handler.removeCallbacks(scheduleRunnable)
        handler.removeCallbacks(prepareTimeoutRunnable)
        handler.removeCallbacks(fadeRunnable)
        handler.removeCallbacks(handoffRunnable)
        handler.removeCallbacks(handoffFallbackRunnable)
        secondary?.player?.pause()
        secondary?.player?.clearMediaItems()
        secondary?.player?.volume = 0f
        volumeController.clearCrossfade()
        phase = Phase.Idle
        targetIndex = C.INDEX_UNSET
        targetMediaId = null
        fadeOverrideMs = null
        commitTransitionSeen = false
        onCrossfadeStateChanged(false)
    }

    private fun shouldSkipForGapless(
        config: CrossfadeConfig,
        current: MediaItem,
        next: MediaItem,
    ): Boolean =
        config.respectGaplessAlbums &&
            isGaplessPlaybackEnabled() &&
            GaplessAlbumDetector.areConsecutiveAlbumTracks(current, next)

    private val secondaryListener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && phase == Phase.Preloading) {
                    onPreparedForFade()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (phase == Phase.Preloading || phase == Phase.Fading) {
                    abort()
                    scheduleNext()
                }
            }
        }

    private companion object {
        const val PREPARE_LEAD_MS = 900L
        const val PREPARE_TIMEOUT_MS = 1500L
        const val FADE_TICK_MS = 40L
        const val HANDOFF_MS = 160f
        const val HANDOFF_TICK_MS = 20L
        const val COMMIT_TIMEOUT_MS = 3000L
        const val GUARD_MS = 500L
        const val MIN_FADE_MS = 300L
        const val MIN_REMAINING_MS = 800L
        const val MAX_MANUAL_SKIP_FADE_MS = 4000L
        const val MAX_CACHED_GAINS = 256
    }
}
