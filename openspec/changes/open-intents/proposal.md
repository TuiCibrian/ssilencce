# External audio mini player, previews, resume and mute policies

## Why

Opening an audio file from another app replaces the current queue and opens the full
player UI. There is no lightweight way to listen to a song without queueing it, and
Bluetooth/car stacks can resume unexpectedly or start playback while the device is muted.

## What Changes

- Add a Compose dialog-style `MiniPlayerActivity` for `ACTION_VIEW` audio intents with
  play/pause, seek, close and "open in app". Playback runs in a local ExoPlayer so the
  main queue is preserved until the user explicitly opens the app.
- Add a song-menu **Preview** action that plays a song in the same popup without queueing
  it; main playback is paused while the popup is open and restored afterwards.
- Add resume policies: "Only resume if paused by disconnect" and "Prevent unwanted
  autoplay" with a strict variant for buggy Bluetooth/car stacks.
- Extend mute handling: keep pausing/resuming on zero volume and add "Don't start
  playback when muted".
