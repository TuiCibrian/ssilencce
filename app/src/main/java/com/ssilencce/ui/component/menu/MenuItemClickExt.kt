/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.ssilencce.ui.component.menu

import android.content.Intent
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.core.model.shuffle.OpenShuffleMode
import com.ssilencce.data.local.EditTarget
import com.ssilencce.data.local.room.PlaylistWithSongs
import com.ssilencce.data.mapper.toSongs
import com.ssilencce.data.model.Album
import com.ssilencce.data.model.Artist
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.getShareSongIntent
import com.ssilencce.extensions.getShareSongsIntent
import com.ssilencce.extensions.navigation.albumDetailArgs
import com.ssilencce.extensions.navigation.artistDetailArgs
import com.ssilencce.extensions.navigation.findActivityNavController
import com.ssilencce.extensions.navigation.genreDetailArgs
import com.ssilencce.extensions.navigation.songDetailArgs
import com.ssilencce.extensions.showToast
import com.ssilencce.extensions.toChooser
import com.ssilencce.ui.component.base.AbsTagEditorActivity
import com.ssilencce.ui.dialogs.playlists.AddToPlaylistDialog
import com.ssilencce.ui.dialogs.playlists.DeletePlaylistDialog
import com.ssilencce.ui.dialogs.playlists.EditPlaylistDialog
import com.ssilencce.ui.dialogs.showExportFormatChoice
import com.ssilencce.ui.dialogs.showShortcutConfirm
import com.ssilencce.ui.dialogs.songs.DeleteSongsDialog
import com.ssilencce.ui.screen.audiocutter.AudioCutterActivity
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.library.folders.FolderTransferDialog
import com.ssilencce.ui.screen.library.folders.HideSongsDialog
import com.ssilencce.ui.screen.notes.SongNotesFragmentArgs
import com.ssilencce.ui.screen.miniplayer.MiniPlayerActivity
import com.ssilencce.ui.screen.player.PlayerViewModel
import com.ssilencce.ui.screen.replaygain.ReplayGainScanActivity
import com.ssilencce.ui.screen.tageditor.BatchTagEditorActivity
import com.ssilencce.ui.screen.tageditor.BatchTagEditorViewModel
import com.ssilencce.ui.screen.tageditor.SongTagEditorActivity
import com.ssilencce.util.ShortcutEntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.getActivityViewModel

fun Song.onSongMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean {
    if (id == -1L) {
        return false
    }
    return when (menuItem.itemId) {
        R.id.action_preview -> {
            fragment.startActivity(MiniPlayerActivity.previewIntent(fragment.requireContext(), this))
            true
        }

        R.id.action_play_next -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            playerViewModel.queueNext(this)
            fragment.showToast(R.string.added_title_to_playing_queue)
            true
        }

        R.id.action_add_to_playing_queue -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            playerViewModel.enqueue(this)
            fragment.showToast(R.string.added_title_to_playing_queue)
            true
        }

        R.id.action_pause_after_this_song -> {
            fragment.getActivityViewModel<PlayerViewModel>().pauseAfterSong(this)
            true
        }

        R.id.action_play_after_current -> {
            fragment.getActivityViewModel<PlayerViewModel>().playAfterCurrentSong(this)
            true
        }

        R.id.action_load_next_and_pause -> {
            fragment.getActivityViewModel<PlayerViewModel>().loadNextSongAndPause()
            true
        }

        R.id.action_add_to_playlist -> {
            AddToPlaylistDialog
                .create(this)
                .show(fragment.childFragmentManager, "ADD_PLAYLIST")
            true
        }

        R.id.action_go_to_album -> {
            val navController = fragment.findActivityNavController(R.id.fragment_container)
            navController.navigate(R.id.nav_album_detail, albumDetailArgs(this.albumId))
            true
        }

        R.id.action_go_to_artist -> {
            val navController = fragment.findActivityNavController(R.id.fragment_container)
            navController.navigate(R.id.nav_artist_detail, artistDetailArgs(this))
            true
        }

        R.id.action_go_to_genre -> {
            val libraryViewModel = fragment.getActivityViewModel<LibraryViewModel>()
            libraryViewModel.genreBySong(this).observe(fragment.viewLifecycleOwner) {
                val navController = fragment.findActivityNavController(R.id.fragment_container)
                navController.navigate(R.id.nav_genre_detail, genreDetailArgs(it))
            }
            true
        }

        R.id.action_share -> {
            fragment.startActivity(
                fragment
                    .requireContext()
                    .getShareSongIntent(this)
                    .toChooser(fragment.getString(R.string.action_share)),
            )
            true
        }

        R.id.action_details -> {
            fragment
                .findActivityNavController(R.id.fragment_container)
                .navigate(R.id.nav_song_details, songDetailArgs(this))
            true
        }

        R.id.action_song_notes -> {
            fragment
                .findActivityNavController(R.id.fragment_container)
                .navigate(R.id.nav_song_notes, SongNotesFragmentArgs.Builder(this).build().toBundle())
            true
        }

        R.id.action_tag_editor -> {
            val tagEditorIntent =
                Intent(fragment.requireContext(), SongTagEditorActivity::class.java)
            tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_TARGET, EditTarget.song(this))
            fragment.startActivity(tagEditorIntent)
            true
        }

        R.id.action_calculate_replay_gain -> {
            fragment.startActivity(
                ReplayGainScanActivity.intent(fragment.requireContext(), longArrayOf(id)),
            )
            true
        }

        R.id.action_trim_audio -> {
            fragment.startActivity(
                AudioCutterActivity.intent(fragment.requireContext(), id),
            )
            true
        }

        R.id.action_delete_from_device -> {
            DeleteSongsDialog.create(this).show(fragment.childFragmentManager, "DELETE_SONGS")
            true
        }

        R.id.action_move_to_folder -> {
            FolderTransferDialog.show(fragment, listOf(this), copy = false)
            true
        }

        R.id.action_copy_to_folder -> {
            FolderTransferDialog.show(fragment, listOf(this), copy = true)
            true
        }

        R.id.action_hide_songs -> {
            HideSongsDialog.show(fragment, listOf(this))
            true
        }

        else -> {
            false
        }
    }
}

fun List<Song>.onSongsMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean {
    if (isEmpty()) {
        return false
    }
    return when (menuItem.itemId) {
        R.id.action_play -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            playerViewModel.openQueue(this, shuffleMode = OpenShuffleMode.Off)
            true
        }

        R.id.action_shuffle_play -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            playerViewModel.openAndShuffleQueue(this)
            true
        }

        R.id.action_shuffle_play_next -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            val shuffled = shuffled()
            playerViewModel.queueNext(shuffled)
            if (size == 1) {
                fragment.showToast(R.string.added_title_to_playing_queue)
            } else {
                fragment.showToast(fragment.getString(R.string.added_x_titles_to_playing_queue, size))
            }
            true
        }

        R.id.action_shuffle_add_to_queue -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            val shuffled = shuffled()
            playerViewModel.enqueue(shuffled)
            if (size == 1) {
                fragment.showToast(R.string.added_title_to_playing_queue)
            } else {
                fragment.showToast(fragment.getString(R.string.added_x_titles_to_playing_queue, size))
            }
            true
        }

        R.id.action_play_next -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            playerViewModel.queueNext(this)
            if (size == 1) {
                fragment.showToast(R.string.added_title_to_playing_queue)
            } else {
                fragment.showToast(fragment.getString(R.string.added_x_titles_to_playing_queue, size))
            }
            true
        }

        R.id.action_add_to_playing_queue -> {
            val playerViewModel = fragment.getActivityViewModel<PlayerViewModel>()
            playerViewModel.enqueue(this)
            if (size == 1) {
                fragment.showToast(R.string.added_title_to_playing_queue)
            } else {
                fragment.showToast(fragment.getString(R.string.added_x_titles_to_playing_queue, size))
            }
            true
        }

        R.id.action_add_to_playlist -> {
            AddToPlaylistDialog
                .create(this)
                .show(fragment.childFragmentManager, "ADD_PLAYLIST")
            true
        }

        R.id.action_share -> {
            fragment.startActivity(
                fragment
                    .requireContext()
                    .getShareSongsIntent(this)
                    .toChooser(fragment.getString(R.string.action_share)),
            )
            true
        }

        R.id.action_tag_editor -> {
            launchBatchTagEditor(fragment, map { it.id })
            true
        }

        R.id.action_calculate_replay_gain -> {
            fragment.startActivity(
                ReplayGainScanActivity.intent(
                    fragment.requireContext(),
                    map { it.id }.toLongArray(),
                ),
            )
            true
        }

        R.id.action_trim_audio -> {
            if (size == 1) {
                fragment.startActivity(
                    AudioCutterActivity.intent(fragment.requireContext(), first().id),
                )
            } else {
                fragment.showToast(R.string.audio_cutter_select_single)
            }
            true
        }

        R.id.action_delete_from_device -> {
            DeleteSongsDialog.create(this).show(fragment.childFragmentManager, "DELETE_SONGS")
            true
        }

        R.id.action_move_to_folder -> {
            FolderTransferDialog.show(fragment, this, copy = false)
            true
        }

        R.id.action_copy_to_folder -> {
            FolderTransferDialog.show(fragment, this, copy = true)
            true
        }

        R.id.action_hide_songs -> {
            HideSongsDialog.show(fragment, this)
            true
        }

        R.id.action_song_notes -> {
            val song = singleOrNull() ?: return false
            fragment
                .findActivityNavController(R.id.fragment_container)
                .navigate(R.id.nav_song_notes, SongNotesFragmentArgs.Builder(song).build().toBundle())
            true
        }

        else -> {
            false
        }
    }
}

private fun launchBatchTagEditor(
    fragment: Fragment,
    songIds: List<Long>,
) {
    val ids = songIds.filter { it != -1L }.distinct()
    when {
        ids.isEmpty() -> {
            fragment.showToast(R.string.batch_tag_editor_empty)
        }

        ids.size > BatchTagEditorViewModel.MAX_BATCH_SONGS -> {
            fragment.showToast(
                fragment.getString(
                    R.string.batch_tag_editor_too_many,
                    BatchTagEditorViewModel.MAX_BATCH_SONGS,
                ),
            )
        }

        else -> {
            fragment.startActivity(
                BatchTagEditorActivity.intent(fragment.requireContext(), ids.toLongArray()),
            )
        }
    }
}

fun Album.onAlbumMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean =
    when (menuItem.itemId) {
        R.id.action_go_to_artist -> {
            fragment
                .findActivityNavController(R.id.fragment_container)
                .navigate(R.id.nav_artist_detail, artistDetailArgs(this))
            true
        }

        R.id.action_tag_editor -> {
            launchBatchTagEditor(fragment, songs.map { it.id })
            true
        }

        R.id.action_add_to_home -> {
            showAddShortcutDialog(fragment, ShortcutEntityType.Album, id.toString(), name)
            true
        }

        else -> {
            songs.onSongsMenu(fragment, menuItem)
        }
    }

fun List<Album>.onAlbumsMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean {
    fragment.lifecycleScope.launch(Dispatchers.IO) {
        val songs = flatMap { it.songs }
        withContext(Dispatchers.Main) {
            songs.onSongsMenu(fragment, menuItem)
        }
    }
    return true
}

fun Artist.onArtistMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean =
    when (menuItem.itemId) {
        R.id.action_tag_editor -> {
            if (composerName != null) {
                false
            } else {
                launchBatchTagEditor(fragment, songs.map { it.id })
                true
            }
        }

        R.id.action_add_to_home -> {
            if (composerName != null) {
                false
            } else {
                showAddShortcutDialog(fragment, ShortcutEntityType.Artist, id.toString(), name)
                true
            }
        }

        else -> {
            songs.onSongsMenu(fragment, menuItem)
        }
    }

fun List<Artist>.onArtistsMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean {
    fragment.lifecycleScope.launch(Dispatchers.IO) {
        val songs = flatMap { it.songs }
        withContext(Dispatchers.Main) {
            songs.onSongsMenu(fragment, menuItem)
        }
    }
    return true
}

fun PlaylistWithSongs.onPlaylistMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean {
    if (this == PlaylistWithSongs.Empty) {
        return false
    }

    when (menuItem.itemId) {
        // R.id.action_rename_playlist removed as obsolete

        R.id.action_edit_playlist -> {
            EditPlaylistDialog
                .create(playlistEntity)
                .show(fragment.childFragmentManager, "EDIT_PLAYLIST")
            return true
        }

        R.id.action_delete_playlist -> {
            DeletePlaylistDialog.create(this).show(fragment.childFragmentManager, "DELETE_PLAYLIST")
            return true
        }

        R.id.action_export_playlist -> {
            showExportFormatDialog(fragment, listOf(this))
            return true
        }

        R.id.action_add_to_home -> {
            showAddShortcutDialog(
                fragment,
                ShortcutEntityType.Playlist,
                playlistEntity.playListId.toString(),
                playlistEntity.playlistName,
            )
            return true
        }
    }
    return songs.toSongs().onSongsMenu(fragment, menuItem)
}

fun List<PlaylistWithSongs>.onPlaylistsMenu(
    fragment: Fragment,
    menuItem: MenuItem,
): Boolean {
    when (menuItem.itemId) {
        R.id.action_delete_playlist -> {
            DeletePlaylistDialog
                .create(this)
                .show(fragment.childFragmentManager, "DELETE_PLAYLISTS")
            return true
        }

        R.id.action_export_playlist -> {
            showExportFormatDialog(fragment, this@onPlaylistsMenu)
            return true
        }
    }
    return flatMap { it.songs.toSongs() }.onSongsMenu(fragment, menuItem)
}

private fun showExportFormatDialog(
    fragment: Fragment,
    playlists: List<PlaylistWithSongs>,
) {
    fragment.showExportFormatChoice(playlists.map { it.playlistEntity.playListId })
}

fun showAddShortcutDialog(
    fragment: Fragment,
    type: ShortcutEntityType,
    id: String,
    name: String,
    path: String? = null,
) {
    fragment.showShortcutConfirm(type, id, name, path)
}
