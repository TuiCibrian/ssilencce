package com.ssilencce.playback

import android.util.Log
import com.ssilencce.data.local.room.NamedQueueDao
import com.ssilencce.data.local.room.NamedQueueItemEntity
import com.ssilencce.data.local.room.QueueDao
import com.ssilencce.data.local.room.QueueEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal data class QueueStateSnapshot(
    val mediaIds: List<String>,
    val namedQueueId: String?,
    val resumeIndex: Int,
    val resumePositionMs: Long,
    val repeatMode: Int,
    val shuffleEnabled: Boolean,
    val savePlaylist: Boolean,
    val updatedAt: Long,
)

/** Serial, application-owned writes outlive service teardown without retaining or reading a player. */
internal object PlaybackStateWriter {
    private data class Write(
        val queueDao: QueueDao?,
        val namedQueueDao: NamedQueueDao?,
        val snapshot: QueueStateSnapshot?,
        val completion: CompletableDeferred<Unit>?,
    )

    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            for (write in writes) {
                try {
                    write.snapshot?.let { snapshot ->
                        snapshot.namedQueueId?.let { queueId ->
                            val dao = requireNotNull(write.namedQueueDao)
                            if (snapshot.savePlaylist) {
                                dao.replaceItemsAndResume(
                                    queueId = queueId,
                                    items =
                                        snapshot.mediaIds.mapIndexed { index, mediaId ->
                                            NamedQueueItemEntity(queueId, index, mediaId)
                                        },
                                    resumeIndex = snapshot.resumeIndex,
                                    resumePositionMs = snapshot.resumePositionMs,
                                    repeatMode = snapshot.repeatMode,
                                    shuffleEnabled = snapshot.shuffleEnabled,
                                    updatedAt = snapshot.updatedAt,
                                )
                            } else {
                                dao.updateResumeState(
                                    queueId = queueId,
                                    resumeIndex = snapshot.resumeIndex,
                                    resumePositionMs = snapshot.resumePositionMs,
                                    repeatMode = snapshot.repeatMode,
                                    shuffleEnabled = snapshot.shuffleEnabled,
                                    updatedAt = snapshot.updatedAt,
                                )
                            }
                        }
                        if (snapshot.savePlaylist) {
                            write.queueDao?.replaceQueue(
                                snapshot.mediaIds.mapIndexed { index, id -> QueueEntity(id, index) },
                            )
                        }
                    }
                    write.completion?.complete(Unit)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    Log.e("PlaybackStateWriter", "Failed to persist playback snapshot", exception)
                    write.completion?.completeExceptionally(exception)
                }
            }
        }
    }

    fun persist(
        queueDao: QueueDao,
        namedQueueDao: NamedQueueDao,
        snapshot: QueueStateSnapshot,
    ) {
        check(writes.trySend(Write(queueDao, namedQueueDao, snapshot, null)).isSuccess)
    }

    fun persistNamedQueue(
        namedQueueDao: NamedQueueDao,
        snapshot: QueueStateSnapshot,
    ): Deferred<Unit> {
        val completion = CompletableDeferred<Unit>()
        check(writes.trySend(Write(null, namedQueueDao, snapshot, completion)).isSuccess)
        return completion
    }

    suspend fun awaitPendingWrites() {
        val completion = CompletableDeferred<Unit>()
        check(writes.trySend(Write(null, null, null, completion)).isSuccess)
        completion.await()
    }
}
