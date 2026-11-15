package com.ssilencce.playback.crossfade

import android.content.Context
import android.os.HandlerThread
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import com.ssilencce.playback.extractor.ExtraExtractorsFactory
import com.ssilencce.playback.processor.BalanceAudioProcessor
import com.ssilencce.playback.processor.MonoAudioProcessor
import com.ssilencce.playback.processor.ReplayGainAudioProcessor
import com.ssilencce.playback.renderer.AlacWorkaroundCodecSelector
import com.ssilencce.playback.renderer.SsilencceRenderersFactory

/**
 * A secondary player used only to play the incoming track during a crossfade. It mirrors the
 * main player's renderer stack and shares its audio session id so equalizer effects apply to
 * both players during the overlap. It never owns the queue, media session, audio focus or
 * wake lock; the [com.ssilencce.playback.PlaybackService] keeps all of those on the main player.
 */
@OptIn(UnstableApi::class)
class CrossfadePlayer private constructor(
    val player: ExoPlayer,
    val balanceProcessor: BalanceAudioProcessor,
    val monoProcessor: MonoAudioProcessor,
    val replayGainProcessor: ReplayGainAudioProcessor,
    private val playbackThread: HandlerThread,
) {
    fun release() {
        player.release()
        playbackThread.quitSafely()
    }

    companion object {
        fun create(
            context: Context,
            audioSessionId: Int,
            enableFloatOutput: Boolean,
            skipSilence: Boolean,
            mp3IndexSeeking: Boolean,
            isGaplessEnabled: () -> Boolean,
        ): CrossfadePlayer {
            val balanceProcessor = BalanceAudioProcessor()
            val monoProcessor = MonoAudioProcessor()
            val replayGainProcessor = ReplayGainAudioProcessor()
            val playbackThread =
                HandlerThread("Ssilencce-Crossfade", Process.THREAD_PRIORITY_AUDIO).apply {
                    start()
                }
            val player =
                ExoPlayer
                    .Builder(context)
                    // The main player owns the wake lock and audio focus.
                    .setWakeMode(C.WAKE_MODE_NONE)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        false,
                    ).setRenderersFactory(
                        SsilencceRenderersFactory(
                            context,
                            balanceProcessor,
                            monoProcessor,
                            replayGainProcessor,
                            isGaplessEnabled = isGaplessEnabled,
                        ).setEnableAudioFloatOutput(enableFloatOutput)
                            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                            .setMediaCodecSelector(AlacWorkaroundCodecSelector())
                            .setEnableDecoderFallback(true),
                    ).setMediaSourceFactory(
                        DefaultMediaSourceFactory(
                            context,
                            ExtraExtractorsFactory(
                                DefaultExtractorsFactory()
                                    .setConstantBitrateSeekingEnabled(true)
                                    .also {
                                        if (mp3IndexSeeking) {
                                            it.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
                                        }
                                    },
                            ),
                        ),
                    ).setSkipSilenceEnabled(skipSilence)
                    .setHandleAudioBecomingNoisy(false)
                    .setPlaybackLooper(playbackThread.looper)
                    .build()
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                player.setAudioSessionId(audioSessionId)
            }
            player.volume = 0f
            return CrossfadePlayer(
                player = player,
                balanceProcessor = balanceProcessor,
                monoProcessor = monoProcessor,
                replayGainProcessor = replayGainProcessor,
                playbackThread = playbackThread,
            )
        }
    }
}
