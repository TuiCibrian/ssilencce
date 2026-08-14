# ADR-007: MediaStore as the Source of Truth for the Music Library

## Status

Accepted

## Context

The music library (songs, albums, artists, genres, playlists, album art) is the user's on-device
media collection. The app could index files itself (like Musicolet-style players) or query the
system MediaStore. Indexing gives control but requires permission-driven full scans and
maintaining a local catalog; MediaStore is maintained by Android itself and works with every
music-managing app.

## Decision

- Treat MediaStore as the source of truth for all audio content. Room (ADR-008) holds only
  derived app data, never a copy of the library.
- Build all MediaStore queries through `MediaQueryDispatcher`, a fluent builder that projects,
  selects, and sorts audio URIs and executes `ContentResolver.query` safely (worker threads
  only, `IllegalArgumentException` guarded).
- Map cursors to models with safe readers (`CursorExt`: `mapIfValid`, `getLongSafe`,
  `getStringSafe`) and implement entities as aggregations over songs (`Album.splitIntoAlbums`,
  `Artist` grouping, `Genre` via `MediaStore.Genres`, `ReleaseYear`, `Folder` over the
  filesystem).
- Keep the library in sync with the system through `MediaStoreObserver`, a `ContentObserver`
  that batches change notifications for 500 ms and triggers a refresh callback (used by
  `PlaybackService`).
- Filter the library with the blacklist/whitelist feature by composing SQL selection on top of
  MediaStore queries (`InclExclDao` paths + `BASE_SELECTION` `IS_MUSIC` and non-empty title).
- Never cache full song lists; the in-memory caches in `LibraryViewModel` are refreshable
  views over the same queries.

## Consequences

- Zero-maintenance catalog: new files, removals and metadata edits by any app are reflected
  (debounced) without manual scanning.
- Queries are only as fast as MediaStore; large libraries pay on first load per screen.
- The library cannot represent data MediaStore does not expose (e.g. reliable per-file paths
  on scoped storage), which shapes folder browsing and delete flows (SAF, ADR-031).
- Aggregations (albums, artists, years) are recomputed from song rows on every query — no
  persisted aggregates, so custom merge rules are limited to in-memory grouping.
