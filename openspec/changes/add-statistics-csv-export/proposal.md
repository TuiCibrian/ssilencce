# Proposal: Manual statistics CSV export

## Why

Top Tracks and Top Artists already expose period-aware listening statistics, but the data
cannot leave the app. Users who want to archive or analyse their listening history have to
copy values manually. Musicolet offers statistics export, and the missing CSV gap is tracked
in `docs/MISSING_FEATURES.md`.

## What Changes

- Add an **Export statistics** action to the Top Tracks and Top Artists options menu only.
- Export the currently selected period's top 100 rows through the Storage Access Framework
  using `text/csv`, with suggested `ssilencce-top-tracks-<period>.csv` and
  `ssilencce-top-artists-<period>.csv` names.
- Preserve the screen's ordering and semantics: all-time tracks use aggregate play counts,
  period tracks use recorded play events, and artists use the existing `ArtistPlayStat`
  recorded-name/percentage model.
- Write UTF-8 CSV without a BOM with CRLF rows, RFC 4180 text quoting, and spreadsheet
  formula-injection neutralization.
- Handle empty statistics, cancel, query/write failures and partial documents without leaving
  a corrupt export or crashing the app.

## Impact

- Affected code: `Repository`/`SmartRepository` play-stat queries, a new `SongPlayStat`
  model, a pure `StatisticsCsvWriter`, a Koin `StatisticsCsvExporter`,
  `DetailListFragment`, `menu_list_detail.xml` and main English strings.
- Affected docs: `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`; OpenSpec change only, no ADR.
- No database schema, storage permission, dependency, test source set, version or translated
  resource changes.
