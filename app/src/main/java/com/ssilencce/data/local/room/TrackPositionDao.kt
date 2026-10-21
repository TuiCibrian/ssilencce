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

package com.ssilencce.data.local.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TrackPositionDao {
    @Upsert
    suspend fun upsert(position: TrackPositionEntity)

    @Query("SELECT * FROM TrackPositionEntity WHERE media_id = :mediaId LIMIT 1")
    suspend fun position(mediaId: String): TrackPositionEntity?

    @Query("DELETE FROM TrackPositionEntity WHERE media_id = :mediaId")
    suspend fun delete(mediaId: String)

    @Query("DELETE FROM TrackPositionEntity")
    suspend fun clear()

    @Query(
        """
        DELETE FROM TrackPositionEntity
        WHERE media_id NOT IN (
            SELECT media_id FROM TrackPositionEntity
            ORDER BY updated_at DESC
            LIMIT :keep
        )
        """,
    )
    suspend fun prune(keep: Int)
}
