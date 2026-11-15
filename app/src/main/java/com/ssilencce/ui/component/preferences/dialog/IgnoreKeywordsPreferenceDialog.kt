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

package com.ssilencce.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssilencce.R
import com.ssilencce.core.sort.IgnoreKeywordAddResult
import com.ssilencce.core.sort.IgnoreKeywordPolicy
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences

/**
 * Library settings dialog for user-defined leading keywords ignored while sorting.
 */
class IgnoreKeywordsPreferenceDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                    )
                    setContent {
                        SsilencceTheme {
                            IgnoreKeywordsScreen(onClose = { dismissAllowingStateLoss() })
                        }
                    }
                },
            ).create()
}

@Composable
private fun IgnoreKeywordsScreen(onClose: () -> Unit) {
    var keywords by remember { mutableStateOf(Preferences.sortingIgnoreKeywords.sorted()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    val maxHeight =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() * 0.85f
        }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sorting_ignore_keywords_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.sorting_ignore_keywords_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            val errorMessage = error
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        input = value
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.sorting_ignore_keywords_hint)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText =
                        errorMessage?.let { message ->
                            { Text(stringResource(message)) }
                        },
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                )
                TextButton(
                    onClick = {
                        when (val result = IgnoreKeywordPolicy.add(keywords, input)) {
                            is IgnoreKeywordAddResult.Added -> {
                                keywords = result.keywords.sorted()
                                Preferences.sortingIgnoreKeywords = result.keywords.toSet()
                                input = ""
                                error = null
                            }

                            IgnoreKeywordAddResult.Blank -> {
                                error = R.string.sorting_ignore_keywords_blank
                            }

                            IgnoreKeywordAddResult.TooLong -> {
                                error = R.string.sorting_ignore_keywords_too_long
                            }

                            IgnoreKeywordAddResult.Duplicate -> {
                                error = R.string.sorting_ignore_keywords_duplicate
                            }

                            IgnoreKeywordAddResult.Full -> {
                                error = R.string.sorting_ignore_keywords_full
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.sorting_ignore_keywords_add))
                }
            }

            if (keywords.isEmpty()) {
                Text(
                    text = stringResource(R.string.sorting_ignore_keywords_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                keywords.forEach { keyword ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = keyword,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(
                            onClick = {
                                val updated = IgnoreKeywordPolicy.remove(keywords, keyword)
                                keywords = updated.sorted()
                                Preferences.sortingIgnoreKeywords = updated.toSet()
                                error = null
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_24dp),
                                contentDescription =
                                    stringResource(
                                        R.string.sorting_ignore_keywords_remove,
                                        keyword,
                                    ),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.action_done))
                }
            }
        }
    }
}
