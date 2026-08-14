# ADR-032: Crash Handling and Permissions Onboarding

## Status

Accepted

## Context

A local-music player runs on diverse OEM firmware where MediaStore, `audiofx` and media
decoders fail in unpredictable ways. Crashes should never be silent: users need a readable
report path (and a GitHub issue entry point) instead of a dead app. Additionally, first-run
requires several permissions whose availability varies by Android version
(storage/audio, notifications, Bluetooth, exact alarms), which deserves an explicit
onboarding screen rather than a pile of system dialogs.

## Decision

- Handle uncaught crashes with `CustomActivityOnCrash` (cat.ereza):
  - configure the error activity (`ErrorActivity`) in `App` (`CaocConfig`),
  - `ErrorActivity` saves the report to `Crash_<timestamp>.log` in `filesDir`, shows it to
    the user, and offers copy / share / GitHub issue (via `Constants.ISSUE_TRACKER_LINK`,
    clipboard pre-filled) / restart,
  - enable `StrictMode` in debug builds for early violation detection.
- Provide `PermissionsActivity` as the first-run onboarding screen (launched from the app
  shell before `MainActivity` completes):
  - version-gated `PermissionView` cards covering READ_MEDIA_AUDIO (33+) /
    READ_EXTERNAL_STORAGE (≤32), READ_MEDIA_IMAGES, POST_NOTIFICATIONS,
    BLUETOOTH_CONNECT, and SCHEDULE_EXACT_ALARM (settings redirect),
  - rechecks grant state on resume; back press finishes the task.
- Centralize runtime permission requests in `AbsBaseActivity`
  (`PERMISSION_REQUEST = 100`, `BLUETOOTH_PERMISSION_REQUEST = 101`) with Snackbar
  rationale or settings redirect, consumed by all activities.

## Consequences

- Every crash becomes an actionable, shareable report; the GitHub issue path lowers the
  barrier for device-specific bug reports.
- Users understand permission purposes upfront (each card explains why), reducing support
  tickets; the screen stays version-adaptive as permission models change.
- The crash handler writes a log file on first crash — negligible storage cost, high
  debugging value.
- Permission flows are spread across onboarding + runtime requests; both go through
  `AbsBaseActivity` to keep the version branches in one place.
