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

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

/** Extra containers the app recognises beyond the MediaStore/Media3 defaults. */
@OptIn(UnstableApi::class)
enum class ExtraFormatKind(
    val extensions: Set<String>,
    val containerMimeType: String,
) {
    AIFF(setOf("aif", "aiff", "aifc"), ExtraMimeTypes.AIFF),
    DSF(setOf("dsf"), ExtraMimeTypes.DSF),
    DFF(setOf("dff"), ExtraMimeTypes.DFF),
    APE(setOf("ape"), ExtraMimeTypes.APE),
    WMA(setOf("wma", "asf"), ExtraMimeTypes.WMA),
    ;

    companion object {
        fun forPath(path: String): ExtraFormatKind? {
            val extension = path.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { extension in it.extensions }
        }
    }
}

/** Why an extra format cannot be played in this build. */
enum class ExtraFormatUnsupportedReason {
    APE_DECODER_MISSING,
    WMA_DECODER_MISSING,
}

/**
 * Technical description shown in the song info screen and used by the scanner to avoid indexing
 * files that cannot be played.
 */
data class ExtraFormatDescriptor(
    val kind: ExtraFormatKind,
    val container: String,
    val codec: String,
    val decoder: String?,
    val unsupportedReason: ExtraFormatUnsupportedReason? = null,
) {
    val isSupported: Boolean get() = unsupportedReason == null
}

object ExtraFormatSupport {
    @OptIn(UnstableApi::class)
    fun describe(kind: ExtraFormatKind): ExtraFormatDescriptor =
        when (kind) {
            ExtraFormatKind.AIFF -> {
                ExtraFormatDescriptor(
                    kind = kind,
                    container = "AIFF/AIFC",
                    codec = "PCM / IEEE float",
                    decoder = "Ssilencce AIFF extractor",
                )
            }

            ExtraFormatKind.DSF -> {
                ExtraFormatDescriptor(
                    kind = kind,
                    container = "DSF (DSD Stream File)",
                    codec = "DSD 1-bit",
                    decoder = "Ssilencce DSD decimator (88.2 kHz PCM)",
                )
            }

            ExtraFormatKind.DFF -> {
                ExtraFormatDescriptor(
                    kind = kind,
                    container = "DSDIFF (DFF)",
                    codec = "DSD 1-bit",
                    decoder = "Ssilencce DSD decimator (88.2 kHz PCM)",
                )
            }

            ExtraFormatKind.APE -> {
                val supported = BundledFfmpegProbe.hasDecoder("ape")
                ExtraFormatDescriptor(
                    kind = kind,
                    container = "Monkey's Audio (APE)",
                    codec = "Monkey's Audio",
                    decoder = if (supported) "FFmpeg (ape)" else null,
                    unsupportedReason = if (supported) null else ExtraFormatUnsupportedReason.APE_DECODER_MISSING,
                )
            }

            ExtraFormatKind.WMA -> {
                val platformDecoder = hasPlatformAudioDecoder(ExtraMimeTypes.WMA)
                val ffmpegDecoder = BundledFfmpegProbe.hasDecoder("wmav2")
                ExtraFormatDescriptor(
                    kind = kind,
                    container = "ASF (Windows Media Audio)",
                    codec = "WMA",
                    decoder =
                        if (platformDecoder) {
                            "Platform MediaCodec (WMA)"
                        } else if (ffmpegDecoder) {
                            "FFmpeg (wmav2)"
                        } else {
                            null
                        },
                    unsupportedReason =
                        if (platformDecoder || ffmpegDecoder) null else ExtraFormatUnsupportedReason.WMA_DECODER_MISSING,
                )
            }
        }

    /** Whether the supplementary scanner should index files of this kind. */
    fun isPlayable(kind: ExtraFormatKind): Boolean = describe(kind).isSupported

    /** Extensions the scanner may index in this build. */
    val scannableExtensions: Set<String> =
        ExtraFormatKind.entries
            .filter { isPlayable(it) }
            .flatMap { it.extensions }
            .toSet()
}
