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

package com.ssilencce.ui.screen.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.notes.SongBookmark
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.ui.component.compose.menu.MenuItem
import com.ssilencce.ui.component.compose.menu.TopAppBarMenu
import com.ssilencce.ui.screen.player.SeekTimeParser
import com.ssilencce.util.Preferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongNotesScreen(
    song: Song,
    state: SongNotesUiState,
    isCurrentSong: Boolean,
    currentPositionMs: Long,
    onBackClick: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onSaveNotes: () -> Unit,
    onAddBookmark: (Long, String) -> Unit,
    onUpdateBookmark: (Int, Long, String) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onSeekBookmark: (Long) -> Unit,
    onImportClick: () -> Unit,
    onShareClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    var showAddBookmark by remember { mutableStateOf(false) }
    var editingBookmarkIndex by remember { mutableStateOf<Int?>(null) }
    var deletingBookmarkIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        containerColor =
            if (Preferences.isAuroraAppearance) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.background
            },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.action_song_notes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back_24dp),
                            contentDescription = stringResource(R.string.back_action),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddBookmark = true },
                        enabled = !state.loadFailed,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark_24dp),
                            contentDescription = stringResource(R.string.action_add_bookmark),
                        )
                    }
                    TopAppBarMenu(
                        items =
                            listOf(
                                MenuItem.Button.DropDown(
                                    text = stringResource(R.string.song_notes_import),
                                    icon = painterResource(R.drawable.ic_download_24dp),
                                    onClick = onImportClick,
                                    enabled = !state.loadFailed,
                                ),
                                MenuItem.Button.DropDown(
                                    text = stringResource(R.string.song_notes_share),
                                    icon = painterResource(R.drawable.ic_share_24dp),
                                    onClick = onShareClick,
                                    enabled = state.hasContent,
                                ),
                            ),
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = paddingValues.calculateTopPadding() + 12.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (state.loadFailed) {
                item {
                    LoadFailedCard(onRetryClick = onRetryClick)
                }
            } else {
                item {
                    NotesEditor(
                        notes = state.notes,
                        enabled = !state.isLoading,
                        isDirty = state.isDirty,
                        onNotesChanged = onNotesChanged,
                        onSaveNotes = onSaveNotes,
                    )
                }
                item {
                    BookmarksHeader(
                        count = state.bookmarks.size,
                        enabled = !state.isLoading,
                        onAddClick = { showAddBookmark = true },
                    )
                }
                if (state.bookmarks.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.song_notes_no_bookmarks),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    itemsIndexed(state.bookmarks) { index, bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            onSeekClick = { onSeekBookmark(bookmark.positionMs) },
                            onEditClick = { editingBookmarkIndex = index },
                            onDeleteClick = { deletingBookmarkIndex = index },
                        )
                    }
                    if (!isCurrentSong) {
                        item {
                            Text(
                                text = stringResource(R.string.song_notes_start_song_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (state.storagePath.isNotBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.song_notes_storage_hint, state.storagePath),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showAddBookmark) {
        BookmarkDialog(
            title = stringResource(R.string.action_add_bookmark),
            initialTimestampMs = if (isCurrentSong) currentPositionMs else 0L,
            initialText = "",
            confirmLabel = stringResource(R.string.add_action),
            onDismiss = { showAddBookmark = false },
            onConfirm = { timestampMs, text ->
                onAddBookmark(timestampMs, text)
                showAddBookmark = false
            },
        )
    }

    editingBookmarkIndex?.let { index ->
        state.bookmarks.getOrNull(index)?.let { bookmark ->
            BookmarkDialog(
                title = stringResource(R.string.song_notes_edit_bookmark),
                initialTimestampMs = bookmark.positionMs,
                initialText = bookmark.text,
                confirmLabel = stringResource(R.string.action_save),
                onDismiss = { editingBookmarkIndex = null },
                onConfirm = { timestampMs, text ->
                    onUpdateBookmark(index, timestampMs, text)
                    editingBookmarkIndex = null
                },
            )
        }
    }

    deletingBookmarkIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { deletingBookmarkIndex = null },
            title = { Text(stringResource(R.string.song_notes_delete_bookmark)) },
            text = { Text(stringResource(R.string.song_notes_delete_bookmark_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBookmark(index)
                        deletingBookmarkIndex = null
                    },
                ) {
                    Text(stringResource(R.string.delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingBookmarkIndex = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NotesEditor(
    notes: String,
    enabled: Boolean,
    isDirty: Boolean,
    onNotesChanged: (String) -> Unit,
    onSaveNotes: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.song_notes_notes_section),
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 160.dp),
            placeholder = { Text(stringResource(R.string.song_notes_notes_hint)) },
            minLines = 5,
            maxLines = 12,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onSaveNotes,
                enabled = enabled && isDirty,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_save_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.song_notes_save))
            }
        }
    }
}

@Composable
private fun BookmarksHeader(
    count: Int,
    enabled: Boolean,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.song_notes_bookmarks_section_count, count),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAddClick, enabled = enabled) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.action_add_bookmark))
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: SongBookmark,
    onSeekClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onSeekClick).padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = bookmark.positionMs.asReadableDuration(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(min = 56.dp),
            )
            Text(
                text =
                    bookmark.text.ifBlank {
                        stringResource(R.string.song_notes_bookmark_untitled)
                    },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            IconButton(onClick = onEditClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_24dp),
                    contentDescription = stringResource(R.string.song_notes_edit_bookmark),
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24dp),
                    contentDescription = stringResource(R.string.song_notes_delete_bookmark),
                )
            }
        }
    }
}

@Composable
private fun LoadFailedCard(onRetryClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.song_notes_load_failed),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetryClick) {
                Text(stringResource(R.string.song_notes_retry))
            }
        }
    }
}

@Composable
private fun BookmarkDialog(
    title: String,
    initialTimestampMs: Long,
    initialText: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    var timestampText by remember { mutableStateOf(SeekTimeParser.format(initialTimestampMs)) }
    var text by remember { mutableStateOf(initialText) }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timestampText,
                    onValueChange = {
                        timestampText = it
                        invalid = false
                    },
                    label = { Text(stringResource(R.string.song_notes_bookmark_time_hint)) },
                    singleLine = true,
                    isError = invalid,
                    supportingText =
                        if (invalid) {
                            { Text(stringResource(R.string.song_notes_bookmark_time_invalid)) }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.song_notes_bookmark_text_hint)) },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val position = SeekTimeParser.parse(timestampText)
                    if (position == null) {
                        invalid = true
                    } else {
                        onConfirm(position, text)
                    }
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
