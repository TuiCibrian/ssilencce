# Documentation delta - merge lyrics-tools

Apply `openspec/changes/lyrics-tools/DOCS_DELTA.md` with these corrections for the integration.

## docs/FEATURES.md - section 3 "Lyrics"

Replace the proposed **Lyric line selection** bullet with:

- **Lyric line selection** - On the full lyrics screen, the select-lines action enters selection mode; tap lines to toggle them (long-press also toggles in this mode), Select all, then Copy to the clipboard or Share as `text/plain` through the system chooser. Plain lyrics can also start selection with a long-press. Selection is reset when the song or resolved lyrics change. Copied text contains the main line text only (translations/transliterations are not included).

Keep the **A-B repeat from lyrics** bullet from `ab-repeat-speed`; long-press on a synced line continues to open the A-B repeat sheet in normal mode.

## docs/CODE_MAP.md - update existing entries

- `ui/component/compose/lyrics/LyricsView.kt`
  - **Inputs:** add `selectionMode`, `selectedLineIds`, `onLineSelectionToggle`; keep `onLineLongPress` (A-B repeat).
  - **Responsibilities:** add "normal-mode tap seeks and long-press opens A-B repeat; in selection mode tap and long-press toggle line selection; selected lines are highlighted and auto-scroll pauses."
  - **Related files:** add `LyricsSelectionBar.kt`, `AbRepeatLineSheet.kt`.
- `ui/screen/lyrics/LyricsScreen.kt`
  - **Responsibilities:** add "select-lines action for lyric multi-selection (coexisting with A-B repeat long-press), lyrics-source FAB/dialog, SAF document pick with persisted read permission, plain lyrics rendered as selectable LazyColumn rows."
  - **Inputs:** add `lyricsSourceSelection` flow.
  - **Dependencies:** add `LyricsSelectionBar`, `LyricsSourceDialog`, `AbRepeatLineSheet`, ActivityResultContracts.OpenDocument.
  - **Related files:** add `LyricsSelectionBar.kt`, `LyricsSourceDialog.kt`.
  - **Notes:** add "selection resets on lyrics/song changes; auto-scroll pauses in selection mode; synced-line long-press is shared, with selection mode taking precedence while active."
- `ui/component/compose/lyrics/LyricsSelectionBar.kt`
  - **Notes:** change the select-all note to "enabled whenever the bar is visible; selection can be entered before any line is chosen."

## AGENTS.md (condensed code map)

- `ui/screen/` lyrics mention: use "lyrics/ (select-lines multi-select/copy/share, per-song source picker, manual LRC sync)" instead of the wording proposed in the lyrics-tools delta.

## Decisions

- **Long-press coexistence:** A-B repeat keeps the normal-mode long-press on synced lines, matching the already-integrated `ab-repeat-speed` behavior. Lyric multi-selection gets an explicit select-lines FAB; while selection mode is active, tap and long-press both toggle lines.
- **Selection with zero lines:** entering selection mode from the FAB leaves no line selected, so Select all is enabled while the bar is visible instead of being disabled at count zero.
- **Unchanged surfaces:** the lyrics-source FAB/dialog and the Edit/Sync LRC editor stay on the same screen and keep their original callbacks. No source-resolution or sync-editor behavior was altered by the merge.
- **Lint:** the `lint-checkpoint-1` A-B `stringResource` composition-time message resolution is retained unchanged.
- **New English string:** `lyrics_select_lines` ("Select lyrics") for the new FAB content description; no other strings changed and no non-English resource was edited.
- **No ADR:** this is a UI-callback merge, not an architectural change.
