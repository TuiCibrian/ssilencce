# Design: Inline quick search on detail/secondary lists

## Context

`AbsRecyclerViewFragment` already owns the top-level quick-search pattern: a bottom Compose
`QuickSearchBar`, a fragment-owned `quickSearchQuery`, `STATE_QUICK_SEARCH` save/restore, 72 dp
extra list padding and live mini-player/navigation clearance. Detail screens extend
`AbsMainActivityFragment` instead, use different layouts and keep their adapters locally, so they
need a small host rather than the top-level base class. `PlaylistDetailFragment` additionally uses
`PlaylistSongAdapter.saveSongs(dataSet)` on pause and drag reordering, which makes a naive
filtered adapter a data-loss risk.

## Goals / Non-Goals

**Goals:**

- One consistent inline bar on every in-scope secondary list.
- Filter only already-loaded data; no debounce, repository, MediaStore or Room call per keystroke.
- Preserve full-list metadata and normal empty behavior; show `quick_search_empty` only for a
  real no-match.
- Keep the query across rotation/process recreation and restore the full list on clear.
- Make playlist filtering drag-safe and never persist a filtered subset.

**Non-Goals:**

- Search history (separate requirement; no storage/UI is added).
- Filtering album grids, similar-artist rails, wiki/header content or multiple sections.
- New navigation, ViewModel, Compose screen, dependency, permission or database schema.
- Replacing the existing scoped global-search action.

## Decisions

### Shared host and pure policy

`QuickSearchSupport.kt` introduces:

- `QuickSearchHost(fragment, bar, recyclerView, miniPlayerMargin, extraBottomPadding,
  queryProvider, onQueryChanged)`: sets `DisposeOnViewTreeLifecycleDestroyed`, hosts
  `QuickSearchBar` in `SsilencceTheme`, wires the mini-player/navigation margin, adds the same
  72 dp extra list padding as the top-level screens and replays a restored query.
- `QuickSearchState`: the shared `quick_search_query` saved-state key.

`QuickSearchPolicy.kt` is Android-free: blank matches all, otherwise a case-insensitive
`contains` over any supplied field; `isNoMatch` is true only when the query is non-blank, the
source is non-empty and the visible list is empty.

`DetailQuickSearch.kt` maps models to fields:

- Song: title, album name, display artist; the folder screen additionally includes `fileName`.
- Album: name, artist name, album-artist name.
- Artist: display name.
- `ArtistPlayStat`: recorded artist name and resolved artist display name.

### Fragment ownership

Each fragment stores `quickSearchQuery`, restores it in `onViewCreated` with
`QuickSearchState.restore`, saves it in `onSaveInstanceState`, and re-applies it after every data
emission. The bar's Compose text is only a mirror; the fragment is the durable owner. This matches
the existing top-level pattern and survives view recreation.

### DetailListFragment

Maintains full snapshots (`allSongs`, `allArtists`, `allAlbums`, `allArtistStats`) separate from
the adapter's visible data. `applyQuickSearch` filters the snapshot for the active content type:

- song types filter `allSongs`;
- Top Artists filters `ArtistPlayStat` **before** `submitPlayStats`, preserving each stat's
  percentage and using the filtered artists for Play/Shuffle;
- recent artists filter `allArtists`;
- top/recent albums filter `allAlbums`.

Subtitle counts/durations remain based on the full snapshot. A full empty source keeps the
existing navigate-up/empty behavior; a filtered empty result never navigates up and shows
`quick_search_empty`.

### Album / Artist detail

Only the Songs section is filtered. `allSongs` remains the full album/artist song list; the song
adapter holds the visible subset. The section header keeps the full song count or shows
`quick_search_empty` for a real no-match. Header Play/Shuffle use the visible subset when a query
is active. Album grids/rails, similar-artist rails and wiki content are untouched.

### Genre / Year / Folder detail

Maintain `allSongs`; filter the existing `SongAdapter` in place. Full duration/count metadata
stays on the subtitle. A genuine empty source retains the previous navigate-up/pop behavior; a
filtered empty result shows `quick_search_empty`. Folder matching adds `Song.fileName`.

### Playlist drag safety

`PlaylistDetailFragment` keeps `allSongs` as the full playlist order and filters only the adapter
display list:

1. On a non-blank query, cancel any active drag and force `setLockDrag(true)`.
2. Never let `onPause` save the filtered adapter list. `PlaylistSongAdapter.saveSongs` gains a
   `songs` parameter; the fragment always passes `allSongs`.
3. On clear, restore `allSongs`, restore the user's `Preferences.lockedPlaylists` drag state and
   re-apply the normal empty state.
4. A Room emission while filtered replaces `allSongs` and re-applies the query; removal actions
   still operate on visible songs and never persist the filtered subset.
5. Do not save before the first song emission (`songsLoaded` guard), avoiding a transient empty
   save during fast navigation.

### Layout and clearance

`fragment_detail_list.xml`, `fragment_album_detail.xml`, `fragment_artist_detail.xml` and
`fragment_playlist_detail.xml` gain a bottom-gravity `ComposeView` named `quick_search_bar`,
initially gone. The host makes it visible, applies the mini-player/navigation bottom margin and
adds 72 dp list padding (plus the existing 16 dp album/artist space). No XML search field is
added.

### Strings and scope

Reuse `quick_search_hint`, `clear_action` and `quick_search_empty`; no translated files or new
English strings are required. Search history, permissions, Gradle dependencies, Room schema and
manifest components are untouched.

## Risks / Trade-offs

- Filtering is O(n) per keystroke over the loaded list; lists are already in memory. No debounce
  is added. If profiling shows jank on very large lists, a short debounce can be added later
  without changing the persistence/contract.
- Album/artist screens intentionally filter only songs; users may expect albums to filter too.
  The section-only behavior keeps the mixed screen predictable and is documented.
- Playlist filtering temporarily forces drag lock. The menu lock state still reflects the user's
  global preference; clearing the query restores that preference.
- `DetailListFragment` has several content types and adapters; the full snapshots are the source
  of truth for counts and empty behavior to avoid filtered metadata.

## Verification

- Pure JVM probe for `QuickSearchPolicy` (case-insensitivity, blank behavior, field isolation and
  no-match semantics).
- Static source probe for host/bar usage on every in-scope screen, correct match fields, Top
  Artists stat filtering, folder filename matching, saved-state key, playlist full-list save and
  absence of search-history/dependency/schema/permission changes.
- ktlint, `git diff --check`, `openspec validate --strict`, JDK 21 assemble/lint.
- Runtime/visual pass is deferred to the w5:pG runtime owner (no emulator is started by this
  change).
