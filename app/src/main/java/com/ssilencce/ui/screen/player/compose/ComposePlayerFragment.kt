package com.ssilencce.ui.screen.player.compose

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.data.local.EditTarget
import com.ssilencce.extensions.currentFragment
import com.ssilencce.extensions.navigation.findActivityNavController
import com.ssilencce.ui.component.base.AbsTagEditorActivity
import com.ssilencce.ui.component.base.goToAlbum
import com.ssilencce.ui.component.base.goToArtist
import com.ssilencce.ui.component.base.goToDestination
import com.ssilencce.ui.dialogs.WebSearchDialog
import com.ssilencce.ui.dialogs.playlists.AddToPlaylistDialog
import com.ssilencce.ui.dialogs.songs.DeleteSongsDialog
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.lyrics.LyricsFragment
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.lyrics.LyricsEditorFragmentArgs
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.screen.tageditor.SongTagEditorActivity
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ComposePlayerFragment : Fragment() {
    private val playerViewModel: PlayerViewModel by activityViewModel()
    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val lyricsViewModel: LyricsViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            setContent {
                SsilencceTheme {
                    val currentSong by
                        playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
                    val isFavorite by
                        libraryViewModel
                            .isSongFavorite(currentSong)
                            .observeAsState(initial = false)
                    when (Preferences.nowPlayingScreen) {
                        NowPlayingScreen.M3 -> M3NowPlayingScreen(
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            onAction = { performNowPlayingAction(it) },
                            isFavorite = isFavorite,
                        )
                        NowPlayingScreen.Plain -> PlainNowPlayingScreen(
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            onAction = { performNowPlayingAction(it) },
                            isFavorite = isFavorite,
                        )
                        NowPlayingScreen.Gradient -> GradientNowPlayingScreen(
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            onAction = { performNowPlayingAction(it) },
                            isFavorite = isFavorite,
                        )
                        NowPlayingScreen.FullCover -> FullCoverNowPlayingScreen(
                            playerViewModel = playerViewModel,
                            onAction = { performNowPlayingAction(it) },
                            isFavorite = isFavorite,
                            onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        )
                        NowPlayingScreen.Expressive -> ExpressiveNowPlayingScreen(
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            onAction = { performNowPlayingAction(it) },
                            isFavorite = isFavorite,
                            onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        )
                        NowPlayingScreen.Peek -> PeekNowPlayingScreen(
                            playerViewModel = playerViewModel,
                            onAction = { performNowPlayingAction(it) },
                            onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        )
                        else -> DefaultNowPlayingScreen(
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            onAction = { performNowPlayingAction(it) },
                            isFavorite = isFavorite,
                        )
                    }
                }
            }
        }

    private fun performNowPlayingAction(action: NowPlayingAction): Boolean {
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
                goToDestination(requireActivity(), R.id.nav_queue)
                true
            }

            NowPlayingAction.TogglePlayState -> {
                playerViewModel.togglePlayPause()
                true
            }

            NowPlayingAction.WebSearch -> {
                WebSearchDialog
                    .create(currentSong)
                    .show(childFragmentManager, "WEB_SEARCH_DIALOG")
                true
            }

            NowPlayingAction.DeleteFromDevice -> {
                DeleteSongsDialog
                    .create(currentSong)
                    .show(childFragmentManager, "DELETE_SONGS")
                true
            }

            NowPlayingAction.Lyrics -> {
                if (currentFragment(R.id.fragment_container) is LyricsFragment) {
                    (activity as? MainActivity)?.collapsePanel()
                } else {
                    goToDestination(requireActivity(), R.id.nav_lyrics)
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

            NowPlayingAction.AddToPlaylist -> {
                AddToPlaylistDialog
                    .create(currentSong)
                    .show(childFragmentManager, "ADD_TO_PLAYLIST")
                true
            }

            NowPlayingAction.ToggleFavoriteState -> {
                playerViewModel.toggleFavorite()
                true
            }

            NowPlayingAction.TagEditor -> {
                val intent = Intent(requireContext(), SongTagEditorActivity::class.java)
                intent.putExtra(AbsTagEditorActivity.EXTRA_TARGET, EditTarget.song(currentSong))
                startActivity(intent)
                true
            }

            NowPlayingAction.SleepTimer -> {
                findActivityNavController(R.id.fragment_container)
                    .navigate(R.id.nav_sleep_timer)
                true
            }

            NowPlayingAction.SoundSettings -> {
                goToDestination(requireActivity(), R.id.nav_sound_settings)
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

            NowPlayingAction.Nothing -> {
                false
            }
        }
    }
}
