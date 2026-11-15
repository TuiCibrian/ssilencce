# Tasks

- [x] Add plain-text notes/bookmark model and codec (header + `[Notes]` + `[Bookmarks]` sections).
- [x] Add `SongNotesRepository` with app-specific external `notes/` folder, id-based file lookup,
      atomic writes and empty-file cleanup.
- [x] Register repository and `SongNotesViewModel` in Koin.
- [x] Add `SongNotesFragment`/`SongNotesScreen` (notes editor, bookmark list with seek, add/edit/
      delete, share, import, storage path hint) and navigation destination.
- [x] Add Now Playing quick bookmark action (quick-access action, player overflow menu, Aurora
      header/overflow) and `NowPlayingAction` entries.
- [x] Add song-menu entry for Notes & bookmarks.
- [x] Add bookmark drawable and English strings.
- [x] Include notes in backup archives and restore as a selectable backup category.
- [x] Compile gate passes (`:app:compileGithubDebugKotlin`).
