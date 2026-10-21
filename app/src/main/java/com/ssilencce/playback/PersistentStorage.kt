package com.ssilencce.playback

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.room.NamedQueueDao
import com.ssilencce.data.local.room.QueueDao
import com.ssilencce.playback.ImprovedShuffleOrder.SerializedOrder
import com.ssilencce.util.ACTIVE_NAMED_QUEUE_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

typealias RestorationListener = (MediaItemsWithStartPosition, ShuffleOrder?) -> Unit

@UnstableApi
@OptIn(ExperimentalAtomicApi::class)
class PersistentStorage(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val player: AdvancedForwardingPlayer,
) : KoinComponent {
    // Synchronization lock for listener sets
    private val lock = Any()

    private val queueDao: QueueDao by inject()
    private val namedQueueDao: NamedQueueDao by inject()
    private val repository: Repository by inject()
    private val appPreferences: android.content.SharedPreferences by inject()
    private val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    // Tracks the latest save operation to prevent overlapping writes
    private var saveJob: Job? = null
    private var pendingPlaylistSave = false
    private var finished = false

    // Guards the persisted play/pause flag against an older debounced save completing after a
    // user pause. The generation changes synchronously on pause.
    private val stateGeneration = AtomicLong(0)

    // Set when a named-queue switch starts. Saves that still see the old timeline are skipped
    // instead of being attributed to the newly bound queue.
    private var expectedNamedQueueMediaIds: List<String>? = null
    private var expectedNamedQueueId: String? = null

    private val simpleListeners = LinkedHashSet<Runnable>()
    private val mediaItemsListeners = LinkedHashSet<RestorationListener>()

    val wasPlaying: Boolean
        get() = preferences.getBoolean(WAS_PLAYING, false)

    fun expectNamedQueueSwitch(
        queueId: String,
        mediaIds: List<String>,
    ) {
        expectedNamedQueueId = queueId
        expectedNamedQueueMediaIds = mediaIds
    }

    fun cancelNamedQueueSwitch() {
        expectedNamedQueueId = null
        expectedNamedQueueMediaIds = null
    }

    /**
     * Saves the current player state into a named queue before a switch replaces the timeline.
     * Reads the player on main and writes one transactional item/resume update.
     */
    fun saveNamedQueueState(queueId: String): Deferred<Unit> {
        val snapshot =
            QueueStateSnapshot(
                mediaIds = player.mediaItems.map { it.mediaId },
                namedQueueId = queueId,
                repeatMode = player.repeatMode,
                shuffleEnabled = player.shuffleModeEnabled,
                resumeIndex = player.currentMediaItemIndex,
                resumePositionMs = player.currentPosition,
                savePlaylist = true,
                updatedAt = System.currentTimeMillis(),
            )
        return PlaybackStateWriter.persistNamedQueue(namedQueueDao, snapshot)
    }

    // Atomic reference for restoration state management
    private var state = AtomicReference(RestorationState.Awaiting)
    val restorationState get() = state.load()

    /**
     * Represents the internal state of playback restoration.
     */
    enum class RestorationState(
        val isRestored: Boolean = false,
    ) {
        Awaiting(false), // Not started yet
        Restoring(false), // Currently restoring
        Restored(true), // Completed (either failed or successful)
    }

    /**
     * Adds a listener that will run once restoration is completed.
     * If restoration is already done, the listener runs immediately.
     */
    fun waitForRestoration(listener: Runnable) {
        if (restorationState.isRestored) {
            listener.run()
        } else {
            synchronized(lock) {
                simpleListeners.add(listener)
            }
        }
    }

    /**
     * Adds a listener that will receive the restored media items once available.
     * Ignored if restoration has already completed.
     */
    fun waitForMediaItems(listener: RestorationListener) {
        if (!restorationState.isRestored) {
            synchronized(lock) {
                mediaItemsListeners.add(listener)
            }
        }
    }

    /**
     * Restores the last known player state:
     * - Reads saved playlist and playback state from disk.
     * - Reconstructs MediaItems using Repository.
     * - Applies repeat/shuffle modes and playback position.
     * - Notifies all waiting listeners once complete.
     */
    fun restoreState(callback: RestorationListener) =
        coroutineScope.launch(Dispatchers.IO) {
            try {
                PlaybackStateWriter.awaitPendingWrites()
                // Ensure player is empty before restoring
                val emptyTimeline = withContext(Dispatchers.Main) { player.currentTimeline.isEmpty }

                // Only one restoration allowed
                val movedToRestoringState = state.compareAndSet(RestorationState.Awaiting, RestorationState.Restoring)
                if (movedToRestoringState && emptyTimeline) {
                    var startPosition = preferences.getInt(LAST_INDEX, C.INDEX_UNSET)
                    var startPositionMs = preferences.getLong(POSITION_IN_TRACK, C.TIME_UNSET)

                    // Load saved queue from database
                    val savedMediaItems =
                        queueDao.savedItems().map {
                            MediaItem
                                .Builder()
                                .setMediaId(it.id)
                                .setMediaMetadata(
                                    MediaMetadata
                                        .Builder()
                                        .setExtras(
                                            Bundle().apply {
                                                putInt("original_index", it.order)
                                            },
                                        ).build(),
                                ).build()
                        }

                    // Resolve valid items from repository
                    val (restoredMediaItems) =
                        repository
                            .songsByMediaItems(savedMediaItems)
                            .let { (songs, missingMediaItems) ->
                                songs.toMediaItems() to missingMediaItems
                            }

                    // Restored items keep saved order minus unresolved entries. Map every
                    // saved position to its restored position (or -1) so the resume index and
                    // the shuffle order follow the same occurrence-aware compaction.
                    val savedToRestored = IntArray(savedMediaItems.size) { -1 }
                    var restoredPointer = 0
                    savedMediaItems.forEachIndexed { savedIndex, savedItem ->
                        if (restoredMediaItems.getOrNull(restoredPointer)?.mediaId == savedItem.mediaId) {
                            savedToRestored[savedIndex] = restoredPointer
                            restoredPointer++
                        }
                    }
                    val mappingComplete = restoredPointer == restoredMediaItems.size

                    // Build session state object
                    val items =
                        if (restoredMediaItems.isNotEmpty()) {
                            val currentPositionIsSaved =
                                mappingComplete && startPosition in savedToRestored.indices
                            val mappedStartPosition =
                                if (currentPositionIsSaved) savedToRestored[startPosition] else -1
                            if (mappedStartPosition >= 0) {
                                startPosition = mappedStartPosition
                            } else {
                                // The saved current item is gone: resume at the item that took
                                // its place, and never seek into an unrelated replacement track.
                                var replacementPosition = 0
                                if (currentPositionIsSaved) {
                                    for (savedIndex in 0 until startPosition) {
                                        if (savedToRestored[savedIndex] >= 0) replacementPosition++
                                    }
                                }
                                startPosition = replacementPosition.coerceIn(0, restoredMediaItems.lastIndex)
                                startPositionMs = 0L
                            }

                            MediaItemsWithStartPosition(
                                restoredMediaItems,
                                startPosition,
                                startPositionMs,
                            )
                        } else {
                            // No items found, return an empty session state
                            MediaItemsWithStartPosition(
                                emptyList(),
                                C.INDEX_UNSET,
                                C.TIME_UNSET,
                            )
                        }

                    val repeatMode = preferences.getInt(REPEAT_MODE, Player.REPEAT_MODE_OFF)
                    val shuffleModeEnabled = preferences.getBoolean(SHUFFLE_MODE, false)

                    // Load serialized shuffle order
                    var shuffleOrderDecodeFailed = false
                    val serializedOrder =
                        preferences.getString(SHUFFLE_ORDER, null)?.let {
                            try {
                                SerializedOrder.serializedFromJson(it)
                            } catch (e: SerializationException) {
                                Log.e(TAG, "Couldn't deserialize saved shuffle order: $it", e)
                                shuffleOrderDecodeFailed = true
                                preferences.edit(commit = true) { putString(SHUFFLE_ORDER, null) }
                                null
                            }
                        }

                    val shuffleOrder =
                        if (shuffleModeEnabled) {
                            if (shuffleOrderDecodeFailed) {
                                null
                            } else if (items.mediaItems.isEmpty()) {
                                // Preserve the previous empty-queue behavior: no saved order or an
                                // empty saved order keeps shuffle enabled instead of resetting it.
                                if (serializedOrder == null) {
                                    ShuffleOrder.UnshuffledShuffleOrder(0)
                                } else if (serializedOrder.data != null && serializedOrder.data.isEmpty()) {
                                    serializedOrder.toShuffleOrder()
                                } else {
                                    null
                                }
                            } else if (serializedOrder == null) {
                                ShuffleOrder.UnshuffledShuffleOrder(items.mediaItems.size)
                            } else {
                                val remappedOrder =
                                    if (serializedOrder.data == null) {
                                        serializedOrder.toShuffleOrder(
                                            firstIndex = items.startIndex,
                                            length = items.mediaItems.size,
                                        )
                                    } else if (mappingComplete &&
                                        serializedOrder.data.size == savedMediaItems.size
                                    ) {
                                        serializedOrder.remap(savedToRestored)?.toShuffleOrder()
                                    } else {
                                        null
                                    }
                                if (remappedOrder == null) {
                                    Log.e(TAG, "Previous shuffle order is no longer valid")
                                    preferences.edit(commit = true) { putString(SHUFFLE_ORDER, null) }
                                }
                                remappedOrder
                            }
                        } else {
                            if (serializedOrder != null) {
                                Log.e(TAG, "Found orphan shuffle order")
                                preferences.edit(commit = true) { putString(SHUFFLE_ORDER, null) }
                            }
                            null
                        }

                    withContext(Dispatchers.Main) {
                        // Apply repeat/shuffle modes on main thread
                        player.repeatMode = repeatMode
                        player.shuffleModeEnabled = shuffleModeEnabled && shuffleOrder != null

                        dispatchItems(callback, items, shuffleOrder)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "State restoration failed", t)
                dispatchItems(
                    callback = callback,
                    items = MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET),
                    shuffleOrder = null,
                )
            } finally {
                // Always mark as restored to unblock future listeners
                state.store(RestorationState.Restored)
            }
        }

    /**
     * Saves the current playback state to disk.
     * Optionally persists the playlist order when [savePlaylist] is true.
     *
     * This operation is debounced to avoid writing too frequently.
     */
    fun saveState(savePlaylist: Boolean = false) {
        // Avoid writing while restoring
        if (finished || restorationState == RestorationState.Restoring) {
            return
        }
        pendingPlaylistSave = pendingPlaylistSave || savePlaylist

        // Save playlist requests have higher priority
        if (!savePlaylist && saveJob?.isActive == true) {
            return
        }

        saveJob?.cancel()
        saveJob =
            coroutineScope.launch {
                try {
                    // Small delay to prevent race conditions (e.g. fast track changes)
                    delay(500)

                    if (isActive && !finished) persistCurrentState(pendingPlaylistSave)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to write current state to disk", e)
                }
            }
    }

    /**
     * Persists the inactive state without waiting for the debounced save. A service can be
     * destroyed immediately after a pause, in which case cancelling [coroutineScope] would
     * otherwise leave a stale WAS_PLAYING=true value and let session recovery resume a queue the
     * user paused.
     */
    fun markNotPlaying() {
        if (finished) return
        saveJob?.cancel()
        saveJob = null
        stateGeneration.incrementAndGet()
        // Apply on Main so this marker is ordered after any snapshot already applied there. The
        // generation guard drops a delayed save superseded by this pause; no disk commit is made
        // on Main and the preference write is not repeated from an IO job.
        preferences.edit {
            putBoolean(WAS_PLAYING, false)
        }
        // A pause must not discard the queue edit that was waiting for the debounce.
        if (pendingPlaylistSave && restorationState != RestorationState.Restoring) {
            persistCurrentState(savePlaylist = true)
        }
    }

    fun finish() {
        if (finished) return
        saveJob?.cancel()
        saveJob = null
        if (restorationState.isRestored || pendingPlaylistSave) persistCurrentState(savePlaylist = true)
        finished = true
    }

    private fun persistCurrentState(savePlaylist: Boolean) {
        val generation = stateGeneration.get()
        val activeNamedQueueId = appPreferences.getString(ACTIVE_NAMED_QUEUE_ID, null)
        val mediaIds = player.mediaItems.map { it.mediaId }
        val expectedIds = expectedNamedQueueMediaIds
        val expectedQueueId = expectedNamedQueueId
        // Clear the guard only when both the expected timeline and its target binding are current.
        val namedQueueIdsMatch =
            expectedIds == null || (expectedQueueId == activeNamedQueueId && expectedIds == mediaIds)
        if (expectedIds != null && namedQueueIdsMatch) {
            expectedNamedQueueId = null
            expectedNamedQueueMediaIds = null
        }
        val snapshot =
            QueueStateSnapshot(
                mediaIds = mediaIds,
                namedQueueId = activeNamedQueueId.takeIf { namedQueueIdsMatch },
                resumeIndex = player.currentMediaItemIndex,
                resumePositionMs = player.currentPosition,
                repeatMode = player.repeatMode,
                shuffleEnabled = player.shuffleModeEnabled,
                savePlaylist = savePlaylist,
                updatedAt = System.currentTimeMillis(),
            )
        val wasPlaying = player.isPlaying
        val shuffleOrder =
            (player.exoPlayer.shuffleOrder as? ImprovedShuffleOrder)?.let {
                SerializedOrder.serializedFromOrder(it).toString()
            }
        if (generation == stateGeneration.get()) {
            preferences.edit {
                putInt(REPEAT_MODE, snapshot.repeatMode)
                putBoolean(SHUFFLE_MODE, snapshot.shuffleEnabled)
                putString(SHUFFLE_ORDER, shuffleOrder)
                putInt(LAST_INDEX, snapshot.resumeIndex)
                putLong(POSITION_IN_TRACK, snapshot.resumePositionMs)
                putBoolean(WAS_PLAYING, wasPlaying)
            }
        }
        PlaybackStateWriter.persist(queueDao, namedQueueDao, snapshot)
        if (savePlaylist) pendingPlaylistSave = false
    }

    private suspend fun dispatchItems(
        callback: RestorationListener,
        items: MediaItemsWithStartPosition,
        shuffleOrder: ShuffleOrder?,
    ) {
        withContext(Dispatchers.Main) {
            synchronized(lock) {
                if (!mediaItemsListeners.contains(callback)) {
                    callback(items, shuffleOrder)
                }

                mediaItemsListeners.forEach { it(items, null) }
                mediaItemsListeners.clear()

                simpleListeners.forEach { it.run() }
                simpleListeners.clear()
            }
        }
    }

    companion object {
        private const val TAG = "PersistentStorage"
        private const val PREFERENCE_NAME = "playback_state"

        const val REPEAT_MODE = "repeat_mode"
        const val SHUFFLE_MODE = "shuffle_mode"
        const val SHUFFLE_ORDER = "shuffle_order"
        const val POSITION_IN_TRACK = "position_in_track"
        const val LAST_INDEX = "last_index"
        const val WAS_PLAYING = "was_playing"
    }
}
