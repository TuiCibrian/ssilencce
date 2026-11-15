package com.ssilencce.ui.screen.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssilencce.R
import com.ssilencce.core.model.equalizer.ReverbPreset
import com.ssilencce.core.model.equalizer.ReverbSettings
import com.ssilencce.core.model.equalizer.ReverbState
import com.ssilencce.ui.component.compose.SwitchCard
import kotlin.math.roundToInt

@Composable
fun ReverbCard(
    state: ReverbState,
    effectsAllowed: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (ReverbPreset) -> Unit,
    onSettingsChange: (ReverbSettings) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.supported) return

    SwitchCard(
        onCheckedChange = onEnabledChange,
        checked = state.enabled,
        title = stringResource(R.string.reverb_label),
        icon = painterResource(R.drawable.ic_surround_sound_24dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        enabled = effectsAllowed,
        modifier = modifier.fillMaxWidth(),
    ) { cardContentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(cardContentPadding),
        ) {
            Text(
                text = stringResource(R.string.reverb_experimental),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
            ) {
                ReverbPreset.Selectable.forEach { preset ->
                    FilterChip(
                        selected = state.preset == preset,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(stringResource(preset.nameRes)) },
                        enabled = effectsAllowed,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                if (state.preset.isCustom) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(stringResource(R.string.reverb_preset_custom)) },
                        enabled = false,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            val settings = state.settings
            ReverbLevelSlider(
                label = stringResource(R.string.reverb_room_level),
                value = settings.roomLevel / MILLIBELS_PER_DB,
                range = -90f..0f,
                enabled = effectsAllowed,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(roomLevel = it.toMillibels()))
                },
            )
            ReverbLevelSlider(
                label = stringResource(R.string.reverb_room_hf_level),
                value = settings.roomHfLevel / MILLIBELS_PER_DB,
                range = -90f..0f,
                enabled = effectsAllowed,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(roomHfLevel = it.toMillibels()))
                },
            )
            ParameterSlider(
                label = stringResource(R.string.reverb_decay_time),
                value = settings.decayTimeMs.toFloat(),
                range = 100f..20000f,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(decayTimeMs = it.roundToInt()))
                },
                format = "%.0f",
                unit = " ms",
                enabled = effectsAllowed,
            )
            ParameterSlider(
                label = stringResource(R.string.reverb_decay_hf_ratio),
                value = settings.decayHfRatio.toFloat(),
                range = 100f..2000f,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(decayHfRatio = it.roundToInt()))
                },
                format = "%.0f",
                unit = "%",
                enabled = effectsAllowed,
            )
            ReverbLevelSlider(
                label = stringResource(R.string.reverb_reflections_level),
                value = settings.reflectionsLevel / MILLIBELS_PER_DB,
                range = -90f..10f,
                enabled = effectsAllowed,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(reflectionsLevel = it.toMillibels()))
                },
            )
            ParameterSlider(
                label = stringResource(R.string.reverb_reflections_delay),
                value = settings.reflectionsDelayMs.toFloat(),
                range = 0f..65f,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(reflectionsDelayMs = it.roundToInt()))
                },
                format = "%.0f",
                unit = " ms",
                enabled = effectsAllowed,
            )
            ReverbLevelSlider(
                label = stringResource(R.string.reverb_reverb_level),
                value = settings.reverbLevel / MILLIBELS_PER_DB,
                range = -90f..20f,
                enabled = effectsAllowed,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(reverbLevel = it.toMillibels()))
                },
            )
            ParameterSlider(
                label = stringResource(R.string.reverb_reverb_delay),
                value = settings.reverbDelayMs.toFloat(),
                range = 0f..100f,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(reverbDelayMs = it.roundToInt()))
                },
                format = "%.0f",
                unit = " ms",
                enabled = effectsAllowed,
            )
            ParameterSlider(
                label = stringResource(R.string.reverb_diffusion),
                value = settings.diffusion / PER_MILLE_TO_PERCENT,
                range = 0f..100f,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(diffusion = it.toPerMille()))
                },
                format = "%.0f",
                unit = "%",
                enabled = effectsAllowed,
            )
            ParameterSlider(
                label = stringResource(R.string.reverb_density),
                value = settings.density / PER_MILLE_TO_PERCENT,
                range = 0f..100f,
                onValueChangeFinished = {
                    onSettingsChange(settings.copy(density = it.toPerMille()))
                },
                format = "%.0f",
                unit = "%",
                enabled = effectsAllowed,
            )
            ResetButton(onClick = onReset)
        }
    }
}

@Composable
private fun ReverbLevelSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChangeFinished: (Float) -> Unit,
) {
    ParameterSlider(
        label = label,
        value = value,
        range = range,
        onValueChangeFinished = onValueChangeFinished,
        format = "%+.1f",
        unit = " dB",
        enabled = enabled,
    )
}

private fun Float.toMillibels(): Int = (this * MILLIBELS_PER_DB).roundToInt()

private fun Float.toPerMille(): Int = (this * PER_MILLE_TO_PERCENT).roundToInt()

private const val MILLIBELS_PER_DB = 100f
private const val PER_MILLE_TO_PERCENT = 10f
