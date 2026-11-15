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

package com.ssilencce.ui.screen.lyrics

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.data.local.lyrics.lrc.LrcTools
import com.ssilencce.data.local.lyrics.lrc.StampedLyricLine
import com.ssilencce.extensions.showToast

internal data class SyncLineState(
    val text: String,
    val timestamp: Long? = null,
)

@Stable
internal class LyricsSyncState internal constructor() {
    internal val lines = mutableStateListOf<SyncLineState>()
    internal var manualTarget by mutableStateOf<Int?>(null)
    internal var sourceContent: String = ""

    internal fun setContent(content: String) {
        if (content == sourceContent) {
            return
        }
        sourceContent = content
        lines.clear()
        lines.addAll(LrcTools.parseLines(content).map { SyncLineState(it.text, it.timestamp) })
        manualTarget = null
    }
}

private val LyricsSyncStateSaver =
    Saver<LyricsSyncState, Bundle>(
        save = { state ->
            Bundle().apply {
                putString(SAVED_SOURCE_CONTENT, state.sourceContent)
                putString(
                    SAVED_LINES,
                    state.lines.joinToString("\n") { line ->
                        "${line.timestamp ?: NO_TIMESTAMP}|${line.text}"
                    },
                )
            }
        },
        restore = { bundle ->
            LyricsSyncState().apply {
                sourceContent = bundle.getString(SAVED_SOURCE_CONTENT).orEmpty()
                bundle
                    .getString(SAVED_LINES)
                    .orEmpty()
                    .lineSequence()
                    .forEach { entry ->
                        val separator = entry.indexOf('|')
                        if (separator >= 0) {
                            val timestamp = entry.substring(0, separator).toLongOrNull() ?: NO_TIMESTAMP
                            lines.add(
                                SyncLineState(
                                    text = entry.substring(separator + 1),
                                    timestamp = timestamp.takeIf { it >= 0 },
                                ),
                            )
                        }
                    }
            }
        },
    )

private const val SAVED_SOURCE_CONTENT = "source_content"
private const val SAVED_LINES = "lines"

private const val NO_TIMESTAMP = -1L

/**
 * Saveable stamping state, kept by the editor screen so switching between Edit and Sync modes does
 * not discard timestamps the user already assigned. Call [LyricsSyncState.setContent] when the
 * editable content changes.
 */
@Composable
internal fun rememberLyricsSyncState(): LyricsSyncState = rememberSaveable(saver = LyricsSyncStateSaver) { LyricsSyncState() }

/**
 * Stamping surface shown by the lyrics editor's Sync mode.
 *
 * The parent owns the player connection, the editable line list and the save destinations; this
 * composable renders the stamping controls and exposes the finished LRC through the save callbacks.
 */
@Composable
internal fun LyricsSyncEditor(
    state: LyricsSyncState,
    currentPosition: Long,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onTogglePlayPause: () -> Unit,
    onSaveEmbedded: (String) -> Unit,
    onExportFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lines = state.lines
    var showSaveDialog by remember { mutableStateOf(false) }

    val firstUnstamped = lines.indexOfFirst { it.timestamp == null }
    val targetIndex = state.manualTarget?.takeIf { it in lines.indices } ?: firstUnstamped
    val stampedCount = lines.count { it.timestamp != null }

    fun stampTarget() {
        val index = targetIndex
        if (index !in lines.indices) {
            context.showToast(R.string.lyrics_sync_all_stamped)
            return
        }
        val previous = lines.getOrNull(index - 1)?.timestamp
        val timestamp = if (previous != null) maxOf(currentPosition, previous + 100) else currentPosition
        lines[index] = lines[index].copy(timestamp = timestamp)
        state.manualTarget = (index + 1 until lines.size).firstOrNull { lines[it].timestamp == null }
    }

    fun adjust(
        index: Int,
        delta: Long,
    ) {
        val current = lines.getOrNull(index)?.timestamp ?: return
        lines[index] = lines[index].copy(timestamp = (current + delta).coerceAtLeast(0))
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.lyrics_sync_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.lyrics_sync_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!isCurrentSong) {
            Text(
                text = stringResource(R.string.lyrics_sync_not_current_song),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilledIconButton(
                onClick = onTogglePlayPause,
                enabled = isCurrentSong,
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
                        ),
                    contentDescription =
                        stringResource(
                            if (isPlaying) R.string.action_pause else R.string.action_play,
                        ),
                )
            }

            Text(
                text = LrcTools.formatTimestamp(currentPosition).removeSurrounding("[", "]"),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { stampTarget() },
                enabled = isCurrentSong && targetIndex in lines.indices,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.lyrics_sync_stamp))
            }
        }

        Text(
            text = stringResource(R.string.lyrics_sync_stamped_count, stampedCount, lines.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        if (lines.isEmpty()) {
            Text(
                text = stringResource(R.string.lyrics_sync_no_lines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(lines) { index, line ->
                    SyncLineRow(
                        line = line,
                        isTarget = index == targetIndex,
                        onSelect = { state.manualTarget = index },
                        onAdjustEarlier = { adjust(index, -100) },
                        onAdjustLater = { adjust(index, 100) },
                    )
                }
            }
        }

        TextButton(
            onClick = { showSaveDialog = true },
            enabled = stampedCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_save_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.lyrics_sync_save))
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.lyrics_sync_save)) },
            text = {
                Column {
                    val lrc = LrcTools.buildLrc(lines.map { it.toStampedLine() })
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            onSaveEmbedded(lrc)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_audio_file_24dp),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.lyrics_sync_save_embedded),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            onExportFile(lrc)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_save_24dp),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.lyrics_sync_save_file),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.close_action))
                }
            },
        )
    }
}

@Composable
private fun SyncLineRow(
    line: SyncLineState,
    isTarget: Boolean,
    onSelect: () -> Unit,
    onAdjustEarlier: () -> Unit,
    onAdjustLater: () -> Unit,
) {
    Surface(
        color =
            if (isTarget) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.Transparent
            },
        shape = MaterialTheme.shapes.small,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        line.timestamp
                            ?.let { LrcTools.formatTimestamp(it).removeSurrounding("[", "]") }
                            ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TextButton(
                onClick = onAdjustEarlier,
                enabled = line.timestamp != null,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_sync_adjust_earlier),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            TextButton(
                onClick = onAdjustLater,
                enabled = line.timestamp != null,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_sync_adjust_later),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun SyncLineState.toStampedLine() = StampedLyricLine(text, timestamp)
