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

import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint

/** Container MIME types used by the extra-format extractors. */
@UnstableApi
object ExtraMimeTypes {
    const val AIFF = "audio/x-aiff"
    const val DSF = "audio/x-dsf"
    const val DFF = "audio/x-dff"
    const val APE = "audio/x-ape"
    const val ASF = "audio/x-ms-asf"
    const val WMA = "audio/x-ms-wma"
}

/** Byte readers shared by the extra-format extractors. */
internal object ExtractorIo {
    fun u16be(
        data: ByteArray,
        offset: Int,
    ): Int = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    fun u32be(
        data: ByteArray,
        offset: Int,
    ): Long =
        ((data[offset].toLong() and 0xFF) shl 24) or
            ((data[offset + 1].toLong() and 0xFF) shl 16) or
            ((data[offset + 2].toLong() and 0xFF) shl 8) or
            (data[offset + 3].toLong() and 0xFF)

    fun u64be(
        data: ByteArray,
        offset: Int,
    ): Long = (u32be(data, offset) shl 32) or u32be(data, offset + 4)

    fun u16le(
        data: ByteArray,
        offset: Int,
    ): Int = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

    fun u32le(
        data: ByteArray,
        offset: Int,
    ): Long =
        (data[offset].toLong() and 0xFF) or
            ((data[offset + 1].toLong() and 0xFF) shl 8) or
            ((data[offset + 2].toLong() and 0xFF) shl 16) or
            ((data[offset + 3].toLong() and 0xFF) shl 24)

    fun u64le(
        data: ByteArray,
        offset: Int,
    ): Long = u32le(data, offset) or (u32le(data, offset + 4) shl 32)

    /** Reads an 80-bit IEEE 754 extended-precision float (AIFF sample rate). */
    fun extendedFloat80(
        data: ByteArray,
        offset: Int,
    ): Double {
        val exponent = u16be(data, offset)
        if (exponent == 0 && u64be(data, offset + 2) == 0L) return 0.0
        val sign = if (exponent and 0x8000 != 0) -1.0 else 1.0
        val exp = exponent and 0x7FFF
        val mantissa = u64be(data, offset + 2)
        // Treat the 64-bit mantissa as unsigned: split it to avoid Long's sign bit.
        val fraction =
            (
                (mantissa ushr 32).toDouble() * 4_294_967_296.0 +
                    (mantissa and 0xFFFFFFFFL).toDouble()
            ) / 9.223372036854776E18
        return sign * fraction * Math.pow(2.0, (exp - 16383).toDouble())
    }

    fun isAscii(
        data: ByteArray,
        offset: Int,
        value: String,
    ): Boolean {
        if (offset + value.length > data.size) return false
        for (i in value.indices) {
            if (data[offset + i].toInt() != value[i].code) return false
        }
        return true
    }

    fun readAscii(
        data: ByteArray,
        offset: Int,
        length: Int,
    ): String = String(data, offset, length, Charsets.US_ASCII)

    /** Rounds up to the next even byte, as required by IFF chunk padding. */
    fun paddedSize(size: Long): Long = size + (size and 1L)
}

/**
 * Maps a constant-size PCM frame stream to byte positions.
 *
 * Unlike [androidx.media3.extractor.ConstantBitrateSeekMap] this map knows the exact duration and
 * the byte offset of the first sample, so AIFF files with leading chunks seek accurately.
 */
@UnstableApi
class LinearPcmSeekMap(
    private val dataStart: Long,
    private val dataEnd: Long,
    private val bytesPerFrame: Int,
    private val sampleRate: Int,
    private val totalFrames: Long,
    private val durationUs: Long,
) : SeekMap {
    override fun isSeekable(): Boolean = true

    override fun getDurationUs(): Long = durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        val frame =
            if (totalFrames <= 0) {
                0L
            } else {
                (timeUs.coerceAtLeast(0) * sampleRate / 1_000_000L).coerceIn(0, totalFrames)
            }
        val position = (dataStart + frame * bytesPerFrame).coerceAtMost(dataEnd)
        val actualTimeUs = frame * 1_000_000L / sampleRate
        return SeekMap.SeekPoints(SeekPoint(actualTimeUs, position))
    }
}

/**
 * Accumulates converted PCM and emits ~100 ms samples, mirroring WavExtractor's sample sizing.
 */
@UnstableApi
internal class PcmOutputWriter(
    private val trackOutput: androidx.media3.extractor.TrackOutput,
    private val bytesPerFrame: Int,
    private val sampleRate: Int,
) {
    private val targetBytes =
        (sampleRate.toLong() * bytesPerFrame / TARGET_SAMPLES_PER_SECOND)
            .coerceAtLeast(bytesPerFrame.toLong())
            .coerceAtMost(MAX_SAMPLE_BYTES.toLong())
            .toInt()

    private var pendingBytes = 0
    private var pendingStartFrame = 0L

    fun reset(timeUs: Long) {
        pendingBytes = 0
        pendingStartFrame = timeUs * sampleRate / 1_000_000L
    }

    fun write(
        buffer: ByteArray,
        size: Int,
    ) {
        if (size <= 0) return
        trackOutput.sampleData(
            androidx.media3.common.util
                .ParsableByteArray(buffer, size),
            size,
        )
        pendingBytes += size
        if (pendingBytes >= targetBytes) {
            emit(pendingBytes / bytesPerFrame * bytesPerFrame)
        }
    }

    fun flush() {
        emit(pendingBytes / bytesPerFrame * bytesPerFrame)
    }

    private fun emit(size: Int) {
        if (size <= 0) return
        val offset = pendingBytes - size
        val timeUs = pendingStartFrame * 1_000_000L / sampleRate
        trackOutput.sampleMetadata(
            timeUs,
            androidx.media3.common.C.BUFFER_FLAG_KEY_FRAME,
            size,
            offset,
            null,
        )
        pendingStartFrame += size / bytesPerFrame
        pendingBytes = offset
    }

    private companion object {
        const val TARGET_SAMPLES_PER_SECOND = 10
        const val MAX_SAMPLE_BYTES = 1 shl 20
    }
}
