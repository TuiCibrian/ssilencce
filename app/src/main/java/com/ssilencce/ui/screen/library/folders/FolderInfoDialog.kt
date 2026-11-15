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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ssilencce.R
import com.ssilencce.core.model.filesystem.FolderInfo
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.extensions.files.asReadableFileSize
import com.ssilencce.extensions.files.getPrettyAbsolutePath
import com.ssilencce.extensions.media.asReadableDuration
import com.ssilencce.extensions.utilities.dateStr
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.FolderInfoProvider
import org.koin.android.ext.android.inject
import java.io.File

/**
 * Read-only summary of a folder: path, direct songs, sub-folders, total duration, size and the
 * most recent modification time.
 */
class FolderInfoDialog : DialogFragment() {
    private val repository: Repository by inject()

    private val folderPath: String
        get() = requireArguments().getString(ARG_PATH).orEmpty()

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
                    val info by
                        produceState<FolderInfo?>(initialValue = null, folderPath) {
                            value =
                                runCatching {
                                    FolderInfoProvider.load(repository, folderPath)
                                }.getOrNull()
                        }
                    FolderInfoContent(info = info, onDismiss = { dismissAllowingStateLoss() })
                }
            }
        }

    companion object {
        private const val ARG_PATH = "folder_path"
        private const val TAG = "FolderInfoDialog"

        fun show(
            fragment: Fragment,
            path: String,
        ) {
            FolderInfoDialog()
                .apply { arguments = Bundle().apply { putString(ARG_PATH, path) } }
                .show(fragment.childFragmentManager, TAG)
        }
    }
}

@Composable
private fun FolderInfoContent(
    info: FolderInfo?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_info_title)) },
        text = {
            if (info == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(
                        label = stringResource(R.string.folder_info_path),
                        value = File(info.path).getPrettyAbsolutePath(),
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_songs),
                        value = info.songCount.toString(),
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_total_songs),
                        value = info.totalSongCount.toString(),
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_subfolders),
                        value = info.subFolderCount.toString(),
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_total_duration),
                        value = info.totalDurationMs.asReadableDuration(),
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_total_size),
                        value = info.totalSizeBytes.asReadableFileSize(),
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_last_modified),
                        value =
                            if (info.lastModifiedSeconds > 0) {
                                context.dateStr(info.lastModifiedSeconds * MILLIS_PER_SECOND)
                            } else {
                                stringResource(R.string.folder_info_unknown)
                            },
                    )
                    InfoRow(
                        label = stringResource(R.string.folder_info_created),
                        value =
                            if (info.createdSeconds > 0) {
                                context.dateStr(info.createdSeconds * MILLIS_PER_SECOND)
                            } else {
                                stringResource(R.string.folder_info_unknown)
                            },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_action))
            }
        },
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val MILLIS_PER_SECOND = 1000L
