# Design: Library sub-categorization

## Context

`AlbumListFragment` shows one album grid backed by `LibraryViewModel.getAlbums()` and
`AlbumAdapter`. Songs already carry `composerName` from the API 30+ MediaStore projection added by
LIB-01, but `Album` has no composer field and no album screen exposes a composer selector.

`GenreDetailFragment` currently shows one `SongAdapter` backed by `repository.songsByGenre()`.
`GenreRepository` exposes only genre songs, while `AlbumRepository.splitIntoAlbums()` and
`RealArtistRepository` already group songs into albums and artists. Existing album/artist adapters
and detail screens can therefore be reused without new queries or persistence.

## Goals / Non-Goals

**Goals:**

- Narrow the existing album grid by normalized composer on API 30+ when composer data exists.
- Offer Songs/Albums/Artists inside genre detail using existing adapters and detail screens.
- Keep playback, shuffle, quick search and sorting aligned with the visible subcategory while the
  genre header stays full-genre.
- No schema migration, dependency, version change or new list layout.

**Non-Goals:**

- Sectioned album headers or a multiple-composer browser.
- Android Auto and global-search subcategory entries.
- Splitting multi-composer tags into separate names.
- Composer data on API < 30 through metadata scanning.

## Decisions

### Album composer filter

`AlbumListFragment` derives normalized composer names from `album.songs` with
`ComposerNamePolicy`. The overflow action is visible only when `hasR()` is true and at least one
normalized name exists; `showComposerChoice()` uses the existing Compose choice dialog with
"All composers" at index 0. A selected composer filters the existing grid to albums containing at
least one song whose normalized composer key matches. Quick search applies on top of the composer
filter. The selection is saved in instance state. No sectioned adapter is introduced.

### Genre content derivation

`Repository.genreContent(genreId)` performs one `songsByGenre()` query, groups the same songs into
albums with `splitIntoAlbums()`, and groups them into artists with the artist repository. The
existing minimum-song preferences continue to apply to album/artist collections. `GenreContent`
carries the full song snapshot plus derived albums/artists.

### Genre subcategory UI

`GenreDetailFragment` uses a pure `GenreSubcategory` enum (Songs/Albums/Artists) and the existing
Compose choice dialog for the View action. Each subcategory owns its existing adapter
(`SongAdapter`, `AlbumAdapter`, `ArtistAdapter`) and its dedicated sort mode
(`GenreSongs`, `GenreAlbums`, `GenreArtists`). Quick search filters the visible collection.
Play/Shuffle flatten the visible albums/artists back into their songs. The header subtitle remains
the full genre song count/duration, and the selected subcategory is saved in instance state.

## Risks / Trade-offs

- Composer filtering derives data from album songs, so it is only as complete as the API 30+
  MediaStore composer column. Devices below API 30 never see the action.
- Genre album/artist collections reflect songs that are actually tagged with that genre; an album
  with only some songs in the genre shows only those songs in the genre subcategory.
- The existing adapters render all three subcategories; no new Compose screen is added because the
  surrounding library is still View-based and the task explicitly reuses existing screens.
