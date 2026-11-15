# Automatic statistics CSV

## Purpose

Add one optional automatic statistics export for the latest completed local calendar week,
month or year while preserving manual current-period export, the existing CSV format and the
app's no-new-permission/dependency boundary.

## ADDED Requirements

### Requirement: One optional cadence exports only the last completed period

The app SHALL offer one automatic cadence choice: Weekly, Monthly or Yearly. Each run SHALL
target the latest completed local calendar period: the previous Monday–Sunday for Weekly, the
previous calendar month for Monthly, or the previous calendar year for Yearly. The current
partial period SHALL NOT be exported.

#### Scenario: Weekly boundary
- **WHEN** the current local date is Monday
- **THEN** the target is the previous Monday 00:00 through the immediately preceding Sunday

#### Scenario: Monthly boundary
- **WHEN** the current local date is the first day of a month
- **THEN** the target is the entire previous calendar month

#### Scenario: Yearly boundary
- **WHEN** the current local date is January 1
- **THEN** the target is the entire previous calendar year

### Requirement: Range queries exclude the period end

Play-event statistics for the automatic export SHALL use a start-inclusive, end-exclusive
millisecond range. The artist percentage denominator SHALL use the same range. Manual
`ListeningPeriod` queries and all-time aggregate behavior SHALL remain unchanged.

#### Scenario: Event exactly at the end boundary
- **WHEN** a play event has `played_at` equal to the next period's start
- **THEN** it is excluded from the completed period

### Requirement: Two top-100 CSV files use the existing format

For a completed period with data, the app SHALL write up to two files:
`ssilencce-auto-top-tracks-<start>_<end>.csv` and
`ssilencce-auto-top-artists-<start>_<end>.csv`. Each SHALL contain at most 100 rows and SHALL use
the existing `StatisticsCsvWriter` headers, column order, CRLF, UTF-8 without BOM, RFC 4180
quoting and formula-injection neutralization. An empty content type SHALL NOT produce an empty
CSV.

#### Scenario: Both content types have data
- **WHEN** the period has track and artist plays
- **THEN** both files are written with the existing format and the inclusive end date in the name

#### Scenario: One content type is empty
- **WHEN** only one of tracks/artists has rows
- **THEN** only that file is written

#### Scenario: Both content types are empty
- **WHEN** the completed period has no plays
- **THEN** no file is written and the result is recorded as NoData

### Requirement: SAF folder access is persisted read/write

The user SHALL choose a folder through `OpenDocumentTree`. Enabling SHALL require a persisted
read and write grant. The app SHALL NOT add a storage permission, manifest provider, AlarmManager
dependency or notification channel.

#### Scenario: Enable without a usable folder
- **WHEN** the user enables the feature without a folder or persisted access
- **THEN** saving is rejected with an inline error and the worker is not enabled

#### Scenario: Permission lost later
- **WHEN** the persisted folder permission is no longer available
- **THEN** the worker records a failure and does not crash or create a fallback location

### Requirement: Daily WorkManager check with latest-only catch-up

The app SHALL enqueue one unique daily WorkManager check named `automatic_statistics_csv` with
battery-not-low and storage-not-low constraints and `ExistingPeriodicWorkPolicy.UPDATE`. The
worker SHALL target a pending failed period before the latest completed period. It SHALL NOT
backfill older missed periods.

#### Scenario: Missed several periods
- **WHEN** the daily check has not run for several weeks
- **THEN** only the latest completed period is exported on the next run

#### Scenario: Failed period retry
- **WHEN** a run records a handled failure
- **THEN** the next daily run retries the same pending period before advancing

#### Scenario: Cadence change
- **WHEN** the user changes the cadence
- **THEN** the pending key is cleared and the next run uses the latest completed period for the
  new cadence

### Requirement: Idempotent files and staged partial cleanup

The persisted per-cadence last key SHALL be the primary rerun gate: when the latest completed
period key equals the stored key and no period is pending, the worker SHALL exit without
touching the provider. Before writing a file, the worker SHALL skip an existing non-empty target.
A zero-byte length SHALL NOT by itself authorize deletion: the worker SHALL verify emptiness by
opening the document and reading the first byte, delete only a confirmed empty document, and keep
an unverifiable document while recording a handled failure.

Each missing automatic CSV SHALL be written to a feature-owned `<final>.partial` staging document,
fully written and closed, and only then renamed to its final name. Only a successfully renamed
file SHALL count as written. At the start of a run the worker SHALL delete only stale staging
documents whose name matches the automatic prefix and `.partial` suffix; final and unowned
documents SHALL never be deleted. A rename failure SHALL delete the staging document and record a
handled failure without advancing the key. Process death before rename SHALL leave only staging,
which the next run deletes and rewrites. A successful final file from a partial pair SHALL be kept
for the retry. Older completed files SHALL never be deleted.

#### Scenario: Rerun same completed period
- **WHEN** the stored key equals the latest completed period and no failure is pending
- **THEN** the worker exits without listing or writing provider files

#### Scenario: Process death before rename
- **WHEN** a non-empty `.partial` staging document exists and its final file does not
- **THEN** the next run deletes only that staging document, rewrites it and renames it; it never
  treats the staging document as a completed CSV or records success from it

#### Scenario: Rename failure
- **WHEN** the staging document is fully written but `renameTo(final)` fails
- **THEN** the staging document is deleted, the run records a handled failure and no last key is
  advanced

#### Scenario: Final non-empty preserved
- **WHEN** a final automatic CSV already exists non-empty
- **THEN** the worker keeps it, does not overwrite it and counts it as an existing file

#### Scenario: Provider reports unknown length
- **WHEN** an existing target document reports zero length but cannot be opened for verification
- **THEN** the document is kept and the run records a handled failure instead of overwriting it

#### Scenario: One file from a partial run
- **WHEN** the track file exists non-empty and the artist write failed previously
- **THEN** the next retry keeps the track file and attempts only the missing artist file

#### Scenario: User deletes a completed file
- **WHEN** the user deletes an exported CSV for the stored completed period
- **THEN** the worker does not recreate it; only a newly completed period triggers another export

#### Scenario: User deletes an old file
- **WHEN** the user deletes an older period's CSV
- **THEN** the worker does not backfill that older period; only the current target is considered

### Requirement: Handled results are recorded and retried next day

The app SHALL record `Success`, `NoData` or `Failure` with the last attempt time. `NoData` SHALL
advance the period key and write no file. Handled failures SHALL record a pending key and return
WorkManager success so the next daily check retries. Cancellation SHALL delete the partial
document and rethrow.

#### Scenario: No data
- **WHEN** both content types are empty for the completed period
- **THEN** the key advances, no CSV is created and the result is NoData

#### Scenario: Export failure
- **WHEN** a folder, permission, create-file or write operation fails
- **THEN** the failure is recorded, the target remains pending and no success is claimed

### Requirement: Settings UI is a Compose Advanced-settings sheet

The Advanced settings screen SHALL contain a Statistics category with one automatic CSV row.
The row SHALL open a Compose sheet with enable, Weekly/Monthly/Yearly cadence, folder selection,
last result/error, Cancel and Save. The row summary SHALL reflect disabled/enabled state and the
last result. Only main English strings SHALL be added.

#### Scenario: Save successful configuration
- **WHEN** the user enables a cadence with a valid folder and saves
- **THEN** the configuration persists, daily work is scheduled and the sheet closes

#### Scenario: Disable
- **WHEN** the user disables and saves
- **THEN** the unique daily work is cancelled and the folder/cadence/last keys remain stored
