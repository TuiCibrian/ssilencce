package com.ssilencce.core.appwidgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.KeyEvent
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import com.ssilencce.core.appwidgets.state.WidgetSettings
import com.ssilencce.core.appwidgets.state.WidgetSettingsStore
import com.ssilencce.core.appwidgets.state.WidgetTheme
import com.ssilencce.core.appwidgets.state.WidgetThemeMode
import com.ssilencce.playback.PlaybackService
import com.ssilencce.ui.theme.PaletteStyle
import com.ssilencce.ui.theme.dynamicColorSchemes
import androidx.glance.color.ColorProvider as colorProvider
import androidx.glance.unit.ColorProvider as UnitColorProvider

fun Dp.toPx(context: Context): Int =
    TypedValue
        .applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.value,
            context.resources.displayMetrics,
        ).toInt()

fun WidgetTheme(
    @ColorInt
    sourceColor: Int,
    style: PaletteStyle = PaletteStyle.Fidelity,
): WidgetTheme {
    val colorSchemes =
        dynamicColorSchemes(
            keyColor = Color(sourceColor),
            style = style,
            contrastLevel = 0.75,
        )
    return WidgetTheme(
        lightSurfaceColor = colorSchemes.lightColorScheme.surface.toArgb(),
        lightOnSurfaceColor = colorSchemes.lightColorScheme.onSurface.toArgb(),
        lightOnSurfaceVariantColor = colorSchemes.lightColorScheme.onSurfaceVariant.toArgb(),
        lightPrimaryColor = colorSchemes.lightColorScheme.primary.toArgb(),
        lightOnPrimaryColor = colorSchemes.lightColorScheme.onPrimary.toArgb(),
        lightPrimaryContainerColor = colorSchemes.lightColorScheme.primaryContainer.toArgb(),
        lightOnPrimaryContainerColor = colorSchemes.lightColorScheme.onPrimaryContainer.toArgb(),
        lightTertiaryContainerColor = colorSchemes.lightColorScheme.tertiaryContainer.toArgb(),
        lightOnTertiaryContainerColor = colorSchemes.lightColorScheme.onTertiaryContainer.toArgb(),
        darkSurfaceColor = colorSchemes.darkColorScheme.surface.toArgb(),
        darkOnSurfaceColor = colorSchemes.darkColorScheme.onSurface.toArgb(),
        darkOnSurfaceVariantColor = colorSchemes.darkColorScheme.onSurfaceVariant.toArgb(),
        darkPrimaryColor = colorSchemes.darkColorScheme.primary.toArgb(),
        darkOnPrimaryColor = colorSchemes.darkColorScheme.onPrimary.toArgb(),
        darkPrimaryContainerColor = colorSchemes.darkColorScheme.primaryContainer.toArgb(),
        darkOnPrimaryContainerColor = colorSchemes.darkColorScheme.onPrimaryContainer.toArgb(),
        darkTertiaryContainerColor = colorSchemes.darkColorScheme.tertiaryContainer.toArgb(),
        darkOnTertiaryContainerColor = colorSchemes.darkColorScheme.onTertiaryContainer.toArgb(),
    )
}

suspend fun WidgetSettings.resolveWallpaperTheme(
    context: Context,
    appWidgetId: Int,
): WidgetTheme? =
    if (themeMode == WidgetThemeMode.Wallpaper) {
        WallpaperThemeProvider.themeFor(context, appWidgetId, wallpaperColor)
    } else {
        null
    }

fun WidgetTheme?.resolveTheme(
    themeMode: WidgetThemeMode,
    wallpaperTheme: WidgetTheme?,
): WidgetTheme? =
    when (themeMode) {
        WidgetThemeMode.System -> null
        WidgetThemeMode.AlbumArt -> this
        WidgetThemeMode.Wallpaper -> wallpaperTheme ?: this
    }

private fun UnitColorProvider.withAlpha(
    context: Context,
    alpha: Float,
): UnitColorProvider {
    val color = getColor(context).copy(alpha = alpha)
    return androidx.glance.color.ColorProvider(day = color, night = color)
}

private fun systemColorsWithAlpha(
    context: Context,
    colors: ColorProviders,
    alpha: Float,
): ColorProviders =
    colorProviders(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryContainer.withAlpha(context, alpha),
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = colors.secondaryContainer,
        onSecondaryContainer = colors.onSecondaryContainer,
        tertiary = colors.tertiary,
        onTertiary = colors.onTertiary,
        tertiaryContainer = colors.tertiaryContainer.withAlpha(context, alpha),
        onTertiaryContainer = colors.onTertiaryContainer,
        error = colors.error,
        errorContainer = colors.errorContainer,
        onError = colors.onError,
        onErrorContainer = colors.onErrorContainer,
        background = colors.background.withAlpha(context, alpha),
        onBackground = colors.onBackground,
        surface = colors.surface.withAlpha(context, alpha),
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant.withAlpha(context, alpha),
        onSurfaceVariant = colors.onSurfaceVariant,
        outline = colors.outline,
        inverseOnSurface = colors.inverseOnSurface,
        inverseSurface = colors.inverseSurface,
        inversePrimary = colors.inversePrimary,
        widgetBackground = colors.widgetBackground.withAlpha(context, alpha),
    )

@Composable
fun WidgetTheme?.getColors(
    context: Context,
    themeMode: WidgetThemeMode,
    transparencyPercent: Int,
): ColorProviders {
    val systemColors = GlanceTheme.colors
    val surfaceAlpha = WidgetSettingsStore.surfaceAlpha(transparencyPercent)
    if (themeMode == WidgetThemeMode.System || this == null) {
        return if (transparencyPercent == 0) {
            systemColors
        } else {
            systemColorsWithAlpha(context, systemColors, surfaceAlpha)
        }
    }

    return colorProviders(
        primary =
            androidx.glance.color.ColorProvider(
                day = Color(lightPrimaryColor),
                night = Color(darkPrimaryColor),
            ),
        onPrimary =
            androidx.glance.color.ColorProvider(
                day = Color(lightOnPrimaryColor),
                night = Color(darkOnPrimaryColor),
            ),
        primaryContainer =
            androidx.glance.color.ColorProvider(
                day = Color(lightPrimaryContainerColor).copy(alpha = surfaceAlpha),
                night = Color(darkPrimaryContainerColor).copy(alpha = surfaceAlpha),
            ),
        onPrimaryContainer =
            androidx.glance.color.ColorProvider(
                day = Color(lightOnPrimaryContainerColor),
                night = Color(darkOnPrimaryContainerColor),
            ),
        secondary = systemColors.secondary,
        onSecondary = systemColors.onSecondary,
        secondaryContainer = systemColors.secondaryContainer,
        onSecondaryContainer = systemColors.onSecondaryContainer,
        tertiary = systemColors.tertiary,
        onTertiary = systemColors.onTertiary,
        tertiaryContainer =
            androidx.glance.color.ColorProvider(
                day = Color(lightTertiaryContainerColor).copy(alpha = surfaceAlpha),
                night = Color(darkTertiaryContainerColor).copy(alpha = surfaceAlpha),
            ),
        onTertiaryContainer =
            androidx.glance.color.ColorProvider(
                day = Color(lightOnTertiaryContainerColor),
                night = Color(darkOnTertiaryContainerColor),
            ),
        error = systemColors.error,
        errorContainer = systemColors.errorContainer,
        onError = systemColors.onError,
        onErrorContainer = systemColors.onErrorContainer,
        background =
            androidx.glance.color.ColorProvider(
                day = Color(lightSurfaceColor).copy(alpha = surfaceAlpha),
                night = Color(darkSurfaceColor).copy(alpha = surfaceAlpha),
            ),
        onBackground = systemColors.onBackground,
        surface =
            androidx.glance.color.ColorProvider(
                day = Color(lightSurfaceColor).copy(alpha = surfaceAlpha),
                night = Color(darkSurfaceColor).copy(alpha = surfaceAlpha),
            ),
        onSurface =
            androidx.glance.color.ColorProvider(
                day = Color(lightOnSurfaceColor),
                night = Color(darkOnSurfaceColor),
            ),
        surfaceVariant = systemColors.surfaceVariant.withAlpha(context, surfaceAlpha),
        onSurfaceVariant =
            androidx.glance.color.ColorProvider(
                day = Color(lightOnSurfaceVariantColor),
                night = Color(darkOnSurfaceVariantColor),
            ),
        outline = systemColors.outline,
        inverseOnSurface = systemColors.inverseOnSurface,
        inverseSurface = systemColors.inverseSurface,
        inversePrimary = systemColors.inversePrimary,
        widgetBackground = systemColors.widgetBackground.withAlpha(context, surfaceAlpha),
    )
}

@Composable
fun WidgetTheme?.getScrimColor(
    context: Context,
    themeMode: WidgetThemeMode,
    transparencyPercent: Int,
): UnitColorProvider {
    val scrimAlpha = WidgetSettingsStore.scrimAlpha(transparencyPercent)
    return if ((themeMode == WidgetThemeMode.AlbumArt || themeMode == WidgetThemeMode.Wallpaper) && this != null) {
        androidx.glance.color.ColorProvider(
            day = Color(lightPrimaryContainerColor).copy(alpha = scrimAlpha),
            night = Color(darkPrimaryContainerColor).copy(alpha = scrimAlpha),
        )
    } else {
        GlanceTheme.colors.primaryContainer.withAlpha(context, scrimAlpha)
    }
}

fun playbackAction(
    context: Context,
    mediaKeyCode: Int,
): Action {
    val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode))
    return actionStartService(intent, true)
}

fun toggleShuffleAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_TOGGLE_SHUFFLE)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}

fun cycleRepeatAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_CYCLE_REPEAT)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}

fun toggleFavoriteAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_TOGGLE_FAVORITE)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}
