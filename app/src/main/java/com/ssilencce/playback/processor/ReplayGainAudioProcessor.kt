package com.ssilencce.playback.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.ssilencce.data.local.replaygain.ReplayGainMath
import com.ssilencce.data.model.replaygain.ReplayGain
import com.ssilencce.data.model.replaygain.ReplayGainMode
import com.ssilencce.playback.processor.ByteUtils.getInt24
import com.ssilencce.playback.processor.ByteUtils.putInt24
import java.nio.ByteBuffer
import kotlin.math.pow

@OptIn(UnstableApi::class)
class ReplayGainAudioProcessor(
    var mode: ReplayGainMode = ReplayGainMode.Off,
    var preAmpGain: Float = 0f,
    var preAmpGainWithoutTag: Float = 0f,
    @Volatile var useEmbeddedTags: Boolean = true,
    @Volatile var constantVolume: Boolean = false,
    @Volatile var preventClipping: Boolean = true,
    @Volatile var headroomDb: Float = 0f,
) : BaseAudioProcessor() {
    var currentGain: ReplayGain? = null
        @Synchronized get

        @Synchronized set

    private val gain: Float
        get() {
            if (mode == ReplayGainMode.Off) return 0f

            val headroom = headroomDb.coerceIn(0f, MAX_HEADROOM_DB)
            if (constantVolume) {
                return (preAmpGain - headroom).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            }

            val replayGain = if (useEmbeddedTags) currentGain else null
            if (replayGain == null) {
                return (preAmpGainWithoutTag - headroom).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            }

            val adjustDB =
                when (mode) {
                    ReplayGainMode.Album -> replayGain.albumGain ?: replayGain.trackGain
                    ReplayGainMode.Track -> replayGain.trackGain ?: replayGain.albumGain
                    ReplayGainMode.Off -> null
                }
            if (adjustDB == null) {
                return (preAmpGainWithoutTag - headroom).coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            }

            var value = adjustDB + preAmpGain - headroom
            if (preventClipping) {
                val peak =
                    when (mode) {
                        ReplayGainMode.Album -> replayGain.albumPeak ?: replayGain.trackPeak
                        ReplayGainMode.Track -> replayGain.trackPeak ?: replayGain.albumPeak
                        ReplayGainMode.Off -> null
                    }
                if (peak != null) {
                    value = ReplayGainMath.limitByPeak(value, peak)
                }
            }
            return value.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_24BIT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (gain != 0.0f) {
            val size = inputBuffer.remaining()
            val buffer = replaceOutputBuffer(size)
            val delta = 10.0.pow(gain / 20.0)

            when (outputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val sample = inputBuffer.short
                        val scaled =
                            (sample * delta)
                                .toInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        buffer.putShort(scaled)
                    }
                }

                C.ENCODING_PCM_24BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val sample = inputBuffer.getInt24()
                        val scaled =
                            (sample * delta)
                                .toInt()
                                .coerceIn(ByteUtils.Int24_MIN_VALUE, ByteUtils.Int24_MAX_VALUE)
                        buffer.putInt24(scaled)
                    }
                }

                else -> { /* No-op */ }
            }

            inputBuffer.position(inputBuffer.limit())
            buffer.flip()
        } else {
            val remaining = inputBuffer.remaining()
            if (remaining > 0) {
                replaceOutputBuffer(remaining).put(inputBuffer).flip()
            }
        }
    }

    private companion object {
        const val MIN_GAIN_DB = -24f
        const val MAX_GAIN_DB = 24f
        const val MAX_HEADROOM_DB = 24f
    }
}
