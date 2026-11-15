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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.data.local.repository.SongRepository
import com.ssilencce.data.model.Song
import com.ssilencce.util.GhostMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GhostSearchUiState(
    val loading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val query: String = "",
    val enabled: Boolean = GhostMode.isEnabled,
    val hasFolders: Boolean = GhostMode.hasFolders,
)

class GhostSearchViewModel(
    private val songRepository: SongRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GhostSearchUiState())
    val state: StateFlow<GhostSearchUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    enabled = GhostMode.isEnabled,
                    hasFolders = GhostMode.hasFolders,
                )
            }
            val songs = withContext(Dispatchers.IO) { songRepository.ghostSongs() }
            _state.update {
                it.copy(
                    loading = false,
                    songs = songs,
                    enabled = GhostMode.isEnabled,
                    hasFolders = GhostMode.hasFolders,
                )
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }
}
