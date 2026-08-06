# ADR-018: Smart Shuffle and Playback Statistics

## Status

Accepted

## Context

Beyond plain shuffle, the app offers "smart" shuffle modes (most played, favorites, forgotten
songs, recently added, combined) and group shuffle by album/artist. These need per-song play
statistics, a fair (non-repeating, distribution-aware) weighting, and history windows. All
statistics must be derived from playback behavior without user-visible tracking overhead.

## Decision

- Track statistics in Room (ADR-008):
  - `PlayCountEntity` (playCount, skipCount, timePlayed) upserted transactionally on play/skip
    events, with a top-100 cap,
  - `HistoryEntity` (song snapshot + timePlayed) with cutoff-based queries (today/week/month/…
    via `CalendarExt.getCutoffTimeMillis`) and a 100-entry limit.
- Decide the play-count bump with `SongPlayCountHelper`: a stopwatch that increments the count
  when the song has been played for 50% of its duration (skip-count on earlier change).
- Implement smart shuffle in `ShuffleManager`:
  - weighted modes (Most Played, Favorites, Forgotten, Recently Added, Combined, Pure Random)
    scoring songs with `ln(r)/weight` sorted descending,
  - group modes (ByGroup/BySong/FullRandom) over album/artist providers,
  - enrichment from `ExpandedSong` (Song + playCount/skipCount/lastPlayedAt/isFavorite),
  - graceful fallback to plain shuffle on any failure.
- Prune statistics for deleted files (repository-side cleanup on library refresh).
- Expose the modes via `SpecialShuffleMode`/`GroupShuffleMode` enums and a
  `ShuffleOperationState` (Idle/InProgress) so the UI can show busy state during long shuffles.

## Consequences

- "Forgotten songs" and "Most played" work from real listening data with no extra permissions.
- Weighted shuffles are O(n log n) over the library; large libraries show a short busy state
  (surfaced in the shuffle bottom sheet).
- The 50%-duration rule keeps counts honest against skipping behavior.
- Statistics live in app-scoped Room data, so they survive neither device migration nor
  reinstall — backups (ADR-031) cover settings/playlists, not play stats.
