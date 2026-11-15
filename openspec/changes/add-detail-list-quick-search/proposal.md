# Proposal: Inline quick search on detail/secondary lists

## Why

The seven top-level lists and the queue already have an inline quick-search bar, but detail and
secondary lists do not. Users can open a genre, album, artist, folder, playlist or statistics
list and lose the ability to filter the songs already on screen. The existing Compose
`QuickSearchBar`, fragment-owned query and saved-state pattern already provide the right
interaction; the remaining work is to host it on the secondary screens and filter their loaded
snapshots in place.

## What Changes

- Reuse the existing Compose `QuickSearchBar` and the `quick_search_query` saved-state key.
- Add a shared `QuickSearchHost` and pure `QuickSearchPolicy` for in-memory, case-insensitive
  substring filtering without debounce, database or network queries.
- Add the bar to `DetailListFragment` for all supported content types and to the Songs sections of
  `AlbumDetailFragment` and `ArtistDetailFragment`.
- Add the bar to `GenreDetailFragment`, `YearDetailFragment` and `FolderDetailFragment`.
- Include `PlaylistDetailFragment` only with drag-safe behavior: filter a separate display list,
  force drag lock while the query is non-blank, and persist the full playlist order, never the
  filtered subset.
- Keep header subtitle counts/durations based on the full list; Play/Shuffle operate on the
  visible filtered list when a query is active.
- Use `quick_search_empty` only for a non-blank query with zero visible rows; preserve each
  screen's normal empty behavior for a genuinely empty source.
- Do not add search history, dependencies, schema, permissions, translated strings or new
  navigation destinations.

## Impact

- New: `QuickSearchSupport.kt`, `QuickSearchPolicy.kt`, `DetailQuickSearch.kt`.
- Modified: `DetailListFragment`, album/artist/genre/year/folder/playlist detail fragments,
  `PlaylistSongAdapter.saveSongs` overload, four detail layouts.
- Unchanged: top-level quick-search behavior, global/scoped search, `QuickSearchBar`, search
  history (still absent), CSV/backup/headset/timer/import behavior.
