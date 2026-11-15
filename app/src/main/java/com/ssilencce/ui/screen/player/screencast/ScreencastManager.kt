package com.ssilencce.ui.screen.player.screencast

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.common.util.concurrent.ListenableFuture
import com.ssilencce.R
import com.ssilencce.data.local.repository.LyricsRepository
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.showToast
import com.ssilencce.playback.PlaybackService
import com.ssilencce.playback.ProgressObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the external-display presentation and the read-only media controller that feeds it.
 *
 * Audio is deliberately not touched: playback keeps running in [PlaybackService] and only the
 * visual Now Playing screen is sent to the external display.
 */
class ScreencastManager(
    context: Context,
    private val repository: Repository,
    lyricsRepository: LyricsRepository,
) {
    private val appContext = context.applicationContext
    private val displayManager = appContext.getSystemService<DisplayManager>()
    private val mediaRouter = MediaRouter.getInstance(appContext)
    private val lyricsLoader = ScreencastLyricsLoader(lyricsRepository)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val progressObserver = ProgressObserver(PROGRESS_INTERVAL_MS)

    private val _displays = MutableStateFlow(emptyList<ScreencastDisplayInfo>())
    val displays = _displays.asStateFlow()

    private val _activeDisplayId = MutableStateFlow<Int?>(null)
    val activeDisplayId = _activeDisplayId.asStateFlow()

    private val _state = MutableStateFlow(ScreencastNowPlayingState())
    val state = _state.asStateFlow()

    private var presentation: ScreencastPresentation? = null
    private var activeDisplayName: String? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var lyricsJob: Job? = null
    private var lyricsMediaId: String? = null

    val isActive: Boolean
        get() = _activeDisplayId.value != null

    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                refreshDisplays()
            }

            override fun onDisplayRemoved(displayId: Int) {
                refreshDisplays()
                if (_activeDisplayId.value == displayId) {
                    val name = activeDisplayName.orEmpty().ifBlank { displayFallback(displayId) }
                    stop()
                    appContext.showToast(
                        appContext.getString(R.string.screencast_display_disconnected, name),
                    )
                }
            }

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) refreshDisplays()
            }
        }

    private val playerListener =
        object : Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                publishPlaybackState()
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY,
                    )
                ) {
                    syncMediaItem()
                }
                updateProgressTicker()
            }
        }

    init {
        runCatching {
            displayManager?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        }
        refreshDisplays()
    }

    /** Re-reads the current presentation displays (HDMI, wireless display, remote route). */
    fun refreshDisplays() {
        val current =
            currentPresentationDisplays().map { display ->
                val mode = runCatching { display.mode }.getOrNull()
                ScreencastDisplayInfo(
                    id = display.displayId,
                    name = display.name.orEmpty().trim(),
                    widthPx = mode?.physicalWidth ?: 0,
                    heightPx = mode?.physicalHeight ?: 0,
                )
            }
        _displays.value = current
    }

    fun startFirstAvailable(): Boolean {
        if (isActive) return true
        refreshDisplays()
        val display = displays.value.firstOrNull() ?: return false
        return startDisplay(display.id)
    }

    fun startDisplay(displayId: Int): Boolean {
        if (displayId == Display.DEFAULT_DISPLAY) return false
        if (_activeDisplayId.value == displayId) return true
        refreshDisplays()

        val display = displayManager?.getDisplay(displayId)
        if (display == null || !display.isValid) {
            appContext.showToast(R.string.screencast_display_unavailable)
            refreshDisplays()
            return false
        }

        dismissPresentation()
        val created =
            try {
                ScreencastPresentation(appContext, display, state)
            } catch (error: RuntimeException) {
                Log.e(TAG, "Could not create the presentation for display $displayId", error)
                releasePlayback()
                appContext.showToast(R.string.screencast_start_failed)
                return false
            }
        created.setOnDismissListener {
            if (presentation === created) {
                presentation = null
                _activeDisplayId.value = null
                activeDisplayName = null
                releasePlayback()
            }
        }
        try {
            created.show()
        } catch (error: RuntimeException) {
            Log.e(TAG, "Could not show the presentation on display $displayId", error)
            created.setOnDismissListener(null)
            runCatching { created.dismiss() }
            releasePlayback()
            appContext.showToast(R.string.screencast_start_failed)
            return false
        }

        presentation = created
        _activeDisplayId.value = displayId
        activeDisplayName =
            display.name
                .orEmpty()
                .trim()
                .ifBlank { displayFallback(displayId) }
        connectPlayback()
        publishPlaybackState()
        syncMediaItem()
        updateProgressTicker()
        appContext.showToast(
            appContext.getString(R.string.screencast_started, activeDisplayName),
        )
        return true
    }

    fun stop() {
        dismissPresentation()
        releasePlayback()
    }

    /** Opens the system live-video route chooser (Miracast/wireless display/remote TV). */
    fun showDisplayChooser(activity: Activity) {
        refreshDisplays()
        val selector =
            MediaRouteSelector
                .Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                .build()
        val router = MediaRouter.getInstance(activity)
        val dialog =
            try {
                MediaRouteChooserDialog(activity).apply { routeSelector = selector }
            } catch (error: RuntimeException) {
                Log.e(TAG, "Could not create the media route chooser", error)
                appContext.showToast(R.string.screencast_settings_unavailable)
                return
            }
        val callback =
            object : MediaRouter.Callback() {
                override fun onRouteSelected(
                    router: MediaRouter,
                    route: MediaRouter.RouteInfo,
                    reason: Int,
                ) {
                    val display = route.presentationDisplay
                    runCatching { dialog.dismiss() }
                    if (display == null) {
                        appContext.showToast(R.string.screencast_route_unsupported)
                    } else {
                        startDisplay(display.displayId)
                    }
                }
            }
        router.addCallback(
            selector,
            callback,
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY,
        )
        dialog.setOnDismissListener {
            router.removeCallback(callback)
        }
        try {
            dialog.show()
        } catch (error: RuntimeException) {
            Log.e(TAG, "Could not show the media route chooser", error)
            router.removeCallback(callback)
            appContext.showToast(R.string.screencast_settings_unavailable)
        }
    }

    fun openDisplaySettings(activity: Activity) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.ACTION_CAST_SETTINGS)
            } else {
                Intent(Settings.ACTION_DISPLAY_SETTINGS)
            }
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            appContext.showToast(R.string.screencast_settings_unavailable)
        }
    }

    private fun dismissPresentation() {
        val current = presentation ?: return
        presentation = null
        current.setOnDismissListener(null)
        runCatching { current.dismiss() }
        _activeDisplayId.value = null
        activeDisplayName = null
        progressObserver.stop()
    }

    private fun releasePlayback() {
        progressObserver.stop()
        lyricsJob?.cancel()
        lyricsJob = null
        lyricsMediaId = null
        controller?.let { player ->
            runCatching { player.removeListener(playerListener) }
            runCatching { player.release() }
        }
        controller = null
        val future = controllerFuture
        controllerFuture = null
        if (future != null && !future.cancel(true)) {
            runCatching { future.get()?.release() }
        }
        _state.value = ScreencastNowPlayingState()
    }

    private fun connectPlayback() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future =
            try {
                MediaController.Builder(appContext, token).buildAsync()
            } catch (error: RuntimeException) {
                Log.e(TAG, "Could not connect to the playback session", error)
                return
            }
        controllerFuture = future
        future.addListener(
            {
                if (controllerFuture !== future) {
                    runCatching { future.get()?.release() }
                    return@addListener
                }
                controllerFuture = null
                val player = runCatching { future.get() }.getOrNull()
                if (player == null) {
                    Log.w(TAG, "The playback session is not available")
                    return@addListener
                }
                controller = player
                player.addListener(playerListener)
                publishPlaybackState()
                syncMediaItem()
                updateProgressTicker()
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    private fun publishPlaybackState() {
        val player = controller ?: return
        val metadata = player.currentMediaItem?.mediaMetadata
        val duration =
            player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
        val hasMedia = player.currentMediaItem != null && player.mediaItemCount > 0
        _state.value =
            _state.value.copy(
                hasMedia = hasMedia,
                title = metadata?.title?.toString().orEmpty(),
                artist = metadata?.artist?.toString().orEmpty(),
                album = metadata?.albumTitle?.toString().orEmpty(),
                artworkUri = metadata?.artworkUri,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = duration,
            )
    }

    private fun syncMediaItem() {
        val item: MediaItem? = controller?.currentMediaItem
        val mediaId = item?.mediaId
        if (mediaId == lyricsMediaId) return
        lyricsMediaId = mediaId
        lyricsJob?.cancel()
        lyricsJob = null
        if (mediaId == null) {
            _state.value = _state.value.copy(lyrics = ScreencastLyrics.None)
            return
        }
        _state.value = _state.value.copy(lyrics = ScreencastLyrics.None)
        lyricsJob =
            scope.launch {
                val song =
                    try {
                        withContext(Dispatchers.IO) { repository.songByMediaItem(item) }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not resolve the song for media item $mediaId", error)
                        return@launch
                    }
                if (lyricsMediaId != mediaId || song == Song.emptySong) return@launch
                val lyrics =
                    try {
                        withContext(Dispatchers.IO) { lyricsLoader.load(song) }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not load lyrics for $mediaId", error)
                        ScreencastLyrics.None
                    }
                if (lyricsMediaId == mediaId) {
                    _state.value = _state.value.copy(lyrics = lyrics)
                }
            }
    }

    private fun updateProgressTicker() {
        if (presentation != null && controller?.isPlaying == true) {
            progressObserver.start { publishPlaybackState() }
        } else {
            progressObserver.stop()
        }
    }

    private fun currentPresentationDisplays(): List<Display> {
        val found = LinkedHashMap<Int, Display>()
        runCatching {
            displayManager
                ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
                ?.forEach { display -> found[display.displayId] = display }
        }
        runCatching {
            mediaRouter.selectedRoute.presentationDisplay?.let { display ->
                found[display.displayId] = display
            }
        }
        return found.values.filter { display ->
            display.isValid && display.displayId != Display.DEFAULT_DISPLAY
        }
    }

    private fun displayFallback(displayId: Int): String = appContext.getString(R.string.screencast_display_unknown, displayId)

    companion object {
        private const val TAG = "ScreencastManager"
        private const val PROGRESS_INTERVAL_MS = 500L
    }
}
