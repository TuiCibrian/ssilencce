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

package com.ssilencce.ui.screen.queue

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.core.model.LibraryMargin
import com.ssilencce.data.model.QueueSong
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.media.displayArtistName
import com.ssilencce.extensions.media.songCountStr
import com.ssilencce.extensions.media.songInfo
import com.ssilencce.extensions.media.songsDurationStr
import com.ssilencce.extensions.observeKeyAsState
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.utilities.buildInfoString
import com.ssilencce.ui.component.compose.AnimatedEqBars
import com.ssilencce.ui.component.compose.MediaImage
import com.ssilencce.ui.component.compose.ObserveAsEvent
import com.ssilencce.ui.component.compose.SmallHeader
import com.ssilencce.ui.component.compose.aurora.AuroraUnavailableRow
import com.ssilencce.ui.component.compose.aurora.rememberAuroraMotionScale
import com.ssilencce.ui.component.compose.menu.MenuDefaults
import com.ssilencce.ui.component.compose.menu.MenuItem
import com.ssilencce.ui.component.compose.menu.OverflowMenu
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.screen.player.QUEUE_DEBOUNCE
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.util.ANIMATE_PLAYER_CONTROL
import com.ssilencce.util.LOCKED_QUEUE
import com.ssilencce.util.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.viewmodel.koinActivityViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onAddToPlaylistClick: (List<Song>) -> Unit,
    onDeleteSongClick: (Song) -> Unit,
    onShareClick: (Song) -> Unit,
    onDetailsClick: (Song) -> Unit,
    playerViewModel: PlayerViewModel = koinActivityViewModel(),
    libraryViewModel: LibraryViewModel = koinActivityViewModel(),
    contentVisible: Boolean = true,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val coroutineScope = rememberCoroutineScope()

    val themeColors = MaterialTheme.colorScheme
    val aurora = Preferences.isAuroraAppearance
    val animateBars = !aurora || rememberAuroraMotionScale(contentVisible, ANIMATE_PLAYER_CONTROL) > 0f
    val miniMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))
    val bottomPadding = with(LocalDensity.current) { miniMargin.totalMarginWithNavigation.toDp() }

    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val position by playerViewModel.positionFlow.collectAsStateWithLifecycle()
    val playQueue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffleMode by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()

    val queueLocked by sharedPreferences.observeKeyAsState(LOCKED_QUEUE, false)

    var shouldLocateCurrentTrackOnUpdate by remember { mutableStateOf(false) }
    var showLocateCurrentTrack by remember { mutableStateOf(false) }

    var reorderInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var reorderingQueue by remember { mutableStateOf<List<QueueSong>?>(null) }

    val listBottomPadding by animateDpAsState(
        targetValue = if (showLocateCurrentTrack) 96.dp else 16.dp,
        animationSpec = tween(200),
    )

    val queueContentOffset = if (aurora) 1 else 0
    val listState = rememberLazyListState(if (aurora) 0 else position.current.coerceAtLeast(0))
    val reorderableListState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromSong = from.index - queueContentOffset
            val toSong = to.index - queueContentOffset
            if (fromSong !in playQueue.indices || toSong !in playQueue.indices) return@rememberReorderableLazyListState
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)

            reorderInfo =
                if (reorderInfo == null) {
                    Pair(fromSong, toSong)
                } else {
                    reorderInfo!!.first to toSong
                }

            reorderingQueue =
                reorderingQueue?.toMutableList()?.apply {
                    add(toSong, removeAt(fromSong))
                }
        }

    ObserveAsEvent(playerViewModel.stopAfterPosition) { (title, canceled) ->
        if (title != null) {
            if (canceled) {
                context.showToast(context.getString(R.string.sleep_timer_stop_after_x_canceled, title))
            } else {
                context.showToast(context.getString(R.string.sleep_timer_stop_after_x, title))
            }
        }
    }

    fun toggleHapticFeedback(checked: Boolean) {
        if (checked) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
        } else {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
        }
    }

    LaunchedEffect(playQueue) {
        delay(QUEUE_DEBOUNCE.milliseconds)
        reorderingQueue = null
    }

    LaunchedEffect(position, playQueue) {
        delay(QUEUE_DEBOUNCE.milliseconds)
        if (position.current in playQueue.indices && (
                shouldLocateCurrentTrackOnUpdate ||
                    (position.previous in playQueue.indices && listState.firstVisibleItemIndex == position.previous + queueContentOffset)
            )
        ) {
            listState.animateScrollToItem(position.current + queueContentOffset)
            if (shouldLocateCurrentTrackOnUpdate) {
                shouldLocateCurrentTrackOnUpdate = false
            }
        }
    }

    LaunchedEffect(listState, position.current, playQueue.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { itemInfos ->
                showLocateCurrentTrack = position.current in playQueue.indices && itemInfos.none { it.index == position.current + queueContentOffset }
            }
    }

    val header: @Composable ColumnScope.() -> Unit = {
        if (aurora) {
            Text(stringResource(R.string.playing_queue_label), Modifier.padding(horizontal = 18.dp, vertical = 16.dp), fontSize = 19.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.aurora_multiple_queues) + " · " + stringResource(R.string.aurora_future_title), Modifier.padding(horizontal = 18.dp, vertical = 4.dp), fontSize = 12.sp, color = AuroraDesign.muted)
        } else {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        if (!aurora && playQueue.isNotEmpty()) {
            SmallHeader(
                title = currentSong.title,
                subtitle = currentSong.displayArtistName(),
                imageModel = currentSong,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(if (aurora) 4.dp else 16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
        ) {
            TonalToggleButton(
                checked = shuffleMode,
                onCheckedChange = { checked ->
                    toggleHapticFeedback(checked)
                    shouldLocateCurrentTrackOnUpdate = true
                    playerViewModel.toggleShuffleMode()
                },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shuffle_24dp),
                    contentDescription = stringResource(R.string.action_toggle_shuffle),
                )
            }

            TonalToggleButton(
                checked = repeatMode != Player.REPEAT_MODE_OFF,
                onCheckedChange = { checked ->
                    toggleHapticFeedback(checked)
                    playerViewModel.cycleRepeatMode()
                },
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter =
                        when (repeatMode) {
                            Player.REPEAT_MODE_ALL -> painterResource(R.drawable.ic_repeat_24dp)
                            Player.REPEAT_MODE_ONE -> painterResource(R.drawable.ic_repeat_one_24dp)
                            else -> painterResource(R.drawable.ic_repeat_24dp)
                        },
                    contentDescription = stringResource(R.string.action_cycle_repeat),
                )
            }

            TonalToggleButton(
                checked = queueLocked,
                onCheckedChange = { checked ->
                    toggleHapticFeedback(checked)
                    // TODO migrate to DataStore Preferences
                    sharedPreferences.edit {
                        putBoolean(LOCKED_QUEUE, checked)
                    }
                },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    painter =
                        if (queueLocked) {
                            painterResource(R.drawable.ic_lock_24dp)
                        } else {
                            painterResource(R.drawable.ic_lock_open_24dp)
                        },
                    contentDescription = stringResource(R.string.action_toggle_queue_lock),
                )
            }
        }

        Spacer(Modifier.height(if (aurora) 4.dp else 16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (aurora) 0.dp else 16.dp),
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColors.surfaceContainerLowest.copy(alpha = .6f))
                    .padding(vertical = if (aurora) 4.dp else 12.dp)
                    .padding(start = if (aurora) 0.dp else 16.dp, end = 8.dp),
        ) {
            if (aurora) {
                IconButton(onClick = playerViewModel::seekToPrevious, enabled = playQueue.isNotEmpty()) {
                    Icon(painterResource(R.drawable.ic_previous_24dp), stringResource(R.string.action_previous))
                }
                IconButton(onClick = playerViewModel::togglePlayPause, enabled = playQueue.isNotEmpty()) {
                    Icon(painterResource(if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp), stringResource(if (isPlaying) R.string.action_pause else R.string.action_play))
                }
                IconButton(onClick = playerViewModel::seekToNext, enabled = playQueue.isNotEmpty()) {
                    Icon(painterResource(R.drawable.ic_next_24dp), stringResource(R.string.action_next))
                }
            } else {
                Icon(
                    painterResource(R.drawable.ic_queue_music_24dp),
                    tint = themeColors.secondary,
                    contentDescription = null,
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (aurora) "${(position.current + 1).coerceIn(0, playQueue.size)} / ${playQueue.size}" else stringResource(R.string.up_next),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeColors.secondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )

                Text(
                    text =
                        buildInfoString(
                            playQueue.songCountStr(context),
                            playQueue.songsDurationStr(),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant,
                )
            }

            OverflowMenu(
                items =
                    listOf(
                        MenuItem.Button.DropDown(
                            icon = painterResource(R.drawable.ic_playlist_add_24dp),
                            text = stringResource(R.string.action_add_to_playlist),
                            onClick = { onAddToPlaylistClick(playQueue) },
                        ),
                        MenuItem.Button.DropDown(
                            icon = painterResource(R.drawable.ic_clear_all_24dp),
                            text = stringResource(R.string.clear_queue),
                            dangerous = true,
                            onClick = { playerViewModel.clearQueue() },
                        ),
                    ),
                colors = MenuDefaults.dropDownMenuColors(themeColors.surfaceContainerHigh),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (aurora) {
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(bottom = bottomPadding)
                    } else {
                        Modifier.wrapContentHeight()
                    },
                ).nestedScroll(rememberNestedScrollInteropConnection()),
    ) {
        if (!aurora) header()
        Box(if (aurora) Modifier.weight(1f) else Modifier) {
            if (!aurora && playQueue.isEmpty()) {
                Text(stringResource(R.string.aurora_empty_queue), Modifier.align(Alignment.Center).padding(24.dp), color = themeColors.onSurfaceVariant)
            }
            LazyColumnScrollbar(
                state = listState,
                settings =
                    ScrollbarSettings(
                        hideDelayMillis = 3000,
                        thumbMaxLength = 0.1f,
                        thumbSelectedColor = themeColors.primary,
                        thumbUnselectedColor = themeColors.primary,
                    ),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = listBottomPadding),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (aurora) {
                        item(key = "aurora_queue_header") { Column { header() } }
                        if (playQueue.isEmpty()) {
                            item(key = "aurora_empty_queue") {
                                Text(stringResource(R.string.aurora_empty_queue), Modifier.padding(24.dp), color = themeColors.onSurfaceVariant)
                            }
                        }
                    }
                    itemsIndexed(
                        items = reorderingQueue ?: playQueue,
                        key = { _, item -> item.key },
                    ) { index, song ->
                        ReorderableItem(reorderableListState, key = song.key) {
                            val isCurrentSong = index == position.current

                            val alpha by animateFloatAsState(
                                targetValue = if (isCurrentSong || index >= position.current) 1f else .5f,
                            )
                            val color by animateColorAsState(
                                targetValue =
                                    if (aurora) {
                                        if (isCurrentSong) Color.White.copy(alpha = .06f) else AuroraDesign.glass
                                    } else if (isCurrentSong) {
                                        themeColors.primaryContainer
                                    } else {
                                        themeColors.surfaceContainer
                                    },
                                animationSpec = tween(400),
                            )
                            val contentColor by animateColorAsState(
                                targetValue =
                                    if (isCurrentSong) {
                                        themeColors.onPrimaryContainer
                                    } else {
                                        themeColors.onSurfaceVariant
                                    },
                                animationSpec = tween(400),
                            )
                            val cornerRadius by animateDpAsState(
                                targetValue =
                                    if (aurora) {
                                        6.dp
                                    } else if (isPlaying && isCurrentSong) {
                                        50.dp
                                    } else {
                                        12.dp
                                    },
                                animationSpec = tween(400),
                            )
                            val imageCornerRadius by animateDpAsState(
                                targetValue =
                                    if (aurora) {
                                        4.dp
                                    } else if (isPlaying && isCurrentSong) {
                                        50.dp
                                    } else {
                                        8.dp
                                    },
                                animationSpec = tween(400),
                            )

                            Surface(
                                onClick = {
                                    if (isCurrentSong) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSongAt(index)
                                    }
                                },
                                shape = RoundedCornerShape(cornerRadius),
                                color = color.copy(alpha = color.alpha * alpha),
                                contentColor = contentColor.copy(alpha),
                                border = if (aurora && isCurrentSong) BorderStroke(1.dp, Color.White) else null,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                ) {
                                    Crossfade(
                                        targetState = isPlaying && isCurrentSong,
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(imageCornerRadius)),
                                    ) { isPlayingSong ->
                                        if (aurora) {
                                            Box(Modifier.fillMaxSize()) {
                                                MediaImage(model = song, modifier = Modifier.fillMaxSize())
                                                if (isPlayingSong) {
                                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                                                        AnimatedEqBars(color = Color.White, isPlaying = isPlaying, modifier = Modifier.size(16.dp), animate = animateBars)
                                                    }
                                                }
                                            }
                                        } else if (isPlayingSong) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .background(themeColors.primary),
                                            ) {
                                                AnimatedEqBars(
                                                    color = themeColors.onPrimary,
                                                    isPlaying = isPlaying,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        } else {
                                            MediaImage(
                                                model = song,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = song.title,
                                            style = if (aurora) MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp) else MaterialTheme.typography.titleMedium,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                        )

                                        Text(
                                            text = if (aurora) song.displayArtistName() else song.songInfo(),
                                            style = if (aurora) MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp) else MaterialTheme.typography.bodyMedium,
                                            color = contentColor.copy(alpha = 0.8f),
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 1,
                                        )
                                        if (aurora) Text(song.albumName, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp), color = AuroraDesign.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    if (aurora) Text(song.duration.asReadableDuration(), color = AuroraDesign.muted, fontSize = 12.5.sp)

                                    if (!queueLocked) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_drag_handle_24dp),
                                            contentDescription = stringResource(R.string.reorder_action),
                                            modifier =
                                                Modifier.size(48.dp).draggableHandle(
                                                    onDragStarted = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.GestureThresholdActivate,
                                                        )
                                                        reorderInfo = null
                                                        reorderingQueue = playQueue
                                                    },
                                                    onDragStopped = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.GestureEnd,
                                                        )
                                                        reorderInfo?.let { (from, to) ->
                                                            playerViewModel.moveSong(from, to)
                                                        }
                                                    },
                                                ),
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    QueueItemMenu(
                                        itemIndex = index,
                                        queueIndex = position.current,
                                        onStopAfterThisTrackClick = {
                                            playerViewModel.stopAt(index)
                                        },
                                        onPutAfterCurrentTrackClick = {
                                            playerViewModel.moveToNextPosition(index)
                                        },
                                        onRemoveFromQueueClick = {
                                            playerViewModel.removePosition(index)
                                        },
                                        onAddToPlaylistClick = {
                                            onAddToPlaylistClick(listOf(song))
                                        },
                                        onShareClick = {
                                            onShareClick(song)
                                        },
                                        onDeleteFromDeviceClick = {
                                            onDeleteSongClick(song)
                                        },
                                        onDetailsClick = {
                                            onDetailsClick(song)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (aurora) item(key = "aurora_queue_search") { AuroraUnavailableRow(R.string.aurora_queue_search) }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showLocateCurrentTrack && position.current in playQueue.indices,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier =
                    Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.BottomCenter),
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(position.current + queueContentOffset)
                        }
                    },
                    contentPadding = ButtonDefaults.MediumContentPadding,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recenter_24dp),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.go_to_current_track))
                }
            }
        }
    }
}

@Composable
private fun QueueItemMenu(
    itemIndex: Int,
    queueIndex: Int,
    onStopAfterThisTrackClick: () -> Unit,
    onPutAfterCurrentTrackClick: () -> Unit,
    onRemoveFromQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteFromDeviceClick: () -> Unit,
    onDetailsClick: () -> Unit,
) {
    OverflowMenu(
        items =
            listOf(
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_stop_circle_24dp),
                    text = stringResource(R.string.sleep_timer_stop_after_this_track),
                    enabled = itemIndex >= queueIndex,
                    onClick = onStopAfterThisTrackClick,
                ),
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_queue_play_next_24dp),
                    text = stringResource(R.string.put_after_current_track),
                    enabled = itemIndex > (queueIndex + 1),
                    onClick = onPutAfterCurrentTrackClick,
                ),
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_remove_from_queue_24dp),
                    text = stringResource(R.string.action_remove_from_playing_queue),
                    onClick = onRemoveFromQueueClick,
                ),
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_playlist_add_24dp),
                    text = stringResource(R.string.action_add_to_playlist),
                    onClick = onAddToPlaylistClick,
                ),
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_share_24dp),
                    text = stringResource(R.string.action_share),
                    onClick = onShareClick,
                ),
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_delete_24dp),
                    text = stringResource(R.string.action_delete_from_device),
                    onClick = onDeleteFromDeviceClick,
                ),
                MenuItem.Button.DropDown(
                    icon = painterResource(R.drawable.ic_info_24dp),
                    text = stringResource(R.string.action_details),
                    onClick = onDetailsClick,
                ),
            ),
        colors =
            MenuDefaults.dropDownMenuColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
    )
}
