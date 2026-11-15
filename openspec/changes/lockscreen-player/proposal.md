# Lock screen player

## Why

ssilencce has no custom lock screen experience. Musicolet offers a full-screen lock
screen player with playback controls, queue/lyrics, gestures, wallpaper choices and
library shortcuts, started automatically when the screen turns off during playback.
This change ports that feature as an opt-in, battery-conscious addition.

## What Changes

- New opt-in `lock_screen_player_enabled` preference (default off) and a dedicated
  "Lock screen player" settings screen (`Settings > Lock screen player`).
- New `LockScreenPlayerActivity`: a Compose, full-screen activity using
  `setShowWhenLocked`/`setTurnScreenOn`, with playback controls, seek bar, queue and
  lyrics panels, landscape layout, cover gestures (reusing `PlayerGesturesController`)
  and an upward-flick-to-dismiss gesture.
- Wallpaper options: blurred current cover or a user-picked image (persisted SAF URI).
- Library shortcuts for the current artist, folder, genre and the Favorites playlist;
  they open the matching detail screen through `MainActivity`.
- Visibility options: skip when the device is already unlocked and skip when playback
  has been paused for more than one minute; a preview action bypasses both.
- A dynamic `ACTION_SCREEN_OFF` receiver registered by `PlaybackService` only while
  playback is active (playing or about to play), so an idle/paused queue costs nothing.
- "Lock screen permissions" guide with OEM detection (reusing `PowerManagementGuide`),
  overlay permission status, app notification settings, battery guide and app details.
- New strings/arrays in the default `values` resources only; defaults preserve the
  current behavior (feature disabled).
