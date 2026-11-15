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

package com.ssilencce.ui.dialogs.playlists

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.fragment.app.DialogFragment
import com.ssilencce.R
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.library.PlaylistImportConflictState
import com.ssilencce.ui.screen.library.PlaylistImportGeneralError
import com.ssilencce.ui.screen.library.PlaylistImportRenameError
import com.ssilencce.ui.screen.library.PlaylistImportReplaceBlock
import com.ssilencce.ui.theme.SsilencceTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Compose conflict resolver for a device-playlist import whose exact source name already
 * exists. Keep both is the default; Replace is only available for one non-Favorites match.
 */
class PlaylistImportConflictDialogFragment : DialogFragment() {
    private val viewModel: LibraryViewModel by activityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
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
                    val state by viewModel.playlistImportConflict.collectAsState()
                    val current = state
                    LaunchedEffect(current) {
                        if (current == null) {
                            dismissAllowingStateLoss()
                        }
                    }
                    LaunchedEffect(current?.applying) {
                        dialog?.setCancelable(current?.applying != true)
                    }
                    if (current != null) {
                        PlaylistImportConflictContent(
                            state = current,
                            onCancel = {
                                viewModel.cancelPlaylistImport()
                                dismissAllowingStateLoss()
                            },
                            onKeepBoth = { viewModel.applyKeepBoth(requireContext().applicationContext) },
                            onReplace = { viewModel.applyReplace(requireContext().applicationContext) },
                            onRename = { name ->
                                viewModel.applyRename(requireContext().applicationContext, name)
                            },
                            onRenameChanged = viewModel::clearPlaylistImportRenameError,
                        )
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        if (viewModel.playlistImportConflict.value?.applying == true) return
        viewModel.cancelPlaylistImport()
        super.onCancel(dialog)
    }
}

private enum class PlaylistImportChoice {
    KeepBoth,
    Replace,
    Rename,
}

@Composable
private fun PlaylistImportConflictContent(
    state: PlaylistImportConflictState,
    onCancel: () -> Unit,
    onKeepBoth: () -> Unit,
    onReplace: () -> Unit,
    onRename: (String) -> Unit,
    onRenameChanged: () -> Unit,
) {
    var choice by remember(state.sourceName) {
        mutableStateOf(
            if (state.defaultName != null) PlaylistImportChoice.KeepBoth else PlaylistImportChoice.Rename,
        )
    }
    var renameText by remember(state.sourceName) {
        mutableStateOf(state.defaultName ?: state.sourceName)
    }
    LaunchedEffect(state.defaultName) {
        if (state.defaultName == null && choice == PlaylistImportChoice.KeepBoth) {
            choice = PlaylistImportChoice.Rename
        }
    }

    val cleanName = renameText.trim()
    val localRenameError =
        when {
            cleanName.isEmpty() -> PlaylistImportRenameError.Blank
            cleanName in state.existingNames -> PlaylistImportRenameError.Duplicate
            else -> null
        }
    val renameError = state.renameError ?: localRenameError
    val canApply =
        !state.applying &&
            when (choice) {
                PlaylistImportChoice.KeepBoth -> state.defaultName != null
                PlaylistImportChoice.Replace -> state.canReplace
                PlaylistImportChoice.Rename -> renameError == null
            }
    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.82f
        }

    Surface(shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.import_conflict_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text =
                    stringResource(
                        R.string.import_conflict_message,
                        state.sourceName,
                        state.selectedSongCount,
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(modifier = Modifier.selectableGroup()) {
                ConflictChoice(
                    selected = choice == PlaylistImportChoice.KeepBoth,
                    enabled = !state.applying && state.defaultName != null,
                    title = stringResource(R.string.import_keep_both),
                    summary =
                        state.defaultName?.let {
                            stringResource(R.string.import_keep_both_summary, it)
                        } ?: stringResource(R.string.import_keep_both_unavailable),
                    onSelect = { choice = PlaylistImportChoice.KeepBoth },
                )
                ConflictChoice(
                    selected = choice == PlaylistImportChoice.Replace,
                    enabled = !state.applying && state.canReplace,
                    title = stringResource(R.string.import_replace_existing),
                    summary =
                        if (state.canReplace) {
                            stringResource(
                                R.string.import_replace_warning,
                                state.existingSongCount,
                                state.sourceName,
                                state.selectedSongCount,
                            )
                        } else {
                            when (state.replaceBlock) {
                                PlaylistImportReplaceBlock.Favorites -> {
                                    stringResource(R.string.import_replace_favorites)
                                }

                                PlaylistImportReplaceBlock.MultipleMatches -> {
                                    stringResource(R.string.import_replace_ambiguous)
                                }

                                null -> {
                                    stringResource(R.string.import_replace_unavailable)
                                }
                            }
                        },
                    onSelect = { choice = PlaylistImportChoice.Replace },
                )
                ConflictChoice(
                    selected = choice == PlaylistImportChoice.Rename,
                    enabled = !state.applying,
                    title = stringResource(R.string.import_rename_new),
                    summary = stringResource(R.string.import_rename_summary),
                    onSelect = { choice = PlaylistImportChoice.Rename },
                )
            }

            if (choice == PlaylistImportChoice.Rename) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = {
                        renameText = it
                        onRenameChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.applying,
                    label = { Text(stringResource(R.string.import_rename_hint)) },
                    singleLine = true,
                    isError = renameError != null,
                    supportingText =
                        renameError?.let { error ->
                            {
                                Text(
                                    when (error) {
                                        PlaylistImportRenameError.Blank -> {
                                            stringResource(R.string.import_name_blank)
                                        }

                                        PlaylistImportRenameError.Duplicate -> {
                                            stringResource(R.string.import_name_taken, cleanName)
                                        }
                                    },
                                )
                            }
                        },
                )
            }

            state.generalError?.let { error ->
                Text(
                    text =
                        when (error) {
                            PlaylistImportGeneralError.SuffixExhausted -> {
                                stringResource(R.string.import_conflict_suffix_exhausted)
                            }

                            PlaylistImportGeneralError.NameTaken -> {
                                stringResource(R.string.import_conflict_name_taken)
                            }

                            PlaylistImportGeneralError.ConflictChanged -> {
                                stringResource(R.string.import_conflict_changed)
                            }

                            PlaylistImportGeneralError.DatabaseFailure -> {
                                stringResource(R.string.import_conflict_failed)
                            }
                        },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.applying) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onCancel,
                    enabled = !state.applying,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        when (choice) {
                            PlaylistImportChoice.KeepBoth -> onKeepBoth()
                            PlaylistImportChoice.Replace -> onReplace()
                            PlaylistImportChoice.Rename -> onRename(cleanName)
                        }
                    },
                    enabled = canApply,
                ) {
                    Text(stringResource(R.string.import_conflict_apply))
                }
            }
        }
    }
}

@Composable
private fun ConflictChoice(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    summary: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
