# ADR-008: Room for Derived App Data

## Status

Accepted

## Context

Besides the MediaStore-backed library (ADR-007), the app needs persistent state that MediaStore
does not provide: user-created playlists with custom covers, the playback queue, playback
history, per-song play counts, cached lyrics and the library include/exclude folder lists.
This state is transactional, relational, and queried reactively.

## Decision

- Use Room 2.8.4 (`room-ktx` + KSP compiler) as the persistence layer for derived data.
- Define a single database `SsilencceDatabase` (version 6) with 7 entities and 6 DAOs:
  - `PlaylistEntity` + `SongEntity` + `PlaylistWithSongs` relation: app playlists, songs as
    snapshots keyed by `(playlist_creator_id, song id)`, favorites as a special playlist.
  - `QueueEntity`: persisted playback queue (`id` + `order`).
  - `HistoryEntity`: playback history with cutoffs.
  - `PlayCountEntity`: play/skip counts and total time played (limit 100 top tracks).
  - `InclExclEntity`: blacklist/whitelist folder paths.
  - `LyricsEntity`: cached lyrics (text, provider, instrumental flag).
- Use explicit manual SQL migrations (`MIGRATION_1_2` … `MIGRATION_5_6`) and keep
  `exportSchema = false` (schema JSON is not committed).
- Prefer reactive reads: DAOs expose `Flow`/`LiveData` for playlist relations and stats;
  lyrics DAO is suspend-only (no flow).
- Version 6 history includes a lesson learned: `CanvasEntity` was created in 3→4 and dropped in
  4→5 (feature removed), and `LyricsEntity` was dropped and recreated in 5→6 (schema redesign).

## Consequences

- Transactional consistency and typed queries for playlists, queue, stats and cache.
- Room playlists are independent of MediaStore playlists; device playlists are read/written via
  MediaStore instead (dual playlist model).
- Manual migrations are auditable but error-prone; `exportSchema = false` removes the ability to
  diff schemas automatically in CI, so schema changes require extra care.
- Play count/history entities store song snapshots, which can become stale when tags change;
  repositories prune rows whose files were deleted.
