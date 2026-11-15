@file:SuppressLint("LocalContextGetResourceValueCall")

package com.ssilencce.ui.screen.sleeptimer

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ssilencce.R
import com.ssilencce.playback.SleepTimerEndTime
import com.ssilencce.ui.component.compose.BottomSheetDialogSurface
import com.ssilencce.ui.component.compose.aurora.AuroraUnavailableRow
import com.ssilencce.ui.theme.SliderTokens
import com.ssilencce.util.Preferences
import kotlin.math.round

data class SleepTimerUiState(
    val isRunning: Boolean,
    val waitingFor: SleepTimerWaitingFor?,
    val isFinishMusic: Boolean,
    val isFadeOut: Boolean,
    val fadeOutDuration: Float,
    val timerValue: Float,
    val timerMode: SleepTimerMode,
    val endAtHour: Int,
    val endAtMinute: Int,
    val songCount: Int,
    val history: List<SleepTimerHistoryEntry>,
)

sealed class SleepTimerWaitingFor {
    class Countdown(
        val formattedTimeUntilFinish: String,
    ) : SleepTimerWaitingFor()

    object PendingQuit : SleepTimerWaitingFor()

    class SongCount(
        val songsRemaining: Int,
    ) : SleepTimerWaitingFor()
}

sealed class SleepTimerEvent {
    object Set : SleepTimerEvent()

    object Canceled : SleepTimerEvent()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SleepTimerBottomSheet(
    viewModel: SleepTimerViewModel,
) {
    val hapticFeedback = LocalHapticFeedback.current

    val uiState by viewModel.uiState.collectAsState()
    val sleepTimerEvent by viewModel.sleepTimerEvent.collectAsState(null)

    LaunchedEffect(sleepTimerEvent) {
        sleepTimerEvent?.let { event ->
            when (event) {
                is SleepTimerEvent.Canceled -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
                }

                is SleepTimerEvent.Set -> {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val animatedButtonContentColor by animateColorAsState(
        targetValue = if (uiState.isRunning) colors.onErrorContainer else colors.onPrimary,
        animationSpec = tween(300),
    )
    val animatedButtonColor by animateColorAsState(
        targetValue = if (uiState.isRunning) colors.errorContainer else colors.primary,
        animationSpec = tween(300),
    )
    val animatedButtonRadius by animateDpAsState(
        targetValue = if (uiState.isRunning) 16.dp else 50.dp,
        animationSpec = tween(300),
    )
    val animatedButtonPaddingHorizontal by animateDpAsState(
        targetValue = if (uiState.isRunning) 56.dp else 32.dp,
    )
    val animatedButtonPaddingVertical by animateDpAsState(
        targetValue = if (uiState.isRunning) 24.dp else 16.dp,
    )

    var sliderPosition by remember(uiState.timerValue) { mutableFloatStateOf(uiState.timerValue) }
    var fadeOutDuration by remember(uiState.fadeOutDuration) { mutableFloatStateOf(uiState.fadeOutDuration) }
    var selectedMode by remember(uiState.timerMode) { mutableStateOf(uiState.timerMode) }
    var endAtHour by remember(uiState.endAtHour) { mutableIntStateOf(uiState.endAtHour) }
    var endAtMinute by remember(uiState.endAtMinute) { mutableIntStateOf(uiState.endAtMinute) }
    var songCount by remember(uiState.songCount) { mutableIntStateOf(uiState.songCount) }
    var stopOnlyMusicInForeground by remember { mutableStateOf(Preferences.sleepTimerStopMusicOnlyInForeground) }
    var showTimePicker by remember { mutableStateOf(false) }

    BottomSheetDialogSurface {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Text(
                text = stringResource(R.string.action_sleep_timer),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                ) {
                    FilterChip(
                        selected = selectedMode == SleepTimerMode.Duration,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedMode = SleepTimerMode.Duration
                        },
                        enabled = uiState.isRunning.not(),
                        label = { Text(stringResource(R.string.sleep_timer_duration)) },
                    )
                    FilterChip(
                        selected = selectedMode == SleepTimerMode.EndAt,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedMode = SleepTimerMode.EndAt
                        },
                        enabled = uiState.isRunning.not(),
                        label = { Text(stringResource(R.string.sleep_timer_end_at)) },
                    )
                    FilterChip(
                        selected = selectedMode == SleepTimerMode.Songs,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedMode = SleepTimerMode.Songs
                        },
                        enabled = uiState.isRunning.not(),
                        label = { Text(stringResource(R.string.sleep_timer_songs)) },
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SleepTimerText(
                        text =
                            when (val waitingFor = uiState.waitingFor) {
                                is SleepTimerWaitingFor.Countdown -> {
                                    waitingFor.formattedTimeUntilFinish
                                }

                                is SleepTimerWaitingFor.PendingQuit -> {
                                    stringResource(R.string.sleep_timer_waiting_pending_quit)
                                }

                                is SleepTimerWaitingFor.SongCount -> {
                                    pluralStringResource(
                                        R.plurals.sleep_timer_songs_remaining,
                                        waitingFor.songsRemaining,
                                        waitingFor.songsRemaining,
                                    )
                                }

                                else -> {
                                    when (selectedMode) {
                                        SleepTimerMode.EndAt -> {
                                            stringResource(
                                                R.string.sleep_timer_ends_at,
                                                SleepTimerEndTime.format(endAtHour, endAtMinute),
                                            )
                                        }

                                        SleepTimerMode.Songs -> {
                                            pluralStringResource(R.plurals.x_songs, songCount, songCount)
                                        }

                                        SleepTimerMode.Duration -> {
                                            stringResource(R.string.sleep_timer_x_mins, sliderPosition.toInt())
                                        }
                                    }
                                }
                            },
                        modifier =
                            Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.End),
                    )

                    if (uiState.isRunning && uiState.timerMode == SleepTimerMode.EndAt) {
                        SleepTimerText(
                            text =
                                stringResource(
                                    R.string.sleep_timer_ends_at,
                                    SleepTimerEndTime.format(uiState.endAtHour, uiState.endAtMinute),
                                ),
                            modifier =
                                Modifier
                                    .padding(end = 8.dp)
                                    .align(Alignment.End),
                        )
                    }

                    if (selectedMode == SleepTimerMode.Duration) {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = round(it) },
                            onValueChangeFinished = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.SegmentFrequentTick,
                                )
                                viewModel.setTimerState(value = sliderPosition)
                            },
                            valueRange = 5f..180f,
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(SliderTokens.MediumTrackHeight),
                                )
                            },
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick,
                                    )
                                    viewModel.setTimerState(value = 15f)
                                },
                                shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                                contentPadding = PaddingValues(8.dp),
                                enabled = uiState.isRunning.not(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_timer_15_mins),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick,
                                    )
                                    viewModel.setTimerState(value = 45f)
                                },
                                shape = ShapeDefaults.Small,
                                contentPadding = PaddingValues(8.dp),
                                enabled = uiState.isRunning.not(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_timer_45_mins),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick,
                                    )
                                    viewModel.setTimerState(value = 60f)
                                },
                                shape = ShapeDefaults.Small,
                                contentPadding = PaddingValues(8.dp),
                                enabled = uiState.isRunning.not(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_timer_1_hour),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick,
                                    )
                                    viewModel.setTimerState(value = 120f)
                                },
                                shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                                contentPadding = PaddingValues(8.dp),
                                enabled = uiState.isRunning.not(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_timer_2_hour),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (selectedMode == SleepTimerMode.EndAt) {
                        OutlinedButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                showTimePicker = true
                            },
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.sleep_timer_end_at))
                            Spacer(Modifier.width(8.dp))
                            Text(SleepTimerEndTime.format(endAtHour, endAtMinute))
                        }
                    }
                    if (selectedMode == SleepTimerMode.Songs) {
                        Slider(
                            value = songCount.toFloat(),
                            onValueChange = { songCount = round(it).toInt().coerceIn(1, 20) },
                            onValueChangeFinished = {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.SegmentFrequentTick,
                                )
                                viewModel.setTimerState(songCount = songCount)
                            },
                            valueRange = 1f..20f,
                            steps = 18,
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(SliderTokens.MediumTrackHeight),
                                )
                            },
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                viewModel.startStopAfterThisSong()
                            },
                            enabled = uiState.isRunning.not(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stop_circle_24dp),
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sleep_timer_stop_after_this_song))
                        }
                    }
                }

                if (selectedMode != SleepTimerMode.Songs) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    ) {
                        SwitchButton(
                            enabled = uiState.isRunning.not(),
                            checked = uiState.isFinishMusic,
                            text = stringResource(R.string.sleep_timer_finish_current_music),
                            onValueChange = { isChecked ->
                                if (isChecked) {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ToggleOn,
                                    )
                                } else {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ToggleOff,
                                    )
                                }
                                viewModel.setTimerState(isFinishMusic = isChecked)
                            },
                        )

                        SwitchButton(
                            enabled = uiState.isRunning.not() && uiState.isFinishMusic.not(),
                            checked = uiState.isFadeOut,
                            text = stringResource(R.string.sleep_timer_music_fades_out),
                            onValueChange = { isChecked ->
                                if (isChecked) {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ToggleOn,
                                    )
                                } else {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ToggleOff,
                                    )
                                }
                                viewModel.setTimerState(isFadeOut = isChecked)
                            },
                            expandableContent = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 8.dp, bottom = 16.dp),
                                ) {
                                    Slider(
                                        value = fadeOutDuration,
                                        onValueChange = { fadeOutDuration = it },
                                        onValueChangeFinished = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.SegmentFrequentTick,
                                            )
                                            viewModel.setTimerState(fadeOutDuration = fadeOutDuration)
                                        },
                                        valueRange = 1f..10f,
                                        track = { sliderState ->
                                            SliderDefaults.Track(
                                                sliderState = sliderState,
                                                modifier = Modifier.height(SliderTokens.MediumTrackHeight),
                                            )
                                        },
                                        steps = 8,
                                        enabled = uiState.isRunning.not(),
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    SleepTimerText(
                                        text = stringResource(R.string.sleep_timer_x_secs, fadeOutDuration.toInt()),
                                        modifier =
                                            Modifier
                                                .padding(end = 8.dp)
                                                .align(Alignment.End),
                                    )
                                }
                            },
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                ) {
                    SwitchButton(
                        enabled = uiState.isRunning.not(),
                        checked = stopOnlyMusicInForeground,
                        text = stringResource(R.string.sleep_timer_stop_only_music_in_foreground_title),
                        onValueChange = { isChecked ->
                            if (isChecked) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOff)
                            }
                            stopOnlyMusicInForeground = isChecked
                            viewModel.setStopMusicOnlyInForeground(isChecked)
                        },
                    )
                }

                if (uiState.history.isNotEmpty()) {
                    SleepTimerHistory(
                        history = uiState.history,
                        enabled = uiState.isRunning.not(),
                        onReuse = viewModel::reuseHistory,
                        onClear = viewModel::clearHistory,
                    )
                }

                if (Preferences.isAuroraAppearance) AuroraUnavailableRow(R.string.aurora_song_timer)

                Button(
                    onClick = {
                        if (uiState.isRunning) {
                            viewModel.cancelTimer()
                        } else {
                            viewModel.startTimer(selectedMode, endAtHour, endAtMinute, songCount)
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = animatedButtonColor,
                            contentColor = animatedButtonContentColor,
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = animatedButtonPaddingHorizontal,
                            vertical = animatedButtonPaddingVertical,
                        ),
                    shape = RoundedCornerShape(animatedButtonRadius),
                ) {
                    if (uiState.isRunning) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24dp),
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    }
                    Text(
                        text =
                            if (uiState.isRunning) {
                                stringResource(R.string.sleep_timer_cancel_current_timer)
                            } else {
                                stringResource(R.string.sleep_timer_set_action)
                            },
                    )
                }
            }
        }

        if (showTimePicker) {
            EndAtTimePickerDialog(
                initialHour = endAtHour,
                initialMinute = endAtMinute,
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    endAtHour = hour
                    endAtMinute = minute
                    showTimePicker = false
                },
            )
        }
    }
}

@Composable
private fun SleepTimerHistory(
    history: List<SleepTimerHistoryEntry>,
    enabled: Boolean,
    onReuse: (SleepTimerHistoryEntry) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sleep_timer_recent),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onClear,
            ) {
                Text(stringResource(R.string.sleep_timer_clear_recent))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            history.forEach { entry ->
                AssistChip(
                    onClick = { onReuse(entry) },
                    enabled = enabled,
                    label = { Text(entry.historyLabel()) },
                )
            }
        }
    }
}

@Composable
private fun SleepTimerHistoryEntry.historyLabel(): String =
    when (this) {
        is SleepTimerHistoryEntry.Duration -> {
            stringResource(R.string.sleep_timer_x_mins, minutes)
        }

        is SleepTimerHistoryEntry.EndAt -> {
            stringResource(
                R.string.sleep_timer_ends_at,
                SleepTimerEndTime.format(hour, minute),
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndAtTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.sleep_timer_end_at),
                    style = MaterialTheme.typography.titleLarge,
                )
                TimeInput(state = timePickerState)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.sleep_timer_cancel_current_timer))
                    }
                    TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                        Text(stringResource(R.string.sleep_timer_set_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchButton(
    enabled: Boolean,
    checked: Boolean,
    text: String,
    onValueChange: (Boolean) -> Unit,
    expandableContent: @Composable () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        enabled = enabled,
                        value = checked,
                        role = Role.Switch,
                        onValueChange = onValueChange,
                    ).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                maxLines = 1,
                style = MaterialTheme.typography.labelLargeEmphasized,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = null,
            )
        }

        AnimatedVisibility(enabled && checked) {
            expandableContent()
        }
    }
}

@Composable
private fun SleepTimerText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}
