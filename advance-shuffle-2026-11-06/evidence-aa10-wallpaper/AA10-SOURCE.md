# AA-10 Wallpaper theme — source handback

Date: 2026-11-07
Scope: AA-10 widget source only. No build, no device, no AA-05/AA-06/AA-07 edits.
Status: source complete; r2 combined build/lint clean; runtime gates pending; AA-05 not closed.

## Delivered

- `WidgetThemeMode.Wallpaper(2)` plus `WidgetSettings.wallpaperColor: Int?`.
- Per-widget keys:
  - `widget_theme_<appWidgetId>`
  - `widget_transparency_<appWidgetId>`
  - `widget_wallpaper_color_<appWidgetId>`
- `WallpaperThemeProvider` derives and caches a wallpaper source color:
  - API 27+: use the public `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)`
    primary/secondary/tertiary color.
  - API 26 or unavailable colors: return null and fall back to album-art/system colors.
  - No wallpaper drawable is read. The provider requests no `READ_WALLPAPER_INTERNAL`,
    `MANAGE_EXTERNAL_STORAGE` or new permission, and uses no `PaletteProcessor` or bitmap.
  - Derivation runs on `Dispatchers.IO`; cache writes and color-read failures are logged
    without breaking the widget. `CancellationException` is rethrown.
- Fallback chain for Wallpaper: cached/derived wallpaper theme -> album-art theme -> system
  Glance colors.
- Config UI offers System / Album art / Wallpaper, explains the fallback, derives the color
  on save and disables controls while saving. Cancellation still returns `RESULT_CANCELED`;
  save/update failures still return `RESULT_CANCELED`.
- Transparency/scrim behavior is unchanged: 0.35 surface floor / 0.30 scrim floor, applied
  to the wallpaper-derived providers as well.
- Shared strings only: `widget_theme_wallpaper`,
  `widget_theme_wallpaper_summary`. No locale-specific string files touched.
- Docs/ADR/probe updated so Wallpaper is no longer described as deferred or partial.
- Post-8DB runtime fix: the shared empty state now compacts below 160dp (no icon, 14sp/12sp
  title/action, 6dp padding) so the Responsive 3x1 empty state cannot clip the open action;
  normal 160dp+ layouts are unchanged. Probe is now **74/74**.

## Files

Source/resources:
- `app/src/main/java/com/ssilencce/core/appwidgets/state/WidgetSettings.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/WallpaperThemeProvider.kt` (new)
- `app/src/main/java/com/ssilencce/core/appwidgets/WidgetExtension.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/SsilencceGlanceWidget.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/CardWidget.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/FullWidget.kt`
- `app/src/main/java/com/ssilencce/ui/screen/widgets/WidgetConfigActivity.kt`
- `app/src/main/res/values/strings.xml`

Docs:
- `docs/architecture/ADR-050-per-widget-appearance-and-configuration.md`
- `docs/FEATURES.md`
- `docs/MISSING_FEATURES.md`
- `docs/CODE_MAP.md`
- `AGENTS.md`

Verification:
- `advance-shuffle-2026-11-06/tools/probe_aa10_widgets.py`
- `advance-shuffle-2026-11-06/evidence-aa10-wallpaper/probe_aa10_widgets.json`

## Verification

- ktlint 1.8.0 `format` + `check` on all seven Kotlin files: **exit 0**.
- AA-10 source/pure probe: **PASS 70/70** (previous accepted baseline 47/47).
- Probe covers Wallpaper mode persistence/cleanup, fallback resolution, public
  `WallpaperColors` + API guards, absence of the restricted drawable path, cache-write
  safety, rethrow of `CancellationException`, String/resource resolution, docs wording and
  pure fallback cases.
- Combined r2 build after removing the drawable path: **BUILD SUCCESSFUL**, exit 0,
  **0 lint errors**, 1,509 warnings, 1 hint. APK SHA-256
  `8DB35FE076261953A4D9CA978091E66C92734CE1E5B1C7CC47027C9D906448A6`.
- No device/runtime test was run. AA-05 remains open pending the user contract decision.

## Residual risk

- The first combined build exposed a lint error at `WallpaperThemeProvider.kt:79`
  (`getDrawable` MissingPermission). The r2 source removes the restricted drawable path and
  the r2 lint gate is clean (0 errors).
- Wallpaper API behavior is OEM/API dependent. `WallpaperColors` can return null for live
  wallpapers or while colors are being computed, and API 26 always uses the fallback. All
  such cases fall back without a permission request.
- The cached wallpaper color is a snapshot. Changing the device wallpaper does not
  automatically recolor existing widgets until they are reconfigured or updated.
- Alpha floors remain heuristic, not a guaranteed contrast ratio; RemoteViews alpha is
  approximate and there is no blur or true window transparency.
- Static widget previews are unchanged.
