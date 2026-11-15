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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.ssilencce.core.model.player.NotificationButton
import com.ssilencce.core.model.player.NotificationButtonLayout
import com.ssilencce.core.model.player.NotificationButtonLayoutPolicy
import com.ssilencce.ui.component.compose.DialogListItemWithRadio
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.GhostMode
import com.ssilencce.util.Preferences

/**
 * Playback settings dialog for the compact and expanded notification button arrangement.
 * Changes persist immediately; Reset restores the previous default layout.
 */
class NotificationButtonLayoutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                    )
                    setContent {
                        SsilencceTheme {
                            NotificationButtonLayoutScreen(
                                onClose = { dismissAllowingStateLoss() },
                            )
                        }
                    }
                },
            ).create()
}

private enum class SlotSection {
    Compact,
    Expanded,
}

private data class SlotTarget(
    val section: SlotSection,
    val index: Int,
)

@Composable
private fun NotificationButtonLayoutScreen(onClose: () -> Unit) {
    var layout by remember { mutableStateOf(Preferences.notificationButtonLayout) }
    var editingSlot by remember { mutableStateOf<SlotTarget?>(null) }
    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.85f
        }

    fun updateLayout(updated: NotificationButtonLayout) {
        layout = NotificationButtonLayoutPolicy.normalize(updated)
        Preferences.notificationButtonLayout = layout
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
                text = stringResource(R.string.notification_button_layout_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = stringResource(R.string.notification_button_layout_compact_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.notification_button_layout_compact_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            repeat(NotificationButtonLayoutPolicy.MAX_COMPACT_BUTTONS) { index ->
                ButtonSlotRow(
                    label = stringResource(R.string.notification_button_slot, index + 1),
                    button = layout.compact.getOrNull(index),
                    onClick = { editingSlot = SlotTarget(SlotSection.Compact, index) },
                )
            }

            Text(
                text = stringResource(R.string.notification_button_layout_expanded_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.notification_button_layout_expanded_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            repeat(NotificationButtonLayoutPolicy.MAX_EXPANDED_BUTTONS) { index ->
                ButtonSlotRow(
                    label = stringResource(R.string.notification_button_slot, index + 1),
                    button = layout.expanded.getOrNull(index),
                    onClick = { editingSlot = SlotTarget(SlotSection.Expanded, index) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        Preferences.resetNotificationButtonLayout()
                        layout = Preferences.notificationButtonLayout
                    },
                ) {
                    Text(stringResource(R.string.notification_button_layout_reset))
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close_action))
                }
            }
        }
    }

    editingSlot?.let { slot ->
        val current =
            when (slot.section) {
                SlotSection.Compact -> layout.compact.getOrNull(slot.index)
                SlotSection.Expanded -> layout.expanded.getOrNull(slot.index)
            }
        ButtonPickerDialog(
            title = stringResource(R.string.notification_button_slot, slot.index + 1),
            selected = current,
            onSelect = { button ->
                val updated =
                    when (slot.section) {
                        SlotSection.Compact -> {
                            NotificationButtonLayoutPolicy.setCompactSlot(layout, slot.index, button)
                        }

                        SlotSection.Expanded -> {
                            NotificationButtonLayoutPolicy.setExpandedSlot(layout, slot.index, button)
                        }
                    }
                updateLayout(updated)
                editingSlot = null
            },
            onDismiss = { editingSlot = null },
        )
    }
}

@Composable
private fun ButtonSlotRow(
    label: String,
    button: NotificationButton?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text =
                if (button != null) {
                    stringResource(button.labelRes)
                } else {
                    stringResource(R.string.notification_button_empty)
                },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ButtonPickerDialog(
    title: String,
    selected: NotificationButton?,
    onSelect: (NotificationButton?) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.85f
        }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = maxHeight)
                        .padding(vertical = 16.dp)
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                DialogListItemWithRadio(
                    title = stringResource(R.string.notification_button_empty),
                    isSelected = selected == null,
                    onClick = { onSelect(null) },
                )
                NotificationButton.entries
                    .filter { it != NotificationButton.GhostSearch || GhostMode.isEnabled }
                    .forEach { button ->
                        DialogListItemWithRadio(
                            title = stringResource(button.labelRes),
                            isSelected = button == selected,
                            onClick = { onSelect(button) },
                        )
                    }
            }
        }
    }
}
