"""Source/pure probe for AA-10 widget settings, themes, empty state, config and wallpaper."""
import json, os, re, sys

ROOT = r"C:\Users\geode\Documents\Repos\ssilencce"
J = lambda *p: os.path.join(ROOT, *p)
OUT = J("advance-shuffle-2026-11-06", "evidence-aa10-wallpaper", "probe_aa10_widgets.json")

def read(*p):
    return open(J(*p), encoding="utf-8").read()

settings = read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "state", "WidgetSettings.kt")
ext = read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "WidgetExtension.kt")
provider = read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "WallpaperThemeProvider.kt")
component = read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "WidgetComponent.kt")
config = read("app", "src", "main", "java", "com", "ssilencce", "ui", "screen", "widgets", "WidgetConfigActivity.kt")
manifest = read("app", "src", "main", "AndroidManifest.xml")
shared_strings = read("app", "src", "main", "res", "values", "strings.xml")
strings = shared_strings
adr = read("docs", "architecture", "ADR-050-per-widget-appearance-and-configuration.md")
features = read("docs", "FEATURES.md")
missing_features = read("docs", "MISSING_FEATURES.md")
code_map = read("docs", "CODE_MAP.md")
widgets = {
    "responsive": read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "SsilencceGlanceWidget.kt"),
    "card": read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "CardWidget.kt"),
    "full": read("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "FullWidget.kt"),
}
infos = {
    "responsive": read("app", "src", "main", "res", "xml", "glance_responsive_widget_info.xml"),
    "card": read("app", "src", "main", "res", "xml", "glance_card_widget_info.xml"),
    "full": read("app", "src", "main", "res", "xml", "glance_full_widget_info.xml"),
}

checks = {}
checks["settings_uses_preferences_datastore"] = 'preferencesDataStore(name = "widget_settings")' in settings
checks["settings_keys_include_app_widget_id"] = (
    'intPreferencesKey("widget_theme_$appWidgetId")' in settings
    and 'intPreferencesKey("widget_transparency_$appWidgetId")' in settings
    and 'intPreferencesKey("widget_wallpaper_color_$appWidgetId")' in settings
)
checks["settings_defaults_system_zero"] = (
    "themeMode: WidgetThemeMode = WidgetThemeMode.System" in settings
    and "transparencyPercent: Int = 0" in settings
    and "wallpaperColor: Int? = null" in settings
)
checks["settings_clamps_transparency"] = "coerceIn(0, 100)" in settings
checks["settings_has_contrast_floors"] = "MIN_SURFACE_ALPHA = 0.35f" in settings and "MIN_SCRIM_ALPHA = 0.30f" in settings
checks["settings_empty_predicate"] = "title.isNullOrBlank() && (artworkData == null || artworkData.isEmpty())" in settings
checks["settings_persists_by_app_widget_id"] = all(
    token in settings for token in ("suspend fun load(", "appWidgetId: Int", "suspend fun save(", "suspend fun delete(")
)
checks["settings_load_catches_only_io_corruption"] = (
    "catch (e: IOException)" in settings
    and "catch (e: CorruptionException)" in settings
    and "runCatching" not in settings
)
checks["settings_key_is_stable_hash"] = "fun appWidgetIdKey(glanceId: GlanceId): Int = glanceId.hashCode()" in settings
checks["theme_modes_include_wallpaper"] = (
    "System(0)" in settings and "AlbumArt(1)" in settings and "Wallpaper(2)" in settings
)
checks["settings_persists_wallpaper_color"] = (
    "wallpaperColor = preferences[wallpaperColorKey(appWidgetId)]" in settings
    and "suspend fun saveWallpaperColor(" in settings
    and "preferences[wallpaperColorKey(appWidgetId)] = wallpaperColor" in settings
)
checks["settings_clears_wallpaper_color"] = (
    "preferences.remove(wallpaperColorKey(appWidgetId))" in settings
)
checks["extension_applies_surface_alpha"] = "WidgetSettingsStore.surfaceAlpha(transparencyPercent)" in ext
checks["extension_applies_scrim_floor"] = "WidgetSettingsStore.scrimAlpha(transparencyPercent)" in ext
checks["extension_has_scrim_color_helper"] = "fun WidgetTheme?.getScrimColor(" in ext
checks["extension_supports_wallpaper_scrim"] = (
    "themeMode == WidgetThemeMode.AlbumArt || themeMode == WidgetThemeMode.Wallpaper" in ext
)
checks["extension_resolves_wallpaper_fallback"] = (
    "suspend fun WidgetSettings.resolveWallpaperTheme(" in ext
    and "WidgetThemeMode.Wallpaper -> wallpaperTheme ?: this" in ext
)
checks["wallpaper_provider_uses_public_wallpaper_colors"] = (
    "getWallpaperColors(WallpaperManager.FLAG_SYSTEM)" in provider
    and "WidgetTheme(derived)" in provider
    and "style: PaletteStyle = PaletteStyle.Fidelity" in ext
)
checks["wallpaper_provider_handles_api_levels"] = (
    "Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1" in provider
    and "primaryColor.takeIf" in provider
    and "secondaryColor?.toArgb()" in provider
    and "tertiaryColor?.toArgb()" in provider
)
checks["wallpaper_provider_caches_per_widget"] = (
    "cacheColor(context, appWidgetId, derived)" in provider
    and "WidgetSettingsStore.saveWallpaperColor(context.applicationContext, appWidgetId, color)" in provider
    and "Unable to cache wallpaper color" in provider
)
checks["wallpaper_provider_runs_off_main_thread"] = "withContext(Dispatchers.IO)" in provider
checks["wallpaper_provider_rethrows_cancellation"] = (
    "catch (e: CancellationException)" in provider and "throw e" in provider
)
checks["wallpaper_provider_has_no_restricted_drawable_path"] = (
    "getDrawable" not in provider
    and "drawable" not in provider.lower()
    and "PaletteProcessor" not in provider
    and "Bitmap" not in provider
)
checks["wallpaper_provider_has_safe_fallback"] = (
    "catch (e: Exception)" in provider and "return null" in provider and "Log.w(" in provider
)
checks["wallpaper_provider_has_no_new_dependency"] = (
    "androidx.palette" not in provider
    and "androidx.room" not in provider
    and "io.coil" not in provider
    and "MANAGE_EXTERNAL_STORAGE" not in provider
    and "READ_WALLPAPER_INTERNAL" not in provider
    and "import android.app.WallpaperManager" in provider
)
checks["manifest_has_no_wallpaper_permission"] = (
    "SET_WALLPAPER" not in manifest and "READ_WALLPAPER_INTERNAL" not in manifest
)
checks["empty_state_composable"] = "fun WidgetEmptyState(" in component
checks["empty_state_has_title_and_open_action"] = (
    "R.string.widget_empty_nothing_playing" in component
    and "R.string.widget_empty_open_app" in component
    and "actionStartActivity<MainActivity>()" in component
)
checks["empty_state_adapts_to_short_height"] = (
    "LocalSize.current.height < COMPACT_EMPTY_STATE_MAX_HEIGHT" in component
    and "private val COMPACT_EMPTY_STATE_MAX_HEIGHT = 160.dp" in component
    and "if (!compact)" in component
)
checks["empty_state_compact_keeps_title_and_action"] = (
    "fontSize = 14.sp" in component
    and "fontSize = 12.sp" in component
    and "maxLines = if (compact) 1 else Int.MAX_VALUE" in component
)
checks["empty_state_preserves_normal_layout"] = (
    "GlanceModifier.padding(if (compact) 6.dp else 16.dp)" in component
    and "size(48.dp)" in component
)
checks["config_activity_present"] = "class WidgetConfigActivity" in config
checks["config_uses_appwidget_id_and_results"] = all(
    token in config for token in ("EXTRA_APPWIDGET_ID", "RESULT_OK", "RESULT_CANCELED", "WidgetSettingsStore.save")
)
checks["config_updates_widget"] = "getGlanceIdBy(appWidgetId)" in config and "widget.update(this, glanceId)" in config
checks["config_exact_component_provider"] = all(
    token in config
    for token in (
        "ComponentName(this, SsilencceGlanceWidgetReceiver::class.java)",
        "ComponentName(this, CardWidgetReceiver::class.java)",
        "ComponentName(this, FullWidgetReceiver::class.java)",
    )
) and "endsWith(" not in config
checks["config_saving_state_and_failure"] = (
    "saving = true" in config
    and "enabled = !saving" in config
    and "if (updated) RESULT_OK else RESULT_CANCELED" in config
)
checks["config_offers_wallpaper"] = (
    "WidgetThemeMode.Wallpaper" in config
    and "R.string.widget_theme_wallpaper" in config
    and "R.string.widget_theme_wallpaper_summary" in config
)
checks["config_caches_derived_wallpaper_color"] = (
    "WallpaperThemeProvider.deriveColor(this@WidgetConfigActivity)" in config
    and "settings.copy(wallpaperColor = wallpaperColor)" in config
)
checks["manifest_registers_config_activity"] = (
    'android:name=".ui.screen.widgets.WidgetConfigActivity"' in manifest
    and 'android:exported="true"' in manifest
)
checks["all_three_widgets_configure"] = all(
    'android:configure="com.ssilencce.ui.screen.widgets.WidgetConfigActivity"' in info
    and 'android:widgetFeatures="reconfigurable"' in info
    for info in infos.values()
)
for name, widget in widgets.items():
    checks[f"{name}_reads_settings_by_id"] = (
        "WidgetSettingsStore.appWidgetIdKey(id)" in widget
        and "WidgetSettingsStore.load(context, appWidgetId)" in widget
    )
    checks[f"{name}_uses_per_widget_theme_and_transparency"] = (
        "getColors(context, settings.themeMode, settings.transparencyPercent)" in widget
    )
    checks[f"{name}_resolves_wallpaper_theme"] = (
        "settings.resolveWallpaperTheme(context, appWidgetId)" in widget
        and "resolveTheme(settings.themeMode, wallpaperTheme)" in widget
    )
    checks[f"{name}_renders_empty_state"] = (
        "WidgetSettingsStore.isEmpty(playbackState.currentTitle, playbackState.artworkData)" in widget
        and "WidgetEmptyState(" in widget
    )
    checks[f"{name}_cleans_settings_on_delete"] = "WidgetSettingsStore.delete(context, WidgetSettingsStore.appWidgetIdKey(id))" in widget
checks["full_widget_uses_scrim_floor"] = "getScrimColor(" in widgets["full"]
all_widget_sources = settings + ext + provider + component + config + "".join(widgets.values())
resource_names = set(re.findall(r'<string name="([^"]+)"', strings + shared_strings))
string_refs = set(re.findall(r"R\.string\.([A-Za-z0-9_]+)", all_widget_sources))
missing_strings = sorted(string_refs - resource_names)
checks["all_widget_string_refs_resolve"] = not missing_strings
checks["launcher_icon_resource_exists"] = os.path.exists(J("app", "src", "main", "res", "mipmap-hdpi", "ic_launcher.webp"))
checks["no_room_in_widget_sources"] = "androidx.room" not in all_widget_sources and "@Entity" not in all_widget_sources
checks["no_restricted_appwidget_id"] = "AppWidgetId" not in "".join(widgets.values()) and "getAppWidgetId" not in "".join(widgets.values())
checks["empty_state_uses_normal_radius"] = all("WidgetEmptyState(context = context, cornerRadius = playbackState.imageCornerRadius?.dp?.times(2) ?: 16.dp)" in widgets[name] for name in ("card", "full"))
checks["docs_call_alpha_floors_heuristic"] = "not a guaranteed contrast ratio" in adr and "heuristic" in features
checks["docs_no_longer_defer_wallpaper"] = (
    "deferred" not in adr.lower()
    and "remains deferred" not in features.lower()
    and "wallpaper-derived theme remains" not in missing_features.lower()
)
checks["docs_describe_wallpaper_cache_and_fallback"] = (
    "widget_wallpaper_color_<id>" in adr
    and "fall back" in adr.lower()
    and "WallpaperColors" in adr
    and "PaletteProcessor" not in adr
    and "WallpaperThemeProvider" in code_map
)
checks["new_strings_in_shared"] = all(
    f'name="{key}"' in shared_strings
    for key in (
        "widget_config_title",
        "widget_theme_album_art",
        "widget_theme_wallpaper",
        "widget_theme_wallpaper_summary",
        "widget_transparency_title",
        "widget_empty_nothing_playing",
        "widget_empty_open_app",
    )
)
checks["no_isolated_widget_strings_file"] = not os.path.exists(
    J("app", "src", "main", "res", "values", "strings_widgets.xml")
)
checks["wallpaper_mode_enabled"] = (
    "Wallpaper" in config and "Wallpaper(2)" in settings and os.path.exists(
        J("app", "src", "main", "java", "com", "ssilencce", "core", "appwidgets", "WallpaperThemeProvider.kt")
    )
)

# Pure contract values (mirror the Kotlin constants and fallback chain).
def clamp(v): return max(0, min(100, v))
def surface_alpha(v): return max(0.35, 1.0 - clamp(v) / 100.0)
def scrim_alpha(v): return max(0.30, 1.0 - clamp(v) / 100.0)
def is_empty(title, art): return (not title or title.strip() == "") and not art
def compact_empty_state(height_dp): return height_dp < 160
def from_stored(v): return {0: "system", 1: "album", 2: "wallpaper"}.get(v, "system")
def resolve_theme(mode, has_album, has_wallpaper):
    if mode == "system":
        return "system"
    if mode == "album":
        return "album" if has_album else "system"
    if mode == "wallpaper":
        if has_wallpaper:
            return "wallpaper"
        return "album" if has_album else "system"
    return "system"
def wallpaper_key(app_widget_id): return f"widget_wallpaper_color_{app_widget_id}"
pure = {
    "clamp_low": clamp(-7), "clamp_high": clamp(140), "clamp_mid": clamp(42),
    "surface_0": surface_alpha(0), "surface_100": surface_alpha(100), "surface_100_floor": surface_alpha(100) == 0.35,
    "scrim_100_floor": scrim_alpha(100) == 0.30,
    "empty_default": is_empty("", None),
    "empty_blank_title": is_empty("   ", None),
    "not_empty_title": not is_empty("Track", None),
    "not_empty_art": not is_empty(None, b"art"),
    "compact_empty_96": compact_empty_state(96),
    "compact_empty_159": compact_empty_state(159),
    "normal_empty_160": not compact_empty_state(160),
    "normal_empty_240": not compact_empty_state(240),
    "theme_from_stored": [from_stored(0), from_stored(1), from_stored(2), from_stored(99)],
    "wallpaper_with_cache": resolve_theme("wallpaper", True, True),
    "wallpaper_falls_back_to_album": resolve_theme("wallpaper", True, False),
    "wallpaper_falls_back_to_system": resolve_theme("wallpaper", False, False),
    "album_without_art_falls_back": resolve_theme("album", False, True),
    "system_ignores_art": resolve_theme("system", True, True),
    "wallpaper_key_42": wallpaper_key(42),
    "missing_strings": missing_strings,
}
checks["pure_clamp"] = pure["clamp_low"] == 0 and pure["clamp_high"] == 100 and pure["clamp_mid"] == 42
checks["pure_alpha_floors"] = pure["surface_100_floor"] and pure["scrim_100_floor"] and pure["surface_0"] == 1.0
checks["pure_empty_state"] = pure["empty_default"] and pure["empty_blank_title"] and pure["not_empty_title"] and pure["not_empty_art"]
checks["pure_empty_state_compact_threshold"] = (
    pure["compact_empty_96"]
    and pure["compact_empty_159"]
    and pure["normal_empty_160"]
    and pure["normal_empty_240"]
)
checks["pure_wallpaper_modes"] = pure["theme_from_stored"] == ["system", "album", "wallpaper", "system"]
checks["pure_wallpaper_fallback_chain"] = (
    pure["wallpaper_with_cache"] == "wallpaper"
    and pure["wallpaper_falls_back_to_album"] == "album"
    and pure["wallpaper_falls_back_to_system"] == "system"
    and pure["album_without_art_falls_back"] == "system"
    and pure["system_ignores_art"] == "system"
)
checks["pure_wallpaper_key"] = pure["wallpaper_key_42"] == "widget_wallpaper_color_42"

passed = all(checks.values())
counts = {"passed": sum(1 for v in checks.values() if v), "total": len(checks)}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
json.dump({"checks": checks, "pure": pure, "counts": counts, "passed": passed}, open(OUT, "w", encoding="utf-8"), indent=2)
print("AA10 WIDGET PROBE", "PASS" if passed else "FAIL", f"{counts['passed']}/{counts['total']}")
for k, v in checks.items():
    print(("PASS " if v else "FAIL ") + k)
print(json.dumps(pure, indent=2))
if not passed:
    sys.exit(1)