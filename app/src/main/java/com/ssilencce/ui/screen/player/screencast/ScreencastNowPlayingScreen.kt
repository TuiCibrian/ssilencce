package com.ssilencce.ui.screen.player.screencast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ssilencce.R
import com.ssilencce.data.model.lyrics.SyncedLyrics
import com.ssilencce.extensions.media.asReadableDuration

private val BackgroundTop = Color(0xFF14141A)
private val BackgroundBottom = Color(0xFF000000)
private val PrimaryText = Color.White
private val SecondaryText = Color.White.copy(alpha = 0.72f)
private val MutedText = Color.White.copy(alpha = 0.48f)

/**
 * Full-screen Now Playing surface rendered on the external display. It only consumes
 * [ScreencastNowPlayingState]; playback and DSP stay in the main process pipeline.
 */
@Composable
fun ScreencastNowPlayingScreen(
    state: ScreencastNowPlayingState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom)))
                .padding(40.dp),
    ) {
        if (!state.hasMedia) {
            IdleContent(Modifier.align(Alignment.Center))
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth > maxHeight) {
                    WideLayout(state)
                } else {
                    TallLayout(state)
                }
            }
        }
    }
}

@Composable
private fun WideLayout(state: ScreencastNowPlayingState) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Artwork(
            state = state,
            modifier = Modifier.weight(0.44f).fillMaxHeight(),
        )
        Column(
            modifier = Modifier.weight(0.56f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            TrackHeader(state = state, centered = false)
            Spacer(Modifier.height(24.dp))
            LyricsPanel(
                state = state,
                centered = false,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            PlaybackProgress(state)
        }
    }
}

@Composable
private fun TallLayout(state: ScreencastNowPlayingState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(
            state = state,
            modifier = Modifier.fillMaxWidth(0.58f).weight(0.5f),
        )
        Spacer(Modifier.height(24.dp))
        TrackHeader(state = state, centered = true)
        Spacer(Modifier.height(16.dp))
        LyricsPanel(
            state = state,
            centered = true,
            modifier = Modifier.weight(0.35f).fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        PlaybackProgress(state)
    }
}

@Composable
private fun Artwork(
    state: ScreencastNowPlayingState,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val artworkUri = state.artworkUri
        if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = stringResource(R.string.album_art_preview_content_description),
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp)),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_24dp),
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(96.dp),
                )
            }
        }
    }
}

@Composable
private fun TrackHeader(
    state: ScreencastNowPlayingState,
    centered: Boolean,
) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = state.title.ifBlank { stringResource(R.string.screencast_untitled) },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        if (state.artist.isNotBlank()) {
            Text(
                text = state.artist,
                style = MaterialTheme.typography.headlineSmall,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
        if (state.album.isNotBlank() && state.album != state.title) {
            Text(
                text = state.album,
                style = MaterialTheme.typography.titleMedium,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun LyricsPanel(
    state: ScreencastNowPlayingState,
    centered: Boolean,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val lyrics = state.lyrics) {
            ScreencastLyrics.None -> {
                Unit
            }

            ScreencastLyrics.Instrumental -> {
                Text(
                    text = stringResource(R.string.screencast_instrumental),
                    style = MaterialTheme.typography.headlineSmall,
                    color = SecondaryText,
                )
            }

            is ScreencastLyrics.Plain -> {
                PlainLyrics(
                    lines = lyrics.lines,
                    centered = centered,
                )
            }

            is ScreencastLyrics.Synced -> {
                SyncedLyricsPanel(
                    lyrics = lyrics.lyrics,
                    positionMs = state.positionMs,
                    centered = centered,
                )
            }
        }
    }
}

@Composable
private fun PlainLyrics(
    lines: List<String>,
    centered: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        lines.take(MAX_PLAIN_LINES).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SyncedLyricsPanel(
    lyrics: SyncedLyrics,
    positionMs: Long,
    centered: Boolean,
) {
    val lines = lyrics.lines
    if (lines.isEmpty()) return
    val adjustedPosition = positionMs + lyrics.offset
    val currentIndex = lines.indexOfLast { adjustedPosition >= it.start }
    val firstIndex =
        if (currentIndex < 0) {
            0
        } else {
            (currentIndex - VISIBLE_LYRIC_NEIGHBOURS).coerceAtLeast(0)
        }
    val lastIndex =
        if (currentIndex < 0) {
            (VISIBLE_LYRIC_NEIGHBOURS * 2).coerceAtMost(lines.lastIndex)
        } else {
            (currentIndex + VISIBLE_LYRIC_NEIGHBOURS).coerceAtMost(lines.lastIndex)
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        for (index in firstIndex..lastIndex) {
            val line = lines[index]
            val isCurrent = index == currentIndex
            Text(
                text = line.content.content.ifBlank { "♪" },
                style =
                    if (isCurrent) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent) PrimaryText else MutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlaybackProgress(state: ScreencastNowPlayingState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
            color = PrimaryText,
            trackColor = Color.White.copy(alpha = 0.18f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = state.positionMs.asReadableDuration(),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (state.isPlaying) {
                                R.drawable.ic_pause_24dp
                            } else {
                                R.drawable.ic_play_24dp
                            },
                        ),
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = state.durationMs.asReadableDuration(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                )
            }
        }
    }
}

@Composable
private fun IdleContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_screencast_24dp),
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(96.dp),
        )
        Text(
            text = stringResource(R.string.screencast_idle_title),
            style = MaterialTheme.typography.headlineMedium,
            color = PrimaryText,
        )
        Text(
            text = stringResource(R.string.screencast_idle_summary),
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryText,
        )
    }
}

private const val VISIBLE_LYRIC_NEIGHBOURS = 1
private const val MAX_PLAIN_LINES = 6
