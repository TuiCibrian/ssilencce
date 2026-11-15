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

package com.ssilencce.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.core.model.action.HeadsetButtonAction
import com.ssilencce.playback.HeadsetButtonPolicy
import com.ssilencce.ui.component.compose.DialogListItemWithRadio
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences

/**
 * Playback -> Headphones mapping dialog. Changes persist immediately; Reset restores the
 * legacy defaults.
 */
class HeadsetButtonMappingDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                    )
                    setContent {
                        SsilencceTheme {
                            HeadsetButtonMappingScreen(
                                onClose = { dismissAllowingStateLoss() },
                            )
                        }
                    }
                },
            ).create()
}

private enum class HeadsetGesture(
    @StringRes val labelRes: Int,
) {
    Single(R.string.headset_single_press),
    Double(R.string.headset_double_press),
    Triple(R.string.headset_triple_press),
    Long(R.string.headset_long_press),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeadsetButtonMappingScreen(onClose: () -> Unit) {
    var single by remember { mutableStateOf(Preferences.headsetSingleClickAction) }
    var double by remember { mutableStateOf(Preferences.headsetDoubleClickAction) }
    var triple by remember { mutableStateOf(Preferences.headsetTripleClickAction) }
    var longPress by remember { mutableStateOf(Preferences.headsetLongPressAction) }
    var fourPlus by remember { mutableStateOf(Preferences.headsetFourPlusSeekForward) }
    var delay by remember { mutableIntStateOf(Preferences.headsetClickDelayMs) }
    var editingGesture by remember { mutableStateOf<HeadsetGesture?>(null) }

    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.85f
        }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.headset_button_mapping_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.headset_mapping_disclaimer),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            GestureRow(
                gesture = HeadsetGesture.Single,
                action = single,
                onClick = { editingGesture = HeadsetGesture.Single },
            )
            GestureRow(
                gesture = HeadsetGesture.Double,
                action = double,
                onClick = { editingGesture = HeadsetGesture.Double },
            )
            GestureRow(
                gesture = HeadsetGesture.Triple,
                action = triple,
                onClick = { editingGesture = HeadsetGesture.Triple },
            )
            GestureRow(
                gesture = HeadsetGesture.Long,
                action = longPress,
                onClick = { editingGesture = HeadsetGesture.Long },
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = fourPlus,
                            role = Role.Switch,
                            onValueChange = {
                                fourPlus = it
                                Preferences.headsetFourPlusSeekForward = it
                            },
                        ).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.headset_four_plus_seek_forward_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.headset_four_plus_seek_forward_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = fourPlus,
                    onCheckedChange = null,
                )
            }

            Text(
                text = stringResource(R.string.headset_click_delay_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.headset_click_delay_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeadsetButtonPolicy.DELAY_VALUES.forEach { value ->
                    FilterChip(
                        selected = delay == value,
                        onClick = {
                            delay = value
                            Preferences.headsetClickDelayMs = value
                        },
                        label = { Text(stringResource(R.string.headset_click_delay_value, value)) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        Preferences.resetHeadsetButtonMapping()
                        single = Preferences.headsetSingleClickAction
                        double = Preferences.headsetDoubleClickAction
                        triple = Preferences.headsetTripleClickAction
                        longPress = Preferences.headsetLongPressAction
                        fourPlus = Preferences.headsetFourPlusSeekForward
                        delay = Preferences.headsetClickDelayMs
                    },
                ) {
                    Text(stringResource(R.string.headset_mapping_reset))
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close_action))
                }
            }
        }
    }

    editingGesture?.let { gesture ->
        val selected =
            when (gesture) {
                HeadsetGesture.Single -> single
                HeadsetGesture.Double -> double
                HeadsetGesture.Triple -> triple
                HeadsetGesture.Long -> longPress
            }
        ActionPickerDialog(
            gesture = gesture,
            selected = selected,
            onSelect = { action ->
                when (gesture) {
                    HeadsetGesture.Single -> {
                        single = action
                        Preferences.headsetSingleClickAction = action
                    }

                    HeadsetGesture.Double -> {
                        double = action
                        Preferences.headsetDoubleClickAction = action
                    }

                    HeadsetGesture.Triple -> {
                        triple = action
                        Preferences.headsetTripleClickAction = action
                    }

                    HeadsetGesture.Long -> {
                        longPress = action
                        Preferences.headsetLongPressAction = action
                    }
                }
                editingGesture = null
            },
            onDismiss = { editingGesture = null },
        )
    }
}

@Composable
private fun GestureRow(
    gesture: HeadsetGesture,
    action: HeadsetButtonAction,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(gesture.labelRes),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(action.titleRes),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActionPickerDialog(
    gesture: HeadsetGesture,
    selected: HeadsetButtonAction,
    onSelect: (HeadsetButtonAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .selectableGroup(),
            ) {
                Text(
                    text = stringResource(gesture.labelRes),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                HeadsetButtonAction.entries.forEach { action ->
                    DialogListItemWithRadio(
                        title = stringResource(action.titleRes),
                        isSelected = action == selected,
                        onClick = { onSelect(action) },
                    )
                }
            }
        }
    }
}

@get:StringRes
val HeadsetButtonAction.titleRes: Int
    get() =
        when (this) {
            HeadsetButtonAction.PlayPause -> R.string.action_play_pause
            HeadsetButtonAction.Next -> R.string.action_next
            HeadsetButtonAction.Previous -> R.string.action_previous
            HeadsetButtonAction.SeekForward -> R.string.action_seek_forward
            HeadsetButtonAction.SeekBackward -> R.string.action_seek_backward
            HeadsetButtonAction.Nothing -> R.string.label_nothing
        }
