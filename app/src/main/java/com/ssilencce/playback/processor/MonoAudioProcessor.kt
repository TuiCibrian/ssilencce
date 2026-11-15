package com.ssilencce.playback.processor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.ssilencce.playback.processor.ByteUtils.getInt24
import com.ssilencce.playback.processor.ByteUtils.putInt24
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Downmixes stereo PCM to a single signal written to both channels.
 *
 * The output channel count is kept identical to the input so the sink never has to be
 * reconfigured while playback is running; toggling the switch therefore takes effect on the
 * next buffer. Mono sources are passed through unchanged and wider layouts are left untouched
 * for the platform downmixer.
 */
@OptIn(UnstableApi::class)
class MonoAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var isMonoEnabled: Boolean = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat = inputAudioFormat

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val buffer = replaceOutputBuffer(remaining)
        if (isMonoEnabled && inputAudioFormat.channelCount == 2) {
            when (inputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> mix16(inputBuffer, buffer)
                C.ENCODING_PCM_24BIT -> mix24(inputBuffer, buffer)
            }
        }
        if (inputBuffer.hasRemaining()) {
            buffer.put(inputBuffer)
        }
        buffer.flip()
    }

    private fun mix16(
        input: ByteBuffer,
        output: ByteBuffer,
    ) {
        input.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)
        while (input.remaining() >= 4) {
            val left = input.short
            val right = input.short
            val mono = ((left + right) / 2).toShort()
            output.putShort(mono)
            output.putShort(mono)
        }
    }

    private fun mix24(
        input: ByteBuffer,
        output: ByteBuffer,
    ) {
        while (input.remaining() >= 6) {
            val left = input.getInt24()
            val right = input.getInt24()
            val mono = (left + right) / 2
            output.putInt24(mono)
            output.putInt24(mono)
        }
    }
}
