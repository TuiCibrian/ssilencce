package com.ssilencce.ui.screen.player.compose

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import android.content.res.Configuration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.commit451.coiltransformations.BlurTransformation
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.isArtistNameUnknown
import com.ssilencce.ui.screen.lyrics.CoverLyricsScreen
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.PlayerTheme
import com.ssilencce.util.Preferences

@Composable
fun DefaultNowPlayingScreen(
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

    val nps = NowPlayingScreen.Default
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
                    Crossfade(
                        targetState = showLyricsOnCover,
                        label = "coverLyricsCrossfade",
                    ) { showLyrics ->
                        if (showLyrics && currentSong.id != -1L) {
                            CoverLyricsScreen(
                                lyricsViewModel = lyricsViewModel,
                                playerViewModel = playerViewModel,
                                onExpandClick = { onAction(NowPlayingAction.Lyrics) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
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
                        }
                    }
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

@Composable
private fun NowPlayingBackground(
    song: com.ssilencce.data.model.Song,
    scheme: PlayerColorScheme,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (song.id != -1L && scheme.blurToken.isBlur) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(song)
                        .size(256)
                        .memoryCacheKey("nowplaying:song:${song.id}")
                        .crossfade(1000)
                        .transformations(
                            BlurTransformation(
                                context = LocalContext.current,
                                radius = scheme.blurToken.blurRadius.coerceIn(0f, 25f),
                            ),
                        )
                        .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    scheme.surface.copy(alpha = 0.7f),
                                    scheme.surface,
                                ),
                        ),
                    ),
        )
    }
}

@Composable
private fun SongMeta(
    song: com.ssilencce.data.model.Song,
    extraInfo: String?,
    modifier: Modifier = Modifier,
) {
    val displayAlbum = Preferences.displayAlbumTitle
    val artist = song.displayArtistName()
    val album = song.albumName
    val secondaryInfo =
        if (album.isNotEmpty() && displayAlbum) {
            "$artist — $album"
        } else {
            artist
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        MarqueeText(
            text = song.title,
            animationEnabled = Preferences.enableScrollingText,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = secondaryInfo,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (extraInfo != null) {
            Text(
                text = extraInfo,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun IconActionsRow(
    isFavorite: Boolean,
    onLyricsClick: () -> Unit,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .padding(top = 16.dp),
    ) {
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_equalizer_24dp),
            contentDescription = stringResource(R.string.equalizer_label),
            onClick = { onAction(NowPlayingAction.SoundSettings) },
        )
        AuroraIconButton(
            icon = painterResource(if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp),
            contentDescription = stringResource(R.string.toggle_favorite),
            isActive = isFavorite,
            activeTint = MaterialTheme.colorScheme.primary,
            onClick = { onAction(NowPlayingAction.ToggleFavoriteState) },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_lyrics_outline_24dp),
            contentDescription = stringResource(R.string.action_show_lyrics),
            onClick = onLyricsClick,
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_queue_music_24dp),
            contentDescription = stringResource(R.string.playing_queue_label),
            onClick = { onAction(NowPlayingAction.OpenPlayQueue) },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_timer_24dp),
            contentDescription = stringResource(R.string.action_sleep_timer),
            onClick = { onAction(NowPlayingAction.SleepTimer) },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_playlist_add_24dp),
            contentDescription = stringResource(R.string.action_add_to_playlist),
            onClick = { onAction(NowPlayingAction.AddToPlaylist) },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_more_vert_24dp),
            contentDescription = stringResource(R.string.action_more),
            onClick = { onAction(NowPlayingAction.WebSearch) },
        )
    }
}

@Composable
private fun ProgressSection(
    progress: Long,
    duration: Long,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val fraction =
        if (duration > 0) {
            (progress.toFloat() / duration).coerceIn(0f, 1f)
        } else {
            0f
        }

    Column(modifier = modifier.padding(top = 8.dp).fillMaxWidth()) {
        AuroraSlider(
            value = fraction,
            enabled = true,
            onValueChange = {},
            onValueChangeFinished = { f ->
                viewModel.seekTo((f * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(progress),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatTime(duration),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    viewModel: PlayerViewModel,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 4.dp),
    ) {
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_repeat_24dp),
            contentDescription = stringResource(R.string.repeat_mode),
            isActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
            onClick = { viewModel.cycleRepeatMode() },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_previous_24dp),
            contentDescription = null,
            onClick = { viewModel.seekToPrevious() },
            onLongClick = { viewModel.seekBack() },
        )
        AuroraPlayButton(
            isPlaying = isPlaying,
            onClick = { viewModel.togglePlayPause() },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_next_24dp),
            contentDescription = null,
            onClick = { viewModel.seekToNext() },
            onLongClick = { viewModel.seekForward() },
        )
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_shuffle_24dp),
            contentDescription = stringResource(R.string.action_toggle_shuffle),
            isActive = shuffleEnabled,
            onClick = { viewModel.toggleShuffleMode() },
        )
    }
}

@Composable
private fun NextSongInfo(
    nextSong: com.ssilencce.data.model.Song,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showNextSong = Preferences.isShowNextSong
    if (!showNextSong) return

    val isEnd = nextSong.isArtistNameUnknown() && nextSong.id == 0L
    val label =
        if (nextSong.id == 0L) {
            stringResource(R.string.list_end)
        } else {
            val artist = nextSong.displayArtistName()
            if (nextSong.isArtistNameUnknown()) {
                stringResource(R.string.next_song_x, nextSong.title)
            } else {
                stringResource(R.string.next_song_x_by_artist_x, nextSong.title, artist)
            }
        }

    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .padding(top = 10.dp)
                .clickable { onAction(NowPlayingAction.OpenPlayQueue) },
    )
}

private fun formatTime(ms: Long): String {
    if (ms == C.TIME_UNSET || ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
