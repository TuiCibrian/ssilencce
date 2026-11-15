package com.ssilencce.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.UnshuffledShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.toBitmap
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.ssilencce.R
import com.ssilencce.coil.CoilBitmapLoader
import com.ssilencce.core.appwidgets.CardWidget
import com.ssilencce.core.appwidgets.FullWidget
import com.ssilencce.core.appwidgets.SsilencceGlanceWidget
import com.ssilencce.core.appwidgets.WidgetTheme
import com.ssilencce.core.appwidgets.state.PlaybackState
import com.ssilencce.core.appwidgets.state.PlaybackStateDefinition
import com.ssilencce.core.audio.AudioOutputObserver
import com.ssilencce.core.model.action.HeadsetButtonAction
import com.ssilencce.core.model.equalizer.TempoState
import com.ssilencce.core.model.player.MetadataField
import com.ssilencce.core.model.player.NotificationStyle
import com.ssilencce.core.model.shuffle.ShuffleBehavior
import com.ssilencce.core.model.shuffle.ShuffleModeState
import com.ssilencce.core.palette.PaletteProcessor
import com.ssilencce.data.local.MediaStoreObserver
import com.ssilencce.data.local.ReplayGainTagExtractor
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.local.room.SongPlaybackParamDao
import com.ssilencce.data.local.room.SongPlaybackParamEntity
import com.ssilencce.data.local.room.TrackPositionDao
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.network.NetworkFeature
import com.ssilencce.data.model.network.ScrobblingService
import com.ssilencce.extensions.isBluetoothA2dpConnected
import com.ssilencce.extensions.isBluetoothA2dpDisconnected
import com.ssilencce.extensions.showToast
import com.ssilencce.playback.cast.CastPlayback
import com.ssilencce.playback.cast.CastPlaybackProvider
import com.ssilencce.playback.crossfade.CrossfadeConfig
import com.ssilencce.playback.crossfade.CrossfadeController
import com.ssilencce.playback.crossfade.CrossfadePlayer
import com.ssilencce.playback.equalizer.EqualizerManager
import com.ssilencce.playback.extractor.ExtraExtractorsFactory
import com.ssilencce.playback.library.AutoMediaButtons
import com.ssilencce.playback.library.LibraryProvider
import com.ssilencce.playback.library.MediaIDs
import com.ssilencce.playback.notification.CustomLayoutMediaNotificationProvider
import com.ssilencce.playback.notification.NotificationArtworkPolicy
import com.ssilencce.playback.notification.SsilencceMediaNotificationProvider
import com.ssilencce.playback.processor.BalanceAudioProcessor
import com.ssilencce.playback.processor.MonoAudioProcessor
import com.ssilencce.playback.processor.ReplayGainAudioProcessor
import com.ssilencce.playback.renderer.AlacWorkaroundCodecSelector
import com.ssilencce.playback.renderer.SsilencceRenderersFactory
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.screen.lockscreen.LockScreenPlaybackClock
import com.ssilencce.ui.screen.lockscreen.LockScreenScreenOffReceiver
import com.ssilencce.util.ACTIVE_NAMED_QUEUE_ID
import com.ssilencce.util.AUDIO_FOCUS_MODE
import com.ssilencce.util.AppVisibility
import com.ssilencce.util.CLEAR_QUEUE_ON_COMPLETION
import com.ssilencce.util.CROSSFADE_DURATION
import com.ssilencce.util.CROSSFADE_GAPLESS_ALBUMS
import com.ssilencce.util.CROSSFADE_ON_MANUAL_SKIP
import com.ssilencce.util.ENABLE_HISTORY
import com.ssilencce.util.GAPLESS_PLAYBACK
import com.ssilencce.util.GHOST_FOLDERS
import com.ssilencce.util.GHOST_MODE_ENABLED
import com.ssilencce.util.GhostMode
import com.ssilencce.util.HEADSET_CLICK_DELAY_MS
import com.ssilencce.util.HEADSET_DOUBLE_CLICK_ACTION
import com.ssilencce.util.HEADSET_FOUR_PLUS_SEEK_FORWARD
import com.ssilencce.util.HEADSET_LONG_PRESS_ACTION
import com.ssilencce.util.HEADSET_SINGLE_CLICK_ACTION
import com.ssilencce.util.HEADSET_TRIPLE_CLICK_ACTION
import com.ssilencce.util.IGNORE_AUDIO_FOCUS
import com.ssilencce.util.LOCK_SCREEN_PLAYER_ENABLED
import com.ssilencce.util.MP3_INDEX_SEEKING
import com.ssilencce.util.NOTIFICATION_ALWAYS_EXPANDED
import com.ssilencce.util.NOTIFICATION_BACKGROUND_MODE
import com.ssilencce.util.NOTIFICATION_BROADCAST_ALBUM_ART
import com.ssilencce.util.NOTIFICATION_COMPACT_BUTTONS
import com.ssilencce.util.NOTIFICATION_CUSTOM_BACKGROUND_COLOR
import com.ssilencce.util.NOTIFICATION_EXPANDED_BUTTONS
import com.ssilencce.util.NOTIFICATION_REPLACE_CLOSE_WITH_ADD_TO_PLAYLIST
import com.ssilencce.util.NOTIFICATION_STYLE
import com.ssilencce.util.PAUSE_ON_ZERO_VOLUME
import com.ssilencce.util.PLAY_ON_STARTUP_MODE
import com.ssilencce.util.PREVIOUS_BUTTON_ACTION
import com.ssilencce.util.PlayOnStartupMode
import com.ssilencce.util.Preferences
import com.ssilencce.util.Preferences.requireString
import com.ssilencce.util.QUEUE_NEXT_MODE
import com.ssilencce.util.REMEMBER_TRACK_POSITION
import com.ssilencce.util.REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE
import com.ssilencce.util.REPEAT_LOAD_NEXT_AND_PAUSE_AVAILABLE
import com.ssilencce.util.REWIND_WITH_BACK
import com.ssilencce.util.SEEK_BACK_INTERVAL
import com.ssilencce.util.SEEK_FORWARD_INTERVAL
import com.ssilencce.util.SEEK_INTERVAL
import com.ssilencce.util.SESSION_RECOVERY
import com.ssilencce.util.SLEEP_TIMER_STOP_MUSIC_ONLY_IN_FOREGROUND
import com.ssilencce.util.STOP_WHEN_CLOSED_FROM_RECENTS
import com.ssilencce.util.SongPlayCountHelper
import com.ssilencce.util.WIDGET_DYNAMIC_COLORS
import com.ssilencce.util.WIDGET_IMAGE_CORNER_RADIUS
import com.ssilencce.util.WIDGET_SMALL_LAYOUT_STYLE
import com.ssilencce.util.WIDGET_THIRD_LINE_CONTENT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import kotlin.random.Random

/**
 * Process-wide one-shot guard for session recovery. A service recreation in the same process
 * must not auto-resume again after the first recovery attempt.
 */
internal object SessionRecoveryGuard {
    @Volatile
    var consumed: Boolean = false
}

@OptIn(UnstableApi::class)
class PlaybackService :
    MediaLibraryService(),
    MediaLibrarySession.Callback,
    Player.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val serviceScope = CoroutineScope(Job() + Main)
    private val namedQueueSwitchMutex = Mutex()
    private val uiHandler = Handler(Looper.getMainLooper())

    private val glanceManager by lazy { GlanceAppWidgetManager(applicationContext) }

    private val preferences: SharedPreferences by inject()
    private val sleepTimer: SleepTimer by inject()
    private val equalizerManager: EqualizerManager by inject()
    private val audioOutputObserver: AudioOutputObserver by inject()
    private val repository: Repository by inject()
    private val trackPositionDao: TrackPositionDao by inject()
    private val songPlaybackParamDao: SongPlaybackParamDao by inject()

    private val libraryProvider = LibraryProvider(repository)
    private val songPlayCountHelper = SongPlayCountHelper()
    private val mediaStoreObserver =
        MediaStoreObserver(uiHandler) {
            mediaSession?.broadcastCustomCommand(
                SessionCommand(Playback.EVENT_MEDIA_CONTENT_CHANGED, Bundle.EMPTY),
                Bundle.EMPTY,
            )
        }

    private val playerThread = HandlerThread("Ssilencce-ExoPlayer", Process.THREAD_PRIORITY_AUDIO)
    private val balanceProcessor: BalanceAudioProcessor by inject()
    private val monoProcessor: MonoAudioProcessor by inject()
    private val replayGainProcessor: ReplayGainAudioProcessor by inject()

    private lateinit var nm: NotificationManager
    private lateinit var persistentStorage: PersistentStorage
    private lateinit var audioFocusController: AudioFocusController
    private lateinit var customCommands: List<CommandButton>
    private lateinit var player: AdvancedForwardingPlayer
    private lateinit var castPlayback: CastPlayback
    private lateinit var sessionPlayer: Player
    private lateinit var systemNotificationProvider: SsilencceMediaNotificationProvider
    private lateinit var customNotificationProvider: CustomLayoutMediaNotificationProvider
    private var mediaSession: MediaLibrarySession? = null

    /** The player that is currently audible: the Cast receiver when a session is active. */
    private val activePlayer: Player
        get() = if (::sessionPlayer.isInitialized) sessionPlayer else player

    private val isCasting: Boolean
        get() =
            ::sessionPlayer.isInitialized &&
                sessionPlayer.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE

    private val notificationProgressRunnable =
        object : Runnable {
            override fun run() {
                if (!::player.isInitialized || !activePlayer.isPlaying) return
                if (!Preferences.notificationStyle.showsSeekBar) return
                refreshNotificationActions()
                uiHandler.postDelayed(this, NOTIFICATION_PROGRESS_TICK_MS)
            }
        }

    private var eqStateHandler: Handler? = Handler(Looper.getMainLooper())

    private var errorRecoveryRetryCount = 0
    private var pausedByMute = false
    private var playDeferredByMute = false
    private val resumeAutoplayPolicy = ResumeAutoplayPolicy()
    private var hasSetUnshuffledOrder = false
    private var stopIndex = -1
    private var queueStopsAtEndOfCurrentItem = false
    private var sleepTimerStopsAtEndOfCurrentItem = false

    private var lastKnownPositionMs = 0L
    private var lastKnownDurationMs = 0L
    private var lastKnownMediaId: String? = null

    @Volatile
    private var currentSongIsFavorite = false
    private var lastPositionPersistedAt = 0L
    private var suppressNextPerTrackRestore = false
    private var positionSeekGeneration = 0L

    private val headsetButtonController = HeadsetButtonController()
    private val headsetKeyCodes =
        setOf(
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        )
    private val headsetSeekKeyCodes =
        setOf(
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND,
        )
    private val headsetClickRunnable = Runnable { dispatchScheduledHeadsetClick() }

    private var lastPlaybackState: PlaybackState? = null
    private var widgetUpdateJob: Job? = null
    private lateinit var volumeController: PlaybackVolumeController
    private lateinit var fadeController: PlaybackFadeController
    private lateinit var crossfadeController: CrossfadeController
    private var crossfadeCommitMediaId: String? = null

    private var globalTempo: TempoState = TempoState.Unspecified
    private var songPlaybackParams: SongPlaybackParamEntity? = null
    private var abRepeat: AbRepeatRange? = null
    private var lastAbRepeatSeekAt = 0L
    private val abRepeatRunnable = Runnable { checkAbRepeat() }

    private data class AbRepeatRange(
        val startMs: Long,
        val endMs: Long,
    )

    val isInTransientFocusLoss: Boolean
        get() =
            if (::audioFocusController.isInitialized) {
                audioFocusController.isInTransientLoss
            } else {
                player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
            }

    val isPlaying: Boolean
        get() = activePlayer.isPlaying

    /**
     * Pauses playback for a user or policy action. This must disarm auto-resume even when the
     * player is already paused, because no playWhenReady callback is emitted for a no-op pause.
     */
    private fun pausePlayback() {
        pausedByMute = false
        playDeferredByMute = false
        resumeAutoplayPolicy.onUserPause()
        if (::audioFocusController.isInitialized) {
            audioFocusController.onUserPause()
        }
        if (::crossfadeController.isInitialized) {
            crossfadeController.cancel()
        }
        activePlayer.pause()
    }

    private fun pauseForDisconnect() {
        val playbackWasActive = player.playWhenReady
        pausePlayback()
        resumeAutoplayPolicy.onDisconnectPause(playbackWasActive)
    }

    private fun resumeAfterConnect() {
        if (!resumeAutoplayPolicy.canResumeOnConnect()) return
        resumeAutoplayPolicy.consumeResumeOnConnect()
        activePlayer.play()
    }

    /**
     * Sleep-timer end: stop the music, and quit the playback session when the timer ended while
     * the app UI was not visible and the user opted into quitting.
     */
    private fun stopPlaybackForSleepTimer() {
        pausePlayback()
        quitPlaybackServiceIfSleepTimerRequested()
    }

    private fun quitPlaybackServiceIfSleepTimerRequested() {
        // "Stop only music if app UI is in foreground": the session survives a foreground stop
        // so the user can keep browsing, but a background stop also releases the media service.
        if (preferences.getBoolean(SLEEP_TIMER_STOP_MUSIC_ONLY_IN_FOREGROUND, false) &&
            !AppVisibility.isUiVisible
        ) {
            // Defer out of player callbacks: stopSelf() releases the session and the player.
            uiHandler.post { pauseAllPlayersAndStopSelf() }
        }
    }

    /**
     * The player has a single pause-at-end flag shared by the queue stop position and the sleep
     * timer. Deriving it from both sources lets either one cancel without clearing the other.
     */
    private fun updatePauseAtEndOfMediaItems() {
        if (::player.isInitialized) {
            player.exoPlayer.pauseAtEndOfMediaItems = queueStopsAtEndOfCurrentItem || sleepTimerStopsAtEndOfCurrentItem
        }
    }

    private fun currentHeadsetConfig(): HeadsetButtonConfig =
        HeadsetButtonConfig(
            single = Preferences.headsetSingleClickAction,
            double = Preferences.headsetDoubleClickAction,
            triple = Preferences.headsetTripleClickAction,
            longPress = Preferences.headsetLongPressAction,
            fourPlusSeekForward = Preferences.headsetFourPlusSeekForward,
            delayMs = Preferences.headsetClickDelayMs,
        )

    private fun handleHeadsetDecision(decision: HeadsetButtonDecision) {
        when (decision) {
            is HeadsetButtonDecision.Dispatch -> {
                uiHandler.removeCallbacks(headsetClickRunnable)
                executeHeadsetAction(decision.action)
            }

            is HeadsetButtonDecision.Schedule -> {
                uiHandler.removeCallbacks(headsetClickRunnable)
                uiHandler.postDelayed(headsetClickRunnable, decision.delayMs)
            }

            HeadsetButtonDecision.Consumed -> {
                Unit
            }
        }
    }

    private fun dispatchScheduledHeadsetClick() {
        val config = currentHeadsetConfig()
        handleHeadsetDecision(headsetButtonController.onScheduledDispatch(config))
    }

    private fun executeHeadsetAction(action: HeadsetButtonAction) {
        if (!::player.isInitialized) return
        val target = activePlayer
        when (action) {
            HeadsetButtonAction.PlayPause -> if (target.isPlaying) pausePlayback() else target.play()
            HeadsetButtonAction.Next -> target.seekToNext()
            HeadsetButtonAction.Previous -> target.seekToPrevious()
            HeadsetButtonAction.SeekForward -> if (isSeekControlsAvailable()) target.seekForward() else target.seekToNext()
            HeadsetButtonAction.SeekBackward -> if (isSeekControlsAvailable()) target.seekBack() else target.seekToPrevious()
            HeadsetButtonAction.Nothing -> Unit
        }
    }

    private fun resetHeadsetClickState() {
        uiHandler.removeCallbacks(headsetClickRunnable)
        headsetButtonController.reset()
    }

    private val shuffleCommand: CommandButton
        get() =
            if (activePlayer.shuffleModeEnabled) {
                customCommands[1]
            } else {
                customCommands[0]
            }

    private val repeatCommand: CommandButton
        get() =
            when (activePlayer.repeatMode) {
                Player.REPEAT_MODE_ALL -> customCommands[3]
                Player.REPEAT_MODE_ONE -> customCommands[4]
                else -> customCommands[2]
            }

    private val pauseOnZeroVolume: Boolean
        get() = preferences.getBoolean(PAUSE_ON_ZERO_VOLUME, false)
    private val sequentialTimeline: Boolean
        get() = preferences.getString(QUEUE_NEXT_MODE, "1") == "1"
    private val handleAudioFocus: Boolean
        get() = false
    private val rememberTrackPosition: Boolean
        get() = preferences.getBoolean(REMEMBER_TRACK_POSITION, false)
    private val repeatLoadNextPauseActive: Boolean
        get() = preferences.getBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE, false)
    private val repeatLoadNextPauseAvailable: Boolean
        get() = preferences.getBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_AVAILABLE, false)
    private val repeatPolicyMode: RepeatPolicy.Mode
        get() = RepeatPolicy.current(player.repeatMode, repeatLoadNextPauseActive)
    private val maxSeekToPreviousMs: Long
        get() = if (Preferences.previousButtonRestartsTrack()) REWIND_INSTEAD_PREVIOUS_MILLIS else 0
    private val seekForwardInterval: Long
        get() = Preferences.seekForwardIntervalSeconds * 1000L
    private val seekBackInterval: Long
        get() = Preferences.seekBackIntervalSeconds * 1000L
    private val fadeInDuration: Long
        get() = Preferences.fadeInDurationMs.toLong()
    private val fadeOutDuration: Long
        get() = Preferences.fadeOutDurationMs.toLong()

    private fun isSeekControlsAvailable(): Boolean = Preferences.isSeekControlsAvailable(activePlayer.duration)

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        nm = requireNotNull(getSystemService<NotificationManager>())
        createNotificationChannel()

        customCommands =
            listOf(
                CommandButton
                    .Builder(CommandButton.ICON_SHUFFLE_OFF)
                    .setDisplayName(getString(R.string.shuffle_mode))
                    .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, true)
                    .build(),
                CommandButton
                    .Builder(CommandButton.ICON_SHUFFLE_ON)
                    .setDisplayName(getString(R.string.shuffle_mode))
                    .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, false)
                    .build(),
                CommandButton
                    .Builder(CommandButton.ICON_REPEAT_OFF)
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_ALL)
                    .build(),
                CommandButton
                    .Builder(CommandButton.ICON_REPEAT_ALL)
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_ONE)
                    .build(),
                CommandButton
                    .Builder(CommandButton.ICON_REPEAT_ONE)
                    .setDisplayName(getString(R.string.repeat_mode))
                    .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_OFF)
                    .build(),
            )

        playerThread.start()
        player =
            AdvancedForwardingPlayer(
                ExoPlayer
                    .Builder(this)
                    .setWakeMode(C.WAKE_MODE_LOCAL)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        handleAudioFocus,
                    ).setRenderersFactory(
                        SsilencceRenderersFactory(
                            this,
                            balanceProcessor,
                            monoProcessor,
                            replayGainProcessor,
                            isGaplessEnabled = { Preferences.isGaplessPlaybackEnabled },
                        ).setEnableAudioFloatOutput(equalizerManager.audioFloatOutput.value)
                            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                            .setMediaCodecSelector(AlacWorkaroundCodecSelector())
                            .setEnableDecoderFallback(true),
                    ).setMediaSourceFactory(
                        DefaultMediaSourceFactory(
                            this,
                            ExtraExtractorsFactory(
                                DefaultExtractorsFactory()
                                    .setConstantBitrateSeekingEnabled(true)
                                    .also {
                                        if (preferences.getBoolean(MP3_INDEX_SEEKING, false)) {
                                            it.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
                                        }
                                    },
                            ),
                        ),
                    ).setSkipSilenceEnabled(equalizerManager.skipSilence.value)
                    .setHandleAudioBecomingNoisy(true)
                    .setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
                    .setSeekBackIncrementMs(seekBackInterval)
                    .setSeekForwardIncrementMs(seekForwardInterval)
                    .setPlaybackLooper(playerThread.looper)
                    .build(),
            )

        volumeController = PlaybackVolumeController(player)
        player.exoPlayer.addAnalyticsListener(
            object : AnalyticsListener {
                override fun onAudioInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: androidx.media3.common.Format,
                    decoderReuseEvaluation: DecoderReuseEvaluation?,
                ) {
                    val mediaId = player.exoPlayer.currentMediaItem?.mediaId ?: return
                    PlaybackDecoderRegistry.onFormat(mediaId, format)
                }

                override fun onAudioDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    val mediaId = player.exoPlayer.currentMediaItem?.mediaId ?: return
                    PlaybackDecoderRegistry.onDecoder(mediaId, decoderName)
                }
            },
        )
        fadeController =
            PlaybackFadeController(
                scope = serviceScope,
                volumeController = volumeController,
                fadeInDurationMs = { fadeInDuration },
                fadeOutDurationMs = { fadeOutDuration },
            )
        player.onPlayRequested = fadeController::onPlayRequested
        player.onPauseRequested = { commit ->
            if (::crossfadeController.isInitialized) crossfadeController.cancel()
            fadeController.requestPause(commit)
        }
        player.canStartPlayback = {
            !Preferences.preventPlaybackWhenMuted || !audioOutputObserver.isMutedNow()
        }
        player.onPlaybackBlocked = {
            playDeferredByMute = true
            if (AppVisibility.isUiVisible) {
                showToast(R.string.playback_muted_message)
            }
        }
        crossfadeController =
            CrossfadeController(
                mainPlayer = player.exoPlayer,
                volumeController = volumeController,
                configProvider = ::currentCrossfadeConfig,
                isCrossfadeAllowed = {
                    !isCasting &&
                        abRepeat == null &&
                        !queueStopsAtEndOfCurrentItem &&
                        !sleepTimerStopsAtEndOfCurrentItem &&
                        !repeatLoadNextPauseActive
                },
                isGaplessPlaybackEnabled = { Preferences.isGaplessPlaybackEnabled },
                baseVolumeProvider = { volumeController.currentOutputVolume },
                secondaryPlayerFactory = ::createCrossfadePlayer,
                configureSecondaryProcessors = ::applyCrossfadeProcessorState,
                resolveIncomingGain = ::resolveCrossfadeReplayGain,
                onCommitRequested = { mediaId -> crossfadeCommitMediaId = mediaId },
            )
        player.onSeekToNextRequested = crossfadeController::onNextCommand
        player.onSeekToPreviousRequested = crossfadeController::cancel

        player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(0, 0, Random.nextLong())
        ShuffleStateHolder.update(ShuffleModeState.Off)
        player.setSequentialTimelineEnabled(sequentialTimeline)
        player.previousButtonRestartsTrack = Preferences.previousButtonRestartsTrack()
        castPlayback = CastPlaybackProvider.get(this)
        sessionPlayer = castPlayback.createSessionPlayer(player)
        sessionPlayer.addListener(this)

        audioFocusController =
            AudioFocusController(
                context = this,
                player = player.exoPlayer,
                preferences = preferences,
                volumeController = volumeController,
                onPlaybackStart = { fadeController.onPlayRequested(player.isPlaying) },
                onPendingAskChanged = ::broadcastAudioFocusAsk,
            )
        player.onPlaybackIntent = { playWhenReady ->
            if (!playWhenReady) {
                pausedByMute = false
                playDeferredByMute = false
                resumeAutoplayPolicy.onUserPause()
            }
            audioFocusController.onPlaybackIntent(playWhenReady)
        }

        mediaSession =
            with(MediaLibrarySession.Builder(this, sessionPlayer, this)) {
                setId(packageName)
                setSessionActivity(createSessionActivityIntent())
                setBitmapLoader(CacheBitmapLoader(CoilBitmapLoader(this@PlaybackService)))
                build()
            }

        setForegroundServiceTimeoutMs(FOREGROUND_SERVICE_TIMEOUT)
        systemNotificationProvider =
            SsilencceMediaNotificationProvider(
                this,
                { _ -> NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.playing_notification_name,
                favoriteStateProvider = { currentSongIsFavorite },
                seekControlsAvailableProvider = ::isSeekControlsAvailable,
            ).apply {
                setSmallIcon(R.drawable.ic_stat_music_playback)
            }
        customNotificationProvider =
            CustomLayoutMediaNotificationProvider(
                this,
                { _ -> NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.playing_notification_name,
                favoriteStateProvider = { currentSongIsFavorite },
                seekControlsAvailableProvider = ::isSeekControlsAvailable,
            )
        applyNotificationStyle(updateNotification = false)

        mediaStoreObserver.init(this)

        persistentStorage = PersistentStorage(this, serviceScope, player)
        persistentStorage.restoreState { items, shuffleOrder ->
            val shouldResume =
                !SessionRecoveryGuard.consumed &&
                    preferences.getBoolean(SESSION_RECOVERY, false) &&
                    persistentStorage.wasPlaying
            suppressNextPerTrackRestore = items.mediaItems.isNotEmpty()
            player.setMediaItems(
                NotificationArtworkPolicy.apply(items.mediaItems),
                items.startIndex,
                items.startPositionMs,
            )
            if (items.mediaItems.isEmpty()) {
                suppressNextPerTrackRestore = false
            }
            player.prepare()
            if (player.shuffleModeEnabled && shuffleOrder != null) {
                player.exoPlayer.shuffleOrder = shuffleOrder
            }
            syncShuffleState()
            if (isCasting && items.mediaItems.isNotEmpty()) {
                // The service may have been recreated while a receiver is still connected: the
                // session player is remote, so the restored queue must be sent there as well.
                sessionPlayer.setMediaItems(
                    items.mediaItems,
                    items.startIndex.coerceIn(0, items.mediaItems.lastIndex),
                    items.startPositionMs,
                )
                sessionPlayer.repeatMode = player.repeatMode
                if (sessionPlayer.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE)) {
                    sessionPlayer.shuffleModeEnabled = player.shuffleModeEnabled
                }
                sessionPlayer.prepare()
            }
            if (shouldResume && items.mediaItems.isNotEmpty()) {
                SessionRecoveryGuard.consumed = true
                activePlayer.play()
            }
        }
        startTrackPositionObserver()

        sleepTimer.addFinishListener { sleepParams ->
            val target = activePlayer
            if (sleepParams.pendingQuit || !target.playWhenReady || !target.isPlaying) {
                return@addFinishListener
            }
            if (::crossfadeController.isInitialized) {
                crossfadeController.cancel()
            }
            if (isCasting) {
                // Volume fading and the volume controller are local-only; the receiver is paused
                // directly instead.
                target.pause()
                quitPlaybackServiceIfSleepTimerRequested()
            } else if (sleepParams.fadeOut) {
                fadeController.fadeOut(sleepParams.fadeDuration) {
                    player.pauseWithoutFade()
                    quitPlaybackServiceIfSleepTimerRequested()
                }
            } else {
                stopPlaybackForSleepTimer()
            }
        }
        sleepTimer.addStopAtEndListener { armed ->
            uiHandler.post {
                sleepTimerStopsAtEndOfCurrentItem = armed
                updatePauseAtEndOfMediaItems()
                if (::crossfadeController.isInitialized) {
                    if (armed) crossfadeController.cancel() else crossfadeController.refresh()
                }
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(this)
        audioOutputObserver.startObserver()

        prepareEqualizerAndSoundSettings()
        registerReceivers()
        LockScreenPlaybackClock.onPlaybackIntentChanged(activePlayer.playWhenReady)
        updateLockScreenReceiver()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if ((!isPlaybackOngoing && !isInTransientFocusLoss) ||
            preferences.getBoolean(STOP_WHEN_CLOSED_FROM_RECENTS, false)
        ) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        if (!player.playWhenReady) {
            persistentStorage.markNotPlaying()
        }
        if (::fadeController.isInitialized) {
            fadeController.release()
        }
        if (::crossfadeController.isInitialized) {
            crossfadeController.release()
        }
        persistentStorage.finish()
        persistTrackPositionOnDestroy()
        super.onDestroy()
        if (bluetoothConnectedRegistered) {
            unregisterReceiver(bluetoothReceiver)
            bluetoothConnectedRegistered = false
        }
        if (headsetReceiverRegistered) {
            unregisterReceiver(headsetReceiver)
            headsetReceiverRegistered = false
        }
        if (lockScreenReceiverRegistered) {
            runCatching { unregisterReceiver(lockScreenReceiver) }
            lockScreenReceiverRegistered = false
        }
        eqStateHandler?.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacks(abRepeatRunnable)
        uiHandler.removeCallbacks(notificationProgressRunnable)
        if (::customNotificationProvider.isInitialized) {
            customNotificationProvider.release()
        }
        resetHeadsetClickState()
        serviceScope.cancel()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        audioOutputObserver.stopObserver()
        mediaStoreObserver.stop(this)
        mediaSession?.release()
        if (::audioFocusController.isInitialized) {
            audioFocusController.release()
        }
        if (::sessionPlayer.isInitialized) {
            sessionPlayer.removeListener(this)
        }
        if (::castPlayback.isInitialized) {
            castPlayback.release()
        }
        if (::sessionPlayer.isInitialized) {
            // In Cast flavors this releases both the remote player and the local player.
            sessionPlayer.release()
        } else {
            player.removeListener(this)
            player.release()
        }
        playerThread.quitSafely()
        equalizerManager.release()
        sleepTimer.release()
        isRunning = false
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_TOGGLE_FAVORITE -> {
                toggleFavorite()
                return START_STICKY
            }

            ACTION_TOGGLE_SHUFFLE -> {
                toggleShuffle()
                return START_STICKY
            }

            ACTION_CYCLE_REPEAT -> {
                cycleRepeat()
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val availableCommands =
            MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()

        availableCommands.add(SessionCommand(Playback.CYCLE_REPEAT, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_SHUFFLE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.GET_SHUFFLE_STATE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SEEK_FORWARD, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SEEK_BACK, Bundle.EMPTY))
        if (controller.uid == Process.myUid()) {
            availableCommands.add(SessionCommand(Playback.ADD_TO_PLAYLIST, Bundle.EMPTY))
            availableCommands.add(SessionCommand(Playback.CLOSE_NOTIFICATION, Bundle.EMPTY))
            APP_INTERNAL_COMMANDS.forEach { availableCommands.add(SessionCommand(it, Bundle.EMPTY)) }
        }
        availableCommands.add(SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_STOP_POSITION, Bundle.EMPTY))
        if (controller.uid == Process.myUid()) {
            availableCommands.add(
                SessionCommand(Playback.EVENT_REPEAT_LOAD_NEXT_PAUSE_CHANGED, Bundle.EMPTY),
            )
            availableCommands.add(
                SessionCommand(Playback.EVENT_SHUFFLE_STATE_CHANGED, Bundle.EMPTY),
            )
        }

        val resultBuilder =
            MediaSession.ConnectionResult
                .AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableCommands.build())
                .setAvailablePlayerCommands(connectionResult.availablePlayerCommands)
        if (isAutoController(session, controller)) {
            resultBuilder.setMediaButtonPreferences(autoMediaButtonPreferences())
        }
        return resultBuilder.build()
    }

    private fun isAutoController(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): Boolean = session.isAutomotiveController(controller) || session.isAutoCompanionController(controller)

    private fun autoMediaButtonPreferences(): ImmutableList<CommandButton> {
        val hasTimeline = !player.currentTimeline.isEmpty
        if (!hasTimeline) return ImmutableList.of()
        return ImmutableList
            .builder<CommandButton>()
            .addAll(
                AutoMediaButtons.build(
                    context = this,
                    hasTimeline = true,
                    seekControlsAvailable = isSeekControlsAvailable(),
                    favorite = currentSongIsFavorite,
                    shuffleEnabled = player.shuffleModeEnabled,
                ),
            ).add(repeatCommand)
            .build()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int,
    ): Int {
        if (playerCommand == Player.COMMAND_PLAY_PAUSE &&
            controller.uid != Process.myUid() &&
            !player.playWhenReady &&
            resumeAutoplayPolicy.shouldBlockExternalPlay()
        ) {
            Log.d(TAG, "Blocked external play request while preventing unwanted autoplay")
            return SessionResult.RESULT_ERROR_INVALID_STATE
        }
        return super.onPlayerCommandRequest(session, controller, playerCommand)
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent,
    ): Boolean {
        val ke = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        if (ke == null) {
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }
        if (ke.keyCode in headsetSeekKeyCodes && !isSeekControlsAvailable()) {
            // Dedicated FF/RW keys fall back to the next/previous item for short audio.
            if (ke.action == KeyEvent.ACTION_DOWN && ke.repeatCount == 0) {
                if (ke.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                    activePlayer.seekToNext()
                } else {
                    activePlayer.seekToPrevious()
                }
            }
            return true
        }
        if (ke.keyCode !in headsetKeyCodes) {
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }
        if (ke.action == KeyEvent.ACTION_DOWN) {
            val config = currentHeadsetConfig()
            val decision =
                if (ke.repeatCount > 0) {
                    headsetButtonController.onRepeat(config)
                } else {
                    headsetButtonController.onDown(config)
                }
            handleHeadsetDecision(decision)
        }
        return true
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        equalizerManager.setSessionId(audioSessionId)
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val libraryParams =
            LibraryParams
                .Builder()
                .setOffline(true)
                .setRecent(true)
                .setSuggested(false)
                .build()
        val mediaItem =
            when {
                params?.isRecent == true -> {
                    MediaItem
                        .Builder()
                        .setMediaId(MediaIDs.RECENT_SONGS)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .build(),
                        ).build()
                }

                else -> {
                    MediaItem
                        .Builder()
                        .setMediaId(MediaIDs.ROOT)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .build(),
                        ).build()
                }
            }
        return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, libraryParams))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val currentQueue =
            if (parentId == MediaIDs.CURRENT_QUEUE) {
                captureCurrentQueue()
            } else {
                emptyList()
            }
        return serviceScope.future(IO) {
            val result =
                runCatching {
                    libraryProvider.getChildren(this@PlaybackService, parentId, currentQueue)
                }
            if (result.isSuccess) {
                LibraryResult.ofItemList(result.getOrThrow(), params)
            } else {
                LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
            }
        }
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        serviceScope.future(IO) {
            val mediaItem =
                runCatching { libraryProvider.getItem(mediaId) }
                    .getOrDefault(MediaItem.EMPTY)
            if (mediaItem != MediaItem.EMPTY) {
                LibraryResult.ofItem(mediaItem, null)
            } else {
                LibraryResult.ofError(SessionError.ERROR_IO)
            }
        }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> =
        serviceScope.future(IO) {
            runCatching { libraryProvider.search(query) }
                .onSuccess { session.notifySearchResultChanged(browser, query, it.size, params) }

            LibraryResult.ofVoid()
        }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        Futures.immediateFuture(
            LibraryResult.ofItemList(libraryProvider.searchResult, params),
        )

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> =
        serviceScope.future(IO) {
            runCatching { libraryProvider.getMediaItemsForPlayback(mediaItems) }
                .getOrDefault(emptyList())
                .let { NotificationArtworkPolicy.apply(it) }
        }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        player.exoPlayer.let { exoPlayer ->
            if (exoPlayer.shuffleOrder !is ImprovedShuffleOrder &&
                exoPlayer.shuffleOrder !is RandomizeShuffleOrder &&
                !hasSetUnshuffledOrder
            ) {
                exoPlayer.shuffleOrder =
                    buildImprovedShuffleOrder(
                        firstIndex = player.currentMediaItemIndex,
                        length = player.mediaItemCount,
                    )
            }

            (exoPlayer.shuffleOrder as? ImprovedShuffleOrder)
                ?.playerIndex = startIndex

            hasSetUnshuffledOrder = false
        }
        val autoQueue =
            if (mediaItems.any { item -> MediaIDs.splitPath(item.mediaId).firstOrNull() == MediaIDs.CURRENT_QUEUE }) {
                captureCurrentQueue()
            } else {
                emptyList()
            }
        return serviceScope
            .future(IO) {
                val resolved =
                    if (mediaSession.isAutomotiveController(controller) ||
                        mediaSession.isAutoCompanionController(controller)
                    ) {
                        runCatching { libraryProvider.getMediaItemsForAAOSPlayback(mediaItems, autoQueue) }
                            .getOrNull()
                            .let {
                                MediaItemsWithStartPosition(
                                    it?.first ?: emptyList(),
                                    it?.second ?: C.INDEX_UNSET,
                                    startPositionMs,
                                )
                            }
                    } else {
                        runCatching {
                            libraryProvider.getMediaItemsForPlayback(
                                mediaItems = mediaItems,
                                tryToResolveComplexPaths = true,
                            )
                        }.getOrDefault(emptyList()).let {
                            MediaItemsWithStartPosition(it, startIndex, startPositionMs)
                        }
                    }
                MediaItemsWithStartPosition(
                    NotificationArtworkPolicy.apply(resolved.mediaItems),
                    resolved.startIndex,
                    resolved.startPositionMs,
                )
            }.also { future ->
                future.addListener({
                    val result = runCatching { future.get() }.getOrNull()
                    if (result != null && result.mediaItems.isNotEmpty()) {
                        this.mediaSession?.broadcastCustomCommand(
                            SessionCommand(Playback.EVENT_PLAYBACK_STARTED, Bundle.EMPTY),
                            Bundle.EMPTY,
                        )
                    }
                }, ContextCompat.getMainExecutor(this))
            }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction in APP_INTERNAL_COMMANDS && controller.uid != Process.myUid()) {
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
        }
        return when (customCommand.customAction) {
            Playback.TOGGLE_SHUFFLE -> {
                toggleShuffle()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SEEK_FORWARD -> {
                if (player.currentMediaItem == null) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                } else {
                    if (isSeekControlsAvailable()) player.seekForward() else player.seekToNext()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            Playback.SEEK_BACK -> {
                if (player.currentMediaItem == null) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                } else {
                    if (isSeekControlsAvailable()) player.seekBack() else player.seekToPrevious()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            Playback.RESHUFFLE_QUEUE -> {
                if (isCasting ||
                    !QueueReshufflePolicy.canReshuffle(
                        shuffleEnabled = player.shuffleModeEnabled,
                        mediaItemCount = player.mediaItemCount,
                        randomize = currentShuffleState() == ShuffleModeState.Randomize,
                    )
                ) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                } else {
                    // Start the new order at the current item so the current media item and
                    // position are retained; only the shuffle order changes.
                    player.exoPlayer.shuffleOrder =
                        buildImprovedShuffleOrder(
                            firstIndex = player.currentMediaItemIndex,
                            length = player.mediaItemCount,
                        )
                    persistentStorage.saveState()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            Playback.GET_SHUFFLE_STATE -> {
                Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putString(Playback.EXTRA_SHUFFLE_STATE, currentShuffleState().value)
                        },
                    ),
                )
            }

            Playback.MOVE_QUEUE_BLOCK -> {
                val fromIndices =
                    customCommand.customExtras.getIntArray(Playback.EXTRA_MOVE_FROM_INDICES)
                val toIndices =
                    customCommand.customExtras.getIntArray(Playback.EXTRA_MOVE_TO_INDICES)
                val target = activePlayer
                val itemCount = target.mediaItemCount
                if (fromIndices == null ||
                    toIndices == null ||
                    fromIndices.isEmpty() ||
                    fromIndices.size != toIndices.size ||
                    fromIndices.any { it !in 0 until itemCount } ||
                    toIndices.any { it !in 0 until itemCount }
                ) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                } else {
                    val oldCurrentIndex = target.currentMediaItemIndex
                    val oldPosition = target.currentPosition
                    val oldStopIndex = stopIndex
                    val oldOrder = MutableList(itemCount) { it }
                    for (index in fromIndices.indices) {
                        val from = fromIndices[index]
                        val to = toIndices[index]
                        oldOrder.add(to, oldOrder.removeAt(from))
                        target.moveMediaItem(from, to)
                    }
                    val newIndexByOld = IntArray(itemCount)
                    oldOrder.forEachIndexed { newIndex, oldIndex ->
                        newIndexByOld[oldIndex] = newIndex
                    }
                    val expectedCurrentIndex =
                        newIndexByOld.getOrElse(oldCurrentIndex) { target.currentMediaItemIndex }
                    if (target.currentMediaItemIndex != expectedCurrentIndex) {
                        target.seekTo(expectedCurrentIndex, oldPosition)
                    }
                    QueueBatchMovePolicy
                        .remapStopAfter(
                            oldStopIndex = oldStopIndex,
                            oldToNewIndex = newIndexByOld,
                            currentMediaItemIndex = target.currentMediaItemIndex,
                        )?.let { remap ->
                            stopIndex = remap.stopIndex
                            queueStopsAtEndOfCurrentItem = remap.pauseAtEndOfMediaItems
                            updatePauseAtEndOfMediaItems()
                        }
                    if (!isCasting) {
                        persistentStorage.saveState(true)
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            Playback.CYCLE_REPEAT -> {
                cycleRepeat()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.TOGGLE_FAVORITE -> {
                toggleFavorite()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.ADD_TO_PLAYLIST -> {
                openAddToPlaylist()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.OPEN_GHOST_SEARCH -> {
                openGhostSearch()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.CLOSE_NOTIFICATION -> {
                pauseAllPlayersAndStopSelf()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.RESTORE_PLAYBACK -> {
                val playOnStartupMode = preferences.requireString(PLAY_ON_STARTUP_MODE, PlayOnStartupMode.NEVER)
                if (playOnStartupMode != PlayOnStartupMode.NEVER) {
                    CallbackToFutureAdapter.getFuture { completer ->
                        persistentStorage.waitForRestoration {
                            if (!player.currentTimeline.isEmpty) {
                                mediaSession?.broadcastCustomCommand(
                                    SessionCommand(
                                        Playback.EVENT_PLAYBACK_RESTORED,
                                        Bundle.EMPTY,
                                    ),
                                    Bundle.EMPTY,
                                )
                                completer.set(SessionResult(SessionResult.RESULT_SUCCESS))
                            } else {
                                completer.setException(IllegalStateException("Timeline is empty"))
                            }
                        }
                    }
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                }
            }

            Playback.SET_UNSHUFFLED_ORDER -> {
                hasSetUnshuffledOrder = true
                player.exoPlayer.shuffleOrder = UnshuffledShuffleOrder(player.mediaItemCount)
                syncShuffleState()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SET_STOP_POSITION -> {
                if (isCasting) {
                    return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                }
                val newStopIndex = customCommand.customExtras.getInt("index", -1)
                val canceled = newStopIndex > -1 && newStopIndex == stopIndex
                if (canceled) {
                    queueStopsAtEndOfCurrentItem = false
                    stopIndex = -1
                } else if (newStopIndex == player.currentMediaItemIndex) {
                    queueStopsAtEndOfCurrentItem = true
                    stopIndex = -1
                } else {
                    queueStopsAtEndOfCurrentItem = false
                    stopIndex = newStopIndex
                }
                updatePauseAtEndOfMediaItems()
                Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean("canceled", canceled)
                        },
                    ),
                )
            }

            Playback.SET_AB_REPEAT -> {
                val endMs = customCommand.customExtras.getLong(Playback.EXTRA_AB_END_MS, -1L)
                val startMs = customCommand.customExtras.getLong(Playback.EXTRA_AB_START_MS, 0L)
                setAbRepeat(startMs, endMs.takeIf { it > startMs })
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.GET_AB_REPEAT -> {
                val range = abRepeat
                Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(Playback.EXTRA_AB_ACTIVE, range != null)
                            putLong(Playback.EXTRA_AB_START_MS, range?.startMs ?: -1L)
                            putLong(Playback.EXTRA_AB_END_MS, range?.endMs ?: -1L)
                        },
                    ),
                )
            }

            Playback.SET_SONG_PLAYBACK_PARAMS -> {
                val songId = customCommand.customExtras.getLong(Playback.EXTRA_SONG_ID, -1L)
                val speed = customCommand.customExtras.getFloat(Playback.EXTRA_SONG_SPEED, 1f)
                val pitch = customCommand.customExtras.getFloat(Playback.EXTRA_SONG_PITCH, 1f)
                if (songId <= 0L ||
                    !speed.isFinite() ||
                    speed <= 0f ||
                    !pitch.isFinite() ||
                    pitch <= 0f
                ) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                } else {
                    serviceScope.future(IO) {
                        val params = SongPlaybackParamEntity(songId, speed, pitch)
                        songPlaybackParamDao.upsert(params)
                        withContext(Main) {
                            if (player.currentMediaItem?.mediaId?.toLongOrNull() == songId) {
                                songPlaybackParams = params
                                applyPlaybackTempo(speed, pitch)
                            }
                        }
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    }
                }
            }

            Playback.RESET_SONG_PLAYBACK_PARAMS -> {
                val songId = customCommand.customExtras.getLong(Playback.EXTRA_SONG_ID, -1L)
                if (songId <= 0L) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                } else {
                    serviceScope.future(IO) {
                        songPlaybackParamDao.delete(songId)
                        withContext(Main) {
                            if (player.currentMediaItem?.mediaId?.toLongOrNull() == songId) {
                                songPlaybackParams = null
                                applyCurrentTempo()
                            }
                        }
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    }
                }
            }

            Playback.CONFIRM_AUDIO_FOCUS -> {
                val requestId = args.getString(Playback.EXTRA_AUDIO_FOCUS_REQUEST_ID)
                val confirmed = requestId != null && audioFocusController.confirmPendingAsk(requestId)
                Futures.immediateFuture(
                    SessionResult(if (confirmed) SessionResult.RESULT_SUCCESS else SessionError.ERROR_INVALID_STATE),
                )
            }

            Playback.CANCEL_AUDIO_FOCUS -> {
                val requestId = args.getString(Playback.EXTRA_AUDIO_FOCUS_REQUEST_ID)
                val cancelled = requestId != null && audioFocusController.cancelPendingAsk(requestId)
                Futures.immediateFuture(
                    SessionResult(if (cancelled) SessionResult.RESULT_SUCCESS else SessionError.ERROR_INVALID_STATE),
                )
            }

            Playback.GET_AUDIO_FOCUS_STATE -> {
                Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putString(Playback.EXTRA_AUDIO_FOCUS_REQUEST_ID, audioFocusController.pendingAskId)
                        },
                    ),
                )
            }

            Playback.SWITCH_NAMED_QUEUE -> {
                if (isCasting) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                } else {
                    switchNamedQueue(customCommand.customExtras)
                }
            }

            else -> {
                Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        if (persistentStorage.restorationState.isRestored) {
            return Futures.immediateFailedFuture(IllegalStateException("No MediaItems saved"))
        } else {
            val settableFuture = SettableFuture.create<MediaItemsWithStartPosition>()
            persistentStorage.waitForMediaItems { items, shuffleOrder ->
                if (items.mediaItems.isNotEmpty()) {
                    if (player.shuffleModeEnabled && shuffleOrder != null) {
                        player.exoPlayer.shuffleOrder = shuffleOrder
                    }
                    syncShuffleState()
                    settableFuture.set(items)
                } else {
                    settableFuture.setException(IllegalStateException("No MediaItems saved"))
                }
            }
            return settableFuture
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (activePlayer.playbackState == Player.STATE_ENDED) {
            sleepTimer.onQueueEnded()
            if (!isCasting) {
                forgetCurrentTrackPosition()
            }
        }
        if (!isCasting &&
            player.playbackState == Player.STATE_ENDED &&
            preferences.getBoolean(CLEAR_QUEUE_ON_COMPLETION, false)
        ) {
            player.exoPlayer.clearMediaItems()
        } else if (!isCasting &&
            player.playbackState == Player.STATE_ENDED &&
            repeatPolicyMode == RepeatPolicy.Mode.LoadNextAndPause &&
            player.mediaItemCount > 0
        ) {
            // The policy repeats the queue but leaves the first item loaded and paused.
            player.exoPlayer.pause()
            player.seekToDefaultPosition(0)
        }
        refreshMediaButtonCustomLayout()
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        if (!isCasting) {
            persistentStorage.saveState(true)
        }
    }

    override fun onPlayWhenReadyChanged(
        playWhenReady: Boolean,
        reason: Int,
    ) {
        if (playWhenReady) {
            resumeAutoplayPolicy.onPlaybackStarted()
        }
        if (!isCasting && ::audioFocusController.isInitialized) {
            audioFocusController.onPlayWhenReadyChanged(playWhenReady, reason)
        }
        LockScreenPlaybackClock.onPlaybackIntentChanged(playWhenReady)
        updateLockScreenReceiver()
        if (!isCasting && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
            queueStopsAtEndOfCurrentItem = false
            sleepTimerStopsAtEndOfCurrentItem = false
            updatePauseAtEndOfMediaItems()
            val consumedPendingQuit = sleepTimer.consumePendingQuit()
            if (stopIndex == player.currentMediaItemIndex) {
                stopIndex = -1
            }
            if (consumedPendingQuit) {
                quitPlaybackServiceIfSleepTimerRequested()
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateLockScreenReceiver()
        if (!isCasting &&
            isPlaying &&
            pauseOnZeroVolume &&
            audioOutputObserver.isMutedNow()
        ) {
            // Starting while muted must not leave an inaudible player running until the next
            // volume broadcast; re-check after the player settles to avoid callback reentrancy.
            uiHandler.post {
                if (!isCasting && player.isPlaying && pauseOnZeroVolume && audioOutputObserver.isMutedNow()) {
                    pausePlayback()
                    pausedByMute = true
                }
            }
        }
        if (isCasting) {
            // The receiver owns playback; a local crossfade would fight the CastPlayer handoff.
            if (::crossfadeController.isInitialized) {
                crossfadeController.cancel()
            }
        } else {
            if (!isPlaying) {
                rememberCurrentTrackPosition()
                // Persist the inactive state immediately and again through the normal debounced path.
                // If the service is destroyed before the debounce fires, recovery must not resume a
                // queue that the user paused.
                persistentStorage.markNotPlaying()
                if (!player.currentTimeline.isEmpty) {
                    persistentStorage.saveState()
                }
            } else {
                pausedByMute = false
                fadeController.onPlaybackStarted()
                persistentStorage.saveState()
            }
            scheduleAbRepeat()
        }
        songPlayCountHelper.notifyPlayStateChanged(isPlaying)
        updateWidgets()
        updateNotificationProgressUpdates()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        syncShuffleState()
        updateWidgets()
        refreshMediaButtonCustomLayout()
        if (!isCasting) {
            persistentStorage.saveState()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        if (!isCasting && repeatMode != Player.REPEAT_MODE_OFF && repeatLoadNextPauseActive) {
            // An external Media3 repeat mode wins over the player-level pause policy.
            preferences.edit { putBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE, false) }
        }
        updateWidgets()
        refreshMediaButtonCustomLayout()
        if (!isCasting) {
            persistentStorage.saveState()
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (!isCasting && reason == Player.DISCONTINUITY_REASON_SEEK) {
            // Any seek invalidates a bookmark lookup that was started for this item.
            positionSeekGeneration++
            // A same-item seek is not followed by onMediaItemTransition, so refresh the sample
            // used when the user immediately skips to another track.
            if (oldPosition.mediaItem?.mediaId == newPosition.mediaItem?.mediaId) {
                lastKnownPositionMs = newPosition.positionMs.coerceAtLeast(0L)
                val duration = player.duration
                if (duration > 0) {
                    lastKnownDurationMs = duration
                }
            }
            if (Preferences.notificationStyle.showsSeekBar) {
                refreshNotificationActions()
            }
        }
        if (!isCasting) {
            scheduleAbRepeat()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        if (!isCasting) {
            scheduleAbRepeat()
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // A crossfade commit reaches the player as a seek; present it to the rest of the
        // service as a normal automatic transition so play counts, scrobbles and sleep-timer
        // song accounting behave exactly like queue advancement.
        val isCrossfadeCommit = crossfadeCommitMediaId != null && mediaItem?.mediaId == crossfadeCommitMediaId
        crossfadeCommitMediaId = null
        val transitionReason =
            if (isCrossfadeCommit && reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            } else {
                reason
            }
        if (!isCasting) {
            fadeController.commitPendingPause()
            handleTrackPositionTransition(mediaItem, transitionReason)
            // An A-B loop is scoped to a single track and must not leak into the next one.
            abRepeat = null
            lastAbRepeatSeekAt = 0L
            uiHandler.removeCallbacks(abRepeatRunnable)
            applySongPlaybackParams(mediaItem)
            applyNotificationArtworkPolicy(player.currentMediaItemIndex)
        }
        val isPlaying = activePlayer.isPlaying

        serviceScope.launch(IO) {
            val newSong = repository.songByMediaItem(mediaItem)

            val previousSong = songPlayCountHelper.song
            val shouldBumpPlayCount = songPlayCountHelper.shouldBumpPlayCount()
            songPlayCountHelper.notifySongChanged(newSong, isPlaying)

            if (newSong != Song.emptySong) {
                if (replayGainProcessor.useEmbeddedTags) {
                    replayGainProcessor.currentGain = ReplayGainTagExtractor.getReplayGain(newSong)
                } else {
                    replayGainProcessor.currentGain = null
                }
                if (preferences.getBoolean(ENABLE_HISTORY, true)) {
                    repository.upsertSongInHistory(newSong)
                }
                if (NetworkFeature.Lastfm.NowPlaying.isAvailable) {
                    launch { repository.updateNowPlaying(ScrobblingService.Lastfm, newSong) }
                }
                if (NetworkFeature.ListenBrainz.NowPlaying.isAvailable) {
                    launch { repository.updateNowPlaying(ScrobblingService.ListenBrainz, newSong) }
                }
            }
            if (previousSong != Song.emptySong) {
                val timestampMillis = System.currentTimeMillis()
                val timestampSeconds = (timestampMillis / 1000)
                if (shouldBumpPlayCount) {
                    repository.insertOrIncrementPlayCount(
                        song = previousSong,
                        timePlayed = timestampMillis,
                    )
                    if (NetworkFeature.Lastfm.Scrobbling.isAvailable) {
                        launch { repository.scrobble(ScrobblingService.Lastfm, previousSong, timestampSeconds) }
                    }
                    if (NetworkFeature.ListenBrainz.Scrobbling.isAvailable) {
                        launch { repository.scrobble(ScrobblingService.ListenBrainz, previousSong, timestampSeconds) }
                    }
                } else if (transitionReason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    repository.insertOrIncrementSkipCount(previousSong)
                }
            }
        }

        if (!isCasting && player.currentMediaItemIndex == stopIndex) {
            queueStopsAtEndOfCurrentItem = true
            updatePauseAtEndOfMediaItems()
        }

        if (transitionReason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            sleepTimer.onSongCompleted()
        }

        if (!isCasting && RepeatPolicy.shouldPauseLoadedItem(repeatPolicyMode, transitionReason)) {
            // Load the item at its start and leave it paused; a user seek must keep playing.
            player.exoPlayer.pause()
            player.seekToDefaultPosition()
        }

        if (!isCasting) {
            persistentStorage.saveState()
            applyRememberedTrackPosition(mediaItem, transitionReason)
        }
        updateWidgets(force = true)
        refreshMediaButtonCustomLayout()
    }

    private fun persistTrackPositionOnDestroy() {
        if (!rememberTrackPosition || !player.isPlaying) return
        val mediaId = lastKnownMediaId ?: player.currentMediaItem?.mediaId ?: return
        val duration = if (lastKnownDurationMs > 0) lastKnownDurationMs else player.duration
        val position = player.currentPosition.coerceAtLeast(0L)
        if (duration < LONG_TRACK_THRESHOLD_MS || position < TRACK_START_TOLERANCE_MS) return
        TrackPositionWriter.persistFinalSnapshot(
            dao = trackPositionDao,
            snapshot = TrackPositionSnapshot(mediaId, position, duration),
        )
    }

    private fun startTrackPositionObserver() =
        serviceScope.launch {
            while (true) {
                delay(TRACK_POSITION_TICK_MS)
                if (isCasting || !player.isPlaying) continue
                val mediaId = player.currentMediaItem?.mediaId ?: continue
                val duration = player.duration
                lastKnownMediaId = mediaId
                lastKnownPositionMs = player.currentPosition
                lastKnownDurationMs = duration
                val now = System.currentTimeMillis()
                if (rememberTrackPosition &&
                    duration >= LONG_TRACK_THRESHOLD_MS &&
                    now - lastPositionPersistedAt >= TRACK_POSITION_PERSIST_INTERVAL_MS
                ) {
                    persistTrackPosition(mediaId, lastKnownPositionMs)
                    lastPositionPersistedAt = now
                }
            }
        }

    private fun handleTrackPositionTransition(
        nextMediaItem: MediaItem?,
        reason: Int,
    ) {
        val previousMediaId = lastKnownMediaId
        // At onMediaItemTransition the player already points at the next item, so currentPosition
        // is not the previous track's position. lastKnown* is the last sample of the old track.
        val previousPosition = lastKnownPositionMs.coerceAtLeast(0L)
        val previousDuration = lastKnownDurationMs
        if (previousMediaId != null && rememberTrackPosition) {
            val completed =
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT ||
                    (
                        previousDuration > 0 &&
                            previousPosition >= previousDuration - TRACK_END_TOLERANCE_MS
                    )
            if (completed) {
                TrackPositionWriter.delete(trackPositionDao, previousMediaId)
            } else if (previousDuration >= LONG_TRACK_THRESHOLD_MS &&
                previousPosition >= TRACK_START_TOLERANCE_MS
            ) {
                TrackPositionWriter.persist(trackPositionDao, previousMediaId, previousPosition)
                TrackPositionWriter.prune(trackPositionDao, MAX_REMEMBERED_TRACKS)
            }
        }
        lastKnownMediaId = nextMediaItem?.mediaId
        lastKnownPositionMs = 0L
        lastKnownDurationMs = 0L
        lastPositionPersistedAt = 0L
    }

    private fun applyRememberedTrackPosition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // The first transition during a full-session restore must keep the queue-level
        // position instead of applying a per-track position from another session. Consume the
        // marker even when the feature is off or the item is null so it cannot suppress a later
        // user transition.
        if (suppressNextPerTrackRestore) {
            suppressNextPerTrackRestore = false
            return
        }
        if (mediaItem == null) return
        if (!rememberTrackPosition) return
        if (persistentStorage.restorationState != PersistentStorage.RestorationState.Restored) {
            return
        }
        // The queue-level restore already seeks to the saved session position.
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
            player.currentPosition >= TRACK_START_TOLERANCE_MS
        ) {
            return
        }
        val mediaId = mediaItem.mediaId
        val generation = positionSeekGeneration
        serviceScope.launch(IO) {
            val saved = trackPositionDao.position(mediaId) ?: return@launch
            val duration = withContext(Main) { player.duration }
            val position = saved.positionMs
            if (position < TRACK_START_TOLERANCE_MS) return@launch
            if (duration > 0 && duration - position < TRACK_END_TOLERANCE_MS) return@launch
            if (duration in 1 until LONG_TRACK_THRESHOLD_MS) return@launch
            withContext(Main) {
                if (generation == positionSeekGeneration &&
                    player.currentMediaItem?.mediaId == mediaId
                ) {
                    player.seekTo(position)
                }
            }
        }
    }

    private fun rememberCurrentTrackPosition() {
        if (!rememberTrackPosition) return
        val mediaId = player.currentMediaItem?.mediaId ?: lastKnownMediaId ?: return
        val duration = if (lastKnownDurationMs > 0) lastKnownDurationMs else player.duration
        // Pause must use the live player position and refresh the transition sample; otherwise
        // a subsequent skip can persist an older observer sample over this accurate pause.
        val position = player.currentPosition.coerceAtLeast(0L)
        lastKnownMediaId = mediaId
        lastKnownPositionMs = position
        lastKnownDurationMs = duration
        if (duration < LONG_TRACK_THRESHOLD_MS) return
        if (position < TRACK_START_TOLERANCE_MS) {
            TrackPositionWriter.delete(trackPositionDao, mediaId)
            return
        }
        if (position >= duration - TRACK_END_TOLERANCE_MS) {
            TrackPositionWriter.delete(trackPositionDao, mediaId)
        } else {
            persistTrackPosition(mediaId, position)
        }
    }

    private fun persistTrackPosition(
        mediaId: String,
        positionMs: Long,
    ) {
        TrackPositionWriter.persist(trackPositionDao, mediaId, positionMs)
        TrackPositionWriter.prune(trackPositionDao, MAX_REMEMBERED_TRACKS)
    }

    private fun forgetCurrentTrackPosition() {
        val mediaId = player.currentMediaItem?.mediaId ?: lastKnownMediaId ?: return
        lastKnownPositionMs = 0L
        TrackPositionWriter.delete(trackPositionDao, mediaId)
    }

    private fun applySongPlaybackParams(mediaItem: MediaItem?) {
        // Reset to the global tempo immediately so the previous song's override cannot bleed
        // into the new one while its row is being read.
        songPlaybackParams = null
        applyCurrentTempo()
        val mediaId = mediaItem?.mediaId ?: return
        val songId = mediaId.toLongOrNull() ?: return
        serviceScope.launch(IO) {
            val params = songPlaybackParamDao.params(songId) ?: return@launch
            withContext(Main) {
                if (player.currentMediaItem?.mediaId == mediaId) {
                    songPlaybackParams = params
                    applyPlaybackTempo(params.speed, params.pitch)
                }
            }
        }
    }

    private fun applyCurrentTempo() {
        val params = songPlaybackParams
        applyPlaybackTempo(
            params?.speed ?: globalTempo.speed,
            params?.pitch ?: globalTempo.actualPitch,
        )
    }

    private fun applyPlaybackTempo(
        speed: Float,
        pitch: Float,
    ) {
        val current = player.playbackParameters
        if (current.speed != speed || current.pitch != pitch) {
            player.playbackParameters = PlaybackParameters(speed, pitch)
        }
    }

    private fun setAbRepeat(
        startMs: Long,
        endMs: Long?,
    ) {
        uiHandler.removeCallbacks(abRepeatRunnable)
        lastAbRepeatSeekAt = 0L
        if (endMs == null) {
            abRepeat = null
            if (::crossfadeController.isInitialized) {
                crossfadeController.refresh()
            }
            return
        }
        val safeStart = startMs.coerceAtLeast(0L)
        val duration = player.duration
        val safeEnd = if (duration > 0) endMs.coerceAtMost(duration) else endMs
        if (safeEnd - safeStart < MIN_AB_REPEAT_RANGE_MS) {
            abRepeat = null
            if (::crossfadeController.isInitialized) {
                crossfadeController.refresh()
            }
            return
        }
        abRepeat = AbRepeatRange(safeStart, safeEnd)
        if (::crossfadeController.isInitialized) {
            crossfadeController.cancel()
        }
        val position = player.currentPosition
        if (position < safeStart || position >= safeEnd) {
            lastAbRepeatSeekAt = SystemClock.elapsedRealtime()
            player.seekTo(safeStart)
        }
        scheduleAbRepeat()
    }

    /**
     * Schedules a single wake-up at B instead of polling the player. The player position is only
     * read when the scheduled message fires or a seek/play-state change invalidates the schedule.
     */
    private fun scheduleAbRepeat() {
        uiHandler.removeCallbacks(abRepeatRunnable)
        val range = abRepeat ?: return
        if (!player.isPlaying) return
        val position = player.currentPosition
        if (position >= range.endMs - AB_REPEAT_END_TOLERANCE_MS) {
            uiHandler.post(abRepeatRunnable)
            return
        }
        val speed = player.playbackParameters.speed.takeIf { it > 0f } ?: 1f
        val delayMs = ((range.endMs - position) / speed).toLong().coerceAtLeast(0L)
        uiHandler.postDelayed(abRepeatRunnable, delayMs)
    }

    private fun checkAbRepeat() {
        val range = abRepeat ?: return
        if (!player.isPlaying) return
        if (player.currentPosition >= range.endMs - AB_REPEAT_END_TOLERANCE_MS) {
            // A pending seek can temporarily leave currentPosition on the old side of B; avoid
            // issuing another immediate seek until the previous one had time to be processed.
            val now = SystemClock.elapsedRealtime()
            if (now - lastAbRepeatSeekAt >= MIN_AB_REPEAT_SEEK_INTERVAL_MS) {
                lastAbRepeatSeekAt = now
                player.seekTo(range.startMs)
            }
            uiHandler.postDelayed({ scheduleAbRepeat() }, AB_REPEAT_RESCHEDULE_DELAY_MS)
        } else {
            scheduleAbRepeat()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val target = activePlayer
        val nextMediaIndex = target.nextMediaItemIndex
        if (nextMediaIndex != C.INDEX_UNSET &&
            errorRecoveryRetryCount < MAX_RETRY_COUNT_AFTER_ERROR
        ) {
            errorRecoveryRetryCount++
            target.seekToNextMediaItem()
            target.prepare()
        }
        showToast(getString(R.string.playback_error_code, error.errorCodeName))
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
            events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
            events.contains(Player.EVENT_TIMELINE_CHANGED)
        ) {
            if (player.isPlaying) errorRecoveryRetryCount = 0
        }
        if (!isCasting &&
            events.contains(Player.EVENT_IS_PLAYING_CHANGED) &&
            !events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
        ) {
            updateEqualizerSessionState(player.isPlaying)
        }
        if (!isCasting &&
            events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) &&
            !events.contains(Player.EVENT_TIMELINE_CHANGED)
        ) {
            if (player.shuffleModeEnabled &&
                persistentStorage.restorationState.isRestored &&
                this.player.exoPlayer.shuffleOrder !is RandomizeShuffleOrder
            ) {
                this.player.exoPlayer.shuffleOrder =
                    buildImprovedShuffleOrder(
                        firstIndex = player.currentMediaItemIndex,
                        length = player.mediaItemCount,
                    )
            }
            syncShuffleState()
        }
    }

    /*
    override fun onTracksChanged(tracks: Tracks) {
        var sampleRate = -1
        var channelCount = -1
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val format = group.getTrackFormat(i)
                        sampleRate = format.sampleRate
                        channelCount = format.channelCount
                        break
                    }
                }
            }
        }
        audioOutputObserver.updatePlaybackFormat(sampleRate, channelCount)
    }
     */

    override fun onSharedPreferenceChanged(
        preferences: SharedPreferences,
        key: String?,
    ) {
        when (key) {
            HEADSET_SINGLE_CLICK_ACTION,
            HEADSET_DOUBLE_CLICK_ACTION,
            HEADSET_TRIPLE_CLICK_ACTION,
            HEADSET_LONG_PRESS_ACTION,
            HEADSET_FOUR_PLUS_SEEK_FORWARD,
            HEADSET_CLICK_DELAY_MS,
            -> {
                resetHeadsetClickState()
            }

            QUEUE_NEXT_MODE -> {
                player.setSequentialTimelineEnabled(sequentialTimeline)
            }

            ENABLE_HISTORY -> {
                if (!preferences.getBoolean(key, true)) {
                    serviceScope.launch(IO) {
                        repository.clearSongHistory()
                    }
                }
            }

            IGNORE_AUDIO_FOCUS -> {
                if (::audioFocusController.isInitialized) {
                    audioFocusController.onModeChanged()
                }
            }

            AUDIO_FOCUS_MODE -> {
                if (::audioFocusController.isInitialized) {
                    audioFocusController.onModeChanged()
                }
            }

            REWIND_WITH_BACK,
            PREVIOUS_BUTTON_ACTION,
            -> {
                player.previousButtonRestartsTrack = Preferences.previousButtonRestartsTrack()
                player.exoPlayer.setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
            }

            CROSSFADE_DURATION,
            CROSSFADE_ON_MANUAL_SKIP,
            CROSSFADE_GAPLESS_ALBUMS,
            GAPLESS_PLAYBACK,
            -> {
                if (::crossfadeController.isInitialized) {
                    crossfadeController.refresh()
                }
            }

            SEEK_INTERVAL,
            SEEK_FORWARD_INTERVAL,
            SEEK_BACK_INTERVAL,
            -> {
                player.exoPlayer.setSeekBackIncrementMs(seekBackInterval)
                player.exoPlayer.setSeekForwardIncrementMs(seekForwardInterval)
            }

            REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE -> {
                broadcastRepeatLoadNextPauseState(repeatLoadNextPauseActive)
            }

            REPEAT_LOAD_NEXT_AND_PAUSE_AVAILABLE -> {
                if (!repeatLoadNextPauseAvailable && repeatLoadNextPauseActive) {
                    preferences.edit { putBoolean(REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE, false) }
                }
            }

            WIDGET_DYNAMIC_COLORS,
            WIDGET_SMALL_LAYOUT_STYLE,
            WIDGET_IMAGE_CORNER_RADIUS,
            WIDGET_THIRD_LINE_CONTENT,
            GHOST_FOLDERS,
            -> {
                updateWidgets()
            }

            NOTIFICATION_COMPACT_BUTTONS,
            NOTIFICATION_EXPANDED_BUTTONS,
            NOTIFICATION_REPLACE_CLOSE_WITH_ADD_TO_PLAYLIST,
            -> {
                refreshNotificationActions()
            }

            GHOST_MODE_ENABLED -> {
                refreshNotificationActions()
                updateWidgets()
            }

            NOTIFICATION_STYLE -> {
                applyNotificationStyle()
            }

            NOTIFICATION_BACKGROUND_MODE,
            NOTIFICATION_CUSTOM_BACKGROUND_COLOR,
            NOTIFICATION_ALWAYS_EXPANDED,
            -> {
                refreshNotificationActions()
            }

            NOTIFICATION_BROADCAST_ALBUM_ART -> {
                applyNotificationArtworkPolicyToQueue()
            }

            LOCK_SCREEN_PLAYER_ENABLED -> {
                updateLockScreenReceiver()
            }
        }
    }

    private fun toggleShuffle() {
        val states =
            ShuffleCyclePolicy.states(
                standardEnabled = Preferences.shuffleStandardEnabled,
                randomizeEnabled = Preferences.shuffleRandomizeEnabled,
            )
        setShuffleState(ShuffleCyclePolicy.next(currentShuffleState(), states))
    }

    private fun currentShuffleState(): ShuffleModeState =
        when {
            !activePlayer.shuffleModeEnabled -> ShuffleModeState.Off
            !isCasting && player.exoPlayer.shuffleOrder is RandomizeShuffleOrder -> ShuffleModeState.Randomize
            else -> ShuffleModeState.Shuffle
        }

    private fun buildImprovedShuffleOrder(
        firstIndex: Int,
        length: Int,
    ): ImprovedShuffleOrder =
        ImprovedShuffleOrder(
            firstIndex = firstIndex,
            length = length,
            randomSeed = Random.nextLong(),
            keepPrefix = Preferences.shuffleBehavior == ShuffleBehavior.BelowCurrent,
        )

    private fun setShuffleState(state: ShuffleModeState) {
        if (isCasting) {
            val target = activePlayer
            if (target.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE)) {
                target.shuffleModeEnabled = state != ShuffleModeState.Off
            }
            syncShuffleState()
            return
        }
        if (currentShuffleState() == state) {
            syncShuffleState()
            return
        }
        val exoPlayer = player.exoPlayer
        val itemCount = player.mediaItemCount
        when (state) {
            ShuffleModeState.Off -> {
                player.shuffleModeEnabled = false
                exoPlayer.shuffleOrder = UnshuffledShuffleOrder(itemCount)
            }

            ShuffleModeState.Shuffle -> {
                exoPlayer.shuffleOrder = buildImprovedShuffleOrder(player.currentMediaItemIndex, itemCount)
                player.shuffleModeEnabled = true
            }

            ShuffleModeState.Randomize -> {
                exoPlayer.shuffleOrder =
                    RandomizeShuffleOrder(
                        length = itemCount,
                        belowCurrentOnly = Preferences.shuffleBehavior == ShuffleBehavior.BelowCurrent,
                    )
                player.shuffleModeEnabled = true
            }
        }
        syncShuffleState()
        persistentStorage.saveState()
    }

    /** Applies the standard fixed shuffle order, used when a named queue restores its shuffle flag. */
    private fun setStandardShuffleEnabled(enabled: Boolean) {
        if (enabled) {
            player.exoPlayer.shuffleOrder =
                buildImprovedShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount)
            player.shuffleModeEnabled = true
        } else {
            player.shuffleModeEnabled = false
            player.exoPlayer.shuffleOrder = UnshuffledShuffleOrder(player.mediaItemCount)
        }
        syncShuffleState()
    }

    private fun syncShuffleState() {
        val state = currentShuffleState()
        ShuffleStateHolder.update(state)
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_SHUFFLE_STATE_CHANGED, Bundle.EMPTY),
            Bundle().apply { putString(Playback.EXTRA_SHUFFLE_STATE, state.value) },
        )
    }

    private fun cycleRepeat() {
        val current = RepeatPolicy.current(activePlayer.repeatMode, repeatLoadNextPauseActive)
        val next = RepeatPolicy.next(current, repeatLoadNextPauseAvailable)
        activePlayer.repeatMode = RepeatPolicy.media3RepeatMode(next)
        preferences.edit {
            putBoolean(
                REPEAT_LOAD_NEXT_AND_PAUSE_ACTIVE,
                next == RepeatPolicy.Mode.LoadNextAndPause,
            )
        }
    }

    private fun broadcastRepeatLoadNextPauseState(active: Boolean) {
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_REPEAT_LOAD_NEXT_PAUSE_CHANGED, Bundle.EMPTY),
            Bundle().apply { putBoolean(Playback.EXTRA_LOAD_NEXT_PAUSE_ACTIVE, active) },
        )
    }

    private fun toggleFavorite() =
        serviceScope.launch {
            val currentMediaItem =
                activePlayer.currentMediaItem
                    ?: return@launch

            val isFavorite =
                withContext(IO) {
                    val song = repository.songByMediaItem(currentMediaItem)
                    repository.toggleFavorite(song)
                    repository.isSongFavorite(song.id)
                }

            currentSongIsFavorite = isFavorite
            updateWidgets()

            refreshMediaButtonCustomLayout()
            refreshNotificationActions()
            mediaSession?.broadcastCustomCommand(
                SessionCommand(Playback.EVENT_FAVORITE_CONTENT_CHANGED, Bundle.EMPTY),
                Bundle.EMPTY,
            )
        }

    private suspend fun buildPlaybackState(isForeground: Boolean): PlaybackState {
        val target = activePlayer
        val mediaItem = target.currentMediaItem
        val id = mediaItem?.mediaId?.toLongOrNull()
        if (mediaItem == null || id == null) return PlaybackState.empty

        val isPlaying = target.isPlaying
        val isShuffleMode = target.shuffleModeEnabled
        val repeatMode = target.repeatMode
        return withContext(IO) {
            val song = repository.songById(id)
            val isFavorite = repository.isSongFavorite(song.id)
            if (currentSongIsFavorite != isFavorite) {
                currentSongIsFavorite = isFavorite
                withContext(Main) {
                    refreshMediaButtonCustomLayout()
                    refreshNotificationActions()
                }
            }
            val result =
                SingletonImageLoader.get(this@PlaybackService).execute(
                    ImageRequest
                        .Builder(this@PlaybackService)
                        .data(song)
                        .scale(Scale.FILL)
                        .size(300)
                        .build(),
                )
            val bitmap = result.image?.toBitmap(300, 300)
            val artworkData =
                bitmap?.let {
                    val stream = ByteArrayOutputStream()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        it.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, stream)
                    } else {
                        it.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    }
                    stream.toByteArray()
                }
            val widgetTheme =
                if (preferences.getBoolean(WIDGET_DYNAMIC_COLORS, false)) {
                    val paletteColor =
                        bitmap?.let {
                            PaletteProcessor.getPaletteColor(this@PlaybackService, bitmap)
                        }
                    if (paletteColor != null) {
                        WidgetTheme(paletteColor.backgroundColor)
                    } else {
                        null
                    }
                } else {
                    null
                }
            val additionalInfo =
                MetadataField.getMetadataValue(
                    song = song,
                    fields =
                        Preferences.getExtraInfoContent(
                            key = WIDGET_THIRD_LINE_CONTENT,
                            defaultContent = Preferences.getDefaultWidgetInfo(),
                        ),
                )
            val hideGhostMetadata = GhostMode.isGhostSong(song)
            PlaybackState(
                isSimplifiedSmallLayout = preferences.getString(WIDGET_SMALL_LAYOUT_STYLE, null) == "simplified",
                isForeground = isForeground,
                isPlaying = isPlaying,
                isFavorite = if (hideGhostMetadata) false else isFavorite,
                isShuffleMode = isShuffleMode,
                repeatMode = repeatMode,
                currentTitle = if (hideGhostMetadata) getString(R.string.ghost_hidden_track) else song.title,
                currentArtist = if (hideGhostMetadata) getString(R.string.ghost_mode_title) else song.artistName,
                additionalInfo = additionalInfo.takeUnless { hideGhostMetadata },
                artworkData = artworkData.takeUnless { hideGhostMetadata },
                widgetTheme = widgetTheme.takeUnless { hideGhostMetadata },
                imageCornerRadius = preferences.getInt(WIDGET_IMAGE_CORNER_RADIUS, 8).toFloat(),
            )
        }
    }

    private data class OutgoingNamedQueueState(
        val previousQueueId: String?,
        val mediaIds: List<String>,
        val resumeIndex: Int,
        val repeatMode: Int,
        val shuffleEnabled: Boolean,
        val save: Deferred<Unit>?,
    )

    private fun switchNamedQueue(extras: Bundle): ListenableFuture<SessionResult> {
        val targetQueueId = extras.getString("queueId")
        val mediaIds = extras.getStringArrayList("mediaIds")
        if (targetQueueId == null || mediaIds.isNullOrEmpty()) {
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
        }
        val future = SettableFuture.create<SessionResult>()
        serviceScope.launch {
            try {
                namedQueueSwitchMutex.withLock {
                    val requestedItems =
                        mediaIds.map { mediaId ->
                            MediaItem.Builder().setMediaId(mediaId).build()
                        }
                    val (resolvedSongs, _) =
                        withContext(IO) { repository.songsByMediaItems(requestedItems) }
                    val restoredItems =
                        withContext(IO) {
                            NotificationArtworkPolicy.apply(resolvedSongs.toMediaItems())
                        }
                    if (restoredItems.isEmpty()) {
                        future.set(SessionResult(SessionError.ERROR_BAD_VALUE))
                        return@launch
                    }

                    val savedToRestored = IntArray(mediaIds.size) { -1 }
                    var restoredPointer = 0
                    mediaIds.forEachIndexed { savedIndex, mediaId ->
                        if (resolvedSongs.getOrNull(restoredPointer)?.id?.toString() == mediaId) {
                            savedToRestored[savedIndex] = restoredPointer
                            restoredPointer++
                        }
                    }
                    var startIndex = 0
                    var startPositionMs = 0L
                    val savedStartIndex = extras.getInt("startIndex", 0)
                    if (savedStartIndex in savedToRestored.indices) {
                        val mappedIndex = savedToRestored[savedStartIndex]
                        if (mappedIndex >= 0) {
                            startIndex = mappedIndex
                            startPositionMs = extras.getLong("startPositionMs", 0L)
                        }
                    }

                    val targetMediaIds = restoredItems.map { it.mediaId }
                    var switched = false
                    var attempts = 0
                    while (!switched && attempts < MAX_OUTGOING_SAVE_ATTEMPTS) {
                        attempts++
                        val outgoing =
                            withContext(Main) {
                                val previousQueueId = preferences.getString(ACTIVE_NAMED_QUEUE_ID, null)
                                val mediaIds = player.mediaItems.map { it.mediaId }
                                val resumeIndex = player.currentMediaItemIndex
                                val repeatMode = player.repeatMode
                                val shuffleEnabled = player.shuffleModeEnabled
                                val save =
                                    if (previousQueueId != null && previousQueueId != targetQueueId) {
                                        persistentStorage.saveNamedQueueState(previousQueueId)
                                    } else {
                                        null
                                    }
                                OutgoingNamedQueueState(
                                    previousQueueId = previousQueueId,
                                    mediaIds = mediaIds,
                                    resumeIndex = resumeIndex,
                                    repeatMode = repeatMode,
                                    shuffleEnabled = shuffleEnabled,
                                    save = save,
                                )
                            }
                        try {
                            outgoing.save?.let { save -> withContext(IO) { save.await() } }
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (error: Exception) {
                            Log.e(TAG, "Departing named queue was not saved before switch", error)
                            future.set(SessionResult(SessionError.ERROR_UNKNOWN))
                            return@launch
                        }
                        switched =
                            withContext(Main) {
                                if (preferences.getString(ACTIVE_NAMED_QUEUE_ID, null) != outgoing.previousQueueId ||
                                    player.mediaItems.map { it.mediaId } != outgoing.mediaIds ||
                                    player.currentMediaItemIndex != outgoing.resumeIndex ||
                                    player.repeatMode != outgoing.repeatMode ||
                                    player.shuffleModeEnabled != outgoing.shuffleEnabled
                                ) {
                                    return@withContext false
                                }
                                persistentStorage.expectNamedQueueSwitch(targetQueueId, targetMediaIds)
                                // Bind only after the departing snapshot is durably queued and
                                // current; setMediaItems triggers a synchronous save.
                                preferences.edit { putString(ACTIVE_NAMED_QUEUE_ID, targetQueueId) }
                                var timelineApplied = false
                                try {
                                    player.setMediaItems(restoredItems, startIndex, startPositionMs)
                                    timelineApplied = true
                                    persistentStorage.markNamedQueueSwitchApplied()
                                    player.repeatMode = extras.getInt("repeatMode", Player.REPEAT_MODE_OFF)
                                    setStandardShuffleEnabled(extras.getBoolean("shuffleEnabled", false))
                                    player.prepare()
                                    player.play()
                                } catch (throwable: Throwable) {
                                    if (!timelineApplied) {
                                        timelineApplied =
                                            player.mediaItems.map { it.mediaId } == targetMediaIds
                                        if (timelineApplied) persistentStorage.markNamedQueueSwitchApplied()
                                    }
                                    if (!timelineApplied) {
                                        persistentStorage.cancelNamedQueueSwitch()
                                        preferences.edit {
                                            if (outgoing.previousQueueId != null) {
                                                putString(ACTIVE_NAMED_QUEUE_ID, outgoing.previousQueueId)
                                            } else {
                                                remove(ACTIVE_NAMED_QUEUE_ID)
                                            }
                                        }
                                        throw throwable
                                    }
                                    // The target timeline and binding are committed. A later setup
                                    // failure must not turn the switch into a UI error or roll back
                                    // the binding to the departing queue.
                                    Log.e(TAG, "Named queue switched but setup failed", throwable)
                                }
                                true
                            }
                    }
                    if (!switched) {
                        Log.e(TAG, "Named queue switch aborted because departing state kept changing")
                        future.set(SessionResult(SessionError.ERROR_UNKNOWN))
                        return@launch
                    }
                    future.set(
                        SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            Bundle().apply { putBoolean("switched", true) },
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                future.cancel(false)
                throw cancellation
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to switch named queue", throwable)
                future.set(SessionResult(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
    }

    private fun broadcastAudioFocusAsk(requestId: String?) {
        val session = mediaSession ?: return
        val args = Bundle().apply { putString(Playback.EXTRA_AUDIO_FOCUS_REQUEST_ID, requestId) }
        session.connectedControllers.filter { it.uid == Process.myUid() }.forEach {
            session.sendCustomCommand(it, SessionCommand(Playback.EVENT_AUDIO_FOCUS_ASK, Bundle.EMPTY), args)
        }
    }

    private fun updateWidgets(
        force: Boolean = false,
        isForeground: Boolean = isPlaybackOngoing,
    ) {
        widgetUpdateJob?.cancel()
        widgetUpdateJob =
            serviceScope.launch {
                if (!force) delay(WIDGET_UPDATE_DEBOUNCE)

                val state = buildPlaybackState(isForeground)
                if (lastPlaybackState != state) {
                    lastPlaybackState = state
                    updateGlanceWidgets(state)
                }
            }
    }

    private suspend fun updateGlanceWidgets(playbackState: PlaybackState) =
        withContext(IO) {
            try {
                val ssilencceWidget = SsilencceGlanceWidget()
                val ssilencceWidgetIds = glanceManager.getGlanceIds(ssilencceWidget.javaClass)
                if (ssilencceWidgetIds.isNotEmpty()) {
                    ssilencceWidgetIds.forEach { id ->
                        updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                            playbackState
                        }
                        ssilencceWidget.update(applicationContext, id)
                    }
                }

                val cardWidget = CardWidget()
                val cardWidgetIds = glanceManager.getGlanceIds(cardWidget.javaClass)
                if (cardWidgetIds.isNotEmpty()) {
                    cardWidgetIds.forEach { id ->
                        updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                            playbackState
                        }
                        cardWidget.update(applicationContext, id)
                    }
                }

                val fullWidget = FullWidget()
                val fullWidgetIds = glanceManager.getGlanceIds(fullWidget.javaClass)
                if (fullWidgetIds.isNotEmpty()) {
                    fullWidgetIds.forEach { id ->
                        updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                            playbackState
                        }
                        fullWidget.update(applicationContext, id)
                    }
                }
            } catch (e: Exception) {
                Log.e("PlaybackService", "Couldn't update Glance widgets", e)
            }
        }

    private fun createSessionActivityIntent(): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        var notificationChannel = nm.getNotificationChannel(CHANNEL_ID)
        if (notificationChannel == null) {
            notificationChannel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.playing_notification_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.playing_notification_description)
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                        setShowBadge(false)
                    }
                }
            nm.createNotificationChannel(notificationChannel)
        }
    }

    private fun refreshMediaButtonCustomLayout() {
        val session = mediaSession ?: return
        val hasTimeline = !activePlayer.currentTimeline.isEmpty
        session.connectedControllers.forEach { controllerInfo ->
            when {
                isAutoController(session, controllerInfo) -> {
                    session.setMediaButtonPreferences(controllerInfo, autoMediaButtonPreferences())
                }

                session.isRemoteController(controllerInfo) -> {
                    val buttonLayout =
                        if (hasTimeline) {
                            ImmutableList.of(repeatCommand, shuffleCommand)
                        } else {
                            emptyList()
                        }
                    session.setMediaButtonPreferences(controllerInfo, buttonLayout)
                }
            }
        }
    }

    private fun captureCurrentQueue(): List<MediaItem> {
        if (!::player.isInitialized) return emptyList()
        return List(player.mediaItemCount) { index -> player.getMediaItemAt(index) }
    }

    private fun refreshNotificationActions() {
        val session = mediaSession ?: return
        onUpdateNotification(session, false)
    }

    /**
     * Installs the provider for the selected notification style and restarts the seek-bar refresh
     * loop. The system style keeps the existing Media3 provider.
     */
    private fun applyNotificationStyle(updateNotification: Boolean = true) {
        if (!::systemNotificationProvider.isInitialized || !::customNotificationProvider.isInitialized) {
            return
        }
        val style = Preferences.notificationStyle
        setMediaNotificationProvider(
            if (style == NotificationStyle.System) systemNotificationProvider else customNotificationProvider,
        )
        updateNotificationProgressUpdates()
        if (updateNotification) {
            uiHandler.post { refreshNotificationActions() }
        }
    }

    private fun updateNotificationProgressUpdates() {
        uiHandler.removeCallbacks(notificationProgressRunnable)
        if (!::player.isInitialized ||
            !activePlayer.isPlaying ||
            !Preferences.notificationStyle.showsSeekBar
        ) {
            return
        }
        uiHandler.postDelayed(notificationProgressRunnable, NOTIFICATION_PROGRESS_TICK_MS)
    }

    private fun openAddToPlaylist() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_ADD_TO_PLAYLIST
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }

    private fun openGhostSearch() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_GHOST_SEARCH
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }

    private fun applyNotificationArtworkPolicyToQueue() {
        if (!::player.isInitialized || isCasting) return
        val itemCount = player.mediaItemCount
        if (itemCount == 0) return
        val currentItems = (0 until itemCount).map { player.getMediaItemAt(it) }
        val updatedItems = NotificationArtworkPolicy.apply(currentItems)
        if (updatedItems != currentItems) {
            player.replaceMediaItems(0, itemCount, updatedItems)
        }
    }

    private fun applyNotificationArtworkPolicy(index: Int) {
        if (!::player.isInitialized || index !in 0 until player.mediaItemCount) return
        val mediaItem = player.getMediaItemAt(index)
        val updatedItem = NotificationArtworkPolicy.apply(mediaItem)
        if (updatedItem != mediaItem) {
            player.replaceMediaItem(index, updatedItem)
        }
    }

    private fun currentCrossfadeConfig(): CrossfadeConfig =
        CrossfadeConfig(
            durationMs = Preferences.crossfadeDurationMs.toLong(),
            manualSkip = Preferences.crossfadeOnManualSkip,
            respectGaplessAlbums = Preferences.crossfadeRespectsGaplessAlbums,
        )

    private fun createCrossfadePlayer(audioSessionId: Int): CrossfadePlayer =
        CrossfadePlayer.create(
            context = this,
            audioSessionId = audioSessionId,
            enableFloatOutput = equalizerManager.audioFloatOutput.value,
            skipSilence = player.exoPlayer.skipSilenceEnabled,
            mp3IndexSeeking = preferences.getBoolean(MP3_INDEX_SEEKING, false),
            isGaplessEnabled = { Preferences.isGaplessPlaybackEnabled },
        )

    private fun applyCrossfadeProcessorState(crossfadePlayer: CrossfadePlayer) {
        val (left, right) = balanceProcessor.balanceSnapshot()
        crossfadePlayer.balanceProcessor.setBalance(left, right)
        crossfadePlayer.monoProcessor.isMonoEnabled = monoProcessor.isMonoEnabled
        crossfadePlayer.replayGainProcessor.apply {
            mode = replayGainProcessor.mode
            preAmpGain = replayGainProcessor.preAmpGain
            preAmpGainWithoutTag = replayGainProcessor.preAmpGainWithoutTag
            useEmbeddedTags = replayGainProcessor.useEmbeddedTags
            constantVolume = replayGainProcessor.constantVolume
            preventClipping = replayGainProcessor.preventClipping
            headroomDb = replayGainProcessor.headroomDb
        }
    }

    private fun resolveCrossfadeReplayGain(mediaItem: MediaItem) {
        serviceScope.launch(IO) {
            val song = repository.songByMediaItem(mediaItem)
            val gain =
                if (song != Song.emptySong && replayGainProcessor.useEmbeddedTags) {
                    ReplayGainTagExtractor.getReplayGain(song)
                } else {
                    null
                }
            crossfadeController.onIncomingGain(mediaItem.mediaId, gain)
        }
    }

    private fun prepareEqualizerAndSoundSettings() {
        serviceScope.launch {
            equalizerManager.initializeEqualizer()
        }
        serviceScope.launch {
            equalizerManager.volumeState.collect { volume ->
                volumeController.setBaseVolume(volume.currentVolume)
            }
        }
        serviceScope.launch {
            equalizerManager.audioOffload.collect { audioOffloadingEnabled ->
                player.trackSelectionParameters =
                    player.trackSelectionParameters
                        .buildUpon()
                        .setAudioOffloadPreferences(
                            AudioOffloadPreferences
                                .Builder()
                                .setAudioOffloadMode(
                                    if (audioOffloadingEnabled) {
                                        AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                                    } else {
                                        AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                                    },
                                ).setIsSpeedChangeSupportRequired(true)
                                .build(),
                        ).build()
            }
        }
        serviceScope.launch {
            equalizerManager.skipSilence.collect {
                player.exoPlayer.skipSilenceEnabled = it
            }
        }
        serviceScope.launch {
            equalizerManager.tempoState.collect { tempo ->
                globalTempo = tempo
                // A per-song override owns the playback parameters until the next transition
                // or an explicit reset.
                if (songPlaybackParams == null) {
                    applyPlaybackTempo(tempo.speed, tempo.actualPitch)
                }
            }
        }
        serviceScope.launch {
            audioOutputObserver.systemVolumeState.collect {
                if (!persistentStorage.restorationState.isRestored) {
                    // Don't handle volume changes until our player is fully restored.
                    return@collect
                }
                val muted = audioOutputObserver.isMutedNow()
                if (!isCasting && pauseOnZeroVolume) {
                    if (isPlaying && muted) {
                        pausePlayback()
                        pausedByMute = true
                    } else if (pausedByMute && !muted) {
                        pausedByMute = false
                        activePlayer.play()
                    }
                }
                if (!muted && playDeferredByMute) {
                    playDeferredByMute = false
                    activePlayer.play()
                }
            }
        }
    }

    private fun updateEqualizerSessionState(isPlaying: Boolean) {
        eqStateHandler?.removeCallbacksAndMessages(null)
        if (isPlaying) {
            equalizerManager.setSessionIsActive(true)
        } else {
            eqStateHandler?.postDelayed(500) {
                equalizerManager.setSessionIsActive(false)
            }
        }
    }

    private fun registerReceivers() {
        if (!bluetoothConnectedRegistered) {
            ContextCompat.registerReceiver(
                this,
                bluetoothReceiver,
                bluetoothConnectedIntentFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            bluetoothConnectedRegistered = true
        }

        if (!headsetReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                headsetReceiver,
                headsetReceiverIntentFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            headsetReceiverRegistered = true
        }
    }

    /**
     * The screen-off receiver is intentionally dynamic and only present while playback is active
     * (or about to start), so the lock screen player costs nothing on an idle queue.
     */
    private fun updateLockScreenReceiver() {
        if (!::player.isInitialized) return
        val target = activePlayer
        val shouldRegister =
            Preferences.lockScreenPlayerEnabled &&
                (target.isPlaying || target.playWhenReady)
        if (shouldRegister == lockScreenReceiverRegistered) return
        if (shouldRegister) {
            ContextCompat.registerReceiver(
                this,
                lockScreenReceiver,
                lockScreenReceiverIntentFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        } else {
            runCatching { unregisterReceiver(lockScreenReceiver) }
        }
        lockScreenReceiverRegistered = shouldRegister
    }

    private var lockScreenReceiverRegistered = false
    private val lockScreenReceiver = LockScreenScreenOffReceiver()
    private val lockScreenReceiverIntentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)

    private var bluetoothConnectedRegistered = false
    private val bluetoothConnectedIntentFilter =
        IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
    private val bluetoothReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                            BluetoothA2dp.STATE_CONNECTED -> {
                                resumeAutoplayPolicy.onRouteConnected()
                                if (Preferences.isResumeOnConnect(true)) {
                                    resumeAfterConnect()
                                }
                            }

                            BluetoothA2dp.STATE_DISCONNECTED -> {
                                if (Preferences.isPauseOnDisconnect(true)) {
                                    pauseForDisconnect()
                                }
                            }
                        }
                    }

                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        if (context.isBluetoothA2dpConnected()) {
                            resumeAutoplayPolicy.onRouteConnected()
                            if (Preferences.isResumeOnConnect(true)) {
                                resumeAfterConnect()
                            }
                        }
                    }

                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        if (context.isBluetoothA2dpDisconnected() && Preferences.isPauseOnDisconnect(true)) {
                            pauseForDisconnect()
                        }
                    }
                }
            }
        }

    private var receivedHeadsetConnected = false
    private var headsetReceiverRegistered = false
    private val headsetReceiverIntentFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
    private val headsetReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (Intent.ACTION_HEADSET_PLUG == intent.action && !isInitialStickyBroadcast) {
                    when (intent.getIntExtra("state", -1)) {
                        0 -> {
                            if (Preferences.isPauseOnDisconnect(false)) {
                                pauseForDisconnect()
                            }
                        }

                        // Check whether the current song is empty which means the playing queue hasn't restored yet
                        1 -> {
                            resumeAutoplayPolicy.onRouteConnected()
                            if (Preferences.isResumeOnConnect(false)) {
                                if (activePlayer.currentMediaItem != null) {
                                    resumeAfterConnect()
                                } else {
                                    receivedHeadsetConnected = true
                                }
                            }
                        }
                    }
                }
            }
        }

    companion object {
        private const val TAG = "PlaybackService"
        private const val PACKAGE_NAME = "com.ssilencce"

        /** Avoids cold-starting the media service when a lightweight player wants to pause it. */
        @Volatile
        var isRunning: Boolean = false
            private set
        private val APP_INTERNAL_COMMANDS =
            setOf(
                Playback.RESTORE_PLAYBACK,
                Playback.CONFIRM_AUDIO_FOCUS,
                Playback.CANCEL_AUDIO_FOCUS,
                Playback.GET_AUDIO_FOCUS_STATE,
                Playback.SWITCH_NAMED_QUEUE,
                Playback.RESHUFFLE_QUEUE,
                Playback.MOVE_QUEUE_BLOCK,
                Playback.SET_AB_REPEAT,
                Playback.GET_AB_REPEAT,
                Playback.SET_SONG_PLAYBACK_PARAMS,
                Playback.RESET_SONG_PLAYBACK_PARAMS,
                Playback.OPEN_GHOST_SEARCH,
            )

        const val ACTION_TOGGLE_SHUFFLE = "$PACKAGE_NAME.action.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "$PACKAGE_NAME.action.ACTION_CYCLE_REPEAT"
        const val ACTION_TOGGLE_FAVORITE = "$PACKAGE_NAME.action.ACTION_TOGGLE_FAVORITE"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "playing_notification"

        private const val MAX_RETRY_COUNT_AFTER_ERROR = 3
        private const val MAX_OUTGOING_SAVE_ATTEMPTS = 3
        private const val WIDGET_UPDATE_DEBOUNCE = 300L
        private const val REWIND_INSTEAD_PREVIOUS_MILLIS = 5000L

        private const val FOREGROUND_SERVICE_TIMEOUT = (60 * 1000) * 2L

        private const val LONG_TRACK_THRESHOLD_MS = 10 * 60 * 1000L
        private const val TRACK_POSITION_TICK_MS = 5000L
        private const val NOTIFICATION_PROGRESS_TICK_MS = 1000L
        private const val TRACK_POSITION_PERSIST_INTERVAL_MS = 15000L
        private const val TRACK_START_TOLERANCE_MS = 5000L
        private const val TRACK_END_TOLERANCE_MS = 5000L
        private const val MAX_REMEMBERED_TRACKS = 500
        private const val MIN_AB_REPEAT_RANGE_MS = 500L
        private const val AB_REPEAT_END_TOLERANCE_MS = 100L
        private const val AB_REPEAT_RESCHEDULE_DELAY_MS = 50L
        private const val MIN_AB_REPEAT_SEEK_INTERVAL_MS = 250L
    }
}
