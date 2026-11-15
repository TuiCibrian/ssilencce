# Design: Most Played periods and artist statistics

## Context

`PlayCountEntity` holds `playCount`, `skipCount` and `timePlayed` (the latest play). Plays are
recorded in `PlaybackService.onMediaItemTransition` through
`Repository.insertOrIncrementPlayCount`. `DetailListFragment` renders Top Tracks and Top Artists;
`LibraryViewModel.playCountSongsFlow()` is all-time. The history table is capped at 100 rows and
is not a statistics source.

## Goals / Non-Goals

**Goals:**
- Period queries with real recorded data for this week, this month, the last 12 months, this
  year and all time.
- Artist play counts and percentages over the same period.
- A UI that fits the existing Top Tracks/Top Artists screens and clearly labels the period.

**Non-Goals:**
- Retroactive reconstruction of period data before the feature exists.
- CSV export, automatic stats export, or cross-device analytics.
- Changing the play-count increment threshold or scrobble behavior.

## Decisions

- **Append-only play events.** New `PlayEventEntity(id auto, songId, artistName, playedAt)`.
  `insertOrIncrementPlayCount` writes one event per counted play in the same transaction as the
  aggregate increment. Skips do not write events.
- **Period cutoffs are computed in local time.** Reuse `CalendarExt`/`calendarSingleton` for the
  start of the current week/month/year and a 12-month window; the DAO compares epoch millis.
- **Top songs query by event count.** Group events by song id inside the cutoff, order by count
  and then latest play, limit to 100, and map ids through the existing play-count rows. Fall
  back to the aggregate table for all time so pre-existing counts remain visible.
- **Artist stats derive from event rows.** Group by `artistName` for the cutoff, compute the
  percentage of total plays, and show it in the Top Artists subtitle (for example,
  `42 plays · 18%`). All-time artist stats use the existing top-artists path plus aggregate
  counts, and the UI labels the data source honestly.
- **Period selection is in-memory UI state.** The selected period is remembered for the screen
  session; it is not a global setting.

## Risks / Trade-offs

- The event table grows with listening; it is small per play and can be pruned by a later
  retention policy. No automatic pruning is added now to keep history honest.
- Period statistics are empty for periods before the feature; the UI explains this instead of
  fabricating numbers.
- Mapping event ids to songs still depends on MediaStore availability; missing media are skipped.
