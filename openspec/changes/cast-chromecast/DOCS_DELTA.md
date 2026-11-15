# Docs delta — cast-chromecast

Apply these edits in the docs agent pass. Do not copy this file into the docs.

## docs/FEATURES.md — §8 Network & Integrations

Add this bullet after **Built-in updater** (or next to the other integration bullets):

- **Chromecast** — GitHub and Play Store flavors only: Cast button in the now-playing toolbar (legacy overflow and Aurora header/overflow), system device chooser, queue/position hand-off between local `ExoPlayer` and media3 `CastPlayer`, and an embedded LAN HTTP server (audio with range requests and cover artwork) that runs only while casting. Repeat, play/pause, seek, next/previous and favorite keep working from the notification and in-app controls; the `fdroid` flavor has a no-op implementation and no Google Cast dependency.

## docs/MISSING_FEATURES.md — §13 Cast & Screencast (PRO)

Replace the bullet:

- **Chromecast** - Cast SDK, decode-on-device option, companion "cast plugin" app.

with:

- **Chromecast** — implemented in the `github`/`playstore` flavors (see `docs/FEATURES.md` §8); `fdroid` stays GMS-free and has no Cast support. Still missing vs Musicolet: the **decode-on-device** option (streaming the processed PCM/WAV signal) and the companion "cast plugin" app.

Keep **Screencast** and the **Cast plugin companion app detection** bullet under §16 unchanged.

## docs/CODE_MAP.md — new entries

Add these file entries (same Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed by / Related files / Notes shape as the rest of the file):

- `app/src/main/java/com/ssilencce/playback/cast/CastPlayback.kt` — flavor-agnostic Chromecast boundary.
  - **Purpose:** Defines the `CastPlayback` contract used by main source code so GMS and non-GMS flavors compile from the same code.
  - **Responsibilities:** Expose availability, menu setup, device chooser, session-player factory and release; no Cast SDK types leak into `main`.
  - **Inputs:** `Context`, `Activity`, `Menu`, local `Player`.
  - **Outputs:** A `Player` that is either the local player or a media3 `CastPlayer` facade.
  - **Dependencies:** Media3 `Player`; implementation supplied by the flavor source set.
  - **Consumed by:** `PlaybackService`, `AbsPlayerFragment`, `AuroraPlayerFragment`.
  - **Related files:** `src/cast/java/.../CastPlaybackImpl.kt`, `src/nocast/java/.../CastPlaybackProvider.kt`.
  - **Notes:** `CastPlaybackProvider` intentionally lives only in flavor source sets to avoid duplicate classes.

- `app/src/cast/java/com/ssilencce/playback/cast/CastPlaybackImpl.kt` — real Chromecast implementation.
  - **Purpose:** Creates the media3 `CastPlayer` facade and owns Cast SDK session lifecycle.
  - **Responsibilities:** Build `CastPlayer` around the local `Player`; register/unregister `SessionManagerListener`; start/stop the LAN server; show the Cast device chooser; surface the Cast menu entry; end the receiver session on service teardown; expose the process-wide `CastPlaybackProvider`.
  - **Inputs:** Application context, Cast SDK session events, player-menu inflation.
  - **Outputs:** Session player, chooser dialog, toasts, LAN server lifecycle.
  - **Dependencies:** `androidx.media3:media3-cast` (`CastPlayer`, `RemoteCastPlayer`, `MediaItemConverter`), `androidx.mediarouter`, `play-services-cast-framework`, `CastMediaServer`, `CastMediaItemConverter`, `SsilencceCastOptionsProvider`.
  - **Consumed by:** `PlaybackService`, `AbsPlayerFragment`, `AuroraPlayerFragment`.
  - **Related files:** `CastMediaServer.kt`, `CastMediaItemConverter.kt`, `SsilencceCastOptionsProvider.kt`, `src/cast/AndroidManifest.xml`.
  - **Notes:** Falls back to the local player when Google Play services/Cast is unavailable; the server starts lazily on the first media-item conversion.

- `app/src/cast/java/com/ssilencce/playback/cast/CastMediaServer.kt` — embedded LAN HTTP server.
  - **Purpose:** Serves the current queue's local audio and artwork to the Cast receiver while a session is active.
  - **Responsibilities:** Bind a random port to the LAN, register tokenised audio/artwork resources, parse minimal HTTP/1.1 GET/HEAD, serve `Range`/`206` responses with `Content-Length`, use `AssetFileDescriptor`/`FileInputStream` for content/file URIs, and stop/clear all resources when casting ends.
  - **Inputs:** `Context`, registered `Uri`s and MIME types, HTTP requests from the receiver.
  - **Outputs:** `baseUrl`, HTTP media streams, range responses.
  - **Dependencies:** `java.net.ServerSocket`, Android `ContentResolver`, `NetworkInterface`.
  - **Consumed by:** `CastPlaybackImpl.ensureServer`, `CastMediaItemConverter`.
  - **Related files:** `CastMediaItemConverter.kt`.
  - **Notes:** No wakelock and no polling loop; one daemon accept thread plus a cached daemon worker pool; URLs contain random UUIDs and only registered paths are served.

- `app/src/cast/java/com/ssilencce/playback/cast/CastMediaItemConverter.kt` — local↔Cast media-item adapter.
  - **Purpose:** Rewrites local content/file URIs to HTTP URLs before media3 sends them to the receiver.
  - **Responsibilities:** Register audio and artwork with the LAN server, set Cast-friendly MIME types/media type, delegate metadata/custom-data serialization to `DefaultMediaItemConverter`, and map remote timeline items back to the original local items (or reconstruct a local item from the media id after process recreation).
  - **Inputs:** Local `MediaItem`s, `MediaQueueItem`s from the remote timeline, `CastMediaServer`.
  - **Outputs:** `MediaQueueItem`s with HTTP URLs; local `MediaItem`s.
  - **Dependencies:** Media3 cast converter APIs, `CastMediaServer`, `CoverProvider`, `MimeTypeMap`.
  - **Consumed by:** `RemoteCastPlayer` in `CastPlaybackImpl`.
  - **Related files:** `CastMediaServer.kt`, `data/model/Song.kt`.
  - **Notes:** Keeps a media-id→local-item map; the map is per converter and is repopulated from media ids if the process restarted mid-session.

- `app/src/cast/java/com/ssilencce/playback/cast/SsilencceCastOptionsProvider.kt` — Cast options.
  - **Purpose:** Supplies Cast SDK options for the GMS flavors.
  - **Responsibilities:** Use Google's default media receiver (`CC1AD845`) and stop the receiver app when the session ends.
  - **Inputs:** `Context`.
  - **Outputs:** `CastOptions`.
  - **Dependencies:** `play-services-cast-framework`.
  - **Consumed by:** Cast SDK via `com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME`.
  - **Related files:** `src/cast/AndroidManifest.xml`.
  - **Notes:** The default receiver is sufficient because audio is served by the app over HTTP.

- `app/src/nocast/java/com/ssilencce/playback/cast/CastPlaybackProvider.kt` — fdroid no-op implementation.
  - **Purpose:** Keeps `fdroid` GMS-free while satisfying the main-source-set contract.
  - **Responsibilities:** Report unavailable, hide the Cast menu item, return the local player unchanged, no-op release.
  - **Inputs:** `Context`.
  - **Outputs:** `NoOpCastPlayback`.
  - **Dependencies:** `CastPlayback` only.
  - **Consumed by:** `PlaybackService`, player fragments.
  - **Related files:** `CastPlayback.kt`.
  - **Notes:** No Cast SDK or Play Services classes are referenced.

- `app/src/cast/AndroidManifest.xml` — cast-flavor manifest overlay.
  - **Purpose:** Declares the Cast options provider metadata for `github`/`playstore` only.
  - **Responsibilities:** `com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME` and `com.google.android.gms.version`.
  - **Inputs:** —
  - **Outputs:** Merged manifest metadata.
  - **Dependencies:** `SsilencceCastOptionsProvider`.
  - **Consumed by:** Android manifest merger.
  - **Related files:** `app/build.gradle.kts` (`manifest.srcFile`), `SsilencceCastOptionsProvider.kt`.
  - **Notes:** Deliberately absent from `fdroid` (verified clean merged manifest).

- `app/src/main/res/drawable/ic_cast_24dp.xml` — Cast icon used by the now-playing toolbar and Aurora overflow.

## docs/CODE_MAP.md — entry updates

- `app/build.gradle.kts` — add to **Responsibilities**: GMS-flavored Chromecast wiring (`src/cast` for `github`/`playstore`, `src/nocast` for `fdroid`, `media3-cast` dependencies, cast manifest overlay). Keep the version note.
- `gradle/libs.versions.toml` — note `media3-cast` uses the existing `media3` version.
- `app/src/main/java/com/ssilencce/playback/PlaybackService.kt` — add to **Responsibilities**: build the media session around the flavor session player (`CastPlayer` facade), route transport/queue/repeat/favorite commands to the active player, guard local-only persistence/DSP/position work while casting, and keep widgets/notification/sleep timer in sync with the active player.
- `app/src/main/java/com/ssilencce/data/model/Song.kt` — add to **Responsibilities**: `mimeType` derived from the file extension and set on `toMediaItem()` so Cast can choose the receiver MIME type without content-resolver lookups.
- `app/src/main/java/com/ssilencce/ui/component/base/AbsPlayerFragment.kt` — add: Cast menu item setup and click handling in the now-playing menu.
- `app/src/main/java/com/ssilencce/ui/screen/player/aurora/AuroraPlayerScreen.kt` and `AuroraPlayerFragment.kt` — add: Cast header action and overflow entry, shown only when Cast is available.

## AGENTS.md — Repository Code Map

In the **playback/** section add:

- `playback/cast/` — Flavor-agnostic `CastPlayback` contract (main) plus the real `CastPlayer` + Cast SDK session + embedded LAN HTTP server implementation (`src/cast`, github/playstore) and the GMS-free no-op provider (`src/nocast`, fdroid). The media session runs on the flavor session player and transfers queue/position across session availability.

In the **Root & Build Configuration** bullet for `app/build.gradle.kts`, append: “per-flavor Chromecast source sets (`src/cast` for github/playstore, `src/nocast` for fdroid) and `media3-cast` dependencies (same media3 version)”.

## docs/agents/domain.md

- Change “51 ADRs, ADR-001 through ADR-051” to “52 ADRs, ADR-001 through ADR-052”.
- Append to the key-stack-decisions paragraph: “ADR-052 adds flavor-scoped Chromecast (media3 `CastPlayer` facade + LAN HTTP server; github/playstore only, fdroid GMS-free).”

## New ADR

Create `docs/architecture/ADR-052-flavor-scoped-chromecast.md`:

```markdown
# ADR-052: Flavor-Scoped Chromecast with a media3 CastPlayer Facade

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

- Keep one `MediaLibrarySession`. Media3 1.10's `cast.CastPlayer` facade wraps the existing local
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
  observer, fade in/out, shuffle order) on the local ExoPlayer and document the limits.

## Consequences

- One session and one notification continue to describe the audible player; no controller
  reconnection is needed when casting starts or stops.
- `fdroid` stays GMS-free: no Cast classes, no Cast metadata, no `media3-cast`.
- Cast playback runs through the receiver, so local DSP (EQ, ReplayGain, mono, A-B repeat,
  per-song tempo) and the local fade/volume controllers do not apply while casting. The Cast
  receiver also does not support shuffle mode through `RemoteCastPlayer`; the queue is played in
  its stored order. Repeat, seek, play/pause, next/previous, favorite, history and scrobbling
  continue to work because media ids, metadata and the media-session pipeline are unchanged.
- The phone must stay reachable on the same LAN and the playback service must stay alive while
  casting (the foreground media service already covers this); no additional wakelock is taken.
- "Decode on device" is intentionally not implemented: streaming processed PCM/WAV would need a
  clock-synchronised live encoder and cannot be delivered through the receiver's pull-based HTTP
  model reliably.
- The server is tokenised and read-only, but it is an HTTP listener on the local network; the
  tokens are unguessable and expire when the session/server stops.
```

## Decisions

- **Feature default:** Chromecast is available wherever the Cast SDK is (github/playstore).
  There is no app-level master switch; the toolbar entry is hidden when Cast is unavailable and
  the no-op provider keeps fdroid behavior unchanged.
- **Decode on device:** not implemented (optional item). Reason: real-time PCM/WAV streaming to a
  Cast receiver requires a live encoder and clock synchronisation that the pull-based receiver
  protocol does not provide; documented as a remaining gap.
- **Shuffle while casting:** `RemoteCastPlayer` does not implement shuffle (`setShuffleModeEnabled`
  is a no-op in media3 1.10.1), so the receiver plays the transferred queue in stored order.
  Repeat and next/previous are supported. This is documented as a limitation rather than
  reordering the queue behind the user's back.
- **Local-only features while casting:** per-track resume persistence, EQ/ReplayGain/mono/A-B
  repeat, per-song tempo, queue stop-at-position, named-queue switching, zero-volume auto-pause,
  track-position observer and fade in/out stay on the local ExoPlayer. Generic transport,
  repeat, favorite, queue moves, history/scrobbling and widgets follow the active player.
- **Screen-cast / Miracast:** out of scope, remains missing.
