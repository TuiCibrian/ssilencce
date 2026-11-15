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
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import kotlin.math.roundToInt

/**
 * Extracts DSF and DSDIFF (DFF) 1-bit DSD streams and converts them to 88.2 kHz 16-bit PCM.
 *
 * DSF stores one block per channel (block-interleaved, LSB-first); DSDIFF stores one byte per
 * channel in an interleaved stream (MSB-first). DST-compressed DSDIFF cannot be decoded without a
 * DST codec and is rejected with an explicit unsupported-feature error.
 */
@UnstableApi
class DsdExtractor : Extractor {
    private lateinit var extractorOutput: ExtractorOutput
    private lateinit var trackOutput: TrackOutput

    private var state = STATE_INIT
    private var format: DsdFormat? = null
    private var decoders: Array<DsdChannelDecoder> = emptyArray()
    private var writer: PcmOutputWriter? = null
    private var readBuffer = ByteArray(0)
    private var pcmBuffer = ByteArray(0)
    private var channelSamples: Array<IntArray> = emptyArray()

    override fun sniff(input: ExtractorInput): Boolean {
        val header = ByteArray(16)
        if (!input.peekFully(header, 0, header.size, true)) {
            input.resetPeekPosition()
            return false
        }
        input.resetPeekPosition()
        if (ExtractorIo.isAscii(header, 0, "DSD ") && ExtractorIo.u64le(header, 4) == 28L) {
            return true
        }
        return ExtractorIo.isAscii(header, 0, "FRM8") && ExtractorIo.isAscii(header, 12, "DSD ")
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
        decoders.forEach { it.reset() }
        state =
            when {
                position <= 0L -> STATE_INIT
                format != null -> STATE_READ_DATA
                else -> STATE_INIT
            }
    }

    override fun release() = Unit

    override fun read(
        input: ExtractorInput,
        seekPosition: PositionHolder,
    ): Int =
        when (state) {
            STATE_INIT -> {
                val header = ByteArray(16)
                input.readFully(header, 0, header.size)
                format =
                    if (ExtractorIo.isAscii(header, 0, "DSD ")) {
                        readDsf(input, header)
                    } else if (ExtractorIo.isAscii(header, 0, "FRM8")) {
                        readDff(input, header)
                    } else {
                        throw ParserException.createForMalformedContainer("Unknown DSD container", null)
                    }
                configureOutput(format!!)
                Extractor.RESULT_CONTINUE
            }

            STATE_READ_DATA -> {
                readSampleData(input)
            }

            else -> {
                Extractor.RESULT_END_OF_INPUT
            }
        }

    private fun readDsf(
        input: ExtractorInput,
        header: ByteArray,
    ): DsdFormat {
        if (ExtractorIo.u64le(header, 4) != 28L) {
            throw ParserException.createForMalformedContainer("Invalid DSF header", null)
        }
        skipTo(input, 28)
        val fmtHeader = ByteArray(12)
        input.readFully(fmtHeader, 0, fmtHeader.size)
        if (!ExtractorIo.isAscii(fmtHeader, 0, "fmt ")) {
            throw ParserException.createForMalformedContainer("Missing DSF fmt chunk", null)
        }
        val fmtSize = ExtractorIo.u64le(fmtHeader, 4)
        if (fmtSize < 52 || fmtSize > 4096) {
            throw ParserException.createForMalformedContainer("Invalid DSF fmt chunk size", null)
        }
        val fmt = ByteArray((fmtSize - 12).toInt())
        input.readFully(fmt, 0, fmt.size)
        val version = ExtractorIo.u32le(fmt, 0)
        val formatId = ExtractorIo.u32le(fmt, 4)
        if (version != 1L || formatId != 0L) {
            throw ParserException.createForUnsupportedContainerFeature(
                "Unsupported DSF format version/id: $version/$formatId",
            )
        }
        val channelCount = ExtractorIo.u32le(fmt, 12).toInt()
        val dsdRate = ExtractorIo.u32le(fmt, 16).toInt()
        val bitsPerSample = ExtractorIo.u32le(fmt, 20).toInt()
        val sampleCount = ExtractorIo.u64le(fmt, 24)
        val blockSize = ExtractorIo.u32le(fmt, 32).toInt()
        if (channelCount !in 1..8 || dsdRate <= 0 || blockSize <= 0 || sampleCount <= 0) {
            throw ParserException.createForMalformedContainer("Invalid DSF stream properties", null)
        }
        if (bitsPerSample != 1 && bitsPerSample != 8) {
            throw ParserException.createForUnsupportedContainerFeature(
                "Unsupported DSF bit order marker: $bitsPerSample",
            )
        }
        val decimation = chooseDecimation(dsdRate, blockSize * 8)
        val outputRate = dsdRate / decimation
        val totalBytes = sampleCount / 8 * channelCount

        val dataHeader = ByteArray(12)
        input.readFully(dataHeader, 0, dataHeader.size)
        if (!ExtractorIo.isAscii(dataHeader, 0, "data")) {
            throw ParserException.createForMalformedContainer("Missing DSF data chunk", null)
        }
        val dataSize = ExtractorIo.u64le(dataHeader, 4) - 12
        if (dataSize <= 0) {
            throw ParserException.createForMalformedContainer("Empty DSF data chunk", null)
        }
        val dataStart = input.position
        val dataEnd =
            minOf(dataStart + dataSize, dataStart + totalBytes)
                .coerceAtMost(input.length.takeIf { it > 0 } ?: Long.MAX_VALUE)
        if (dataEnd <= dataStart) {
            throw ParserException.createForMalformedContainer("Empty DSF data chunk", null)
        }
        val totalOutputSamples = sampleCount / decimation
        return DsdFormat(
            containerMime = ExtraMimeTypes.DSF,
            layout = DsdLayout.DSF,
            dataStart = dataStart,
            dataEnd = dataEnd,
            channelCount = channelCount,
            dsdRate = dsdRate,
            decimation = decimation,
            outputRate = outputRate,
            blockSize = blockSize,
            lsbFirst = bitsPerSample == 1,
            totalOutputSamples = totalOutputSamples,
            durationUs = totalOutputSamples * 1_000_000L / outputRate,
        )
    }

    private fun readDff(
        input: ExtractorInput,
        header: ByteArray,
    ): DsdFormat {
        var dsdRate = 0
        var channelCount = 0
        var dataStart = -1L
        var dataSize = 0L
        val chunkHeader = ByteArray(12)
        while (true) {
            if (!input.readFully(chunkHeader, 0, chunkHeader.size, true)) break
            val chunkId = ExtractorIo.readAscii(chunkHeader, 0, 4)
            val chunkSize = ExtractorIo.u64be(chunkHeader, 4)
            val chunkStart = input.position
            when (chunkId) {
                "PROP" -> {
                    val formType = ByteArray(4)
                    input.readFully(formType, 0, formType.size)
                    if (!ExtractorIo.isAscii(formType, 0, "SND ")) {
                        throw ParserException.createForMalformedContainer("Invalid DSDIFF PROP chunk", null)
                    }
                    val propEnd = minOf(chunkStart + chunkSize, input.length.takeIf { it > 0 } ?: Long.MAX_VALUE)
                    while (input.position + 12 <= propEnd) {
                        val subHeader = ByteArray(12)
                        input.readFully(subHeader, 0, subHeader.size)
                        val subId = ExtractorIo.readAscii(subHeader, 0, 4)
                        val subSize = ExtractorIo.u64be(subHeader, 4)
                        val subStart = input.position
                        when (subId) {
                            "FS  " -> {
                                val value = ByteArray(4)
                                input.readFully(value, 0, value.size)
                                dsdRate = ExtractorIo.u32be(value, 0).toInt()
                            }

                            "CHNL" -> {
                                val value = ByteArray(2)
                                input.readFully(value, 0, value.size)
                                channelCount = ExtractorIo.u16be(value, 0)
                            }

                            "CMPR" -> {
                                val value = ByteArray(4)
                                input.readFully(value, 0, value.size)
                                val compression = ExtractorIo.readAscii(value, 0, 4)
                                if (compression == "DST ") {
                                    throw ParserException.createForUnsupportedContainerFeature(
                                        "DST-compressed DSDIFF is not supported",
                                    )
                                }
                            }
                        }
                        skipTo(input, subStart + subSize + (subSize and 1L))
                    }
                    skipTo(input, propEnd)
                }

                "DSD " -> {
                    dataStart = chunkStart
                    dataSize = chunkSize
                    break
                }

                else -> {
                    skipTo(input, chunkStart + chunkSize)
                }
            }
            if (chunkSize and 1L != 0L) input.skipFully(1)
        }
        if (dataStart < 0 || dsdRate <= 0 || channelCount !in 1..8) {
            throw ParserException.createForMalformedContainer("Incomplete DSDIFF header", null)
        }
        val dataEnd =
            (if (dataSize > 0) dataStart + dataSize else Long.MAX_VALUE)
                .coerceAtMost(input.length.takeIf { it > 0 } ?: Long.MAX_VALUE)
        if (dataEnd <= dataStart) {
            throw ParserException.createForMalformedContainer("Empty DSDIFF data chunk", null)
        }
        val decimation = chooseDecimation(dsdRate, 0)
        val outputRate = dsdRate / decimation
        val totalBytes = dataEnd - dataStart
        val totalOutputSamples = totalBytes / channelCount * 8 / decimation
        return DsdFormat(
            containerMime = ExtraMimeTypes.DFF,
            layout = DsdLayout.DFF,
            dataStart = dataStart,
            dataEnd = dataEnd,
            channelCount = channelCount,
            dsdRate = dsdRate,
            decimation = decimation,
            outputRate = outputRate,
            blockSize = 0,
            lsbFirst = false,
            totalOutputSamples = totalOutputSamples,
            durationUs = totalOutputSamples * 1_000_000L / outputRate,
        )
    }

    private fun configureOutput(format: DsdFormat) {
        val bytesPerFrame = format.channelCount * 2
        val outputFormat =
            Format
                .Builder()
                .setContainerMimeType(format.containerMime)
                .setSampleMimeType(MimeTypes.AUDIO_RAW)
                .setPcmEncoding(C.ENCODING_PCM_16BIT)
                .setChannelCount(format.channelCount)
                .setSampleRate(format.outputRate)
                .setAverageBitrate(format.outputRate * bytesPerFrame * 8)
                .setMaxInputSize(maxOf(format.outputRate * bytesPerFrame / 10, 64 * 1024))
                .build()
        extractorOutput.seekMap(DsdSeekMap(format))
        trackOutput.format(outputFormat)
        trackOutput.durationUs(format.durationUs)
        writer = PcmOutputWriter(trackOutput, bytesPerFrame, format.outputRate)
        writer?.reset(0)
        decoders = Array(format.channelCount) { DsdChannelDecoder(format.decimation) }
        if (format.layout == DsdLayout.DSF) {
            val blockBytes = format.blockSize * format.channelCount
            readBuffer = ByteArray(blockBytes)
            val samplesPerChannel = format.blockSize * 8 / format.decimation + 1
            channelSamples = Array(format.channelCount) { IntArray(samplesPerChannel) }
            pcmBuffer = ByteArray(samplesPerChannel * format.channelCount * 2)
        } else {
            val groupBytes = format.decimation / 8 * format.channelCount
            readBuffer = ByteArray((64 * 1024 / groupBytes).coerceAtLeast(1) * groupBytes)
            channelSamples = Array(format.channelCount) { IntArray(1) }
            pcmBuffer = ByteArray(readBuffer.size / groupBytes * format.channelCount * 2)
        }
        state = STATE_READ_DATA
    }

    private fun readSampleData(input: ExtractorInput): Int {
        val format = format ?: return Extractor.RESULT_END_OF_INPUT
        val bytesLeft = format.dataEnd - input.position
        if (bytesLeft <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        return when (format.layout) {
            DsdLayout.DSF -> readDsfBlock(input, format, bytesLeft)
            DsdLayout.DFF -> readDffChunk(input, format, bytesLeft)
        }
    }

    private fun readDsfBlock(
        input: ExtractorInput,
        format: DsdFormat,
        bytesLeft: Long,
    ): Int {
        val blockBytes = format.blockSize * format.channelCount
        val bytesToRead = minOf(bytesLeft, blockBytes.toLong()).toInt()
        var read = 0
        while (read < bytesToRead) {
            val count = input.read(readBuffer, read, bytesToRead - read)
            if (count == C.RESULT_END_OF_INPUT) break
            read += count
        }
        if (read <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        val perChannelBytes = read / format.channelCount
        if (perChannelBytes <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        var frames = Int.MAX_VALUE
        for (channel in 0 until format.channelCount) {
            val decoder = decoders[channel]
            val samples = channelSamples[channel]
            var sampleCount = 0
            val start = channel * perChannelBytes
            for (index in 0 until perChannelBytes) {
                val sample = decoder.processByte(readBuffer[start + index].toInt() and 0xFF, !format.lsbFirst)
                if (sample != DsdChannelDecoder.NO_SAMPLE) {
                    samples[sampleCount++] = sample
                }
            }
            frames = minOf(frames, sampleCount)
        }
        if (frames == Int.MAX_VALUE || frames <= 0) {
            return Extractor.RESULT_CONTINUE
        }
        var output = 0
        for (frame in 0 until frames) {
            for (channel in 0 until format.channelCount) {
                val sample = channelSamples[channel][frame]
                pcmBuffer[output++] = sample.toByte()
                pcmBuffer[output++] = (sample shr 8).toByte()
            }
        }
        writer?.write(pcmBuffer, output)
        return Extractor.RESULT_CONTINUE
    }

    private fun readDffChunk(
        input: ExtractorInput,
        format: DsdFormat,
        bytesLeft: Long,
    ): Int {
        val bytesPerChannel = format.decimation / 8
        val groupBytes = bytesPerChannel * format.channelCount
        val bytesToRead = (minOf(bytesLeft, readBuffer.size.toLong()) / groupBytes * groupBytes).toInt()
        if (bytesToRead <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        var read = 0
        while (read < bytesToRead) {
            val count = input.read(readBuffer, read, bytesToRead - read)
            if (count == C.RESULT_END_OF_INPUT) break
            read += count
        }
        if (read <= 0) {
            writer?.flush()
            state = STATE_END
            return Extractor.RESULT_END_OF_INPUT
        }
        val groups = read / groupBytes
        var output = 0
        for (group in 0 until groups) {
            val groupOffset = group * groupBytes
            for (channel in 0 until format.channelCount) {
                var sample = DsdChannelDecoder.NO_SAMPLE
                for (byteIndex in 0 until bytesPerChannel) {
                    val index = groupOffset + byteIndex * format.channelCount + channel
                    val decoded = decoders[channel].processByte(readBuffer[index].toInt() and 0xFF, true)
                    if (decoded != DsdChannelDecoder.NO_SAMPLE) sample = decoded
                }
                if (sample == DsdChannelDecoder.NO_SAMPLE) sample = 0
                pcmBuffer[output++] = sample.toByte()
                pcmBuffer[output++] = (sample shr 8).toByte()
            }
        }
        writer?.write(pcmBuffer, output)
        return Extractor.RESULT_CONTINUE
    }

    private fun skipTo(
        input: ExtractorInput,
        position: Long,
    ) {
        var remaining = position - input.position
        while (remaining > 0) {
            val chunk = minOf(remaining, 256L * 1024L).toInt()
            input.skipFully(chunk)
            remaining -= chunk
        }
    }

    private fun chooseDecimation(
        dsdRate: Int,
        bitsPerBlock: Int,
    ): Int {
        var decimation = (dsdRate / 88_200.0).roundToInt().coerceAtLeast(8)
        decimation = (decimation / 8).coerceAtLeast(1) * 8
        if (bitsPerBlock > 0 && bitsPerBlock % decimation != 0) {
            // Keep block boundaries aligned with whole output samples.
            for (candidate in decimation downTo 8 step 4) {
                if (bitsPerBlock % candidate == 0) {
                    decimation = candidate
                    break
                }
            }
        }
        return decimation
    }

    private enum class DsdLayout {
        DSF,
        DFF,
    }

    private data class DsdFormat(
        val containerMime: String,
        val layout: DsdLayout,
        val dataStart: Long,
        val dataEnd: Long,
        val channelCount: Int,
        val dsdRate: Int,
        val decimation: Int,
        val outputRate: Int,
        val blockSize: Int,
        val lsbFirst: Boolean,
        val totalOutputSamples: Long,
        val durationUs: Long,
    )

    private class DsdSeekMap(
        private val format: DsdFormat,
    ) : SeekMap {
        private val samplesPerBlock =
            if (format.layout == DsdLayout.DSF) {
                (format.blockSize * 8 / format.decimation).coerceAtLeast(1)
            } else {
                0
            }
        private val blockBytes = format.blockSize * format.channelCount
        private val groupBytes = (format.decimation / 8) * format.channelCount

        override fun isSeekable(): Boolean = true

        override fun getDurationUs(): Long = format.durationUs

        override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
            var sample = timeUs.coerceAtLeast(0) * format.outputRate / 1_000_000L
            sample = sample.coerceIn(0, format.totalOutputSamples)
            val position =
                if (format.layout == DsdLayout.DSF) {
                    format.dataStart + (sample / samplesPerBlock) * blockBytes
                } else {
                    format.dataStart + sample * groupBytes
                }
            val clampedPosition = position.coerceAtMost(format.dataEnd)
            val alignedSample =
                if (format.layout == DsdLayout.DSF) {
                    (clampedPosition - format.dataStart) / blockBytes * samplesPerBlock
                } else {
                    (clampedPosition - format.dataStart) / groupBytes
                }
            val actualTimeUs = alignedSample * 1_000_000L / format.outputRate
            return SeekMap.SeekPoints(SeekPoint(actualTimeUs, clampedPosition))
        }
    }

    private companion object {
        const val STATE_INIT = 0
        const val STATE_READ_DATA = 1
        const val STATE_END = 2
    }
}
