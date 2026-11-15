# Tasks - merge lyrics-tools

- [x] Applied `lyrics-tools.patch` with `git apply --3way`; only `LyricsView.kt` and `LyricsScreen.kt` needed manual conflict resolution.
- [x] `LyricsView`: kept `onLineLongPress` for A-B repeat in normal mode; taps and long-presses route to `onLineSelectionToggle` only while selection mode is active.
- [x] `LyricsScreen`: A-B line sheet and lyric selection state coexist; added a "Select lyrics" FAB so multi-selection remains reachable now that synced-line long-press is reserved for A-B repeat.
- [x] Kept the per-song lyrics source FAB/dialog and the Edit/Sync LRC editor unchanged and reachable.
- [x] Preserved the `lint-checkpoint-1` fix: A-B toast messages are resolved with `stringResource` during composition.
- [x] Preserved the four already-integrated briefs (`ab-repeat-speed`, `library-data`, `playback-transport`, `queue-actions`); no changes outside the lyrics integration area.
- [x] Compile gate green.
- [x] Self-review of the merge diff; no debug code or unrelated edits.
- [x] Integration adjustments documented in `DOCS_DELTA.md`.
