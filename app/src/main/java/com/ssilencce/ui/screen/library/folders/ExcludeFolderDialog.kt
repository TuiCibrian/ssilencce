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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.data.local.room.InclExclDao
import com.ssilencce.data.local.room.InclExclEntity
import com.ssilencce.extensions.files.getCanonicalPathSafe
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.library.ReloadType
import com.ssilencce.ui.theme.SsilencceTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.io.File

/**
 * Confirms and writes a folder path into the existing blacklist. Used both from the folder list
 * item menu and for the folder currently open in the Folders tab.
 */
class ExcludeFolderDialog : DialogFragment() {
    private val inclExclDao: InclExclDao by inject()
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private val folderPath: String
        get() = requireArguments().getString(ARG_PATH).orEmpty()

    private val folderName: String
        get() = requireArguments().getString(ARG_NAME).orEmpty()

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
                    ExcludeFolderContent(
                        folderName = folderName,
                        onConfirm = { excludeFolder() },
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    private fun excludeFolder() {
        if (folderPath.isBlank()) {
            dismissAllowingStateLoss()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                inclExclDao.insertPath(
                    InclExclEntity(
                        File(folderPath).getCanonicalPathSafe(),
                        InclExclDao.BLACKLIST,
                    ),
                )
            }
            val appContext = context?.applicationContext ?: return@launch
            appContext.showToast(R.string.folder_excluded)
            libraryViewModel.forceReload(ReloadType.Folders)
            notifyMediaStoreChanged(appContext)
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_NAME = "name"
        private const val TAG = "ExcludeFolderDialog"

        fun show(
            fragment: Fragment,
            path: String,
            name: String,
        ) {
            if (path.isBlank()) return
            ExcludeFolderDialog()
                .apply {
                    arguments =
                        Bundle().apply {
                            putString(ARG_PATH, path)
                            putString(ARG_NAME, name)
                        }
                }.show(fragment.childFragmentManager, TAG)
        }
    }
}

@Composable
private fun ExcludeFolderContent(
    folderName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exclude_folder_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.exclude_folder_message, folderName),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.exclude_folder_positive))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
