# ADR-010: Single Repository Facade over Specialized Repositories

## Status

Accepted

## Context

The library and playback layers need songs, albums, artists, genres, years, folders, playlists,
search results, smart content and network metadata (scrobbling, artist/album info, cover
lookups). Exposing ten separate repositories to ViewModels and the playback service would
explode the number of injected dependencies and duplicate orchestration logic (e.g. delete
cleanup across entities, home suggestions, chunking).

## Decision

- Define one `Repository` interface (`data/local/repository/Repository.kt`) as the single
  façade consumed by `LibraryViewModel`, `PlayerViewModel`, `PlaybackService`, `LibraryProvider`
  and dialogs.
- The implementation delegates to specialized repositories, each with a `Real*` implementation
  bound in Koin (ADR-006):
  - `RealSongRepository`, `RealAlbumRepository`, `RealArtistRepository`,
    `RealGenreRepository` (MediaStore queries, ADR-007),
  - `RealSmartRepository` (top/recent/never-played content, play counts, history),
  - `RealSpecialRepository` (release years, filesystem folders),
  - `RealPlaylistRepository` (Room + device playlists, favorites),
  - `RealSearchRepository` (global + scoped search),
  - `NetworkRepositoryImpl` (Last.fm/ListenBrainz scrobbling, Deezer, MusicBrainz).
- Centralize cross-cutting concerns in the façade:
  - delete cleanup across playlists/queue/stats when songs are removed,
  - home suggestions (`ContentType`-based),
  - scrobble/now-playing dispatch,
  - chunked reads (`MAX_ITEMS_PER_CHUNK = 900`) to respect SQLite query limits.
- Keep model aggregation logic (album splitting, artist grouping, file system queries) inside
  the repositories and their mappers, not in ViewModels.

## Consequences

- ViewModels and the playback service depend on one contract; adding a data source only touches
  `Repository` and Koin bindings.
- The façade is large and grows with every feature; cohesion is preserved by delegating to
  single-purpose repositories.
- Search stays composable via `SearchFilter` implementations (Smart/Basic/LastAdded) that
  internally reuse the same repositories.
- Testing the façade requires stubbing every sub-repository; no test configuration exists yet
  (see Pending list).
