package com.ssilencce.data.local.audio.clip

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream

/**
 * Frame-aligned, byte-for-byte MP3 trimming.
 *
 * MP3 is not a container that [android.media.MediaMuxer] can write, so the selected range is copied
 * as whole MPEG audio frames. An ID3v2 tag is copied verbatim when present, keeping the usual
 * title/artist metadata in the clip.
 */
internal class Mp3RawTrimmer {
    data class Result(
        val durationMs: Long,
        val bytes: Long,
    )

    fun trim(
        source: InputStream,
        destination: OutputStream,
        startUs: Long,
        endUs: Long,
        checkActive: () -> Unit,
    ): Result {
        val input = PushbackInputStream(BufferedInputStream(source, STREAM_BUFFER_SIZE), PUSHBACK_SIZE)
        val output = BufferedOutputStream(destination, STREAM_BUFFER_SIZE)
        val scratch = ByteArray(STREAM_BUFFER_SIZE)
        val frameBuffer = ByteArray(MAX_FRAME_LENGTH)
        copyId3v2Tag(input, output, scratch)
        var positionUs = 0L
        var firstIncludedStartUs = -1L
        var lastIncludedEndUs = -1L
        var bytes = 0L
        while (true) {
            checkActive()
            val frame = readFrameHeader(input) ?: break
            if (positionUs >= endUs) break
            val bodySize = frame.frameLength - MPEG_HEADER_SIZE
            if (readFully(input, frameBuffer, 0, bodySize) < bodySize) break
            val frameEndUs = positionUs + frame.durationUs
            if (frameEndUs > startUs) {
                // A leading Xing/Info/VBRI frame describes the original file; keeping it would
                // advertise the wrong clip length to players.
                val isVbrHeader = positionUs == 0L && isVbrHeaderFrame(frameBuffer, bodySize)
                if (!isVbrHeader) {
                    output.write(frame.rawHeader)
                    output.write(frameBuffer, 0, bodySize)
                    if (firstIncludedStartUs < 0) {
                        firstIncludedStartUs = positionUs
                    }
                    lastIncludedEndUs = frameEndUs
                    bytes += frame.frameLength
                }
            }
            positionUs = frameEndUs
        }
        output.flush()
        if (firstIncludedStartUs < 0) {
            throw IOException("No MPEG audio frames found in the selected range")
        }
        val durationMs = ((lastIncludedEndUs - firstIncludedStartUs).coerceAtLeast(0L) / 1000L)
        return Result(durationMs, bytes)
    }

    private fun copyId3v2Tag(
        input: PushbackInputStream,
        output: OutputStream,
        scratch: ByteArray,
    ) {
        val header = ByteArray(ID3V2_HEADER_SIZE)
        val read = readFully(input, header)
        val isId3v2 =
            read == ID3V2_HEADER_SIZE &&
                header[0] == 'I'.code.toByte() &&
                header[1] == 'D'.code.toByte() &&
                header[2] == '3'.code.toByte() &&
                (header[3].toInt() and 0xFF) != 0xFF &&
                (header[4].toInt() and 0xFF) != 0xFF
        if (!isId3v2) {
            if (read > 0) {
                input.unread(header, 0, read)
            }
            return
        }
        val flags = header[5].toInt() and 0xFF
        val size = syncSafeSize(header, 6)
        if (size < 0 || size > MAX_ID3V2_SIZE) {
            throw IOException("Invalid ID3v2 tag size")
        }
        output.write(header)
        copyExactly(input, output, size, scratch)
        if ((flags and ID3V2_FOOTER_FLAG) != 0) {
            copyExactly(input, output, ID3V2_FOOTER_SIZE, scratch)
        }
    }

    private fun readFrameHeader(input: PushbackInputStream): Mp3FrameHeader? {
        val raw = ByteArray(MPEG_HEADER_SIZE)
        while (true) {
            val read = readFully(input, raw)
            if (read < MPEG_HEADER_SIZE) return null
            val header = parseFrameHeader(raw)
            if (header != null) return header
            // Move forward one byte and retry; keeps scanning through junk between tags/frames.
            input.unread(raw, 1, MPEG_HEADER_SIZE - 1)
        }
    }

    private fun parseFrameHeader(raw: ByteArray): Mp3FrameHeader? {
        val first = raw[0].toInt() and 0xFF
        val second = raw[1].toInt() and 0xFF
        if (first != 0xFF || (second and 0xE0) != 0xE0) return null

        val versionBits = (second shr 3) and 0x3
        if (versionBits == MPEG_VERSION_RESERVED) return null
        val layerBits = (second shr 1) and 0x3
        if (layerBits == LAYER_RESERVED) return null

        val third = raw[2].toInt() and 0xFF
        val bitrateIndex = (third shr 4) and 0xF
        if (bitrateIndex == BITRATE_FREE || bitrateIndex == BITRATE_INVALID) return null
        val sampleRateIndex = (third shr 2) and 0x3
        if (sampleRateIndex == SAMPLE_RATE_RESERVED) return null
        val padding = (third shr 1) and 0x1

        val mpeg1 = versionBits == MPEG_VERSION_1
        val layer =
            when (layerBits) {
                LAYER_1_BITS -> LAYER_1
                LAYER_2_BITS -> LAYER_2
                else -> LAYER_3
            }
        val bitrateKbps = bitrateTable(mpeg1, layer)[bitrateIndex]
        if (bitrateKbps <= 0) return null
        val sampleRate = sampleRateTable(versionBits)[sampleRateIndex]
        if (sampleRate <= 0) return null

        val frameLength =
            when (layer) {
                LAYER_1 -> ((12 * bitrateKbps * 1000 / sampleRate) + padding) * 4
                LAYER_2 -> (144 * bitrateKbps * 1000 / sampleRate) + padding
                else -> ((if (mpeg1) 144 else 72) * bitrateKbps * 1000 / sampleRate) + padding
            }
        if (frameLength !in MPEG_HEADER_SIZE..MAX_FRAME_LENGTH) return null

        val samplesPerFrame =
            when (layer) {
                LAYER_1 -> 384
                LAYER_2 -> 1152
                else -> if (mpeg1) 1152 else 576
            }
        val durationUs = samplesPerFrame * 1_000_000L / sampleRate
        return Mp3FrameHeader(raw, frameLength, durationUs)
    }

    private fun bitrateTable(
        mpeg1: Boolean,
        layer: Int,
    ): IntArray =
        if (mpeg1) {
            when (layer) {
                LAYER_1 -> MPEG1_LAYER1_BITRATES
                LAYER_2 -> MPEG1_LAYER2_BITRATES
                else -> MPEG1_LAYER3_BITRATES
            }
        } else {
            when (layer) {
                LAYER_1 -> MPEG2_LAYER1_BITRATES
                else -> MPEG2_LAYER2_AND_3_BITRATES
            }
        }

    private fun sampleRateTable(versionBits: Int): IntArray =
        when (versionBits) {
            MPEG_VERSION_1 -> MPEG1_SAMPLE_RATES
            MPEG_VERSION_2 -> MPEG2_SAMPLE_RATES
            else -> MPEG25_SAMPLE_RATES
        }

    private fun isVbrHeaderFrame(
        body: ByteArray,
        length: Int,
    ): Boolean {
        val limit = minOf(length, VBR_HEADER_SCAN_SIZE)
        for (index in 0..limit - 4) {
            if ((
                    body[index] == 'X'.code.toByte() &&
                        body[index + 1] == 'i'.code.toByte() &&
                        body[index + 2] == 'n'.code.toByte() &&
                        body[index + 3] == 'g'.code.toByte()
                ) ||
                (
                    body[index] == 'I'.code.toByte() &&
                        body[index + 1] == 'n'.code.toByte() &&
                        body[index + 2] == 'f'.code.toByte() &&
                        body[index + 3] == 'o'.code.toByte()
                ) ||
                (
                    body[index] == 'V'.code.toByte() &&
                        body[index + 1] == 'B'.code.toByte() &&
                        body[index + 2] == 'R'.code.toByte() &&
                        body[index + 3] == 'I'.code.toByte()
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun readFully(
        input: InputStream,
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size,
    ): Int {
        var total = 0
        while (total < length) {
            val read = input.read(buffer, offset + total, length - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    private fun copyExactly(
        input: InputStream,
        output: OutputStream,
        count: Int,
        scratch: ByteArray,
    ) {
        var remaining = count
        while (remaining > 0) {
            val read = input.read(scratch, 0, minOf(remaining, scratch.size))
            if (read < 0) throw IOException("Unexpected end of MP3 stream")
            output.write(scratch, 0, read)
            remaining -= read
        }
    }

    private fun syncSafeSize(
        header: ByteArray,
        offset: Int,
    ): Int =
        ((header[offset].toInt() and 0x7F) shl 21) or
            ((header[offset + 1].toInt() and 0x7F) shl 14) or
            ((header[offset + 2].toInt() and 0x7F) shl 7) or
            (header[offset + 3].toInt() and 0x7F)

    private data class Mp3FrameHeader(
        val rawHeader: ByteArray,
        val frameLength: Int,
        val durationUs: Long,
    )

    private companion object {
        const val STREAM_BUFFER_SIZE = 64 * 1024
        const val PUSHBACK_SIZE = 16
        const val MPEG_HEADER_SIZE = 4
        const val MAX_FRAME_LENGTH = 8192
        const val ID3V2_HEADER_SIZE = 10
        const val ID3V2_FOOTER_SIZE = 10
        const val ID3V2_FOOTER_FLAG = 0x10
        const val MAX_ID3V2_SIZE = 256 * 1024 * 1024
        const val VBR_HEADER_SCAN_SIZE = 40

        const val MPEG_VERSION_RESERVED = 1
        const val MPEG_VERSION_1 = 3
        const val MPEG_VERSION_2 = 2
        const val LAYER_RESERVED = 0
        const val LAYER_1_BITS = 3
        const val LAYER_2_BITS = 2
        const val LAYER_1 = 1
        const val LAYER_2 = 2
        const val LAYER_3 = 3
        const val BITRATE_FREE = 0
        const val BITRATE_INVALID = 15
        const val SAMPLE_RATE_RESERVED = 3

        val MPEG1_LAYER1_BITRATES =
            intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 0)
        val MPEG1_LAYER2_BITRATES =
            intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 0)
        val MPEG1_LAYER3_BITRATES =
            intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
        val MPEG2_LAYER1_BITRATES =
            intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0)
        val MPEG2_LAYER2_AND_3_BITRATES =
            intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0)
        val MPEG1_SAMPLE_RATES = intArrayOf(44100, 48000, 32000, 0)
        val MPEG2_SAMPLE_RATES = intArrayOf(22050, 24000, 16000, 0)
        val MPEG25_SAMPLE_RATES = intArrayOf(11025, 12000, 8000, 0)
    }
}
