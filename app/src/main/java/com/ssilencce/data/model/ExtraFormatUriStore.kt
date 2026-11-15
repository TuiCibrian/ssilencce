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

package com.ssilencce.data.model

import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves the playback URI of songs that do not come from MediaStore.
 *
 * Supplementary scans assign negative ids and a `file://` path; keeping the mapping outside
 * [Song] means every copy of a song (expanded, queue, favorite or history rows) keeps working
 * because the id is stable and the resolver is process-wide. The map is filled from the
 * persisted cache before any screen can show an extra-format song.
 */
object ExtraFormatUriStore {
    private val uris = ConcurrentHashMap<Long, String>()

    fun uriFor(songId: Long): String? = if (songId < 0) uris[songId] else null

    fun replace(songUris: Map<Long, String>) {
        uris.clear()
        uris.putAll(songUris)
    }

    fun clear() = uris.clear()
}
