# Tasks: Backup ZIP sharing

> Status 2026-11-02: source, docs, change artifacts, JDK 21 build/lint and qa26/API26 runtime
> probes completed. Evidence: `IMPLEMENTATION.md`, `RUNTIME.md`, `DELTA.md` and the `build/`,
> `runtime/`, `logs/` folders under the evidence root.

## 1. Staging policy and helper

- [x] 1.1 Add pure `BackupSharePolicy` for display-name sanitization, 1..256 MiB bounds,
  canonical containment, temp-name detection and stale-file selection.
- [x] 1.2 Add `BackupShareStaging` with `cacheDir/backup_share`, SAF display-name resolution,
  free-space/size preflight, cancellable copy, hidden temp file, atomic replace (with fallback),
  single-slot stale cleanup, `isShareable` and `isStagedFile`.
- [x] 1.3 Serialize staging with a process-wide Mutex; keep archive creation and scheduled
  backups untouched.

## 2. UI and integration

- [x] 2.1 Add `BackupShareDialogFragment` with a Compose sheet: **Backup saved**, disclosure,
  **Share backup**, **Done**.
- [x] 2.2 Share via `ACTION_SEND` + `EXTRA_STREAM` + `ClipData` +
  `FLAG_GRANT_READ_URI_PERMISSION`, MIME `application/zip`, chooser titled **Share backup**.
- [x] 2.3 Wire the manual launcher to `BackupHelper.createBackupFile` + staging; success sheet,
  failure toast, staging failure falls back to the saved-but-not-shareable toast; guard
  fragment/state lifecycle.
- [x] 2.4 Add the narrow `<cache-path name="backup_share" path="backup_share/"/>` only; no manifest
  provider, permission or dependency change.

## 3. Strings and documentation

- [x] 3.1 Add main English strings only to `values/strings.xml`.
- [x] 3.2 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP` without reverting the settled
  HH:MM sleep-timer or playlist-import documentation.
- [x] 3.3 Add this OpenSpec change (proposal, design, spec, tasks, `.openspec.yaml`).

## 4. Verification

- [x] 4.1 Run ktlint check/format on the changed Kotlin files only; do not invoke Gradle.
- [x] 4.2 Run the focused pure-policy compiled probe (20 checks) and a static source probe
  (54 checks) for provider path, intent flags/MIME, sheet wiring, no share-existing row and
  untouched playlist restore.
- [x] 4.3 Combined build: `:app:assembleGithubDebug :app:lintGithubDebug` on JDK 21 with
  `--no-daemon`; BUILD SUCCESSFUL, lint exit 0. APK hashes/sizes/mtimes and source-vs-APK ordering
  are in `build/APKS.csv` and `IMPLEMENTATION.md`.
- [x] 4.4 Runtime on qa26/API26: manual backup -> Compose **Backup saved** sheet -> Share backup
  chooser delivered `ACTION_SEND`/`application/zip`/FileProvider URI to Gmail; Done; cancel
  chooser; missing staged file; scheduled backup produced its archive with no share sheet; APK
  permission/provider checks showed no new permission or provider. The shared ZIP was inspected
  (valid `prefs/` entry, matching MD5) rather than re-restored; rotation/leave-settings were not
  separately timed (the lifecycle guards are static-probed).
- [x] 4.5 Evidence and final diff recorded under
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/backup-zip-sharing-2026-11-02`
  (`IMPLEMENTATION.md`, `RUNTIME.md`, `DELTA.md`, `baseline/`, `delta/`, `after/`, `build/`,
  `runtime/`, `logs/`, `probes/`).
