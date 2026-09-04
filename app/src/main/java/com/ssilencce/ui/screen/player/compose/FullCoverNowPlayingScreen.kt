package com.ssilencce.ui.screen.player.compose

import android.content.res.Configuration
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ssilencce.ui.component.compose.MediaImage
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.PlayerTheme
import com.ssilencce.util.Preferences

@Composable
fun FullCoverNowPlayingScreen(
    playerViewModel: PlayerViewModel,
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
    val nextSong by playerViewModel.nextSongFlow.collectAsStateWithLifecycle()
    val queue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val position by playerViewModel.positionFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffleEnabled by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()
    val extraInfo by playerViewModel.extraInfoFlow.collectAsState()

    val scheme by playerViewModel.colorSchemeFlow.collectAsState(
        initial = PlayerColorScheme.themeColorScheme(context),
    )

    val nps = NowPlayingScreen.FullCover
    val transition = Preferences.getNowPlayingTransition(nps)
    val showNextSong = Preferences.isShowNextSong

    PlayerTheme(scheme) {
        Box(modifier = modifier.fillMaxSize().background(scheme.surface)) {
            CoverPager(
                queue = queue,
                currentPosition = position.current.coerceAtLeast(0),
                transition = transition,
                isCarouselEffect = false,
                isLandscape =
                    LocalConfiguration.current.orientation ==
                        Configuration.ORIENTATION_LANDSCAPE,
                coverGestures = Modifier,
                onSongSelected = playerViewModel::playSongAt,
                cornerRadius = 0.dp,
                enableShadow = false,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(scheme.surface.copy(alpha = 0.9f), Color.Transparent),
                            ),
                        ),
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
                                        scheme.surface.copy(alpha = 0.75f),
                                        scheme.surface,
                                    ),
                            ),
                        ),
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    AuroraIconButton(
                        icon = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                        contentDescription = stringResource(R.string.close_action),
                        onClick = onClose,
                    )
                    if (showNextSong) {
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = stringResource(R.string.next_song_label),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                            Text(
                                text = if (nextSong.id == 0L) stringResource(R.string.list_end) else nextSong.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onAction(NowPlayingAction.OpenPlayQueue) },
                            )
                        }
                        MediaImage(
                            model = nextSong,
                            placeholderIcon = R.drawable.ic_music_note_24dp,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onAction(NowPlayingAction.OpenPlayQueue) },
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
