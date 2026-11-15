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

package com.ssilencce.ui.component.compose.lyrics

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ssilencce.R

/**
 * Contextual bar shown while lyric lines are being selected. It replaces the regular top area so
 * the selection can be copied or shared without leaving the lyrics screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSelectionBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.lyrics_lines_selected, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24dp),
                    contentDescription = stringResource(R.string.close_action),
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSelectAll,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_select_all_24dp),
                    contentDescription = stringResource(R.string.select_all_title),
                )
            }
            IconButton(
                onClick = onCopy,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_content_copy_24dp),
                    contentDescription = stringResource(android.R.string.copy),
                )
            }
            IconButton(
                onClick = onShare,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share_24dp),
                    contentDescription = stringResource(R.string.action_share),
                )
            }
        },
    )
}
