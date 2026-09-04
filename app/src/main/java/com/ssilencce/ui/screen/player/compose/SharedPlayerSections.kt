package com.ssilencce.ui.screen.player.compose

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.commit451.coiltransformations.BlurTransformation
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.isArtistNameUnknown
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.util.Preferences

@Composable
fun NowPlayingBackground(
    song: Song,
    scheme: PlayerColorScheme,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
fun SongMeta(
    song: Song,
    extraInfo: String?,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    textAlign: TextAlign = TextAlign.Center,
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
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        MarqueeText(
            text = song.title,
            animationEnabled = Preferences.enableScrollingText,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = secondaryInfo,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
        )
        if (extraInfo != null) {
            Text(
                text = extraInfo,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
fun IconActionsRow(
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
fun ProgressSection(
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
fun TransportRow(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    viewModel: PlayerViewModel,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
    previousIcon: Painter? = null,
    nextIcon: Painter? = null,
    playIcon: Painter? = null,
    pauseIcon: Painter? = null,
    useSecondaryPlayButton: Boolean = false,
    playButtonSize: androidx.compose.ui.unit.Dp = 62.dp,
) {
    @Composable
    fun PlayControl() {
        if (useSecondaryPlayButton) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(64.dp)
                        .shadow(12.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { viewModel.togglePlayPause() },
            ) {
                androidx.compose.material3.Icon(
                    painter = if (isPlaying) (pauseIcon ?: painterResource(R.drawable.ic_pause_24dp)) else (playIcon ?: painterResource(R.drawable.ic_play_24dp)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else if (playIcon != null || pauseIcon != null) {
            AuroraPlayButton(
                isPlaying = isPlaying,
                onClick = { viewModel.togglePlayPause() },
                playIcon = playIcon ?: painterResource(R.drawable.ic_play_24dp),
                pauseIcon = pauseIcon ?: painterResource(R.drawable.ic_pause_24dp),
                size = playButtonSize,
            )
        } else {
            AuroraPlayButton(
                isPlaying = isPlaying,
                onClick = { viewModel.togglePlayPause() },
                size = playButtonSize,
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 4.dp),
    ) {
        AuroraIconButton(
            icon = painterResource(R.drawable.ic_repeat_24dp),
            contentDescription = stringResource(R.string.repeat_mode),
            isActive = repeatMode != Player.REPEAT_MODE_OFF,
            onClick = { viewModel.cycleRepeatMode() },
        )
        AuroraIconButton(
            icon = previousIcon ?: painterResource(R.drawable.ic_previous_24dp),
            contentDescription = null,
            onClick = { viewModel.seekToPrevious() },
            onLongClick = { viewModel.seekBack() },
        )
        PlayControl()
        AuroraIconButton(
            icon = nextIcon ?: painterResource(R.drawable.ic_next_24dp),
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

    @Suppress("UNUSED_EXPRESSION")
    Unit.also { onAction }
}

@Composable
fun NextSongInfo(
    nextSong: Song,
    onAction: (NowPlayingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showNextSong = Preferences.isShowNextSong
    if (!showNextSong) return

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

@Composable
fun CoverLyricsCrossfade(
    showLyrics: Boolean,
    hasSong: Boolean,
    lyricsContent: @Composable () -> Unit,
    coverContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Crossfade(
            targetState = showLyrics,
            label = "coverLyricsCrossfade",
        ) { show ->
            if (show && hasSong) {
                lyricsContent()
            } else {
                coverContent()
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms == C.TIME_UNSET || ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
