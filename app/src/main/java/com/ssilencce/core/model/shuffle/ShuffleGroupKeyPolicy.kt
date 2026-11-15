package com.ssilencce.core.model.shuffle

import com.ssilencce.data.model.Song

/**
 * Pure grouping keys for the album-artist and composer most-played shuffle strategies.
 *
 * Existing modes keep their current keys. Album-artist groups use the tagged album artist and fall
 * back to the song artist when the tag is blank; songs without a composer share one unknown group.
 */
object ShuffleGroupKeyPolicy {
    const val UNKNOWN_GROUP = "\u0000unknown"

    fun albumArtist(song: Song): String =
        song.albumArtistName.normalizedOrNull()
            ?: song.artistName.normalizedOrNull()
            ?: UNKNOWN_GROUP

    fun composer(song: Song): String = song.composerName.normalizedOrNull() ?: UNKNOWN_GROUP

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
