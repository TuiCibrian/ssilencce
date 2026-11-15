# Documentation delta (lyrics-tools)

## docs/FEATURES.md — section 3 "Lyrics"

Append these bullets after the existing **Sources** bullet:

- **Lyric line selection** - Long-press any lyric line (synced or plain) to enter selection mode; tap lines to toggle, Select all, then Copy to the clipboard or Share as `text/plain` through the system chooser. Selection is reset when the song or resolved lyrics change. Copied text contains the main line text only (translations/transliterations are not included).
- **Per-song lyrics source** - A tune action on the full lyrics screen opens a source picker: Automatic (existing file → embedded → downloaded priority), Embedded only, Downloaded only, or any `.lrc`/`.ttml` document picked through SAF. The choice is stored per song id in `filesDir/lyrics/lyrics_sources.json` (no Room schema change); picked documents keep a persisted read permission, and an unreadable document falls back to automatic resolution.
- **Manual lyric synchronization** - The lyrics editor has Edit and Sync modes. Sync mode lists the plain lyric lines, plays the current song, assigns the playback position to the target line on **Stamp**, enforces increasing timestamps, and adjusts each stamped line by ±100 ms. Stamped lines can be saved as embedded synced lyrics (existing metadata writer) or exported as an `.lrc` file next to the audio file (direct write when possible, SAF `CreateDocument` otherwise; a SAF export becomes the song's chosen lyrics file). TTML sources are not offered as stampable plain text.

Replace the existing **Lyrics editor** bullet with:

- **Lyrics editor** - Edit text with preview across Embedded/Downloaded/File sources, instrumental detection, online search, save to lyrics/LRC files, and an Edit/Sync mode switch for manual lyric timing (Stamp, ±100 ms, embedded or `.lrc` output).

Also update the Aurora capability bullet that currently reads "Tags/composers, manual lyrics timing, QR sharing, crossfade, mono and song-count timer remain visibly unavailable..." — remove "manual lyrics timing" from the unavailable list; manual timing is now implemented through the editor's Sync mode (QR sharing remains unavailable).

## docs/AURORA_REFERENCE.md

- Table row `Lyrics editor`: replace "no manual timing editor" with "manual timing through the Edit/Sync mode switch".
- "Explicitly unavailable" list: remove "Manual lyrics timing/synchronization" so the remaining item is "Lyrics QR sharing."

## docs/MISSING_FEATURES.md — section 7 "Tags, lyrics & album art"

Remove these three bullets:

- `**Create/edit synchronized LRC in-app** (ssilencce's editor has no sync/timing editing).`
- `**Select/copy/share lyric lines**.`
- `**Lyrics source selection per song** (embedded or any `.lrc` file).`

No other Part 1/Part 2/Appendix A bullet is affected.

## docs/CODE_MAP.md — new files

Add entries in the matching sections (same Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed by / Related files / Notes shape):

```text
- `data/model/lyrics/LyricsSourceSelection.kt` - Per-song lyrics source choice (Automatic/Embedded/Downloaded/File) plus optional SAF document uri/name.
  - **Purpose:** Model the user's per-song lyrics source override.
  - **Responsibilities:** Carry the source choice and picked-document metadata; expose title/summary resources.
  - **Inputs:** LyricsSourceChoice, file uri/name.
  - **Outputs:** Immutable LyricsSourceSelection instances.
  - **Dependencies:** Compose @Immutable, R.string.
  - **Consumed by:** LyricsSourceStore, LyricsRepository, LyricsViewModel, LyricsScreen, LyricsSourceDialog, LyricsEditorScreen.
  - **Related files:** LyricsSource.kt, LyricsRepository.kt, LyricsSourceStore.kt.
  - **Notes:** Automatic keeps the legacy file → embedded → downloaded priority.
```

```text
- `data/local/lyrics/LyricsSourceStore.kt` - JSON-backed per-song lyrics source overrides in `filesDir/lyrics/lyrics_sources.json`.
  - **Purpose:** Persist source choices without a Room schema migration.
  - **Responsibilities:** Lazy load, mutex-protected get/put, atomic temporary-file write, unknown-entry tolerant decoding.
  - **Inputs:** Song id, LyricsSourceSelection.
  - **Outputs:** LyricsSourceSelection/automatic default; updated JSON document.
  - **Dependencies:** kotlinx.serialization Json, coroutines Mutex/IO, File.
  - **Consumed by:** RealLyricsRepository.
  - **Related files:** LyricsSourceSelection.kt.
  - **Notes:** Automatic removes the entry; corrupt documents load as empty.
```

```text
- `data/local/lyrics/lrc/LrcTools.kt` - LRC timestamp formatting/parsing helpers for the manual sync editor.
  - **Purpose:** Convert plain lines to/from `[mm:ss.xx]` LRC text.
  - **Responsibilities:** Format/parse timestamps (1-3 fraction digits), strip existing line/word timestamps, skip metadata and TTML markup, serialize stamped lines in timestamp order.
  - **Inputs:** Lyric text, StampedLyricLine lists, playback millis.
  - **Outputs:** Timestamped LRC string or List<StampedLyricLine>.
  - **Dependencies:** java.util.Locale.
  - **Consumed by:** LyricsSyncEditor, LyricsEditorScreen (embedded/LRC saves).
  - **Related files:** LrcLyricsParser.kt, LyricsSyncEditor.kt.
  - **Notes:** Sorting on write keeps third-party players compatible with out-of-order adjustments.
```

```text
- `ui/component/compose/lyrics/LyricsSelectionBar.kt` - Contextual top bar for lyric line multi-select (count, select all, copy, share, close).
  - **Purpose:** Host the selection actions above the full lyrics list.
  - **Responsibilities:** Render count and icon actions; disable actions with no selection.
  - **Inputs:** selectedCount, onClose/onSelectAll/onCopy/onShare.
  - **Outputs:** Material3 TopAppBar composable.
  - **Dependencies:** Material3 TopAppBar/IconButton, resources.
  - **Consumed by:** LyricsScreen.
  - **Related files:** LyricsView.kt, LyricsScreen.kt.
  - **Notes:** Uses `lyrics_lines_selected`, `select_all_title`, platform copy and `action_share` labels.
```

```text
- `ui/screen/lyrics/LyricsSourceDialog.kt` - Per-song lyrics source picker dialog (Automatic/Embedded/Downloaded + SAF file).
  - **Purpose:** Let the user override the resolved lyrics source for one song.
  - **Responsibilities:** Radio list of source choices, current picked file name/summary, file picker entry point.
  - **Inputs:** Current LyricsSourceSelection, callbacks.
  - **Outputs:** Source choice or file-picker request.
  - **Dependencies:** AlertDialog, DialogListItemWithRadio, LyricsSourceChoice.
  - **Consumed by:** LyricsScreen.
  - **Related files:** LyricsSourceSelection.kt, LyricsScreen.kt.
  - **Notes:** The File row doubles as the chooser when no document is stored yet.
```

```text
- `ui/screen/lyrics/LyricsSyncEditor.kt` - Manual LRC sync surface and saveable stamping state for the lyrics editor.
  - **Purpose:** Stamp plain lyric lines against live playback and export the result.
  - **Responsibilities:** Play/pause and position display, Stamp target with monotonic timestamps, per-line ±100 ms, save dialog (embedded or `.lrc`), Bundle-saveable LyricsSyncState that survives Edit/Sync switches.
  - **Inputs:** LyricsSyncState, current position, isPlaying/isCurrentSong, save callbacks.
  - **Outputs:** Stamped LRC string through onSaveEmbedded/onExportFile; rendered list.
  - **Dependencies:** LrcTools, Material3, rememberSaveable/Saver.
  - **Consumed by:** LyricsEditorScreen.
  - **Related files:** LrcTools.kt, LyricsEditorScreen.kt, LyricsSourceSelection.kt.
  - **Notes:** When the song is not current, stamping/playback controls are disabled; TTML markup yields no stampable lines.
```

## docs/CODE_MAP.md — update existing entries

- `data/local/repository/LyricsRepository.kt`
  - **Responsibilities:** add "user-picked SAF lyrics document (`userFileLyrics`), per-song source selection get/set, file-cache invalidation".
  - **Inputs:** add "song id + LyricsSourceSelection".
  - **Dependencies:** add `LyricsSourceStore`.
  - **Related files:** add `LyricsSourceStore.kt`, `LyricsSourceSelection.kt`.
  - **Notes:** add "JSON store avoids a Room migration; unreadable picked documents fall back to automatic resolution."
- `ui/screen/lyrics/LyricsScreen.kt`
  - **Responsibilities:** add "long-press lyric selection with copy/share (synced and plain), lyrics-source FAB/dialog, SAF document pick with persisted read permission, plain lyrics rendered as selectable LazyColumn rows."
  - **Inputs:** add "lyricsSourceSelection flow".
  - **Dependencies:** add `LyricsSelectionBar`, `LyricsSourceDialog`, ActivityResultContracts.OpenDocument.
  - **Related files:** add `LyricsSelectionBar.kt`, `LyricsSourceDialog.kt`.
  - **Notes:** add "selection resets on lyrics/song changes; auto-scroll pauses in selection mode."
- `ui/screen/lyrics/LyricsViewModel.kt`
  - **Responsibilities:** add "per-song source resolution/choice flow (`lyricsSourceSelection`, `setLyricsSourceSelection`), lyrics-file cache refresh after exports."
  - **Outputs:** add "lyricsSourceSelection StateFlow".
  - **Dependencies:** add `LyricsSourceChoice`, `LyricsSourceSelection`.
  - **Notes:** add "explicit File documents win over automatic discovery; unreadable documents fall back."
- `ui/screen/lyrics/LyricsEditorScreen.kt`
  - **Responsibilities:** add "Edit/Sync mode switch, stamped LRC embedded save, direct/SAF `.lrc` export, player position/play-pause wiring, file source forces Edit."
  - **Inputs:** add `PlayerViewModel`.
  - **Dependencies:** add `LyricsSyncEditor`, `LrcTools`, `PlayerViewModel`, ActivityResultContracts.CreateDocument.
  - **Notes:** replace the obsolete "explanatory disabled manual-sync/QR actions" note with "Aurora manual-sync action now opens Sync mode; QR sharing remains unavailable."
- `ui/component/compose/lyrics/LyricsView.kt`
  - **Inputs:** add `selectionMode`, `selectedLineIds`, `onLineLongClick`, `onLineSelectionToggle`.
  - **Responsibilities:** add "long-press/tap selection with selected-line highlight and paused auto-scroll in selection mode."
  - **Related files:** add `LyricsSelectionBar.kt`.
- `extensions/files/SAFExt.kt`
  - **Responsibilities:** add "`getDocumentDisplayName(uri)` OpenableColumns helper reused by the lyrics source and LRC export flows."
  - **Consumed by:** add `LyricsScreen`, `LyricsEditorScreen`.

## AGENTS.md (condensed code map)

- `data/local/` bullet: append "; `lyrics/LyricsSourceStore` (per-song lyrics source JSON overrides)" and "`lyrics/lrc/LrcTools` (sync-editor timestamp/LRC helpers)".
- `data/model/` bullet: append "lyrics source selection".
- `ui/component/compose/` bullet: append "LyricsSelectionBar".
- `ui/screen/` lyrics mention: replace "lyrics/" with "lyrics/ (line multi-select/copy/share, per-song source picker, manual LRC sync)".
- No ADR count change.

## Decisions

- **No Room schema change:** per-song source choices live in `filesDir/lyrics/lyrics_sources.json`, as allowed by the brief. Automatic choices remove their entry so the file stays small.
- **Copy/share scope:** only the selected main lyric line texts are copied (no timestamps, translations or transliterations). Copy and Share end selection mode; the toast confirms copy.
- **Selection availability:** line selection is enabled on the full lyrics screen only; cover lyrics keep their existing tap-to-seek behavior (long-press remains a no-op there).
- **Unreadable picked document:** resolution falls back to the legacy automatic priority rather than showing an empty screen; the persisted choice remains so the file can become readable again.
- **File source vs. Sync:** choosing the File source forces Edit mode and hides the mode switch because file contents remain read-only in the editor.
- **TTML in Sync mode:** TTML markup is not split into stampable lines; Sync mode reports no synchronizable lines instead of presenting XML tags.
- **LRC export fallback:** a direct write next to the audio file is attempted off the main thread; when the folder is not writable, SAF `CreateDocument` is used and the exported document becomes the song's chosen lyrics file.
- **No new preferences:** the features are per-song/per-use actions; defaults keep the current behavior (automatic source, no selection mode, editor opens in Edit).

## ADR

None. The only storage choice is the brief-mandated JSON file in app storage; no architectural decision beyond that was made.
