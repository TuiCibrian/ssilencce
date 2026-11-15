# Proposal: Sleep timer exact end time

## Why

The sleep timer only accepts a minute countdown. Users who want playback to stop at a
specific wall-clock time must calculate the remaining minutes themselves, which is awkward
around midnight and when the current time is close to the target.

## What Changes

- Add `Duration` and `End at` modes to the existing Compose sleep-timer sheet.
- Let `End at` choose a local 24-hour `HH:mm` using a Compose Material 3 time input dialog.
- Default to the next full hour when no end time has been confirmed.
- Persist the last confirmed mode/hour/minute alongside the existing sleep-timer DataStore
  preferences.
- Schedule the next occurrence strictly after now; a time that is now or earlier today
  schedules tomorrow, and midnight rolls over correctly.
- Add a pure `SleepTimerEndTime` helper for delay calculation and `HH:mm` formatting.
- Preserve duration mode, cancel, fade-out, finish-last-song and the existing AlarmManager
  playback-stop path.

## Impact

- Affected code: `SleepTimerViewModel`, `SleepTimerScreen`, new `SleepTimerEndTime`, main
  English strings.
- Affected docs: `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`; OpenSpec change only, no ADR.
- No database schema, dependency, test source set, version or translated resource changes.
