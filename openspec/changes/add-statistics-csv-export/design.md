# Design: Manual statistics CSV export

## Context

`DetailListFragment` already loads Top Tracks and Top Artists for five `ListeningPeriod`
values and keeps the selected period in `selectedPeriod`/`selectedPeriodFlow`. The screen's
Top Tracks flow uses aggregate `PlayCountEntity` counts for all time and `PlayEventEntity`
counts for bounded periods; Top Artists uses `ArtistPlayStat`, including recorded names and
the period denominator. Both queries cap the outer list at 100.

## Goals / Non-Goals

**Goals:**
- Export the current period's Top Tracks or Top Artists data manually.
- Keep the CSV independent of Android for escaping and formatting.
- Use only the SAF `CreateDocument` contract; request no storage permission.
- Make empty, canceled, failed and interrupted exports predictable.

**Non-Goals:**
- Automatic or scheduled CSV exports.
- New statistics screens, charts, filters, or a history of exports.
- Changing existing period/artist statistics semantics or database schema.

## Decisions

- **SAF `CreateDocument("text/csv")`.** The action opens a user-chosen document and writes
  through its content resolver URI. Cancel is a no-op. Suggested names include the period
  slug (`ListeningPeriod.name.lowercase()`).
- **Repository-backed snapshot.** On menu selection, the exporter queries the repository off
  Main using the currently selected period, takes the top 100, and prepares the complete CSV
  before opening the picker. This provides the required empty-statistics toast without
  launching a picker and avoids holding a live Room flow across SAF.
- **New `SongPlayStat` + `Repository.songPlayStats`.** Top Tracks needs both the song and its
  displayed play count. The existing screen flow drops counts, so the new query preserves
  ordering, the 100-row cap, read-only missing-file filtering (without deleting play-count
  rows), aggregate all-time counts and period event counts. Top Artists continues to use the
  existing `ArtistPlayStat` snapshot.
- **Pure writer.** `StatisticsCsvWriter` owns headers, CRLF, RFC 4180 quoting and formula
  neutralization. Numeric columns stay plain; text columns quote when needed and prefix a
  single apostrophe when their first non-space character is `=`, `+`, `-` or `@`.
- **Exporter owns IO and cleanup.** `StatisticsCsvExporter` uses the Koin `Repository` and
  writes through a supplied `ContentResolver`. Query and write run on `Dispatchers.IO`.
  Cancellation is rethrown; failures are surfaced to the fragment. Both paths attempt a
  best-effort deletion of the selected document.
- **Fragment lifecycle.** The fragment prepares the document, launches the picker, and writes
  the pending document from `lifecycleScope`. Pending data is stored in saved instance state
  so a picker result can still be completed after recreation.
- **No new screen.** The existing options menu, SAF picker and toasts are sufficient.

## Risks / Trade-offs

- The SAF provider controls the final document; best-effort deletion may fail after a partial
  write. The user receives a failure toast and no success is claimed.
- Statistics can change between the prepared snapshot and the eventual save. The export is a
  deliberate snapshot of the period selected when the action was invoked.
- The picker result can be lost if the process is killed; saved state covers activity
  recreation but not process death while the external picker is open.
