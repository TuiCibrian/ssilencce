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

package com.ssilencce.playback

import com.ssilencce.data.local.room.TrackPositionDao
import com.ssilencce.data.local.room.TrackPositionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

data class TrackPositionSnapshot(
    val mediaId: String,
    val positionMs: Long,
    val durationMs: Long,
)

/**
 * Application-owned serial writer for per-track positions. All service callbacks enqueue before
 * the playback service is destroyed, so the final snapshot cannot be overtaken by an older write
 * that was still in flight on the service scope. The caller snapshots the player on Main; this
 * object never reads the player and never blocks the main thread. Writes can still be lost if
 * Android kills the whole process before the queue is drained; this is not process-kill durability.
 */
object TrackPositionWriter {
    private sealed interface Write {
        val dao: TrackPositionDao

        data class Upsert(
            override val dao: TrackPositionDao,
            val mediaId: String,
            val positionMs: Long,
        ) : Write

        data class Delete(
            override val dao: TrackPositionDao,
            val mediaId: String,
        ) : Write

        data class Prune(
            override val dao: TrackPositionDao,
            val keep: Int,
        ) : Write
    }

    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            for (write in writes) {
                runCatching {
                    when (write) {
                        is Write.Upsert -> {
                            write.dao.upsert(
                                TrackPositionEntity(
                                    mediaId = write.mediaId,
                                    positionMs = write.positionMs,
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                        }

                        is Write.Delete -> {
                            write.dao.delete(write.mediaId)
                        }

                        is Write.Prune -> {
                            write.dao.prune(write.keep)
                        }
                    }
                }
            }
        }
    }

    fun persist(
        dao: TrackPositionDao,
        mediaId: String,
        positionMs: Long,
    ) {
        writes.trySend(Write.Upsert(dao, mediaId, positionMs))
    }

    fun delete(
        dao: TrackPositionDao,
        mediaId: String,
    ) {
        writes.trySend(Write.Delete(dao, mediaId))
    }

    fun prune(
        dao: TrackPositionDao,
        keep: Int,
    ) {
        writes.trySend(Write.Prune(dao, keep))
    }

    fun persistFinalSnapshot(
        dao: TrackPositionDao,
        snapshot: TrackPositionSnapshot,
    ) {
        if (snapshot.positionMs >= snapshot.durationMs - END_TOLERANCE_MS) {
            delete(dao, snapshot.mediaId)
        } else {
            persist(dao, snapshot.mediaId, snapshot.positionMs)
        }
    }

    private const val END_TOLERANCE_MS = 5000L
}
