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

package com.ssilencce.data.local.extra

import com.ssilencce.playback.extractor.ExtraFormatKind
import com.ssilencce.playback.extractor.ExtractorIo
import java.io.File
import java.io.RandomAccessFile

/**
 * Minimal header reader used only when TagLib cannot report a duration for an extra-format file.
 * It reads a few dozen bytes per file, so a scan never opens the audio data itself.
 */
internal object ExtraFormatFileProbe {
    fun durationMs(
        file: File,
        kind: ExtraFormatKind,
    ): Long? =
        runCatching {
            RandomAccessFile(file, "r").use { input ->
                when (kind) {
                    ExtraFormatKind.AIFF -> aiffDurationMs(input)
                    ExtraFormatKind.DSF -> dsfDurationMs(input)
                    ExtraFormatKind.DFF -> dffDurationMs(input)
                    else -> null
                }
            }
        }.getOrNull()

    private fun aiffDurationMs(input: RandomAccessFile): Long? {
        val header = ByteArray(12)
        input.readFully(header)
        if (!ExtractorIo.isAscii(header, 0, "FORM")) return null
        val chunkHeader = ByteArray(8)
        while (true) {
            if (input.read(chunkHeader) != chunkHeader.size) return null
            val id = ExtractorIo.readAscii(chunkHeader, 0, 4)
            val size = ExtractorIo.u32be(chunkHeader, 4)
            if (id == "COMM") {
                val comm = ByteArray(minOf(size, 64).toInt())
                input.readFully(comm)
                if (comm.size < 18) return null
                val frames = ExtractorIo.u32be(comm, 2)
                val rate = ExtractorIo.extendedFloat80(comm, 8)
                if (rate <= 0.0) return null
                return (frames * 1000.0 / rate).toLong()
            }
            input.seek(input.filePointer + size + (size and 1L))
        }
    }

    private fun dsfDurationMs(input: RandomAccessFile): Long? {
        val header = ByteArray(28)
        input.readFully(header)
        if (!ExtractorIo.isAscii(header, 0, "DSD ")) return null
        input.seek(28)
        val fmtHeader = ByteArray(12)
        input.readFully(fmtHeader)
        if (!ExtractorIo.isAscii(fmtHeader, 0, "fmt ")) return null
        val fmt = ByteArray(40)
        input.readFully(fmt)
        val rate = ExtractorIo.u32le(fmt, 16)
        val sampleCount = ExtractorIo.u64le(fmt, 24)
        if (rate <= 0 || sampleCount <= 0) return null
        return sampleCount * 1000 / rate
    }

    private fun dffDurationMs(input: RandomAccessFile): Long? {
        val header = ByteArray(16)
        input.readFully(header)
        if (!ExtractorIo.isAscii(header, 0, "FRM8") || !ExtractorIo.isAscii(header, 12, "DSD ")) return null
        var sampleRate = 0L
        var channels = 0
        var dataSize = -1L
        val chunkHeader = ByteArray(12)
        while (input.read(chunkHeader) == chunkHeader.size) {
            val id = ExtractorIo.readAscii(chunkHeader, 0, 4)
            val size = ExtractorIo.u64be(chunkHeader, 4)
            val chunkStart = input.filePointer
            when (id) {
                "PROP" -> {
                    val form = ByteArray(4)
                    input.readFully(form)
                    val propEnd = minOf(chunkStart + size, input.length())
                    while (input.filePointer + 12 <= propEnd) {
                        val subHeader = ByteArray(12)
                        input.readFully(subHeader)
                        val subId = ExtractorIo.readAscii(subHeader, 0, 4)
                        val subSize = ExtractorIo.u64be(subHeader, 4)
                        val subStart = input.filePointer
                        when (subId) {
                            "FS  " -> {
                                val value = ByteArray(4)
                                input.readFully(value)
                                sampleRate = ExtractorIo.u32be(value, 0)
                            }

                            "CHNL" -> {
                                val value = ByteArray(2)
                                input.readFully(value)
                                channels = ExtractorIo.u16be(value, 0)
                            }
                        }
                        input.seek(subStart + subSize + (subSize and 1L))
                    }
                    input.seek(propEnd)
                }

                "DSD " -> {
                    dataSize = size
                    break
                }

                else -> {
                    input.seek(chunkStart + size + (size and 1L))
                }
            }
        }
        if (sampleRate <= 0 || channels <= 0 || dataSize <= 0) return null
        val bits = dataSize / channels * 8
        return bits * 1000 / sampleRate
    }
}
