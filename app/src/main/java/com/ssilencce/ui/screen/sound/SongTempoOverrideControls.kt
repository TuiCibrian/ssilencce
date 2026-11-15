package com.ssilencce.ui.screen.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssilencce.R

/**
 * "Apply to this song only" / "Reset song" controls shown below the global speed and pitch.
 */
@Composable
fun SongTempoOverrideControls(
    songTitle: String?,
    overrideSpeed: Float?,
    overridePitch: Float?,
    onApply: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSong = !songTitle.isNullOrBlank()
    val hasOverride = overrideSpeed != null && overridePitch != null
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.song_tempo_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                if (hasSong) {
                    stringResource(R.string.song_tempo_current_song, songTitle.orEmpty())
                } else {
                    stringResource(R.string.song_tempo_no_song)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hasOverride) {
            Text(
                text =
                    stringResource(
                        R.string.song_tempo_override_active,
                        overrideSpeed,
                        overridePitch,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onApply,
                enabled = hasSong,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.song_tempo_apply_to_song),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onReset,
                enabled = hasSong && hasOverride,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.song_tempo_reset_song),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
