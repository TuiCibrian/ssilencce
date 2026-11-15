# Docs delta: tag-editor-finish

The docs agent applies these to the project docs. Do not regenerate the whole files.

## docs/FEATURES.md

Replace the `**Multi-song batch tag editor**` bullet (section 10, currently the line starting with
"The song multi-select menu opens a Compose editor for the 17 text fields") with:

- **Multi-song batch tag editor** — The song multi-select menu and the single-entity album/artist
  tag actions open a Compose editor for the 17 text fields plus the cover. Fields show a common
  value, `Varies` or `Not set`; every field defaults to `Keep`, and only explicit `Set`/`Clear`
  fields are written. Values are aggregated from every readable selected file, unreadable/missing
  songs are reported as skipped, and writes reuse `MetadataWriter` with a read-back report for each
  file and field (`Written`, `Cleared`, `Unsupported`, `Mismatch`, `KeptChanged`). Per-file values
  are available from each field row, skipped files are listed before save, and the writing state can
  stop after the current file. The cover can be replaced with a Photo Picker image or removed from
  every selected file, with a read-back cover status and a MediaStore album-art refresh. Every file
  is copied to `cacheDir/tag-write-backup/` before the destructive open; a failed write restores the
  current file, and the result screen offers a session undo that restores every file written in the
  session. The runtime format matrix is still unproven and the operation remains sequential and
  foreground-only. The legacy album/artist editors aggregate all selected files, show `Varies`,
  write only changed fields, confirm destructive totals/disc writes, and no longer clear `ARTIST`
  conditionally; they stay in the manifest but are no longer reachable from menus because the
  single-entity album/artist actions now route to the batch editor.

## docs/MISSING_FEATURES.md

Part 1 §7, replace the `**Multi-song (batch) tag editing**` bullet with:

- **Multi-song (batch) tag editing** across all fields and formats (ssilencce has a multi-select
  text-field and cover batch editor with Keep/Set/Clear, per-file values, per-field read-back,
  in-place backup/restore and session undo; the album/artist single-entity actions route to the same
  editor; the full runtime format matrix across ID3v1/v2, MP4, FLAC/Vorbis, OGG/Opus, WAV, AIFF, WMA,
  DFF and DSF remains unproven).

Part 2 §17, replace the `- Remaining:` block (currently "batch artwork replace/delete,
single-entity routing to the batch editor ... runtime format matrix, session undo/rollback and
in-place write backup/restore") with:

- Remaining: the runtime format matrix across every container/codec combination (the editor, batch
  cover replace/delete, session undo/rollback and in-place write backup/restore are implemented at
  source; no device PASS is claimed).

## docs/CODE_MAP.md

Add:

- `core/model/tag/TagEditCover.kt` — Purpose: batch cover model. Responsibilities: `CoverOperation`
  with `Keep`/`Replace(bitmap)`/`Delete` and `CoverWriteStatus`; `hasChange` drives the writer and
  the UI summary. Inputs: picked `Bitmap`. Outputs: per-file cover status. Dependencies:
  `android.graphics.Bitmap`. Consumed by: `MetadataWriter.writeBatch`,
  `BatchTagEditorViewModel`, `BatchTagEditorScreen`, `BatchTagEditorCover`. Related:
  `TagEditPlan.kt`, `TagEditReport.kt`.
- `data/local/BatchWriteJournal.kt` — Purpose: session-scoped original-bytes backup and restore for
  batch tag writes. Responsibilities: copy each file to
  `cacheDir/tag-write-backup/<uuid>/<songId>.bak`, fail closed on copy/size/cap failures (2 GiB per
  file, 4 GiB per session), verify and restore with `"rwt"` plus restored-size check, notify/scan
  the restored file, discard entries, prune sessions older than seven days, delete the session
  directory on close. Inputs: `EditTarget.Content`, `Context` cache dir. Outputs: `Entry` tokens and
  restore results. Dependencies: `ContentResolver`, `MediaScannerConnection`. Consumed by:
  `MetadataWriter.writeBatch`, `BatchTagEditorViewModel`. Notes: transient cache storage, no hash
  sidecar and no process-death recovery (ADR-051 subset).
- `ui/screen/tageditor/BatchTagEditorCover.kt` — Purpose: Compose cover card for the batch editor.
  Responsibilities: preview the current or picked bitmap, expose Keep/Replace/Remove chips, show
  the no-cover and load-failure notes. Inputs: `BatchCoverState`, click callbacks. Consumed by:
  `BatchTagEditorScreen`.

Update:

- `data/local/MetadataWriter.kt` — add `writeBatch(cover, journal, albumIdBySongId,
  onProgress, shouldContinue)`: sequential per-file cover embed/delete with read-back verification,
  optional journal backup and current-file rollback, per-file cover status folded into the file
  status, and album-art cache refresh for affected albums. `write(context, target)` is unchanged.
- `core/model/tag/TagEditReport.kt` — `FileWriteReport` gains `coverStatus` and `holdsChanges`;
  `BatchWriteReport` gains `coverRequested`, cover counters and cover-aware `isFullSuccess`.
- `ui/screen/tageditor/BatchTagEditorViewModel.kt` — cover state/preview and picked-image decoding
  (≤1200 px, 32 MiB cap), journal creation, `albumIdBySongId`, `undo`/`undoUris`/`onUndoUnavailable`,
  `Result.canUndo`, `Undoing`/`Undone` states, skipped reports with cover status, journal cleanup on
  `onCleared`.
- `ui/screen/tageditor/BatchTagEditorScreen.kt` — cover card entry, cover-aware save summary,
  cover report rows, undo/undoing/undone states, backup/rollback detail labels, busy-state close
  blocking during undo.
- `ui/screen/tageditor/BatchTagEditorActivity.kt` — Photo Picker registration for the cover,
  SAVE/UNDO `createWriteRequest` purposes, undo callback, back blocking during undo.
- `ui/component/menu/MenuItemClickExt.kt` — `launchBatchTagEditor` helper shared by the multi-select,
  album and artist actions; single-entity album/artist tag actions now launch the batch editor.
- `ui/screen/tageditor/AlbumTagEditorActivity.kt`, `ArtistTagEditorActivity.kt`,
  `LegacyTagAggregator.kt` — unchanged sources, no longer reachable from menus (retained until
  runtime parity evidence allows removal).

## AGENTS.md

In the Repository Code Map, replace the `ui/screen/tageditor/` line fragment

`tageditor/ (single-song/album/artist editors, LegacyTagAggregator, and the multi-select batch tag editor with per-file values and cooperative stop)`

with

`tageditor/ (single-song editor, LegacyTagAggregator, and the multi-select + single-entity album/artist Compose batch editor with per-file values, cover replace/delete, cooperative stop, in-place backup/restore and session undo; the legacy album/artist editors remain declared but are no longer reachable from menus)`

## docs/agents/domain.md

After the ADR range line, append:

`ADR-051 is partially implemented by openspec/changes/tag-editor-finish (cache-dir session journal,
current-file rollback for hard failures, session undo); its fail-closed hash/sidecar validation and
process-death recovery are not implemented.`

## docs/architecture/ADR-051-batch-tag-write-integrity.md

Replace the Status section with:

```
## Status

Partially implemented (2026-11-14). `openspec/changes/add-batch-tag-editor` and
`openspec/changes/tag-editor-finish` implement a bounded subset: a session journal copies each
file's original bytes to `cacheDir/tag-write-backup/<uuid>/` before the destructive open (2 GiB
per file, 4 GiB per session, 7-day stale pruning), a failed open/save/read-back restores the
current file, and the editor offers a session undo for successfully written files. Not
implemented: the `noBackupFilesDir` binding, SHA-256/sidecar validation, fail-closed pre-open
identity checks, the 512 MiB free-space reserve, `MISMATCH`/`KEPT_CHANGED` rollback, and
`WRITE_STARTED`/`RECOVERY_NEEDED` process-death recovery. No device/runtime evidence exists.
```

Optional Decision addition:

- Session undo intentionally restores only hard failures; `Mismatch`/`KeptChanged` stay reported as
  `Partial` with undo available, because rolling them back would discard fields that did persist.

## Other change-local docs

- `openspec/changes/add-batch-tag-editor/specs/batch-tag-editor/spec.md` — Purpose sentence updated
  to point covers, session undo/rollback, in-place backup/restore and single-entity routing at the
  2026-11-14 follow-up. The new requirements live in
  `openspec/changes/tag-editor-finish/specs/batch-tag-editor/spec.md` (batch cover replace/delete,
  in-place backup and failure rollback, session undo, single-entity routing); `openspec validate`
  passes for both changes.

## Decisions

- Backup location: `cacheDir` as requested by the brief (ADR-051 proposed `noBackupFilesDir`). The
  system may clear cache under pressure; a missing backup makes undo/rollback fail cleanly and is
  reported, never silently ignored.
- Fail closed: a file whose original bytes cannot be copied or that exceeds the caps is not modified
  (`backup_failed`), preserving the current on-disk state.
- Rollback scope: hard failures only (`open_failed` no-op, `write_failed`, `save_failed`,
  `read_back_failed`). `Mismatch`/`KeptChanged` keep the existing `Partial` reporting and are
  revertible through undo.
- Cover preview: the first readable file's front cover is shown; there is no cross-file
  common/varies cover detection. Replacing or deleting applies to every written file.
- Album-art refresh: cover writes/deletes drop the MediaStore album-art row for each affected album
  (and insert a thumbnail on pre-Q); undo drops it again so the provider regenerates from the
  restored files.
- Routing: the legacy album/artist editors stay in the manifest but are unreachable from menus.
  Their exclusive capabilities (Deezer cover download, artist image, MusicBrainz) are not carried
  into the batch editor except cover replace/delete.
- Device/runtime gates (parent tasks 6.5, 8.7, 9.5) are deferred and marked as such; no build,
  lint, APK or device run is claimed.
