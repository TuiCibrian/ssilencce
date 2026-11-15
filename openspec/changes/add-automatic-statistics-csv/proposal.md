# Proposal: Automatic statistics CSV export

## Why

Manual Top Tracks/Top Artists CSV export already exists, but users must remember to run it.
`docs/MISSING_FEATURES.md` still records automatic weekly/monthly/yearly CSV export as the
remaining statistics gap. The existing manual export only has lower-bound period cutoffs, so it
cannot reliably export the calendar period that just ended.

## What Changes

- Add one optional automatic export with a single cadence: **Weekly**, **Monthly** or **Yearly**.
- Export only the latest completed local calendar period:
  - Weekly: previous Monday 00:00 through Sunday 23:59:59.999.
  - Monthly: previous calendar month.
  - Yearly: previous calendar year.
- Add explicit `[start, end)` play-event queries; do not extend `ListeningPeriod` or change the
  manual current-period picker.
- Write up to two files per completed period when data exists, using the existing
  `StatisticsCsvWriter` format and top-100 cap:
  - `ssilencce-auto-top-tracks-<start>_<end>.csv`
  - `ssilencce-auto-top-artists-<start>_<end>.csv`
- Stage each file as `<final>.partial`, then rename it to the final name only after the complete
  write; stale feature-owned staging files are cleaned on the next run.
- Use the existing SAF `OpenDocumentTree` flow with a persisted read/write grant. No storage
  permission, manifest provider, AlarmManager, notification or new dependency.
- Use one daily WorkManager check (`automatic_statistics_csv`) with battery/storage-not-low
  constraints and a persisted period key. Only the latest completed period is caught up after
  missed runs; a failed period is retried before advancing. No historical backfill.
- Record `Success`, `NoData` or `Failure` in a dedicated `statistics_csv` preferences file and
  show the result in a Compose Advanced-settings sheet.
- `NoData` is a successful no-file result. Handled failures return WorkManager success and are
  retried on the next daily check.
- Never delete older CSVs or implement retention.

## Impact

- New: pure period helper, store, scheduler, worker, controller, Compose preference and dialog.
- Modified: `StatisticsCsvExporter`, `PlayEventDao`, `Repository`, `SmartRepository`,
  `preferences_screen_advanced.xml`, main English strings and docs.
- Unchanged: manual export UI/behavior, `StatisticsCsvWriter`, `ListeningPeriod`, Room schema,
  permissions, manifest, Gradle dependencies/version and translated strings.
