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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.data.local.room.InclExclDao
import com.ssilencce.data.local.room.InclExclEntity
import com.ssilencce.data.model.Song
import com.ssilencce.extensions.files.getCanonicalPathSafe
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.library.ReloadType
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.io.File

/**
 * Adds the selected files to the existing path blacklist. Hidden files stay on the device and
 * remain reversible from Settings > Library > Blacklist.
 */
class HideSongsDialog : DialogFragment() {
    private val inclExclDao: InclExclDao by inject()
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private val songs: List<Song>
        get() =
            BundleCompat
                .getParcelableArrayList(requireArguments(), ARG_SONGS, Song::class.java)
                .orEmpty()

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
                    HideSongsContent(
                        songs = songs,
                        onConfirm = { hideSongs() },
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    private fun hideSongs() {
        val files = songs.map { it.data }.filter { it.isNotBlank() }.distinct()
        if (files.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                inclExclDao.insertPaths(
                    files.map {
                        InclExclEntity(
                            File(it).getCanonicalPathSafe(),
                            InclExclDao.BLACKLIST,
                        )
                    },
                )
            }
            val context = context?.applicationContext ?: return@launch
            val message =
                if (Preferences.blacklistEnabled) {
                    context.resources.getQuantityString(
                        R.plurals.x_songs_hidden,
                        files.size,
                        files.size,
                    )
                } else {
                    context.getString(R.string.songs_hidden_blacklist_disabled)
                }
            context.showToast(message)
            libraryViewModel.forceReload(ReloadType.Folders)
            libraryViewModel.forceReload(ReloadType.Songs)
            notifyMediaStoreChanged(context)
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val ARG_SONGS = "songs"
        private const val TAG = "HideSongsDialog"

        fun show(
            fragment: Fragment,
            songs: List<Song>,
        ) {
            if (songs.isEmpty()) return
            HideSongsDialog()
                .apply {
                    arguments =
                        Bundle().apply {
                            putParcelableArrayList(ARG_SONGS, ArrayList(songs))
                        }
                }.show(fragment.childFragmentManager, TAG)
        }
    }
}

@Composable
private fun HideSongsContent(
    songs: List<Song>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hide_songs_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text =
                        if (songs.size == 1) {
                            stringResource(R.string.hide_the_song_x, songs.first().title)
                        } else {
                            stringResource(R.string.hide_x_songs, songs.size)
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.hide_songs_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.hide_songs_positive))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
