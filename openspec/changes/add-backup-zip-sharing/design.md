# Design: Share the manually created backup ZIP

## Context

`PreferencesScreenFragment` launches `ActivityResultContracts.CreateDocument("application/*")`
and previously called `BackupHelper.createBackup`, which wrote the archive and showed a toast.
`BackupHelper.createBackupFile` already does the archive work without UI feedback and is also
used by `ScheduledBackupWorker`; it returns `BackupCreationResult.Success/Failure` and cleans a
failed SAF target. The archive contains playlist M3Us, the settings XML, the lyrics JSON and
custom artist images.

The manifest already declares `androidx.core.content.FileProvider` with authority
`${applicationId}.provider`, `exported=false` and `grantUriPermissions=true`. Its
`provider_paths.xml` exposes files, external files, external storage and `/storage/`, but has no
`<cache-path>`, so a cache file cannot currently receive a FileProvider URI.

The manual backup UI is a View-based `PreferenceFragmentCompat` in Advanced settings. The
scheduled-backup configuration already demonstrates the project's Compose-in-
`BottomSheetDialogFragment` pattern.

## Goals / Non-Goals

**Goals:**

- Offer share immediately after a successful manual backup, without changing the archive write.
- Keep all share state in a private cache slot and expose only a read-only per-URI grant.
- Degrade cleanly when staging, URI creation, file checks or chooser launch fail.
- Keep scheduled backups, restore, the archive format and the manifest provider unchanged.
- Keep the change isolated from the in-flight playlist import/conflict and sleep-timer work.

**Non-Goals:**

- A permanent “Share existing backup” settings row or a second SAF picker.
- Auto-sharing or notifying scheduled backups.
- Changing `BackupHelper.createBackupFile`, `BackupHelper.restorePlaylists` or the ZIP format.
- Adding permissions, dependencies, test source sets, a new provider, a schema change or a
  version change.
- Keeping multiple historical share copies or implementing a full content provider with
  delete-on-close semantics.

## Decisions

### Reuse the existing writer, stage after success

The manual callback calls `BackupHelper.createBackupFile(appContext, uri)` and handles the
result itself. It does not call the legacy toast-only `BackupHelper.createBackup`, which remains
in place but has no current caller to avoid touching the backup file during the playlist work.
Only after `BackupCreationResult.Success` does it call `BackupShareStaging.stageFromSaf`.

This ordering guarantees that a staging problem cannot corrupt or invalidate the SAF backup. It
also avoids generating the archive twice and leaves scheduled backups on their existing path.

### Private single-slot cache staging

`BackupShareStaging` uses `context.cacheDir/backup_share/`. The slot is intentionally transient:
cache is excluded from Android Auto Backup, is system-evictable and is not user-visible.

`stageFromSaf`:

1. Ensures the directory exists and removes only old `.backup-share-*.tmp` files.
2. Resolves the SAF display name through `DocumentFile.fromSingleUri`, falling back to the
   existing `getFormattedFileName("Backup", "bmgbak")`.
3. Sanitizes the name: strips path separators/NUL, trims, removes leading dots, caps the length
   and appends `.bmgbak` when absent.
4. Rejects a known source length that is zero/oversized or larger than cache free space.
5. Copies the SAF stream to a hidden temp file with cancellation checks.
6. Rejects an empty or oversized temp file (1 byte..256 MiB).
7. Creates the FileProvider URI for the target, then atomically moves the temp file into the
   single slot (with a non-atomic replace fallback for providers/filesystems that reject
   `ATOMIC_MOVE`).
8. Deletes every other entry in the dedicated directory, leaving exactly the new slot.
9. Returns `StagedBackup(uri, file, displayName)`.

A process-wide `Mutex` serializes staging so two manual share preparations cannot delete each
other's temp files. It does not serialize archive creation; the existing manual/scheduled
`filesDir/Playlists` race remains a separate pre-existing issue.

### FileProvider path and grant

Add exactly:

```xml
<cache-path name="backup_share" path="backup_share/"/>
```

The URI shape is
`content://<applicationId>.provider/backup_share/<sanitized-name>.bmgbak`. The path is narrow;
do not add `<cache-path path="."/>`. The manifest provider remains unchanged. The share intent
grants only `FLAG_GRANT_READ_URI_PERMISSION`; no write or persistable grant is added.

### Share intent

Use `ACTION_SEND` with:

- `type = "application/zip"`;
- `EXTRA_STREAM = contentUri`;
- `ClipData.newRawUri(file.name, contentUri)`;
- `FLAG_GRANT_READ_URI_PERMISSION`;
- `Intent.createChooser(sendIntent, getString(R.string.backup_share_action))`.

The chooser title and the sheet button both use **Share backup**. There is no “shared
successfully” signal: `createChooser` has no result callback, so cancelling the chooser is
silent and the sheet is dismissed when the chooser is launched.

### Pre-share validation and failure handling

Before launching the chooser, `BackupShareStaging.isShareable` requires:

- `file.isFile`;
- `file.length()` in 1..256 MiB;
- the canonical file path strictly inside `cacheDir/backup_share`.

A missing, empty, oversized or out-of-directory file shows `backup_share_failed` and dismisses
the sheet. `ActivityNotFoundException` shows `backup_share_unavailable` and leaves the sheet open
for retry. Other launch errors are logged and show `backup_share_failed`.

If staging fails after the SAF write, the manual flow shows the combined
**Backup saved, but it could not be prepared for sharing.** toast and does not show the sheet.
The backup is never reported as failed because sharing was unavailable, and the message makes the
completed backup explicit. If the SAF write fails, the existing **Backup failed.** toast is shown
and no staging is attempted.

### Compose confirmation sheet

A new `BackupShareDialogFragment` follows `ScheduledBackupDialogFragment`:

- `BottomSheetDialogFragment`, expanded state, ComposeView with
  `DisposeOnViewTreeLifecycleDestroyed`, `SsilencceTheme`;
- arguments carry the URI string, staged file path and display name;
- title **Backup saved**, body **Your backup is ready to share.**, the SAF display name,
  a one-line disclosure about settings/account sign-ins, **Done** and **Share backup**;
- hosted from `PreferencesScreenFragment` with `childFragmentManager` only when the fragment is
  added, its view exists and the FragmentManager is not state-saved; otherwise the success toast
  is shown.

No new navigation destination or settings row is added.

### Cleanup and retention

- Failed/cancelled staging deletes the temp file and leaves any previous slot intact.
- A successful stage replaces the previous slot and removes other entries in the directory.
- Nothing deletes the SAF source backup.
- The cache slot remains until the next successful manual backup or OS cache eviction. A share
  target that is still reading an old slot can be invalidated by a later manual backup; this is
  accepted for the bounded single-slot design and documented as a risk.
- No app-start pruning is added; `App.onCreate` has debug StrictMode disk-write detection and
  pruning there would add main-thread I/O or process-start work for little benefit.

### Probe

`BackupSharePolicy` is a pure JVM object (no Android imports) covering name sanitization, size
bounds, canonical containment, temporary-name detection and stale-file selection. A focused
compiled probe can exercise it without Gradle or an emulator, alongside a static source probe
for the provider path, intent flags/MIME, and the absence of a share-existing row.

## Verification

Completed on 2026-11-02; evidence is under
`C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/backup-zip-sharing-2026-11-02`
(`IMPLEMENTATION.md`, `RUNTIME.md`, `DELTA.md`):

- ktlint format/check passed on the changed Kotlin files.
- Pure `BackupSharePolicy` compiled probe passed 20 checks; static source probe passed 54 checks.
- JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug --no-daemon` completed successfully
  (3m 33s, lint exit 0). APK hashes/sizes/mtimes and source-vs-APK ordering are recorded.
- qa26/API26 runtime verified: manual backup sheet, `ACTION_SEND`/`application/zip`/FileProvider
  URI and read grant, Done, chooser cancel, missing staged file, and a scheduled backup that
  produced its archive with no share sheet. APK permission/provider checks showed no new
  permission or provider.
- Not separately timed: restore of the shared archive and rotation/leave-settings during the
  write. The archive format was structurally inspected and is unchanged; lifecycle guards were
  covered by the static probe.

## Risks / Trade-offs

- **SAF read-back may fail** (cloud provider, revoked grant): the backup still succeeds; the
  user gets the existing success toast and no sheet. This is deliberate.
- **Cache eviction or a second backup can invalidate a staged file**: the sheet checks existence
  and size before sharing and reports `backup_share_failed`; it never crashes.
- **The archive contains sensitive settings**, including scrobbling login tokens. Sharing is
  explicit and the sheet discloses settings/account sign-ins. The app does not auto-share.
- **Single-slot replacement can break a long-lived share target**: accepted; the next successful
  manual backup replaces the slot. A future timestamped/multi-slot retention policy is out of
  scope.
- **Manual and scheduled archive creation share `filesDir/Playlists`**: pre-existing race, not
  changed here. The staging Mutex intentionally covers only cache staging.
- **The 256 MiB staging cap exceeds the existing 100 MiB uncompressed restore cap**: a very large
  archive can be shared even though this app may refuse to restore it. Restore limits are
  unchanged by this feature; the share flow never truncates the archive.
- **Working-tree conflicts**: the implementation does not edit `BackupHelper.restorePlaylists`
  or playlist import/conflict files and keeps new strings in the backup block. `BackupHelper`
  itself is not edited; its legacy `createBackup` wrapper remains unused.
