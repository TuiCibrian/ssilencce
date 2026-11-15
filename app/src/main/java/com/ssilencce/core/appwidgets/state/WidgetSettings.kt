package com.ssilencce.core.appwidgets.state

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.widgetSettingsDataStore by preferencesDataStore(name = "widget_settings")

enum class WidgetThemeMode(
    val storedValue: Int,
) {
    System(0),
    AlbumArt(1),
    Wallpaper(2),
    ;

    companion object {
        fun fromStored(value: Int): WidgetThemeMode = entries.firstOrNull { it.storedValue == value } ?: System
    }
}

data class WidgetSettings(
    val themeMode: WidgetThemeMode = WidgetThemeMode.System,
    val transparencyPercent: Int = 0,
    val wallpaperColor: Int? = null,
)

object WidgetSettingsStore {
    const val MIN_SURFACE_ALPHA = 0.35f
    const val MIN_SCRIM_ALPHA = 0.30f

    fun clampTransparency(value: Int): Int = value.coerceIn(0, 100)

    fun surfaceAlpha(transparencyPercent: Int): Float = (1f - clampTransparency(transparencyPercent) / 100f).coerceAtLeast(MIN_SURFACE_ALPHA)

    fun scrimAlpha(transparencyPercent: Int): Float = (1f - clampTransparency(transparencyPercent) / 100f).coerceAtLeast(MIN_SCRIM_ALPHA)

    fun isEmpty(
        title: String?,
        artworkData: ByteArray?,
    ): Boolean = title.isNullOrBlank() && (artworkData == null || artworkData.isEmpty())

    // AppWidgetId is a single-value data class whose hashCode is the stable appWidgetId.
    fun appWidgetIdKey(glanceId: GlanceId): Int = glanceId.hashCode()

    suspend fun load(
        context: Context,
        appWidgetId: Int,
    ): WidgetSettings =
        try {
            val preferences = context.widgetSettingsDataStore.data.first()
            WidgetSettings(
                themeMode = WidgetThemeMode.fromStored(preferences[themeKey(appWidgetId)] ?: 0),
                transparencyPercent = clampTransparency(preferences[transparencyKey(appWidgetId)] ?: 0),
                wallpaperColor = preferences[wallpaperColorKey(appWidgetId)],
            )
        } catch (e: IOException) {
            WidgetSettings()
        } catch (e: CorruptionException) {
            WidgetSettings()
        }

    suspend fun save(
        context: Context,
        appWidgetId: Int,
        settings: WidgetSettings,
    ) {
        context.widgetSettingsDataStore.edit { preferences ->
            preferences[themeKey(appWidgetId)] = settings.themeMode.storedValue
            preferences[transparencyKey(appWidgetId)] = clampTransparency(settings.transparencyPercent)
            val wallpaperColor = settings.wallpaperColor
            if (wallpaperColor != null) {
                preferences[wallpaperColorKey(appWidgetId)] = wallpaperColor
            } else {
                preferences.remove(wallpaperColorKey(appWidgetId))
            }
        }
    }

    suspend fun saveWallpaperColor(
        context: Context,
        appWidgetId: Int,
        color: Int,
    ) {
        context.widgetSettingsDataStore.edit { preferences ->
            preferences[wallpaperColorKey(appWidgetId)] = color
        }
    }

    suspend fun delete(
        context: Context,
        appWidgetId: Int,
    ) {
        context.widgetSettingsDataStore.edit { preferences ->
            preferences.remove(themeKey(appWidgetId))
            preferences.remove(transparencyKey(appWidgetId))
            preferences.remove(wallpaperColorKey(appWidgetId))
        }
    }

    private fun themeKey(appWidgetId: Int) = intPreferencesKey("widget_theme_$appWidgetId")

    private fun transparencyKey(appWidgetId: Int) = intPreferencesKey("widget_transparency_$appWidgetId")

    private fun wallpaperColorKey(appWidgetId: Int) = intPreferencesKey("widget_wallpaper_color_$appWidgetId")
}
