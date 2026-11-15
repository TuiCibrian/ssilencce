# Proposal: Finish the batch tag editor (covers, undo, routing)

## Why

`openspec/changes/add-batch-tag-editor` delivered the bounded text-field batch editor and the
legacy aggregation guard, but tasks 7.6, 7.7, 8.6 and 9.6 stayed open: batch cover replace/delete,
session undo/rollback with an in-place backup of the original bytes, and routing the single-entity
album/artist tag actions to the batch editor. This change closes those source items.

## What Changes

- Batch covers: `CoverOperation` (`Keep`/`Replace`/`Delete`) is added to the batch plan. The editor
  shows the current cover, lets the user pick a replacement through the Photo Picker or remove the
  embedded cover, and the writer embeds/removes pictures per file with a read-back cover status
  (`Written`/`Deleted`/`Mismatch`/`NotAttempted`). Album-art cache rows are refreshed for the
  affected albums after the write.
- In-place backup/restore: a new `BatchWriteJournal` copies each file's original bytes to
  `cacheDir/tag-write-backup/<session>/` before the destructive open, fails closed when the copy
  cannot be verified, and restores the current file when a write fails (status `Failed` with
  `rolled_back`/`rollback_failed`). Backups are capped at 2 GiB per file and 4 GiB per session.
- Session undo: after a write, the result screen offers "Undo changes". The editor restores every
  file written in the session, rescans, drops the album-art cache for the restored albums, and
  reports restored/failed counts. Backups are deleted when the editor session ends.
- Single-entity routing: `Album.onAlbumMenu` / `Artist.onArtistMenu` now launch
  `BatchTagEditorActivity` with the entity's song IDs (composer-artist entries stay unavailable)
  instead of the legacy album/artist editors. Multi-select album/artist actions reach the same
  batch path.
- UI: the Compose batch editor gains a cover card, a cover section in the save summary and report,
  busy-state blocking during undo, and report detail strings for backup/rollback outcomes. Only
  main English strings are added.
- Deferred: device/runtime gates 6.5, 8.7 and 9.5 (format matrix, read-only/corrupt/process-death,
  legacy L1–L7) are marked "deferred: device gate" in the parent change; no build, APK or device run
  is performed here.

## Impact

- New: `core/model/tag/TagEditCover.kt`, `data/local/BatchWriteJournal.kt`,
  `ui/screen/tageditor/BatchTagEditorCover.kt`.
- Modified: `data/local/MetadataWriter.kt`, `core/model/tag/TagEditReport.kt`,
  `ui/screen/tageditor/BatchTagEditorViewModel.kt`, `BatchTagEditorScreen.kt`,
  `BatchTagEditorActivity.kt`, `ui/component/menu/MenuItemClickExt.kt`, `values/strings.xml`,
  `openspec/changes/add-batch-tag-editor/*` (tasks/design/proposal follow-up).
- Unchanged: Room schema/version, Gradle dependencies, app version, translated resources, the
  single-song editor and the legacy album/artist editor sources (they stay in the manifest but are
  no longer reachable from menus).
