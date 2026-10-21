# Tasks: Automatic scheduled backups

> Status 2026-10-21: opt-in/cancel/unique-work and worker archive behavior are PASS on the API 36 emulator. A real WorkManager cadence crossing a DST transition was not executed; the 540-check helper remains the DST evidence.

## 1. Dependencies and storage

- [x] 1.1 Add WorkManager to the version catalog and app dependencies without upgrading unrelated libraries.
- [x] 1.2 Add preferences for enabled, frequency, destination tree URI, charging-only and last-run status.
- [x] 1.3 Add a SAF folder picker path to Advanced settings that persists read/write tree permission and shows the chosen folder.

## 2. Scheduling and worker

- [x] 2.1 Add a scheduler that enqueues/cancels unique periodic work when preferences change and never schedules while disabled.
- [x] 2.2 Add a `CoroutineWorker` that resolves the tree, skips an already-completed window, creates a timestamped document and calls the existing backup creation path.
- [x] 2.3 Apply battery-not-low and optional charging constraints; report success/failure to preferences without retry storms.
- [x] 2.4 Keep manual backup behavior unchanged and share archive creation logic rather than duplicating it.

## 3. UI and documentation

- [x] 3.1 Show schedule summary, destination, last run result and validation messages in Advanced settings.
- [x] 3.2 Add English strings in `values/strings.xml` only.
- [x] 3.3 Update CODE_MAP, FEATURES, MISSING_FEATURES, AGENTS/domain notes and a scheduled-backup ADR.

## 4. Verification

- [x] 4.1 Build `:app:assembleGithubDebug` and `:app:lintGithubDebug` on JDK 21 after integration.
- [x] 4.2 Verify scheduling is opt-in, cancels cleanly, and a worker run produces a restorable archive in the chosen folder on the emulator.
- [x] 4.3 Verify no work is enqueued while disabled and no duplicate unique work remains after repeated preference changes.
