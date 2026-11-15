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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ssilencce.R
import com.ssilencce.data.model.notes.SongNotesCodec
import com.ssilencce.extensions.materialSharedAxis
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.screen.MainActivity
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.theme.SsilencceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SongNotesFragment : Fragment() {
    private val arguments by navArgs<SongNotesFragmentArgs>()
    private val playerViewModel: PlayerViewModel by activityViewModel()
    private val viewModel: SongNotesViewModel by viewModel {
        parametersOf(arguments.extraSong)
    }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            val context = context ?: return@registerForActivityResult
            lifecycleScope.launch {
                val text =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                        }.getOrNull()
                    }
                if (text != null) {
                    viewModel.importText(text)
                } else {
                    viewModel.reportImportFailure()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext())
            .apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    SsilencceTheme {
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        val currentSong by playerViewModel.currentSongFlow.collectAsStateWithLifecycle()
                        val progress by playerViewModel.progressFlow.collectAsStateWithLifecycle()
                        val context = LocalContext.current
                        val resources = LocalResources.current
                        val currentResources by rememberUpdatedState(resources)
                        LaunchedEffect(Unit) {
                            viewModel.events.collect { event ->
                                when (event) {
                                    is SongNotesEvent.BookmarkAdded -> {
                                        context.showToast(
                                            currentResources.getString(
                                                R.string.song_notes_bookmark_added,
                                                event.timestampMs.asReadableDuration(),
                                            ),
                                        )
                                    }

                                    SongNotesEvent.NotesSaved -> {
                                        context.showToast(R.string.song_notes_saved)
                                    }

                                    SongNotesEvent.BookmarkDeleted -> {
                                        context.showToast(R.string.song_notes_bookmark_deleted)
                                    }

                                    SongNotesEvent.Imported -> {
                                        context.showToast(R.string.song_notes_imported)
                                    }

                                    SongNotesEvent.ImportedNothing -> {
                                        context.showToast(R.string.song_notes_import_nothing)
                                    }

                                    SongNotesEvent.Failed -> {
                                        context.showToast(R.string.song_notes_update_failed)
                                    }
                                }
                            }
                        }
                        SongNotesScreen(
                            song = arguments.extraSong,
                            state = state,
                            isCurrentSong = currentSong.id == arguments.extraSong.id,
                            currentPositionMs = progress.coerceAtLeast(0L),
                            onBackClick = { findNavController().navigateUp() },
                            onNotesChanged = viewModel::onNotesChanged,
                            onSaveNotes = viewModel::saveNotes,
                            onAddBookmark = viewModel::addBookmark,
                            onUpdateBookmark = viewModel::updateBookmark,
                            onDeleteBookmark = viewModel::deleteBookmark,
                            onSeekBookmark = ::seekToBookmark,
                            onImportClick = { importLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                            onShareClick = {
                                shareNotes(
                                    SongNotesCodec.shareText(
                                        title = arguments.extraSong.title,
                                        artist = arguments.extraSong.artistName,
                                        notes = state.notes,
                                        bookmarks = state.bookmarks,
                                    ),
                                )
                            },
                            onRetryClick = viewModel::reload,
                        )
                    }
                }
            }.also {
                materialSharedAxis(it)
            }

    override fun onDestroyView() {
        super.onDestroyView()
        if (arguments.fromPlayer && playerViewModel.queue.isNotEmpty()) {
            (activity as? MainActivity)?.expandPanel()
        }
    }

    private fun seekToBookmark(positionMs: Long) {
        if (playerViewModel.currentSong.id != arguments.extraSong.id) {
            requireContext().showToast(R.string.song_notes_start_song_hint)
            return
        }
        playerViewModel.seekTo(positionMs)
        findNavController().navigateUp()
    }

    private fun shareNotes(text: String) {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        runCatching {
            startActivity(Intent.createChooser(intent, getString(R.string.song_notes_share)))
        }
    }
}
