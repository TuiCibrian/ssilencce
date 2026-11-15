# Proposal: Add path and general song-list track sort keys

## Why

The documented sort-key gap is smaller than the issue text implies. A source audit found that
filename, track number, date modified and natural/numeric ordering already exist:

- filename sorts in AllSongs, FolderSongs and AllFiles;
- track number sorts in AlbumSongs and AllFiles;
- date modified sorts in AllSongs, FolderSongs, AllFolders and AllFiles;
- `sortingCollator()` already enables ICU `numericCollation`, giving natural/alphanumeric number
  ordering, and article-aware normalization is applied through `normalizeForSorting`.

Two gaps remain in this package. Full-path ordering has no `SortKey`, and the general song
surfaces (AllSongs and FolderSongs) do not expose the existing track-number key. The custom
ignore-keyword whitelist is adjacent but belongs to LIB-05, so it is out of scope.

## What Changes

- Add `SortKey.Path` (`path_key`) and `KeySortItem.Path`.
- Expose Path on AllSongs, FolderSongs and AllFiles, mirroring the existing filename coverage.
- Sort songs and folders by `filePath` with the shared locale collator.
- Expose the existing Track key on AllSongs and FolderSongs.
- Preserve all existing sort keys, menu ordering, descending behavior, per-list persistence,
  article normalization and numeric collation.
- Add only the English `sort_order_path` string and its menu id; do not touch translated strings.

## Impact

- Modified: `SortKey.kt`, `SortItem.kt`, `SortMode.kt`, `SongAdapter.kt`, `values/strings.xml`,
  `values/ids.xml`, `docs/FEATURES.md`, `docs/MISSING_FEATURES.md`, `docs/CODE_MAP.md`.
- No schema, dependency, preference, playback, queue or Compose-screen changes.
- Deferred: date-last-played and most/least-played keys need play-stat data on the song list;
  custom ignore-keyword whitelist is LIB-05.
