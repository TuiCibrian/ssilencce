# ADR-053: Flavor-Scoped Chromecast with a media3 CastPlayer Facade

## Status

Accepted (2026-11-14; runtime verification pending)

## Context

The library is local, but users want to send the current queue to a Chromecast/Google TV
receiver. The app already has a single `MediaLibrarySession` that owns ExoPlayer, notification,
lock screen, widgets, Android Auto and in-app controllers. Switching to a second, separately
released session would disconnect every controller and break the notification.

The Cast SDK is GMS-only; the fdroid flavor must remain GMS-free. The repository still runs a
single `:app` module with product flavors, so the integration must compile from one shared
`main` source set.

## Decision

- Keep one `MediaLibrarySession`. Media3's `cast.CastPlayer` facade wraps the existing local
  `Player` and switches its active delegate to `RemoteCastPlayer` when a Cast session becomes
  available, transferring playback state before the previous player is stopped. The session is
  built against this facade, so the notification, System UI, Android Auto and in-app
  `MediaController` keep working without reconnection or a second session.
- Put the contract (`CastPlayback`) in `main`, the real implementation in `src/cast` (added to
  the `github` and `playstore` source sets) and a no-op provider in `src/nocast` (added to
  `fdroid`). Only the GMS flavors declare `androidx.media3:media3-cast` (existing media3
  version) and the Cast options-provider manifest entry.
- Use a custom `MediaItemConverter` for `RemoteCastPlayer`: local content/file URIs are rewritten
  to tokenised HTTP URLs served by a small embedded `ServerSocket` on the LAN, while the original
  local item is preserved for reading the remote timeline back. The server starts lazily on the
  first conversion and stops on session end/service teardown. It serves audio with HTTP `Range`
  and the cover-provider artwork, and is not exposed beyond the LAN.
- Use Google's default media receiver (`CC1AD845`) through `SsilencceCastOptionsProvider`; no
  custom receiver app is required because the sender serves the media.
- Route generic transport, repeat, favorite and queue-move requests to the active player. Keep
  local-only behavior (per-track resume persistence, equalizer/replay gain/mono/A-B repeat,
  per-song tempo, queue stop-at-position, named-queue switching, zero-volume pause, position
  observer, fade in/out, crossfade, shuffle order) on the local ExoPlayer and document the
  limits.

## Consequences

- One session and one notification continue to describe the audible player; no controller
  reconnection is needed when casting starts or stops.
- `fdroid` stays GMS-free: no Cast classes, no Cast metadata, no `media3-cast`.
- Cast playback runs through the receiver, so local DSP (EQ, ReplayGain, mono, A-B repeat,
  per-song tempo, crossfade) and the local fade/volume controllers do not apply while casting.
  The Cast receiver also does not support shuffle mode through `RemoteCastPlayer`; the queue is
  played in its stored order. Repeat, seek, play/pause, next/previous, favorite, history and
  scrobbling continue to work because media ids, metadata and the media-session pipeline are
  unchanged.
- The phone must stay reachable on the same LAN and the playback service must stay alive while
  casting (the foreground media service already covers this); no additional wakelock is taken.
- "Decode on device" is intentionally not implemented: streaming processed PCM/WAV would need a
  clock-synchronised live encoder and cannot be delivered through the receiver's pull-based HTTP
  model reliably.
- The server is tokenised and read-only, but it is an HTTP listener on the local network; the
  tokens are unguessable and expire when the session/server stops.

## Related

- ADR-003 (flavors and source sets), ADR-011 (Media3 playback engine), ADR-052 (dual-player
  crossfade), ADR-054 (presentation-based screencast).
- `openspec/changes/cast-chromecast/`, `openspec/changes/merge-cast-chromecast/`.
