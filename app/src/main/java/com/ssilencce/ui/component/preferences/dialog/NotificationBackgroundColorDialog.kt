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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ssilencce.R
import com.ssilencce.core.model.player.NotificationBackgroundMode
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences
import java.util.Locale
import android.graphics.Color as AndroidColor

/**
 * Bottom sheet for the custom notification background color. Selecting a color stores it and
 * switches the background mode to custom.
 */
class NotificationBackgroundColorDialog : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                SsilencceTheme {
                    NotificationBackgroundColorContent(
                        currentColor = Preferences.notificationCustomBackgroundColor,
                        onColorSelected = { color ->
                            Preferences.notificationCustomBackgroundColor = color
                            Preferences.notificationBackgroundMode = NotificationBackgroundMode.Custom
                            dismissAllowingStateLoss()
                        },
                        onDismiss = { dismissAllowingStateLoss() },
                    )
                }
            }
        }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    companion object {
        const val TAG = "NotificationBackgroundColorDialog"
    }
}

private val presetColors =
    listOf(
        Color(0xFF1E1E1E),
        Color(0xFF263238),
        Color(0xFF37474F),
        Color(0xFF3E2723),
        Color(0xFF4E342E),
        Color(0xFF1A237E),
        Color(0xFF0D47A1),
        Color(0xFF004D40),
        Color(0xFF1B5E20),
        Color(0xFF4A148C),
        Color(0xFF880E4F),
        Color(0xFF6D4C41),
        Color(0xFF546E7A),
        Color(0xFFE3F2FD),
        Color(0xFFFFF3E0),
    )

@Composable
private fun NotificationBackgroundColorContent(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val themeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val isCustomColor = currentColor != 0
    val initialColor =
        remember(currentColor, themeBackground) {
            if (isCustomColor) currentColor else themeBackground.toArgb()
        }
    val initialHsv =
        remember(initialColor) {
            FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) }
        }
    var customHue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var customSaturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var customBrightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    val customColor =
        Color(
            AndroidColor.HSVToColor(
                floatArrayOf(customHue, customSaturation, customBrightness),
            ),
        )
    val customHex = String.format(Locale.ROOT, "#%06X", 0xFFFFFF and customColor.toArgb())

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.notification_background_color_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24dp),
                    contentDescription = stringResource(R.string.close_action),
                )
            }
        }

        Text(
            text = stringResource(R.string.notification_background_color_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.notification_background_color_presets),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        presetColors.chunked(5).forEach { rowColors ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                rowColors.forEach { color ->
                    val hex = String.format(Locale.ROOT, "#%06X", 0xFFFFFF and color.toArgb())
                    BackgroundColorDot(
                        color = color,
                        selected = isCustomColor && color.toArgb() == currentColor,
                        label = stringResource(R.string.notification_background_color_swatch, hex),
                        onClick = { onColorSelected(color.toArgb()) },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.notification_background_color_custom_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(customColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = customHex,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        BackgroundColorSlider(
            label = stringResource(R.string.notification_background_color_hue),
            value = customHue,
            valueRange = 0f..360f,
            onValueChange = { customHue = it },
        )
        BackgroundColorSlider(
            label = stringResource(R.string.notification_background_color_saturation),
            value = customSaturation,
            valueRange = 0f..1f,
            onValueChange = { customSaturation = it },
        )
        BackgroundColorSlider(
            label = stringResource(R.string.notification_background_color_brightness),
            value = customBrightness,
            valueRange = 0.1f..1f,
            onValueChange = { customBrightness = it },
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onColorSelected(customColor.toArgb()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.notification_background_color_apply))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BackgroundColorSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@Composable
private fun BackgroundColorDot(
    color: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    shape = CircleShape,
                ).clickable(onClick = onClick)
                .semantics { contentDescription = label },
    )
}
