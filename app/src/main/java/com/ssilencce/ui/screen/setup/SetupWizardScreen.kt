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

package com.ssilencce.ui.screen.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import java.io.File

private enum class FolderPickerTarget {
    Main,
    Include,
    Exclude,
}

@Composable
fun SetupWizardScreen(
    state: SetupWizardUiState,
    isFirstRun: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onSetMainFolder: (String) -> Unit,
    onResetMainFolder: () -> Unit,
    onAddInclude: (String) -> Unit,
    onRemoveInclude: (String) -> Unit,
    onAddExclude: (String) -> Unit,
    onRemoveExclude: (String) -> Unit,
    onAutoRescanChange: (Boolean) -> Unit,
    onRescan: () -> Unit,
) {
    var pickerTarget by rememberSaveable { mutableStateOf<FolderPickerTarget?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp),
    ) {
        WizardHeader(
            step = state.step,
            isFirstRun = isFirstRun,
            saving = state.saving,
            onSkip = onSkip,
        )
        StepIndicator(currentStep = state.step)

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                state.step == SetupWizardStep.Welcome -> {
                    WelcomeStep()
                }

                state.step == SetupWizardStep.Folders -> {
                    FoldersStep(
                        mainFolder = state.mainFolder,
                        includeFolders = state.includeFolders,
                        excludeFolders = state.excludeFolders,
                        onChangeMainFolder = { pickerTarget = FolderPickerTarget.Main },
                        onResetMainFolder = onResetMainFolder,
                        onAddInclude = { pickerTarget = FolderPickerTarget.Include },
                        onRemoveInclude = onRemoveInclude,
                        onAddExclude = { pickerTarget = FolderPickerTarget.Exclude },
                        onRemoveExclude = onRemoveExclude,
                    )
                }

                state.step == SetupWizardStep.Scanning -> {
                    ScanningStep(
                        autoRescan = state.autoRescan,
                        onAutoRescanChange = onAutoRescanChange,
                    )
                }

                else -> {
                    FinishStep(
                        state = state,
                        onRescan = onRescan,
                    )
                }
            }
        }

        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.setup_wizard_save_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        WizardNavigation(
            step = state.step,
            loading = state.loading,
            saving = state.saving,
            onBack = onBack,
            onNext = onNext,
            onFinish = onFinish,
        )
    }

    pickerTarget?.let { target ->
        SetupFolderPickerDialog(
            title =
                stringResource(
                    when (target) {
                        FolderPickerTarget.Main -> R.string.setup_wizard_choose_main_folder
                        FolderPickerTarget.Include -> R.string.setup_wizard_add_folder
                        FolderPickerTarget.Exclude -> R.string.setup_wizard_add_folder
                    },
                ),
            startPath = if (target == FolderPickerTarget.Main) state.mainFolder else null,
            onDismiss = { pickerTarget = null },
            onSelect = { path ->
                when (target) {
                    FolderPickerTarget.Main -> onSetMainFolder(path)
                    FolderPickerTarget.Include -> onAddInclude(path)
                    FolderPickerTarget.Exclude -> onAddExclude(path)
                }
                pickerTarget = null
            },
        )
    }
}

@Composable
private fun WizardHeader(
    step: SetupWizardStep,
    isFirstRun: Boolean,
    saving: Boolean,
    onSkip: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_wizard_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(step.titleRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onSkip, enabled = !saving) {
            Text(
                text = stringResource(if (isFirstRun) R.string.setup_wizard_skip else R.string.action_cancel),
            )
        }
    }
}

@Composable
private fun StepIndicator(currentStep: SetupWizardStep) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SetupWizardStep.entries.forEach { step ->
            val color =
                when {
                    step.ordinal < currentStep.ordinal -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    step == currentStep -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            Box(
                modifier =
                    Modifier
                        .height(4.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    StepColumn {
        Text(
            text = stringResource(R.string.welcome_to_x, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.setup_wizard_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FeatureRow(
            iconRes = R.drawable.ic_folder_24dp,
            title = stringResource(R.string.setup_wizard_feature_folders_title),
            summary = stringResource(R.string.setup_wizard_feature_folders_summary),
        )
        FeatureRow(
            iconRes = R.drawable.ic_library_music_24dp,
            title = stringResource(R.string.setup_wizard_feature_main_folder_title),
            summary = stringResource(R.string.setup_wizard_feature_main_folder_summary),
        )
        FeatureRow(
            iconRes = R.drawable.ic_restart_alt_24dp,
            title = stringResource(R.string.setup_wizard_feature_rescan_title),
            summary = stringResource(R.string.setup_wizard_feature_rescan_summary),
        )
    }
}

@Composable
private fun FoldersStep(
    mainFolder: String,
    includeFolders: List<String>,
    excludeFolders: List<String>,
    onChangeMainFolder: () -> Unit,
    onResetMainFolder: () -> Unit,
    onAddInclude: () -> Unit,
    onRemoveInclude: (String) -> Unit,
    onAddExclude: () -> Unit,
    onRemoveExclude: (String) -> Unit,
) {
    StepColumn {
        Text(
            text = stringResource(R.string.setup_wizard_folders_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MainFolderCard(
            mainFolder = mainFolder,
            onChange = onChangeMainFolder,
            onReset = onResetMainFolder,
        )
        FolderListSection(
            title = stringResource(R.string.setup_wizard_include_title),
            summary = stringResource(R.string.setup_wizard_include_summary),
            emptyText = stringResource(R.string.setup_wizard_include_empty),
            folders = includeFolders,
            addLabel = stringResource(R.string.setup_wizard_add_include),
            onAdd = onAddInclude,
            onRemove = onRemoveInclude,
        )
        FolderListSection(
            title = stringResource(R.string.setup_wizard_exclude_title),
            summary = stringResource(R.string.setup_wizard_exclude_summary),
            emptyText = stringResource(R.string.setup_wizard_exclude_empty),
            folders = excludeFolders,
            addLabel = stringResource(R.string.setup_wizard_add_exclude),
            onAdd = onAddExclude,
            onRemove = onRemoveExclude,
        )
    }
}

@Composable
private fun MainFolderCard(
    mainFolder: String,
    onChange: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.setup_wizard_main_folder_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.setup_wizard_main_folder_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                ) {
                    Text(
                        text = folderName(mainFolder),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mainFolder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.setup_wizard_reset_main_folder))
                }
                TextButton(onClick = onChange) {
                    Text(stringResource(R.string.setup_wizard_change_folder))
                }
            }
        }
    }
}

@Composable
private fun FolderListSection(
    title: String,
    summary: String,
    emptyText: String,
    folders: List<String>,
    addLabel: String,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (folders.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Column {
                    folders.forEachIndexed { index, path ->
                        FolderRow(
                            path = path,
                            onRemove = { onRemove(path) },
                        )
                        if (index != folders.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onAdd) {
            Icon(
                painter = painterResource(R.drawable.ic_add_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(addLabel)
        }
    }
}

@Composable
private fun FolderRow(
    path: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        ) {
            Text(
                text = folderName(path),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.setup_wizard_remove_folder, folderName(path)),
            )
        }
    }
}

@Composable
private fun ScanningStep(
    autoRescan: Boolean,
    onAutoRescanChange: (Boolean) -> Unit,
) {
    StepColumn {
        Text(
            text = stringResource(R.string.setup_wizard_scanning_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Switch) { onAutoRescanChange(!autoRescan) },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setup_wizard_auto_rescan_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.setup_wizard_auto_rescan_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = autoRescan,
                    onCheckedChange = null,
                )
            }
        }
        NoteCard(
            title = stringResource(R.string.setup_wizard_scan_limits_title),
            text = stringResource(R.string.setup_wizard_scan_limits_summary),
        )
    }
}

@Composable
private fun FinishStep(
    state: SetupWizardUiState,
    onRescan: () -> Unit,
) {
    StepColumn {
        Text(
            text = stringResource(R.string.setup_wizard_finish_intro),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.setup_wizard_settings_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryRow(
                    label = stringResource(R.string.setup_wizard_main_folder_title),
                    value = folderName(state.mainFolder),
                )
                SummaryRow(
                    label = stringResource(R.string.setup_wizard_summary_included),
                    value =
                        if (state.includeFolders.isEmpty()) {
                            stringResource(R.string.setup_wizard_all_folders)
                        } else {
                            state.includeFolders.size.toString()
                        },
                )
                SummaryRow(
                    label = stringResource(R.string.setup_wizard_summary_excluded),
                    value = state.excludeFolders.size.toString(),
                )
                SummaryRow(
                    label = stringResource(R.string.setup_wizard_auto_rescan_title),
                    value =
                        stringResource(
                            if (state.autoRescan) R.string.setup_wizard_state_on else R.string.setup_wizard_state_off,
                        ),
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.setup_wizard_rescan_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.setup_wizard_rescan_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                when (state.scanState) {
                    is SetupScanState.Scanning -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.setup_wizard_rescan_running),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    is SetupScanState.Finished -> {
                        Text(
                            text = stringResource(R.string.scan_finished),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onRescan, enabled = !state.saving) {
                            RescanButtonContent()
                        }
                    }

                    SetupScanState.Idle -> {
                        OutlinedButton(onClick = onRescan, enabled = !state.saving) {
                            RescanButtonContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RescanButtonContent() {
    Icon(
        painter = painterResource(R.drawable.ic_restart_alt_24dp),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(stringResource(R.string.setup_wizard_rescan_now))
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FeatureRow(
    iconRes: Int,
    title: String,
    summary: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteCard(
    title: String,
    text: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WizardNavigation(
    step: SetupWizardStep,
    loading: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step != SetupWizardStep.Welcome) {
            TextButton(onClick = onBack, enabled = !loading && !saving) {
                Text(stringResource(R.string.setup_wizard_back))
            }
        }
        Spacer(Modifier.weight(1f))
        when (step) {
            SetupWizardStep.Welcome -> {
                Button(onClick = onNext, enabled = !loading) {
                    Text(stringResource(R.string.get_started))
                }
            }

            SetupWizardStep.Finish -> {
                Button(onClick = onFinish, enabled = !loading && !saving) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.setup_wizard_finish))
                    }
                }
            }

            else -> {
                Button(onClick = onNext, enabled = !loading) {
                    Text(stringResource(R.string.setup_wizard_next))
                }
            }
        }
    }
}

@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

private fun folderName(path: String): String = File(path).name.ifEmpty { path }

private fun SetupWizardStep.titleRes(): Int =
    when (this) {
        SetupWizardStep.Welcome -> R.string.setup_wizard_step_welcome
        SetupWizardStep.Folders -> R.string.setup_wizard_step_folders
        SetupWizardStep.Scanning -> R.string.setup_wizard_step_scanning
        SetupWizardStep.Finish -> R.string.setup_wizard_step_finish
    }
