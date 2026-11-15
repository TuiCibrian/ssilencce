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

package com.ssilencce.ui.screen.settings.backup

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ssilencce.R
import com.ssilencce.extensions.showToast
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.backup.BackupShareStaging
import java.io.File

/**
 * Confirmation sheet shown only after a successful manual backup. Shares the staged cache copy;
 * it never creates or modifies a backup.
 */
class BackupShareDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.let {
            it.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
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
                    BackupShareSheet(
                        fileName = arguments?.getString(ARG_FILE_NAME).orEmpty(),
                        onShare = ::shareBackup,
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    private fun shareBackup() {
        val context = context ?: return
        val args = arguments
        val uri = args?.getString(ARG_URI)?.let { Uri.parse(it) }
        val file = args?.getString(ARG_FILE_PATH)?.let { File(it) }
        if (uri == null || file == null) {
            context.showToast(R.string.backup_share_failed)
            dismissAllowingStateLoss()
            return
        }
        if (!BackupShareStaging.isShareable(context, file)) {
            context.showToast(R.string.backup_share_failed)
            dismissAllowingStateLoss()
            return
        }
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        try {
            startActivity(Intent.createChooser(sendIntent, getString(R.string.backup_share_action)))
            dismissAllowingStateLoss()
        } catch (error: ActivityNotFoundException) {
            context.showToast(R.string.backup_share_unavailable)
        } catch (error: Exception) {
            Log.e(TAG, "Could not launch the backup share chooser", error)
            context.showToast(R.string.backup_share_failed)
        }
    }

    companion object {
        const val TAG = "BackupShareDialog"

        private const val ARG_URI = "arg_uri"
        private const val ARG_FILE_PATH = "arg_file_path"
        private const val ARG_FILE_NAME = "arg_file_name"
        private const val MIME_TYPE = "application/zip"

        fun newInstance(
            uri: Uri,
            file: File,
            displayName: String,
        ): BackupShareDialogFragment =
            BackupShareDialogFragment().apply {
                arguments =
                    bundleOf(
                        ARG_URI to uri.toString(),
                        ARG_FILE_PATH to file.absolutePath,
                        ARG_FILE_NAME to displayName,
                    )
            }
    }
}

@Composable
private fun BackupShareSheet(
    fileName: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.backup_share_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.backup_share_message),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (fileName.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.backup_share_disclosure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
            Button(
                onClick = onShare,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(stringResource(R.string.backup_share_action))
            }
        }
    }
}
