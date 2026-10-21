/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.screen.settings.backup

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ssilencce.R
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.backup.ScheduledBackupController
import com.ssilencce.util.backup.ScheduledBackupFailure
import com.ssilencce.util.backup.ScheduledBackupFrequency
import com.ssilencce.util.backup.ScheduledBackupLastResult
import java.text.DateFormat
import java.util.Date

class ScheduledBackupDialogFragment : BottomSheetDialogFragment() {
    private val controller by lazy { ScheduledBackupController(requireContext()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SsilencceTheme {
                    ScheduledBackupSheet(
                        controller = controller,
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    companion object {
        const val TAG = "ScheduledBackupDialog"
    }
}

@Composable
private fun ScheduledBackupSheet(
    controller: ScheduledBackupController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val failureMessage = stringResource(R.string.scheduled_backup_failure)
    val folderRequiredMessage = stringResource(R.string.scheduled_backup_folder_required)
    val state by controller.state.collectAsState()
    var enabled by remember { mutableStateOf(state.enabled) }
    var frequency by remember { mutableStateOf(state.frequency) }
    var chargingOnly by remember { mutableStateOf(state.chargingOnly) }
    var destination by remember { mutableStateOf(state.destination) }
    var destinationName by remember { mutableStateOf(state.destinationName) }
    var error by remember { mutableStateOf<String?>(null) }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            if (controller.rememberDestination(uri)) {
                destination = uri
                destinationName = resolveFolderName(context, uri)
                error = null
            } else {
                error = failureMessage
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.scheduled_backup_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = {
                            enabled = it
                            error = null
                        },
                    ).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.scheduled_backup_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.scheduled_backup_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.scheduled_backup_frequency_title),
            style = MaterialTheme.typography.labelLarge,
        )
        ScheduledBackupFrequency.entries.forEach { option ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            role = Role.RadioButton,
                        ).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = frequency == option, onClick = null)
                Text(
                    text = stringResource(option.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.scheduled_backup_folder_title),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = destinationName ?: stringResource(R.string.scheduled_backup_folder_none),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedButton(
            onClick = { folderPicker.launch(null) },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.scheduled_backup_choose_folder))
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = chargingOnly,
                        role = Role.Switch,
                        onValueChange = { chargingOnly = it },
                    ).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.scheduled_backup_charging_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.scheduled_backup_charging_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = chargingOnly,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        Spacer(Modifier.height(20.dp))
        LastRunStatus(state.lastRunAt, state.lastResult, state.lastError)

        error?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    error = null
                    when (controller.configure(enabled, frequency, chargingOnly, destination)) {
                        ScheduledBackupController.ConfigureResult.Success -> {
                            onDismiss()
                        }

                        ScheduledBackupController.ConfigureResult.MissingFolder -> {
                            error = folderRequiredMessage
                        }

                        ScheduledBackupController.ConfigureResult.PermissionDenied -> {
                            error = failureMessage
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun LastRunStatus(
    lastRunAt: Long,
    result: ScheduledBackupLastResult,
    lastError: String?,
) {
    if (lastRunAt == 0L) {
        Text(
            text = stringResource(R.string.scheduled_backup_never_run),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val formatted =
        remember(lastRunAt) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastRunAt))
        }
    Text(
        text = stringResource(R.string.scheduled_backup_last_run, formatted),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    when (result) {
        ScheduledBackupLastResult.Success -> {
            Text(
                text = stringResource(R.string.scheduled_backup_success),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        ScheduledBackupLastResult.Failure -> {
            Text(
                text = stringResource(R.string.scheduled_backup_failure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp),
            )
            ScheduledBackupFailure.fromStorageValue(lastError)?.let { failure ->
                Text(
                    text = stringResource(failure.messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        ScheduledBackupLastResult.None -> {
            Unit
        }
    }
}

private fun resolveFolderName(
    context: Context,
    uri: Uri,
): String =
    runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
        ?: uri.lastPathSegment
        ?: context.getString(R.string.scheduled_backup_folder_none)
