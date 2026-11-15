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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.data.local.repository.MissingSongsCleanupResult
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import com.ssilencce.ui.theme.SsilencceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Maintenance action that scans MediaStore for songs whose files no longer exist and removes
 * their playlist entries, play counts, play events and history rows.
 */
class RemoveMissingSongsDialog : DialogFragment() {
    private val repository: Repository by inject()

    private var state by mutableStateOf<CleanupState>(CleanupState.Scanning)

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
                    CleanupContent(
                        state = state,
                        onRemove = { removeMissingSongs(it) },
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
        if (state is CleanupState.Scanning) {
            scan()
        }
    }

    private fun scan() {
        lifecycleScope.launch {
            state = CleanupState.Scanning
            val missing =
                withContext(Dispatchers.IO) {
                    runCatching { repository.findMissingSongs() }.getOrDefault(emptyList())
                }
            state =
                if (missing.isEmpty()) {
                    CleanupState.NothingMissing
                } else {
                    CleanupState.Confirm(missing)
                }
        }
    }

    private fun removeMissingSongs(songs: List<Song>) {
        lifecycleScope.launch {
            state = CleanupState.Running
            val result =
                withContext(Dispatchers.IO) {
                    runCatching { repository.removeMissingSongs(songs) }.getOrNull()
                }
            state =
                if (result == null) {
                    CleanupState.Failed
                } else {
                    CleanupState.Done(result)
                }
        }
    }

    companion object {
        const val TAG = "RemoveMissingSongsDialog"
    }
}

private sealed interface CleanupState {
    data object Scanning : CleanupState

    data class Confirm(
        val songs: List<Song>,
    ) : CleanupState

    data object Running : CleanupState

    data class Done(
        val result: MissingSongsCleanupResult,
    ) : CleanupState

    data object NothingMissing : CleanupState

    data object Failed : CleanupState
}

@Composable
private fun CleanupContent(
    state: CleanupState,
    onRemove: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (state !is CleanupState.Running) onDismiss() },
        title = { Text(stringResource(R.string.remove_missing_songs_title)) },
        text = {
            when (state) {
                CleanupState.Scanning -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.remove_missing_songs_scanning),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                is CleanupState.Confirm -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text =
                                stringResource(
                                    R.string.remove_missing_songs_confirm,
                                    state.songs.size,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                CleanupState.Running -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text(
                            text = stringResource(R.string.saving_changes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                is CleanupState.Done -> {
                    Text(
                        text =
                            stringResource(
                                R.string.remove_missing_songs_done,
                                state.result.missingSongs,
                                state.result.playlistEntries,
                                state.result.playCountEntries + state.result.playEventEntries,
                                state.result.historyEntries,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                CleanupState.NothingMissing -> {
                    Text(
                        text = stringResource(R.string.remove_missing_songs_none),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                CleanupState.Failed -> {
                    Text(
                        text = stringResource(R.string.remove_missing_songs_failed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                is CleanupState.Confirm -> {
                    TextButton(onClick = { onRemove(state.songs) }) {
                        Text(stringResource(R.string.remove_missing_songs_positive))
                    }
                }

                CleanupState.Scanning,
                CleanupState.Running,
                -> {
                    Unit
                }

                else -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close_action))
                    }
                }
            }
        },
        dismissButton = {
            when (state) {
                is CleanupState.Confirm, CleanupState.Scanning -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }

                else -> {
                    Unit
                }
            }
        },
    )
}
