# ADR-019: Sleep Timer with AlarmManager

## Status

Accepted

## Context

A sleep timer must stop (or fade out) playback at an absolute deadline even if the process is
killed, update the UI countdown while running, and support optional fade-out and
"pending quit" (stop after current song). Timer deadlines are wall-clock-relative, so a
process-local countdown alone is not sufficient.

## Decision

- Implement `SleepTimer` on top of `AlarmManager`:
  - an exact alarm with `ELAPSED_REALTIME_WAKEUP`,
  - on Android 12+, the `SCHEDULE_EXACT_ALARM` permission is requested and the timer falls back
    gracefully when not granted,
  - a `CountDownTimer` drives the UI countdown via StateFlows,
  - optional fade-out is executed by `PlaybackService` at alarm time,
  - optional "pending quit" consumes the current-song-end before stopping
    (`SleepTimerWaitingFor` states).
- Persist timer options (duration presets, fade-out duration) in the `sleep_timer` DataStore
  (ADR-009) through `SleepTimerViewModel`, which also surfaces `SleepTimerEvent` one-shot
  events to the Compose bottom sheet (`SleepTimerScreen`).
- Register the service-side listeners through `PlaybackService` and expose start/cancel via
  the session command surface so the timer can be toggled from the player UI.

## Consequences

- The timer survives process death; playback stops at the deadline deterministically.
- Exact-alarm policy on Android 12+ requires user consent; the UI must explain and redirect to
  settings when permission is missing.
- Fade-out is best-effort (depends on the process being alive at alarm time) — an accepted
  limitation of the platform model.
- Sleep-timer state is observable reactive state (StateFlows), so the Compose UI stays in sync
  with the service.
