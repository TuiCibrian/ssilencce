package com.ssilencce.ui.screen.miniplayer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ssilencce.R
import com.ssilencce.extensions.media.asReadableDuration

@Composable
fun MiniPlayerScreen(
    state: MiniPlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenInApp: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onClose() } }
                .safeDrawingPadding()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Header(
                    isPreview = state.isPreview,
                    onClose = onClose,
                )
                Spacer(modifier = Modifier.size(16.dp))
                when {
                    state.hasError -> {
                        ErrorBody(onClose = onClose)
                    }

                    state.isLoading || state.track == null -> {
                        LoadingBody()
                    }

                    else -> {
                        TrackBody(
                            state = state,
                            onPlayPause = onPlayPause,
                            onSeek = onSeek,
                            onOpenInApp = onOpenInApp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    isPreview: Boolean,
    onClose: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.mini_player_popup_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    stringResource(
                        if (isPreview) R.string.mini_player_preview_label else R.string.mini_player_external_label,
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.mini_player_close),
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Text(
            text = stringResource(R.string.mini_player_loading),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ErrorBody(onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.mini_player_unplayable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onClose) {
            Text(text = stringResource(R.string.close_action))
        }
    }
}

@Composable
private fun TrackBody(
    state: MiniPlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenInApp: () -> Unit,
) {
    val track = state.track ?: return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Artwork(track)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                track.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                track.album?.let { album ->
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        SeekBar(state = state, onSeek = onSeek)
        if (state.blockedByMute) {
            Text(
                text = stringResource(R.string.playback_muted_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.isPreview) {
            Text(
                text = stringResource(R.string.mini_player_preview_no_queue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (state.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
                        ),
                    contentDescription =
                        stringResource(
                            if (state.isPlaying) R.string.mini_player_pause else R.string.mini_player_play,
                        ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.canOpenInApp) {
                TextButton(onClick = onOpenInApp) {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new_24dp),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.mini_player_open_in_app))
                }
            }
        }
    }
}

@Composable
private fun Artwork(track: MiniPlayerTrack) {
    Surface(
        modifier = Modifier.size(96.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (track.artworkUri != null) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = stringResource(R.string.mini_player_artwork_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SeekBar(
    state: MiniPlayerUiState,
    onSeek: (Long) -> Unit,
) {
    var dragFraction by remember(state.track?.uri) { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.coerceAtLeast(0L)
    val fraction =
        dragFraction
            ?: if (duration > 0L) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val seekDescription = stringResource(R.string.mini_player_seek_position)
    Column {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            enabled = duration > 0L,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = seekDescription },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = (dragFraction?.let { (it * duration).toLong() } ?: state.positionMs).asReadableDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = duration.asReadableDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
