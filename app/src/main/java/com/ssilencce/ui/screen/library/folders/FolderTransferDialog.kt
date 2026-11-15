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

package com.ssilencce.ui.screen.library.folders

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Folder
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.getPrettyAbsolutePath
import com.ssilencce.extensions.hasR
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.dialogs.SAFDialog
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.FolderFileOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Folder picker that copies or moves the selected songs. The whole operation lives in this
 * dialog so every song list can trigger it without extra state, and the SAF grant flow only has
 * to re-run the pending destination.
 */
class FolderTransferDialog :
    DialogFragment(),
    SAFDialog.SAFResultListener {
    private val repository: Repository by inject()

    private var uiState by mutableStateOf<TransferUiState>(TransferUiState.Loading)
    private var pendingResult: FolderFileOperations.TransferResult? = null
    private var pendingDestination: String? = null
    private var availableFolders: List<Folder> = emptyList()
    private var safRetried = false

    private val songs: List<Song>
        get() =
            BundleCompat
                .getParcelableArrayList(requireArguments(), ARG_SONGS, Song::class.java)
                .orEmpty()

    private val copy: Boolean
        get() = requireArguments().getBoolean(ARG_COPY)

    private val deleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                removePendingOriginals()
            } else {
                uiState = TransferUiState.Finished(pendingResult, originalsRemovalFailed = true)
            }
        }

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
                    TransferContent(
                        state = uiState,
                        copy = copy,
                        songCount = songs.size,
                        onSelect = { path ->
                            (uiState as? TransferUiState.Picking)?.let {
                                uiState = it.copy(selectedPath = path)
                            }
                        },
                        onConfirm = { path -> startTransfer(path) },
                        onRemoveOriginals = { requestOriginalRemoval() },
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            loadFolders()
            pendingDestination?.let { destination ->
                pendingDestination = null
                startTransfer(destination)
            }
        } else if (uiState is TransferUiState.Loading) {
            loadFolders()
        }
    }

    override fun onSAFResult(treeUri: Uri?) {
        if (treeUri == null) {
            safRetried = false
            return
        }
        if (safRetried) {
            safRetried = false
            return
        }
        val destination = pendingDestination ?: return
        pendingDestination = null
        safRetried = true
        startTransfer(destination)
    }

    private fun loadFolders() {
        lifecycleScope.launch {
            val folders =
                withContext(Dispatchers.IO) {
                    runCatching {
                        repository.allFolders().children.filterIsInstance<Folder>()
                    }.getOrDefault(emptyList())
                }
            uiState =
                TransferUiState.Picking(
                    folders = folders.sortedBy { it.filePath.lowercase() },
                )
            availableFolders = folders.sortedBy { it.filePath.lowercase() }
        }
    }

    private fun startTransfer(destinationPath: String) {
        if (uiState is TransferUiState.Running) return
        lifecycleScope.launch {
            uiState = TransferUiState.Running(done = 0, total = songs.size)
            val appContext = context?.applicationContext ?: return@launch
            val operations = FolderFileOperations(appContext)
            val outcome =
                operations.transferSongs(
                    songs = songs,
                    destinationPath = destinationPath,
                    copy = copy,
                    onProgress = { done, total ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            uiState = TransferUiState.Running(done, total)
                        }
                    },
                )
            when (outcome) {
                is FolderFileOperations.TransferOutcome.Completed -> {
                    pendingResult = outcome.result
                    uiState = TransferUiState.Finished(outcome.result)
                }

                FolderFileOperations.TransferOutcome.NeedsSafAccess -> {
                    if (safRetried || !isAdded) {
                        safRetried = false
                        uiState = TransferUiState.Finished(errorRes = R.string.transfer_failed)
                    } else {
                        pendingDestination = destinationPath
                        uiState =
                            TransferUiState.Picking(
                                folders = availableFolders,
                                selectedPath = destinationPath,
                            )
                        SAFDialog.show(this@FolderTransferDialog)
                    }
                }

                is FolderFileOperations.TransferOutcome.Failure -> {
                    uiState = TransferUiState.Finished(errorRes = R.string.transfer_failed)
                }
            }
        }
    }

    private fun requestOriginalRemoval() {
        val result = pendingResult ?: return
        if (!hasR()) {
            uiState = TransferUiState.Finished(result, originalsRemovalFailed = true)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val resolver = context?.contentResolver
            if (resolver == null) {
                uiState = TransferUiState.Finished(result, originalsRemovalFailed = true)
                return
            }
            launchDeleteRequest(
                MediaStore
                    .createDeleteRequest(resolver, result.pendingDeleteUris)
                    .intentSender,
            )
        } else {
            val sender: IntentSender? = result.recoveryIntentSender
            if (sender == null) {
                uiState = TransferUiState.Finished(result, originalsRemovalFailed = true)
            } else {
                launchDeleteRequest(sender)
            }
        }
    }

    private fun launchDeleteRequest(sender: IntentSender) {
        runCatching {
            deleteLauncher.launch(
                IntentSenderRequest.Builder(sender).build(),
            )
        }.onFailure {
            uiState = TransferUiState.Finished(pendingResult, originalsRemovalFailed = true)
        }
    }

    private fun removePendingOriginals() {
        val result = pendingResult ?: return
        lifecycleScope.launch {
            val remaining =
                withContext(Dispatchers.IO) {
                    val appContext = context?.applicationContext ?: return@withContext result.pendingDeleteUris
                    FolderFileOperations(appContext)
                        .deleteApprovedSources(result.pendingDeleteUris)
                }
            val removedCount = result.pendingDeleteUris.size - remaining.size
            if (removedCount <= 0) {
                uiState = TransferUiState.Finished(result, originalsRemovalFailed = true)
            } else {
                val updated =
                    result.copy(
                        moved = result.moved + removedCount,
                        pendingDeleteUris = remaining,
                    )
                pendingResult = updated
                val message = context?.getString(R.string.transfer_originals_removed, removedCount)
                context?.showToast(message)
                uiState = TransferUiState.Finished(updated)
            }
        }
    }

    companion object {
        private const val ARG_SONGS = "songs"
        private const val ARG_COPY = "copy"
        private const val TAG = "FolderTransferDialog"

        fun show(
            fragment: Fragment,
            songs: List<Song>,
            copy: Boolean,
        ) {
            if (songs.isEmpty()) return
            FolderTransferDialog()
                .apply {
                    arguments =
                        Bundle().apply {
                            putParcelableArrayList(ARG_SONGS, ArrayList(songs))
                            putBoolean(ARG_COPY, copy)
                        }
                }.show(fragment.childFragmentManager, TAG)
        }
    }
}

private sealed interface TransferUiState {
    data object Loading : TransferUiState

    data class Picking(
        val folders: List<Folder>,
        val selectedPath: String? = null,
    ) : TransferUiState

    data class Running(
        val done: Int,
        val total: Int,
    ) : TransferUiState

    data class Finished(
        val result: FolderFileOperations.TransferResult? = null,
        val errorRes: Int? = null,
        val originalsRemovalFailed: Boolean = false,
    ) : TransferUiState
}

@Composable
private fun TransferContent(
    state: TransferUiState,
    copy: Boolean,
    songCount: Int,
    onSelect: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onRemoveOriginals: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        TransferUiState.Loading -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.choose_destination_folder)) },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        is TransferUiState.Picking -> {
            var query by remember { mutableStateOf("") }
            val visibleFolders =
                remember(state.folders, query) {
                    if (query.isBlank()) {
                        state.folders
                    } else {
                        state.folders.filter {
                            it.fileName.contains(query, ignoreCase = true) ||
                                it.filePath.contains(query, ignoreCase = true)
                        }
                    }
                }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        stringResource(
                            if (copy) R.string.transfer_copy_title else R.string.transfer_move_title,
                            songCount,
                        ),
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.transfer_choose_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.search_label)) },
                        )
                        if (visibleFolders.isEmpty()) {
                            Text(
                                text = stringResource(R.string.transfer_no_folders),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                                items(visibleFolders, key = { it.filePath }) { folder ->
                                    val selected = folder.filePath == state.selectedPath
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelect(folder.filePath) }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                    ) {
                                        Text(
                                            text = folder.fileName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = folder.filePath.getPrettyPath(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { state.selectedPath?.let(onConfirm) },
                        enabled = state.selectedPath != null,
                    ) {
                        Text(
                            stringResource(
                                if (copy) R.string.transfer_action_copy else R.string.transfer_action_move,
                            ),
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        is TransferUiState.Running -> {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        stringResource(
                            if (copy) R.string.transfer_copy_title else R.string.transfer_move_title,
                            state.total,
                        ),
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                if (state.total == 0) 0f else state.done.toFloat() / state.total
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.transfer_progress,
                                    state.done,
                                    state.total,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {},
            )
        }

        is TransferUiState.Finished -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        stringResource(
                            if (copy) R.string.transfer_copy_title else R.string.transfer_move_title,
                            songCount,
                        ),
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val errorRes = state.errorRes
                        if (errorRes != null) {
                            Text(
                                text = stringResource(errorRes),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            val result = state.result
                            if (result != null) {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.transfer_result,
                                            result.copied,
                                            result.moved,
                                            result.failed,
                                        ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (result.skipped > 0) {
                                    Text(
                                        text = stringResource(R.string.transfer_skipped, result.skipped),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (result.missingSources > 0) {
                                    Text(
                                        text = stringResource(R.string.transfer_source_missing),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (result.needsOriginalRemoval) {
                                    Text(
                                        text =
                                            stringResource(
                                                R.string.transfer_source_not_removed,
                                                result.pendingDeleteUris.size,
                                            ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (state.originalsRemovalFailed) {
                                    Text(
                                        text = stringResource(R.string.transfer_originals_removal_failed),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (state.result?.needsOriginalRemoval == true) {
                                TextButton(onClick = onRemoveOriginals) {
                                    Text(stringResource(R.string.transfer_remove_originals))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close_action))
                    }
                },
                dismissButton = {},
            )
        }
    }
}

private fun String.getPrettyPath(): String = java.io.File(this).getPrettyAbsolutePath()
