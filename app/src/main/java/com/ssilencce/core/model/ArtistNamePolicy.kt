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

package com.ssilencce.core.model

/**
 * Resolves song artist names when the artist tag is missing or holds a placeholder value.
 *
 * The album-artist fallback is opt-in; callers pass the user preference so the default keeps the
 * raw artist tag untouched.
 */
object ArtistNamePolicy {
    fun isMissing(name: String?): Boolean {
        val value = name?.trim().orEmpty()
        return value.isEmpty() || value.equals("unknown", ignoreCase = true) || value.equals("<unknown>", ignoreCase = true)
    }

    fun resolve(
        artistName: String?,
        albumArtistName: String?,
        useAlbumArtistFallback: Boolean,
    ): String {
        val artist = artistName.orEmpty()
        if (!useAlbumArtistFallback || !isMissing(artist)) return artist
        val albumArtist = albumArtistName?.trim().orEmpty()
        return if (!isMissing(albumArtist)) albumArtist else artist
    }
}
