# Design: Playback resilience and focus controls

## Context

`PlaybackService` owns ExoPlayer, a `MediaLibrarySession`, `PersistentStorage` (queue + index +
position + shuffle) and a 500 ms `ProgressObserver`. ExoPlayer currently receives
`setAudioAttributes(..., handleAudioFocus)`, where `handleAudioFocus` is the inverse of the
existing Ignore audio focus switch. `PlayerViewModel` owns UI facing flows and opens queues.
Room is at v7.

## Goals / Non-Goals

**Goals:**
- Long-form tracks resume where the user left them, without affecting normal short tracks when
  the feature is off.
- Audio focus behavior is explainable and configurable; calls never leave playback in a
  contradictory state.
- The Quick Settings tile mirrors the real session and does not hold a controller while not
  visible.
- A service restart can restore an active playback session when the user opted in.

**Non-Goals:**
- Bypassing Android force-stop, killing the process, or continuously restarting the app.
- Gapless/crossfade/DSP changes.
- Exact focus arbitration with every third-party app; the platform focus API remains the source
  of truth.

## Decisions

- **Per-track position lives in Room.** New `TrackPositionEntity(mediaId, positionMs,
  updatedAt)` with a DAO. The service updates an in-memory position while playing and writes at
  most every 15 seconds for tracks longer than 10 minutes, plus on pause, transition and service
  destroy. A stored position below 5 seconds or within 5 seconds of the end is discarded. On
  completion (STATE_ENDED or an automatic transition at the end) the row is deleted.
- **Restore interaction.** The first item after a full-session restore uses the queue-level
  saved position, so per-track resume does not override the restored queue position. Subsequent
  items consult the per-track store.
- **Audio focus has one owner.** Disable ExoPlayer's internal focus handling and add a small
  `AudioFocusController` around `AudioManager.requestAudioFocus`. It handles gain, transient
  loss, duck and loss, remembers whether playback was active, and applies the mode:
  Normal (pause on transient loss, resume on gain), Ignore (no requests), Ask (UI confirmation
  before taking focus when another app is audibly playing), Resume after call (pause on
  transient loss, resume only when the user enabled it). Duck lowers player volume and restores
  it.
- **Ask mode is UI-mediated.** The service emits a session event/state when focus would be
  taken from another audible app; `MainActivity` shows the confirmation. If no UI is available,
  playback stays paused rather than silently stealing focus.
- **Tile is lifecycle-bounded.** `PlaybackTileService` connects a `MediaController` in
  `onStartListening`, updates the tile from player events, and releases it in
  `onStopListening`/`onDestroy`. It never polls.
- **Session recovery is opt-in and one-shot.** Persist `playWhenReady` and an `active` marker
  with the existing playback state. On a service recreation caused by the system (not a
  force-stop), restore the queue and, if recovery is enabled and the saved session was active,
  start playback once. A process-level guard prevents loops.

## Risks / Trade-offs

- Frequent position writes cost IO; the 15-second cadence and long-track threshold bound it.
- Custom focus handling replaces a well-tested ExoPlayer path; it must be reviewed against
  transient loss, ducking and abandonment on service destroy.
- Ask mode cannot prompt when no UI is present; the documented behavior is to stay paused.
- Session recovery cannot survive force-stop; this is an Android platform boundary.

## Migration Plan

- Room v7 to v8 adds `TrackPositionEntity` (additive) and the named-queue tables from
  `add-named-queues`; the migration is additive and preserves all existing rows.
- Preferences are additive with safe defaults (feature off, normal focus).
