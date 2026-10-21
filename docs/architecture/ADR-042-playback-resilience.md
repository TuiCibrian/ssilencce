# ADR-042: Playback Resilience, Focus Modes and System Tile

## Status

Accepted on 2026-10-20. Amends ADR-011 and ADR-012.

## Context

Media3 already owns the session, notification and queue restoration, but the app lacked
per-track resume for long-form audio, configurable audio-focus behavior beyond an ignore switch,
a Quick Settings tile, and an explicit session-recovery policy after service recreation.

## Decision

- **Per-track positions** live in `TrackPositionEntity` (media id, position, updatedAt). The
  service updates an in-memory position while playing and writes at most every 15 seconds for
  tracks longer than 10 minutes, plus on pause, transition, completion and service destruction.
  All writes go through one application-owned serial queue so the final snapshot cannot be
  overtaken by an older write. Seeks refresh the transition sample, and a late bookmark lookup
  cannot override a newer user seek in the current item. A saved position below 5 seconds or
  within 5 seconds of the end is discarded. The first item after a queue-level restore keeps
  the restored session position instead of the per-track store.
- **Audio focus** has one owner: `AudioFocusController`. ExoPlayer's built-in focus handling is
  disabled. Modes are Normal, Ignore, Ask and Resume after call. Ask pauses and broadcasts an
  event; the activity confirms before focus is requested. If no UI can confirm, playback stays
  paused. Explicit pause (including no-op pause), cancel, abandon/mode change or release clears
  a pending Ask; repeated play keeps the same request. The restorable Compose dialog and
  same-UID consent commands are bound to its unique live request id (ADR-045).
  Transient loss pauses; on gain, any non-Ignore mode resumes if playback was active before the
  loss, so Normal/default and Resume after call both resume. Ducking lowers player volume and
  restores it.
- **Quick Settings** uses a `TileService` that connects a `MediaController` only while
  listening, mirrors active/paused/unavailable state from player events, and releases the
  controller when hidden. No polling.
- **Session recovery** persists `wasPlaying` with the existing playback state. The inactive
  marker is applied on Main when playback pauses, and a generation guard prevents an older
  delayed snapshot from restoring it. After a system service/process restart, the queue is
  restored and, when the opt-in setting is enabled and the session was active, playback resumes
  once. This does not and cannot bypass Android force-stop or an OS kill that never restarts
  the service.

## Consequences

- Long-form listening resumes per track; short tracks are unaffected and the feature is opt-in.
- Custom focus handling replaces a well-tested ExoPlayer path, so call/duck/gain cases require
  device verification.
- Ask mode without a visible controller stays paused rather than silently taking focus.
- Session recovery is honest about platform limits and never claims force-stop survival.
- Room version 8 adds `TrackPositionEntity`; playback preferences are additive.
