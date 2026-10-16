package com.ssilencce.ui.component.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssilencce.ui.theme.AuroraDesign
import com.ssilencce.util.Preferences

@Composable
fun BottomSheetDialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit),
) {
    Surface(
        modifier = modifier,
        color = if (Preferences.isAuroraAppearance) AuroraDesign.glassStrong else MaterialTheme.colorScheme.surfaceContainer,
        border = if (Preferences.isAuroraAppearance) BorderStroke(1.dp, AuroraDesign.border) else null,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        content = content,
    )
}
