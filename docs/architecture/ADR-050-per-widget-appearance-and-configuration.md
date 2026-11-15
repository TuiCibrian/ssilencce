# ADR-050: Per-Widget Appearance Settings and Configuration

## Status

Accepted (2026-11-07; runtime verification pending)

## Context

The three Glance widgets share one global `PlaybackState` snapshot and one artwork-derived
`WidgetTheme`. There is no per-widget appearance choice, transparency control, empty-state
layout or edit-after-add configuration. Musicolet offers per-widget themes, transparency and
empty states; AA-10 defines the source-confirmed minimal scope.

The playback snapshot must remain global (one playback session for all widgets). Appearance is
per placement, so it needs a separate store keyed by `appWidgetId`; reusing
`PlaybackStateDefinition` would incorrectly couple every widget to one theme.

## Decision

- Add `WidgetSettingsStore`, a Preferences DataStore (`widget_settings`) keyed by
  `appWidgetId`, with `themeMode` (`System`, `AlbumArt` or `Wallpaper`) and
  `transparencyPercent` (0-100, default 0). No Room, new dependency or schema migration.
- Default existing widgets to `System` and opaque. Remove the entries when the widget is
  deleted (`GlanceAppWidget.onDelete`).
- Resolve `System` from `GlanceTheme.colors`; resolve `AlbumArt` from the existing global
  `PlaybackState.widgetTheme`, falling back to system when artwork is absent.
- Resolve `Wallpaper` from the public `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)`
  API on Android 8.1/API 27+, preferring the system-extracted primary color and using the
  secondary/tertiary colors only when the primary is transparent. Build a `WidgetTheme` with
  `PaletteStyle.Fidelity`. Cache the resulting ARGB value per `appWidgetId`
  (`widget_wallpaper_color_<id>`) so playback-driven redraws do not repeat the wallpaper IPC.
  Re-selecting Wallpaper in the configuration re-derives and replaces the cache. On Android
  8.0/API 26, or when wallpaper colors are unavailable, return null and fall back to the
  album-art theme and then to system colors; the failure is logged, not fatal. The widget
  does not read the wallpaper drawable and does not request `READ_WALLPAPER_INTERNAL`,
  `MANAGE_EXTERNAL_STORAGE` or any new permission. No new dependency or Room entity is added.
- Apply transparency to surface/background/container providers, keeping text/on-colors opaque.
  Enforce **heuristic alpha floors** of **0.35** for surfaces and **0.30** for the Full widget
  scrim; these are not a guaranteed contrast ratio. RemoteViews alpha is approximate; no blur
  or true window transparency.
- Add one shared `WidgetEmptyState` shown when the title is blank and artwork is absent; it
  opens `MainActivity` and is used by all three widgets. A paused title-bearing track keeps the
  normal layout. The empty state adapts to short widget heights: below **160dp** it intentionally
  omits the 48dp icon and uses a compact title/action layout with 14sp/12sp text, keeping both
  texts visible; at 160dp and above it keeps the existing icon+title+action layout. The whole
  widget surface remains the accessible click target.
- Add a Compose `WidgetConfigActivity`, registered as `android:configure` and
  `android:widgetFeatures="reconfigurable"` on all three providers. It saves settings, updates
  the matching Glance widget and returns `RESULT_OK`; cancellation returns `RESULT_CANCELED`.

## Consequences

- Each widget placement can use a different theme/transparency while sharing playback state.
- Appearance settings survive process death and launcher reconfiguration; deletion removes
  them.
- System-theme transparency wraps the current resolved Glance colors for day and night; a live
  day/night change may require a widget redraw. AlbumArt and Wallpaper keep their stored
  light/dark colors.
- The configuration activity is a new exported component but adds no permission, dependency,
  Room schema or version change.
- Wallpaper appearance is a cached snapshot: changing the device wallpaper does not
  automatically recolor existing widgets until they are reconfigured or updated. This keeps
  playback-driven redraws cheap and avoids repeating wallpaper color extraction.
- Wallpaper derivation is best-effort. API 26, a live wallpaper without computed colors or
  a provider failure falls back to album art/system colors without breaking the widget.
- Static preview layouts remain unchanged; runtime matrix (independent widget configuration,
  Wallpaper derivation and fallback, reboot persistence, delete/re-add cleanup,
  maximum-transparency contrast) is still required.

## Related

- ADR-030 (Glance widgets and serialized playback state), ADR-029 (dynamic color theming),
  `widgets-audit-2026-11-06/AUDIT.md`.
