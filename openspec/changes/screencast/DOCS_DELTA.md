# Docs delta — screencast

Apply these edits in the docs agent pass. Do not copy this file into the docs.

## docs/FEATURES.md — §8 Network & Integrations

Add this bullet after the Chromecast bullet (after **Built-in updater** if the Chromecast bullet is
not present yet):

- **Screencast without mirroring** — the Now Playing screen (cover, title, artist, album, lyrics
  and progress) is rendered on an external HDMI, Miracast/wireless display or Chromecast-as-display
  window through `android.app.Presentation`. The phone screen is not mirrored and playback keeps
  running in the local `ExoPlayer` pipeline, so the equalizer, ReplayGain, crossfade and gapless
  chain stay active. The Now Playing menu opens a toggle/display picker with wireless-display
  discovery and a shortcut to the system display settings; the presentation closes automatically
  when the display disconnects.

## docs/MISSING_FEATURES.md — §13 Cast & Screencast (PRO)

Replace the bullet:

- **Screencast** - Chromecast and Miracast / Wireless Display without mirroring; EQ, ReplayGain and
  crossfade keep working during screencast. (Entirely absent from ssilencce.)

with:

- **Screencast** — implemented for external presentation displays (HDMI, Miracast/wireless display,
  Chromecast-as-display routes exposed by the OS): the Now Playing screen is shown with
  `android.app.Presentation` without mirroring, and audio stays in the local playback pipeline so
  EQ, ReplayGain and crossfade keep working. Remaining gap vs Musicolet: per-screencast-device EQ
  profiles (still listed under §4).

## docs/CODE_MAP.md — new entries

Add these file entries (same Purpose / Responsibilities / Inputs / Outputs / Dependencies /
Consumed by / Related files / Notes shape as the rest of the file), under `ui/screen/player/`:

- `ui/screen/player/screencast/ScreencastManager.kt` — process-level owner of the external-display
  presentation and its playback snapshot.
  - **Purpose:** Discover presentation displays and drive a full-screen Now Playing `Presentation`
    without mirroring the phone.
  - **Responsibilities:** Enumerate `DisplayManager.DISPLAY_CATEGORY_PRESENTATION` displays plus the
    `MediaRouter` live-video selected route; show/hide the `Presentation`; open the system live-video
    chooser and cast/display settings; connect a read-only media3 `MediaController` to
    `PlaybackService`; publish metadata/playback/position to a `StateFlow`; resolve and load local
    lyrics per media item; stop and release on display removal or user disable.
  - **Inputs:** Application context, `Repository`, `LyricsRepository`, display/route events, player
    events.
  - **Outputs:** `displays`, `activeDisplayId` and `ScreencastNowPlayingState` StateFlows;
    Presentation lifecycle; toasts.
  - **Dependencies:** `DisplayManager`, `MediaRouter`/`MediaRouteChooserDialog`,
    `android.app.Presentation`, media3 `MediaController`/`SessionToken`, `ProgressObserver`,
    `ScreencastLyricsLoader`, `ScreencastPresentation`.
  - **Consumed by:** `AbsPlayerFragment`, `ScreencastPickerDialogFragment`, Koin `dataModule`.
  - **Related files:** `ScreencastPresentation.kt`, `ScreencastNowPlayingScreen.kt`,
    `ScreencastModels.kt`, `ScreencastLyricsLoader.kt`, `PlaybackService.kt`.
  - **Notes:** Audio is untouched (no `MediaProjection`, no route selection, no DSP changes). The
    500 ms progress ticker runs only while a presentation is visible and playback is playing; there
    is no wakelock and no polling loop. Requires the playback media session to be reachable; the
    display list refreshes on display add/remove/change.

- `ui/screen/player/screencast/ScreencastPresentation.kt` — external-display `Presentation` window.
  - **Purpose:** Host the Compose Now Playing screen on a chosen external display.
  - **Responsibilities:** Create the display `ComposeView`, force the dark/black theme, hide system
    bars, keep the display awake while presented and collect the manager state.
  - **Inputs:** Outer application context, `Display`, `ScreencastNowPlayingState` StateFlow.
  - **Outputs:** Full-screen window on the external display.
  - **Dependencies:** `android.app.Presentation`, ComposeView, `SsilencceTheme`,
    `ScreencastNowPlayingScreen`.
  - **Consumed by:** `ScreencastManager`.
  - **Related files:** `ScreencastNowPlayingScreen.kt`, `ScreencastManager.kt`.
  - **Notes:** `FLAG_KEEP_SCREEN_ON` applies to this presentation window only; the phone screen is not
    mirrored and its own keep-screen-on behavior is unchanged.

- `ui/screen/player/screencast/ScreencastNowPlayingScreen.kt` — Compose Now Playing surface for the
  external display.
  - **Purpose:** Render cover art, title/artist/album, lyrics and progress in a landscape or portrait
    layout.
  - **Responsibilities:** Adaptive wide/tall layout, Coil artwork, synced lyric line highlighting
    around the current position, first six lines of plain lyrics, instrumental/idle states, progress
    bar and elapsed/total time.
  - **Inputs:** `ScreencastNowPlayingState`.
  - **Outputs:** Composable UI.
  - **Dependencies:** Compose Material 3, Coil `AsyncImage`, `SyncedLyrics`, `asReadableDuration`.
  - **Consumed by:** `ScreencastPresentation`.
  - **Related files:** `ScreencastModels.kt`, `ScreencastPresentation.kt`.
  - **Notes:** Non-interactive display; no seek or transport controls. Artwork falls back to a music
    note placeholder and the idle state explains that nothing is playing.

- `ui/screen/player/screencast/ScreencastModels.kt` — presentation models.
  - **Purpose:** Define the display descriptor, lyrics presentation variants and Now Playing snapshot.
  - **Responsibilities:** `ScreencastDisplayInfo` (id/name/resolution), `ScreencastLyrics`
    (None/Instrumental/Plain/Synced), `ScreencastNowPlayingState` with derived progress.
  - **Inputs:** Display metadata, media metadata, parsed lyrics, player position/duration.
  - **Outputs:** Immutable state consumed by the manager, presentation and picker.
  - **Dependencies:** `SyncedLyrics`, Compose `@Immutable`, `Uri`.
  - **Consumed by:** `ScreencastManager`, `ScreencastPresentation`, `ScreencastPickerScreen`.
  - **Related files:** `ScreencastLyricsLoader.kt`, `ScreencastNowPlayingScreen.kt`.

- `ui/screen/player/screencast/ScreencastLyricsLoader.kt` — local-lyrics resolver for the external
  display.
  - **Purpose:** Reuse the per-song lyrics source selection without triggering network downloads.
  - **Responsibilities:** Honour an explicit SAF file selection, then walk file/embedded/stored
    sources in the automatic priority; return synced, plain or instrumental lyrics.
  - **Inputs:** `Song`, `LyricsRepository`.
  - **Outputs:** `ScreencastLyrics`.
  - **Dependencies:** `LyricsRepository`, `RawLyrics`, `LyricsSourceSelection`.
  - **Consumed by:** `ScreencastManager` (on IO).
  - **Related files:** `LyricsViewModel.kt`, `LyricsRepository.kt`, `ScreencastModels.kt`.
  - **Notes:** Deliberately calls `storedLyrics(song, allowDownload = false)` so a TV display can
    never start a lyrics download implicitly.

- `ui/screen/player/screencast/ScreencastPickerDialogFragment.kt` — Compose toggle/display picker
  dialog.
  - **Purpose:** Host the screencast controls from the Now Playing menu.
  - **Responsibilities:** Collect the manager's display/active flows with the view lifecycle, wire
    the switch, display selection, wireless-display chooser, system settings and close action;
    refresh displays on resume.
  - **Inputs:** `ScreencastManager` from Koin.
  - **Outputs:** Manager start/stop/select/chooser calls; Compose dialog UI.
  - **Dependencies:** `ScreencastManager`, `ScreencastPickerScreen`, `SsilencceTheme`.
  - **Consumed by:** `AbsPlayerFragment` (`action_screencast`).
  - **Related files:** `ScreencastPickerScreen.kt`, `menu_now_playing.xml`.

- `ui/screen/player/screencast/ScreencastPickerScreen.kt` — Compose picker/toggle content.
  - **Purpose:** Present the screencast switch, discovered displays, discovery/settings actions and
    the explanation that audio stays local.
  - **Responsibilities:** Scrollable dialog content, enable switch, selectable display rows with
    resolution, empty-display guidance, labelled action buttons.
  - **Inputs:** Displays, active display id and callbacks.
  - **Outputs:** Composable UI.
  - **Dependencies:** Compose Material 3, `ScreencastDisplayInfo`.
  - **Consumed by:** `ScreencastPickerDialogFragment`.
  - **Related files:** `ScreencastPickerDialogFragment.kt`, `ScreencastModels.kt`.

- `app/src/main/res/drawable/ic_screencast_24dp.xml` — external-display icon used by the player menu
  and picker.

## docs/CODE_MAP.md — entry updates

- `ui/component/base/AbsPlayerFragment.kt` — add to **Responsibilities**: update/check the
  `action_screencast` menu item and open `ScreencastPickerDialogFragment`.
- `MainModule.kt` — add to **Responsibilities**: bind the `ScreencastManager` singleton in
  `dataModule` with the app context, `Repository` and `LyricsRepository`.
- `app/src/main/res/menu/menu_now_playing.xml` — add the checkable `action_screencast` entry.
- `app/src/main/res/values/strings.xml` — add the English screencast strings next to the Cast
  strings.
- `app/src/main/AndroidManifest.xml` — no change was required: `Presentation`, `DisplayManager` and
  the live-video `MediaRouter` chooser need no additional permission or component.

## AGENTS.md — Repository Code Map

In the **ui/screen/** section (player bullet), append:

- `player/screencast/` — Presentation-based external-display Now Playing (`ScreencastManager`,
  `ScreencastPresentation`, Compose screen and toggle/display-picker dialog). Audio stays in the
  local ExoPlayer pipeline; no mirroring, no MediaProjection.

In the **Root & Build Configuration** bullet for `app/build.gradle.kts`, no dependency change is
needed: the feature reuses `androidx.mediarouter` and media3 from the main dependencies.

## docs/agents/domain.md

- Increment the ADR count by 2 from the current value (51 → 53) once ADR-052 (Chromecast) and
  ADR-053 (this change) are both added; if applied alone, increment to 52.
- Append to the key-stack-decisions paragraph: "ADR-053 renders screencast output as an
  `android.app.Presentation` on external presentation displays while audio stays in the local
  ExoPlayer/DSP pipeline (no mirroring)."

## New ADR

Create `docs/architecture/ADR-053-presentation-based-screencast.md`:

```markdown
# ADR-053: Presentation-Based Screencast to External Displays

## Status

Accepted (2026-11-14; runtime verification pending)

## Context

Users want the Now Playing screen on a TV, HDMI screen or Miracast/wireless display. Screen
mirroring (MediaProjection or the system cast mirroring service) sends the whole phone UI, captures
audio outside the app pipeline and costs battery. The app already has one `MediaLibrarySession`
owning ExoPlayer, EqualizerManager, ReplayGain, crossfade and gapless processing. Sending only the
visual Now Playing surface lets every audio feature keep working.

Android exposes secondary displays through `DisplayManager`'s presentation category and through
`MediaRouter` live-video routes. `android.app.Presentation` is the public, permission-free way to
draw an app window on a chosen display.

## Decision

- Discover external displays with `DisplayManager.getDisplays(DISPLAY_CATEGORY_PRESENTATION)` and
  union the `MediaRouter` live-video selected route; start wireless-display discovery with
  `MediaRouteChooserDialog` filtered by `MediaControlIntent.CATEGORY_LIVE_VIDEO`.
- Render a full-screen Compose Now Playing screen (cover, title, artist, album, synced/plain
  lyrics and progress) inside an `android.app.Presentation` owned by a process-level
  `ScreencastManager`. The presentation stops when its display disconnects.
- Feed the presentation from a separate read-only media3 `MediaController` connected to
  `PlaybackService`. Metadata changes are event-driven; a 500 ms main-thread ticker updates only
  the progress and lyric highlight while a presentation is visible and playback is playing.
- Never touch audio: no `MediaProjection`, no system mirroring route selection, no audio-route
  override and no DSP bypass. Audio continues through the existing ExoPlayer/equalizer/ReplayGain/
  crossfade chain; the OS decides whether output follows the display, Bluetooth, HDMI or the
  speaker.
- Expose the feature as a checkable Screencast entry in the Now Playing menu that opens a Compose
  toggle/display picker with discovery and a system display-settings shortcut.
- Load lyrics only from on-device sources (file, embedded, stored); the presentation never
  triggers a network lyrics download.

## Consequences

- The phone screen is not mirrored; private content stays on the phone and the external display
  shows only the Now Playing screen.
- Equalizer, ReplayGain, crossfade, mono, per-song tempo and gapless behavior are unchanged
  because the audio path is untouched.
- The phone must be connected to the display through the system (HDMI, Miracast/wireless display,
  or a Chromecast-as-display route exposed by the OS). The system may keep audio on the phone,
  Bluetooth or another output; the app intentionally does not force an audio route.
- The presentation is a process-level window and keeps updating while the app is backgrounded as
  long as the playback service/process is alive. There is no persistence: process death, display
  removal or user disable closes it.
- Synced lyrics use the stored offset and show the previous/current/next lines; plain lyrics show
  the first six lines. The display is non-interactive (no seek/transport controls).
- No new permission, Room schema, preference key or dependency is introduced.
```

## Decisions

- **Feature default:** off. The feature is runtime-only (no preference key); the user enables it
  from the Now Playing menu for the current display and it stops on display removal or process
  death. This keeps existing behavior unchanged for everyone else.
- **Audio routing:** left to the OS. The app deliberately does not call `MediaRouter.selectRoute`
  or set a preferred audio device; the requirement is that audio stays in the app pipeline, not
  that the app overrides the user's output choice.
- **Presentation appearance:** forced dark/black theme, because the external screen is a
  Now Playing surface and should not follow the phone's light theme or Material You wallpaper
  palette.
- **Lyrics:** local sources only, no network download; synced lines are limited to the
  previous/current/next three lines and plain lyrics to the first six lines to fit an external
  screen without scrolling.
- **Ticker:** 500 ms only while a presentation is visible and `isPlaying`; no wakelock and no
  background polling. The counter stops on pause/stop.
- **Chromecast:** this is display-based screencasting (the OS exposes the receiver as a
  presentation display). App-level Google Cast remains the separate `CastPlayback` integration and
  is not replaced.
- **Dependencies:** none added; `androidx.mediarouter` and media3 were already main dependencies.
```
