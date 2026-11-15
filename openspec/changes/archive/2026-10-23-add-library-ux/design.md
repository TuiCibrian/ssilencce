# Design: Library history, search, export and shortcuts

## Context

The history screen is `DetailListFragment(ContentType.History)` backed by
`LibraryViewModel.historySongsFlow()` and `SongAdapter`; `HistoryDao` already exposes single and
batch deletes. Settings use one `PreferenceScreen` per section. Top-level lists share
`AbsRecyclerViewFragment` but have different adapters and data models. Playlist export goes
through `M3UWriter`; launcher shortcuts use a static `shortcuts.xml` plus `MainActivity` action
handling.

## Goals / Non-Goals

**Goals:**
- History deletion works for one song and for a multi-selection, without touching queue rows,
  play counts or media files.
- Settings search finds every preference title/summary and navigates to its section.
- Quick search filters the visible list in place and resets when the query is cleared.
- Export offers M3U, M3U8 and PLS with honest success/failure messages.
- Per-entity shortcuts survive process death, can be removed, and report when the launcher limit
  is reached.

**Non-Goals:**
- Playlist import conflict handling, CSV stats export, or playlist export to a user-picked
  folder outside the existing MediaStore/SAF path.
- Full-text search across library content in the quick-search bar; it filters the already
  loaded list only.
- Search history, voice search changes, or system-wide search integration.

## Decisions

- **History removal uses song ids.** Add `deleteSongsInHistory(List<Long>)` to `Repository` and
  call it from `LibraryViewModel`; SQLite deletes history rows only. A history row is keyed by
  song id, so "remove this play from history" removes the song's history entry rather than one
  occurrence.
- **History actions are scoped to the history screen.** `SongAdapter` receives `historyMode`;
  the item overflow and multi-select action mode show `Remove from history` only there.
  Confirmation is required for a non-empty selection, then a snackbar reports the count.
- **Settings search is index + navigate.** Build an index by inflating each `SettingsScreen`
  preference XML and walking titles/summaries, then show grouped Compose results. Selecting a
  result navigates to the section (and scrolls to the key when the preference exists).
- **Quick search is per-list filtering.** `AbsRecyclerViewFragment` hosts a compact bottom bar;
  list fragments implement a `QuickSearchable` interface that receives the query and filters
  their cached data before assigning the adapter data set. Filtering is case-insensitive and
  locale-aware through `String.contains(ignoreCase = true)`.
- **Export format is a writer concern.** `M3UWriter` gains an `ExportFormat` enum and a common
  entry collector; M3U/M3U8 share the extended-M3U body with the appropriate header/extension,
  PLS writes numbered `FileN`/`TitleN`/`LengthN` keys. The UI uses a single-choice dialog; batch
  export uses the same format for every selected playlist.
- **Shortcuts are dynamic and entity-typed.** A helper builds `ShortcutInfoCompat` entries with
  action `com.ssilencce.shortcut.ENTITY` and extras for type and id/path; the confirmation
  dialog is the ask-before-adding step. `MainActivity` resolves the entity to songs and opens
  the queue. Dynamic shortcuts are refreshed/removed when the entity disappears.

## Risks / Trade-offs

- Removing a song from history is not an undoable event; a confirmation dialog limits accidental
  loss, and playback queues/play counts remain untouched.
- Settings search navigates to a section rather than opening a deep preference; the target key is
  scrolled into view when present.
- Quick search touches several list fragments; adapters keep their existing selection logic.
- Launcher shortcut capacity is launcher-controlled; the helper reports failure instead of
  silently dropping the request.
