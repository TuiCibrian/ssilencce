package com.ssilencce.ui.screen.tageditor

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ssilencce.R
import com.ssilencce.core.model.tag.BatchWriteReport
import com.ssilencce.core.model.tag.FieldOperation
import com.ssilencce.core.model.tag.FieldWriteResult
import com.ssilencce.core.model.tag.FieldWriteStatus
import com.ssilencce.core.model.tag.FileWriteReport
import com.ssilencce.core.model.tag.FileWriteStatus
import com.ssilencce.core.model.tag.TagEditField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchTagEditorScreen(
    state: BatchTagEditorUiState,
    onOperationChange: (TagEditField, FieldOperation) -> Unit,
    onPerFileOperation: (Long, TagEditField, FieldOperation) -> Unit,
    onOpenPerFile: (TagEditField) -> Unit,
    onClosePerFile: () -> Unit,
    onSave: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val isWriting = state is BatchTagEditorUiState.Writing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.batch_tag_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !isWriting) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24dp),
                            contentDescription = stringResource(R.string.close_action),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (state is BatchTagEditorUiState.Ready) {
                BatchSaveBar(state, onSave)
            }
        },
    ) { contentPadding ->
        when (state) {
            is BatchTagEditorUiState.Loading -> {
                LoadingContent(state, contentPadding)
            }

            BatchTagEditorUiState.Empty -> {
                MessageContent(contentPadding, R.string.batch_tag_editor_empty, onClose)
            }

            is BatchTagEditorUiState.Ready -> {
                ReadyContent(state, contentPadding, onOperationChange, onOpenPerFile)
                state.perFileField?.let { field ->
                    PerFileFieldDialog(
                        field = field,
                        state = state,
                        onOperationChange = onPerFileOperation,
                        onClose = onClosePerFile,
                    )
                }
            }

            is BatchTagEditorUiState.Writing -> {
                WritingContent(state, contentPadding, onStop)
            }

            is BatchTagEditorUiState.Result -> {
                ResultContent(state.report, contentPadding, onClose)
            }

            is BatchTagEditorUiState.Error -> {
                ErrorContent(state, contentPadding, onClose)
            }
        }
    }
}

@Composable
private fun LoadingContent(
    state: BatchTagEditorUiState.Loading,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text =
                    if (state.total > 0) {
                        stringResource(R.string.batch_tag_editor_loading_progress, state.completed, state.total)
                    } else {
                        stringResource(R.string.batch_tag_editor_loading)
                    },
                textAlign = TextAlign.Center,
            )
            state.currentTitle?.let { title ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: BatchTagEditorUiState.Ready,
    contentPadding: PaddingValues,
    onOperationChange: (TagEditField, FieldOperation) -> Unit,
    onOpenPerFile: (TagEditField) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SelectionHeader(state)
        }
        items(state.fields, key = { it.field.name }) { fieldState ->
            BatchFieldEditor(
                state = fieldState,
                perFileCount = state.perSong.count { it.value[fieldState.field] != null },
                readableCount = state.readableCount,
                onOperationChange = onOperationChange,
                onOpenPerFile = onOpenPerFile,
            )
        }
    }
}

@Composable
private fun SelectionHeader(state: BatchTagEditorUiState.Ready) {
    var showSkipped by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.batch_tag_editor_selected_count, state.selectedCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    stringResource(
                        R.string.batch_tag_editor_readable_count,
                        state.readableCount,
                        state.selectedCount - state.readableCount,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.skippedFiles.isNotEmpty()) {
                TextButton(onClick = { showSkipped = !showSkipped }) {
                    Text(
                        text =
                            if (showSkipped) {
                                stringResource(R.string.batch_tag_editor_hide_skipped)
                            } else {
                                stringResource(R.string.batch_tag_editor_show_skipped, state.skippedFiles.size)
                            },
                    )
                }
                if (showSkipped) {
                    state.skippedFiles.forEach { skipped ->
                        Text(
                            text =
                                stringResource(
                                    R.string.batch_tag_editor_skipped_line,
                                    skipped.name,
                                    skipReasonLabel(skipped.reason),
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun skipReasonLabel(reason: BatchSkipReason): String =
    when (reason) {
        BatchSkipReason.MISSING -> stringResource(R.string.batch_tag_editor_skipped_missing)
        BatchSkipReason.UNREADABLE -> stringResource(R.string.batch_tag_editor_skipped_unreadable)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchFieldEditor(
    state: BatchFieldState,
    perFileCount: Int,
    readableCount: Int,
    onOperationChange: (TagEditField, FieldOperation) -> Unit,
    onOpenPerFile: (TagEditField) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(state.field.labelRes),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    when (val aggregate = state.aggregate) {
                        is FieldAggregate.Common -> aggregate.value
                        FieldAggregate.Varies -> stringResource(R.string.batch_tag_editor_varies)
                        FieldAggregate.Absent -> stringResource(R.string.batch_tag_editor_absent)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (state.aggregate is FieldAggregate.Common) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.operation is FieldOperation.Keep,
                    onClick = { onOperationChange(state.field, FieldOperation.Keep) },
                    label = { Text(stringResource(R.string.batch_tag_editor_keep)) },
                )
                FilterChip(
                    selected = state.operation is FieldOperation.Set,
                    onClick = {
                        val value =
                            (state.operation as? FieldOperation.Set)?.value
                                ?: state.commonValue.orEmpty()
                        onOperationChange(state.field, FieldOperation.Set(value))
                    },
                    label = { Text(stringResource(R.string.batch_tag_editor_set)) },
                )
                FilterChip(
                    selected = state.operation is FieldOperation.Clear,
                    onClick = { onOperationChange(state.field, FieldOperation.Clear) },
                    label = { Text(stringResource(R.string.batch_tag_editor_clear)) },
                    colors =
                        if (state.operation is FieldOperation.Clear) {
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        },
                )
            }
            when (val operation = state.operation) {
                is FieldOperation.Set -> {
                    OutlinedTextField(
                        value = operation.value,
                        onValueChange = { onOperationChange(state.field, FieldOperation.Set(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.batch_tag_editor_set_hint)) },
                        isError = operation.value.isBlank(),
                        supportingText =
                            if (operation.value.isBlank()) {
                                { Text(stringResource(R.string.batch_tag_editor_set_blank)) }
                            } else {
                                null
                            },
                        singleLine = state.field.inputKind != TagEditField.InputKind.MULTILINE,
                        minLines = if (state.field.inputKind == TagEditField.InputKind.MULTILINE) 3 else 1,
                        maxLines = if (state.field.inputKind == TagEditField.InputKind.MULTILINE) 8 else 1,
                        keyboardOptions =
                            if (state.field.inputKind == TagEditField.InputKind.NUMBER) {
                                KeyboardOptions(keyboardType = KeyboardType.Number)
                            } else {
                                KeyboardOptions.Default
                            },
                    )
                }

                FieldOperation.Clear -> {
                    Text(
                        text = stringResource(R.string.batch_tag_editor_clear_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                FieldOperation.Keep -> {
                    Unit
                }
            }
            if (readableCount > 1) {
                TextButton(onClick = { onOpenPerFile(state.field) }) {
                    Text(
                        text =
                            if (perFileCount > 0) {
                                stringResource(R.string.batch_tag_editor_per_file_count, perFileCount)
                            } else {
                                stringResource(R.string.batch_tag_editor_per_file)
                            },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerFileFieldDialog(
    field: TagEditField,
    state: BatchTagEditorUiState.Ready,
    onOperationChange: (Long, TagEditField, FieldOperation) -> Unit,
    onClose: () -> Unit,
) {
    var selectedSongId by remember(field) { mutableStateOf(state.files.firstOrNull()?.songId) }
    val selectedFile = state.files.firstOrNull { it.songId == selectedSongId }
    val commonOperation = state.fields.firstOrNull { it.field == field }?.operation ?: FieldOperation.Keep
    val selectedOperation = selectedFile?.let { state.perSong[it.songId]?.get(field) } ?: commonOperation
    var value by remember(field, selectedSongId) {
        mutableStateOf(
            (selectedOperation as? FieldOperation.Set)?.value
                ?: selectedFile?.values?.get(field).orEmpty(),
        )
    }
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.batch_tag_editor_per_file_title,
                            stringResource(field.labelRes),
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        value = newValue
                        selectedSongId?.let {
                            onOperationChange(it, field, FieldOperation.Set(newValue))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.batch_tag_editor_per_file_hint)) },
                    singleLine = field.inputKind != TagEditField.InputKind.MULTILINE,
                    minLines = if (field.inputKind == TagEditField.InputKind.MULTILINE) 2 else 1,
                    maxLines = if (field.inputKind == TagEditField.InputKind.MULTILINE) 5 else 1,
                    keyboardOptions =
                        if (field.inputKind == TagEditField.InputKind.NUMBER) {
                            KeyboardOptions(keyboardType = KeyboardType.Number)
                        } else {
                            KeyboardOptions.Default
                        },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedOperation is FieldOperation.Keep,
                        onClick = {
                            selectedSongId?.let { onOperationChange(it, field, FieldOperation.Keep) }
                        },
                        label = { Text(stringResource(R.string.batch_tag_editor_keep)) },
                    )
                    FilterChip(
                        selected = selectedOperation is FieldOperation.Set,
                        onClick = {
                            selectedSongId?.let {
                                onOperationChange(it, field, FieldOperation.Set(value))
                            }
                        },
                        enabled = value.isNotBlank(),
                        label = { Text(stringResource(R.string.batch_tag_editor_set)) },
                    )
                    FilterChip(
                        selected = selectedOperation is FieldOperation.Clear,
                        onClick = {
                            selectedSongId?.let { onOperationChange(it, field, FieldOperation.Clear) }
                        },
                        label = { Text(stringResource(R.string.batch_tag_editor_clear)) },
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.files, key = { it.songId }) { file ->
                        val operation = state.perSong[file.songId]?.get(field) ?: commonOperation
                        val selected = selectedSongId == file.songId
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ).selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { selectedSongId = file.songId },
                                    ).padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = file.values[field] ?: stringResource(R.string.batch_tag_editor_absent),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = fieldOperationLabel(operation),
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (operation is FieldOperation.Keep) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.batch_tag_editor_per_file_close))
                }
            }
        }
    }
}

@Composable
private fun fieldOperationLabel(operation: FieldOperation): String =
    when (operation) {
        FieldOperation.Keep -> stringResource(R.string.batch_tag_editor_keep)
        is FieldOperation.Set -> stringResource(R.string.batch_tag_editor_set)
        FieldOperation.Clear -> stringResource(R.string.batch_tag_editor_clear)
    }

@Composable
private fun BatchSaveBar(
    state: BatchTagEditorUiState.Ready,
    onSave: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text =
                    if (state.changedFieldCount == 0) {
                        stringResource(R.string.batch_tag_editor_no_changes)
                    } else {
                        stringResource(
                            R.string.batch_tag_editor_save_summary,
                            state.changedFieldCount,
                            state.readableCount,
                        )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.perFileOverrideCount > 0) {
                Text(
                    text =
                        stringResource(
                            R.string.batch_tag_editor_per_file_summary,
                            state.perFileOverrideCount,
                            state.readableCount,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.hasBlankSet) {
                Text(
                    text = stringResource(R.string.batch_tag_editor_set_blank),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.batch_tag_editor_save))
            }
        }
    }
}

@Composable
private fun WritingContent(
    state: BatchTagEditorUiState.Writing,
    contentPadding: PaddingValues,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.batch_tag_editor_saving, state.completed, state.total),
            style = MaterialTheme.typography.titleMedium,
        )
        state.currentTitle?.let { title ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.batch_tag_editor_writing, title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStop) {
            Text(stringResource(R.string.batch_tag_editor_stop))
        }
    }
}

@Composable
private fun ResultContent(
    report: BatchWriteReport,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.batch_tag_editor_result_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text =
                    stringResource(
                        R.string.batch_tag_editor_result_summary,
                        report.succeededFiles,
                        report.partialFiles,
                        report.failedFiles,
                        report.skippedFiles,
                        report.unchangedFiles,
                        report.cancelledFiles,
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text =
                    if (report.isFullSuccess) {
                        stringResource(R.string.batch_tag_editor_result_full_success)
                    } else {
                        stringResource(R.string.batch_tag_editor_result_problems)
                    },
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (report.isFullSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
        items(report.files, key = { "${it.songId}-${it.fileName}" }) { file ->
            FileReportCard(file)
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.close_action))
            }
        }
    }
}

@Composable
private fun FileReportCard(file: FileWriteReport) {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = fileStatusLabel(file.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = fileStatusColor(file.status),
                )
            }
            file.detail?.let { detail ->
                Text(
                    text = detailLabel(detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            file.overrides.entries.forEach { (field, operation) ->
                Text(
                    text = "${stringResource(field.labelRes)}: ${fieldOperationLabel(operation)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            file.fields.filter { it.isReportable() }.forEach { result ->
                Text(
                    text =
                        stringResource(
                            R.string.batch_tag_editor_field_status,
                            stringResource(result.field.labelRes),
                            fieldStatusLabel(result.status),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (result.status == FieldWriteStatus.KEPT_CHANGED) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    state: BatchTagEditorUiState.Error,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorMessage(state),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.close_action))
        }
    }
}

@Composable
private fun MessageContent(
    contentPadding: PaddingValues,
    @StringRes messageRes: Int,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.close_action))
        }
    }
}

@Composable
private fun errorMessage(state: BatchTagEditorUiState.Error): String =
    when (state.error) {
        BatchTagEditorError.TOO_MANY -> {
            stringResource(R.string.batch_tag_editor_too_many, BatchTagEditorViewModel.MAX_BATCH_SONGS)
        }

        BatchTagEditorError.NO_READABLE_FILES -> {
            stringResource(R.string.batch_tag_editor_no_readable)
        }

        BatchTagEditorError.PERMISSION_DENIED -> {
            stringResource(R.string.batch_tag_editor_permission_denied)
        }

        BatchTagEditorError.UNEXPECTED -> {
            stringResource(R.string.batch_tag_editor_error_unexpected)
        }
    }

@Composable
private fun fileStatusLabel(status: FileWriteStatus): String =
    when (status) {
        FileWriteStatus.SUCCEEDED -> stringResource(R.string.batch_tag_editor_file_updated)
        FileWriteStatus.PARTIAL -> stringResource(R.string.batch_tag_editor_file_partial)
        FileWriteStatus.FAILED -> stringResource(R.string.batch_tag_editor_file_failed)
        FileWriteStatus.SKIPPED -> stringResource(R.string.batch_tag_editor_file_skipped)
        FileWriteStatus.UNCHANGED -> stringResource(R.string.batch_tag_editor_file_unchanged)
        FileWriteStatus.CANCELLED -> stringResource(R.string.batch_tag_editor_file_cancelled)
    }

@Composable
private fun fileStatusColor(status: FileWriteStatus): Color =
    when (status) {
        FileWriteStatus.SUCCEEDED -> MaterialTheme.colorScheme.primary
        FileWriteStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        FileWriteStatus.FAILED -> MaterialTheme.colorScheme.error
        FileWriteStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        FileWriteStatus.UNCHANGED -> MaterialTheme.colorScheme.onSurfaceVariant
        FileWriteStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }

@Composable
private fun fieldStatusLabel(status: FieldWriteStatus): String =
    when (status) {
        FieldWriteStatus.KEPT -> stringResource(R.string.batch_tag_editor_field_kept)
        FieldWriteStatus.WRITTEN -> stringResource(R.string.batch_tag_editor_field_written)
        FieldWriteStatus.CLEARED -> stringResource(R.string.batch_tag_editor_field_cleared)
        FieldWriteStatus.UNSUPPORTED -> stringResource(R.string.batch_tag_editor_field_unsupported)
        FieldWriteStatus.MISMATCH -> stringResource(R.string.batch_tag_editor_field_mismatch)
        FieldWriteStatus.KEPT_CHANGED -> stringResource(R.string.batch_tag_editor_field_kept_changed)
        FieldWriteStatus.NOT_ATTEMPTED -> stringResource(R.string.batch_tag_editor_field_not_attempted)
    }

@Composable
private fun detailLabel(detail: String): String =
    when (detail) {
        "missing" -> stringResource(R.string.batch_tag_editor_missing_file)
        "unreadable" -> stringResource(R.string.batch_tag_editor_unreadable_file)
        "open_failed" -> stringResource(R.string.batch_tag_editor_open_failed)
        "save_failed" -> stringResource(R.string.batch_tag_editor_save_failed)
        "read_back_failed" -> stringResource(R.string.batch_tag_editor_read_back_failed)
        "scan_failed" -> stringResource(R.string.batch_tag_editor_scan_failed)
        "no_changes" -> stringResource(R.string.batch_tag_editor_no_changes)
        "cancelled" -> stringResource(R.string.batch_tag_editor_file_cancelled)
        else -> stringResource(R.string.batch_tag_editor_unknown_detail)
    }

private fun FieldWriteResult.isReportable(): Boolean =
    status == FieldWriteStatus.UNSUPPORTED ||
        status == FieldWriteStatus.MISMATCH ||
        status == FieldWriteStatus.KEPT_CHANGED ||
        status == FieldWriteStatus.NOT_ATTEMPTED
