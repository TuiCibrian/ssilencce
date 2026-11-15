/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ssilencce.ui.screen.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.ssilencce.R
import com.ssilencce.data.local.room.NamedQueueSummary
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.component.base.AbsMainActivityFragment
import com.ssilencce.ui.component.compose.ObserveAsEvent
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.SsilencceTheme

class QueuesFragment : AbsMainActivityFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SsilencceTheme {
                    QueuesScreen(
                        playerViewModel = playerViewModel,
                        onOpenQueue = {
                            if (isAdded) findNavController().navigate(R.id.nav_queue)
                        },
                    )
                }
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        applyWindowInsetsFromView(view)
        checkForMargins(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { checkForMargins(it) }
    }

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater,
    ) {}

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
}

@Composable
private fun QueuesScreen(
    playerViewModel: PlayerViewModel,
    onOpenQueue: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val playQueue by playerViewModel.queueFlow.collectAsStateWithLifecycle()
    val queues by playerViewModel.namedQueuesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeQueueId by playerViewModel.activeNamedQueueIdFlow.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<NamedQueueSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<NamedQueueSummary?>(null) }
    var mergeSource by remember { mutableStateOf<NamedQueueSummary?>(null) }
    var switchingQueueId by remember { mutableStateOf<String?>(null) }

    ObserveAsEvent(playerViewModel.namedQueueMessage) { message ->
        context.showToast(resources.getString(message.messageRes, *message.formatArgs.toTypedArray()))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.queues_label),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.playing_queue_label)) },
                supportingContent = {
                    Text(stringResource(R.string.named_queue_items, playQueue.size))
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_queue_music_24dp),
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier.clickable(enabled = playQueue.isNotEmpty() && switchingQueueId == null) {
                        onOpenQueue()
                    },
            )
        }

        item {
            Button(
                onClick = { showSaveDialog = true },
                enabled = playQueue.isNotEmpty() && switchingQueueId == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.named_queue_save_current))
            }
        }

        item {
            Text(
                text = stringResource(R.string.queues_saved_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (queues.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.named_queue_none),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(queues, key = { it.id }) { queue ->
                SavedQueueRow(
                    queue = queue,
                    active = queue.id == activeQueueId,
                    enabled = switchingQueueId == null,
                    switching = switchingQueueId == queue.id,
                    onSwitch = {
                        if (queue.id == activeQueueId) {
                            onOpenQueue()
                        } else {
                            switchingQueueId = queue.id
                            playerViewModel.switchToNamedQueueWithResult(queue.id) { success ->
                                switchingQueueId = null
                                if (success) onOpenQueue()
                            }
                        }
                    },
                    onRename = { renameTarget = queue },
                    onDelete = { deleteTarget = queue },
                    onMerge = { mergeSource = queue },
                    mergeEnabled = queues.size > 1,
                )
            }
        }
    }

    if (showSaveDialog) {
        QueueNameDialog(
            title = R.string.named_queue_save_title,
            initialName = "",
            onConfirm = { name ->
                playerViewModel.saveCurrentQueueAs(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    renameTarget?.let { queue ->
        QueueNameDialog(
            title = R.string.named_queue_rename_title,
            initialName = queue.name,
            onConfirm = { name ->
                playerViewModel.renameNamedQueue(queue.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { queue ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.named_queue_delete_title)) },
            text = {
                Text(stringResource(R.string.named_queue_delete_message, queue.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playerViewModel.deleteNamedQueue(queue.id)
                        deleteTarget = null
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
    mergeSource?.let { source ->
        NamedQueueMergeDialog(
            source = source,
            targets = queues.filterNot { it.id == source.id },
            onDismiss = { mergeSource = null },
            onMerge = { targetQueueId, skipDuplicates ->
                mergeSource = null
                playerViewModel.mergeNamedQueues(source.id, targetQueueId, skipDuplicates)
            },
        )
    }
}

@Composable
private fun SavedQueueRow(
    queue: NamedQueueSummary,
    active: Boolean,
    enabled: Boolean,
    switching: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMerge: () -> Unit,
    mergeEnabled: Boolean,
) {
    ListItem(
        headlineContent = {
            Text(
                text = queue.name,
                color =
                    if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.named_queue_items, queue.itemCount))
                if (active) {
                    Text(
                        text = stringResource(R.string.named_queue_active),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        trailingContent = {
            if (switching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMerge, enabled = enabled && mergeEnabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_playlist_add_24dp),
                            contentDescription = stringResource(R.string.named_queue_merge),
                        )
                    }
                    IconButton(onClick = onRename, enabled = enabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_24dp),
                            contentDescription = stringResource(R.string.named_queue_rename),
                        )
                    }
                    IconButton(onClick = onDelete, enabled = enabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_24dp),
                            contentDescription = stringResource(R.string.named_queue_remove),
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onSwitch),
    )
}

@Composable
private fun QueueNameDialog(
    @StringRes title: Int,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.named_queue_name_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
