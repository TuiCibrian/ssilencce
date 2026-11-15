package com.ssilencce.playback.crossfade

import androidx.media3.common.MediaItem
import com.ssilencce.core.model.MediaItemExtras

/**
 * Detects album-continuous transitions where the gapless engine should be left alone.
 *
 * A pair qualifies only when both items carry the same (non-zero) album id and the incoming
 * item is the immediate successor: the next track on the same disc, or track 1 of the next
 * disc. Unknown track numbers are never treated as continuous so a shuffled queue of the
 * same album cannot accidentally suppress crossfade.
 */
object GaplessAlbumDetector {
    fun areConsecutiveAlbumTracks(
        current: MediaItem?,
        next: MediaItem?,
    ): Boolean {
        if (current == null || next == null) return false
        val currentExtras = current.mediaMetadata.extras ?: return false
        val nextExtras = next.mediaMetadata.extras ?: return false
        val currentAlbumId = currentExtras.getLong(MediaItemExtras.ALBUM_ID, 0L)
        val nextAlbumId = nextExtras.getLong(MediaItemExtras.ALBUM_ID, 0L)
        if (currentAlbumId <= 0L || currentAlbumId != nextAlbumId) return false

        val currentTrack = current.mediaMetadata.trackNumber ?: return false
        val nextTrack = next.mediaMetadata.trackNumber ?: return false
        if (currentTrack <= 0 || nextTrack <= 0) return false

        val currentDisc = currentExtras.getInt(MediaItemExtras.DISC_NUMBER, 0)
        val nextDisc = nextExtras.getInt(MediaItemExtras.DISC_NUMBER, 0)
        if (currentDisc != nextDisc) {
            return nextDisc == currentDisc + 1 && nextTrack == 1
        }
        return nextTrack == currentTrack + 1
    }
}
