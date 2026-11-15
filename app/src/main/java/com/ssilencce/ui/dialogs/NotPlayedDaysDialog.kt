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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.ssilencce.R
import com.ssilencce.core.model.NotPlayedDaysPolicy
import com.ssilencce.ui.theme.SsilencceTheme

/**
 * Compose dialog for the arbitrary Not Recently Played window.
 *
 * Returns [RESULT_DAYS] with 0 for "use the existing history interval" or a custom day count.
 */
class NotPlayedDaysDialogFragment : DialogFragment() {
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
                        NotPlayedDaysContent(
                            currentDays = arguments?.getInt(ARG_CURRENT_DAYS, 0) ?: 0,
                            historyDescription = arguments?.getString(ARG_HISTORY_DESCRIPTION).orEmpty(),
                            onDismiss = { dismissAllowingStateLoss() },
                            onConfirm = { days -> submit(days) },
                        )
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

    private fun submit(days: Int) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putInt(RESULT_DAYS, NotPlayedDaysPolicy.normalizeDays(days))
            },
        )
        dismissAllowingStateLoss()
    }

    companion object {
        const val RESULT_KEY = "not_played_days_result"
        const val RESULT_DAYS = "not_played_days"

        private const val ARG_CURRENT_DAYS = "current_days"
        private const val ARG_HISTORY_DESCRIPTION = "history_description"

        fun newInstance(
            currentDays: Int,
            historyDescription: String,
        ): NotPlayedDaysDialogFragment =
            NotPlayedDaysDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putInt(ARG_CURRENT_DAYS, NotPlayedDaysPolicy.normalizeDays(currentDays))
                        putString(ARG_HISTORY_DESCRIPTION, historyDescription)
                    }
            }
    }
}

@Composable
private fun NotPlayedDaysContent(
    currentDays: Int,
    historyDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var useHistory by rememberSaveable {
        mutableStateOf(currentDays == NotPlayedDaysPolicy.USE_HISTORY_INTERVAL)
    }
    var daysText by rememberSaveable {
        mutableStateOf(if (currentDays > NotPlayedDaysPolicy.USE_HISTORY_INTERVAL) currentDays.toString() else "")
    }
    val parsedDays = NotPlayedDaysPolicy.parseDays(daysText)
    val showError = !useHistory && parsedDays == null

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.not_played_days_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.not_played_days_message),
            style = MaterialTheme.typography.bodyMedium,
        )
        OptionRow(
            selected = useHistory,
            onClick = { useHistory = true },
            label = stringResource(R.string.not_played_use_history_interval, historyDescription),
        )
        OptionRow(
            selected = !useHistory,
            onClick = { useHistory = false },
            label = stringResource(R.string.not_played_custom_days),
        )
        OutlinedTextField(
            value = daysText,
            onValueChange = { value -> daysText = value },
            enabled = !useHistory,
            singleLine = true,
            label = { Text(stringResource(R.string.not_played_days_label)) },
            isError = showError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        if (showError) {
            Text(
                text = stringResource(R.string.not_played_days_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
            TextButton(
                onClick = { onConfirm(if (useHistory) NotPlayedDaysPolicy.USE_HISTORY_INTERVAL else parsedDays!!) },
                enabled = useHistory || parsedDays != null,
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun OptionRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
