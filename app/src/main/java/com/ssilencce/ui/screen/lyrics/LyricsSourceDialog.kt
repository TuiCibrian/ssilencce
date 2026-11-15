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

package com.ssilencce.ui.screen.lyrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.data.model.lyrics.LyricsSourceChoice
import com.ssilencce.data.model.lyrics.LyricsSourceSelection
import com.ssilencce.ui.component.compose.DialogListItemWithRadio

/**
 * Per-song lyrics source picker. Radio items apply immediately; the file row opens SAF and the
 * resulting document becomes the song's chosen lyrics file.
 */
@Composable
fun LyricsSourceDialog(
    selection: LyricsSourceSelection,
    onDismissRequest: () -> Unit,
    onSourceSelected: (LyricsSourceChoice) -> Unit,
    onPickFile: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.lyrics_source_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (choice in LyricsSourceChoice.entries) {
                    if (choice == LyricsSourceChoice.File) continue
                    DialogListItemWithRadio(
                        title = stringResource(choice.titleRes),
                        subtitle = stringResource(choice.summaryRes),
                        onClick = { onSourceSelected(choice) },
                        isSelected = selection.source == choice,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                DialogListItemWithRadio(
                    title = stringResource(R.string.lyrics_source_pick_file),
                    subtitle = selection.fileName ?: stringResource(R.string.lyrics_source_file_summary),
                    onClick = onPickFile,
                    isSelected = selection.source == LyricsSourceChoice.File,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close_action))
            }
        },
    )
}
