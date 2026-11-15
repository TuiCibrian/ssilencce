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

package com.ssilencce.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import java.io.File

@Composable
fun GhostSettingsScreen(
    enabled: Boolean,
    folders: List<String>,
    pinEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    var showPinDialog by rememberSaveable { mutableStateOf(false) }
    var showRemovePinDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GhostIntroCard()
        }
        item {
            GhostSwitchCard(
                enabled = enabled,
                onEnabledChange = onEnabledChange,
            )
        }
        item {
            GhostFoldersCard(
                folders = folders,
                onAddFolder = onAddFolder,
                onRemoveFolder = onRemoveFolder,
            )
        }
        item {
            GhostPinCard(
                pinEnabled = pinEnabled,
                onPinEnabledChange = { requested ->
                    if (requested) {
                        showPinDialog = true
                    } else {
                        showRemovePinDialog = true
                    }
                },
                onChangePin = { showPinDialog = true },
            )
        }
        item {
            FilledTonalButton(
                onClick = onOpenSearch,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_24dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.ghost_search_open))
            }
        }
        item {
            Text(
                text = stringResource(R.string.ghost_mode_explanation),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPinDialog) {
        GhostPinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                onSetPin(pin)
                showPinDialog = false
            },
        )
    }

    if (showRemovePinDialog) {
        AlertDialog(
            onDismissRequest = { showRemovePinDialog = false },
            title = { Text(stringResource(R.string.ghost_pin_remove_title)) },
            text = { Text(stringResource(R.string.ghost_pin_remove_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemovePin()
                        showRemovePinDialog = false
                    },
                ) {
                    Text(stringResource(R.string.remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun GhostIntroCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_visibility_off_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.ghost_mode_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.ghost_mode_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GhostSwitchCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.ghost_mode_enabled_title)) },
            supportingContent = { Text(stringResource(R.string.ghost_mode_enabled_summary)) },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GhostFoldersCard(
    folders: List<String>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ghost_folders_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            if (folders.isEmpty()) {
                                stringResource(R.string.ghost_folders_empty)
                            } else {
                                pluralStringResource(R.plurals.ghost_folder_count, folders.size, folders.size)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onAddFolder) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.ghost_add_folder))
                }
            }
            if (folders.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                folders.forEach { folder ->
                    GhostFolderRow(
                        folder = folder,
                        onRemove = { onRemoveFolder(folder) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GhostFolderRow(
    folder: String,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = File(folder).name.ifBlank { folder },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = folder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_folder_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24dp),
                    contentDescription = stringResource(R.string.ghost_remove_folder),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GhostPinCard(
    pinEnabled: Boolean,
    onPinEnabledChange: (Boolean) -> Unit,
    onChangePin: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.ghost_pin_title)) },
            supportingContent = { Text(stringResource(R.string.ghost_pin_summary)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = pinEnabled,
                    onCheckedChange = onPinEnabledChange,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (pinEnabled) {
            TextButton(
                onClick = onChangePin,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            ) {
                Text(stringResource(R.string.ghost_pin_change))
            }
        }
    }
}

@Composable
private fun GhostPinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(PinError.None) }
    val confirm: () -> Unit = {
        error =
            when {
                pin.length < GHOST_PIN_MIN_LENGTH -> {
                    PinError.TooShort
                }

                pin != confirmation -> {
                    PinError.Mismatch
                }

                else -> {
                    onConfirm(pin)
                    PinError.None
                }
            }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ghost_pin_set_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(GHOST_PIN_MAX_LENGTH)
                        error = PinError.None
                    },
                    label = { Text(stringResource(R.string.ghost_pin_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Next,
                        ),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = {
                        confirmation = it.filter(Char::isDigit).take(GHOST_PIN_MAX_LENGTH)
                        error = PinError.None
                    },
                    label = { Text(stringResource(R.string.ghost_pin_confirm_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                )
                if (error != PinError.None) {
                    Text(
                        text =
                            stringResource(
                                when (error) {
                                    PinError.TooShort -> R.string.ghost_pin_too_short
                                    PinError.Mismatch -> R.string.ghost_pin_mismatch
                                    PinError.None -> R.string.ghost_pin_label
                                },
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = confirm,
                enabled = pin.isNotEmpty() && confirmation.isNotEmpty(),
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

private enum class PinError {
    None,
    TooShort,
    Mismatch,
}

private const val GHOST_PIN_MIN_LENGTH = 4
private const val GHOST_PIN_MAX_LENGTH = 12
