# Proposal: Automatic scheduled backups

## Why

Manual backup/restore exists, but users must remember to create a backup file and choose a
destination every time. Audiobook and large-library users need a durable, opt-in backup that
lands in a folder they own, with no repeated work while disabled.

## What Changes

- Add an opt-in scheduled backup preference in Advanced settings with daily, weekly and every-30-days cadences.
- Let the user choose a destination folder through the Storage Access Framework and persist
  read/write access to that tree.
- Schedule daily, weekly or monthly backups with WorkManager; require battery-not-low and
  optionally charging, and never enqueue work while the feature is disabled.
- Write timestamped backup archives through the existing `BackupHelper.createBackup` path so
  the ZIP format, limits and failure reporting stay identical to manual backups.
- Show the last run result and allow the user to disable or reschedule without duplicating work.

## Impact

- Affected code: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AdvancedPreferencesFragment`,
  `preferences_screen_advanced.xml`, new `util/backup` worker/scheduler, `BackupHelper`,
  `AndroidManifest` only if a component must be declared (WorkManager needs no manifest entry).
- Affected docs: CODE_MAP, FEATURES, MISSING_FEATURES, ADR for scheduled backup policy.
- New strings in `values/strings.xml` only.
