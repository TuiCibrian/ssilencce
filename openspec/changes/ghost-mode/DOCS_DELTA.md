# Documentation delta — ghost mode

## `docs/FEATURES.md`

In section 4 (**Library & Organization**), after the existing **Library filtering** bullet, add:

> - **Ghost mode** — User-chosen ghost folders are excluded by path prefix from the shared song
>   query, so songs, albums, artists, genres, years, folders, playlists, favorites, history, play
>   counts, search, shuffle sources, widget now-playing metadata and Android Auto browsing all
>   ignore them. The filter lives in `GhostMode` + `RealSongRepository.makeSongCursor`; Room-backed
>   playlist/stat queries reapply it when mapping stored entities. Folder menus and
>   **Advanced > Privacy > Ghost mode** add/remove ghost folders; the master switch is off by
>   default. No Room or permission change.

In section 6 (**Search**), after **Search filters**, add:

> - **Ghost search** — A separate search screen lists and plays only songs inside ghost folders.
>   Open it by long-pressing the search field, from **Advanced > Privacy > Ghost mode**, or from
>   the optional `Ghost search` media-notification button. An optional 4–12 digit PIN gates the
>   screen; with no PIN configured it opens directly. The screen explains when ghost mode is off or
>   no folder is configured.

In section 1 or the notification bullet list, add to the notification-button description:

> - `Ghost search` is an optional media-notification button, shown only while ghost mode is enabled
>   and restricted to the app's own controller.

## `docs/MISSING_FEATURES.md`

Remove this line from section 6 (**Scanning & folder management**):

> - **Ghost mode** — hidden folders kept out of the library but searchable via GhostSearch (also from the lock screen).

## `docs/CODE_MAP.md`

Add under `util/` (near `AppVisibility.kt`):

```markdown
- `util/GhostMode.kt` — Ghost-folder privacy filter and optional PIN gate.
  - **Purpose:** Hide user-selected folder trees from library surfaces without a Room change.
  - **Responsibilities:** Normalize folder paths, match path prefixes, build MediaStore exclusion/inclusion selections, filter Song/SongEntity lists, hash and verify the optional PIN, and hold the per-process unlock flag.
  - **Inputs:** Preferences (ghost folders, enabled flag, PIN salt/hash), song paths.
  - **Outputs:** SQL selection + arguments, filtered lists, PIN verification/session lock.
  - **Dependencies:** Preferences, AudioColumns, java.io.File, MessageDigest, SecureRandom, Base64.
  - **Consumed by:** RealSongRepository, RealPlaylistRepository, RealSmartRepository, RealGenreRepository, PlaybackService, GhostSearchViewModel, GhostSettingsFragment.
  - **Related files:** Preferences.kt, SongRepository.kt.
  - **Notes:** PIN is an access barrier, not encryption; direct ID/path/media-item lookup bypasses the filter so playing a ghost song and restoring a queue still work.
```

Add under `ui/screen/`:

```markdown
- `ui/screen/ghost/GhostSearchFragment.kt` — Compose host for the ghost search destination.
  - **Purpose:** Bridge the main nav graph, PIN session and player to GhostSearchScreen.
  - **Responsibilities:** Lock/unlock on resume, refresh on return from other screens, open the selected song list as the playback queue.
  - **Inputs:** GhostSearchViewModel, PlayerViewModel, GhostMode.
  - **Outputs:** GhostSearchScreen composition and playback requests.
  - **Dependencies:** GhostSearchScreen/ViewModel, PlayerViewModel, GhostMode, CollapsibleAppBarScaffold.
  - **Consumed by:** `nav_ghost_search` (long-press search, notification action, ghost settings shortcut).
  - **Related files:** GhostSearchScreen.kt, GhostSearchViewModel.kt, graph_main.xml.

- `ui/screen/ghost/GhostSearchViewModel.kt` — Ghost search state holder.
  - **Purpose:** Load ghost-only songs off the main thread and hold the query.
  - **Responsibilities:** Read `SongRepository.ghostSongs()`, expose loading/list/query/mode state.
  - **Inputs:** SongRepository.
  - **Outputs:** GhostSearchUiState StateFlow.
  - **Dependencies:** SongRepository, GhostMode, viewModelScope.
  - **Consumed by:** GhostSearchFragment/Screen.
  - **Related files:** SongRepository.kt.

- `ui/screen/ghost/GhostSearchScreen.kt` — Compose ghost search UI.
  - **Purpose:** Search/play hidden songs and explain configuration/lock states.
  - **Responsibilities:** Reused quick-search bar, filtered list, empty/disabled/no-folder states, PIN keypad, play rows.
  - **Inputs:** GhostSearchUiState, locked flag, callbacks.
  - **Outputs:** User intents only.
  - **Dependencies:** Material3, QuickSearchBar, CollapsibleAppBarScaffold.
  - **Consumed by:** GhostSearchFragment.
  - **Related files:** GhostSearchFragment.kt.

- `ui/screen/settings/GhostSettingsFragment.kt` — Compose ghost-mode settings destination.
  - **Purpose:** Manage the master switch, ghost folders and optional PIN.
  - **Responsibilities:** FolderChooserDialog callback, preference updates, library reload, open Ghost search.
  - **Inputs:** GhostMode, LibraryViewModel.
  - **Outputs:** GhostSettingsScreen composition and persisted preferences.
  - **Dependencies:** GhostSettingsScreen, FolderChooserDialog, GhostMode.
  - **Consumed by:** `nav_ghost_settings` from Advanced > Privacy.
  - **Related files:** GhostSettingsScreen.kt, graph_settings.xml.

- `ui/screen/settings/GhostSettingsScreen.kt` — Compose ghost-mode settings UI.
  - **Purpose:** Present ghost mode controls and PIN setup/removal dialogs.
  - **Responsibilities:** Master switch card, ghost folder list/remove/add, PIN validation, open-search button and explanation.
  - **Inputs:** enabled/folders/pinEnabled and callbacks.
  - **Outputs:** User intents only.
  - **Dependencies:** Material3, File.
  - **Consumed by:** GhostSettingsFragment.
  - **Related files:** GhostSettingsFragment.kt.
```

Update `data/local/repository/SongRepository.kt`: add `ghostSongs()` and the `includeGhost`/`ghostOnly` cursor flags plus the central ghost selection. Update `PlaylistRepository.kt`: ghost filtering on Room-backed playlist/favorite queries and `PlaylistWithSongs`. Update `SmartRepository.kt`: ghost filtering in history/play-stat mappings and unresolved artist stats. Update `GenreRepository.kt`: ghost selection in direct genre count/song queries. Update `Preferences.kt`: ghost keys/accessors. Update `PlaybackService.kt`: `OPEN_GHOST_SEARCH` command, notification refresh and hidden widget metadata. Update `SsilencceMediaNotificationProvider.kt`: optional `GhostSearch` button. Update `MainActivity.kt`: `ACTION_OPEN_GHOST_SEARCH` and background PIN lock. Update the settings/folder fragments and navigation graphs.

## `AGENTS.md`

In the **Repository Code Map**:

- Under `util/`, add: "`util/GhostMode.kt` — ghost-folder path filter, MediaStore selections, and optional PIN/session gate."
- Under `ui/screen/`, add: "`ghost/` — Ghost search screen for songs hidden by ghost mode; `settings/GhostSettings*` — ghost-folder and PIN configuration."
- Under `data/local/repository/`, note that song/playlist/history/genre queries apply the ghost filter centrally.

## `docs/agents/domain.md`

ADR count changes from 51 to 52.

## ADR draft (`docs/architecture/ADR-052-ghost-mode-library-filter.md`)

```markdown
# ADR-052: Ghost mode filters the shared media query layer

- **Status:** Accepted
- **Context:** Private folders must disappear from every library surface without duplicating
  filtering logic in each screen, and without a Room schema change.
- **Decision:** Store ghost folder paths as a JSON list in preferences and apply a `DATA NOT LIKE`
  prefix exclusion inside `RealSongRepository.makeSongCursor`. Room-backed playlist/stat queries
  reapply the same path filter when mapping stored entities. Direct ID, file-path and media-item
  lookups bypass the filter so playback and queue restoration keep working. A separate
  `ghostSongs()` query uses an inclusion selection. The optional PIN is a preference-backed hash
  and a per-process unlock flag, not encryption.
- **Consequences:** All MediaStore-derived lists, counts, search, shuffle sources and Android Auto
  browsing honor the filter automatically. Any future query that bypasses `makeSongCursor` must
  apply `GhostMode` explicitly. Ghost songs already present in an active playback queue remain
  playable until removed, because the queue is playback state rather than a library list.
```

## Decisions

- **Preferences, not Room.** Ghost folders and the PIN live in SharedPreferences; the brief
  required no Room change.
- **Central cursor filter.** Screening `makeSongCursor` covers albums, artists, genres, years,
  folders, search, smart lists and Auto in one place. Genre queries that bypass the cursor and
  Room-backed playlist/stat mappings were updated explicitly.
- **Direct lookups bypass the filter.** `song(id)`, `songsByMediaItems`, `songByMediaItem` and
  `songByFilePath` keep ghost songs resolvable for playback/queue restore.
- **Folder selection auto-enables ghost mode.** Adding a folder from a folder menu or the settings
  picker is an explicit hide request; the master switch can still temporarily reveal everything.
- **Settings entry is hidden with unimportant settings.** The Advanced > Privacy item participates
  in the existing “Show unimportant settings” policy; long-press search and settings search remain
  alternate entries.
- **PIN scope.** The PIN is a 4–12 digit access barrier for Ghost search, hashed with a per-device
  salt; it does not encrypt files or preferences. The session locks when the main activity stops.
- **Notification button is optional.** The `Ghost search` notification button is only available
  while ghost mode is enabled; notification metadata itself is left untouched so the user can
  control playback from the lock screen. Widget now-playing metadata is replaced with “Hidden
  track” because widgets are always visible on the home screen.
- **Queue behavior.** Already-queued ghost songs remain in the active queue (playback state, not a
  library list); newly opened library/shuffle queues exclude them.
- **No lock-screen quick action beyond the notification button.** The notification button is the
  lockscreen entry; it deliberately opens the PIN gate when one is configured.
