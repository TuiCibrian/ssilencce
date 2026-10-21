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

package com.ssilencce.data.local.repository

import com.ssilencce.data.local.room.NamedQueueDao
import com.ssilencce.data.local.room.NamedQueueEntity
import com.ssilencce.data.local.room.NamedQueueItemEntity
import com.ssilencce.data.local.room.NamedQueueSummary
import com.ssilencce.data.local.room.NamedQueueWithItems
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class NamedQueueSnapshot(
    val mediaIds: List<String>,
    val resumeIndex: Int,
    val resumePositionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
) {
    val isEmpty: Boolean
        get() = mediaIds.isEmpty()
}

/**
 * Single writer for named queues. The active playback queue stays in [com.ssilencce.data.local.room.QueueEntity];
 * named queues are user-created snapshots that can be loaded into it.
 */
class NamedQueueRepository(
    private val dao: NamedQueueDao,
) {
    fun summariesFlow(): Flow<List<NamedQueueSummary>> = dao.summariesFlow()

    suspend fun summaries(): List<NamedQueueSummary> = dao.summaries()

    suspend fun queue(queueId: String): NamedQueueWithItems? =
        dao.queue(queueId)?.let { queue ->
            NamedQueueWithItems(queue, dao.items(queueId))
        }

    suspend fun create(
        requestedName: String,
        snapshot: NamedQueueSnapshot,
    ): NamedQueueEntity {
        val now = System.currentTimeMillis()
        val entity =
            NamedQueueEntity(
                id = UUID.randomUUID().toString(),
                name = uniqueName(requestedName),
                createdAt = now,
                updatedAt = now,
                resumeIndex = snapshot.resumeIndex,
                resumePositionMs = snapshot.resumePositionMs,
                repeatMode = snapshot.repeatMode,
                shuffleEnabled = snapshot.shuffleEnabled,
            )
        dao.saveQueue(entity, snapshot.toItems(entity.id))
        return entity
    }

    suspend fun update(
        queueId: String,
        snapshot: NamedQueueSnapshot,
    ) {
        val existing = dao.queue(queueId) ?: return
        dao.saveQueue(
            existing.copy(
                updatedAt = System.currentTimeMillis(),
                resumeIndex = snapshot.resumeIndex,
                resumePositionMs = snapshot.resumePositionMs,
                repeatMode = snapshot.repeatMode,
                shuffleEnabled = snapshot.shuffleEnabled,
            ),
            snapshot.toItems(queueId),
        )
    }

    suspend fun rename(
        queueId: String,
        requestedName: String,
    ) {
        val existing = dao.queue(queueId) ?: return
        val name = uniqueName(requestedName, exceptQueueId = queueId)
        if (name != existing.name) {
            dao.renameQueue(queueId, name, System.currentTimeMillis())
        }
    }

    suspend fun delete(queueId: String) {
        dao.deleteQueueWithItems(queueId)
    }

    suspend fun deleteOthers(activeQueueId: String) {
        dao
            .queues()
            .filterNot { it.id == activeQueueId }
            .forEach { dao.deleteQueueWithItems(it.id) }
    }

    private suspend fun uniqueName(
        requestedName: String,
        exceptQueueId: String? = null,
    ): String {
        val base = requestedName.trim().ifBlank { DEFAULT_QUEUE_NAME }
        val existing =
            dao
                .queues()
                .filterNot { it.id == exceptQueueId }
                .map { it.name.lowercase() }
                .toSet()
        if (base.lowercase() !in existing) return base
        var suffix = 2
        while ("$base ($suffix)".lowercase() in existing) {
            suffix++
        }
        return "$base ($suffix)"
    }

    private fun NamedQueueSnapshot.toItems(queueId: String): List<NamedQueueItemEntity> =
        mediaIds.mapIndexed { index, mediaId ->
            NamedQueueItemEntity(queueId = queueId, order = index, mediaId = mediaId)
        }

    companion object {
        const val DEFAULT_QUEUE_NAME = "Queue"
    }
}
