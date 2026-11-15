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

package com.ssilencce.playback

import androidx.media3.common.Format

/** The decoder and stream details ExoPlayer selected for the currently playing song. */
data class PlaybackDecoderSnapshot(
    val mediaId: String,
    val decoderName: String?,
    val containerMimeType: String?,
    val sampleMimeType: String?,
    val codecs: String?,
    val sampleRate: Int,
    val channelCount: Int,
)

/**
 * Process-wide cache of the last audio decoder selected by ExoPlayer.
 *
 * The song info screen cannot reach the playback service (it is a dialog fragment fed only with a
 * Song), so PlaybackService publishes the AnalyticsListener details here. Only the current song is
 * retained; anything else falls back to the format-derived description.
 */
object PlaybackDecoderRegistry {
    @Volatile
    private var snapshot: PlaybackDecoderSnapshot? = null

    @Synchronized
    fun onFormat(
        mediaId: String,
        format: Format,
    ) {
        val previousDecoder = snapshot?.takeIf { it.mediaId == mediaId }?.decoderName
        snapshot =
            PlaybackDecoderSnapshot(
                mediaId = mediaId,
                decoderName = previousDecoder,
                containerMimeType = format.containerMimeType,
                sampleMimeType = format.sampleMimeType,
                codecs = format.codecs,
                sampleRate = format.sampleRate,
                channelCount = format.channelCount,
            )
    }

    @Synchronized
    fun onDecoder(
        mediaId: String,
        decoderName: String,
    ) {
        val current = snapshot
        snapshot =
            if (current != null && current.mediaId == mediaId) {
                current.copy(decoderName = decoderName)
            } else {
                PlaybackDecoderSnapshot(
                    mediaId = mediaId,
                    decoderName = decoderName,
                    containerMimeType = null,
                    sampleMimeType = null,
                    codecs = null,
                    sampleRate = Format.NO_VALUE,
                    channelCount = Format.NO_VALUE,
                )
            }
    }

    fun forSong(songId: Long): PlaybackDecoderSnapshot? = snapshot?.takeIf { it.mediaId == songId.toString() }

    @Synchronized
    fun clear() {
        snapshot = null
    }
}
