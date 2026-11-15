# Screencast without mirroring

## Why

Casting the whole phone screen to a TV wastes battery, exposes private content and forces the
system mirroring stack to handle audio. Users want the Now Playing screen (cover, title, lyrics
and progress) on a Miracast / wireless display, TV or HDMI screen while the app keeps decoding and
processing audio locally, so the equalizer, ReplayGain, crossfade and gapless chain stay intact.

## What Changes

- Add a process-level screencast manager that discovers presentation displays through
  `DisplayManager` (and `MediaRouter` live-video routes) and shows an `android.app.Presentation`
  with a full-screen Compose Now Playing screen on the selected external display.
- The presentation observes the playback session through its own media controller, so it keeps
  updating when the phone UI is closed; audio never leaves the existing `ExoPlayer` pipeline.
- Show cover art, title, artist, album, lyrics (synced or plain), play/pause state and progress.
- Add a Screencast entry to the Now Playing menu with an enable switch, display picker, wireless
  display chooser and a shortcut to the system display settings.
- Stop and close the presentation when its display disconnects.
