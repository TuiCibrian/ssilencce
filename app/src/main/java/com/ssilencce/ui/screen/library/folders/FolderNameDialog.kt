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

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ssilencce.R
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.dialogs.SAFDialog
import com.ssilencce.ui.screen.library.LibraryViewModel
import com.ssilencce.ui.screen.library.ReloadType
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.FolderFileOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Name prompt shared by "Create folder" and "Rename folder". The dialog performs the operation
 * itself and keeps it across the SAF grant flow, so callers only have to show it.
 */
class FolderNameDialog :
    DialogFragment(),
    SAFDialog.SAFResultListener {
    private val libraryViewModel: LibraryViewModel by activityViewModel()

    private var name by mutableStateOf("")
    private var errorRes by mutableStateOf<Int?>(null)
    private var working by mutableStateOf(false)
    private var safRetried = false

    private val mode: Mode
        get() = Mode.entries.getOrElse(requireArguments().getInt(ARG_MODE)) { Mode.Create }

    private val path: String
        get() = requireArguments().getString(ARG_PATH).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        if (savedInstanceState == null) {
            name = requireArguments().getString(ARG_INITIAL_NAME).orEmpty()
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SsilencceTheme {
                FolderNameContent(
                    title =
                        stringResource(
                            if (mode == Mode.Create) {
                                R.string.create_folder_title
                            } else {
                                R.string.rename_folder_title
                            },
                        ),
                    name = name,
                    errorRes = errorRes,
                    working = working,
                    onNameChange = {
                        name = it
                        errorRes = null
                    },
                    onConfirm = { submit() },
                    onDismiss = { dismissAllowingStateLoss() },
                )
            }
        }
    }

    override fun onSAFResult(treeUri: Uri?) {
        if (treeUri == null) {
            safRetried = false
            return
        }
        if (safRetried) {
            safRetried = false
            return
        }
        safRetried = true
        submit()
    }

    private fun submit() {
        if (working) return
        val folderName = name.trim()
        when {
            folderName.isEmpty() -> {
                errorRes = R.string.folder_name_empty
                return
            }

            folderName.contains('/') || folderName.contains('\\') -> {
                errorRes = R.string.folder_name_invalid
                return
            }
        }
        working = true
        errorRes = null
        lifecycleScope.launch {
            val appContext = context?.applicationContext ?: return@launch
            val operations = FolderFileOperations(appContext)
            val result =
                withContext(Dispatchers.IO) {
                    when (mode) {
                        Mode.Create -> operations.createFolder(path, folderName)
                        Mode.Rename -> operations.renameFolder(path, folderName)
                    }
                }
            working = false
            when (result) {
                is FolderFileOperations.Result.Success -> {
                    val message =
                        appContext.getString(
                            if (mode == Mode.Create) R.string.folder_created else R.string.folder_renamed,
                            folderName,
                        )
                    appContext.showToast(message)
                    libraryViewModel.forceReload(ReloadType.Folders)
                    val renamed = mode == Mode.Rename
                    dismissAllowingStateLoss()
                    if (renamed) {
                        (parentFragment as? FolderDetailFragment)?.onFolderRenamed()
                    }
                }

                FolderFileOperations.Result.NeedsSafAccess -> {
                    if (safRetried || !isAdded) {
                        errorRes = R.string.folder_operation_failed_short
                    } else {
                        SAFDialog.show(this@FolderNameDialog)
                    }
                }

                is FolderFileOperations.Result.Failure -> {
                    errorRes =
                        when (result.reason) {
                            FolderFileOperations.FailureReason.EmptyName -> R.string.folder_name_empty
                            FolderFileOperations.FailureReason.InvalidName -> R.string.folder_name_invalid
                            FolderFileOperations.FailureReason.AlreadyExists -> R.string.folder_name_exists
                            else -> R.string.folder_operation_failed_short
                        }
                }
            }
        }
    }

    enum class Mode {
        Create,
        Rename,
    }

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_PATH = "path"
        private const val ARG_INITIAL_NAME = "initial_name"
        private const val TAG = "FolderNameDialog"

        fun showCreate(
            fragment: Fragment,
            parentPath: String,
        ) {
            FolderNameDialog()
                .apply {
                    arguments =
                        Bundle().apply {
                            putInt(ARG_MODE, Mode.Create.ordinal)
                            putString(ARG_PATH, parentPath)
                        }
                }.show(fragment.childFragmentManager, TAG)
        }

        fun showRename(
            fragment: Fragment,
            folderPath: String,
            currentName: String,
        ) {
            FolderNameDialog()
                .apply {
                    arguments =
                        Bundle().apply {
                            putInt(ARG_MODE, Mode.Rename.ordinal)
                            putString(ARG_PATH, folderPath)
                            putString(ARG_INITIAL_NAME, currentName)
                        }
                }.show(fragment.childFragmentManager, TAG)
        }
    }
}

@Composable
private fun FolderNameContent(
    title: String,
    name: String,
    errorRes: Int?,
    working: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.folder_name_label)) },
                singleLine = true,
                isError = errorRes != null,
                enabled = !working,
                supportingText =
                    errorRes?.let {
                        { Text(stringResource(it)) }
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            onConfirm()
                        },
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !working) {
                Text(stringResource(if (working) R.string.saving_changes else R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
