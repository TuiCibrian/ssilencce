package com.ssilencce.ui.screen.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.ui.component.compose.TitledCard

/**
 * Shown when the System engine is selected: the in-app equalizer is off and the panel action hands
 * control to the device's system/OEM equalizer.
 */
@Composable
fun SystemEqualizerCard(
    isAvailable: Boolean,
    onOpenSystemEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitledCard(
        title = stringResource(R.string.eq_engine_system_title),
        icon = painterResource(R.drawable.ic_equalizer_24dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) { cardContentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(cardContentPadding),
        ) {
            Text(
                text = stringResource(R.string.eq_system_equalizer_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isAvailable) {
                Button(onClick = onOpenSystemEqualizer) {
                    Text(stringResource(R.string.eq_system_equalizer_open))
                }
            } else {
                Text(
                    text = stringResource(R.string.eq_system_equalizer_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
