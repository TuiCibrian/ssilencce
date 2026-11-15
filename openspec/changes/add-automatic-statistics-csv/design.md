# Design: Automatic statistics CSV export

## Context

The manual export path snapshots the currently selected `ListeningPeriod`, uses
`StatisticsCsvWriter` and writes one SAF `CreateDocument` document. Current period queries only
take a lower-bound cutoff, which would produce an almost-empty current month/year at a boundary.
The scheduled-backup feature already demonstrates the safe patterns for SAF tree grants,
WorkManager periodic work, dedicated preference state and handled worker failures.

## Goals / Non-Goals

**Goals:**

- Export the last completed local calendar week/month/year, never the current partial period.
- Produce two auto-named top-100 CSV files using the existing writer and format, with staged
  `.partial` writes renamed only after a complete write.
- Reuse SAF read/write persistence and WorkManager without new permissions or dependencies.
- Be idempotent, catch up only the latest completed period, retry a failed period, and never
  backfill a history of missed periods.
- Keep manual export, `ListeningPeriod`, the writer and the restore/backup paths untouched.

**Non-Goals:**

- Independent weekly + monthly + yearly schedules; one selected cadence only.
- A “Run now” button, notifications, deletion/retention, or a statistics history screen.
- Changing manual export names/behavior or adding current-period auto-export.
- Room schema/migration, new storage permissions, manifest components or translated strings.

## Decisions

### Pure calendar window

`StatisticsCsvPeriod.kt` contains `StatisticsCsvCadence` (weekly/monthly/yearly),
`StatisticsCsvWindow` and `StatisticsCsvSchedule`. It has no Android imports and is compiled with
a fixed-clock JVM probe.

- Weekly current start is `previousOrSame(MONDAY)`; latest completed starts one week earlier and
  ends one day before the current Monday.
- Monthly uses the first day of the current month; latest completed is the prior month.
- Yearly uses January 1; latest completed is the prior year.
- `endDateInclusive` is used only in the file stem. Query end is
  `endDateInclusive.plusDays(1).atStartOfDay(zone)`, so the query is `[start, endExclusive)` and
  DST-safe.
- `key` is `weekly:<start>`, `monthly:<YYYY-MM>` or `yearly:<YYYY>`. `windowFromKey` rebuilds a
  pending window; malformed/mismatched keys fall back to the latest completed period.

### Explicit range queries

`PlayEventDao` gains `topSongsBetween(startMillis, endMillis, limit)` and
`topArtistsBetween(startMillis, endMillis, limit)`. Both use
`played_at >= :startMillis AND played_at < :endMillis`; the artist total-count subquery uses the
same range so percentages are period-correct. `Repository`/`SmartRepository` gain
`songPlayStats(startMillis, endMillis)` and `artistPlayStats(startMillis, endMillis)`.

The existing mapping/filtering (`toSongPlayStats`, recorded artist names, 100-row cap) is reused.
All-time aggregate behavior and `ListeningPeriod` queries are unchanged.

### Exporter

`StatisticsCsvExporter.prepareAutomatic(contentType, window)` uses the range queries, takes
`MAX_ROWS = 100`, returns null for empty stats and uses `StatisticsCsvWriter` unchanged. The
writer is reused for CRLF, UTF-8, RFC 4180 quoting and formula neutralization. Manual `prepare`,
`write`, file names and the detail-screen export remain unchanged.

Automatic file names are:

- `ssilencce-auto-top-tracks-${start}_${endInclusive}.csv`
- `ssilencce-auto-top-artists-${start}_${endInclusive}.csv`

### Store, cadence and scheduling

`StatisticsCsvStore` uses a dedicated `statistics_csv` SharedPreferences file:

- `enabled`, `cadence`, `destination`, `destination_name`.
- `last_attempt_at`, `last_result` (`None`/`Success`/`NoData`/`Failure`), `last_error`.
- Per-cadence last keys: `last_weekly_key`, `last_monthly_key`, `last_yearly_key`.
- One `pending_key` + `pending_cadence` for a failed period.

Changing cadence clears the pending key; changing folder leaves the pending key so the same
period is retried in the new folder. Disabling keeps configuration and last keys.

`StatisticsCsvScheduler` enqueues one unique daily periodic work named
`automatic_statistics_csv` with `ExistingPeriodicWorkPolicy.UPDATE`, battery-not-low and
storage-not-low constraints. Cadence is read per run; no charging constraint and no AlarmManager.

### Worker behavior

`StatisticsCsvWorker` chooses a pending window when its cadence matches, otherwise the latest
completed window. It then:

1. Checks enabled and exits immediately when the latest completed key equals the persisted
   per-cadence key and no period is pending. This key gate is what makes reruns and `NoData`
   idempotent even when a provider cannot enumerate files.
2. Otherwise checks destination, persisted read+write access and folder writability.
3. Deletes stale staging documents only when their name matches the automatic prefix and the
   `.partial` suffix. Final and unowned documents are never touched by this cleanup.
4. Resolves the two expected final file names and checks existing documents. A positive reported
   length skips the file. For a zero/unknown length, the worker opens the document and reads the
   first byte: content counts as an existing file, EOF is a confirmed partial and is deleted before
   recreation, and an unreadable document is kept and recorded as a handled failure.
5. If both files already exist non-empty, records success and advances the key without querying.
6. Prepares only missing content types. Empty types are skipped; if both are empty and no file
   exists, records `NoData`.
7. Creates each missing file under `<final>.partial`, writes through
   `StatisticsCsvExporter.write`, closes it, then calls `renameTo(final)`. Only a successful rename
   counts as written. A rename failure deletes staging and records a handled failure; a
   write/cancellation failure deletes staging before rethrowing/returning.
8. On `Success`/`NoData`, stores the completed key and clears pending. On handled failure, stores
   `Failure` + pending key and returns `Result.success()` so the next daily check retries.
9. Never deletes completed older files and never backfills more than the pending/latest target.

The `.partial` staging name is feature-owned (`ssilencce-auto-top-*.partial`). If the process dies
during a write, the next run's cleanup removes the stale staging document and rewrites it. A final
file is therefore only observable after a complete write and rename, and a non-empty final from a
previous successful run is preserved.

Because the stored key is authoritative, deleting a file for the already-stored period does not
trigger regeneration; only a newly completed period (or a pending failed period) runs again.

If a previous file remains from a partially successful run, it is kept and skipped on retry.
User deletion of old files is not a trigger for historical regeneration; only the currently
targeted period is checked/recreated.

### Settings UI

`preferences_screen_advanced.xml` gets a new Statistics category with
`StatisticsCsvPreference`. The preference opens `StatisticsCsvDialogFragment`, modeled on
`ScheduledBackupDialogFragment`, with:

- title/description and compatibility/data-history note;
- enable switch;
- Weekly/Monthly/Yearly radio rows;
- folder name, `OpenDocumentTree` chooser and folder-required validation;
- last attempt/result/error;
- Cancel/Save, immediate persistence on save.

Enabling requires a persisted read+write grant. Disabling cancels the unique work but keeps
configuration. No new navigation destination or fragment route is required; the custom
preference handles its own click, so `PreferencesScreenFragment` is not modified.

## Risks / Trade-offs

- WorkManager periodic delivery is inexact; the daily check is a cadence, not an exact boundary
  alarm. A late run still exports the correct completed period.
- Clock/time-zone changes can shift local period keys; file names and existing-file checks make
  retries idempotent.
- Play events begin when this version started recording them; earlier periods may be empty or
  partial. The sheet reuses the existing statistics-history caveat.
- SAF providers vary in `createFile`/rename/delete and reported-length behavior. The worker writes
  staging `.partial` documents, renames only after a full write, skips existing non-empty finals,
  verifies zero-length documents by reading a byte, keeps unverifiable documents and records
  handled failures. A provider that cannot rename makes the export fail rather than expose a
  partially written final.
- A partial pair is kept: the successful file is not deleted, and the missing file is retried.
- Top-100 truncation is retained for consistency with manual export.
- The daily WorkManager work is tiny; no charging constraint or notification is added.

## Verification

- Pure period probe with fixed clocks, leap years, month/year boundaries, DST and pending-key
  parsing.
- Static source probe for the unique daily work, constraints, range predicates, SAF read/write
  grant, both content types, file names, no empty CSV, partial cleanup, no manifest/dependency/
  translated-string change and manual export preservation.
- ktlint/`git diff --check` and `openspec validate --strict` in this pass. JDK 21 build/lint and
  device runtime are deferred until the shared device/build lease is free.
