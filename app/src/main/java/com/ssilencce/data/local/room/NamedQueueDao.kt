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
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class NamedQueueSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int,
)

data class NamedQueueWithItems(
    @Embedded val queue: NamedQueueEntity,
    @Relation(parentColumn = "id", entityColumn = "queue_id")
    val items: List<NamedQueueItemEntity>,
) {
    fun orderedItems(): List<NamedQueueItemEntity> = items.sortedBy { it.order }
}

@Dao
interface NamedQueueDao {
    @Query("SELECT * FROM NamedQueueEntity ORDER BY updated_at DESC")
    suspend fun queues(): List<NamedQueueEntity>

    @Transaction
    @Query("SELECT * FROM NamedQueueEntity ORDER BY updated_at DESC")
    suspend fun queuesWithItems(): List<NamedQueueWithItems>

    @Query("SELECT * FROM NamedQueueEntity WHERE id = :queueId LIMIT 1")
    suspend fun queue(queueId: String): NamedQueueEntity?

    @Query(
        """
        SELECT q.id AS id, q.name AS name, q.created_at AS createdAt, q.updated_at AS updatedAt,
               (SELECT COUNT(*) FROM NamedQueueItemEntity i WHERE i.queue_id = q.id) AS itemCount
        FROM NamedQueueEntity q
        ORDER BY q.created_at ASC, q.id ASC
        """,
    )
    fun summariesFlow(): Flow<List<NamedQueueSummary>>

    @Query(
        """
        SELECT q.id AS id, q.name AS name, q.created_at AS createdAt, q.updated_at AS updatedAt,
               (SELECT COUNT(*) FROM NamedQueueItemEntity i WHERE i.queue_id = q.id) AS itemCount
        FROM NamedQueueEntity q
        ORDER BY q.created_at ASC, q.id ASC
        """,
    )
    suspend fun summaries(): List<NamedQueueSummary>

    @Query("SELECT * FROM NamedQueueItemEntity WHERE queue_id = :queueId ORDER BY `order`")
    suspend fun items(queueId: String): List<NamedQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQueue(queue: NamedQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<NamedQueueItemEntity>)

    @Query("DELETE FROM NamedQueueItemEntity WHERE queue_id = :queueId")
    suspend fun deleteItems(queueId: String)

    @Query("DELETE FROM NamedQueueEntity WHERE id = :queueId")
    suspend fun deleteQueue(queueId: String)

    @Query("UPDATE NamedQueueEntity SET name = :name, updated_at = :updatedAt WHERE id = :queueId")
    suspend fun renameQueue(
        queueId: String,
        name: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE NamedQueueEntity
        SET resume_index = :resumeIndex,
            resume_position_ms = :resumePositionMs,
            repeat_mode = :repeatMode,
            shuffle_enabled = :shuffleEnabled,
            updated_at = :updatedAt
        WHERE id = :queueId
        """,
    )
    suspend fun updateResumeState(
        queueId: String,
        resumeIndex: Int,
        resumePositionMs: Long,
        repeatMode: Int,
        shuffleEnabled: Boolean,
        updatedAt: Long,
    )

    @Transaction
    suspend fun saveQueue(
        queue: NamedQueueEntity,
        items: List<NamedQueueItemEntity>,
    ) {
        upsertQueue(queue)
        deleteItems(queue.id)
        insertItems(items)
    }

    @Transaction
    suspend fun replaceItemsAndResume(
        queueId: String,
        items: List<NamedQueueItemEntity>,
        resumeIndex: Int,
        resumePositionMs: Long,
        repeatMode: Int,
        shuffleEnabled: Boolean,
        updatedAt: Long,
    ) {
        deleteItems(queueId)
        insertItems(items)
        updateResumeState(
            queueId = queueId,
            resumeIndex = resumeIndex,
            resumePositionMs = resumePositionMs,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            updatedAt = updatedAt,
        )
    }

    @Transaction
    suspend fun deleteQueueWithItems(queueId: String) {
        deleteItems(queueId)
        deleteQueue(queueId)
    }
}
