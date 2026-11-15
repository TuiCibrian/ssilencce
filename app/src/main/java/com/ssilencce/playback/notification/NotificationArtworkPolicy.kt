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

package com.ssilencce.playback.notification

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.ssilencce.coil.CoverProvider
import com.ssilencce.util.Preferences

/**
 * Applies the "Broadcast album art to system lock screen" preference to the media items
 * owned by the player. The system lock screen and the media notification read artwork
 * from the session metadata, so omitting it there hides the artwork in both surfaces.
 *
 * Metadata-only replacements are supported in place by ExoPlayer and do not reload the
 * current media source.
 */
@UnstableApi
object NotificationArtworkPolicy {
    fun apply(item: MediaItem): MediaItem = apply(item, Preferences.notificationBroadcastAlbumArt)

    fun apply(
        item: MediaItem,
        broadcastArtwork: Boolean,
    ): MediaItem {
        val metadata = item.mediaMetadata
        val hasArtwork = metadata.artworkUri != null || metadata.artworkData != null
        return if (broadcastArtwork) {
            if (hasArtwork) {
                item
            } else {
                val songId = item.mediaId.toLongOrNull() ?: return item
                item
                    .buildUpon()
                    .setMediaMetadata(
                        metadata
                            .buildUpon()
                            .setArtworkUri(CoverProvider.getImageUri(CoverProvider.SONG_COVER_PATH, songId))
                            .build(),
                    ).build()
            }
        } else {
            if (!hasArtwork) {
                item
            } else {
                item
                    .buildUpon()
                    .setMediaMetadata(
                        metadata
                            .buildUpon()
                            .setArtworkUri(null)
                            .setArtworkData(null)
                            .build(),
                    ).build()
            }
        }
    }

    fun apply(
        items: List<MediaItem>,
        broadcastArtwork: Boolean = Preferences.notificationBroadcastAlbumArt,
    ): List<MediaItem> = items.map { apply(it, broadcastArtwork) }
}
