package com.ssilencce.ui.screen.player.compose

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
fun GradientNowPlayingScreen(
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
    val queue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val position by playerViewModel.positionFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffleEnabled by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()
    val extraInfo by playerViewModel.extraInfoFlow.collectAsState()

    val scheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(context),
    )

    val nps = NowPlayingScreen.Gradient
    val transition = Preferences.getNowPlayingTransition(nps)

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

    val scrimAlpha by animateFloatAsState(if (showLyricsOnCover) 0f else 1f, label = "gradientScrim")

    PlayerTheme(scheme) {
        Box(modifier = modifier.fillMaxSize().background(scheme.surface)) {
            Column(modifier = Modifier.fillMaxSize()) {
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
                                isCarouselEffect = false,
                                isLandscape =
                                    LocalConfiguration.current.orientation ==
                                        Configuration.ORIENTATION_LANDSCAPE,
                                coverGestures = Modifier.playerCoverGestures(coverGestures),
                                onSongSelected = playerViewModel::playSongAt,
                                cornerRadius = 0.dp,
                                enableShadow = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                scheme.surface.copy(alpha = 0.85f * scrimAlpha),
                                                scheme.surface.copy(alpha = scrimAlpha),
                                            ),
                                    ),
                                ),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    AuroraIconButton(
                        icon = painterResource(if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp),
                        contentDescription = stringResource(R.string.toggle_favorite),
                        isActive = isFavorite,
                        onClick = { onAction(NowPlayingAction.ToggleFavoriteState) },
                    )
                    SongMeta(
                        song = currentSong,
                        extraInfo = null,
                        modifier = Modifier.weight(1f),
                    )
                    AuroraIconButton(
                        icon = painterResource(R.drawable.ic_more_vert_24dp),
                        contentDescription = stringResource(R.string.action_more),
                        onClick = { onAction(NowPlayingAction.WebSearch) },
                    )
                }

                ProgressSection(
                    progress = progress,
                    duration = if (duration != C.TIME_UNSET) duration else currentSong.duration,
                    viewModel = playerViewModel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )

                TransportRow(
                    isPlaying = isPlaying,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    viewModel = playerViewModel,
                    onAction = onAction,
                    playButtonSize = 72.dp,
                    modifier = Modifier.fillMaxWidth(),
                )

                Box(
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val infoText = extraInfo
                    if (infoText != null) {
                        Text(
                            text = infoText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    AuroraIconButton(
                        icon = painterResource(R.drawable.ic_queue_music_24dp),
                        contentDescription = stringResource(R.string.playing_queue_label),
                        onClick = { onAction(NowPlayingAction.OpenPlayQueue) },
                    )
                    AuroraIconButton(
                        icon = painterResource(R.drawable.ic_lyrics_outline_24dp),
                        contentDescription = stringResource(R.string.action_show_lyrics),
                        onClick = { handleAction(NowPlayingAction.Lyrics) },
                    )
                    AuroraIconButton(
                        icon = painterResource(R.drawable.ic_equalizer_24dp),
                        contentDescription = stringResource(R.string.equalizer_label),
                        onClick = { onAction(NowPlayingAction.SoundSettings) },
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
