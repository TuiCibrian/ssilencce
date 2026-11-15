# Docs delta — notification-styles

## Decisions

- The default remains the Media3 system style (`NotificationStyle.System`), so upgrading
  does not change the notification. The custom styles are opt-in.
- The custom styles are rendered by `CustomLayoutMediaNotificationProvider`, a
  `MediaNotification.Provider` implementation that builds `RemoteViews`. It uses
  `MediaStyleNotificationHelper.DecoratedMediaCustomViewStyle` so the notification stays
  associated with the MediaSession. Android 13 and later may ignore custom media
  notification layouts; the style setting summary documents that the style applies only
  where the system honors custom layouts.
- `Small` uses the compact button layout, `BigSimple` uses the expanded button layout, and
  `BigAdvanced` uses a fixed previous / play-pause / next / favorite / add-to-playlist row
  plus the next three upcoming songs. Favorite and add-to-playlist reuse the session
  commands added by notification-plus. The replace-close-with-add-to-playlist preference is
  also honored by the custom styles.
- The always-expanded option uses the big custom layout as the collapsed content view.
  Android 11 and later may clamp or ignore taller custom notification views; the option is
  disabled for `Small` and the platform limitation is described in its summary.
- Big layouts set the custom big content view, so they remain expandable on Android 10 and
  earlier. The seek bar is shown only when the track has a duration and seek controls are
  available. While a big style is playing, a one-second main-thread refresh updates the
  progress and current position; the refresh stops when playback pauses, the screen style
  changes, or the service is destroyed. No extra thread, wakelock, or background poll is
  used.
- Background modes: `Theme` resolves `colorSurfaceContainerHigh` from the app theme;
  `Cover` uses `PaletteProcessor` on the current artwork and falls back to the theme until
  the palette is ready (or when artwork is not broadcast); `Custom` stores an ARGB color
  picked from a Compose bottom sheet. Title/subtitle/icon colors are derived from the
  background luminance for custom colors and from the palette for cover colors.
- Artwork is loaded through the session `BitmapLoader` and cached by artwork URI/data hash;
  palette computation is cached per artwork key. Loading and palette extraction are
  asynchronous and do not block the main thread.

## docs/FEATURES.md

Add under **9. UI, Themes & System**, after the **Widgets** bullet:

- **Notification styles** - System (Media3, default), Small, Big simple and Big with
  upcoming songs, rendered by a custom Media3 `MediaNotification.Provider` with RemoteViews.
  Big styles show artwork, title/artist, a progress bar with elapsed/total time and the
  configured compact/expanded buttons; the advanced style adds favorite and add-to-playlist
  actions plus the next three upcoming songs. Optional always-expanded mode uses the big
  layout while collapsed. Backgrounds can follow the app theme, the cover palette, or a
  custom color. The custom big content view keeps notifications expandable on Android 10
  and earlier; Android 13 and later may replace custom media layouts with the system media
  controls.

## docs/MISSING_FEATURES.md

In Part 1 §12, replace the whole bullet list with:

```markdown
- Android 13 and later may ignore custom notification layouts and show the system media
  controls instead; the custom styles apply only where the platform honors custom media
  RemoteViews.
```

In Part 2 §15, replace the ssilencce line with:

```markdown
- ssilencce: system Media3 media notification by default, plus opt-in Small / Big simple /
  Big with upcoming songs custom RemoteViews styles, configurable compact/expanded buttons,
  seek bar, favorite and add-to-playlist actions, always-expanded, and theme / cover-palette
  / custom notification backgrounds. Android 13+ may use system media controls instead of
  the custom layouts.
```

## docs/CODE_MAP.md

Add under `core/model/player/` (after the notification-plus `NotificationButton.kt` entry):

- `core/model/player/NotificationStyle.kt` — Notification styles, background modes and
  per-style button policy.
  - **Purpose:** Describe the selectable notification presentations and which actions each
    custom style shows.
  - **Responsibilities:** Stable stored values for styles/backgrounds; style flags
    (`isCustom`, `showsSeekBar`, `showsUpcomingSongs`); compact/expanded/big-advanced button
    selection with availability filtering.
  - **Inputs:** `NotificationButtonLayout`, availability predicate.
  - **Outputs:** `NotificationStyle`, `NotificationBackgroundMode`, ordered
    `NotificationButton` lists.
  - **Dependencies:** `NotificationButton`, Android string resources.
  - **Consumed by:** `Preferences`, `CustomLayoutMediaNotificationProvider`,
    `NotificationBackgroundColorDialog`, settings fragment.
  - **Related files:** `NotificationButton.kt`, `Preferences.kt`,
    `preferences_screen_playback.xml`.
  - **Notes:** `System` is the default; compact is capped at 3, other styles at 5.

Add under `playback/notification/` (after the notification-plus provider/artwork entries):

- `playback/notification/CustomLayoutMediaNotificationProvider.kt` — RemoteViews media
  notification provider.
  - **Purpose:** Render the opt-in small/big/advanced notification styles with custom
    layouts.
  - **Responsibilities:** Build small/big RemoteViews (artwork, title, artist, progress,
    upcoming songs, action slots); build player/session actions via the Media3
    `ActionFactory`; honor compact/expanded and replace-close preferences; load and cache
    artwork; extract and cache cover palettes; resolve theme/cover/custom backgrounds and
    contrasting text/icon colors; apply always-expanded behavior.
  - **Inputs:** `MediaSession`, `MediaNotification.ActionFactory`, notification channel info,
    favorite/seek state providers, `Preferences`, `MediaMetadata`.
  - **Outputs:** `MediaNotification` with custom content/big content views.
  - **Dependencies:** Media3 session/notification APIs, Coil-backed session
    `BitmapLoader`, `PaletteProcessor`, `Playback` session commands.
  - **Consumed by:** `PlaybackService.setMediaNotificationProvider` when a custom style is
    selected.
  - **Related files:** `SsilencceMediaNotificationProvider.kt`, `NotificationStyle.kt`,
    `notification_custom_small.xml`, `notification_custom_big.xml`.
  - **Notes:** Async artwork/palette updates call the provider callback; Android 13+ may
    ignore custom media layouts.

Add under `ui/component/preferences/dialog/`:

- `ui/component/preferences/dialog/NotificationBackgroundColorDialog.kt` — Custom
  notification background color sheet.
  - **Purpose:** Let the user pick an exact background color for custom notification
    styles.
  - **Responsibilities:** Preset swatches, HSV sliders, live hex preview, persist the chosen
    ARGB color and switch the background mode to custom.
  - **Inputs:** `Preferences.notificationCustomBackgroundColor`, current theme palette.
  - **Outputs:** Updated custom color/background preferences.
  - **Dependencies:** Compose Material3, `SsilencceTheme`, `Preferences`.
  - **Consumed by:** `PreferencesScreenFragment`.
  - **Related files:** `preferences_screen_playback.xml`, `NotificationStyle.kt`.

Update existing entries:
- `playback/PlaybackService.kt` — installs the system or custom provider from
  `notificationStyle`; swaps providers on preference change; refreshes custom notification
  progress every second only while a big style plays; releases the custom provider and
  ticker on destroy.
- `util/Preferences.kt` — notification style, background mode, custom background color and
  always-expanded keys.
- `ui/screen/settings/PreferencesScreenFragment.kt` — notification style/background rows,
  always-expanded enablement, custom color sheet wiring and summaries.

## AGENTS.md

In the Repository Code Map:
- `core/model/` add `player/NotificationStyle.kt` (notification styles, background modes
  and per-style button policy).
- `playback/` add `notification/` (system and custom Media3 notification providers,
  lock-screen artwork policy).
- `ui/screen/settings/` mention the notification button editor, notification style /
  background rows and the custom background color sheet.

## docs/agents/domain.md

After creating the ADR below, increment the ADR count and append a sentence to the ADR
summary line: the custom notification styles add an opt-in RemoteViews provider while the
system Media3 notification remains the default, and Android 13+ may ignore custom media
layouts.

## ADR — Custom notification styles

**Status:** Proposed (assign the next free ADR number when applying; notification-plus may
already claim ADR-052).

**Context:** Media3's system style cannot show custom layouts, an upcoming-song list, a
seek bar with custom colors, or a cover-derived background. Musicolet offers small / big
simple / big advance notification styles. Android media notifications are platform-owned:
custom RemoteViews are honored on older versions, while Android 13+ may replace them with
system media controls. The existing system Media3 notification must remain the default so
current behavior is unchanged.

**Decision:** Add a persisted `NotificationStyle` (System / Small / BigSimple /
BigAdvanced, default System). For custom styles install
`CustomLayoutMediaNotificationProvider`, a `MediaNotification.Provider` that builds
RemoteViews with `DecoratedMediaCustomViewStyle`, reuses the notification-plus
compact/expanded layout and session commands, and adds seek-bar, upcoming-song and
background-color behavior. Background colors come from the app theme, the cover palette, or
a stored custom ARGB color. Big styles set a custom big content view so they remain
expandable on older Android versions; always-expanded uses the big layout as the collapsed
view. Progress refreshes run on the main thread only while a big style is playing.

**Consequences:** The default notification path is unchanged. Custom styles depend on the
platform honoring custom media layouts, so Android 13+ may silently fall back to the system
media controls; the setting summary documents this. The custom provider must keep its
actions in sync with available session commands and cache artwork/palette work off the main
thread. Always-expanded layouts may be clamped by Android 11+ notification height limits.
