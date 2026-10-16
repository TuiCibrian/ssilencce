package com.ssilencce.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object AuroraDesign {
    val background = Color(0xFF010101)
    val foreground = Color(0xFFEBEBEB)
    val secondary = Color(0xFFC4C4C4)
    val muted = Color(0xFFA5A5A5)
    val border = Color.White.copy(alpha = .095f)
    val glass = Color(0x9E060608)
    val glassStrong = Color(0xE0050507)

    val colors = darkColorScheme(
        primary = Color.White,
        onPrimary = Color(0xFF020202),
        primaryContainer = Color(0xFF181818),
        onPrimaryContainer = foreground,
        secondary = secondary,
        onSecondary = background,
        secondaryContainer = Color(0xFF0F0F0F),
        onSecondaryContainer = foreground,
        tertiary = secondary,
        onTertiary = background,
        tertiaryContainer = Color(0xFF0F0F0F),
        onTertiaryContainer = foreground,
        background = background,
        onBackground = foreground,
        surface = Color(0xFF020202),
        onSurface = foreground,
        surfaceVariant = Color(0xFF0F0F0F),
        onSurfaceVariant = muted,
        surfaceDim = Color(0xFF020202),
        surfaceBright = Color(0xFF181818),
        surfaceContainerLowest = Color(0xFF020202),
        surfaceContainerLow = Color(0xFF040404),
        surfaceContainer = Color(0xFF090909),
        surfaceContainerHigh = Color(0xFF0F0F0F),
        surfaceContainerHighest = Color(0xFF181818),
        outline = Color(0xFF7A7A7A),
        outlineVariant = border,
        inverseSurface = foreground,
        inverseOnSurface = background,
        inversePrimary = background,
        error = Color(0xFFFF6B6B),
        onError = background,
        errorContainer = Color(0xFF3B1111),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}
