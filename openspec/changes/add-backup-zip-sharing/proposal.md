# Proposal: Share the manually created backup ZIP

## Why

A manual backup currently writes a `.bmgbak` archive and shows only a success toast. Users who
want to send the archive to another device or person must leave the app and find the file in the
system file manager. The audit at
`C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/program-2026-11-02/NEXT-backup-zip-sharing.md`
confirmed that the archive creation path is already reusable and that the existing FileProvider
has no cache path, so a small, isolated share flow is possible without touching restore, the
archive format or scheduled backups.

## What Changes

- After a **successful manual backup only**, show a Compose confirmation sheet with
  **Backup saved**, a short disclosure, **Share backup** and **Done**.
- Stage the just-written SAF document into a private `cacheDir/backup_share/` slot. The staged
  copy is the only file exposed for sharing.
- Add a narrow `<cache-path name="backup_share" path="backup_share/"/>` to the existing
  `provider_paths.xml`; keep the existing FileProvider, authority and manifest provider unchanged.
- Share with `ACTION_SEND`, `EXTRA_STREAM`, `ClipData` and read-only
  `FLAG_GRANT_READ_URI_PERMISSION`, MIME `application/zip`, through a chooser titled
  **Share backup**.
- Fail soft: a staging read/write/FileProvider failure shows a clear message and never makes the
  completed backup look failed or crashes. Missing, empty, oversized or out-of-directory staged
  files are rejected before sharing.
- Clean staging safely: temp files are removed, a successful new stage replaces the previous
  single slot and deletes other stale entries; the source SAF backup is never deleted.
- Do **not** add a permanent “Share existing backup” row, auto-share scheduled backups, change
  the restore flow, change the archive format, add permissions, add dependencies, add a manifest
  provider or change the application version.
- Keep `BackupHelper.restorePlaylists` and all playlist import/conflict source untouched.

## Impact

- Affected code: new `util/backup/BackupSharePolicy.kt`,
  `util/backup/BackupShareStaging.kt`, new
  `ui/screen/settings/backup/BackupShareDialogFragment.kt`, and the manual backup launcher in
  `ui/screen/settings/PreferencesScreenFragment.kt`.
- Affected resources: `res/xml/provider_paths.xml` (one narrow cache path) and
  `values/strings.xml` (main English only).
- Affected docs: `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`; this OpenSpec change.
- Unchanged: `BackupHelper.createBackupFile`, `BackupHelper.restorePlaylists`,
  `ScheduledBackupWorker`, archive entries/ZIP format, restore picker, manifest provider,
  permissions, Gradle dependencies.
