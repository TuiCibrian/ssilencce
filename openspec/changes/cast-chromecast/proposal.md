# Chromecast support (github + playstore flavors)

## Why

The app can only play on the device itself. Users with a Chromecast or Google TV
expect to send the current queue to the TV/speaker without losing their place.

## What Changes

- Add `androidx.media3:media3-cast` (same media3 version as the rest of the stack)
  to the `github` and `playstore` flavors only. `fdroid` stays GMS-free and receives
  a no-op implementation.
- Add flavor source sets:
  - `src/cast/java`: real Cast integration (`CastPlayer`, Cast SDK session
    handling, embedded LAN HTTP server).
  - `src/nocast/java`: no-op `CastPlaybackProvider` used by `fdroid`.
- A Cast button in the now-playing toolbar. It opens the system Cast device
  chooser. In `fdroid` the item is hidden.
- When a Cast session starts, the service snapshots the local queue, starts a
  small HTTP server bound to the LAN, rewrites the queue to HTTP media items
  (audio + artwork), hands them to a `CastPlayer`, and switches the media session
  to the `CastPlayer` without releasing the session (so notification, lock screen,
  Android Auto and in-app controllers keep working).
- When the Cast session ends, the queue/position are transferred back to the
  local `ExoPlayer`, and the media session is switched back.
- Local files are streamed from the phone while casting: the server is started
  only for the duration of a Cast session and stops when casting ends.
- Artwork is served from the same server through the existing cover provider.

## Out of scope

- "Decode on device" (streaming processed PCM/WAV to the receiver): requires a
  clock-synchronised live encoder and cannot be done reliably through the Cast
  receiver's pull-based HTTP model. It is not implemented; see `DOCS_DELTA.md`.
- Remote control of local-only DSP (equalizer, replay gain, A-B repeat, per-song
  tempo) while casting. Those remain local features.
