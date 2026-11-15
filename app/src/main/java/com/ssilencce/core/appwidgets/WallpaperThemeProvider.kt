package com.ssilencce.core.appwidgets

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.ssilencce.core.appwidgets.state.WidgetSettingsStore
import com.ssilencce.core.appwidgets.state.WidgetTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Derives a per-widget palette from the current home-screen wallpaper.
 *
 * Uses the public [WallpaperManager.getWallpaperColors] API (Android 8.1/API 27+).
 * Android 8.0 (API 26) and unavailable colors return null, letting the caller fall
 * back to album-art colors or system colors. The source color is cached in
 * [WidgetSettingsStore] so widget redraws do not repeat the wallpaper IPC.
 */
object WallpaperThemeProvider {
    private const val TAG = "WallpaperThemeProvider"

    suspend fun themeFor(
        context: Context,
        appWidgetId: Int,
        cachedColor: Int?,
    ): WidgetTheme? {
        val color =
            cachedColor ?: deriveColor(context)?.also { derived ->
                cacheColor(context, appWidgetId, derived)
            }
        return color?.let { derived -> withContext(Dispatchers.Default) { WidgetTheme(derived) } }
    }

    private suspend fun cacheColor(
        context: Context,
        appWidgetId: Int,
        color: Int,
    ) {
        try {
            WidgetSettingsStore.saveWallpaperColor(context.applicationContext, appWidgetId, color)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A cache write failure must not break widget rendering; this redraw still uses the derived color.
            Log.w(TAG, "Unable to cache wallpaper color", e)
        }
    }

    suspend fun deriveColor(context: Context): Int? =
        withContext(Dispatchers.IO) {
            try {
                wallpaperColorsColor(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to read wallpaper colors; widget will fall back", e)
                null
            }
        }

    private fun wallpaperColorsColor(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        val wallpaperColors =
            WallpaperManager
                .getInstance(context.applicationContext)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
        return wallpaperColors.primaryColor.takeIf { it.alpha() != 0f }?.toArgb()
            ?: wallpaperColors.secondaryColor?.toArgb()
            ?: wallpaperColors.tertiaryColor?.toArgb()
    }
}
