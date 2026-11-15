# Detail-list quick search

## Purpose

Add the existing inline Compose quick-search interaction to secondary/detail lists without
changing their data sources, metadata or playback behavior, and without making playlist
filtering capable of persisting a truncated playlist.

## ADDED Requirements

### Requirement: Inline bar on secondary lists

The app SHALL show the existing Compose `QuickSearchBar` at the bottom of `DetailListFragment`
for every supported content type and on the Songs sections of album, artist, genre, year, folder
and playlist detail screens. The bar SHALL be always visible while the screen is active, use
`quick_search_hint`, and track the mini-player/navigation bottom margin. The list SHALL receive
bottom padding so the last row is not obscured. The bar SHALL be hosted in a `ComposeView` with
`DisposeOnViewTreeLifecycleDestroyed`; no XML search field SHALL be added.

#### Scenario: Open a secondary list
- **WHEN** a user opens a genre, year, folder, playlist, album, artist or detail/statistics list
- **THEN** the bottom quick-search field is visible and ready to filter the loaded list

### Requirement: In-place case-insensitive filtering

Typing SHALL immediately filter the already-loaded snapshot with a case-insensitive `contains`;
blank matches everything. No debounce, database, MediaStore or network query SHALL run per
keystroke. Match fields SHALL be:

- songs: title, album name and display artist; folder songs also include the file name;
- albums: album name, artist name and album-artist name;
- artists: display name;
- Top Artists: `ArtistPlayStat` entries filtered before `submitPlayStats`, preserving each
  entry's existing percentage.

#### Scenario: Filter songs
- **WHEN** the user types a query on a song list
- **THEN** only loaded songs whose title, album or display artist contains it remain, in the
  original order

#### Scenario: Filter Top Artists
- **WHEN** the user types a query on the Top Artists detail list
- **THEN** the visible `ArtistPlayStat` list is filtered before submission and each remaining
  artist keeps its original percentage

#### Scenario: Folder filename
- **WHEN** the user types a file-name fragment in a folder detail screen
- **THEN** matching songs remain even when the fragment is not in their title, album or artist

### Requirement: Clear, restore and saved state

Each fragment SHALL own the durable query and save it under the shared `quick_search_query` key.
The query SHALL be restored before or at the first data emission and re-applied after asynchronous
updates. Clearing SHALL restore the full snapshot and the normal empty state. The query SHALL NOT
be persisted in preferences or a database.

#### Scenario: Rotation
- **WHEN** the screen is recreated while a query is active
- **THEN** the field text, filtered rows and full snapshot are restored consistently

#### Scenario: Async update
- **WHEN** a Room/Flow emission arrives while a query is active
- **THEN** the full snapshot is replaced and the current query is re-applied

### Requirement: Empty-state semantics

`quick_search_empty` SHALL be shown only when the query is non-blank and the loaded source is
non-empty but the visible list is empty. A genuinely empty source SHALL keep the screen's normal
empty behavior, including `DetailListFragment`'s existing navigate-up path. A filtered empty
result SHALL never trigger navigate-up.

#### Scenario: No matches
- **WHEN** a non-blank query matches none of a non-empty source
- **THEN** the screen shows `quick_search_empty`

#### Scenario: Genuinely empty source
- **WHEN** the source list is empty and the query is blank
- **THEN** the normal empty message/behavior is unchanged

### Requirement: Header metadata and playback use visible rows

Header subtitle counts/durations SHALL remain based on the full list. Play/Shuffle actions SHALL
use the visible filtered rows when a query is active and the full list after clearing. Existing
scoped global-search actions SHALL remain available.

#### Scenario: Play while filtered
- **WHEN** a query is active and the user taps Play or Shuffle
- **THEN** only the visible filtered songs are queued

### Requirement: Playlist filtering is drag-safe

`PlaylistDetailFragment` SHALL keep the full playlist order separate from the filtered display
list. While the query is non-blank it SHALL cancel any drag, force drag lock and never persist the
filtered adapter subset. On pause it SHALL save the full `allSongs` order; on clear it SHALL
restore that order and the user's drag-lock preference.

#### Scenario: Leave while filtered
- **WHEN** the user filters a playlist and leaves the screen
- **THEN** the persisted playlist still contains every original song in the full order

#### Scenario: Clear after filtering
- **WHEN** the user clears the query
- **THEN** the full playlist returns and the prior drag-lock preference is restored

### Requirement: No search history or new platform surface

This change SHALL NOT add search history, a new dependency, permission, manifest component,
database entity/migration, navigation destination or translated string. Only main English strings
may be reused; no new string is required.

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no search-history storage/UI, dependency, permission, schema or translated-string
  change is present
