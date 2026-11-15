# Backup ZIP sharing

## Purpose

Offer a share action immediately after a successful manual backup while keeping the archive,
restore, scheduled backups and the existing FileProvider declaration unchanged. The share copy
is a private cache staging file exposed through a narrow read-only FileProvider grant.

## ADDED Requirements

### Requirement: Manual backup success offers sharing

After a manual backup writes successfully, the app SHALL show a Compose confirmation sheet with
the title **Backup saved**, a short disclosure, a **Share backup** action and a **Done** action.
The sheet SHALL be shown only for successful manual backups. Scheduled backups and failed manual
backups SHALL NOT show it.

#### Scenario: Successful manual backup

- **WHEN** the user completes the manual backup file picker and `createBackupFile` returns success
- **THEN** the sheet is shown with the display name of the written backup

#### Scenario: Failed manual backup

- **WHEN** the manual archive write fails
- **THEN** the existing backup-failed message is shown and no share sheet is presented

#### Scenario: Scheduled backup

- **WHEN** a scheduled backup completes
- **THEN** no share sheet or staging copy is created

#### Scenario: Picker cancelled

- **WHEN** the user cancels the create-document picker
- **THEN** no backup, staging copy, sheet or message is produced

### Requirement: Done and cancel leave the backup intact

Choosing **Done**, pressing back or dismissing the sheet SHALL NOT delete or modify the SAF
backup and SHALL NOT show an error. The staged cache copy MAY remain until the next successful
manual backup or OS cache eviction.

#### Scenario: Done dismissed

- **WHEN** the user taps Done or dismisses the sheet
- **THEN** the SAF backup remains unchanged and no share intent is launched

### Requirement: Sharing uses the existing FileProvider with a read-only grant

The app SHALL stage the just-written SAF document into a private cache slot and expose it through
the existing `androidx.core.content.FileProvider`. A narrow cache path SHALL be added to
`provider_paths.xml`. The share intent SHALL use `ACTION_SEND`, `EXTRA_STREAM`, `ClipData`,
`FLAG_GRANT_READ_URI_PERMISSION` and MIME type `application/zip`, wrapped in a chooser titled
**Share backup**. No write or persistable URI grant SHALL be added.

#### Scenario: Share action

- **WHEN** the user taps Share backup on the sheet
- **THEN** a chooser opens and the selected target receives a read-only `content://` URI for the
  staged `.bmgbak` file with MIME `application/zip`

#### Scenario: Chooser cancelled

- **WHEN** the user cancels the chooser
- **THEN** no error is shown, the backup remains intact and no “shared successfully” message is
  claimed

### Requirement: Staging failures never fail the backup

A failure to read the SAF document, create the cache copy, resolve the FileProvider URI or launch
the chooser SHALL NOT report the completed backup as failed. The app SHALL show a clear error for
the share operation and SHALL NOT crash. A staging failure after a successful write SHALL show a
clear message that the backup was saved but could not be prepared for sharing, and SHALL NOT show
the sheet.

#### Scenario: SAF read-back failure

- **WHEN** the SAF document cannot be read after the archive write succeeds
- **THEN** the backup remains saved, a clear “Backup saved, but it could not be prepared for
  sharing” message is shown and no sheet is presented

#### Scenario: FileProvider path failure

- **WHEN** `FileProvider.getUriForFile` cannot create a URI
- **THEN** the failure is logged, no share sheet is presented and the backup remains saved

#### Scenario: No share target

- **WHEN** the chooser cannot start because no activity is available
- **THEN** the share-unavailable message is shown and the sheet remains available to retry

### Requirement: Staged files are validated before sharing

Before launching the chooser the app SHALL require the staged file to exist, to have a size
greater than zero and at most 256 MiB, and to be canonically contained inside the dedicated
cache directory. A file that fails any check SHALL produce the share-failed message and SHALL NOT
be shared.

#### Scenario: Missing staged file

- **WHEN** the cache file is removed before Share is tapped
- **THEN** the share-failed message is shown, the sheet dismisses and no intent is launched

#### Scenario: Empty or oversized staged file

- **WHEN** the staged file is zero bytes or larger than 256 MiB
- **THEN** it is rejected without launching a chooser and the backup remains intact

#### Scenario: Path escape

- **WHEN** a path outside `cacheDir/backup_share` is supplied
- **THEN** containment validation rejects it and no URI is shared

### Requirement: Staging cleanup is safe and single-slot

The app SHALL create staging files in `cacheDir/backup_share`, remove abandoned temporary files,
publish a complete copy with a replace move and delete stale entries only after a successful
publish. It SHALL never delete the SAF source backup. It SHALL keep at most the current staged
backup in that directory.

#### Scenario: Successful replacement

- **WHEN** a new manual backup is staged while an older staged copy exists
- **THEN** the new copy replaces the old one and no stale staging entry remains

#### Scenario: Failed replacement

- **WHEN** copying or publishing the new staging copy fails
- **THEN** the temporary file is deleted, the previous staged copy is kept and the SAF backup is
  unchanged

#### Scenario: Orphaned temp file

- **WHEN** a previous process left a `.backup-share-*.tmp` file in the staging directory
- **THEN** the next staging preparation removes it before copying

### Requirement: No permanent share-existing row, no scheduled auto-share and no new permissions

The app SHALL NOT add a permanent “Share existing backup” settings row in this change. It SHALL
NOT automatically share scheduled backups. It SHALL NOT add permissions, dependencies, a new
manifest provider, a restore-flow change or an archive-format change.

#### Scenario: Advanced settings

- **WHEN** the Advanced settings screen is inspected
- **THEN** it still contains only the existing backup, restore and scheduled-backup rows

#### Scenario: Manifest and dependencies

- **WHEN** the merged manifest and Gradle files are inspected
- **THEN** no new permission, provider or dependency is present
