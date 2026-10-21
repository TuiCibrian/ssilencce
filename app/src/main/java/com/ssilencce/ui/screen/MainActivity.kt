package com.ssilencce.ui.screen

import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.ssilencce.R
import com.ssilencce.core.model.CategoryInfo
import com.ssilencce.core.model.MediaEvent
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.data.model.network.NetworkFeature
import com.ssilencce.extensions.currentFragment
import com.ssilencce.extensions.navigation.isValidCategory
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.whichFragment
import com.ssilencce.playback.Playback
import com.ssilencce.playback.library.MediaIDs
import com.ssilencce.ui.IScrollHelper
import com.ssilencce.ui.component.base.AbsSlidingMusicPanelActivity
import com.ssilencce.ui.dialogs.AudioFocusAskDialog
import com.ssilencce.ui.screen.update.UpdateDialog
import com.ssilencce.ui.screen.update.UpdateSearchResult
import com.ssilencce.ui.screen.update.UpdateViewModel
import com.ssilencce.util.Preferences
import com.ssilencce.util.ShortcutEntityType
import com.ssilencce.util.ShortcutHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians M. A. (mardous)
 */
class MainActivity :
    AbsSlidingMusicPanelActivity(),
    MediaController.Listener {
    private val updateViewModel: UpdateViewModel by viewModel()
    private var audioFocusStateRevision = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = null
        supportFragmentManager.setFragmentResultListener(AudioFocusAskDialog.RESULT_KEY, this) { _, result ->
            val requestId = result.getString(AudioFocusAskDialog.ARG_REQUEST_ID) ?: return@setFragmentResultListener
            if (result.getBoolean(AudioFocusAskDialog.ARG_CONFIRM)) {
                playerViewModel.confirmAudioFocus(requestId)
            } else {
                playerViewModel.cancelAudioFocus(requestId)
            }
        }

        updateTabs()
        setupNavigationController()

        val shortcutManager = getSystemService<ShortcutManager>()
        shortcutManager?.removeDynamicShortcuts(OLD_SHORTCUT_IDS)

        prepareUpdateViewModel()
    }

    override fun onConnected(controller: MediaController) {
        super.onConnected(controller)
        refreshAudioFocusAskState(controller)
        intent?.let { handlePlaybackIntent(it, true) }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        mediaControllerOwner.get()?.let(::refreshAudioFocusAskState)
    }

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        controller: MediaController,
        command: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        val sessionResult =
            when (command.customAction) {
                Playback.EVENT_MEDIA_CONTENT_CHANGED -> {
                    playerViewModel.submitEvent(MediaEvent.MediaContentChanged)
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }

                Playback.EVENT_FAVORITE_CONTENT_CHANGED -> {
                    playerViewModel.submitEvent(MediaEvent.FavoriteContentChanged)
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }

                Playback.EVENT_PLAYBACK_STARTED -> {
                    playerViewModel.submitEvent(MediaEvent.PlaybackStarted)
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }

                Playback.EVENT_PLAYBACK_RESTORED -> {
                    playerViewModel.submitEvent(MediaEvent.PlaybackRestored)
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }

                Playback.EVENT_AUDIO_FOCUS_ASK -> {
                    audioFocusStateRevision++
                    reconcileAudioFocusAskDialog(args.getString(Playback.EXTRA_AUDIO_FOCUS_REQUEST_ID))
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }

                else -> {
                    SessionResult(SessionError.ERROR_NOT_SUPPORTED)
                }
            }
        return Futures.immediateFuture(sessionResult)
    }

    fun scanAllPaths() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.scan_media)
            .setMessage(R.string.scan_media_message)
            .setPositiveButton(R.string.scan_media_positive) { _, _ ->
                libraryViewModel.scanAllPaths(this).observe(this) {
                    // TODO show detailed info about scanned songs
                    showToast(R.string.scan_finished)
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupNavigationController() {
        val navController = whichFragment<NavHostFragment>(R.id.fragment_container).navController
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.graph_main)
        if (Preferences.isAuroraAppearance) {
            navGraph.addDestination(
                navController.navigatorProvider.getNavigator(FragmentNavigator::class.java).createDestination().apply {
                    id = R.id.nav_queue
                    setClassName("com.ssilencce.ui.screen.queue.AuroraQueueFragment")
                    label = getString(R.string.playing_queue_label)
                },
            )
        }

        val categoryInfo: CategoryInfo = Preferences.libraryCategories.first { it.visible }
        if (categoryInfo.visible) {
            val lastPage = Preferences.lastPage
            if (!navGraph.isValidCategory(lastPage)) {
                Preferences.lastPage = categoryInfo.category.id
                navGraph.setStartDestination(categoryInfo.category.id)
            } else {
                navGraph.setStartDestination(
                    if (Preferences.isRememberLastPage) {
                        lastPage.let {
                            if (it == 0) {
                                categoryInfo.category.id
                            } else {
                                it
                            }
                        }
                    } else {
                        categoryInfo.category.id
                    },
                )
            }
        }

        navController.graph = navGraph
        if (!Preferences.isAuroraAppearance) navigationView.setupWithNavController(navController)
        // Scroll Fragment to top
        navigationView.setOnItemReselectedListener {
            currentFragment(R.id.fragment_container).apply {
                if (this is IScrollHelper) {
                    scrollToTop()
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == navGraph.startDestinationId) {
                currentFragment(R.id.fragment_container)?.enterTransition = null
            }
            if (destination.navigatorName == "dialog") {
                return@addOnDestinationChangedListener
            }
            if (Preferences.isAuroraAppearance) {
                auroraDestination = destination.id
                if (Preferences.isRememberLastPage) saveTab(destination.id)
                setBottomNavVisibility(
                    visible = true,
                    hideBottomSheet = if (destination.id in listOf(R.id.nav_lyrics_editor, R.id.nav_play_info, R.id.nav_about)) true else null,
                )
                return@addOnDestinationChangedListener
            }
            when (destination.id) {
                R.id.nav_home,
                R.id.nav_songs,
                R.id.nav_albums,
                R.id.nav_artists,
                R.id.nav_folders,
                R.id.nav_playlists,
                R.id.nav_genres,
                R.id.nav_years,
                -> {
                    // Save the last tab
                    if (Preferences.isRememberLastPage) {
                        saveTab(destination.id)
                    }
                    // Show Bottom Navigation Bar
                    setBottomNavVisibility(visible = true, animate = true)
                }

                R.id.nav_queue,
                R.id.nav_lyrics_editor,
                R.id.nav_play_info,
                R.id.nav_about,
                -> {
                    setBottomNavVisibility(visible = false, hideBottomSheet = true)
                }

                else -> {
                    setBottomNavVisibility(visible = false, animate = true)
                } // Hide Bottom Navigation Bar
            }
        }
    }

    private fun saveTab(id: Int) {
        if (Preferences.libraryCategories.firstOrNull { it.category.id == id }?.visible == true) {
            Preferences.lastPage = id
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePlaybackIntent(intent, false)
    }

    private fun handlePlaybackIntent(
        intent: Intent,
        canRestorePlayback: Boolean,
    ) {
        when (intent.action) {
            APP_SHORTCUT_LAST_ADDED -> {
                playerViewModel.playMediaId(MediaIDs.LAST_ADDED)
                setIntent(Intent())
            }

            APP_SHORTCUT_TOP_TRACKS -> {
                playerViewModel.playMediaId(MediaIDs.TOP_TRACKS)
                setIntent(Intent())
            }

            APP_SHORTCUT_SHUFFLE -> {
                playerViewModel.playMediaId(MediaIDs.SONGS, true)
                setIntent(Intent())
            }

            APP_SHORTCUT_FAVORITES -> {
                playerViewModel.playMediaId(MediaIDs.FAVORITES, true)
                setIntent(Intent())
            }

            APP_SHORTCUT_ENTITY -> {
                handleEntityShortcut(intent)
                setIntent(Intent())
            }

            else -> {
                libraryViewModel.handleIntent(intent).observe(this) { result ->
                    if (result.handled) {
                        if (result.songs.isNotEmpty()) {
                            playerViewModel.openQueue(
                                queue = result.songs,
                                position = result.position,
                                shuffleMode = OpenShuffleMode.Off,
                            )
                        }
                        setIntent(Intent())
                    } else if (canRestorePlayback) {
                        playerViewModel.restorePlayback()
                    }
                    if (result.failed) {
                        showToast(R.string.unplayable_file)
                    }
                }
            }
        }
    }

    private fun handleEntityShortcut(intent: Intent) {
        val type = intent.getStringExtra(ShortcutHelper.EXTRA_TYPE) ?: return
        val id = intent.getStringExtra(ShortcutHelper.EXTRA_ID) ?: ""
        val path = intent.getStringExtra(ShortcutHelper.EXTRA_PATH)
        lifecycleScope.launch {
            val songs = libraryViewModel.shortcutSongs(type, id, path)
            if (songs.isEmpty()) {
                val exists = libraryViewModel.shortcutTargetExists(type, id, path)
                if (exists == false) {
                    ShortcutEntityType.entries
                        .firstOrNull { it.name == type }
                        ?.let { ShortcutHelper.remove(this@MainActivity, it, id) }
                }
                showToast(if (exists == true) R.string.shortcut_target_empty else R.string.shortcut_unavailable)
            } else {
                playerViewModel.openQueue(songs, shuffleMode = OpenShuffleMode.Off)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun refreshAudioFocusAskState(controller: MediaController) {
        val revision = audioFocusStateRevision
        lifecycleScope.launch {
            try {
                val result =
                    controller
                        .sendCustomCommand(
                            SessionCommand(Playback.GET_AUDIO_FOCUS_STATE, Bundle.EMPTY),
                            Bundle.EMPTY,
                        ).await()
                if (revision == audioFocusStateRevision && mediaControllerOwner.get() === controller &&
                    result.resultCode == SessionResult.RESULT_SUCCESS
                ) {
                    reconcileAudioFocusAskDialog(result.extras.getString(Playback.EXTRA_AUDIO_FOCUS_REQUEST_ID))
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Reconnection queries authoritative state again; never infer consent from failure.
            }
        }
    }

    private fun reconcileAudioFocusAskDialog(requestId: String?) {
        if (isFinishing || supportFragmentManager.isStateSaved) return
        val existing = supportFragmentManager.findFragmentByTag(AudioFocusAskDialog.TAG) as? AudioFocusAskDialog
        if (existing != null && existing.requestId == requestId) return
        existing?.dismissNow()
        if (requestId != null) {
            AudioFocusAskDialog.newInstance(requestId).showNow(supportFragmentManager, AudioFocusAskDialog.TAG)
        }
    }

    private fun prepareUpdateViewModel() {
        updateViewModel.run {
            updateEventObservable.observe(this@MainActivity) { event ->
                event.getContentIfNotConsumed()?.let { result ->
                    when (result.state) {
                        UpdateSearchResult.State.Completed -> {
                            val release = result.data ?: return@let
                            if (result.wasFromUser || release.isDownloadable(this@MainActivity)) {
                                val existingDialog = supportFragmentManager.findFragmentByTag("UPDATE_FOUND")
                                if (existingDialog == null) {
                                    UpdateDialog().show(supportFragmentManager, "UPDATE_FOUND")
                                }
                            }
                        }

                        UpdateSearchResult.State.Failed -> {
                            if (result.wasFromUser) {
                                showToast(R.string.could_not_check_for_updates)
                            }
                        }

                        else -> {}
                    }
                }
            }
            updateEvent?.peekContent().let { updateState ->
                if (updateState == null || updateState.state == UpdateSearchResult.State.Idle) {
                    if (NetworkFeature.Updater.isAvailable) {
                        searchForUpdate(false)
                    }
                }
            }
        }
    }

    companion object {
        private const val APP_SHORTCUT_LAST_ADDED = "com.ssilencce.shortcut.LAST_ADDED"
        private const val APP_SHORTCUT_TOP_TRACKS = "com.ssilencce.shortcut.TOP_TRACKS"
        private const val APP_SHORTCUT_SHUFFLE = "com.ssilencce.shortcut.SHUFFLE"
        private const val APP_SHORTCUT_FAVORITES = "com.ssilencce.shortcut.FAVORITES"
        private const val APP_SHORTCUT_ENTITY = ShortcutHelper.ACTION_ENTITY

        private val OLD_SHORTCUT_IDS =
            listOf(
                "com.ssilencce.appshortcuts.id.last_added",
                "com.ssilencce.appshortcuts.id.top_tracks",
                "com.ssilencce.appshortcuts.id.shuffle_all",
            )
    }
}
