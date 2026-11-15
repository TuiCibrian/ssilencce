# Design: Sleep timer exact end time

## Context

`SleepTimer` accepts an elapsed-time delay and uses `AlarmManager` with
`ELAPSED_REALTIME_WAKEUP`; `SleepTimerViewModel` reads/writes the existing
`preferencesDataStore("sleep_timer")` and combines it with the timer state. The Compose sheet
currently exposes a 5-180 minute slider and presets.

## Goals / Non-Goals

**Goals:**
- Add a wall-clock `End at` mode without changing the playback-stop implementation.
- Make the selected local time deterministic even around minute boundaries and midnight.
- Persist only the last confirmed mode/hour/minute.
- Keep the UI Compose-only and English strings in `values/strings.xml`.

**Non-Goals:**
- Stop-after-N-songs, timer history, foreground-only stopping, or a new timer engine.
- Changing fade-out, finish-last-song, exact-alarm permission handling or playback semantics.
- Mutating the device clock/timezone for DST handling.

## Decisions

- **Pure `SleepTimerEndTime` helper.** `delayMillis(now: ZonedDateTime, hour, minute)`
  builds the selected local time in `now.zone` with `ZonedDateTime.of`. If that instant is
  not strictly after `now`, it retries on the next local date. `Duration.between` returns the
  AlarmManager delay. `format` produces 24-hour `HH:mm`.
- **DST rules.** `ZonedDateTime.of` applies the zone's rules rather than changing clocks:
  nonexistent times in a spring-forward gap resolve forward to the first valid instant;
  ambiguous times in a fall-back overlap use the earlier offset. If that resolved instant is
  not after `now`, the next date is used. These rules are documented in the helper and
  covered by the focused probe using fixed zones.
- **Compose time picker.** The sheet uses a Compose `Dialog` with Material 3 `TimeInput` and
  a 24-hour `rememberTimePickerState`; no platform `TimePickerDialog` or XML UI.
- **Default.** If no `end_at_hour`/`end_at_minute` is stored, `nextFullHour(now)` supplies
  the initial values. It derives the next local wall-clock hour from `LocalDate`/`LocalTime`
  and resolves it in the zone, enforcing a strictly-future instant; this covers the DST
  fall-back overlap and the 23:xx → 00:00 rollover.
- **Persistence.** The existing DataStore gains `timer_mode`, `end_at_hour` and
  `end_at_minute`. They are written only after a successful exact-alarm schedule; changing
  a picker selection and dismissing it does not confirm it.
- **Existing timer path.** `SleepTimer.set(millisInFuture, allowPendingQuit, fadeOut,
  fadeDuration)` remains untouched. End-at scheduling computes a millisecond delay and uses
  that same path. Finish-last-song therefore retains its existing defer-to-track-end
  semantics; exact pause is tested with it off.
- **Running UI.** Controls are disabled while running; the countdown remains visible and an
  `Ends at HH:mm` label is shown for end-at mode.

## Risks / Trade-offs

- `AlarmManager` exact alarms may be denied on Android 12+; the existing permission flow is
  preserved and no timer is confirmed/persisted before scheduling succeeds.
- A DST overlap selects the earlier offset by `java.time` rule; a later-offset repeat of the
  same local time is not selected. This is deterministic and documented rather than
  clock-mutating behavior.
- The end-at label displays local wall-clock time only; a target deferred to tomorrow shows
  the selected time without a date.
