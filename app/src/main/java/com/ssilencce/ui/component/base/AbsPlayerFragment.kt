/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.ssilencce.ui.component.base

import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.core.view.drawToBitmap
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.preference.PreferenceManager
import coil3.load
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import com.commit451.coiltransformations.BlurTransformation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.core.model.MediaEvent
import com.ssilencce.core.model.PaletteColor
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.action.QuickAccessAction
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.core.model.player.PlayerColorSchemeMode
import com.ssilencce.core.model.player.PlayerTintTarget
import com.ssilencce.data.local.EditTarget
import com.ssilencce.data.local.notes.SongNotesRepository
import com.ssilencce.data.model.Genre
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.currentFragment
import com.ssilencce.extensions.keepScreenOn
import com.ssilencce.extensions.launchAndRepeatWithViewLifecycle
import com.ssilencce.extensions.media.albumArtistName
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.isArtistNameUnknown
import com.ssilencce.extensions.navigation.albumDetailArgs
import com.ssilencce.extensions.navigation.artistDetailArgs
import com.ssilencce.extensions.navigation.findActivityNavController
import com.ssilencce.extensions.navigation.genreDetailArgs
import com.ssilencce.extensions.observeKeyAsState
import com.ssilencce.extensions.resources.animateBackgroundColor
import com.ssilencce.extensions.resources.animateTintColor
import com.ssilencce.extensions.resources.inflateMenu
import com.ssilencce.extensions.resources.setMarquee
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.extensions.whichFragment
import com.ssilencce.playback.cast.CastPlaybackProvider
import com.ssilencce.ui.component.compose.QuickAccessRow
import com.ssilencce.ui.component.menu.newPopupMenu
import com.ssilencce.ui.component.menu.onSongMenu
import com.ssilencce.ui.dialogs.WebSearchDialog
import com.ssilencce.ui.dialogs.playlists.AddToPlaylistDialog
import com.ssilencce.ui.dialogs.songs.DeleteSongsDialog
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.screen.equalizer.EqualizerFragment
import com.ssilencce.ui.screen.equalizer.EqualizerFragmentArgs
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.lyrics.LyricsEditorFragmentArgs
import com.ssilencce.ui.screen.lyrics.LyricsFragment
import com.ssilencce.ui.screen.notes.SongNotesFragmentArgs
import com.ssilencce.ui.screen.player.AbRepeatToggleResult
import com.ssilencce.ui.screen.player.PlayerGesturesController
import com.ssilencce.ui.screen.player.PlayerGesturesController.GestureType
import com.ssilencce.ui.screen.player.PlayerScreenshot
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.screen.player.SeekToDialogFragment
import com.ssilencce.ui.screen.player.art.FullScreenArtDialogFragment
import com.ssilencce.ui.screen.player.cover.CoverPagerFragment
import com.ssilencce.ui.screen.player.screencast.ScreencastManager
import com.ssilencce.ui.screen.player.screencast.ScreencastPickerDialogFragment
import com.ssilencce.ui.screen.tageditor.SongTagEditorActivity
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.KEEP_SCREEN_ON_NOW_PLAYING
import com.ssilencce.util.NOW_PLAYING_EXTRA_INFO
import com.ssilencce.util.NOW_PLAYING_QUICK_ACCESS
import com.ssilencce.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsPlayerFragment(
    @LayoutRes layoutRes: Int,
) : Fragment(layoutRes),
    Toolbar.OnMenuItemClickListener,
    PlayerGesturesController.Listener,
    CoverPagerFragment.Callbacks {
    val playerViewModel: PlayerViewModel by activityViewModel()
    val libraryViewModel: LibraryViewModel by activityViewModel()
    private val songNotesRepository: SongNotesRepository by inject()
    private val screencastManager: ScreencastManager by inject()

    private var gesturesController: PlayerGesturesController? = null
    private var coverFragment: CoverPagerFragment? = null

    protected abstract val colorSchemeMode: PlayerColorSchemeMode
    protected open val playerControlsFragment: AbsPlayerControlsFragment? = null

    protected open val playerToolbar: Toolbar?
        get() = null

    protected open val blurView: ImageView?
        get() = null

    private var colorAnimatorSet: AnimatorSet? = null
    private var playerShown = false
    private var lyricsVisible = false
    private var quickAccessOverlay: ComposeView? = null

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEEP_SCREEN_ON_NOW_PLAYING) applyKeepScreenOn()
        }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onCreateChildFragments()
        onPrepareViewGestures(view)
        setupQuickAccessOverlay(view)
        Preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        playerShown = (activity as? MainActivity)?.panelState == BottomSheetBehavior.STATE_EXPANDED
        applyKeepScreenOn()
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.mediaEvent
                .filter { it == MediaEvent.FavoriteContentChanged }
                .collect {
                    updateIsFavorite(withAnim = true)
                }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.currentSongFlow.collect {
                updateIsFavorite(withAnim = false)
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            playerViewModel.colorSchemeFlow.collect { scheme ->
                applyColorScheme(scheme)?.start()
            }
        }
        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            combine(
                playerViewModel.currentSongFlow,
                playerViewModel.colorSchemeFlow,
            ) { song, scheme -> Pair(song, scheme) }
                .filter { (song, scheme) -> song.id != -1L && scheme != PlayerColorScheme.Unspecified }
                .collect { (song, scheme) ->
                    applyBlur(song, scheme)
                }
        }
    }

    @CallSuper
    protected open fun onCreateChildFragments() {
        coverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        coverFragment?.setCallbacks(this)
    }

    protected open fun onPrepareViewGestures(view: View) {
        gesturesController =
            PlayerGesturesController(
                context = view.context,
                acceptedGestures =
                    setOf(
                        GestureType.Fling(GestureType.Fling.DIRECTION_UP),
                        GestureType.Fling(GestureType.Fling.DIRECTION_LEFT),
                        GestureType.Fling(GestureType.Fling.DIRECTION_RIGHT),
                    ),
                listener = this,
            )
        view.setOnTouchListener(gesturesController)
    }

    internal fun inflateMenuInView(view: View?): PopupMenu? {
        if (view != null) {
            if (view is Toolbar) {
                view.inflateMenu(R.menu.menu_now_playing, this) {
                    onMenuInflated(it)
                }
            } else {
                val popupMenu =
                    newPopupMenu(view, R.menu.menu_now_playing) {
                        onMenuInflated(it)
                    }.also { popupMenu ->
                        popupMenu.setOnMenuItemClickListener { onMenuItemClick(it) }
                    }
                view.setOnClickListener {
                    popupMenu.show()
                }
                return popupMenu
            }
        }
        return null
    }

    @CallSuper
    protected open fun onMenuInflated(menu: Menu) {
        CastPlaybackProvider.get(requireContext()).setupMenu(menu, R.id.action_cast)
        menu.findItem(R.id.action_screencast)?.apply {
            isCheckable = true
            isChecked = screencastManager.isActive
        }
    }

    protected fun Menu.setShowAsAction(
        itemId: Int,
        mode: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM,
    ) {
        findItem(itemId)?.setShowAsAction(mode)
    }

    override fun onMenuItemClick(menuItem: MenuItem): Boolean {
        val currentSong = playerViewModel.currentSong
        return when (menuItem.itemId) {
            R.id.action_playing_queue -> {
                onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
                true
            }

            R.id.action_favorite -> {
                onQuickActionEvent(NowPlayingAction.ToggleFavoriteState)
                true
            }

            R.id.action_cast -> {
                CastPlaybackProvider.get(requireContext()).showDeviceChooser(requireActivity())
                true
            }

            R.id.action_screencast -> {
                if (!childFragmentManager.isStateSaved) {
                    ScreencastPickerDialogFragment
                        .newInstance()
                        .show(childFragmentManager, ScreencastPickerDialogFragment.TAG)
                }
                true
            }

            R.id.action_show_lyrics -> {
                onQuickActionEvent(NowPlayingAction.Lyrics)
                true
            }

            R.id.action_sound_settings -> {
                onQuickActionEvent(NowPlayingAction.SoundSettings)
                true
            }

            R.id.action_sleep_timer -> {
                onQuickActionEvent(NowPlayingAction.SleepTimer)
                true
            }

            R.id.action_pause_after_this_song -> {
                playerViewModel.pauseAfterSong(currentSong)
                true
            }

            R.id.action_load_next_and_pause -> {
                playerViewModel.loadNextSongAndPause()
                true
            }

            R.id.action_ab_repeat -> {
                when (val result = playerViewModel.toggleAbRepeatPoint()) {
                    is AbRepeatToggleResult.PointASet -> {
                        showToast(
                            getString(
                                R.string.ab_repeat_point_a_set,
                                result.positionMs.asReadableDuration(),
                            ),
                        )
                    }

                    is AbRepeatToggleResult.RangeSet -> {
                        showToast(
                            getString(
                                R.string.ab_repeat_range_set,
                                result.startMs.asReadableDuration(),
                                result.endMs.asReadableDuration(),
                            ),
                        )
                    }

                    AbRepeatToggleResult.Cleared -> {
                        showToast(R.string.ab_repeat_cleared)
                    }

                    AbRepeatToggleResult.TooClose -> {
                        showToast(R.string.ab_repeat_too_close)
                    }

                    AbRepeatToggleResult.Unavailable -> {
                        Unit
                    }
                }
                true
            }

            R.id.action_tag_editor -> {
                onQuickActionEvent(NowPlayingAction.TagEditor)
                true
            }

            R.id.action_web_search -> {
                onQuickActionEvent(NowPlayingAction.WebSearch)
                true
            }

            R.id.action_go_to_album -> {
                onQuickActionEvent(NowPlayingAction.OpenAlbum)
                true
            }

            R.id.action_go_to_artist -> {
                onQuickActionEvent(NowPlayingAction.OpenArtist)
                true
            }

            R.id.action_go_to_genre -> {
                libraryViewModel.genreBySong(currentSong).observe(viewLifecycleOwner) { genre ->
                    goToGenre(requireActivity(), genre)
                }
                true
            }

            R.id.action_equalizer -> {
                openEqualizer()
                true
            }

            R.id.action_fullscreen_art -> {
                showFullScreenArt()
                true
            }

            R.id.action_seek_to -> {
                SeekToDialogFragment.show(childFragmentManager)
                true
            }

            R.id.action_add_bookmark -> {
                onQuickActionEvent(NowPlayingAction.AddBookmark)
                true
            }

            R.id.action_song_notes -> {
                onQuickActionEvent(NowPlayingAction.SongNotes)
                true
            }

            R.id.action_share_now_playing -> {
                showShareNowPlayingDialog()
                true
            }

            else -> {
                currentSong.onSongMenu(this, menuItem)
            }
        }
    }

    override fun gestureDetected(gestureType: GestureType): Boolean =
        when (gestureType) {
            is GestureType.Tap -> {
                onQuickActionEvent(Preferences.coverSingleTapAction)
            }

            is GestureType.LongPress -> {
                onQuickActionEvent(Preferences.coverLongPressAction)
            }

            is GestureType.DoubleTap -> {
                when (gestureType.type) {
                    GestureType.DoubleTap.TYPE_LEFT_EDGE -> {
                        val action =
                            Preferences.coverLeftDoubleTapAction
                                .takeIf { it != NowPlayingAction.Nothing }
                                ?: Preferences.coverDoubleTapAction

                        onQuickActionEvent(action)
                    }

                    GestureType.DoubleTap.TYPE_RIGHT_EDGE -> {
                        val action =
                            Preferences.coverRightDoubleTapAction
                                .takeIf { it != NowPlayingAction.Nothing }
                                ?: Preferences.coverDoubleTapAction

                        onQuickActionEvent(action)
                    }

                    GestureType.DoubleTap.TYPE_CENTER -> {
                        onQuickActionEvent(Preferences.coverDoubleTapAction)
                    }

                    else -> {
                        false
                    }
                }
            }

            is GestureType.Fling -> {
                when (gestureType.direction) {
                    GestureType.Fling.DIRECTION_LEFT -> {
                        if (Preferences.isSwipeAnywhere) {
                            playerViewModel.seekToNext()
                            true
                        } else {
                            false
                        }
                    }

                    GestureType.Fling.DIRECTION_RIGHT -> {
                        if (Preferences.isSwipeAnywhere) {
                            playerViewModel.seekToPrevious()
                            true
                        } else {
                            false
                        }
                    }

                    GestureType.Fling.DIRECTION_UP -> {
                        if (Preferences.isSwipeUpQueue) {
                            onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
                            true
                        } else {
                            false
                        }
                    }

                    else -> {
                        false
                    }
                }
            }
        }

    override fun onColorChanged(color: PaletteColor) {
        playerViewModel.generatePlayerScheme(requireContext(), colorSchemeMode, color)
    }

    protected abstract fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget>

    private fun applyColorScheme(scheme: PlayerColorScheme): AnimatorSet? {
        cancelColorAnimator()
        colorAnimatorSet =
            AnimatorSet()
                .setDuration(scheme.mode.preferredAnimDuration)
                .apply {
                    val tintTargets =
                        getTintTargets(scheme).mapTo(mutableListOf()) {
                            if (it.isSurface) {
                                it.target.animateBackgroundColor(it.newColor)
                            } else {
                                it.target.animateTintColor(
                                    fromColor = it.oldColor,
                                    toColor = it.newColor,
                                    isForeground = it.isForeground,
                                    isIconButton = it.isIcon,
                                )
                            }
                        }
                    blurView?.let {
                        if (scheme.blurToken.isBlur) {
                            tintTargets.add(
                                it.animateTintColor(
                                    fromColor =
                                        it.foregroundTintList?.defaultColor
                                            ?: Color.TRANSPARENT,
                                    toColor = scheme.surfaceColor,
                                    isForeground = true,
                                ),
                            )
                        }
                    }
                    playTogether(tintTargets)
                }
        return colorAnimatorSet
    }

    private fun cancelColorAnimator() {
        colorAnimatorSet?.cancel()
        colorAnimatorSet = null
    }

    private fun applyBlur(
        song: Song,
        scheme: PlayerColorScheme,
    ) {
        blurView?.let {
            if (scheme.blurToken.isBlur) {
                it.isVisible = true
                it.load(song) {
                    size(256)
                    precision(Precision.INEXACT)
                    scale(Scale.FILL)
                    memoryCacheKey("nowplaying:song:${song.id}")
                    crossfade(1000)
                    transformations(
                        BlurTransformation(
                            context = it.context,
                            radius = scheme.blurToken.blurRadius.coerceIn(0f, 25f),
                        ),
                    )
                }
            } else {
                it.isGone = true
                it.setImageDrawable(null)
            }
        }
    }

    protected fun Menu.onLyricsVisibilityChang(lyricsVisible: Boolean) {
        val lyricsItem = findItem(R.id.action_show_lyrics)
        if (lyricsItem != null) {
            if (lyricsVisible) {
                lyricsItem
                    .setIcon(getTintedDrawable(R.drawable.ic_lyrics_24dp))
                    .setTitle(R.string.action_hide_lyrics)
            } else {
                lyricsItem
                    .setIcon(getTintedDrawable(R.drawable.ic_lyrics_outline_24dp))
                    .setTitle(R.string.action_show_lyrics)
            }
        }
    }

    override fun onLyricsVisibilityChange(
        animatorSet: AnimatorSet,
        lyricsVisible: Boolean,
    ) {
        this.lyricsVisible = lyricsVisible
        applyKeepScreenOn()
        playerToolbar?.menu?.onLyricsVisibilityChang(lyricsVisible)
    }

    override fun onDestroyView() {
        view?.setOnTouchListener(null)
        gesturesController?.release()
        gesturesController = null
        cancelColorAnimator()
        coverFragment = null
        Preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        (quickAccessOverlay?.parent as? ViewGroup)?.removeView(quickAccessOverlay)
        quickAccessOverlay = null
        activity?.keepScreenOn(false)
        super.onDestroyView()
    }

    internal fun onQuickActionEvent(action: NowPlayingAction): Boolean {
        val currentSong = playerViewModel.currentSong
        return when (action) {
            NowPlayingAction.OpenAlbum -> {
                goToAlbum(requireActivity(), currentSong)
                true
            }

            NowPlayingAction.OpenArtist -> {
                goToArtist(requireActivity(), currentSong)
                true
            }

            NowPlayingAction.OpenPlayQueue -> {
                if (Preferences.isAuroraAppearance) {
                    (activity as? MainActivity)?.navigateAuroraDestination(R.id.nav_queue)
                } else {
                    findNavController().navigate(R.id.nav_queue)
                }
                true
            }

            NowPlayingAction.TogglePlayState -> {
                playerViewModel.togglePlayPause()
                true
            }

            NowPlayingAction.WebSearch -> {
                WebSearchDialog.create(currentSong).show(childFragmentManager, "WEB_SEARCH_DIALOG")
                true
            }

            NowPlayingAction.DeleteFromDevice -> {
                DeleteSongsDialog.create(currentSong).show(childFragmentManager, "DELETE_SONGS")
                true
            }

            NowPlayingAction.Lyrics -> {
                if (coverFragment?.isAllowedToLoadLyrics == true) {
                    coverFragment?.toggleLyrics()
                } else {
                    if (currentFragment(R.id.fragment_container) is LyricsFragment) {
                        (activity as? MainActivity)?.collapsePanel()
                    } else {
                        goToDestination(requireActivity(), R.id.nav_lyrics)
                    }
                }
                true
            }

            NowPlayingAction.LyricsEditor -> {
                goToDestination(
                    requireActivity(),
                    R.id.nav_lyrics_editor,
                    LyricsEditorFragmentArgs
                        .Builder(currentSong)
                        .build()
                        .toBundle(),
                )
                true
            }

            NowPlayingAction.AddBookmark -> {
                addQuickBookmark()
                true
            }

            NowPlayingAction.SongNotes -> {
                openSongNotes()
                true
            }

            NowPlayingAction.AddToPlaylist -> {
                AddToPlaylistDialog
                    .create(currentSong)
                    .show(childFragmentManager, "ADD_TO_PLAYLIST")
                true
            }

            NowPlayingAction.ToggleFavoriteState -> {
                toggleFavorite()
                true
            }

            NowPlayingAction.TagEditor -> {
                val tagEditorIntent = Intent(requireContext(), SongTagEditorActivity::class.java)
                tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_TARGET, EditTarget.song(currentSong))
                startActivity(tagEditorIntent)
                true
            }

            NowPlayingAction.SleepTimer -> {
                findActivityNavController(R.id.fragment_container)
                    .navigate(R.id.nav_sleep_timer)
                true
            }

            NowPlayingAction.SoundSettings -> {
                findNavController().navigate(R.id.nav_sound_settings)
                true
            }

            NowPlayingAction.SeekBackward -> {
                playerViewModel.seekBack()
                true
            }

            NowPlayingAction.SeekForward -> {
                playerViewModel.seekForward()
                true
            }

            NowPlayingAction.ShowFullScreenArt -> {
                showFullScreenArt()
                true
            }

            NowPlayingAction.Nothing -> {
                false
            }
        }
    }

    internal fun onQuickAccessAction(action: QuickAccessAction): Boolean =
        when (action) {
            QuickAccessAction.Lyrics -> {
                onQuickActionEvent(NowPlayingAction.Lyrics)
            }

            QuickAccessAction.Equalizer -> {
                openEqualizer()
                true
            }

            QuickAccessAction.SleepTimer -> {
                onQuickActionEvent(NowPlayingAction.SleepTimer)
            }

            QuickAccessAction.Tempo -> {
                findNavController().navigate(R.id.nav_sound_settings)
                true
            }

            QuickAccessAction.PlayQueue -> {
                onQuickActionEvent(NowPlayingAction.OpenPlayQueue)
            }

            QuickAccessAction.TagEditor -> {
                onQuickActionEvent(NowPlayingAction.TagEditor)
            }

            QuickAccessAction.AddToPlaylist -> {
                onQuickActionEvent(NowPlayingAction.AddToPlaylist)
            }

            QuickAccessAction.Bookmark -> {
                onQuickActionEvent(NowPlayingAction.AddBookmark)
            }

            QuickAccessAction.SongNotes -> {
                onQuickActionEvent(NowPlayingAction.SongNotes)
            }
        }

    private fun openEqualizer() {
        if (currentFragment(R.id.fragment_container) is EqualizerFragment) {
            (activity as? MainActivity)?.collapsePanel()
        } else {
            goToDestination(
                activity = requireActivity(),
                destinationId = R.id.nav_equalizer,
                args =
                    EqualizerFragmentArgs
                        .Builder()
                        .setFromPlayer(true)
                        .build()
                        .toBundle(),
            )
        }
    }

    private fun addQuickBookmark() {
        val song = playerViewModel.currentSong
        if (song.id == -1L) return
        val position = playerViewModel.progress.coerceAtLeast(0L)
        viewLifecycleOwner.lifecycleScope.launch {
            val added = songNotesRepository.addBookmark(song, position, "")
            if (added) {
                showToast(getString(R.string.song_notes_bookmark_added, position.asReadableDuration()))
            } else {
                showToast(R.string.song_notes_bookmark_add_failed)
            }
        }
    }

    private fun openSongNotes() {
        val song = playerViewModel.currentSong
        if (song.id == -1L) return
        goToDestination(
            activity = requireActivity(),
            destinationId = R.id.nav_song_notes,
            args =
                SongNotesFragmentArgs
                    .Builder(song)
                    .setFromPlayer(true)
                    .build()
                    .toBundle(),
        )
    }

    private fun showFullScreenArt() {
        if (childFragmentManager.isStateSaved) return
        val song = playerViewModel.currentSong
        if (song.id == -1L) return
        FullScreenArtDialogFragment
            .newInstance(song)
            .show(childFragmentManager, FullScreenArtDialogFragment.TAG)
    }

    private fun showShareNowPlayingDialog() {
        val options =
            arrayOf(
                getString(R.string.share_now_playing_original),
                getString(R.string.share_now_playing_square),
            )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.share_now_playing)
            .setItems(options) { _, which ->
                shareNowPlaying(square = which == 1)
            }.show()
    }

    private fun shareNowPlaying(square: Boolean) {
        val root = view ?: return
        val context = requireContext()
        val screenshot =
            runCatching { root.drawToBitmap() }
                .onFailure { error ->
                    android.util.Log.e("AbsPlayerFragment", "Could not render the player view", error)
                }.getOrNull()
        if (screenshot == null) {
            showToast(R.string.share_now_playing_failed)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val uri =
                withContext(Dispatchers.IO) {
                    PlayerScreenshot.createShareUri(context, screenshot, square)
                }
            screenshot.recycle()
            if (uri == null) {
                showToast(R.string.share_now_playing_failed)
                return@launch
            }
            val title = getString(R.string.share_now_playing)
            runCatching {
                startActivity(Intent.createChooser(PlayerScreenshot.shareIntent(context, uri, title), title))
            }
        }
    }

    private fun setupQuickAccessOverlay(root: View) {
        if (Preferences.isAuroraAppearance || root !is ViewGroup) return
        val context = requireContext()
        val margin = (12 * resources.displayMetrics.density).toInt()
        val composeView =
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SsilencceTheme {
                        val preferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
                        val raw by preferences.observeKeyAsState(NOW_PLAYING_QUICK_ACCESS, Preferences.quickAccessActionsRaw)
                        val actions = remember(raw) { QuickAccessAction.fromPreference(raw) }
                        QuickAccessRow(
                            actions = actions,
                            onAction = { onQuickAccessAction(it) },
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        val params: ViewGroup.LayoutParams =
            when (root) {
                is ConstraintLayout -> {
                    ConstraintLayout
                        .LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            topMargin = margin
                        }
                }

                is FrameLayout -> {
                    FrameLayout
                        .LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                        ).apply {
                            topMargin = margin
                        }
                }

                is LinearLayout -> {
                    LinearLayout
                        .LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                            topMargin = margin
                        }
                }

                else -> {
                    return
                }
            }
        if (root is LinearLayout) {
            root.addView(composeView, 0, params)
        } else {
            root.addView(composeView, params)
        }
        quickAccessOverlay = composeView
    }

    private fun applyKeepScreenOn() {
        activity?.keepScreenOn(
            playerShown && (Preferences.keepScreenOnForNowPlaying || lyricsVisible),
        )
    }

    @CallSuper
    open fun onShow() {
        playerShown = true
        applyKeepScreenOn()
        coverFragment?.showLyrics()
        playerControlsFragment?.onShow()
    }

    @CallSuper
    open fun onHide() {
        playerShown = false
        applyKeepScreenOn()
        coverFragment?.hideLyrics()
        playerControlsFragment?.onHide()
    }

    protected fun getTintedDrawable(
        drawableRes: Int,
        color: Int = playerViewModel.colorScheme.onSurfaceColor,
    ) = AppCompatResources.getDrawable(requireContext(), drawableRes).also {
        it?.setTint(color)
    }

    protected open fun onIsFavoriteChanged(
        isFavorite: Boolean,
        withAnimation: Boolean,
    ) {
        playerToolbar?.menu?.setIsFavorite(isFavorite, withAnimation)
    }

    private fun updateIsFavorite(
        song: Song = playerViewModel.currentSong,
        withAnim: Boolean = false,
    ) {
        libraryViewModel.isSongFavorite(song).observe(viewLifecycleOwner) { isFavorite ->
            onIsFavoriteChanged(isFavorite, withAnim)
        }
    }

    private fun toggleFavorite() {
        playerViewModel.toggleFavorite()
    }

    fun setMarquee(
        vararg textView: TextView?,
        marquee: Boolean,
    ) {
        val scrollingTextEnabled = Preferences.enableScrollingText
        textView.forEach { it?.setMarquee(marquee && scrollingTextEnabled) }
    }

    fun MaterialButton.setIsFavorite(
        isFavorite: Boolean,
        withAnimation: Boolean,
    ) {
        /*
        val iconRes = if (withAnimation) {
            if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
        } else {
            if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        }
         */

        // There's a bug in the Material Components library that affects the
        // icon animation on a MaterialButton, so for now, we'll change the
        // icon in a simple way.
        val iconRes = if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
        icon =
            ContextCompat.getDrawable(context, iconRes).also { drawable ->
                if (drawable is AnimatedVectorDrawable) {
                    drawable.start()
                }
            }
    }

    protected fun Menu.setIsFavorite(
        isFavorite: Boolean,
        withAnimation: Boolean,
    ) {
        val iconRes =
            if (withAnimation) {
                if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
            } else {
                if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp
            }
        val titleRes = if (isFavorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites

        findItem(R.id.action_favorite)?.apply {
            setTitle(titleRes)
            icon =
                getTintedDrawable(iconRes).also {
                    if (it is AnimatedVectorDrawable) {
                        it.start()
                    }
                }
        }
    }

    fun setViewAction(
        view: View,
        action: NowPlayingAction,
    ) {
        view.setOnClickListener { onQuickActionEvent(action) }
    }

    fun getSongArtist(song: Song): CharSequence {
        val artistName =
            if (Preferences.preferAlbumArtistName) {
                song.albumArtistName().displayArtistName()
            } else {
                song.displayArtistName()
            }
        if (Preferences.displayAlbumTitle) {
            return buildInfoString(artistName, song.albumName)
        }
        return artistName
    }

    fun isExtraInfoEnabled(): Boolean =
        Preferences.displayExtraInfo &&
            Preferences
                .getExtraInfoContent(
                    NOW_PLAYING_EXTRA_INFO,
                    Preferences.getDefaultNowPlayingInfo(),
                ).any { it.isEnabled }

    fun getNextSongInfo(nextSong: Song): String =
        if (nextSong != Song.emptySong) {
            if (!nextSong.isArtistNameUnknown()) {
                getString(R.string.next_song_x_by_artist_x, nextSong.title, nextSong.displayArtistName())
            } else {
                getString(R.string.next_song_x, nextSong.title)
            }
        } else {
            getString(R.string.list_end)
        }
}

fun goToArtist(
    activity: Activity,
    song: Song,
) {
    goToDestination(
        activity,
        R.id.nav_artist_detail,
        artistDetailArgs(song),
        removeTransition = true,
        singleTop = false,
    )
}

fun goToAlbum(
    activity: Activity,
    song: Song,
) {
    goToDestination(
        activity,
        R.id.nav_album_detail,
        albumDetailArgs(song.albumId),
        removeTransition = true,
        singleTop = false,
    )
}

fun goToGenre(
    activity: Activity,
    genre: Genre,
) {
    goToDestination(
        activity,
        R.id.nav_genre_detail,
        genreDetailArgs(genre),
        singleTop = false,
    )
}

fun goToDestination(
    activity: Activity,
    destinationId: Int,
    args: Bundle? = null,
    removeTransition: Boolean = false,
    singleTop: Boolean = true,
) {
    if (activity !is MainActivity) return
    activity.apply {
        if (removeTransition) {
            // Remove exit transition of current fragment, so
            // it doesn't exit with a weird transition
            currentFragment(R.id.fragment_container)?.exitTransition = null
        }

        // Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        Handler(Looper.getMainLooper()).postDelayed(250) {
            val navOptions =
                when {
                    singleTop -> navOptions { launchSingleTop = true }
                    else -> null
                }
            findNavController(R.id.fragment_container)
                .navigate(destinationId, args, navOptions)
        }
    }
}
