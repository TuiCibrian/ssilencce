# Design: Automatic scheduled backups

## Context

`BackupHelper.createBackup(context, uri)` writes the same archive used by the manual flow and
reports failures through logs/toasts. Settings are XML `PreferenceScreen`s backed by the default
`SharedPreferences`. The app has no WorkManager dependency today.

## Goals / Non-Goals

**Goals:**
- Opt-in only; default disabled.
- User-selected SAF folder with persisted permission.
- Battery-aware periodic scheduling that does no work while disabled.
- One backup per schedule window, with a visible last-run status.

**Non-Goals:**
- Cloud upload, retention rotation beyond overwriting same-window files, encryption, or
  incremental/differential backups.
- Exact-time alarms or wake-lock based scheduling.
- Bypassing Android battery optimizations or force-stop.

## Decisions

- **WorkManager periodic work.** Add `androidx.work:work-runtime-ktx` at the current stable
  version. Periodic work is the platform-recommended battery-aware mechanism and survives
  reboots without `BOOT_COMPLETED` handling.
- **Unique work name.** Enqueue `PeriodicWorkRequest` under a single unique name with
  `ExistingPeriodicWorkPolicy.UPDATE`; cancelling the preference cancels the unique work.
- **Cadence.** Daily, weekly and every-30-days options; the last is labeled "Every 30 days"
  because periodic work cannot express calendar months. Idempotence keys follow the same elapsed
  intervals so a DST transition cannot suppress a due run.
- **Folder access.** Persist the tree URI string and call
  `ContentResolver.takePersistableUriPermission(..., READ|WRITE)` when chosen. The worker opens
  the tree with `DocumentFile`, creates a timestamped `ssilencce-YYYYMMDD-HHmm.bmgbak` document,
  and passes its URI to `BackupHelper.createBackup`.
- **Battery.** `Constraints` always require battery-not-low; the user can additionally require
  charging. No custom wake locks.
- **Idempotence.** The worker checks the last successful run timestamp for the current schedule
  window and skips if a backup for that window already exists. It records success/failure in
  preferences for the settings summary.
- **No duplicate scheduling.** Scheduling happens from preference changes only, not from app
  start or every screen visit.

## Risks / Trade-offs

- WorkManager periodic work is inexact; Android may delay runs. Documented, not a defect.
- The chosen folder can disappear or its permission can be revoked; the worker reports failure
  and does not loop or create fallback backups.
- Archive creation is IO-heavy but the existing helper is cancellation-aware and bounded.
