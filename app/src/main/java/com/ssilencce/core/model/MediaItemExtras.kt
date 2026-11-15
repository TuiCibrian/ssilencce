package com.ssilencce.core.model

/**
 * Well-known [android.os.Bundle] extras attached to a [androidx.media3.common.MediaItem]'s
 * metadata. They travel with the item through queue persistence and the "play next"
 * timeline wrappers so playback policies can reason about album continuity.
 */
object MediaItemExtras {
    const val ALBUM_ID = "com.ssilencce.extra.ALBUM_ID"
    const val DISC_NUMBER = "com.ssilencce.extra.DISC_NUMBER"
}
