package com.ssilencce.ui.screen.miniplayer

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ssilencce.coil.CoverProvider
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.resolvedArtistName
import com.ssilencce.playback.PlaybackService
import com.ssilencce.playback.ProgressObserver
import com.ssilencce.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the lightweight ExoPlayer used by the external-audio/preview popup. It intentionally
 * never touches the main queue; it only pauses and later restores the main session.
 */
@OptIn(UnstableApi::class)
class MiniPlayerViewModel(
    private val context: Context,
    private val repository: Repository,
    initialRequest: MiniPlayerRequest,
) : ViewModel(),
    Player.Listener {
    private val _state = MutableStateFlow(MiniPlayerUiState(isPreview = initialRequest is MiniPlayerRequest.Preview))
    val state = _state.asStateFlow()

    private val progressObserver = ProgressObserver(PROGRESS_INTERVAL_MS)
    private var player: ExoPlayer? = null
    private var mainController: MediaController? = null
    private var resumeMainOnClose = false
    private var mainMediaIdAtPause: String? = null
    private var suppressMainResume = false
    private var request: MiniPlayerRequest = initialRequest
    private var loadJob: Job? = null
    private var closed = false

    init {
        load(request)
    }

    fun replaceRequest(newRequest: MiniPlayerRequest) {
        if (newRequest == request) return
        request = newRequest
        closed = false
        loadJob?.cancel()
        stopPlayback()
        load(newRequest)
    }

    fun togglePlayPause() {
        val currentPlayer = player ?: return
        if (currentPlayer.playWhenReady) {
            currentPlayer.pause()
            return
        }
        if (isPlaybackBlockedByMute()) {
            _state.value = _state.value.copy(blockedByMute = true)
            return
        }
        _state.value = _state.value.copy(blockedByMute = false)
        if (currentPlayer.playbackState == Player.STATE_ENDED) {
            currentPlayer.seekTo(0)
        }
        currentPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        val currentPlayer = player ?: return
        val duration = currentPlayer.duration
        if (duration <= 0 || duration == C.TIME_UNSET) return
        currentPlayer.seekTo(positionMs.coerceIn(0L, duration))
        publishProgress()
    }

    /**
     * Stops the popup player and lets the requesting UI open the resolved song in the main app.
     * Main playback is deliberately not restored here: the main session will replace its queue.
     */
    fun prepareOpenInApp(): Uri? {
        val track = _state.value.track ?: return null
        val song = track.song ?: return null
        if (song.id < 0) return null
        suppressMainResume = true
        closed = true
        loadJob?.cancel()
        stopPlayback()
        return song.uri
    }

    fun close() {
        if (closed) return
        closed = true
        loadJob?.cancel()
        stopPlayback()
        resumeMainIfNeeded()
    }

    override fun onCleared() {
        loadJob?.cancel()
        closed = true
        stopPlayback()
        mainController?.release()
        mainController = null
        super.onCleared()
    }

    override fun onPlayWhenReadyChanged(
        playWhenReady: Boolean,
        reason: Int,
    ) {
        publishProgress()
        if (playWhenReady) {
            startProgressUpdates()
        } else {
            progressObserver.stop()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        publishProgress()
        if (playbackState == Player.STATE_ENDED) {
            progressObserver.stop()
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        publishProgress()
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "Mini player playback failed", error)
        progressObserver.stop()
        _state.value =
            _state.value.copy(
                isLoading = false,
                isPlaying = false,
                hasError = true,
            )
    }

    private fun load(loadRequest: MiniPlayerRequest) {
        _state.value =
            MiniPlayerUiState(
                isPreview = loadRequest is MiniPlayerRequest.Preview,
                isLoading = true,
            )
        loadJob =
            viewModelScope.launch {
                val track = resolveTrack(loadRequest)
                if (track == null) {
                    _state.value =
                        MiniPlayerUiState(
                            isPreview = loadRequest is MiniPlayerRequest.Preview,
                            isLoading = false,
                            hasError = true,
                        )
                    return@launch
                }
                pauseMainIfNeeded()
                if (closed) return@launch
                val exoPlayer = buildPlayer()
                player = exoPlayer
                _state.value =
                    MiniPlayerUiState(
                        track = track,
                        isPreview = loadRequest is MiniPlayerRequest.Preview,
                        isLoading = false,
                        durationMs = track.durationMs,
                        canOpenInApp = track.song?.id?.let { it >= 0 } == true,
                    )
                exoPlayer.setMediaItem(track.toMediaItem())
                exoPlayer.prepare()
                if (isPlaybackBlockedByMute()) {
                    _state.value = _state.value.copy(blockedByMute = true)
                } else {
                    exoPlayer.playWhenReady = true
                }
            }
    }

    private suspend fun resolveTrack(request: MiniPlayerRequest): MiniPlayerTrack? =
        when (request) {
            is MiniPlayerRequest.Preview -> {
                trackFromSong(request.song)
            }

            is MiniPlayerRequest.External -> {
                val song =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            repository.songsByUri(request.uri).firstOrNull { it.id >= 0 }
                        }.getOrNull()
                    }
                if (song != null) {
                    trackFromSong(song)
                } else {
                    withContext(Dispatchers.IO) { trackFromUri(request.uri) }
                }
            }

            MiniPlayerRequest.Unsupported -> {
                null
            }
        }

    private fun trackFromSong(song: Song): MiniPlayerTrack =
        MiniPlayerTrack(
            uri = song.uri,
            title = song.title.ifBlank { fallbackTitle(song.uri) },
            artist = song.resolvedArtistName().takeIf { it.isNotBlank() },
            album = song.albumName.takeIf { it.isNotBlank() },
            artworkUri = CoverProvider.getImageUri(CoverProvider.SONG_COVER_PATH, song.id),
            durationMs = song.duration,
            song = song,
        )

    private fun trackFromUri(uri: Uri): MiniPlayerTrack {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            MiniPlayerTrack(
                uri = uri,
                title =
                    retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?: fallbackTitle(uri),
                artist =
                    retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() },
                album =
                    retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?.takeIf { it.isNotBlank() },
                artworkUri = null,
                durationMs =
                    retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.coerceAtLeast(0L)
                        ?: 0L,
                song = null,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read metadata for $uri", e)
            MiniPlayerTrack(
                uri = uri,
                title = fallbackTitle(uri),
                artist = null,
                album = null,
                artworkUri = null,
                durationMs = 0L,
                song = null,
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun fallbackTitle(uri: Uri): String =
        uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: uri.toString()

    private suspend fun pauseMainIfNeeded() {
        if (closed || mainController != null || !PlaybackService.isRunning) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        val controller =
            withTimeoutOrNull(CONTROLLER_TIMEOUT_MS) {
                future.await()
            }
        if (controller == null) {
            if (!future.cancel(true)) {
                runCatching { future.get() }.getOrNull()?.release()
            }
            return
        }
        if (closed) {
            controller.release()
            return
        }
        mainController = controller
        if (controller.playWhenReady) {
            resumeMainOnClose = true
            mainMediaIdAtPause = controller.currentMediaItem?.mediaId
            controller.pause()
        }
    }

    private fun resumeMainIfNeeded() {
        val controller = mainController ?: return
        if (suppressMainResume || !resumeMainOnClose || controller.playWhenReady) return
        val mediaId = mainMediaIdAtPause ?: return
        if (controller.currentMediaItem?.mediaId != mediaId) return
        runCatching { controller.play() }
            .onSuccess { resumeMainOnClose = false }
    }

    private fun buildPlayer(): ExoPlayer =
        ExoPlayer
            .Builder(context)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            ).setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(this) }

    private fun stopPlayback() {
        progressObserver.stop()
        player?.let {
            it.removeListener(this)
            it.release()
        }
        player = null
    }

    private fun startProgressUpdates() {
        progressObserver.start { publishProgress() }
    }

    private fun publishProgress() {
        val currentPlayer = player ?: return
        val current = _state.value
        if (current.track == null || current.hasError) return
        _state.value =
            current.copy(
                isPlaying = currentPlayer.playWhenReady,
                positionMs = currentPlayer.currentPosition.coerceAtLeast(0L),
                durationMs =
                    currentPlayer.duration
                        .takeIf { it > 0 && it != C.TIME_UNSET }
                        ?: current.durationMs,
                blockedByMute = current.blockedByMute && isPlaybackBlockedByMute(),
            )
    }

    private fun isPlaybackBlockedByMute(): Boolean {
        if (!Preferences.preventPlaybackWhenMuted) return false
        val audioManager = context.getSystemService<AudioManager>() ?: return false
        if (audioManager.isVolumeFixed) return false
        return try {
            audioManager.isStreamMute(AudioManager.STREAM_MUSIC) ||
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read media volume", e)
            false
        }
    }

    companion object {
        private const val TAG = "MiniPlayer"
        private const val PROGRESS_INTERVAL_MS = 250L
        private const val CONTROLLER_TIMEOUT_MS = 2_000L
    }
}
