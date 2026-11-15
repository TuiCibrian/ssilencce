# Tasks: First-run setup wizard and library rescan

## 1. Persistence and scanning

- [x] 1.1 Add `setup_wizard_completed` (default `true`, new installs `false`) and
  `auto_rescan_on_media_mount` (default `false`) preferences with typed accessors.
- [x] 1.2 Add `LibraryRescan`: resolve chosen folders (whitelist or storage roots plus the main
  folder), run `MediaScannerConnection.scanFile`, serialize concurrent scans and expose a debounced
  background variant.
- [x] 1.3 Add `MediaMountRescanReceiver` for `ACTION_MEDIA_MOUNTED` with permission and preference
  guards; register it dynamically in `App` (process-scoped, so a disabled feature never starts the
  app on a mount event).

## 2. Setup wizard

- [x] 2.1 Add `SetupWizardViewModel` that loads the existing whitelist/blacklist and start folder,
  edits include/exclude lists, persists them, and runs the rescan action.
- [x] 2.2 Add the Compose `SetupWizardScreen` (welcome, folders, scanning, finish steps) and a
  Compose folder browser dialog over the mounted storage volumes.
- [x] 2.3 Add `SetupWizardActivity`, route to it from `MainActivity` while incomplete, mark the
  wizard complete on finish/skip, and return to Settings when opened from there.

## 3. Settings entry points

- [x] 3.1 Add `Rescan library`, `Auto rescan when media is mounted` and `Setup wizard` preferences
  to Library settings, with a confirmation for the rescan action.
- [x] 3.2 Add the main English strings next to the existing welcome/library strings.

## 4. Verification and hand-off

- [x] 4.1 Run the compile gate until green (`:app:compileGithubDebugKotlin`: BUILD SUCCESSFUL),
  plus project ktlint 1.8.0 check and `git diff --check`.
- [x] 4.2 Self-review the diff and write `DOCS_DELTA.md` (features, missing features, code map,
  ADR, decisions) and `STATUS`.
