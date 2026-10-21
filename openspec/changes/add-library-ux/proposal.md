# Proposal: Library history, search, export and shortcut improvements

## Why

The library has several documented gaps that are locally actionable: playback history can only
be cleared globally, settings cannot be searched, top-level lists have no inline quick search,
playlist export only writes M3U, and launcher shortcuts only cover four built-in actions.

## What Changes

- Add per-song and multi-select removal from playback history, backed by the existing
  `HistoryDao` delete methods and leaving playback queues untouched.
- Add settings search across all preference sections with result navigation.
- Add an inline quick-search bar to the top-level library lists (songs, albums, artists,
  playlists, genres, years, folders).
- Add M3U8 and PLS export formats alongside the existing M3U export, with a format chooser.
- Add launcher shortcuts for specific albums, artists, folders and playlists through
  `ShortcutManagerCompat`, with a confirmation step and refresh-on-change handling.

## Impact

- Affected code: history UI (`DetailListFragment`, `SongAdapter`, menus, `LibraryViewModel`,
  `Repository`), settings (`SettingsFragment`, new search screen), list fragments and adapters,
  `M3UWriter` and playlist menus, `MainActivity`, shortcut helper, `menu_*.xml`,
  `AndroidManifest.xml` (only if a new component is required).
- Docs: CODE_MAP, FEATURES, MISSING_FEATURES, ADR if the shortcut/export decisions are
  architectural.
- New strings in `values/strings.xml` only.
