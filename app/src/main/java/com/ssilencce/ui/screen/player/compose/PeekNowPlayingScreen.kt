package com.ssilencce.ui.screen.player.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import com.ssilencce.R
import com.ssilencce.core.model.action.NowPlayingAction
import com.ssilencce.core.model.player.PlayerColorScheme
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.ui.component.compose.MediaImage
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.PlayerTheme

@Composable
fun PeekNowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onAction: (NowPlayingAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val progress by playerViewModel.progressFlow.collectAsStateWithLifecycle()
    val duration by playerViewModel.durationFlow.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffleEnabled by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()
    val extraInfo by playerViewModel.extraInfoFlow.collectAsState()

    val scheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(context),
    )

    PlayerTheme(scheme) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuroraIconButton(
                    icon = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                    contentDescription = stringResource(R.string.close_action),
                    onClick = onClose,
                    size = 40.dp,
                )
                Spacer(modifier = Modifier.weight(1f))
                AuroraIconButton(
                    icon = painterResource(R.drawable.ic_more_vert_24dp),
                    contentDescription = stringResource(R.string.action_more),
                    onClick = { onAction(NowPlayingAction.WebSearch) },
                    size = 40.dp,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                MediaImage(
                    model = currentSong,
                    placeholderIcon = R.drawable.ic_music_note_24dp,
                    contentDescription = currentSong.title,
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text = currentSong.title,
                        animationEnabled = com.ssilencce.util.Preferences.enableScrollingText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = currentSong.displayArtistName(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ProgressSection(
                progress = progress,
                duration = if (duration != C.TIME_UNSET) duration else currentSong.duration,
                viewModel = playerViewModel,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuroraIconButton(
                    icon = painterResource(R.drawable.ic_repeat_24dp),
                    contentDescription = stringResource(R.string.repeat_mode),
                    isActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
                    onClick = { playerViewModel.cycleRepeatMode() },
                    size = 48.dp,
                )
                AuroraIconButton(
                    icon = painterResource(R.drawable.ic_previous_24dp),
                    contentDescription = null,
                    onClick = { playerViewModel.seekToPrevious() },
                    onLongClick = { playerViewModel.seekBack() },
                    size = 56.dp,
                )
                AuroraPlayButton(
                    isPlaying = isPlaying,
                    onClick = { playerViewModel.togglePlayPause() },
                    size = 64.dp,
                )
                AuroraIconButton(
                    icon = painterResource(R.drawable.ic_next_24dp),
                    contentDescription = null,
                    onClick = { playerViewModel.seekToNext() },
                    onLongClick = { playerViewModel.seekForward() },
                    size = 56.dp,
                )
                AuroraIconButton(
                    icon = painterResource(R.drawable.ic_shuffle_24dp),
                    contentDescription = stringResource(R.string.action_toggle_shuffle),
                    isActive = shuffleEnabled,
                    onClick = { playerViewModel.toggleShuffleMode() },
                    size = 48.dp,
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val infoText = extraInfo
                if (infoText != null) {
                    Text(
                        text = infoText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
