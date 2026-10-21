# ADR-040: Playback Event History for Period Statistics

## Status

Accepted on 2026-10-20. Amends ADR-018.

## Context

ADR-018 stores aggregate play counts in `PlayCountEntity` with only the latest play timestamp.
That is sufficient for all-time top songs and weighted shuffle, but it cannot answer period
questions (this week, month, 12 months, year) or artist percentages without inventing history.
`HistoryEntity` is capped at 100 rows and is a browsing aid, not a statistics source.

## Decision

- Add `PlayEventEntity(songId, artistName, playedAt)` as an append-only event log. The aggregate
  count and the event are committed in one Room transaction through `PlayEventDao.recordPlay`.
- Record only counted plays, using the existing `SongPlayCountHelper` threshold; skips do not
  create events.
- Query top songs and artists by `playedAt >= cutoff` with `GROUP BY` and `COUNT(*)`; all-time
  queries continue to use `PlayCountEntity` so pre-existing counts remain visible.
- Group period artists by the exact recorded event name, never mutable aggregate metadata.
  Keep typed recorded-name and all-time aggregate-ID identities distinct. Current media is
  optional action metadata; unavailable/ambiguous artists remain visible as historical rows.
- Return the unbounded play total in the same SQL snapshot as each limited artist row.
  Percentages therefore use all selected-period plays and remain reactive when only an artist
  outside the displayed top 100 changes. All-time totals still include legitimate legacy counts.
- Period cutoffs use the device's local time zone and calendar boundaries via
  `ListeningPeriod.cutoffMillis`.
- Keep the event log append-only for now. A retention policy is a future decision; silently
  dropping old events would make historical periods dishonest.

## Consequences

- Period statistics are real and reproducible, but start at the version that introduced the
  event log. The UI states this and does not backfill.
- All-time includes plays recorded before the event log through the aggregate table.
- The event table grows with listening; it is small per play and can be pruned later if a
  retention policy is agreed.
- Room version 8 adds the table with `MIGRATION_7_8` (additive).
