package com.ssilencce.ui.screen.player.compose

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.core.model.theme.NowPlayingScreen
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.ui.screen.lyrics.CoverLyricsScreen
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.PlayerTheme
import com.ssilencce.util.Preferences

@Composable
fun ExpressiveNowPlayingScreen(
    playerViewModel: PlayerViewModel,
    lyricsViewModel: LyricsViewModel,
    onAction: (NowPlayingAction) -> Unit,
    isFavorite: Boolean,
    onClose: () -> Unit,
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

    val nps = NowPlayingScreen.Expressive
    val transition = Preferences.getNowPlayingTransition(nps)
    val isCarousel = Preferences.isCarouselEffect
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        ExpressiveCover(
                            queue = queue,
                            position = position.current.coerceAtLeast(0),
                            transition = transition,
                            isCarousel = isCarousel,
                            coverGestures = coverGestures,
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            showLyricsOnCover = showLyricsOnCover,
                            hasSong = currentSong.id != -1L,
                            onAction = onAction,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ExpressiveMeta(
                            song = currentSong,
                            extraInfo = extraInfo,
                            isFavorite = isFavorite,
                            onAction = onAction,
                        )
                        ExpressiveControls(
                            progress = progress,
                            duration = if (duration != C.TIME_UNSET) duration else currentSong.duration,
                            isPlaying = isPlaying,
                            viewModel = playerViewModel,
                        )
                        ExpressiveBottomGroup(
                            repeatMode = repeatMode,
                            shuffleEnabled = shuffleEnabled,
                            viewModel = playerViewModel,
                            onAction = onAction,
                            handleLyrics = { handleAction(NowPlayingAction.Lyrics) },
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        AuroraIconButton(
                            icon = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                            contentDescription = stringResource(R.string.close_action),
                            onClick = onClose,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        AuroraIconButton(
                            icon = painterResource(R.drawable.ic_more_vert_24dp),
                            contentDescription = stringResource(R.string.action_more),
                            onClick = { onAction(NowPlayingAction.WebSearch) },
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        ExpressiveCover(
                            queue = queue,
                            position = position.current.coerceAtLeast(0),
                            transition = transition,
                            isCarousel = isCarousel,
                            coverGestures = coverGestures,
                            playerViewModel = playerViewModel,
                            lyricsViewModel = lyricsViewModel,
                            showLyricsOnCover = showLyricsOnCover,
                            hasSong = currentSong.id != -1L,
                            onAction = onAction,
                        )
                    }

                    ExpressiveMeta(
                        song = currentSong,
                        extraInfo = extraInfo,
                        isFavorite = isFavorite,
                        onAction = onAction,
                    )

                    ExpressiveControls(
                        progress = progress,
                        duration = if (duration != C.TIME_UNSET) duration else currentSong.duration,
                        isPlaying = isPlaying,
                        viewModel = playerViewModel,
                    )

                    ExpressiveBottomGroup(
                        repeatMode = repeatMode,
                        shuffleEnabled = shuffleEnabled,
                        viewModel = playerViewModel,
                        onAction = onAction,
                        handleLyrics = { handleAction(NowPlayingAction.Lyrics) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpressiveCover(
    queue: List<Song>,
    position: Int,
    transition: com.ssilencce.core.model.player.PlayerTransition,
    isCarousel: Boolean,
    coverGestures: CoverGestures,
    playerViewModel: PlayerViewModel,
    lyricsViewModel: LyricsViewModel,
    showLyricsOnCover: Boolean,
    hasSong: Boolean,
    onAction: (NowPlayingAction) -> Unit,
) {
    CoverLyricsCrossfade(
        showLyrics = showLyricsOnCover,
        hasSong = hasSong,
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
                currentPosition = position,
                transition = transition,
                isCarouselEffect = isCarousel,
                isLandscape = false,
                coverGestures = Modifier.playerCoverGestures(coverGestures),
                onSongSelected = playerViewModel::playSongAt,
                modifier = Modifier.fillMaxSize(),
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ExpressiveMeta(
    song: Song,
    extraInfo: String?,
    isFavorite: Boolean,
    onAction: (NowPlayingAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MarqueeText(
                text = song.title,
                animationEnabled = Preferences.enableScrollingText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = song.displayArtistName(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (extraInfo != null) {
                Text(
                    text = extraInfo,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuroraIconButton(
                icon = painterResource(if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp),
                contentDescription = stringResource(R.string.toggle_favorite),
                isActive = isFavorite,
                onClick = { onAction(NowPlayingAction.ToggleFavoriteState) },
                size = 44.dp,
            )
            Box(
                modifier = Modifier.width(1.dp).height(24.dp)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)),
            )
            AuroraIconButton(
                icon = painterResource(R.drawable.ic_more_vert_24dp),
                contentDescription = stringResource(R.string.action_more),
                onClick = { onAction(NowPlayingAction.WebSearch) },
                size = 44.dp,
            )
        }
    }
}

@Composable
private fun ExpressiveControls(
    progress: Long,
    duration: Long,
    isPlaying: Boolean,
    viewModel: PlayerViewModel,
) {
    val fraction =
        if (duration > 0) {
            (progress.toFloat() / duration).coerceIn(0f, 1f)
        } else {
            0f
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        AuroraSlider(
            value = fraction,
            enabled = true,
            onValueChange = {},
            onValueChangeFinished = { f -> viewModel.seekTo((f * duration).toLong()) },
            trackHeight = 12.dp,
            thumbSize = 28.dp,
            containerHeight = 40.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(progress),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { viewModel.seekToPrevious() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_previous_m3_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
            ExpressivePlayButton(isPlaying = isPlaying, onClick = { viewModel.togglePlayPause() })
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { viewModel.seekToNext() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_next_m3_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpressivePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val rotation = rememberInfiniteTransition(label = "expressiveRotation")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "expressiveAngle",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(80.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .graphicsLayer { rotationZ = if (isPlaying) angle else 0f }
                .clickable { onClick() },
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun ExpressiveBottomGroup(
    repeatMode: Int,
    shuffleEnabled: Boolean,
    viewModel: PlayerViewModel,
    onAction: (NowPlayingAction) -> Unit,
    handleLyrics: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val itemModifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                itemModifier
                    .background(
                        if (repeatMode != Player.REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { viewModel.cycleRepeatMode() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_repeat_24dp),
                contentDescription = stringResource(R.string.repeat_mode),
                tint =
                    if (repeatMode != Player.REPEAT_MODE_OFF) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                itemModifier
                    .background(
                        if (shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { viewModel.toggleShuffleMode() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle_24dp),
                contentDescription = stringResource(R.string.action_toggle_shuffle),
                tint =
                    if (shuffleEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = itemModifier.clickable { onAction(NowPlayingAction.OpenPlayQueue) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_queue_music_24dp),
                contentDescription = stringResource(R.string.playing_queue_label),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = itemModifier.clickable { onAction(NowPlayingAction.SoundSettings) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_equalizer_24dp),
                contentDescription = stringResource(R.string.equalizer_label),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = itemModifier.clickable { handleLyrics() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lyrics_outline_24dp),
                contentDescription = stringResource(R.string.action_show_lyrics),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
