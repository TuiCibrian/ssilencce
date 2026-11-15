# Documentation delta

## docs/FEATURES.md

### Settings / system
- **Battery & background guide** (`Settings > Advanced > Battery & background`): detects the
  device OEM (Xiaomi/MIUI, Huawei/EMUI, Samsung, OPPO, Realme, Vivo, ASUS, generic Android) and
  shows tailored background-playback steps, the current battery-optimization status, a
  "pin in recents" tip, and buttons for the system battery-optimization, app-details and known
  OEM auto-start screens. On MIUI it adds the launcher-shortcut permission guide.
- **Accent color** (`Settings > Appearance > Accent color`): choose the accent from a preset
  palette or an HSV custom picker; the color overrides the theme-derived accent in Compose
  (`SsilencceTheme`) and in legacy views on devices with Material dynamic-color support.
  "Default" restores the theme-derived accent. Aurora Glass keeps its fixed white accent.
- **Show unimportant settings** (`Settings > Advanced`): hides a curated set of advanced
  preferences (and categories left empty) when turned off. Enabled by default.
- **Disable album art** (`Settings > Library > Advanced`): shows placeholders instead of
  decoding cover/artist/playlist/genre/year artwork anywhere, including the media notification.

## docs/MISSING_FEATURES.md

Remove:
- Part 1 §7: `- **Disable album-art loading everywhere** (ssilencce only ignores MediaStore covers).`
- Part 1 §11: `- **MIUI-specific launcher-shortcut permission guide** - ... the OEM-specific permission walkthrough is still absent.`
- Part 1 §17: `- **Show/hide unimportant settings** (SmartPrefCategory).`
- Part 1 §17: `- **Accent color** picker (ssilencce derives accent from theme modes/cover).`
- Part 1 §18: `- **Guides per OEM** (MIUI, EMUI/Huawei, Samsung, OPPO, Realme, Vivo, ASUS) for battery optimization / app-killing, **pin in recents**, auto-start, disable optimization.`

Reword (no longer missing):
- Part 1 §18: keep the "Background-playback opt-in (Android 11+), unrestricted battery access
  (Android 15+), WRITE_SETTINGS for status bar" bullet only if those are still absent; the OEM
  guide itself is implemented.
- Appendix A §20: remove "disable loading everywhere" from the Musicolet-only list.
- Appendix A §21: remove "the remaining gap is the MIUI-specific permission guide".

## docs/CODE_MAP.md

Add:
- `app/src/main/java/com/ssilencce/util/oem/PowerManagementGuide.kt` — OEM detection (Xiaomi,
  Huawei, Samsung, OPPO, Realme, Vivo, ASUS, generic), battery-optimization status/intent,
  app-details fallback, known auto-start intents and the MIUI shortcut-permission intent.
- `app/src/main/java/com/ssilencce/ui/screen/settings/BatteryGuideFragment.kt` — preference
  screen that renders the OEM power-management guide and launches the intents.
- `app/src/main/java/com/ssilencce/ui/screen/settings/AccentColorDialog.kt` — Compose bottom
  sheet for preset/custom accent selection.
- `app/src/main/java/com/ssilencce/ui/screen/settings/SettingsVisibilityPolicy.kt` — keys of the
  advanced preferences hidden by the "Show unimportant settings" toggle, plus the empty-category
  hiding rule.
- `app/src/main/res/xml/preferences_screen_battery.xml` — Battery & background screen layout.

Update:
- `ui/theme/Theme.kt` — `SsilencceTheme` applies the custom accent color.
- `core/model/theme/AppTheme.kt` — passes the custom accent seed to `DynamicColors`.
- `util/Preferences.kt` — new keys `accent_color`, `show_unimportant_settings`,
  `disable_album_art`.
- `coil/fetcher/*` — short-circuit when album art is disabled.
- `ui/screen/settings/PreferencesScreenFragment.kt` — hides unimportant preferences and opens
  the accent dialog.

## ADR

No new architectural decision: the feature reuses Material dynamic colors + the existing
m3color scheme generator and the existing PreferenceFragment settings infrastructure.

## Decisions

- `show_unimportant_settings` defaults to **true** so the default settings layout is unchanged;
  turning it off hides the curated advanced set. (Protocol requires defaults to preserve
  current behavior; Musicolet's hidden-by-default variant would change the initial layout.)
- The accent preference is disabled while Aurora Glass is active, because Aurora intentionally
  uses a fixed white accent. A saved accent applies again when Aurora is turned off.
- Legacy views receive the accent through `DynamicColors` content-based schemes, which Material
  exposes on Android 12+; Compose receives it on every supported API through `m3color`.
- Battery optimization is opened via `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` because the
  manifest does not declare `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; if that permission is added
  later, the helper automatically switches to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
