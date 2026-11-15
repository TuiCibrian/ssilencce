package com.ssilencce.ui.screen.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ssilencce.ui.theme.SsilencceTheme
import com.ssilencce.util.Preferences
import java.util.Locale
import android.graphics.Color as AndroidColor

class AccentColorDialog : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                SsilencceTheme {
                    AccentColorContent(
                        currentColor = Preferences.accentColor,
                        onColorSelected = { color ->
                            Preferences.accentColor = color
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
        const val TAG = "AccentColorDialog"
    }
}

private val presetColors =
    listOf(
        Color(0xFF4D5C92),
        Color(0xFF1565C0),
        Color(0xFF00838F),
        Color(0xFF00695C),
        Color(0xFF2E7D32),
        Color(0xFF827717),
        Color(0xFFF9A825),
        Color(0xFFE65100),
        Color(0xFFC62828),
        Color(0xFFC2185B),
        Color(0xFF7B1FA2),
        Color(0xFF5D4037),
        Color(0xFF546E7A),
        Color(0xFF3E2723),
        Color(0xFF37474F),
    )

@Composable
private fun AccentColorContent(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultAccent = MaterialTheme.colorScheme.primary
    val isDefaultSelected = currentColor == 0
    val initialColor =
        remember(currentColor, defaultAccent) {
            if (currentColor != 0) currentColor else defaultAccent.toArgb()
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
                text = stringResource(R.string.accent_color_title),
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
            text = stringResource(R.string.accent_color_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onColorSelected(0) }
                    .padding(vertical = 8.dp),
        ) {
            ColorDot(
                color = defaultAccent,
                selected = isDefaultSelected,
                label = stringResource(R.string.accent_color_default),
                onClick = { onColorSelected(0) },
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.accent_color_default),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Text(
            text = stringResource(R.string.accent_color_presets),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )

        presetColors.chunked(4).forEach { rowColors ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                rowColors.forEach { color ->
                    val hex = String.format(Locale.ROOT, "#%06X", 0xFFFFFF and color.toArgb())
                    ColorDot(
                        color = color,
                        selected = !isDefaultSelected && color.toArgb() == currentColor,
                        label = stringResource(R.string.accent_color_swatch, hex),
                        onClick = { onColorSelected(color.toArgb()) },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.accent_color_custom_section),
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

        AccentSlider(
            label = stringResource(R.string.accent_color_hue),
            value = customHue,
            valueRange = 0f..360f,
            onValueChange = { customHue = it },
        )
        AccentSlider(
            label = stringResource(R.string.accent_color_saturation),
            value = customSaturation,
            valueRange = 0f..1f,
            onValueChange = { customSaturation = it },
        )
        AccentSlider(
            label = stringResource(R.string.accent_color_brightness),
            value = customBrightness,
            valueRange = 0.1f..1f,
            onValueChange = { customBrightness = it },
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onColorSelected(customColor.toArgb()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.accent_color_apply))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccentSlider(
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
private fun ColorDot(
    color: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
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
