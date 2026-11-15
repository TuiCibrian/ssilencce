/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.screen.lockscreen

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.ssilencce.R
import com.ssilencce.coil.songImage
import com.ssilencce.core.model.lockscreen.LockScreenWallpaper
import com.ssilencce.data.model.QueueSong
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.ui.screen.lyrics.LyricsUiState
import com.ssilencce.ui.screen.lyrics.LyricsViewModel
import com.ssilencce.ui.screen.player.PlayerGesturesController
import com.ssilencce.ui.screen.player.PlayerGesturesController.GestureType
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.util.Preferences
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class LockScreenPanel {
    None,
    Queue,
    Lyrics,
}

private val muted = Color.White.copy(alpha = .68f)
private val translucentSurface = Color.White.copy(alpha = .09f)

@Composable
internal fun LockScreenPlayerScreen(
    playerViewModel: PlayerViewModel,
    lyricsViewModel: LyricsViewModel,
    preview: Boolean,
    onDismiss: () -> Unit,
    onOpenArtist: (Song) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenGenre: (Song) -> Unit,
    onOpenPlaylist: (Song) -> Unit,
    ghostModeEnabled: Boolean,
    onOpenGhostSearch: () -> Unit,
) {
    val song by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val nextSong by playerViewModel.nextSongFlow.collectAsStateWithLifecycle()
    val playing by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val queue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val queuePosition by playerViewModel.positionFlow.collectAsStateWithLifecycle()
    val progress by playerViewModel.progressFlow.collectAsStateWithLifecycle()
    val duration by playerViewModel.durationFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val repeatLoadNextPause by playerViewModel.repeatLoadNextPauseFlow.collectAsStateWithLifecycle()
    val shuffle by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()

    var panel by rememberSaveable { mutableStateOf(LockScreenPanel.None) }
    var dismissing by remember { mutableStateOf(false) }
    val dismissNow by rememberUpdatedState(onDismiss)

    BackHandler(enabled = panel != LockScreenPanel.None) { panel = LockScreenPanel.None }
    BackHandler(enabled = panel == LockScreenPanel.None) { dismissing = true }

    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(180)
            dismissNow()
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                if (dismissing) {
                    translationY = -size.height * .12f
                    alpha = .4f
                }
            },
    ) {
        val landscape = maxWidth > maxHeight

        LockScreenWallpaperBackground(song)

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .pointerInput(panel) {
                    if (panel != LockScreenPanel.None) return@pointerInput
                    // Detect a deliberate upward flick after the gesture finishes. Unlike a drag
                    // detector this never steals vertical scrolling from the controls or panels.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        var last = down
                        var handledByChild = false
                        var multiplePointers = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            multiplePointers = multiplePointers || event.changes.size > 1
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.position != change.previousPosition && change.isConsumed) {
                                handledByChild = true
                            }
                            last = change
                        } while (last.pressed)
                        val distanceX = last.position.x - down.position.x
                        val distanceY = last.position.y - down.position.y
                        val threshold = viewConfiguration.touchSlop * 4
                        if (!handledByChild &&
                            !multiplePointers &&
                            distanceY < -threshold &&
                            abs(distanceY) > abs(distanceX)
                        ) {
                            dismissing = true
                        }
                    }
                },
        ) {
            LockScreenTopBar(
                preview = preview,
                queueOpen = panel == LockScreenPanel.Queue,
                lyricsOpen = panel == LockScreenPanel.Lyrics,
                ghostModeEnabled = ghostModeEnabled,
                onClose = { dismissing = true },
                onQueue = { panel = if (panel == LockScreenPanel.Queue) LockScreenPanel.None else LockScreenPanel.Queue },
                onLyrics = { panel = if (panel == LockScreenPanel.Lyrics) LockScreenPanel.None else LockScreenPanel.Lyrics },
                onGhostSearch = onOpenGhostSearch,
            )
            if (landscape) {
                Row(Modifier.weight(1f)) {
                    LockScreenCover(
                        song = song,
                        modifier = Modifier.weight(.95f).fillMaxHeight(),
                        onGesture = { gesture -> handlePlayerGesture(playerViewModel, gesture) },
                    )
                    LockScreenControls(
                        song = song,
                        nextSong = nextSong,
                        playing = playing,
                        progress = progress,
                        duration = duration,
                        repeatMode = repeatMode,
                        repeatLoadNextPause = repeatLoadNextPause,
                        shuffle = shuffle,
                        playerViewModel = playerViewModel,
                        onOpenArtist = onOpenArtist,
                        onOpenFolder = onOpenFolder,
                        onOpenGenre = onOpenGenre,
                        onOpenPlaylist = onOpenPlaylist,
                        modifier = Modifier.weight(1.05f).fillMaxHeight(),
                    )
                }
            } else {
                LockScreenCover(
                    song = song,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onGesture = { gesture -> handlePlayerGesture(playerViewModel, gesture) },
                )
                LockScreenControls(
                    song = song,
                    nextSong = nextSong,
                    playing = playing,
                    progress = progress,
                    duration = duration,
                    repeatMode = repeatMode,
                    repeatLoadNextPause = repeatLoadNextPause,
                    shuffle = shuffle,
                    playerViewModel = playerViewModel,
                    onOpenArtist = onOpenArtist,
                    onOpenFolder = onOpenFolder,
                    onOpenGenre = onOpenGenre,
                    onOpenPlaylist = onOpenPlaylist,
                    modifier = Modifier.weight(1.05f).fillMaxWidth(),
                )
            }
        }

        AnimatedVisibility(
            visible = panel == LockScreenPanel.Queue,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            LockScreenQueuePanel(
                queue = queue,
                currentPosition = queuePosition.current,
                onSelect = { index ->
                    playerViewModel.playSongAt(index)
                    panel = LockScreenPanel.None
                },
                onClose = { panel = LockScreenPanel.None },
            )
        }

        AnimatedVisibility(
            visible = panel == LockScreenPanel.Lyrics,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            LockScreenLyricsPanel(
                lyricsViewModel = lyricsViewModel,
                progress = progress,
                onSeekTo = playerViewModel::seekTo,
                onClose = { panel = LockScreenPanel.None },
            )
        }
    }
}

@Composable
private fun LockScreenWallpaperBackground(song: Song) {
    val wallpaper = remember { Preferences.lockScreenWallpaper }
    val customUri = remember { Preferences.lockScreenWallpaperUri }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (song.id >= 0) {
            AsyncImage(
                model = song,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                        }.blur(42.dp),
            )
        }
        if (wallpaper == LockScreenWallpaper.Custom && !customUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(customUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = .46f),
                            Color.Black.copy(alpha = .58f),
                            Color.Black.copy(alpha = .86f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun LockScreenTopBar(
    preview: Boolean,
    queueOpen: Boolean,
    lyricsOpen: Boolean,
    ghostModeEnabled: Boolean,
    onClose: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onGhostSearch: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painterResource(R.drawable.ic_close_24dp),
                stringResource(R.string.close_action),
                tint = Color.White,
            )
        }
        Text(
            text = stringResource(if (preview) R.string.lock_screen_preview_badge else R.string.lock_screen_player_title),
            modifier = Modifier.weight(1f),
            color = muted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (ghostModeEnabled) {
            IconButton(onClick = onGhostSearch) {
                Icon(
                    painterResource(R.drawable.ic_visibility_off_24dp),
                    stringResource(R.string.ghost_search_title),
                    tint = muted,
                )
            }
        }
        IconButton(onClick = onLyrics) {
            Icon(
                painterResource(if (lyricsOpen) R.drawable.ic_lyrics_24dp else R.drawable.ic_lyrics_outline_24dp),
                stringResource(if (lyricsOpen) R.string.action_hide_lyrics else R.string.action_show_lyrics),
                tint = if (lyricsOpen) Color.White else muted,
            )
        }
        IconButton(onClick = onQueue) {
            Icon(
                painterResource(R.drawable.ic_queue_music_24dp),
                stringResource(R.string.playing_queue_label),
                tint = if (queueOpen) Color.White else muted,
            )
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun LockScreenCover(
    song: Song,
    modifier: Modifier,
    onGesture: (GestureType) -> Unit,
) {
    val context = LocalContext.current
    val enabled = remember { Preferences.lockScreenGesturesEnabled }
    val listener by rememberUpdatedState(onGesture)
    val controller =
        remember(context, enabled) {
            if (!enabled) {
                null
            } else {
                PlayerGesturesController(
                    context,
                    acceptedGestures =
                        setOf(
                            GestureType.Tap,
                            GestureType.DoubleTap(GestureType.DoubleTap.TYPE_LEFT_EDGE),
                            GestureType.DoubleTap(GestureType.DoubleTap.TYPE_CENTER),
                            GestureType.DoubleTap(GestureType.DoubleTap.TYPE_RIGHT_EDGE),
                            GestureType.Fling(GestureType.Fling.DIRECTION_LEFT),
                            GestureType.Fling(GestureType.Fling.DIRECTION_RIGHT),
                        ),
                    listener =
                        object : PlayerGesturesController.Listener {
                            override fun gestureDetected(gestureType: GestureType): Boolean {
                                listener(gestureType)
                                return true
                            }
                        },
                )
            }
        }
    DisposableEffect(controller) {
        onDispose { controller?.release() }
    }

    Box(
        modifier.padding(horizontal = 28.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val coverSize = minOf(maxWidth, maxHeight)
            val coverDescription = stringResource(R.string.album_art_preview_content_description)
            AndroidView(
                factory = { viewContext ->
                    ImageView(viewContext).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        contentDescription = coverDescription
                        setOnTouchListener(controller)
                    }
                },
                update = { imageView ->
                    imageView.songImage(song)
                    imageView.contentDescription = coverDescription
                },
                modifier =
                    Modifier
                        .size(coverSize)
                        .clip(RoundedCornerShape(28.dp)),
            )
        }
    }
}

@Composable
private fun LockScreenControls(
    song: Song,
    nextSong: Song,
    playing: Boolean,
    progress: Long,
    duration: Long,
    repeatMode: Int,
    repeatLoadNextPause: Boolean,
    shuffle: Boolean,
    playerViewModel: PlayerViewModel,
    onOpenArtist: (Song) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenGenre: (Song) -> Unit,
    onOpenPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSong = song.id >= 0
    val shortcutsEnabled = remember { Preferences.lockScreenShortcutsEnabled }
    val showNextSong = remember { Preferences.isShowNextSong }
    val folderPath = remember(song.data) { song.data.substringBeforeLast('/', missingDelimiterValue = "") }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hasSong) song.title else stringResource(R.string.lock_screen_nothing_playing),
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (hasSong) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.displayArtistName(),
                color = muted,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (song.albumName.isNotBlank()) {
                Text(
                    text = song.albumName,
                    color = muted.copy(alpha = .8f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        LockScreenSeekBar(
            progress = progress,
            duration = duration,
            onSeekTo = playerViewModel::seekTo,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LockScreenAction(
                icon = if (shuffle) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp,
                description = stringResource(R.string.action_toggle_shuffle),
                active = shuffle,
                onClick = playerViewModel::toggleShuffleMode,
            )
            LockScreenAction(
                icon = R.drawable.ic_previous_24dp,
                description = stringResource(R.string.action_previous),
                iconSize = 30,
                onClick = playerViewModel::seekToPrevious,
            )
            FilledIconButton(
                onClick = playerViewModel::togglePlayPause,
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                colors =
                    androidx.compose.material3.IconButtonDefaults
                        .filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
            ) {
                Icon(
                    painterResource(if (playing) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp),
                    stringResource(if (playing) R.string.action_pause else R.string.action_play),
                    Modifier.size(38.dp),
                )
            }
            LockScreenAction(
                icon = R.drawable.ic_next_24dp,
                description = stringResource(R.string.action_next),
                iconSize = 30,
                onClick = playerViewModel::seekToNext,
            )
            LockScreenAction(
                icon =
                    when {
                        repeatLoadNextPause -> R.drawable.ic_repeat_pause_24dp
                        repeatMode == Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_on_24dp
                        repeatMode != Player.REPEAT_MODE_OFF -> R.drawable.ic_repeat_on_24dp
                        else -> R.drawable.ic_repeat_24dp
                    },
                description = stringResource(R.string.action_cycle_repeat),
                active = repeatLoadNextPause || repeatMode != Player.REPEAT_MODE_OFF,
                onClick = playerViewModel::cycleRepeatMode,
            )
        }

        if (hasSong && shortcutsEnabled) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (song.artistId > 0 || song.artistName.isNotBlank()) {
                    LockScreenShortcut(
                        icon = R.drawable.ic_artist_24dp,
                        label = stringResource(R.string.lock_screen_shortcut_artist),
                        onClick = { onOpenArtist(song) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (folderPath.isNotBlank()) {
                    LockScreenShortcut(
                        icon = R.drawable.ic_folder_24dp,
                        label = stringResource(R.string.lock_screen_shortcut_folder),
                        onClick = { onOpenFolder(folderPath) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!song.genreName.isNullOrBlank()) {
                    LockScreenShortcut(
                        icon = R.drawable.ic_radio_24dp,
                        label = stringResource(R.string.lock_screen_shortcut_genre),
                        onClick = { onOpenGenre(song) },
                        modifier = Modifier.weight(1f),
                    )
                }
                LockScreenShortcut(
                    icon = R.drawable.ic_playlist_play_24dp,
                    label = stringResource(R.string.lock_screen_shortcut_playlist),
                    onClick = { onOpenPlaylist(song) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (showNextSong && nextSong.id >= 0 && nextSong.id != song.id) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.lock_screen_next_song, nextSong.title),
                color = muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.lock_screen_swipe_up_hint),
            color = muted.copy(alpha = .6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LockScreenSeekBar(
    progress: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
) {
    var pendingSeek by remember { mutableStateOf<Float?>(null) }
    val total = duration.takeIf { it > 0 && it != C.TIME_UNSET }?.toFloat() ?: 0f
    val current = (pendingSeek ?: progress.toFloat()).coerceIn(0f, total.coerceAtLeast(0f))
    val showRemaining = remember { Preferences.preferRemainingTime }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = current.toLong().asReadableDuration(),
            color = muted,
            fontSize = 11.sp,
            modifier = Modifier.width(46.dp),
        )
        Slider(
            value = current,
            onValueChange = { pendingSeek = it },
            onValueChangeFinished = {
                pendingSeek?.let { onSeekTo(it.toLong()) }
                pendingSeek = null
            },
            valueRange = 0f..total.coerceAtLeast(1f),
            enabled = total > 0f,
            colors =
                SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = .24f),
                ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text =
                if (showRemaining) {
                    "-" + (total - current).coerceAtLeast(0f).toLong().asReadableDuration()
                } else {
                    total.toLong().asReadableDuration()
                },
            color = muted,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(46.dp),
        )
    }
}

@Composable
private fun LockScreenAction(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    iconSize: Int = 24,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Icon(
            painterResource(icon),
            description,
            Modifier.size(iconSize.dp),
            tint = if (active) Color.White else muted,
        )
    }
}

@Composable
private fun LockScreenShortcut(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(translucentSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = Color.White)
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LockScreenQueuePanel(
    queue: List<QueueSong>,
    currentPosition: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF0101010))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
    ) {
        LockScreenPanelHeader(stringResource(R.string.playing_queue_label), onClose)
        if (queue.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.lock_screen_queue_empty), color = muted, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                itemsIndexed(queue, key = { index, item -> "${item.key.first}:${item.key.second}:$index" }) { index, item ->
                    LockScreenQueueRow(
                        song = item,
                        isCurrent = index == currentPosition,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LockScreenQueueRow(
    song: QueueSong,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color.White.copy(alpha = .13f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (isCurrent) {
                Icon(
                    painterResource(R.drawable.ic_equalizer_24dp),
                    null,
                    Modifier.size(17.dp),
                    tint = Color.White,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = .9f),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.displayArtistName(),
                color = muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            song.duration.asReadableDuration(),
            color = muted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun LockScreenLyricsPanel(
    lyricsViewModel: LyricsViewModel,
    progress: Long,
    onSeekTo: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val state by lyricsViewModel.lyricsUiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF0101010))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
    ) {
        LockScreenPanelHeader(stringResource(R.string.lock_screen_lyrics_title), onClose)
        when (val lyrics = state) {
            is LyricsUiState.Synced -> {
                val listState = rememberLazyListState()
                val offset = lyrics.syncedLyrics.offset
                val position = progress + offset
                val currentIndex =
                    lyrics.syncedLyrics.lines
                        .indexOfLast { position >= it.start }
                        .coerceAtLeast(0)
                LaunchedEffect(currentIndex) {
                    if (currentIndex in lyrics.syncedLyrics.lines.indices) {
                        listState.animateScrollToItem(currentIndex)
                    }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    itemsIndexed(lyrics.syncedLyrics.lines) { index, line ->
                        val isCurrent = index == currentIndex
                        Text(
                            text = line.content.content.ifBlank { " " },
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = .48f),
                            fontSize = if (isCurrent) 19.sp else 16.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) {
                                        onSeekTo((line.start - offset).coerceAtLeast(0L))
                                    }.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            is LyricsUiState.Plain -> {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = lyrics.lyrics,
                        color = Color.White.copy(alpha = .86f),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is LyricsUiState.Loading -> {
                LockScreenPanelMessage(stringResource(R.string.lyrics_loading), Modifier.weight(1f).fillMaxWidth())
            }

            is LyricsUiState.Instrumental -> {
                LockScreenPanelMessage(stringResource(R.string.lock_screen_instrumental), Modifier.weight(1f).fillMaxWidth())
            }

            is LyricsUiState.Empty -> {
                LockScreenPanelMessage(stringResource(R.string.no_lyrics_found), Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LockScreenPanelHeader(
    title: String,
    onClose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_close_24dp), stringResource(R.string.close_action), tint = Color.White)
        }
    }
}

@Composable
private fun LockScreenPanelMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = muted, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

private fun handlePlayerGesture(
    playerViewModel: PlayerViewModel,
    gesture: GestureType,
) {
    when (gesture) {
        GestureType.Tap -> {
            playerViewModel.togglePlayPause()
        }

        GestureType.LongPress -> {
            Unit
        }

        is GestureType.DoubleTap -> {
            when (gesture.type) {
                GestureType.DoubleTap.TYPE_LEFT_EDGE -> playerViewModel.seekBack()
                GestureType.DoubleTap.TYPE_RIGHT_EDGE -> playerViewModel.seekForward()
                else -> playerViewModel.togglePlayPause()
            }
        }

        is GestureType.Fling -> {
            when (gesture.direction) {
                GestureType.Fling.DIRECTION_LEFT -> playerViewModel.seekToNext()
                GestureType.Fling.DIRECTION_RIGHT -> playerViewModel.seekToPrevious()
            }
        }
    }
}
