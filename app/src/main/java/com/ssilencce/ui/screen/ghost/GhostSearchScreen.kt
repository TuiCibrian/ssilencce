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

package com.ssilencce.ui.screen.ghost

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssilencce.R
import com.ssilencce.data.model.Song
import com.ssilencce.ui.component.compose.CollapsibleAppBarScaffold
import com.ssilencce.ui.component.compose.QuickSearchBar

@Composable
fun GhostSearchRoute(
    viewModel: GhostSearchViewModel,
    locked: Boolean,
    onUnlock: (String) -> Boolean,
    onBackClick: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GhostSearchScreen(
        state = state,
        locked = locked,
        onUnlock = onUnlock,
        onBackClick = onBackClick,
        onQueryChange = viewModel::setQuery,
        onPlay = onPlay,
    )
}

@Composable
private fun GhostSearchScreen(
    state: GhostSearchUiState,
    locked: Boolean,
    onUnlock: (String) -> Boolean,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
) {
    CollapsibleAppBarScaffold(
        title = stringResource(R.string.ghost_search_title),
        onBackClick = onBackClick,
    ) { contentPadding ->
        if (locked) {
            GhostPinGate(
                contentPadding = contentPadding,
                onUnlock = onUnlock,
            )
            return@CollapsibleAppBarScaffold
        }

        val visibleSongs =
            remember(state.songs, state.query) {
                val query = state.query.trim()
                if (query.isEmpty()) {
                    state.songs
                } else {
                    state.songs.filter { song -> song.matchesGhostQuery(query) }
                }
            }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
        ) {
            if (state.hasFolders && state.enabled) {
                QuickSearchBar(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            when {
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !state.hasFolders -> {
                    GhostEmptyState(
                        title = stringResource(R.string.ghost_search_not_configured_title),
                        message = stringResource(R.string.ghost_search_not_configured_message),
                    )
                }

                !state.enabled -> {
                    GhostEmptyState(
                        title = stringResource(R.string.ghost_search_disabled_title),
                        message = stringResource(R.string.ghost_search_disabled_message),
                    )
                }

                state.songs.isEmpty() -> {
                    GhostEmptyState(
                        title = stringResource(R.string.ghost_search_empty_title),
                        message = stringResource(R.string.ghost_search_empty_message),
                    )
                }

                visibleSongs.isEmpty() -> {
                    GhostEmptyState(
                        title = stringResource(R.string.ghost_search_no_match_title),
                        message = stringResource(R.string.ghost_search_no_match_message),
                    )
                }

                else -> {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.ghost_song_count,
                                state.songs.size,
                                state.songs.size,
                            ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(items = visibleSongs, key = { it.id }) { song ->
                            GhostSongRow(
                                song = song,
                                onPlay = {
                                    onPlay(visibleSongs, visibleSongs.indexOf(song))
                                },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostSongRow(
    song: Song,
    onPlay: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text =
                    listOf(song.artistName, song.albumName)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_music_note_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            IconButton(onClick = onPlay) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_24dp),
                    contentDescription = stringResource(R.string.action_play),
                )
            }
        },
        modifier = Modifier.clickable(onClick = onPlay),
    )
}

@Composable
private fun GhostEmptyState(
    title: String,
    message: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_visibility_off_24dp),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GhostPinGate(
    contentPadding: PaddingValues,
    onUnlock: (String) -> Boolean,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val unlock = {
        if (!onUnlock(pin)) {
            error = true
        }
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock_24dp),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.ghost_search_locked_title),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.ghost_search_locked_message),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH)
                error = false
            },
            modifier = Modifier.padding(top = 24.dp),
            label = { Text(stringResource(R.string.ghost_pin_label)) },
            singleLine = true,
            isError = error,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { unlock() }),
        )
        if (error) {
            Text(
                text = stringResource(R.string.ghost_pin_wrong),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = unlock,
            modifier = Modifier.padding(top = 20.dp),
            enabled = pin.length >= MIN_PIN_LENGTH,
        ) {
            Text(text = stringResource(R.string.ghost_search_unlock))
        }
    }
}

private fun Song.matchesGhostQuery(query: String): Boolean =
    listOf(title, artistName, albumName, albumArtistName.orEmpty(), fileName, data)
        .any { it.contains(query, ignoreCase = true) }

const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 12
