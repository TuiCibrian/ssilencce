# Documentation delta — song notes & bookmarks

## `docs/FEATURES.md`

Insert after the **Lyrics editor** bullet (section 3, before **Instrumental detection**):

> - **Song notes & bookmarks** — Per-song notes and timestamped bookmarks stored as plain UTF-8
>   `.txt` files in the app-specific external `notes/` folder (`<song id> - <title>.txt`), readable
>   and editable outside the app. Now Playing adds a one-tap bookmark from the overflow menu, the
>   configurable quick-action row, cover gestures and the Aurora header; a **Notes & bookmarks**
>   screen (player overflow, quick action and song menu) edits notes, lists bookmarks sorted by
>   position (tap to seek), adds/edits/deletes bookmark text, shares note text and imports text from
>   a file. Backups include the notes folder and restore it as the **Song notes & bookmarks**
>   content type.

In the **Backup/restore** bullet (section 12), add "song notes" to the enumerated zip contents, after
"lyrics cache".

## `docs/MISSING_FEATURES.md`

Remove this line from section 7:

> - **Bookmarks/Notes per song** — stored as shareable text files, importable/editable by other apps, quick bookmark button in Now Playing.

## `docs/CODE_MAP.md`

Add after the `data/local/lyrics/*` entries (e.g. after `data/local/lyrics/InstrumentalDetector.kt`):

```markdown
- `data/local/notes/SongNotesRepository.kt` — File-backed song notes and bookmarks in the app-specific external `notes/` directory.
  - **Purpose:** Read/write per-song notes as user-visible text files without a Room schema change.
  - **Responsibilities:** Resolve `<song id> - <title>.txt` (id-based lookup survives title edits), sanitize titles, atomic writes (temp + rename), delete files when content becomes empty, clean old-name duplicates, append one-tap bookmarks, expose note files for backup and validate restored file names.
  - **Inputs:** Context, Song, notes text, SongBookmark list.
  - **Outputs:** SongNotes?, Boolean bookmark append, File list, notes directory.
  - **Dependencies:** SongNotesCodec, java.io.File, Dispatchers.IO, Mutex.
  - **Consumed by:** SongNotesViewModel, AbsPlayerFragment (quick bookmark), BackupHelper.
  - **Related files:** SongNotes.kt, SongNotesCodec.kt, SongNotesViewModel.kt, BackupHelper.kt.
  - **Notes:** Falls back to `filesDir/notes` when external app storage is unavailable; only `<numeric id>` files are listed for backup.
```

Add under `data/model/` where song/queue models are listed:

```markdown
- `data/model/notes/SongNotes.kt` — Song notes model: `SongNotes(songId, notes, bookmarks)` and `SongBookmark(timestampMs, text)`.
  - **Purpose:** Hold the content of one notes file.
  - **Responsibilities:** Normalize bookmark positions and expose `hasContent`.
  - **Inputs/Outputs:** Plain Kotlin data.
  - **Dependencies:** None.
  - **Consumed by:** SongNotesRepository, SongNotesCodec, SongNotesViewModel, SongNotesScreen.
  - **Related files:** SongNotesCodec.kt.

- `data/model/notes/SongNotesCodec.kt` — Human-readable notes file codec.
  - **Purpose:** Encode/decode the `#` header, `[Notes]` and `[Bookmarks]` sections.
  - **Responsibilities:** Parse/format `m:ss`/`h:mm:ss` timestamps with optional milliseconds, tolerate missing sections, treat section-less files as plain notes, build share text.
  - **Inputs:** File text; song id/title/artist, notes, bookmarks.
  - **Outputs:** Content(notes, bookmarks); encoded text; share text; timestamp helpers.
  - **Dependencies:** None (pure Kotlin).
  - **Consumed by:** SongNotesRepository, SongNotesViewModel, SongNotesFragment.
  - **Related files:** SongNotes.kt.
```

Add before `ui/screen/widgets/WidgetConfigActivity.kt` (near the lyrics screens):

```markdown
- `ui/screen/notes/SongNotesViewModel.kt` — Notes screen state holder.
  - **Purpose:** Own notes/bookmark state and persist mutations through SongNotesRepository.
  - **Responsibilities:** Load, edit draft, explicit save, add/update/delete bookmark, merge imported text, emit one-shot events; serialize mutations with a Mutex.
  - **Inputs:** SongNotesRepository, Song.
  - **Outputs:** SongNotesUiState StateFlow, SongNotesEvent flow.
  - **Dependencies:** SongNotesRepository, SongNotesCodec, viewModelScope.
  - **Consumed by:** SongNotesFragment/SongNotesScreen.
  - **Related files:** SongNotesRepository.kt, SongNotesScreen.kt.
  - **Notes:** Import appends notes and unions bookmarks by (position, text).

- `ui/screen/notes/SongNotesFragment.kt` — Compose host for the per-song notes screen.
  - **Purpose:** Bridge navigation args, player state, file picker and sharing to SongNotesScreen.
  - **Responsibilities:** Read `extraSong`/`fromPlayer`, collect UI state and playback position, handle text/* import via OpenDocument, share note text, seek only when the song is current, re-expand the panel on exit when opened from the player.
  - **Inputs:** SongNotesFragmentArgs, PlayerViewModel, SongNotesViewModel.
  - **Outputs:** SongNotesScreen composition; seek/share/import side effects.
  - **Dependencies:** SongNotesViewModel, SongNotesScreen, SongNotesCodec, ActivityResultContracts.OpenDocument, Koin viewModel(parametersOf(Song)).
  - **Consumed by:** `nav_song_notes` from player overflow/quick action and song menu.
  - **Related files:** SongNotesScreen.kt, SongNotesViewModel.kt, graph_main.xml.
  - **Notes:** Navigation arg `fromPlayer` (default false) controls panel re-expansion.

- `ui/screen/notes/SongNotesScreen.kt` — Compose notes editor and bookmark list.
  - **Purpose:** Edit per-song notes, browse timestamped bookmarks and jump to them.
  - **Responsibilities:** TopAppBar add/import/share actions, notes text field with explicit Save, bookmark rows (tap seek, edit/delete dialogs), add-bookmark dialog with time + text, load-failure retry card, storage-path hint, Aurora transparent background.
  - **Inputs:** Song, SongNotesUiState, isCurrentSong, currentPositionMs, callbacks.
  - **Outputs:** User intents to the ViewModel/fragment.
  - **Dependencies:** Material3 Scaffold/TopAppBar/AlertDialog/OutlinedTextField, TopAppBarMenu, SeekTimeParser.
  - **Consumed by:** SongNotesFragment.
  - **Related files:** SongNotesFragment.kt, SongNotesViewModel.kt.
  - **Notes:** Time input accepts `mm:ss`/`h:mm:ss`; the current playback position pre-fills the add dialog.
```

Update the `BackupHelper.kt` entry: add "song notes (`notes/*.txt`)" to the responsibilities/backup contents and note `RestoreEntryKind.Notes` with a 1 MiB per-entry limit.

Update the `data/local/backup/BackupArchivePolicy.kt` entry: add `NOTES_PATH = "notes"` and `Notes` entry-kind detection for `notes/*.txt`.

Update the `ui/component/base/AbsPlayerFragment.kt` entry: Now Playing overflow/quick-access actions `AddBookmark` (atomic append at the current position) and `SongNotes` (navigate to `nav_song_notes`).

Update the `core/model/action/NowPlayingAction.kt` / `QuickAccessAction.kt` entries (if present) with the new `AddBookmark`/`SongNotes` and `Bookmark`/`SongNotes` values.

## `AGENTS.md`

In the **Repository Code Map**:

- Under `data/local/`, add: "`data/local/notes/` — File-backed per-song notes and bookmarks (`notes/<song id> - <title>.txt`), atomic writes and backup file listing."
- Under `data/model/`, add: "`data/model/notes/` — `SongNotes`, `SongBookmark` and the human-readable `SongNotesCodec` (`[Notes]`/`[Bookmarks]` sections)."
- Under `ui/screen/`, add: "`notes/` — Compose notes editor/bookmark list with seek, share and text-file import."
- Under `res/`, mention new menu entries `action_add_bookmark`/`action_song_notes` only if the existing bullet enumerates them (optional).
- Update the BackupHelper bullet in `util/` to include song notes (`notes/*.txt`) in ZIP creation/restore.

## `docs/agents/domain.md`

No change: ADR count remains 51 (no new ADR).

## Decisions

- **No new preference key.** The quick-access row is already user-configurable; the bookmark and
  notes actions are opt-in there, and the player overflow/song menu entry is additive. Existing
  users see no behavior change until they use the new actions.
- **Storage location.** Notes live in `Context.getExternalFilesDir("notes")` (fallback
  `filesDir/notes`) as required by the brief; no SAF folder picker was added because it would
  complicate backup/restore and the default folder needs no permission.
- **Import semantics.** Importing a text file appends its notes (separated by a blank line) and
  unions any `[Bookmarks]` entries by position + text, so an import never destroys current notes.
- **Seek scope.** Tapping a bookmark seeks only when that song is the current playback item;
  otherwise the screen shows "Play this song to seek from a bookmark." rather than replacing the
  user's queue.
- **No Room schema change**, no new dependency, no manifest permission.
