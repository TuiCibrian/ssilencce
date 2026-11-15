package com.ssilencce.data.local.replaygain

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.ssilencce.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ReplayGainAudioFormat(
    val mimeType: String,
    val sampleRate: Int,
    val channelCount: Int,
    val pcmEncoding: Int,
)

class ReplayGainDecodeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Decodes one MediaStore audio item to interleaved float PCM with the framework extractor and
 * codec. It intentionally does not use the playback pipeline, which is real-time and may be
 * backed by the package-private FFmpeg decoder.
 */
class ReplayGainDecoder(
    private val context: Context,
) {
    suspend fun decode(
        song: Song,
        onFormat: (ReplayGainAudioFormat) -> Unit,
        onSamples: (FloatArray, Int) -> Unit,
    ): ReplayGainAudioFormat {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, song.uri, null)
            val trackIndex = findAudioTrack(extractor)
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val expectedDurationUs =
                if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    inputFormat.getLong(MediaFormat.KEY_DURATION)
                } else {
                    0L
                }
            val encoderDelaySamples = inputFormat.intOrZero(MediaFormat.KEY_ENCODER_DELAY)
            val encoderPaddingSamples = inputFormat.intOrZero(MediaFormat.KEY_ENCODER_PADDING)
            val mimeType =
                inputFormat.getString(MediaFormat.KEY_MIME)
                    ?: throw ReplayGainDecodeException("Audio track has no MIME type")

            codec =
                runCatching { MediaCodec.createDecoderByType(mimeType) }
                    .getOrElse { throw ReplayGainDecodeException("No decoder for $mimeType", it) }
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var outputFormat: ReplayGainAudioFormat? = null
            var inputDone = false
            var outputDone = false
            var idleCount = 0
            var decodedFrames = 0L

            while (!outputDone) {
                currentCoroutineContext().ensureActive()
                var progressed = false

                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        progressed = true
                        val inputBuffer =
                            codec.getInputBuffer(inputIndex)
                                ?: throw ReplayGainDecodeException("Decoder input buffer unavailable")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
                                throw ReplayGainDecodeException("Partial audio frame in ${song.title}")
                            }
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime.coerceAtLeast(0),
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (outputFormat != null) {
                            throw ReplayGainDecodeException("Audio format changed during analysis")
                        }
                        outputFormat =
                            codec.outputFormat.toReplayGainFormat(mimeType).also(onFormat)
                        progressed = true
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Unit
                    }

                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Unit
                    }

                    else -> {
                        if (outputIndex >= 0) {
                            progressed = true
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_PARTIAL_FRAME != 0) {
                                throw ReplayGainDecodeException("Partial decoded frame in ${song.title}")
                            }
                            val format =
                                outputFormat
                                    ?: inputFormat.toReplayGainFormat(mimeType).also {
                                        outputFormat = it
                                        onFormat(it)
                                    }
                            val outputBuffer =
                                codec.getOutputBuffer(outputIndex)
                                    ?: throw ReplayGainDecodeException("Decoder output buffer unavailable")
                            if (bufferInfo.size > 0) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                val samples = outputBuffer.toFloatSamples(format.pcmEncoding)
                                val frameCount = samples.size / format.channelCount
                                if (frameCount > 0) {
                                    onSamples(samples, frameCount)
                                    decodedFrames += frameCount
                                }
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }
                }

                idleCount = if (progressed) 0 else idleCount + 1
                if (idleCount > MAX_IDLE_ITERATIONS) {
                    throw ReplayGainDecodeException("Decoder stalled while reading ${song.title}")
                }
            }

            val finalFormat =
                outputFormat ?: inputFormat.toReplayGainFormat(mimeType).also(onFormat)
            validateDecodedDuration(
                song = song,
                sampleRate = finalFormat.sampleRate,
                expectedDurationUs = expectedDurationUs,
                decodedFrames = decodedFrames,
                encoderDelaySamples = encoderDelaySamples,
                encoderPaddingSamples = encoderPaddingSamples,
            )
            return finalFormat
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: ReplayGainDecodeException) {
            throw exception
        } catch (exception: Exception) {
            throw ReplayGainDecodeException("Couldn't decode ${song.title}", exception)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun validateDecodedDuration(
        song: Song,
        sampleRate: Int,
        expectedDurationUs: Long,
        decodedFrames: Long,
        encoderDelaySamples: Int,
        encoderPaddingSamples: Int,
    ) {
        if (expectedDurationUs <= 0L || sampleRate <= 0) return
        val containerFrames = expectedDurationUs * sampleRate / 1_000_000L
        if (containerFrames <= 0L) return
        // AAC/MP4 container durations include encoder priming and padding, but MediaCodec trims
        // those samples before emitting PCM. Counting them as required audio would reject a
        // complete decode (for example 470 AAC frames with a 49,152-sample delay).
        val trimmedFrames =
            (encoderDelaySamples.toLong() + encoderPaddingSamples.toLong())
                .coerceIn(0L, containerFrames)
        val expectedFrames = containerFrames - trimmedFrames
        if (expectedFrames <= 0L) return
        val toleranceFrames = sampleRate.toLong() * SHORT_DECODE_TOLERANCE_MS / 1_000L
        if (decodedFrames + toleranceFrames < expectedFrames) {
            throw ReplayGainDecodeException(
                "Decoded $decodedFrames of $expectedFrames frames for ${song.title}",
            )
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (index in 0 until extractor.trackCount) {
            val mimeType = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mimeType?.startsWith("audio/") == true) {
                return index
            }
        }
        throw ReplayGainDecodeException("No audio track found")
    }

    private fun MediaFormat.intOrZero(key: String): Int =
        if (containsKey(key)) {
            getInteger(key)
        } else {
            0
        }

    private fun MediaFormat.toReplayGainFormat(mimeType: String): ReplayGainAudioFormat {
        val sampleRate = getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount =
            if (containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                1
            }
        if (sampleRate <= 0) {
            throw ReplayGainDecodeException("Invalid sample rate: $sampleRate")
        }
        if (channelCount !in 1..2) {
            throw ReplayGainDecodeException("Unsupported channel count: $channelCount")
        }
        val pcmEncoding =
            if (containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
        return ReplayGainAudioFormat(
            mimeType = mimeType,
            sampleRate = sampleRate,
            channelCount = channelCount,
            pcmEncoding = pcmEncoding,
        )
    }

    private fun ByteBuffer.toFloatSamples(pcmEncoding: Int): FloatArray {
        order(ByteOrder.nativeOrder())
        return when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val shortBuffer = asShortBuffer()
                FloatArray(shortBuffer.remaining()) {
                    shortBuffer.get() / 32768f
                }
            }

            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floatBuffer = asFloatBuffer()
                FloatArray(floatBuffer.remaining()) {
                    floatBuffer.get()
                }
            }

            AudioFormat.ENCODING_PCM_8BIT -> {
                val bytes = duplicate()
                FloatArray(bytes.remaining()) {
                    ((bytes.get().toInt() and 0xFF) - 128) / 128f
                }
            }

            else -> {
                throw ReplayGainDecodeException("Unsupported PCM encoding: $pcmEncoding")
            }
        }
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val MAX_IDLE_ITERATIONS = 500
        const val SHORT_DECODE_TOLERANCE_MS = 250L
    }
}
