# Proposal: Batch tag editor (AA-07, bounded first milestone)

## Why

The AA-07 read-only audit (`tags-batch-audit-2026-11-06/AUDIT.md`; not checked into this
repository, found in this workspace under the OpenCode temp verify directory) confirms that
ssilencce has entity-level bulk overwrite (album/artist) but no true multi-song batch tag editor:

- the album and artist editors read only the first song;
- one field map is applied to every song in the entity;
- blank means clear, so there is no way to express "leave unchanged";
- success means every processed file was scanned, not that each field survived;
- the multi-select song menu has no tag action.

This change adds the bounded first milestone of AA-07: a true multi-song selection editor that
shows common values and `Varies`, defaults every field to `Keep`, and writes only fields the user
explicitly changes or clears. It reuses the existing `MetadataReader`/`MetadataWriter` contract and
reports failures per file and per field.

## What Changes

- Add `action_tag_editor` to the multi-select song menu and launch a new Compose
  `BatchTagEditorActivity` with distinct selected song IDs.
- Add `BatchTagEditorViewModel` that resolves and reads every selected file, aggregates each field
  as `Common(value)`, `Varies` or `Absent`, and keeps unreadable files visible as skipped/failed.
- Add a 17-field text model (`TagEditField`) and a bounded plan (`TagEditPlan`) with explicit
  `Keep`, `Set(value)` and `Clear` operations. `Keep` is the default; blank text is never an
  implicit clear.
- Add `MetadataWriter.writeBatch` (or equivalent) that applies only `Set`/`Clear` overrides per
  file, reuses the existing current-map merge and `TagLib.savePropertyMap`, leaves `Keep` fields
  out of the override, and returns per-file/per-field results.
- Add read-back verification per file and field: `Written`, `Cleared`, `Unsupported`, `Mismatch`,
  `KeptChanged` and file failures. The UI reports failed files and fields; it does not show a
  blanket success when a requested field did not persist.
- Add main English strings (no translated resources) and Compose UI; reuse existing field labels
  and `MetadataReader` constants.
- Apply the 2026-11-07 legacy aggregation guard: `LegacyTagAggregator` reads every selected file,
  both legacy editors show common values/`Varies` and write only dirty fields, destructive
  totals/disc writes require confirmation, and the conditional `ARTIST` clear is removed.

## Scope

**In scope (milestone 1):** the 17 text fields already exposed by the single-song editor
(`TITLE`, `ALBUM`, `ARTIST`, `ALBUM_ARTIST`, `COMPOSER`, `CONDUCTOR`/`PRODUCER`,
`PUBLISHER`/`COPYRIGHT`, `GENRE`, `YEAR`, `TRACK_NUMBER`, `TRACK_TOTAL`, `DISC_NUMBER`,
`DISC_TOTAL`, `LYRICS`, `LYRICIST`, `ARRANGER`, `COMMENT`); one common `Set` value per field across
the selection; explicit `Clear`; sequential per-file write with progress and a final report; a
selection cap that keeps memory and the write request bounded.

**Out of scope for milestone 1:** MusicBrainz or online autofill for batches; new dependencies;
Room schema/entities/DAOs/migrations; app version changes; translated strings; semantic remapping
of the legacy `PRODUCER`/`COPYRIGHT` fields; changing the single-song editor.

Batch cover replace/delete, per-file distinct values, cancellation mid-write, undo/rollback,
in-place write backup/restore and routing the single-entity album/artist actions were originally out
of scope. Per-file values and cooperative stop landed in the 2026-11-08 follow-up; covers, session
undo/rollback, in-place backup/restore and single-entity routing are delivered by the 2026-11-14
follow-up (`openspec/changes/tag-editor-finish`). The runtime format matrix is still deferred.

**Legacy guard:** the 2026-11-07 follow-up applies the aggregation guard to the album/artist
editors: `LegacyTagAggregator` reads every selected file, both editors show common values/`Varies`
and write only dirty fields, destructive totals/disc writes require confirmation, and the
conditional `ARTIST` clear is removed. Since the 2026-11-14 follow-up the single-entity album/artist
tag actions launch the batch editor instead, so the legacy editors remain declared but are no longer
reachable from menus. AA-07 remains partially open only for the runtime format matrix.

## Impact

- New: `core/model/tag/TagEditField.kt`, `core/model/tag/TagEditPlan.kt`,
  `ui/screen/tageditor/BatchTagEditorActivity.kt`, `BatchTagEditorViewModel.kt`,
  `BatchTagEditorScreen.kt`, `ui/screen/tageditor/LegacyTagAggregator.kt`, optional
  `data/local/TagWriteVerifier.kt`, OpenSpec change and static probe plan.
- Modified: `data/local/MetadataWriter.kt`, `core/model/tag/TagEditReport.kt`,
  `res/menu/menu_media_selection.xml`, `ui/component/menu/MenuItemClickExt.kt`, `MainModule.kt`,
  `AndroidManifest.xml`, main English `strings.xml`, `ui/screen/tageditor/AlbumTagEditorActivity.kt`,
  `ui/screen/tageditor/ArtistTagEditorActivity.kt`, `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`.
  The 2026-11-14 follow-up additionally adds `core/model/tag/TagEditCover.kt`,
  `data/local/BatchWriteJournal.kt`, `ui/screen/tageditor/BatchTagEditorCover.kt` and routes the
  single-entity album/artist actions to the batch editor.
- Unchanged: Room schema/version, Gradle dependencies, app version, translated resources, existing
  single-song editor behavior, legacy `write()` cover path.
