# ADR-043: Scheduled Automatic Backups

## Status

Accepted on 2026-10-20. Extends ADR-031 and the backup contract in ADR-039's change set.

## Context

Manual backups require the user to remember the action and choose a file each time. The user
authorized an opt-in scheduled backup that writes the existing archive into a user-owned folder,
with battery-aware scheduling and no work while disabled.

## Decision

- Use WorkManager periodic work with a single unique work name. Scheduling and cancellation
  happen from preference changes, never from app start.
- Require battery-not-low; optionally require charging. No exact alarms, wake locks or
  boot-receiver work.
- Persist a user-selected Storage Access Framework tree URI with read/write permission. The
  worker opens the tree with `DocumentFile`, creates a timestamped `.bmgbak` document and calls
  the existing backup creation path so the archive format, limits and ZIP hardening stay
  identical to manual backups.
- Frequencies are daily, weekly and every 30 days. A schedule-window key uses fixed-duration
  epoch buckets matching the WorkManager interval rather than local calendar dates, so a
  DST transition cannot suppress a due interval. The worker records success/failure and the last
  run for the settings summary. The 30-day option is not a calendar month, and the UI says so.
  A manual wall-clock change backwards can repeat a bucket and skip one run; a re-enable inside
  an already-successful bucket intentionally waits for the next bucket.
- If the folder or permission is unavailable, the run fails and reports it; it does not create a
  fallback backup elsewhere or retry unboundedly.
- WorkManager's periodic timing is inexact by platform design; the setting is a cadence, not an
  exact-time alarm.

## Consequences

- The app gains a WorkManager dependency and a persistent folder permission.
- Automatic backups are opt-in and stop cleanly when disabled.
- Users with revoked folder access get an explicit failure instead of a silent no-op.
- The destination volume's free space cannot be checked reliably through SAF; write failures are
  reported by the existing backup path.
