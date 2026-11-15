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

package com.ssilencce.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ssilencce.R
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.ShortcutEntityType
import com.ssilencce.util.ShortcutHelper
import com.ssilencce.util.m3u.PlaylistExportExecutor
import com.ssilencce.util.m3u.PlaylistExportFormat
import java.util.UUID

/**
 * Small Compose dialogs for short, non-blocking choices and confirmations.
 *
 * Result-producing dialogs use the FragmentResult API with a stable request key. Callers register
 * that key in onCreate, so a dialog restored by configuration change still finds its listener.
 * Export and shortcut confirmations are action-owned: all data needed to complete the action is
 * carried in dialog arguments, so their result does not depend on a transient fragment callback.
 */
class ComposeChoiceDialogFragment : DialogFragment() {
    private var exportSubmitted = false

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
                    Surface(shape = MaterialTheme.shapes.extraLarge) {
                        val containerHeight =
                            with(LocalDensity.current) {
                                LocalWindowInfo.current.containerSize.height
                                    .toDp()
                            }
                        val maxOptionsHeight = containerHeight * 0.45f
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = arguments?.getString(ARG_TITLE).orEmpty(),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            val options = arguments?.getStringArrayList(ARG_OPTIONS).orEmpty()
                            val selected = arguments?.getInt(ARG_SELECTED, -1) ?: -1
                            Column(
                                modifier =
                                    Modifier
                                        .heightIn(max = maxOptionsHeight)
                                        .verticalScroll(rememberScrollState()),
                            ) {
                                options.forEachIndexed { index, option ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = selected == index,
                                                    onClick = { submit(index) },
                                                    role = if (selected >= 0) Role.RadioButton else Role.Button,
                                                ).padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        if (selected >= 0) {
                                            RadioButton(
                                                selected = selected == index,
                                                onClick = null,
                                            )
                                        }
                                        Text(text = option, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { dismissAllowingStateLoss() }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.88f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun submit(index: Int) {
        val exportIds = arguments?.getLongArray(ARG_EXPORT_PLAYLIST_IDS)
        if (exportIds != null) {
            if (exportSubmitted) return
            exportSubmitted = true
            val token = arguments?.getString(ARG_EXPORT_TOKEN) ?: return
            val format = PlaylistExportFormat.entries.getOrNull(index) ?: return
            val appContext = requireContext().applicationContext
            dismissAllowingStateLoss()
            PlaylistExportExecutor.start(token, exportIds.toList(), format, appContext)
            return
        }
        val key = arguments?.getString(ARG_RESULT_KEY) ?: return
        parentFragmentManager.setFragmentResult(
            key,
            Bundle().apply { putInt(RESULT_SELECTED_INDEX, index) },
        )
        dismissAllowingStateLoss()
    }

    companion object {
        const val RESULT_SELECTED_INDEX = "compose_dialog_selected_index"

        private const val ARG_TITLE = "title"
        private const val ARG_OPTIONS = "options"
        private const val ARG_SELECTED = "selected"
        private const val ARG_RESULT_KEY = "result_key"
        private const val ARG_EXPORT_PLAYLIST_IDS = "export_playlist_ids"
        private const val ARG_EXPORT_TOKEN = "export_token"

        fun newInstance(
            title: String,
            options: List<String>,
            selectedIndex: Int,
            resultKey: String? = null,
            exportPlaylistIds: LongArray? = null,
            exportToken: String? = null,
        ): ComposeChoiceDialogFragment =
            ComposeChoiceDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_TITLE, title)
                        putStringArrayList(ARG_OPTIONS, ArrayList(options))
                        putInt(ARG_SELECTED, selectedIndex)
                        resultKey?.let { putString(ARG_RESULT_KEY, it) }
                        exportPlaylistIds?.let { putLongArray(ARG_EXPORT_PLAYLIST_IDS, it) }
                        exportToken?.let { putString(ARG_EXPORT_TOKEN, it) }
                    }
            }
    }
}

class ComposeConfirmDialogFragment : DialogFragment() {
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
                    Surface(shape = MaterialTheme.shapes.extraLarge) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = arguments?.getString(ARG_TITLE).orEmpty(),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = arguments?.getString(ARG_MESSAGE).orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { dismissAllowingStateLoss() }) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                                TextButton(onClick = { submit() }) {
                                    Text(arguments?.getString(ARG_POSITIVE).orEmpty())
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.88f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun submit() {
        addShortcut(
            type =
                arguments?.getString(ARG_SHORTCUT_TYPE)?.let { name ->
                    ShortcutEntityType.entries.firstOrNull { it.name == name }
                },
            id = arguments?.getString(ARG_SHORTCUT_ID).orEmpty(),
            name = arguments?.getString(ARG_SHORTCUT_NAME).orEmpty(),
            path = arguments?.getString(ARG_SHORTCUT_PATH),
        )
        dismissAllowingStateLoss()
    }

    private fun addShortcut(
        type: ShortcutEntityType?,
        id: String,
        name: String,
        path: String?,
    ) {
        if (type == null || id.isBlank() || name.isBlank()) return
        val context = requireContext().applicationContext
        val added = ShortcutHelper.add(context, type, id, name, path)
        Toast
            .makeText(
                context,
                if (added) R.string.shortcut_added else R.string.shortcut_limit_reached,
                Toast.LENGTH_SHORT,
            ).show()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_SHORTCUT_TYPE = "shortcut_type"
        private const val ARG_SHORTCUT_ID = "shortcut_id"
        private const val ARG_SHORTCUT_NAME = "shortcut_name"
        private const val ARG_SHORTCUT_PATH = "shortcut_path"

        fun newInstance(
            title: String,
            message: String,
            positive: String,
            shortcutType: ShortcutEntityType? = null,
            shortcutId: String? = null,
            shortcutName: String? = null,
            shortcutPath: String? = null,
        ): ComposeConfirmDialogFragment =
            ComposeConfirmDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_TITLE, title)
                        putString(ARG_MESSAGE, message)
                        putString(ARG_POSITIVE, positive)
                        shortcutType?.let {
                            putString(ARG_SHORTCUT_TYPE, it.name)
                            putString(ARG_SHORTCUT_ID, shortcutId)
                            putString(ARG_SHORTCUT_NAME, shortcutName)
                            putString(ARG_SHORTCUT_PATH, shortcutPath)
                        }
                    }
            }
    }
}

fun Fragment.showComposeChoice(
    title: String,
    options: List<String>,
    selectedIndex: Int = -1,
    resultKey: String,
) {
    ComposeChoiceDialogFragment
        .newInstance(title, options, selectedIndex, resultKey)
        .show(parentFragmentManager, "compose-choice-$resultKey")
}

fun Fragment.showExportFormatChoice(playlistIds: List<Long>) {
    val formats = PlaylistExportFormat.entries
    ComposeChoiceDialogFragment
        .newInstance(
            title = getString(R.string.export_format_title),
            options = formats.map { getString(it.titleRes) },
            selectedIndex = -1,
            exportPlaylistIds = playlistIds.toLongArray(),
            exportToken = UUID.randomUUID().toString(),
        ).show(parentFragmentManager, "compose-export-format")
}

fun Fragment.showShortcutConfirm(
    type: ShortcutEntityType,
    id: String,
    name: String,
    path: String? = null,
) {
    ComposeConfirmDialogFragment
        .newInstance(
            title = getString(R.string.shortcut_confirm_title),
            message = getString(R.string.shortcut_confirm_message, name),
            positive = getString(R.string.add_to_home),
            shortcutType = type,
            shortcutId = id,
            shortcutName = name,
            shortcutPath = path,
        ).show(parentFragmentManager, "compose-shortcut-confirm")
}
