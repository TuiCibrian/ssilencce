/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.ssilencce.data.model

import com.ssilencce.core.model.ComposerNamePolicy
import com.ssilencce.core.sort.SongSortMode
import com.ssilencce.data.SongProvider
import java.util.Objects

/**
 * @author Christians M. A. (mardous)
 */
data class Artist(
    val id: Long,
    val albums: List<Album>,
    val filterSingles: Boolean,
    val isAlbumArtist: Boolean = false,
    val composerName: String? = null,
    val nameOverride: String? = null,
) : SongProvider {
    constructor(artistName: String, albums: List<Album>, filterSingles: Boolean, isAlbumArtist: Boolean = true) :
        this(albums.firstOrNull()?.artistId ?: -1, albums, filterSingles, true)

    val name: String
        get() = composerName ?: nameOverride ?: if (isAlbumArtist) getAlbumArtistName() ?: "-" else getArtistName()

    val isComposer: Boolean
        get() = composerName != null

    val albumCount: Int
        get() = if (filterSingles) albums.count { !it.isSingle } else albums.size

    val songCount: Int
        get() = songs.size

    val duration: Long
        get() = albums.sumOf { it.duration }

    val sortedAlbums: List<Album>
        get() = if (filterSingles) albums.filterNot { it.isSingle } else albums

    val sortedSongs: List<Song>
        get() = with(SongSortMode.ArtistSongs) { songs.sorted() }

    override val songs: List<Song>
        get() = albums.flatMap { it.songs }

    fun safeGetFirstAlbum(): Album = albums.firstOrNull() ?: Album.empty

    private fun getArtistName(): String = safeGetFirstAlbum().safeGetFirstSong().artistName

    private fun getAlbumArtistName(): String? = safeGetFirstAlbum().safeGetFirstSong().albumArtistName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val artist = other as Artist
        return id == artist.id &&
            songs == artist.songs &&
            isAlbumArtist == artist.isAlbumArtist &&
            filterSingles == artist.filterSingles &&
            composerName == artist.composerName &&
            nameOverride == artist.nameOverride
    }

    override fun hashCode(): Int = Objects.hash(id, songs, isAlbumArtist, filterSingles, composerName, nameOverride)

    override fun toString(): String =
        "Artist{id=$id, albums=$albums, isAlbumArtist=$isAlbumArtist, filterSingles=$filterSingles, " +
            "composerName=$composerName, nameOverride=$nameOverride}"

    companion object {
        const val UNKNOWN = "<unknown>"
        const val VARIOUS_ARTISTS_DISPLAY_NAME = "Various Artists"
        const val VARIOUS_ARTISTS_ID: Long = -2

        private const val ALBUM_ARTIST_FALLBACK_ID_OFFSET = 10_000_000_000L

        fun albumArtistFallbackId(name: String): Long = ALBUM_ARTIST_FALLBACK_ID_OFFSET + ComposerNamePolicy.stableId(name)

        val empty = Artist(-1, arrayListOf(), false)
    }
}
