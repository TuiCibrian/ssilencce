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
) : BaseAudioProcessor() {
    var currentGain: ReplayGain? = null
        @Synchronized get

        @Synchronized set

    private val gain: Float
        get() =
            currentGain?.let { rg ->
                val adjustDB =
                    when (mode) {
                        ReplayGainMode.Album -> rg.albumGain ?: rg.trackGain
                        ReplayGainMode.Track -> rg.trackGain ?: rg.albumGain
                        ReplayGainMode.Off -> return 0f
                    }
                if (adjustDB == null) {
                    return preAmpGainWithoutTag.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
                }

                var value = adjustDB + preAmpGain
                val peak =
                    when (mode) {
                        ReplayGainMode.Album -> rg.albumPeak ?: rg.trackPeak
                        ReplayGainMode.Track -> rg.trackPeak ?: rg.albumPeak
                        ReplayGainMode.Off -> null
                    }
                if (peak != null) {
                    value = ReplayGainMath.limitByPeak(value, peak)
                }
                value.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
            } ?: 0f

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
    }
}
