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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssilencce.data.local.notes.SongNotesRepository
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.notes.SongBookmark
import com.ssilencce.data.model.notes.SongNotesCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SongNotesUiState(
    val notes: String = "",
    val savedNotes: String = "",
    val bookmarks: List<SongBookmark> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val storagePath: String = "",
) {
    val isDirty: Boolean
        get() = notes != savedNotes

    val hasContent: Boolean
        get() = notes.isNotBlank() || bookmarks.isNotEmpty()
}

sealed interface SongNotesEvent {
    data class BookmarkAdded(
        val timestampMs: Long,
    ) : SongNotesEvent

    data object NotesSaved : SongNotesEvent

    data object BookmarkDeleted : SongNotesEvent

    data object Imported : SongNotesEvent

    data object ImportedNothing : SongNotesEvent

    data object Failed : SongNotesEvent
}

class SongNotesViewModel(
    private val repository: SongNotesRepository,
    private val song: Song,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongNotesUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SongNotesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val mutationMutex = Mutex()

    init {
        reload()
    }

    fun reload() =
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val storagePath = withContext(Dispatchers.IO) { repository.notesDirectory().absolutePath }
            val loaded = repository.load(song)
            _uiState.update { state ->
                if (loaded == null) {
                    state.copy(isLoading = false, loadFailed = true, storagePath = storagePath)
                } else {
                    state.copy(
                        notes = loaded.notes,
                        savedNotes = loaded.notes,
                        bookmarks = loaded.bookmarks,
                        isLoading = false,
                        loadFailed = false,
                        storagePath = storagePath,
                    )
                }
            }
        }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun saveNotes() =
        viewModelScope.launch {
            mutationMutex.withLock {
                val state = _uiState.value
                if (state.loadFailed) return@withLock
                persist(state.notes, state.bookmarks, SongNotesEvent.NotesSaved)
            }
        }

    fun addBookmark(
        timestampMs: Long,
        text: String,
    ) = viewModelScope.launch {
        mutationMutex.withLock {
            val state = _uiState.value
            if (state.loadFailed) return@withLock
            persist(
                notes = state.notes,
                bookmarks = state.bookmarks + SongBookmark(timestampMs.coerceAtLeast(0L), text),
                successEvent = SongNotesEvent.BookmarkAdded(timestampMs.coerceAtLeast(0L)),
            )
        }
    }

    fun updateBookmark(
        index: Int,
        timestampMs: Long,
        text: String,
    ) = viewModelScope.launch {
        mutationMutex.withLock {
            val state = _uiState.value
            if (state.loadFailed || index !in state.bookmarks.indices) return@withLock
            val updated =
                state.bookmarks.mapIndexed { bookmarkIndex, bookmark ->
                    if (bookmarkIndex == index) {
                        SongBookmark(timestampMs.coerceAtLeast(0L), text)
                    } else {
                        bookmark
                    }
                }
            persist(state.notes, updated, SongNotesEvent.NotesSaved)
        }
    }

    fun deleteBookmark(index: Int) =
        viewModelScope.launch {
            mutationMutex.withLock {
                val state = _uiState.value
                if (state.loadFailed || index !in state.bookmarks.indices) return@withLock
                persist(
                    notes = state.notes,
                    bookmarks = state.bookmarks.filterIndexed { bookmarkIndex, _ -> bookmarkIndex != index },
                    successEvent = SongNotesEvent.BookmarkDeleted,
                )
            }
        }

    fun importText(text: String) =
        viewModelScope.launch {
            mutationMutex.withLock {
                val state = _uiState.value
                if (state.loadFailed) return@withLock
                val imported = SongNotesCodec.decode(text)
                if (imported.notes.isBlank() && imported.bookmarks.isEmpty()) {
                    _events.send(SongNotesEvent.ImportedNothing)
                    return@withLock
                }
                val mergedNotes = mergeNotes(state.notes, imported.notes)
                val mergedBookmarks =
                    (state.bookmarks + imported.bookmarks)
                        .distinctBy { it.positionMs to it.text }
                        .sortedBy { it.positionMs }
                persist(mergedNotes, mergedBookmarks, SongNotesEvent.Imported)
            }
        }

    fun reportImportFailure() =
        viewModelScope.launch {
            _events.send(SongNotesEvent.Failed)
        }

    private suspend fun persist(
        notes: String,
        bookmarks: List<SongBookmark>,
        successEvent: SongNotesEvent,
    ) {
        val saved = repository.save(song, notes, bookmarks)
        if (saved == null) {
            _events.send(SongNotesEvent.Failed)
            return
        }
        _uiState.update {
            it.copy(
                notes = saved.notes,
                savedNotes = saved.notes,
                bookmarks = saved.bookmarks,
                loadFailed = false,
            )
        }
        _events.send(successEvent)
    }

    private fun mergeNotes(
        current: String,
        imported: String,
    ): String =
        when {
            imported.isBlank() -> current
            current.isBlank() -> imported.trim()
            else -> current.trimEnd() + "\n\n" + imported.trim()
        }
}
