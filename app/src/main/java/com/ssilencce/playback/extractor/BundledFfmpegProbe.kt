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

import android.media.MediaCodecList
import java.util.concurrent.ConcurrentHashMap

/**
 * Probes the FFmpeg decoder extension that ships with the app.
 *
 * Media3's `FfmpegLibrary` only exposes `isAvailable()` publicly; the per-codec check lives in a
 * private native method. Reflection is used here (and only here) so the app can tell the user
 * exactly which decoders the packaged native build contains instead of failing at playback time.
 *
 * The current dependency is `org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1`, whose build script
 * enables `flac alac pcm_mulaw pcm_alaw mp3 aac ac3 eac3 dca mlp truehd`. APE (`ape`) and the WMA
 * decoders (`wmav1`, `wmav2`, `wmapro`, `wmalossless`) are not compiled in, so their probe returns
 * false on every device.
 */
object BundledFfmpegProbe {
    private val decoderCache = ConcurrentHashMap<String, Boolean>()
    private val versionHolder = arrayOfNulls<String>(1)

    fun hasDecoder(codecName: String): Boolean = decoderCache.getOrPut(codecName) { probeDecoder(codecName) }

    fun version(): String? {
        if (versionHolder[0] != null) return versionHolder[0]
        return runCatching {
            val library = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
            if (library.getMethod("isAvailable").invoke(null) != true) return null
            (library.getMethod("getVersion").invoke(null) as? String)?.also { versionHolder[0] = it }
        }.getOrNull()
    }

    private fun probeDecoder(codecName: String): Boolean =
        runCatching {
            val library = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegLibrary")
            if (library.getMethod("isAvailable").invoke(null) != true) return false
            val method = library.getDeclaredMethod("ffmpegHasDecoder", String::class.java)
            method.isAccessible = true
            method.invoke(null, codecName) as Boolean
        }.getOrDefault(false)
}

/** Returns whether a platform MediaCodec decoder accepts [mimeType]. */
internal fun hasPlatformAudioDecoder(mimeType: String): Boolean =
    runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
        }
    }.getOrDefault(false)
