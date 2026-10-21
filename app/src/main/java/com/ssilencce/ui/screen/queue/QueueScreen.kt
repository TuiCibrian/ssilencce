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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.core.model.LibraryMargin
import com.ssilencce.data.local.room.NamedQueueSummary
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
import com.ssilencce.ui.component.compose.menu.MenuDefaults
import com.ssilencce.ui.component.compose.menu.MenuItem
import com.ssilencce.ui.component.compose.menu.OverflowMenu
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.screen.player.QUEUE_DEBOUNCE
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.util.ACTIVE_NAMED_QUEUE_ID
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

// Aurora queue captions render over the moving ribbon; the shared muted and
// secondary tokens miss 4.5:1 on its worst crest. Queue-scoped grays keep the
// hierarchy (artist brighter than album) without touching AuroraDesign, the
// same approach as the tab-label fix.
private val AuroraQueueCaptionColor = Color(0xFFD0D0D0)
private val AuroraQueueArtistColor = Color(0xFFDCDCDC)

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
    onCloseClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val coroutineScope = rememberCoroutineScope()

    val themeColors = MaterialTheme.colorScheme
    val aurora = Preferences.isAuroraAppearance
    val miniMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))
    val bottomPadding = with(LocalDensity.current) { miniMargin.totalMarginWithNavigation.toDp() }

    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val currentSong by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
    val nextSong by playerViewModel.nextSongFlow.collectAsStateWithLifecycle()
    val position by playerViewModel.positionFlow.collectAsStateWithLifecycle()
    val playQueue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val namedQueues by playerViewModel.namedQueuesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeNamedQueueId by playerViewModel.activeNamedQueueIdFlow.collectAsStateWithLifecycle()
    val repeatMode by playerViewModel.repeatModeFlow.collectAsStateWithLifecycle()
    val shuffleMode by playerViewModel.shuffleModeFlow.collectAsStateWithLifecycle()
    val queueDuration = remember(playQueue) { playQueue.songsDurationStr().toString() }

    val queueLocked by sharedPreferences.observeKeyAsState(LOCKED_QUEUE, false)

    var shouldLocateCurrentTrackOnUpdate by remember { mutableStateOf(false) }
    var showLocateCurrentTrack by remember { mutableStateOf(false) }
    var showQueuesManager by remember { mutableStateOf(false) }
    var queueQuery by remember { mutableStateOf("") }

    var reorderInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var reorderingQueue by remember { mutableStateOf<List<QueueSong>?>(null) }

    val listBottomPadding by animateDpAsState(
        targetValue = if (showLocateCurrentTrack) 96.dp else 16.dp,
        animationSpec = tween(200),
    )

    val visibleQueue =
        remember(playQueue, reorderingQueue, queueQuery) {
            val source = reorderingQueue ?: playQueue
            val indexed = source.withIndex()
            if (queueQuery.isBlank()) {
                indexed.toList()
            } else {
                indexed
                    .filter { (_, song) ->
                        song.title.contains(queueQuery, ignoreCase = true) ||
                            song.displayArtistName().contains(queueQuery, ignoreCase = true) ||
                            song.albumName.contains(queueQuery, ignoreCase = true)
                    }.toList()
            }
        }

    val queueContentOffset = if (aurora) 1 else 0
    val listState = rememberLazyListState(if (aurora) 0 else position.current.coerceAtLeast(0))
    val reorderableListState =
        rememberReorderableLazyListState(listState) { from, to ->
            if (queueQuery.isNotBlank()) return@rememberReorderableLazyListState
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

    LaunchedEffect(position, playQueue, queueQuery) {
        delay(QUEUE_DEBOUNCE.milliseconds)
        if (queueQuery.isBlank() && position.current in playQueue.indices && (
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
        BottomSheetDefaults.DragHandle(
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        if (playQueue.isNotEmpty()) {
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

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColors.surfaceContainerLowest.copy(alpha = .6f))
                    .padding(vertical = 12.dp)
                    .padding(start = 16.dp, end = 8.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_queue_music_24dp),
                tint = themeColors.secondary,
                contentDescription = null,
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.up_next),
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
                            icon = painterResource(R.drawable.ic_queue_music_24dp),
                            text = stringResource(R.string.aurora_queues_title),
                            onClick = { showQueuesManager = true },
                        ),
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
                    contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = listBottomPadding + 52.dp + if (aurora) 54.dp else 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (aurora) {
                        item(key = "aurora_queue_header") {
                            AuroraQueueHeader(
                                queueSize = playQueue.size,
                                queuePosition = position.current,
                                isPlaying = isPlaying,
                                nextTitle = nextSong.title.ifBlank { "\u2014" },
                                durationText = queueDuration,
                                queueLocked = queueLocked,
                                shuffleMode = shuffleMode,
                                repeatMode = repeatMode,
                                onCloseClick = onCloseClick,
                                onOpenQueuesManager = { showQueuesManager = true },
                                onTogglePlayPause = playerViewModel::togglePlayPause,
                                onToggleQueueLock = { checked ->
                                    toggleHapticFeedback(checked)
                                    // TODO migrate to DataStore Preferences
                                    sharedPreferences.edit {
                                        putBoolean(LOCKED_QUEUE, checked)
                                    }
                                },
                                onToggleShuffle = {
                                    toggleHapticFeedback(!shuffleMode)
                                    shouldLocateCurrentTrackOnUpdate = true
                                    playerViewModel.toggleShuffleMode()
                                },
                                onCycleRepeat = playerViewModel::cycleRepeatMode,
                                onAddToPlaylist = { onAddToPlaylistClick(playQueue) },
                                onClearQueue = playerViewModel::clearQueue,
                            )
                        }
                        if (playQueue.isEmpty()) {
                            item(key = "aurora_empty_queue") {
                                Text(stringResource(R.string.aurora_empty_queue), Modifier.padding(24.dp), color = AuroraQueueCaptionColor)
                            }
                        }
                    }
                    itemsIndexed(
                        items = visibleQueue,
                        key = { _, item -> item.value.key },
                    ) { _, item ->
                        val song = item.value
                        val index = item.index
                        ReorderableItem(reorderableListState, key = song.key) {
                            val isCurrentSong = index == position.current

                            val alpha by animateFloatAsState(
                                targetValue = if (isCurrentSong || index >= position.current) 1f else .5f,
                            )
                            val color by animateColorAsState(
                                targetValue =
                                    if (aurora) {
                                        if (isCurrentSong) Color.White.copy(alpha = .06f) else Color.Transparent
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
                                        if (isCurrentSong) 12.dp else 0.dp
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
                                        3.dp
                                    } else if (isPlaying && isCurrentSong) {
                                        50.dp
                                    } else {
                                        8.dp
                                    },
                                animationSpec = tween(400),
                            )

                            val dragHandleModifier =
                                Modifier
                                    .size(48.dp)
                                    .draggableHandle(
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
                                    )

                            Column(
                                Modifier.then(if (aurora) Modifier.alpha(alpha) else Modifier),
                            ) {
                                Surface(
                                    onClick = {
                                        if (isCurrentSong) {
                                            playerViewModel.togglePlayPause()
                                        } else {
                                            playerViewModel.playSongAt(index)
                                        }
                                    },
                                    shape = RoundedCornerShape(cornerRadius),
                                    color = if (aurora) color else color.copy(alpha = color.alpha * alpha),
                                    contentColor = if (aurora) themeColors.onSurface else contentColor.copy(alpha),
                                    border = if (aurora && isCurrentSong) BorderStroke(1.dp, Color.White.copy(alpha = .85f)) else null,
                                    modifier =
                                        if (aurora && isCurrentSong) {
                                            Modifier.padding(horizontal = 6.dp).padding(top = 6.dp, bottom = 4.dp)
                                        } else {
                                            Modifier
                                        },
                                ) {
                                    if (aurora) {
                                        // Reference row: handle, 46 dp cover, three-line meta with the
                                        // active lyrics glyph, duration and the compact overflow button.
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 6.dp),
                                        ) {
                                            Box(
                                                modifier =
                                                    if (queueLocked || queueQuery.isNotBlank()) {
                                                        Modifier.size(48.dp).semantics { disabled() }
                                                    } else {
                                                        dragHandleModifier
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_drag_handle_24dp),
                                                    contentDescription = stringResource(R.string.reorder_action),
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (queueLocked) AuroraDesign.muted.copy(alpha = .4f) else AuroraDesign.muted,
                                                )
                                            }

                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .border(1.dp, AuroraDesign.border, RoundedCornerShape(3.dp)),
                                            ) {
                                                MediaImage(model = song, modifier = Modifier.fillMaxSize())
                                            }

                                            Spacer(Modifier.width(9.dp))

                                            Column(Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Text(
                                                        text = song.title,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        fontSize = 14.5.sp,
                                                        fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Medium,
                                                        color = if (isCurrentSong) Color.White else AuroraDesign.foreground,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    if (isCurrentSong) {
                                                        Spacer(Modifier.width(6.dp))
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_lyrics_outline_24dp),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                            tint = Color.White,
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = song.displayArtistName(),
                                                    modifier = Modifier.padding(top = 1.dp),
                                                    fontSize = 12.5.sp,
                                                    color = AuroraQueueArtistColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )

                                                if (song.albumName.isNotBlank()) {
                                                    Text(
                                                        text = song.albumName,
                                                        fontSize = 12.5.sp,
                                                        color = AuroraQueueCaptionColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(6.dp))

                                            Text(
                                                text = song.duration.asReadableDuration(),
                                                fontSize = 12.5.sp,
                                                color = if (isCurrentSong) Color.White else AuroraQueueCaptionColor,
                                                maxLines = 1,
                                            )

                                            AuroraQueueItemMenu(
                                                itemIndex = index,
                                                queueIndex = position.current,
                                                isCurrentSong = isCurrentSong,
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
                                    } else {
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
                                                if (isPlayingSong) {
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
                                                    style = MaterialTheme.typography.titleMedium,
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1,
                                                )

                                                Text(
                                                    text = song.songInfo(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = contentColor.copy(alpha = 0.8f),
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1,
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            if (!queueLocked && queueQuery.isBlank()) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_drag_handle_24dp),
                                                    contentDescription = stringResource(R.string.reorder_action),
                                                    modifier = dragHandleModifier,
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
                                if (aurora && !isCurrentSong) {
                                    HorizontalDivider(color = AuroraDesign.border)
                                }
                            }
                        }
                    }
                    if (visibleQueue.isEmpty() && playQueue.isNotEmpty()) {
                        item(key = "queue_search_empty") {
                            Text(
                                text = stringResource(R.string.named_queue_search_empty),
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                color = themeColors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (playQueue.isNotEmpty()) {
                QueueSearchBar(
                    query = queueQuery,
                    onQueryChange = { queueQuery = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showLocateCurrentTrack && queueQuery.isBlank() && position.current in playQueue.indices,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier =
                    Modifier
                        .padding(bottom = if (aurora) 62.dp else 16.dp)
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

    if (showQueuesManager) {
        AuroraQueuesManagerDialog(
            queues = namedQueues,
            activeQueueId = activeNamedQueueId,
            onDismiss = { showQueuesManager = false },
            onSwitch = playerViewModel::switchToNamedQueue,
            onSaveCurrent = playerViewModel::saveCurrentQueueAs,
            onRename = playerViewModel::renameNamedQueue,
            onDelete = playerViewModel::deleteNamedQueue,
            onRemoveOthers = playerViewModel::deleteOtherNamedQueues,
        )
    }

    ObserveAsEvent(playerViewModel.namedQueueMessage) { messageRes ->
        context.showToast(context.getString(messageRes))
    }
}

// Reference .queue-head/.q-badge/.q-controls: queue selector, live state badge and
// the playback/queue controls. Every control is backed by real queue data or is
// explicitly disabled (save queue).
@Composable
private fun AuroraQueueHeader(
    queueSize: Int,
    queuePosition: Int,
    isPlaying: Boolean,
    nextTitle: String,
    durationText: String,
    queueLocked: Boolean,
    shuffleMode: Boolean,
    repeatMode: Int,
    onCloseClick: (() -> Unit)?,
    onOpenQueuesManager: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleQueueLock: (Boolean) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(AuroraDesign.glass)
                        .border(1.dp, AuroraDesign.border, RoundedCornerShape(11.dp))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.aurora_queues_title), onClick = onOpenQueuesManager)
                        .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.playing_queue_label),
                    modifier = Modifier.weight(1f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AuroraDesign.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_down_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = AuroraDesign.secondary,
                )
            }
            if (onCloseClick != null) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onCloseClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24dp),
                        contentDescription = stringResource(R.string.aurora_queue_close),
                        modifier = Modifier.size(22.dp),
                        tint = AuroraDesign.secondary,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = .06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
            ) {
                Text(
                    text = stringResource(if (isPlaying) R.string.aurora_queue_playing_state else R.string.aurora_queue_paused_state),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .04.em,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.aurora_queue_badge_sub, (queueSize - queuePosition - 1).coerceAtLeast(0), nextTitle),
                modifier = Modifier.weight(1f),
                fontSize = 12.5.sp,
                color = AuroraQueueCaptionColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onTogglePlayPause, enabled = queueSize > 0) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp),
                    contentDescription = stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }

            IconToggleButton(
                checked = queueLocked,
                onCheckedChange = onToggleQueueLock,
                colors =
                    IconButtonDefaults.iconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AuroraDesign.muted,
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = Color.White,
                    ),
            ) {
                Icon(
                    painter = painterResource(if (queueLocked) R.drawable.ic_lock_24dp else R.drawable.ic_lock_open_24dp),
                    contentDescription = stringResource(R.string.action_toggle_queue_lock),
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(queuePosition + 1).coerceIn(0, queueSize)} / $queueSize",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AuroraDesign.foreground,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_timer_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = AuroraQueueCaptionColor,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = durationText, fontSize = 12.sp, color = AuroraQueueCaptionColor)
                }
            }

            IconButton(onClick = onOpenQueuesManager, enabled = queueSize > 0) {
                Icon(
                    painter = painterResource(R.drawable.ic_save_24dp),
                    contentDescription = stringResource(R.string.aurora_queue_save),
                    modifier = Modifier.size(22.dp),
                )
            }

            OverflowMenu(
                items =
                    listOf(
                        MenuItem.Button.DropDown(
                            icon = painterResource(R.drawable.ic_shuffle_24dp),
                            text = stringResource(if (shuffleMode) R.string.shuffle_on else R.string.shuffle_off),
                            onClick = onToggleShuffle,
                        ),
                        MenuItem.Button.DropDown(
                            icon = painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one_24dp else R.drawable.ic_repeat_24dp),
                            text =
                                stringResource(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                        Player.REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                        else -> R.string.repeat_mode_off
                                    },
                                ),
                            onClick = onCycleRepeat,
                        ),
                        MenuItem.Divider,
                        MenuItem.Button.DropDown(
                            icon = painterResource(R.drawable.ic_playlist_add_24dp),
                            text = stringResource(R.string.action_add_to_playlist),
                            onClick = onAddToPlaylist,
                        ),
                        MenuItem.Button.DropDown(
                            icon = painterResource(R.drawable.ic_clear_all_24dp),
                            text = stringResource(R.string.clear_queue),
                            dangerous = true,
                            onClick = onClearQueue,
                        ),
                    ),
                colors = MenuDefaults.dropDownMenuColors(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }

        HorizontalDivider(color = AuroraDesign.border)
    }
}

// Pinned queue search. Filters the visible queue by title, artist and album while leaving the
// playback queue and its original item indices untouched.
@Composable
private fun QueueSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val aurora = Preferences.isAuroraAppearance
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (aurora) AuroraDesign.background else MaterialTheme.colorScheme.surface,
    ) {
        Column {
            HorizontalDivider(
                color = if (aurora) AuroraDesign.border else MaterialTheme.colorScheme.outlineVariant,
                thickness = if (aurora) 2.dp else 1.dp,
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.aurora_queue_search)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_24dp),
                                contentDescription = stringResource(R.string.action_cancel),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

// Real queues manager: saved queues are loaded from Room, the active one is marked, and
// save/rename/delete/switch are all backed by the named-queue repository.
@Composable
private fun AuroraQueuesManagerDialog(
    queues: List<NamedQueueSummary>,
    activeQueueId: String?,
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit,
    onSaveCurrent: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onRemoveOthers: () -> Unit,
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<NamedQueueSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<NamedQueueSummary?>(null) }
    var showRemoveOthers by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth(.92f)
                        .widthIn(max = 380.dp)
                        .padding(top = 64.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                shape = RoundedCornerShape(18.dp),
                color = AuroraDesign.glassStrong,
                border = BorderStroke(1.dp, AuroraDesign.border),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_queue_music_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = AuroraDesign.foreground,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.aurora_queues_title),
                            modifier = Modifier.weight(1f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (queues.isEmpty()) {
                        Text(
                            text = stringResource(R.string.named_queue_none),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            fontSize = 13.5.sp,
                            color = AuroraDesign.muted,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                            items(queues, key = { it.id }) { queue ->
                                NamedQueueRow(
                                    queue = queue,
                                    active = queue.id == activeQueueId,
                                    onSwitch = { onSwitch(queue.id) },
                                    onRename = { renameTarget = queue },
                                    onDelete = { deleteTarget = queue },
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = AuroraDesign.border)

                    TextButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.named_queue_save_current), fontWeight = FontWeight.SemiBold)
                    }

                    if (queues.size > 1) {
                        TextButton(
                            onClick = { showRemoveOthers = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.named_queue_remove_others_title),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = .04.em,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        QueueNameDialog(
            title = stringResource(R.string.named_queue_save_title),
            initialName = "",
            onConfirm = {
                onSaveCurrent(it)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    renameTarget?.let { queue ->
        QueueNameDialog(
            title = stringResource(R.string.named_queue_rename_title),
            initialName = queue.name,
            onConfirm = { name ->
                onRename(queue.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { queue ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.named_queue_delete_title)) },
            text = { Text(stringResource(R.string.named_queue_delete_message, queue.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(queue.id)
                        deleteTarget = null
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showRemoveOthers) {
        AlertDialog(
            onDismissRequest = { showRemoveOthers = false },
            title = { Text(stringResource(R.string.named_queue_remove_others_title)) },
            text = { Text(stringResource(R.string.named_queue_remove_others_message, (queues.size - 1).coerceAtLeast(0))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveOthers()
                        showRemoveOthers = false
                    },
                ) {
                    Text(stringResource(R.string.remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveOthers = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NamedQueueRow(
    queue: NamedQueueSummary,
    active: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onSwitch)
                .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (active) Color.White else AuroraDesign.muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = queue.name,
                fontSize = 15.5.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    buildString {
                        append(stringResource(R.string.named_queue_items, queue.itemCount))
                        if (active) {
                            append(" · ")
                            append(stringResource(R.string.named_queue_active))
                        }
                    },
                fontSize = 12.sp,
                color = AuroraDesign.muted,
            )
        }
        IconButton(onClick = onRename) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_24dp),
                contentDescription = stringResource(R.string.named_queue_rename),
                modifier = Modifier.size(19.dp),
                tint = AuroraDesign.muted,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.named_queue_remove),
                modifier = Modifier.size(19.dp),
                tint = AuroraDesign.muted,
            )
        }
    }
}

@Composable
private fun QueueNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(stringResource(R.string.named_queue_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

// Compact reference overflow trigger (30 dp visual inside a 48 dp target); the
// menu items are the same real per-song actions as the dialog variant.
@Composable
private fun AuroraQueueItemMenu(
    itemIndex: Int,
    queueIndex: Int,
    isCurrentSong: Boolean,
    onStopAfterThisTrackClick: () -> Unit,
    onPutAfterCurrentTrackClick: () -> Unit,
    onRemoveFromQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteFromDeviceClick: () -> Unit,
    onDetailsClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = { expanded = true }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCurrentSong) Color.White.copy(alpha = .10f) else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(1.dp, if (isCurrentSong) Color.White.copy(alpha = .30f) else AuroraDesign.border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_horiz_24dp),
                contentDescription = stringResource(R.string.action_more),
                modifier = Modifier.size(16.dp),
                tint = if (isCurrentSong) Color.White else AuroraDesign.muted,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val items =
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
                )
            items.forEach { item ->
                if (item is MenuItem.Button.DropDown) {
                    DropdownMenuItem(
                        text = { Text(item.text) },
                        leadingIcon = item.icon?.let { icon -> @Composable { Icon(painter = icon, contentDescription = null) } },
                        onClick = {
                            item.onClick()
                            expanded = false
                        },
                        enabled = item.enabled,
                    )
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
