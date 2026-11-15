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

import androidx.room.withTransaction
import com.ssilencce.core.SsilencceDatabase
import com.ssilencce.data.local.room.NamedQueueDao
import com.ssilencce.data.local.room.NamedQueueEntity
import com.ssilencce.data.local.room.NamedQueueItemEntity
import com.ssilencce.data.local.room.NamedQueueSummary
import com.ssilencce.data.local.room.NamedQueueWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class NamedQueueLimitException(
    val limit: Int = QueueNamePolicy.MAX_NAMED_QUEUES,
) : IllegalStateException("Named queue limit reached: $limit")

class NamedQueueNameException(
    val error: QueueNamePolicy.Error,
) : IllegalArgumentException("Invalid named queue name: $error")

data class NamedQueueRenameResult(
    val name: String,
    val replacedQueueId: String?,
)

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
    private val database: SsilencceDatabase,
) {
    private val mutex = Mutex()

    fun summariesFlow(): Flow<List<NamedQueueSummary>> = dao.summariesFlow()

    suspend fun summaries(): List<NamedQueueSummary> = dao.summaries()

    suspend fun orderedQueueIds(): List<String> = dao.summaries().map { it.id }

    suspend fun queue(queueId: String): NamedQueueWithItems? =
        dao.queue(queueId)?.let { queue ->
            NamedQueueWithItems(queue, dao.items(queueId))
        }

    suspend fun create(
        requestedName: String,
        snapshot: NamedQueueSnapshot,
    ): NamedQueueEntity =
        mutex.withLock {
            val existing = dao.queues()
            if (!QueueNamePolicy.canCreate(existing.size)) {
                throw NamedQueueLimitException()
            }
            val name = QueueNamePolicy.uniqueName(validatedName(requestedName), existing.map { it.name })
            val now = System.currentTimeMillis()
            val entity =
                NamedQueueEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    createdAt = now,
                    updatedAt = now,
                    resumeIndex = snapshot.resumeIndex,
                    resumePositionMs = snapshot.resumePositionMs,
                    repeatMode = snapshot.repeatMode,
                    shuffleEnabled = snapshot.shuffleEnabled,
                )
            dao.saveQueue(entity, snapshot.toItems(entity.id))
            entity
        }

    /**
     * Replaces an existing named queue snapshot by id. The requested name is intentionally not
     * applied: the existing queue keeps its stored name and only its snapshot/resume state is
     * overwritten. Active playback queue state is untouched.
     */
    suspend fun replace(
        queueId: String,
        snapshot: NamedQueueSnapshot,
    ): NamedQueueEntity? =
        mutex.withLock {
            val existing = dao.queue(queueId) ?: return@withLock null
            val updated =
                existing.copy(
                    updatedAt = System.currentTimeMillis(),
                    resumeIndex = snapshot.resumeIndex,
                    resumePositionMs = snapshot.resumePositionMs,
                    repeatMode = snapshot.repeatMode,
                    shuffleEnabled = snapshot.shuffleEnabled,
                )
            dao.saveQueue(updated, snapshot.toItems(queueId))
            updated
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

    /**
     * Normal rename keeps the source queue. When [replaceQueueId] points at another queue,
     * Replace deletes the selected target and renames the source queue to the requested name.
     * Everything runs in one Room transaction.
     */
    suspend fun rename(
        queueId: String,
        requestedName: String,
        replaceQueueId: String? = null,
    ): NamedQueueRenameResult? =
        mutex.withLock {
            database.withTransaction {
                val source = dao.queue(queueId) ?: return@withTransaction null
                val base = validatedName(requestedName)
                val target =
                    replaceQueueId
                        ?.takeIf { it != queueId }
                        ?.let { dao.queue(it) }
                if (target != null) {
                    dao.deleteQueueWithItems(target.id)
                }
                val otherNames =
                    dao
                        .queues()
                        .filterNot { it.id == queueId }
                        .map { it.name }
                val name = QueueNamePolicy.uniqueName(base, otherNames)
                if (name != source.name) {
                    dao.renameQueue(queueId, name, System.currentTimeMillis())
                }
                NamedQueueRenameResult(name, target?.id)
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

    private fun validatedName(requested: String): String {
        val name = requested.trim()
        if (name.isBlank()) throw NamedQueueNameException(QueueNamePolicy.Error.Blank)
        if (name.length > QueueNamePolicy.MAX_NAME_LENGTH) {
            throw NamedQueueNameException(QueueNamePolicy.Error.TooLong)
        }
        return name
    }

    private fun NamedQueueSnapshot.toItems(queueId: String): List<NamedQueueItemEntity> =
        mediaIds.mapIndexed { index, mediaId ->
            NamedQueueItemEntity(queueId = queueId, order = index, mediaId = mediaId)
        }
}
