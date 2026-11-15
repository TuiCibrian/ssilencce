/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.component.base

import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.ArtistPlayStat
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.displayName

fun Song.matchesDetailQuery(
    query: String,
    includeFileName: Boolean = false,
): Boolean =
    QuickSearchPolicy.matches(
        query,
        title,
        albumName,
        displayArtistName(),
        fileName.takeIf { includeFileName },
    )

fun Album.matchesDetailQuery(query: String): Boolean = QuickSearchPolicy.matches(query, name, artistName, albumArtistName)

fun Artist.matchesDetailQuery(query: String): Boolean = QuickSearchPolicy.matches(query, displayName())

fun ArtistPlayStat.matchesDetailQuery(query: String): Boolean = QuickSearchPolicy.matches(query, artistName, artist?.displayName())
