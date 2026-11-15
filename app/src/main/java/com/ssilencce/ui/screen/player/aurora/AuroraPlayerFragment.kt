package com.ssilencce.ui.screen.player.aurora

import android.animation.AnimatorSet
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import com.ssilencce.R
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.core.model.player.PlayerColorSchemeMode
import com.ssilencce.core.model.player.PlayerTintTarget
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.databinding.FragmentRibbonPlayerBinding
import com.ssilencce.extensions.media.albumArtistName
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.navigation.findActivityNavController
import com.ssilencce.extensions.navigation.songDetailArgs
import com.ssilencce.playback.cast.CastPlaybackProvider
import com.ssilencce.ui.component.base.AbsPlayerFragment
import com.ssilencce.ui.component.preferences.dialog.ExtraInfoPreferenceDialog
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.screen.other.ShuffleModeFragment
import com.ssilencce.ui.screen.player.SeekToDialogFragment
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences

class AuroraPlayerFragment : AbsPlayerFragment(R.layout.fragment_ribbon_player) {
    private var favorite by mutableStateOf(false)
    private var shown by mutableStateOf(false)
    private var lyricsVisible by mutableStateOf(false)

    override val colorSchemeMode: PlayerColorSchemeMode = PlayerColorSchemeMode.AppTheme

    override fun getTintTargets(scheme: PlayerColorScheme): List<PlayerTintTarget> = emptyList()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentRibbonPlayerBinding.bind(view)
        val height = if (Preferences.nowPlayingScreen == NowPlayingScreen.Peek) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        binding.playerChrome.updateLayoutParams<ViewGroup.LayoutParams> { this.height = height }
        view.doOnAttach { it.updateLayoutParams<ViewGroup.LayoutParams> { this.height = height } }
        val cover = binding.auroraCoverHost
        (cover.parent as ViewGroup).removeView(cover)
        binding.playerChrome.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.playerChrome.setContent {
            SsilencceTheme {
                AuroraPlayerScreen(
                    playerViewModel = playerViewModel,
                    coverView = cover,
                    favorite = favorite,
                    lyricsVisible = lyricsVisible,
                    shown = shown && !(activity as MainActivity).isAuroraOverlayVisible,
                    navigationHeight = with(LocalDensity.current) { (activity as MainActivity).navigationHeight.toDp() },
                    artistName = { song -> if (Preferences.preferAlbumArtistName) song.albumArtistName().displayArtistName() else song.displayArtistName() },
                    nextSongInfo = ::getNextSongInfo,
                    onAction = { onQuickActionEvent(it) },
                    onQuickAccess = { onQuickAccessAction(it) },
                    onSeekTo = { SeekToDialogFragment.show(childFragmentManager) },
                    onCollapse = { (activity as? MainActivity)?.collapsePanel() },
                    onEqualizer = { onMenuItemClick(PopupMenu(requireContext(), binding.playerChrome).menu.apply { add(0, R.id.action_equalizer, 0, R.string.equalizer_label) }.findItem(R.id.action_equalizer)) },
                    onDetails = { findActivityNavController(R.id.fragment_container).navigate(R.id.nav_song_details, songDetailArgs(playerViewModel.currentSong)) },
                    castAvailable = CastPlaybackProvider.get(requireContext()).isAvailable,
                    onCast = { CastPlaybackProvider.get(requireContext()).showDeviceChooser(requireActivity()) },
                    onOverflowItem = { id ->
                        val popupMenu =
                            PopupMenu(requireContext(), binding.playerChrome).apply {
                                menuInflater.inflate(R.menu.menu_now_playing, menu)
                                onMenuInflated(menu)
                            }
                        popupMenu.menu.findItem(id)?.let { onMenuItemClick(it) }
                    },
                    onShuffleOptions = { ShuffleModeFragment().show(childFragmentManager, "SHUFFLE_MODE") },
                    onExtraInfoOptions = { ExtraInfoPreferenceDialog.nowPlaying(requireContext()).show(childFragmentManager, "NOW_PLAYING_EXTRA_INFO") },
                    onGesture = { gestureDetected(it) },
                )
            }
        }
    }

    override fun onPrepareViewGestures(view: View) {}

    override fun onMenuInflated(menu: Menu) {
        super.onMenuInflated(menu)
        menu.setIsFavorite(favorite, false)
        menu.onLyricsVisibilityChang(lyricsVisible)
    }

    override fun onLyricsVisibilityChange(
        animatorSet: AnimatorSet,
        lyricsVisible: Boolean,
    ) {
        super.onLyricsVisibilityChange(animatorSet, lyricsVisible)
        this.lyricsVisible = lyricsVisible
    }

    override fun onResume() {
        super.onResume()
        shown = (activity as? MainActivity)?.panelState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onPause() {
        shown = false
        super.onPause()
    }

    override fun onIsFavoriteChanged(
        isFavorite: Boolean,
        withAnimation: Boolean,
    ) {
        favorite = isFavorite
    }

    override fun onShow() {
        super.onShow()
        shown = true
    }

    override fun onHide() {
        super.onHide()
        shown = false
    }
}
