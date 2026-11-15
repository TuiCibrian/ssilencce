# Tasks — ghost mode

## Storage and filtering

- [x] Add ghost preference keys (`ghost_mode_enabled`, `ghost_folders`, PIN hash/salt) and typed
      accessors next to the blacklist preferences.
- [x] Add `GhostMode` (folder normalization, path matching, SQL exclusion/inclusion selections,
      optional PIN hashing and session lock).
- [x] Filter the shared `RealSongRepository.makeSongCursor` query by ghost folders.
- [x] Provide a ghost-only song query for the Ghost search screen.
- [x] Keep direct ID / file-path / media-item lookups unfiltered so queues and explicit playback
      of ghost songs keep working.
- [x] Filter Room-backed playlist, favorite, history and play-stat queries.
- [x] Filter the direct MediaStore genre count and genre-song queries.
- [x] Exclude ghost songs from widget now-playing metadata.

## UI

- [x] Add `GhostSearchFragment` / `GhostSearchViewModel` / `GhostSearchScreen` (search, list,
      play, empty/disabled guidance, PIN gate).
- [x] Add `GhostSettingsFragment` / `GhostSettingsScreen` (master switch, folder list + picker,
      optional PIN, open Ghost search).
- [x] Add the Advanced > Privacy settings entry with a dynamic summary, hidden with the
      “unimportant settings” policy.
- [x] Open Ghost search by long-pressing the search field.
- [x] Add “Add to ghost folders” to the folder menus and reload all library surfaces after changes.
- [x] Add `nav_ghost_search` and `nav_ghost_settings` destinations.
- [x] Add the `Ghost search` notification button and lock on app background when a PIN exists.

## Strings / verification

- [x] Add English strings and plurals in `values/strings.xml`.
- [x] Compile gate passes.
- [x] Record docs changes in `DOCS_DELTA.md`.
