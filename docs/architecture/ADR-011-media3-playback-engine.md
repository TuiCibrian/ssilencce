# ADR-011: Media3 (ExoPlayer) as the Playback Engine

## Status

Accepted

## Context

The app must provide gapless, reliable local audio playback with a system media session,
notification controls, Android Auto integration and external controller support. The legacy
engine was ExoPlayer; Media3 is its successor with first-class `MediaSession`,
`MediaLibraryService`, and notification primitives, and it is the maintained path on Android.

## Decision

- Use Media3 1.10.1 (`media3-exoplayer`, `media3-session`, `media3-exoplayer-midi`,
  `media3-ui-compose`; plus the Jellyfin fork `media3-ffmpeg-decoder` — see ADR-017).
- Run playback inside `PlaybackService`, a `MediaLibraryService` owning an `ExoPlayer` built
  with `setWakeMode(C.WAKE_MODE_LOCAL)`, audio attributes, and a custom
  `AlacWorkaroundCodecSelector` (ADR-017).
- Run the player on a dedicated `HandlerThread("Ssilencce-ExoPlayer")` with
  `Process.THREAD_PRIORITY_AUDIO` to keep decoding off the main thread and at audio priority.
- Expose a `MediaLibrarySession` (with `MediaLibrarySession.Callback`) so external controllers
  and Android Auto can browse the library (categories via `MediaIDs` hierarchical paths and
  `LibraryProvider`, see ADR-027).
- Notifications: `DefaultMediaNotificationProvider` with custom artwork decoding via
  `CacheBitmapLoader(CoilBitmapLoader)` (Coil-backed, 1024 px cap, memory cache disabled).
- Receive media buttons through `androidx.media3.session.MediaButtonReceiver` and register the
  service for `MediaSessionService`/`MediaLibraryService`/`MediaBrowserService` actions.
- Custom session commands/events for app-specific actions (shuffle toggle, repeat cycle,
  favorite, playback restore) defined centrally in `playback/Playback.kt`.
- UI connects through `MediaControllerOwner`, a lifecycle-aware owner of a `MediaController`
  with a connect state machine (Idle/Connecting/Connected/Disconnected), used by `MainActivity`
  and `PlayerViewModel`.

## Consequences

- Standard-compliant media session: notification, lock screen, Bluetooth, Android Auto and
  third-party controllers work out of the box.
- The dedicated audio-priority thread plus local wake mode balances responsiveness and battery
  (no wake locks beyond playback).
- `MediaLibrarySession` browsing surface is a second entry point to library data; it must stay
  in sync with the MediaStore-based repositories (ADR-007).
- Several Media3 APIs are `@UnstableApi` (e.g. custom shuffle order, ADR-012) — upgrades must
  re-verify those internal APIs.
