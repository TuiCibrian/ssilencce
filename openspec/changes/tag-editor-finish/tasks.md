# Tasks: Finish the batch tag editor (covers, undo, routing)

Parent change: `openspec/changes/add-batch-tag-editor` (tasks 7.6, 7.7, 8.6, 9.6).

## 1. Batch cover replace/delete

- [x] 1.1 Add `core/model/tag/TagEditCover.kt` with `CoverOperation` (`Keep`/`Replace(bitmap)`/
  `Delete`) and `CoverWriteStatus` (`KEPT`/`WRITTEN`/`DELETED`/`MISMATCH`/`NOT_ATTEMPTED`).
- [x] 1.2 Extend `MetadataWriter.writeBatch` with a cover parameter: embed the picked front cover,
  delete every embedded picture, or leave pictures untouched; keep `writeBatch` text-only behavior
  when the cover is `Keep`.
- [x] 1.3 Verify covers by reading pictures back (`Written`/`Deleted`/`Mismatch`) and fold the
  result into the per-file status (`Unchanged` + cover = `Succeeded`/`Failed`, text + cover failure
  = `Partial`).
- [x] 1.4 Refresh MediaStore album-art rows for the affected albums after a cover write or delete.
- [x] 1.5 Add the cover card to the Compose editor (current cover preview, Photo Picker replace,
  remove, keep, load-failure note) and the cover status/report rows.
- [x] 1.6 Register the Photo Picker and the cover callbacks in `BatchTagEditorActivity`.
- [x] 1.7 Add main English strings next to the existing batch strings; no translated files.

## 2. In-place backup and session undo

- [x] 2.1 Add `BatchWriteJournal`: stream each file's original bytes to
  `cacheDir/tag-write-backup/<uuid>/<songId>.bak`, fail closed on copy/size/cap failure, verify the
  original size, restore by overwriting with `"rwt"` and verifying the restored size, notify and
  rescan.
- [x] 2.2 Cap per-file backup at 2 GiB and per-session backups at 4 GiB; prune stale sessions older
  than 7 days.
- [x] 2.3 Create the journal in `BatchTagEditorViewModel.write`, pass it (plus the song→album map) to
  `writeBatch`, and report `backup_failed` without modifying files that could not be backed up.
- [x] 2.4 Restore the current file on open/exception/save/read-back failure, report
  `rolled_back`/`rollback_failed`, and discard the backup after a verified restore.
- [x] 2.5 Add the session undo action to the result screen, restore every changed file, refresh the
  album-art rows of restored albums, and report restored/failed counts.
- [x] 2.6 Re-request `MediaStore.createWriteRequest` for undo on API 30+ and block back/close while
  undo is running.
- [x] 2.7 Delete the session backup directory on `ViewModel.onCleared` (background thread).

## 3. Single-entity routing

- [x] 3.1 Route `Album.onAlbumMenu` `action_tag_editor` to `BatchTagEditorActivity` with the album's
  song IDs.
- [x] 3.2 Route `Artist.onArtistMenu` `action_tag_editor` to `BatchTagEditorActivity` with the
  artist's song IDs; keep composer entries returning `false`.
- [x] 3.3 Share one launch helper with the multi-select song path (dedupe, id filter, batch limit
  toasts).

## 4. Verification

- [x] 4.1 `.orchestra/build-gate.ps1` (`:app:compileGithubDebugKotlin`) passes (three green runs,
  final after the last source edit).
- [x] 4.2 Self-review the diff (no debug code, no unrelated edits, no unused imports); ktlint
  `check` and `git diff --check` pass; `openspec validate` passes for this change and the parent.
- [ ] 4.3 Device/runtime gates: format matrix, read-only/corrupt/process-death/100+ selection,
  legacy L1–L7, cover and undo behavior on a device — **deferred: device gate** (parent tasks 6.5,
  8.7, 9.5).
