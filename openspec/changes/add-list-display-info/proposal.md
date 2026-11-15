# Proposal: Song-list title mode and date modified suffix

## Why

Song rows currently show the file name only as a side effect of the File name sort key, so smart
lists and detail lists sorted by other keys cannot choose it. Lists also have no option to show
the file modification date, even though `Song.rawDateModified` is already available from
MediaStore. The two related gaps are tracked as one library display residual.

## What Changes

- Add a pure `SongTitleMode` (`FollowSort`, `Title`, `FileName`) and `SongListDisplayPolicy`.
  `FollowSort` preserves the current behavior exactly: file name only when sorted by File name.
- Add `song_title_mode` (default `FollowSort`) and `show_list_date_modified` (default `false`) to
  preferences.
- Resolve row titles through the policy in `SongAdapter`, including selection names and the
  FastScroller section popup, and in `SearchAdapter` for global-search song rows.
- Append a date-only "Date modified" label to the existing secondary text when enabled, in
  `SongAdapter`, the `SimpleSongAdapter` override and `SearchAdapter`. Compact rows without a text
  view are unchanged.
- Add the two controls to the Appearance > Display settings section and the main English
  strings/arrays.
- Add this OpenSpec change and update the project docs.
- No Room schema, migration, dependency, layout or version change.

## Impact

- New: `core/model/list/SongListDisplay.kt`, OpenSpec `add-list-display-info`.
- Modified: `Preferences`, `SongAdapter`, `SimpleSongAdapter`, `SearchAdapter`, `DataExt`,
  appearance preferences, main strings/arrays, `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`,
  `AGENTS`.
- Existing sort behavior and per-list sort persistence are unchanged.
- Lists pick up a changed display preference on their next bind/reload; no preference observer is
  added to every adapter.
