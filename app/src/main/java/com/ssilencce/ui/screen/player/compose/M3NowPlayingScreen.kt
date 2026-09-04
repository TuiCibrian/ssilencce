package com.ssilencce.ui.screen.player.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.ui.screen.lyrics.CoverLyricsScreen
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.PlayerTheme
import com.ssilencce.util.Preferences

@Composable
fun M3NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    lyricsViewModel: LyricsViewModel,
    onAction: (NowPlayingAction) -> Unit,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val progress by playerViewModel.progressFlow.collectAsStateWithLifecycle()
    val duration by playerViewModel.durationFlow.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val nextSong by playerViewModel.nextSongFlow.collectAsStateWithLifecycle()
    val queue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val position by playerViewModel.positionFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffleEnabled by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()
    val extraInfo by playerViewModel.extraInfoFlow.collectAsState()

    val scheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(context),
    )

    val nps = NowPlayingScreen.M3
    val transition = Preferences.getNowPlayingTransition(nps)
    val isCarousel = Preferences.isCarouselEffect

    var showLyricsOnCover by remember { mutableStateOf(Preferences.showLyricsOnCover) }

    val handleAction: (NowPlayingAction) -> Unit = { action ->
        if (action == NowPlayingAction.Lyrics && nps.supportsCoverLyrics) {
            showLyricsOnCover = !showLyricsOnCover
            Preferences.showLyricsOnCover = showLyricsOnCover
        } else {
            onAction(action)
        }
    }

    val coverGestures = remember {
        CoverGestures(
            onTap = { handleAction(Preferences.coverSingleTapAction) },
            onDoubleTapLeft = { handleAction(Preferences.coverLeftDoubleTapAction) },
            onDoubleTapCenter = { handleAction(Preferences.coverDoubleTapAction) },
            onDoubleTapRight = { handleAction(Preferences.coverRightDoubleTapAction) },
            onLongPress = { handleAction(Preferences.coverLongPressAction) },
        )
    }

    PlayerTheme(scheme) {
        Box(modifier = modifier.fillMaxSize()) {
            NowPlayingBackground(song = currentSong, scheme = scheme)

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    CoverLyricsCrossfade(
                        showLyrics = showLyricsOnCover,
                        hasSong = currentSong.id != -1L,
                        lyricsContent = {
                            CoverLyricsScreen(
                                lyricsViewModel = lyricsViewModel,
                                playerViewModel = playerViewModel,
                                onExpandClick = { onAction(NowPlayingAction.Lyrics) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                        coverContent = {
                            CoverPager(
                                queue = queue,
                                currentPosition = position.current.coerceAtLeast(0),
                                transition = transition,
                                isCarouselEffect = isCarousel,
                                isLandscape =
                                    LocalConfiguration.current.orientation ==
                                        Configuration.ORIENTATION_LANDSCAPE,
                                coverGestures = Modifier.playerCoverGestures(coverGestures),
                                onSongSelected = playerViewModel::playSongAt,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                SongMeta(
                    song = currentSong,
                    extraInfo = extraInfo,
                    modifier = Modifier.fillMaxWidth(),
                )

                IconActionsRow(
                    isFavorite = isFavorite,
                    onLyricsClick = { handleAction(NowPlayingAction.Lyrics) },
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )

                ProgressSection(
                    progress = progress,
                    duration = if (duration != C.TIME_UNSET) duration else currentSong.duration,
                    viewModel = playerViewModel,
                    modifier = Modifier.fillMaxWidth(),
                )

                TransportRow(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    viewModel = playerViewModel,
                    onAction = onAction,
                    previousIcon = painterResource(R.drawable.ic_previous_m3_24dp),
                    nextIcon = painterResource(R.drawable.ic_next_m3_24dp),
                    playIcon = painterResource(R.drawable.ic_play_m3_24dp),
                    pauseIcon = painterResource(R.drawable.ic_pause_m3_24dp),
                    useSecondaryPlayButton = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                NextSongInfo(
                    nextSong = nextSong,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
