# AA-10 widget source validation

> **Superseded for Wallpaper:** the current AA-10 handback is
> `advance-shuffle-2026-11-06/evidence-aa10-wallpaper/AA10-SOURCE.md`. Wallpaper is implemented
> via public `WallpaperManager.getWallpaperColors` (API 27+) with album-art/system fallback;
> this older validation's deferred/40-of-40 scope is historical.

Status: **source-only implemented and validated**. No build, install, device run, or runtime PASS is claimed.

## Scope implemented

- Per-widget `WidgetSettings` keyed by `appWidgetId`, using the existing Preferences DataStore (`widget_settings` file), defaults `System`/`0%`.
- Per-widget theme modes: `System` and `AlbumArt`; `Wallpaper` is explicitly deferred per the audit.
- Transparency applied to surface/background/container providers with contrast floors: surface `0.35`, scrim `0.30`.
- Shared `WidgetEmptyState` for blank title + no artwork; opens `MainActivity`; used by all three widgets.
- Compose `WidgetConfigActivity` registered as `android:configure` and `reconfigurable` for all three widget providers.
- Settings removed on widget deletion.

## Files

Changed/added:
- `app/src/main/java/com/ssilencce/core/appwidgets/state/WidgetSettings.kt` (new)
- `app/src/main/java/com/ssilencce/ui/screen/widgets/WidgetConfigActivity.kt` (new)
- `app/src/main/java/com/ssilencce/core/appwidgets/WidgetExtension.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/WidgetComponent.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/SsilencceGlanceWidget.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/CardWidget.kt`
- `app/src/main/java/com/ssilencce/core/appwidgets/FullWidget.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/glance_responsive_widget_info.xml`
- `app/src/main/res/xml/glance_card_widget_info.xml`
- `app/src/main/res/xml/glance_full_widget_info.xml`
- `app/src/main/res/values/strings.xml` (widget keys only)
- `docs/FEATURES.md`, `docs/MISSING_FEATURES.md`, `docs/CODE_MAP.md`
- `docs/architecture/ADR-050-per-widget-appearance-and-configuration.md` (new)
- `docs/agents/domain.md`

No AA-05/AA-06/AA-07 source, Room schema, dependency, version or translated resources were changed. H retains AA-07 source ownership.

## Strings

Moved into shared `res/values/strings.xml` now that H released it; the temporary `strings_widgets.xml` was removed.
New keys:
- `widget_config_title` = "Widget settings"
- `widget_theme_album_art` = "Album art"
- `widget_transparency_title` = "Transparency"
- `widget_empty_nothing_playing` = "Nothing playing"
- `widget_empty_open_app` = "Open ssilencce"

Reused: `general_theme_title`, `system_default`, `action_save`, `action_cancel`.

## Docs changed

- `FEATURES.md`: widget bullet now documents per-widget System/AlbumArt themes, transparency floors, shared empty state and configure/reconfigure; wallpaper residual.
- `MISSING_FEATURES.md`: Appendix A widget gap reworded; section 16 records the implemented widget scope and the remaining wallpaper-derived theme.
- `CODE_MAP.md`: new `WidgetSettings.kt` and `WidgetConfigActivity.kt` entries; updated widget/component/extension responsibilities and notes.
- `ADR-050-per-widget-appearance-and-configuration.md`: per-widget DataStore, theme modes, alpha floors, empty state, config activity, Wallpaper deferral and consequences.
- `docs/agents/domain.md`: ADR count 49→50 and ADR-050 summary.

## Validation counts

| Check | Result |
|---|---|
| ktlint (7 changed/new Kotlin files) | **exit 0** |
| AA-10 source/pure probe | **exit 0**, **40/40 PASS**, `passed=true` |
| `git diff --check` (widget source + manifest + XML + shared strings + docs) | **exit 0** |
| XML parse (three widget info files) | all OK |

Probe pure values: clamp `-7→0`, `140→100`, `42→42`; surface alpha at 100% transparency = `0.35`; scrim = `0.30`; empty predicate true for blank title/no art, false when title or art exists; no missing widget string references.

## Residual / deferred

- `Wallpaper` theme deferred by audit decision; only System/AlbumArt ship.
- System-theme transparency wraps Glance providers using the current resolved color for both day/night; a live day/night switch may require a widget redraw. AlbumArt preserves stored light/dark colors.
- RemoteViews alpha is approximate; no blur/true window transparency.
- Static preview layouts were not regenerated (audit residual).
- No compile/build was run, per instruction. The probe covers resource references and the contract, not Kotlin type-checking; the next frozen build must catch any compile issue.
- Runtime matrix (add three widgets, configure independently, reboot persistence, delete/re-add cleanup, max-transparency contrast) remains pending.

## Evidence

- `advance-shuffle-2026-11-06/tools/probe_aa10_widgets.py`
- `advance-shuffle-2026-11-06/evidence-4da/probe_aa10_widgets.json`
- `advance-shuffle-2026-11-06/logs/aa10-ktlint-final.log`, `aa10-ktlint-final.exit.txt`
- `advance-shuffle-2026-11-06/logs/aa10-probe-final.log`, `aa10-probe-final.json`, `aa10-probe-final.summary.txt`
- `advance-shuffle-2026-11-06/logs/aa10-diff-check-final.log`, `aa10-diff-check-final.exit.txt`
