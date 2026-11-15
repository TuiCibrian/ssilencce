package com.ssilencce.ui.component.compose.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssilencce.R

/**
 * Long-press actions for turning a synced lyric line into an A-B repeat range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbRepeatLineSheet(
    lineText: String,
    abRepeatActive: Boolean,
    abStartSet: Boolean,
    canSetEnd: Boolean,
    onDismiss: () -> Unit,
    onRepeatLine: () -> Unit,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.ab_repeat),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            if (lineText.isNotBlank()) {
                Text(
                    text = lineText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            SheetAction(
                text = stringResource(R.string.ab_repeat_this_line),
                onClick = onRepeatLine,
            )
            SheetAction(
                text = stringResource(R.string.ab_repeat_set_a_here),
                onClick = onSetStart,
            )
            SheetAction(
                text = stringResource(R.string.ab_repeat_set_b_here),
                enabled = abStartSet && canSetEnd,
                onClick = onSetEnd,
            )
            if (abRepeatActive) {
                SheetAction(
                    text = stringResource(R.string.ab_repeat_clear),
                    onClick = onClear,
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f)
                    },
            )
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
    )
}
