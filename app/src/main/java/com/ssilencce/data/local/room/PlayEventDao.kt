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
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.ssilencce.data.model.Song
import kotlinx.coroutines.flow.Flow

data class SongPlayCount(
    val songId: Long,
    val playCount: Long,
)

data class ArtistIdPlayCount(
    val artistId: Long,
    val artistName: String,
    val playCount: Long,
    val totalPlayCount: Long,
)

data class ArtistEventPlayCount(
    val artistName: String,
    val playCount: Long,
    val totalPlayCount: Long,
)

@Dao
interface PlayEventDao {
    @Insert
    suspend fun insert(event: PlayEventEntity)

    /**
     * Commits the aggregate increment and its play event in one Room transaction. Period
     * statistics therefore never count a play that the aggregate table did not record.
     */
    @Transaction
    suspend fun recordPlay(
        song: Song,
        timePlayed: Long,
        playCountDao: PlayCountDao,
    ) {
        playCountDao.insertOrIncrementPlayCount(song, timePlayed)
        insert(
            PlayEventEntity(
                songId = song.id,
                artistName = song.artistName,
                playedAt = timePlayed,
            ),
        )
    }

    @Query("SELECT COUNT(*) FROM PlayEventEntity WHERE played_at >= :cutoff")
    suspend fun playCountSince(cutoff: Long): Long

    @Query(
        """
        SELECT song_id AS songId, COUNT(*) AS playCount
        FROM PlayEventEntity
        WHERE played_at >= :cutoff
        GROUP BY song_id
        ORDER BY playCount DESC, MAX(played_at) DESC
        LIMIT :limit
        """,
    )
    suspend fun topSongsSince(
        cutoff: Long,
        limit: Int,
    ): List<SongPlayCount>

    @Query(
        """
        SELECT song_id AS songId, COUNT(*) AS playCount
        FROM PlayEventEntity
        WHERE played_at >= :startMillis AND played_at < :endMillis
        GROUP BY song_id
        ORDER BY playCount DESC, MAX(played_at) DESC
        LIMIT :limit
        """,
    )
    suspend fun topSongsBetween(
        startMillis: Long,
        endMillis: Long,
        limit: Int,
    ): List<SongPlayCount>

    @Query(
        """
        SELECT p.*
        FROM PlayEventEntity e
        INNER JOIN PlayCountEntity p ON p.id = e.song_id
        WHERE e.played_at >= :cutoff
        GROUP BY p.id
        ORDER BY COUNT(*) DESC, MAX(e.played_at) DESC
        LIMIT :limit
        """,
    )
    fun topSongsSinceFlow(
        cutoff: Long,
        limit: Int,
    ): Flow<List<PlayCountEntity>>

    @Query(
        """
        SELECT artist_name AS artistName, COUNT(*) AS playCount,
            (SELECT COUNT(*) FROM PlayEventEntity WHERE played_at >= :cutoff) AS totalPlayCount
        FROM PlayEventEntity
        WHERE played_at >= :cutoff
        GROUP BY artist_name
        ORDER BY playCount DESC, artist_name ASC
        LIMIT :limit
        """,
    )
    suspend fun topArtistsSince(
        cutoff: Long,
        limit: Int,
    ): List<ArtistEventPlayCount>

    @Query(
        """
        SELECT artist_name AS artistName, COUNT(*) AS playCount,
            (SELECT COUNT(*) FROM PlayEventEntity WHERE played_at >= :startMillis AND played_at < :endMillis) AS totalPlayCount
        FROM PlayEventEntity
        WHERE played_at >= :startMillis AND played_at < :endMillis
        GROUP BY artist_name
        ORDER BY playCount DESC, artist_name ASC
        LIMIT :limit
        """,
    )
    suspend fun topArtistsBetween(
        startMillis: Long,
        endMillis: Long,
        limit: Int,
    ): List<ArtistEventPlayCount>

    @Query(
        """
        SELECT artist_name AS artistName, COUNT(*) AS playCount,
            (SELECT COUNT(*) FROM PlayEventEntity WHERE played_at >= :cutoff) AS totalPlayCount
        FROM PlayEventEntity
        WHERE played_at >= :cutoff
        GROUP BY artist_name
        ORDER BY playCount DESC, artist_name ASC
        LIMIT :limit
        """,
    )
    fun topArtistsSinceFlow(
        cutoff: Long,
        limit: Int,
    ): Flow<List<ArtistEventPlayCount>>

    @Query("DELETE FROM PlayEventEntity WHERE played_at < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM PlayEventEntity")
    suspend fun clear()
}
