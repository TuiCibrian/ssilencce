package com.ssilencce.ui.screen.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.ssilencce.R
import com.ssilencce.ui.component.compose.IconifiedSliderTrack
import com.ssilencce.ui.component.compose.TitledCard
import com.ssilencce.ui.theme.SliderTokens
import com.ssilencce.util.CROSSFADE_DURATION
import com.ssilencce.util.CROSSFADE_GAPLESS_ALBUMS
import com.ssilencce.util.CROSSFADE_ON_MANUAL_SKIP
import com.ssilencce.util.MAX_CROSSFADE_DURATION_MS
import kotlin.math.roundToInt

/**
 * Compose crossfade controls for the Sound sheet. It reads and writes the same
 * SharedPreferences keys as the classic playback screen, so both surfaces stay in sync.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CrossfadeSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var durationMs by remember {
        mutableIntStateOf(preferences.getInt(CROSSFADE_DURATION, 0).coerceIn(0, MAX_CROSSFADE_DURATION_MS))
    }
    var crossfadeOnSkip by remember { mutableStateOf(preferences.getBoolean(CROSSFADE_ON_MANUAL_SKIP, false)) }
    var keepGaplessAlbums by remember { mutableStateOf(preferences.getBoolean(CROSSFADE_GAPLESS_ALBUMS, true)) }
    val seconds = durationMs / 1000
    val valueText =
        if (seconds <= 0) {
            stringResource(R.string.crossfade_off)
        } else {
            stringResource(R.string.crossfade_seconds, seconds)
        }

    TitledCard(
        title = stringResource(R.string.crossfade_title),
        modifier = modifier.fillMaxWidth(),
    ) { cardContentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(cardContentPadding),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_crossfade_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Slider(
                    value = durationMs.toFloat(),
                    valueRange = 0f..MAX_CROSSFADE_DURATION_MS.toFloat(),
                    onValueChange = { value ->
                        durationMs =
                            ((value / SLIDER_STEP_MS).roundToInt() * SLIDER_STEP_MS)
                                .coerceIn(0, MAX_CROSSFADE_DURATION_MS)
                    },
                    onValueChangeFinished = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        preferences.edit { putInt(CROSSFADE_DURATION, durationMs) }
                    },
                    track = { sliderState ->
                        IconifiedSliderTrack(
                            state = sliderState,
                            icon = painterResource(R.drawable.ic_crossfade_24dp),
                            modifier = Modifier.height(SliderTokens.LargeTrackHeight),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = valueText,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }

            LabeledSwitch(
                checked = crossfadeOnSkip,
                title = stringResource(R.string.crossfade_on_skip_title),
                description = stringResource(R.string.crossfade_on_skip_summary),
                enabled = durationMs > 0,
                onStateChange = { checked ->
                    crossfadeOnSkip = checked
                    preferences.edit { putBoolean(CROSSFADE_ON_MANUAL_SKIP, checked) }
                },
            )

            LabeledSwitch(
                checked = keepGaplessAlbums,
                title = stringResource(R.string.crossfade_gapless_albums_title),
                description = stringResource(R.string.crossfade_gapless_albums_summary),
                enabled = durationMs > 0,
                onStateChange = { checked ->
                    keepGaplessAlbums = checked
                    preferences.edit { putBoolean(CROSSFADE_GAPLESS_ALBUMS, checked) }
                },
            )
        }
    }
}

private const val SLIDER_STEP_MS = 500
