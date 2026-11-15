# Docs delta — Now Playing extras

> Docs agent: apply these edits to the listed docs. Do not edit the OpenSpec change.

## docs/FEATURES.md — Section 7 "Now Playing Screen"

Add these bullets after **Gestures** (or next to the related existing bullets):

- **Keep screen on** — Optional default-off switch keeps the display awake while the expanded Now Playing panel is visible; cover lyrics continue to keep the screen awake independently, and dialogs do not.
- **Fullscreen artwork** — A Compose viewer opens from the cover tap action (`Open fullscreen art`) or the player overflow menu, with pinch-to-zoom, save to the gallery (`MediaStore.Images`), and set-as-wallpaper for home, lock screen or both (requires the normal `SET_WALLPAPER` permission; pre-Android 10 gallery writes use the storage permission already requested at startup).
- **Share Now Playing** — The player overflow menu renders the full player view to a JPEG and opens the share sheet, with an optional centered 1:1 crop for social apps; the temporary file is exposed through the existing FileProvider authority.
- **Seek helpers** — Tapping the elapsed-time label or the overflow `Seek to...` item opens a Compose dialog that accepts `mm:ss` (or `h:mm:ss`) input and clamps to the track duration. A separate default-off `Touch only on thumb` preference makes both the legacy `MusicSlider` and the Compose Aurora slider ignore track taps so only dragging the thumb seeks.
- **Quick-access row** — Optional user-selected shortcuts (lyrics, equalizer, sleep timer, tempo, queue, tag editor, add to playlist) render as a row in Now Playing. The default is empty (no visual change). Settings offer a checklist with up/down reordering; the Aurora screen places the row in its controls column and legacy styles use a Compose overlay at the top of the player.

Also update the **Gestures** bullet to mention the new `Open fullscreen art` single-tap target, and the **Display options** bullet to mention `Keep screen on` and `Touch only on thumb`.

## docs/MISSING_FEATURES.md

Part 1 §7 "Tags, lyrics & album art":
- Remove: `- **Album-art save to gallery** (fullscreen zoom viewer).`
- Remove: `- **Set album art as lock-screen wallpaper** (own lock screen).`

Part 2 §8 "File operations":
- Remove: `- **Share screenshot of Now Playing** (with Instagram-fit crop option).`

Part 2 §9 "Now Playing screen":
- Replace `- **Seekbar helper buttons** — seek-to precise position, cancel seeking, touch-only-on-thumb.` with
  `- **Seekbar helper buttons** — cancel seeking.`
- Remove: `- **Keep screen on**.`
- Remove: `- **Custom quick access shortcuts**.`
- Remove: `- **Album-art tap action** customization.`

Appendix A, album art comparison bullet:
- Replace `- **Album art change/remove** — both support it; Musicolet adds fullscreen zoom, save to gallery, lock-screen wallpaper.` with
  `- **Album art change/remove** — both support it; both also offer a fullscreen zoom viewer, save to gallery and wallpaper targets (ssilencce: home, lock screen or both).`

Section 20 "Album art handling":
- Update the `ssilencce:` line to add `fullscreen zoom viewer, save to gallery and home/lock/both wallpaper` before the disable-loading gap.
- Replace `- See Part 1 §7 (zoom viewer, gallery, wallpaper, disable-loading).` with
  `- Remaining Part 1 §7 gap: disable album-art loading everywhere.`

## docs/CODE_MAP.md — new files

Add entries (same Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed by / Related files / Notes format):

- `core/model/action/QuickAccessAction.kt` — enum of the seven configurable Now Playing shortcuts with icon/title resources and comma-separated preference parsing/serialization. Pure model; consumed by Preferences, AbsPlayerFragment, AuroraPlayerScreen and QuickAccessPreferenceDialog.
- `ui/component/compose/QuickAccessRow.kt` — Compose horizontal row of icon buttons for the selected QuickAccessAction list; consumes QuickAccessAction and emits selection. Used by AuroraPlayerScreen and the legacy player overlay.
- `ui/component/preferences/dialog/QuickAccessPreferenceDialog.kt` — Compose settings dialog that toggles and reorders QuickAccessAction entries and persists them through `Preferences.quickAccessActions`. Shown from `PreferencesScreenFragment` for `now_playing_quick_access`.
- `ui/screen/player/PlayerScreenshot.kt` — renders/crops a player bitmap to a JPEG in `cacheDir/now_playing_share/`, exposes it through FileProvider, and builds the `ACTION_SEND` intent. Used by AbsPlayerFragment.
- `ui/screen/player/SeekTimeParser.kt` — pure `mm:ss` / `h:mm:ss` parser, duration clamp and total-minutes formatter for the seek dialog and elapsed-time label.
- `ui/screen/player/SeekToDialogFragment.kt` — Compose `material3` dialog for precise seeking; reads the activity-scoped PlayerViewModel, validates the input and calls `seekTo`. Opened from elapsed-time taps and the player overflow menu.
- `ui/screen/player/art/ArtworkStore.kt` — coroutine/IO artwork loading (song cover with CoverProvider fallback), MediaStore gallery insert (Q+ pending flow, legacy scanner path), and WallpaperManager home/lock/both application. Consumed by FullScreenArtDialogFragment.
- `ui/screen/player/art/FullScreenArtDialogFragment.kt` — full-screen Compose album-art viewer (pinch zoom, save, wallpaper target menu, safe-area toolbars). Hosted as a DialogFragment from AbsPlayerFragment.

## docs/CODE_MAP.md — updated files

- `ui/component/base/AbsPlayerFragment.kt` — add: keep-screen-on owner (`applyKeepScreenOn`, preference listener, player/lyrics visibility), fullscreen-art and share-screenshot menu actions, `Seek to...` menu action, quick-access overlay injection for non-Aurora styles, and QuickAccessAction dispatch.
- `ui/component/base/AbsPlayerControlsFragment.kt` — add: elapsed-time click opens `SeekToDialogFragment`; `MusicSlider.touchOnlyOnThumb` applied and updated when `seek_bar_touch_only_on_thumb` changes.
- `ui/component/views/MusicSlider.kt` — add: `touchOnlyOnThumb` mode that blocks gestures outside the thumb hit radius while leaving dragging intact.
- `ui/screen/player/aurora/AuroraPlayerScreen.kt` — add: quick-access row in the controls column, elapsed-time seek dialog callback, thumb-only overlay for the Compose slider, new overflow items, and `MusicSlider.touchOnlyOnThumb` wiring.
- `ui/screen/player/cover/CoverPagerFragment.kt` — keep-screen-on checks now include the new preference and are complemented by AbsPlayerFragment's owner; lyrics visibility still drives cover screen-on.
- `ui/screen/settings/PreferencesScreenFragment.kt` — quick-access preference dialog and summary wiring; single-tap cover action summary refresh.
- `util/Preferences.kt` — new keys/accessors: `keep_screen_on_now_playing`, `seek_bar_touch_only_on_thumb`, `now_playing_quick_access`.
- `core/model/action/NowPlayingAction.kt` — new `ShowFullScreenArt` action.
- `res/xml/preferences_screen_now_playing.xml` — Keep screen on, Touch only on thumb and Quick-access shortcuts preferences.
- `res/menu/menu_now_playing.xml` — fullscreen-art, seek-to and share-now-playing items.
- `res/xml/provider_paths.xml` — `now_playing_share/` cache path for the screenshot.
- `AndroidManifest.xml` — `SET_WALLPAPER` permission.

## ADR

No new ADR: the change extends existing player/settings patterns (DialogFragment + Compose, activity-scoped PlayerViewModel, FileProvider, MediaStore/WallpaperManager). No schema, dependency or navigation-graph change.

## Decisions

- **Tempo shortcut target:** there is no standalone tempo dialog; the shortcut opens Sound settings, where the Speed and pitch controls live.
- **Legacy quick-access placement:** non-Aurora styles receive the row as a Compose overlay anchored at the top of the player root (the Aurora screen renders it inside its controls column). With no selected actions the row composes to nothing, preserving the default layout.
- **Keep-screen-on ownership:** `AbsPlayerFragment` owns `FLAG_KEEP_SCREEN_ON`; cover lyrics remain an independent trigger. The cover pager retains its destination check so opening a dialog over cover lyrics still releases the flag when the preference is off.
- **Screenshot format:** JPEG at quality 95, centered square crop, stored under `cache/now_playing_share/` and shared through `${applicationId}.provider`.
- **Gallery/wallpaper quality:** artwork is decoded at up to 2048 px with a CoverProvider fallback; save-to-gallery uses the MediaStore pending flow on Q+ and the public Pictures folder plus media scanner on API 26–28.
