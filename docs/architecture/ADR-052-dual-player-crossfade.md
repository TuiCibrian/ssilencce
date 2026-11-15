# ADR-052: Dual-Player Crossfade

## Status

Accepted.

## Context

The playback engine uses a single ExoPlayer per `PlaybackService`; the MediaSession, queue,
persistence, widgets, notifications and scrobbling all observe that one player. Users want a
configurable overlap between tracks, which a single ExoPlayer cannot produce: it can only
play one media item at a time, and its gapless path is the opposite of an overlap.

## Decision

Introduce an opt-in crossfade (0–12 s, default off) implemented with a secondary ExoPlayer:

- The main player keeps owning the queue, MediaSession, persistence, audio focus, wake lock,
  widgets, notifications, scrobbling and sleep timer. It stays the only logical player.
- A lazily created secondary `CrossfadePlayer` mirrors the renderer stack (balance, mono,
  ReplayGain, ALAC selector, extractor/seek options, skip-silence, float output), uses its own
  audio-priority playback thread, shares the main player's audio session id and runs with
  `WAKE_MODE_NONE` and audio focus disabled. It uses the same extra-format extractor stack as
  the primary player.
- Before the outgoing track ends, `CrossfadeController` pre-buffers the next queue item on the
  secondary player. At the fade boundary both players run complementary equal-power volume
  ramps (the main player's ramp is a multiplier in `PlaybackVolumeController`, composed with
  the existing base/duck/fade multipliers).
- The controller commits by seeking the main player to the incoming item at the position the
  secondary already reached, then fades the secondary out over a short handoff and clears its
  media items. The queue therefore advances normally and all observers keep working.
- A commit reaches the player as a seek; the service maps that single transition to
  `MEDIA_ITEM_TRANSITION_REASON_AUTO` so play counts, Last.fm/ListenBrainz scrobbles and
  sleep-timer song counting match a normal queue advance.
- Crossfade is suppressed for repeat-one, A-B repeat, stop-after-current-song, sleep-timer
  stop-at-end, repeat-load-next-and-pause, the queue's stop position, and same-album
  consecutive tracks while gapless playback is enabled (preference, default on). It is also
  suppressed while a Chromecast receiver owns playback (ADR-053).
- Manual "next" is intercepted through `AdvancedForwardingPlayer`; when the optional manual-skip
  crossfade is enabled it uses the same engine with the overlap capped at 4 seconds. A repeated
  skip aborts immediately. Previous cancels an in-flight crossfade before the normal
  restart-versus-previous behavior.

## Consequences

- Two audio pipelines can exist during the overlap. Equalizer, reverb and virtualizer remain
  effective because both players share the audio session; ReplayGain for the incoming track is
  resolved off the main thread and applied before or during the fade.
- The secondary player is kept allocated after first use to avoid decoder setup cost on every
  track; it holds no wake lock, audio focus or media items while idle.
- Any pause, seek, queue/timeline change, speed change or incompatible stop policy aborts the
  crossfade and restores normal main-player volume.
- Crossfade stays off by default, so existing gapless behavior is unchanged for users who do
  not enable it.

## Related

- ADR-011 (Media3 playback engine), ADR-014 (audio processing pipeline), ADR-045 (app commands),
  ADR-053 (flavor-scoped Chromecast).
- `openspec/changes/crossfade/`, `openspec/changes/merge-crossfade/`.
