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

package com.ssilencce.playback.extractor

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.ParserException
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.TrackOutput

/**
 * Extracts AIFF and AIFF-C streams, converting their sample representation to the little-endian
 * PCM encodings consumed by DefaultAudioSink.
 *
 * Supported AIFF-C compression types are `NONE`/`twos` (big-endian PCM), `sowt` (little-endian
 * PCM), `raw ` (unsigned 8-bit PCM), `fl32`/`FL32` and `fl64`/`FL64`. IMA4, µ-law and A-law are
 * rejected with an explicit unsupported-feature error instead of producing noise.
 */
@UnstableApi
class AiffExtractor : Extractor {
    private lateinit var extractorOutput: ExtractorOutput
    private lateinit var trackOutput: TrackOutput

    private var state = STATE_READ_HEADER
    private var dataStart = C.INDEX_UNSET.toLong()
    private var dataEnd = C.INDEX_UNSET.toLong()
    private var channelCount = 0
    private var sampleRate = 0
    private var bitsPerSample = 0
    private var totalFrames = 0L
    private var durationUs = C.TIME_UNSET
    private var bytesPerFrame = 0
    private var conversion = PcmConversion.COPY
    private var pcmEncoding = C.ENCODING_INVALID
    private var buffer = ByteArray(0)
    private var writer: PcmOutputWriter? = null

    override fun sniff(input: ExtractorInput): Boolean {
        val header = ByteArray(12)
        if (!input.peekFully(header, 0, header.size, true)) return false
        input.resetPeekPosition()
        return ExtractorIo.isAscii(header, 0, "FORM") &&
            (ExtractorIo.isAscii(header, 8, "AIFF") || ExtractorIo.isAscii(header, 8, "AIFC"))
    }

    override fun init(output: ExtractorOutput) {
        extractorOutput = output
        trackOutput = output.track(0, C.TRACK_TYPE_AUDIO)
        output.endTracks()
    }

    override fun seek(
        position: Long,
        timeUs: Long,
    ) {
        writer?.reset(timeUs)
        state =
            when {
                position <= 0L -> STATE_READ_HEADER
                dataStart != C.INDEX_UNSET.toLong() -> STATE_READ_DATA
                else -> STATE_READ_HEADER
            }
    }

    override fun release() = Unit

    override fun read(
        input: ExtractorInput,
        seekPosition: PositionHolder,
    ): Int =
        when (state) {
            STATE_READ_HEADER -> {
                readHeader(input)
                Extractor.RESULT_CONTINUE
            }

            STATE_READ_DATA -> {
                readSampleData(input)
            }

            else -> {
                Extractor.RESULT_END_OF_INPUT
            }
        }

    private fun readHeader(input: ExtractorInput) {
        val header = ByteArray(12)
        input.readFully(header, 0, header.size)
        if (!ExtractorIo.isAscii(header, 0, "FORM")) {
            throw ParserException.createForMalformedContainer("Invalid AIFF form header", null)
        }
        val isAifc = ExtractorIo.isAscii(header, 8, "AIFC")
        if (!isAifc && !ExtractorIo.isAscii(header, 8, "AIFF")) {
            throw ParserException.createForMalformedContainer("Unsupported AIFF form type", null)
        }

        var compressionType = "NONE"
        val chunkHeader = ByteArray(8)
        while (true) {
            if (!input.readFully(chunkHeader, 0, chunkHeader.size, true)) {
                throw ParserException.createForMalformedContainer("Missing AIFF sound data chunk", null)
            }
            val chunkId = ExtractorIo.readAscii(chunkHeader, 0, 4)
            val chunkSize = ExtractorIo.u32be(chunkHeader, 4)
            val chunkStart = input.position
            when (chunkId) {
                "COMM" -> {
                    val comm = ByteArray(minOf(chunkSize, MAX_COMM_CHUNK_SIZE.toLong()).toInt())
                    input.readFully(comm, 0, comm.size)
                    if (comm.size < 18) {
                        throw ParserException.createForMalformedContainer("Truncated AIFF COMM chunk", null)
                    }
                    channelCount = ExtractorIo.u16be(comm, 0)
                    totalFrames = ExtractorIo.u32be(comm, 2)
                    bitsPerSample = ExtractorIo.u16be(comm, 6)
                    sampleRate = ExtractorIo.extendedFloat80(comm, 8).toInt()
                    if (isAifc && comm.size >= 22) {
                        compressionType = ExtractorIo.readAscii(comm, 18, 4)
                    }
                    skipRemaining(input, chunkSize - comm.size)
                }

                "SSND" -> {
                    if (channelCount <= 0 || sampleRate <= 0) {
                        throw ParserException.createForMalformedContainer(
                            "AIFF SSND chunk precedes COMM",
                            null,
                        )
                    }
                    val ssndHeader = ByteArray(8)
                    input.readFully(ssndHeader, 0, ssndHeader.size)
                    val offset = ExtractorIo.u32be(ssndHeader, 0)
                    skipRemaining(input, offset)
                    dataStart = input.position
                    dataEnd =
                        if (chunkSize > 0) {
                            (chunkStart + chunkSize).coerceAtMost(input.length.takeIf { it > 0 } ?: Long.MAX_VALUE)
                        } else {
                            input.length.takeIf { it > 0 } ?: Long.MAX_VALUE
                        }
                    if (dataEnd <= dataStart) {
                        throw ParserException.createForMalformedContainer("Empty AIFF sound data", null)
                    }
                    configureOutput(compressionType)
                    return
                }

                else -> {
                    skipRemaining(input, chunkSize)
                }
            }
            if (chunkSize and 1L != 0L) {
                input.skipFully(1)
            }
        }
    }

    private fun configureOutput(compressionType: String) {
        val compression = compressionType.trim().ifEmpty { "NONE" }
        when (compression.uppercase()) {
            "NONE", "TWOS", "NON", "" -> {
                when (bitsPerSample) {
                    8 -> {
                        conversion = PcmConversion.SIGNED_8_TO_UNSIGNED
                        pcmEncoding = C.ENCODING_PCM_8BIT
                    }

                    16 -> {
                        conversion = PcmConversion.SWAP_16
                        pcmEncoding = C.ENCODING_PCM_16BIT
                    }

                    24 -> {
                        conversion = PcmConversion.INT24_BE_TO_16_LE
                        pcmEncoding = C.ENCODING_PCM_16BIT
                    }

                    32 -> {
                        conversion = PcmConversion.INT32_BE_TO_16_LE
                        pcmEncoding = C.ENCODING_PCM_16BIT
                    }

                    else -> {
                        unsupportedCompression(compression, bitsPerSample)
                    }
                }
            }

            "SOWT" -> {
                when (bitsPerSample) {
                    8 -> {
                        conversion = PcmConversion.SIGNED_8_TO_UNSIGNED
                        pcmEncoding = C.ENCODING_PCM_8BIT
                    }

                    16 -> {
                        conversion = PcmConversion.COPY
                        pcmEncoding = C.ENCODING_PCM_16BIT
                    }

                    24 -> {
                        conversion = PcmConversion.INT24_LE_TO_16_LE
                        pcmEncoding = C.ENCODING_PCM_16BIT
                    }

                    32 -> {
                        conversion = PcmConversion.INT32_LE_TO_16_LE
                        pcmEncoding = C.ENCODING_PCM_16BIT
                    }

                    else -> {
                        unsupportedCompression(compression, bitsPerSample)
                    }
                }
            }

            "RAW" -> {
                if (bitsPerSample == 8) {
                    conversion = PcmConversion.COPY
                    pcmEncoding = C.ENCODING_PCM_8BIT
                } else {
                    unsupportedCompression(compression, bitsPerSample)
                }
            }

            "FL32" -> {
                if (bitsPerSample != 32) unsupportedCompression(compression, bitsPerSample)
                conversion = PcmConversion.SWAP_32
                pcmEncoding = C.ENCODING_PCM_FLOAT
            }

            "FL64" -> {
                if (bitsPerSample != 64) unsupportedCompression(compression, bitsPerSample)
                conversion = PcmConversion.FLOAT64_BE_TO_FLOAT32_LE
                pcmEncoding = C.ENCODING_PCM_FLOAT
            }

            else -> {
                unsupportedCompression(compression, bitsPerSample)
            }
        }

        if (channelCount <= 0 || channelCount > MAX_CHANNELS) {
            throw ParserException.createForUnsupportedContainerFeature(
                "Unsupported AIFF channel count: $channelCount",
            )
        }
        val inputBytesPerSample = if (pcmEncoding == C.ENCODING_PCM_FLOAT && bitsPerSample == 64) 8 else bitsPerSample / 8
        bytesPerFrame = channelCount * inputBytesPerSample
        if (bytesPerFrame <= 0) {
            throw ParserException.createForMalformedContainer("Invalid AIFF frame size", null)
        }
        if (totalFrames <= 0) {
            totalFrames = ((dataEnd - dataStart) / bytesPerFrame).coerceAtLeast(0)
        }
        durationUs = totalFrames * 1_000_000L / sampleRate
        val outputBytesPerFrame = channelCount * pcmBytesPerSample(pcmEncoding)
        val format =
            Format
                .Builder()
                .setContainerMimeType(ExtraMimeTypes.AIFF)
                .setSampleMimeType(MimeTypes.AUDIO_RAW)
                .setPcmEncoding(pcmEncoding)
                .setChannelCount(channelCount)
                .setSampleRate(sampleRate)
                .setAverageBitrate(sampleRate * outputBytesPerFrame * 8)
                .setMaxInputSize(maxOf(outputBytesPerFrame * sampleRate / 10, 64 * 1024))
                .build()
        extractorOutput.seekMap(
            LinearPcmSeekMap(dataStart, dataEnd, bytesPerFrame, sampleRate, totalFrames, durationUs),
        )
        trackOutput.format(format)
        trackOutput.durationUs(durationUs)
        writer = PcmOutputWriter(trackOutput, outputBytesPerFrame, sampleRate)
        writer?.reset(0)
        buffer = ByteArray((sampleRate * bytesPerFrame / 10).coerceIn(16 * 1024, 1 shl 20))
        state = STATE_READ_DATA
    }

    private fun unsupportedCompression(
        compression: String,
        bits: Int,
    ): Nothing =
        throw ParserException.createForUnsupportedContainerFeature(
            "Unsupported AIFF compression '$compression' with $bits bits per sample",
        )

    private fun readSampleData(input: ExtractorInput): Int {
        val bytesLeft = dataEnd - input.position
        if (bytesLeft <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        val bytesToRead = (minOf(bytesLeft, buffer.size.toLong()) / bytesPerFrame * bytesPerFrame).toInt()
        if (bytesToRead <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        var read = 0
        while (read < bytesToRead) {
            val count = input.read(buffer, read, bytesToRead - read)
            if (count == C.RESULT_END_OF_INPUT) break
            read += count
        }
        if (read <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        val outputSize = conversion.convert(buffer, read)
        writer?.write(buffer, outputSize)
        return Extractor.RESULT_CONTINUE
    }

    private fun skipRemaining(
        input: ExtractorInput,
        bytes: Long,
    ) {
        var remaining = bytes
        while (remaining > 0) {
            val chunk = minOf(remaining, 256L * 1024L).toInt()
            input.skipFully(chunk)
            remaining -= chunk
        }
    }

    private fun pcmBytesPerSample(encoding: Int): Int =
        when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }

    private enum class PcmConversion {
        COPY,
        SWAP_16,
        SWAP_32,
        INT24_BE_TO_16_LE,
        INT24_LE_TO_16_LE,
        INT32_BE_TO_16_LE,
        INT32_LE_TO_16_LE,
        SIGNED_8_TO_UNSIGNED,
        FLOAT64_BE_TO_FLOAT32_LE,
        ;

        fun convert(
            buffer: ByteArray,
            size: Int,
        ): Int =
            when (this) {
                COPY -> {
                    size
                }

                SIGNED_8_TO_UNSIGNED -> {
                    for (i in 0 until size) {
                        buffer[i] = (buffer[i].toInt() xor 0x80).toByte()
                    }
                    size
                }

                SWAP_16 -> {
                    var i = 0
                    while (i + 1 < size) {
                        val first = buffer[i]
                        buffer[i] = buffer[i + 1]
                        buffer[i + 1] = first
                        i += 2
                    }
                    size
                }

                INT24_BE_TO_16_LE -> {
                    var input = 0
                    var output = 0
                    while (input + 2 < size) {
                        buffer[output] = buffer[input + 1]
                        buffer[output + 1] = buffer[input]
                        input += 3
                        output += 2
                    }
                    output
                }

                INT24_LE_TO_16_LE -> {
                    var input = 0
                    var output = 0
                    while (input + 2 < size) {
                        buffer[output] = buffer[input + 1]
                        buffer[output + 1] = buffer[input + 2]
                        input += 3
                        output += 2
                    }
                    output
                }

                INT32_BE_TO_16_LE -> {
                    var input = 0
                    var output = 0
                    while (input + 3 < size) {
                        buffer[output] = buffer[input + 1]
                        buffer[output + 1] = buffer[input]
                        input += 4
                        output += 2
                    }
                    output
                }

                INT32_LE_TO_16_LE -> {
                    var input = 0
                    var output = 0
                    while (input + 3 < size) {
                        buffer[output] = buffer[input + 2]
                        buffer[output + 1] = buffer[input + 3]
                        input += 4
                        output += 2
                    }
                    output
                }

                SWAP_32 -> {
                    var i = 0
                    while (i + 3 < size) {
                        val b0 = buffer[i]
                        val b1 = buffer[i + 1]
                        buffer[i] = buffer[i + 3]
                        buffer[i + 1] = buffer[i + 2]
                        buffer[i + 2] = b1
                        buffer[i + 3] = b0
                        i += 4
                    }
                    size
                }

                FLOAT64_BE_TO_FLOAT32_LE -> {
                    val samples = size / 8
                    var output = 0
                    for (sample in 0 until samples) {
                        val offset = sample * 8
                        val value =
                            Double
                                .fromBits(
                                    (ExtractorIo.u32be(buffer, offset) shl 32) or
                                        ExtractorIo.u32be(buffer, offset + 4),
                                ).toFloat()
                        val bits = value.toRawBits()
                        buffer[output] = bits.toByte()
                        buffer[output + 1] = (bits ushr 8).toByte()
                        buffer[output + 2] = (bits ushr 16).toByte()
                        buffer[output + 3] = (bits ushr 24).toByte()
                        output += 4
                    }
                    output
                }
            }
    }

    private companion object {
        const val STATE_READ_HEADER = 0
        const val STATE_READ_DATA = 1
        const val STATE_END = 2
        const val MAX_CHANNELS = 8
        const val MAX_COMM_CHUNK_SIZE = 4 * 1024
    }
}
