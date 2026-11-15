# Design: Playlist import conflict policy

## Context

`ImportPlaylistDialog` loads device playlists, opens a multi-choice picker, and calls
`LibraryViewModel.importPlaylist`. The picker's `importSongs` subset is computed but not passed,
so the whole source playlist is imported. `LibraryViewModel` then silently suffixes the name up
to 100, creates the playlist and inserts songs in separate DAO calls, and reports the source
name in the success message.

`PlaylistDao.playlist(name)` is an exact, case-sensitive lookup. `PlaylistEntity` has no unique
index on `playlist_name`; duplicate names are possible and Replace must therefore reject an
ambiguous exact match. `Favorites` is an ordinary playlist row whose name comes from
`R.string.favorites_label`.

Backup restore (`BackupHelper.restorePlaylists`) is a separate path: on an exact name match it
merges resolvable songs into the existing playlist without a prompt. That behavior is retained
for this change and must not call the interactive dialog.

## Goals / Non-Goals

**Goals:**
- Import exactly the songs checked in the picker.
- Make every exact-name collision an explicit, user-visible choice.
- Keep the default non-destructive.
- Make create+insert and replace-contents atomic.
- Report the actual stored name.
- Keep backup restore a separate non-interactive merge.

**Non-Goals:**
- Adding a unique-name index or a Room migration.
- Changing the source-device playlist picker to Compose or adding a device-playlist ID.
- Changing backup restore into Keep both, Replace or a prompt.
- Changing playlist ordering, schema or unrelated playlist operations.
- Adding dependencies or test source sets.

## Decisions

### Exact matching and trimming

Conflict detection uses the existing exact SQL equality (`BINARY` collation), so names are
case-sensitive and trailing spaces are significant for the source name. Rename input is
trimmed before validation and storage; blank/whitespace-only input is rejected and a trimmed
name matching any existing name, including Favorites, is rejected.

### Pure name resolver

`PlaylistImportNameResolver` (data-layer package, no Android dependencies) owns:

- `resolveName(sourceName, existingNames)`: return the source name when free; otherwise try
  `"$sourceName 1"` through `"$sourceName 100"`; return `null` when exhausted.
- `validateRename(rawName, existingNames)`: `Blank`, `Duplicate(cleanName)` or
  `Valid(cleanName)` after trimming.
- `MAX_SUFFIX = 100`.

The resolver is probed against the compiled class.

### Import state machine

`LibraryViewModel` keeps a pending import (source name + deduplicated selected songs) and a
`StateFlow<PlaylistImportConflictState?>`. The state contains the exact matches, selected
count, existing-song count, default Keep-both name, all existing names for inline validation,
Replace eligibility and an optional inline error. It is not persisted; process death may lose
the dialog, but no DB mutation occurs.

`ImportPlaylistDialog`:
- opens the picker with all songs checked;
- passes the checked subset (deduplicated) to the ViewModel;
- observes the conflict state and shows `PlaylistImportConflictDialogFragment` when needed;
- observes import results, shows the actual-name success message and refreshes playlists.

The conflict fragment is a Compose `DialogFragment` (ComposeView + `SsilencceTheme` +
`Surface`) and observes the same activity-scoped ViewModel.

### Concurrency and transactions

`LibraryViewModel` wraps preparation, Keep-both resolution + creation, Rename validation +
creation and Replace revalidation + mutation in one `Mutex`. This serializes name resolution
with mutations in the interactive import path. It is not a schema-level guarantee; the design
explicitly avoids a unique index/migration.

`PlaylistDao` supplies the exact-name/count and by-ID queries. `RealPlaylistRepository` runs
each atomic operation in `database.withTransaction`, using the existing
`Song.toSongEntity(playlistId)` mapper:
- `createPlaylistWithSongsIfNameFree(playlist, songs)`: count the exact name inside the
  transaction; return `-1` if taken; insert the playlist row, map every selected `Song` with
  the new ID and insert all rows. Any exception rolls the transaction back.
- `replacePlaylistContentsIfEligible(playlistId, expectedName, favoritesName, songs)`:
  reload the target by ID; require the name to still equal `expectedName`, require exactly one
  exact-name row, and require the name not to equal Favorites; then delete all existing songs
  and map+insert the selected songs in the same transaction. The playlist row/metadata is
  never updated, so ID, name, description and custom cover are preserved.

`Repository`/`RealRepository` forward these operations without adding schema changes.

### Conflict actions

- **Keep both** (default): re-resolve the next free suffix from a fresh name snapshot and
  create transactionally; existing row untouched. If suffixes are exhausted, Keep both is
  disabled and an inline error offers Rename.
- **Replace existing**: available only when the exact match count is 1 and the target is not
  Favorites. Show the existing song count and selected count in the warning. Revalidate inside
  the transaction; if the target disappeared, changed name or became ambiguous, abort with a
  refreshed conflict/error instead of a partial replace.
- **Rename imported**: prefill with the Keep-both name; validate blank/duplicate inline;
  revalidate in the ViewModel; create a new playlist transactionally. It never renames the
  existing playlist.
- **Cancel/back/dismiss**: clear the pending state without calling the repository; no success
  toast.

### Result model

`ImportResult` carries `actualName`, `outcome` (`Created` or `Replaced`), `importedSongCount`
and the message. Created/renamed use `Imported as "%1$s" (%2$d songs).`; Replace uses
`Replaced "%1$s" with %2$d songs.` Failures use the existing error string with the source name.

### Backup restore

`BackupHelper.restorePlaylists` remains unchanged and explicitly separate: exact-name matches
merge resolvable backup songs into the existing playlist without a prompt or suffix/Replace
choice; missing playlists are created. It does not call the conflict dialog and does not use
the interactive policy. This is documented in `FEATURES` and `CODE_MAP` so future changes do
not accidentally unify the paths.

## Risks / Trade-offs

- Without a unique index, an external writer (for example backup restore) can still claim a
  name between snapshots. The conditional DAO transaction detects this and the ViewModel
  revalidates instead of producing a duplicate from the interactive path.
- Replace is intentionally unavailable for duplicate exact names and Favorites; users can
  still Keep both or Rename.
- A committed import cannot be undone by a later UI cancellation; no compensating delete is
  issued.
- Dialog state may be lost on process death; reopening import re-detects the conflict and no
  partial mutation is possible.
