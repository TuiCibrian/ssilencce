# Design: Add path and general song-list track sort keys

## Context

`SortKey` persists a stable string per selected field, `KeySortItem` maps it to a menu id and
title, and each `SortMode` subclass owns its own menu list and `sorted()` implementation. The
shared `sortingCollator()` is a frozen ICU collator with `numericCollation = true`;
`normalizeForSorting` strips leading articles when the setting is enabled.

Audit result for the documented extra keys:

| Key / behavior | Source state |
| --- | --- |
| Filename | Present: AllSongs, FolderSongs, AllFiles |
| Track number | Present: AlbumSongs, AllFiles; absent from AllSongs and FolderSongs |
| Date modified | Present: AllSongs, FolderSongs, AllFolders, AllFiles |
| Natural/numeric/alphanumeric | Present: ICU `numericCollation` on the shared collator |
| Path | Absent |
| Custom ignore-keyword whitelist | Absent; LIB-05, not this package |

## Goals / Non-Goals

**Goals:**

- Add full-path ordering to the song and file surfaces that already offer filename ordering.
- Make the existing track-number key available on the two general song lists.
- Keep the current per-entity menus, persistence keys and descending toggle.
- Reuse the cached locale collator so case/accent/numeric behavior stays consistent.

**Non-Goals:**

- Play-statistics keys (date last played, most/least played); `Song` has no play-stat projection,
  and adding one would require a data-layer join outside this bounded package.
- Custom ignore-keyword whitelist or any preference/settings work (LIB-05).
- Schema, dependency, translated-string, Compose-screen or version changes.
- Reordering or removing existing menu entries.

## Decisions

### Stable key and menu identity

`SortKey.Path` uses the persisted value `path_key`. `KeySortItem.Path` uses a dedicated
`action_sort_order_path` id and the English `sort_order_path` title. Adding the enum entry after
`FileName` does not change any existing persisted value; unknown or old values still fall back to
the per-mode default through `getSortKey`.

### Menu coverage

Path is offered exactly where filename is already offered:

- `SongSortMode.AllSongs`
- `SongSortMode.FolderSongs`
- `FileSortMode.AllFiles`

The track key is added to `SongSortMode.AllSongs` and `SongSortMode.FolderSongs`, which already
have album/artist context but lacked the existing track ordering. Album, artist, genre and year
menus keep their curated key sets. Every menu keeps its existing entries and `DescendingItem`.

### Ordering semantics

Song path ordering uses `compareBy(collator) { it.filePath }`. File-browser path ordering applies
the same comparator to folders and to songs before the existing folder-first concatenation.
Path and filename comparisons intentionally remain unnormalized, matching the current filename
behavior; title/artist/album keys keep article-aware normalization. While Path is selected, the
song-list FastScroller popup is suppressed because its title-based letter would no longer match the
path-based order.

Track ordering reuses the existing `SortKey.Track` branch, which orders by track number with a
title tiebreak and then applies the existing descending reversal.

### Deferred work

Date-last-played and most/least-played sorting need a play-count/last-played projection for every
song row. `PlayCountEntity` and `ExpandedSong` expose those fields, but ordinary `Song` lists do
not. A follow-up can add a read-only projection and reuse these menu ids; this change does not
change the model or database.

The custom ignore-keyword whitelist is LIB-05 and must be implemented separately; this change
does not modify `Preferences` or the library preferences XML.

## Risks / Trade-offs

- Track sorting across all songs groups equal track numbers globally (title tiebreak), which is
  the literal meaning of the key; album detail already has the same key with album context.
- Path sorting exposes full filesystem paths only through ordering, not as a new row field, so
  the compact UI and adapters are unchanged.
- Menu growth is limited to the three filename-covered surfaces and the two general song lists.

## Verification

- Static source probe for the key/menu/comparator wiring, preserved numeric/article behavior,
  English-only resources and absence of preference/schema/dependency/version edits.
- `git diff --check` and `openspec validate add-library-sort-keys --strict`.
- Gradle assemble/lint and runtime checks are deferred until the root grants a build lease.
