# ADR-054: Presentation-Based Screencast to External Displays

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

## Related

- ADR-028 (now-playing styles), ADR-053 (flavor-scoped Chromecast).
- `openspec/changes/screencast/`.
