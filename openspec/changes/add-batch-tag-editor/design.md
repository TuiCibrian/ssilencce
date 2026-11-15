# Design: Batch tag editor (AA-07, bounded first milestone)

## Context

The read-only audit (`tags-batch-audit-2026-11-06/AUDIT.md`) established the current contract:

- `MetadataWriter.write` applies one `properties` map to every `EditTarget.Content`; `write`
  treats success as "contents non-empty and every processed file scanned".
- `writePropertyMap` already preserves the file's current property map, removes null/blank keys,
  canonicalises ReplayGain, and calls `TagLib.savePropertyMap`, which returns only a boolean and
  discards the unsupported-property map.
- `TagEditorViewModel.loadContent` reads `target.first.uri`; `EditTarget.album`/`artist` expand to
  every song, so the old album/artist editors both read the first file and broadcast one map.
- `menu_media_selection.xml` has no tag-editor action; only single-song/album/artist menus expose
  `action_tag_editor`.

The batch editor therefore has to add selection, aggregation, explicit intent and read-back. It
must not duplicate TagLib format logic or treat `savePropertyMap(true)` as proof that every field
was written.

## Goals / Non-Goals

**Goals:**

- true multi-song selection from the existing multi-select menu;
- per-field `Common(value)` / `Varies` / `Absent` display;
- explicit per-field `Keep` (default), `Set(value)` and `Clear`;
- untouched values survive per file, including values that differ across files;
- reuse `MetadataReader` constants, `MetadataWriter`'s current-map merge and TagLib save path;
- per-file and per-field read-back reporting;
- bounded first milestone with no dependency, Room, version or translated-string changes.

**Non-Goals:**

- per-file distinct values beyond the sparse per-song override dialog added in the 2026-11-08 follow-up;
- semantic remap of the legacy `PRODUCER`/`COPYRIGHT` fields;
- standard `n/total` rewrite for track/disc totals;
- background/WorkManager execution;
- MusicBrainz/online autofill for batches;
- changing the single-song editor.

The original milestone excluded batch covers, undo/rollback and single-entity routing. Those are now
implemented by the 2026-11-14 follow-up (`openspec/changes/tag-editor-finish`); see
[Follow-up 2026-11-14](#follow-up-2026-11-14-batch-covers-session-undo-and-routing).

## Decisions

### Entry, identity and bounds

`menu_media_selection.xml` gains an `action_tag_editor` item (reuse
`@string/action_tag_editor` and the existing edit icon). `List<Song>.onSongsMenu` handles it,
because that is the shared multi-select handler and album/artist multi-select already delegates
unknown actions to it.

The menu passes distinct song IDs, not a `List<Song>` or `List<EditTarget.Content>`:

```kotlin
BatchTagEditorActivity.EXTRA_SONG_IDS: LongArray
```

IDs keep the binder payload small and let the editor re-resolve songs through
`Repository.songById`. Duplicate IDs are removed while preserving selection order. Missing or
deleted songs are reported as skipped and never counted as written.

`MAX_BATCH_SONGS = 500` bounds aggregation memory, the write request and the report size. The menu
and the editor both enforce the cap; an empty or oversized selection is rejected with a message
and no writes. The batch editor accepts 1 to `MAX_BATCH_SONGS` selected songs; a one-song selection
does not replace the single-song editor.

Only the multi-select path was new in the first milestone. The single-song editor keeps launching
its existing activity. Since the 2026-11-14 follow-up, the album and artist single-entity tag
actions also launch the batch editor with the entity's song IDs.

### Field model and plan

`TagEditField` is the single source of truth for the 17 text fields, in the same order as the
single-song editor:

| Enum | Property key | Label |
|---|---|---|
| `TITLE` | `MetadataReader.TITLE` | `R.string.title` |
| `ALBUM` | `MetadataReader.ALBUM` | `R.string.album` |
| `ARTIST` | `MetadataReader.ARTIST` | `R.string.artist` |
| `ALBUM_ARTIST` | `MetadataReader.ALBUM_ARTIST` | `R.string.album_artist` |
| `COMPOSER` | `MetadataReader.COMPOSER` | `R.string.composer` |
| `CONDUCTOR` | `MetadataReader.PRODUCER` | `R.string.conductor` |
| `PUBLISHER` | `MetadataReader.COPYRIGHT` | `R.string.publisher` |
| `GENRE` | `MetadataReader.GENRE` | `R.string.genre` |
| `YEAR` | `MetadataReader.YEAR` | `R.string.year` |
| `TRACK_NUMBER` | `MetadataReader.TRACK_NUMBER` | `R.string.track` |
| `TRACK_TOTAL` | `MetadataReader.TRACK_TOTAL` | `R.string.track_total` |
| `DISC_NUMBER` | `MetadataReader.DISC_NUMBER` | `R.string.disc` |
| `DISC_TOTAL` | `MetadataReader.DISC_TOTAL` | `R.string.disc_total` |
| `LYRICS` | `MetadataReader.LYRICS` | `R.string.lyrics` |
| `LYRICIST` | `MetadataReader.LYRICIST` | `R.string.lyricist` |
| `ARRANGER` | `MetadataReader.ARRANGER` | `R.string.arranger` |
| `COMMENT` | `MetadataReader.COMMENT` | `R.string.comment` |

`CONDUCTOR` and `PUBLISHER` deliberately retain the legacy `PRODUCER`/`COPYRIGHT` keys so the
batch editor round-trips with the existing single-song editor. The audit's semantic mismatch is a
documented follow-up, not part of this milestone. Cover/artwork is not a `TagEditField`.

Each field also declares an input kind (`TEXT`, `NUMBER`, `MULTILINE`) for the Compose control.
No format matrix is hard-coded in the field model.

`TagEditPlan` is a map of explicitly chosen operations:

```kotlin
sealed interface FieldOperation {
    data object Keep : FieldOperation
    data class Set(val value: String) : FieldOperation
    data object Clear : FieldOperation
}
```

`Keep` is the default for every field. `TagEditPlan.toPropertyOverrides()` emits only `Set` and
`Clear`:

- `Keep` -> no entry, so the writer's current-map merge leaves the file's value untouched;
- `Set(value)` -> `field.propertyKey to value.trim()`;
- `Clear` -> `field.propertyKey to null`.

A `Set` with a blank value is invalid and blocks save; `Clear` is the only removal operation. This
removes the old "blank means clear" ambiguity.

### Aggregation

For every selected song, resolve the `Song`, build `EditTarget.Content(song.id, song.uri,
song.data)`, and read it with `MetadataReader(uri, readPictures = false)`. Reads are sequential and
retain only normalized field values, not `MetadataReader`/metadata objects, so a large selection
does not hold every tag map in memory.

Display and aggregation use the existing `MetadataReader.merge(propertyKey)` value (multi-value
join with `"; "`), normalized by trimming and treating blank as null. Verification uses raw
property arrays with the same trim/join rule, because it must compare against the exact string
written to TagLib:

```text
trim each value -> drop blanks -> join non-blanks with "; " -> null if empty
```

For each field across all readable files:

- `Common(value)` when every readable file has the same non-null normalized value;
- `Absent` when every readable file has a null/blank value;
- `Varies` otherwise, including mixed absent/present values and two distinct values.

Unreadable files are excluded from the aggregate so they cannot manufacture a false `Varies`; they
are listed in the editor as "could not be read" and skipped by the writer. If every selected file
is unreadable, save is disabled.

### Writer

`MetadataWriter` keeps its existing behavior and gains a batch path. The private per-file function
becomes parameterised instead of reading the writer's shared `properties` field:

```kotlin
private fun writePropertyMap(
    fd: ParcelFileDescriptor,
    overrides: Map<String, String?>,
): Result
```

`writePropertyMap` continues to read the current property map, copy existing non-ReplayGain keys,
apply canonical ReplayGain values, remove null/blank overrides, trim set values and call
`TagLib.savePropertyMap`. `Keep` never enters `overrides`, so the current value survives exactly
through the existing merge.

```kotlin
suspend fun writeBatch(
    context: Context,
    contents: List<EditTarget.Content>,
    plan: BatchTagEditPlan,
    cover: CoverOperation = CoverOperation.Keep,
    journal: BatchWriteJournal? = null,
    albumIdBySongId: Map<Long, Long> = emptyMap(),
    onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    shouldContinue: () -> Boolean = { true },
): BatchWriteReport
```

`writeBatch`:

- validates that every `Set` value is non-blank;
- builds one override map from the plan and applies it to every file (one common value per field);
- loops sequentially, opens each URI with `"rw"`, and calls `writePropertyMap(fd, overrides)`;
- since the 2026-11-14 follow-up, optionally embeds the picked front cover or deletes every
  embedded picture (`CoverOperation`), verifies it by reading pictures back and folds the cover
  status into the file status; `Keep` still writes no cover;
- since the 2026-11-14 follow-up, optionally copies the original bytes to a `BatchWriteJournal`
  before the destructive open, restores the current file on hard failures and refreshes the
  album-art rows of the affected albums;
- notifies each changed URI and scans successful paths after the write loop, mapping scan callbacks
  back to files by path; a file whose scan callback never arrives is reported as a scan failure,
  not silently ignored;
- returns per-file/per-field results instead of the old aggregate `WriteResult.isSuccess`.

`write(context, target)` remains as a compatibility wrapper for the single-song/album/artist
activities. It preserves the current all-fields behavior (blank means clear) and does not gain the
new `Keep` semantics. No existing activity changes behavior.

`EditTarget` is not given a new `Type.MultipleSongs`; `writeBatch` accepts the content list directly,
so no exhaustive `when` over `EditTarget.Type` is disturbed.

### Read-back verification and failure reporting

`TagLib.savePropertyMap` returns only a boolean, so `writeBatch` verifies the persisted result:

1. Capture the before map used by `writePropertyMap` (or read it before saving).
2. After saving, re-open/read the URI and obtain `TagLib.getMetadata(...).propertyMap`.
3. For each operation in the plan:
   - `Set(value)`: normalized read-back equals `value.trim()` -> `Written`; key absent ->
     `Unsupported`; present but different -> `Mismatch`.
   - `Clear`: key absent or blank -> `Cleared`; otherwise `Mismatch`.
   - `Keep`: before and after normalized values equal (including both absent) -> `Kept`; otherwise
     `KeptChanged`.
4. A failed open, failed save or failed read-back marks the file failed and the requested fields
   `NotAttempted`/`FileFailed`.

Statuses are per file and per field. File status is derived:

- `Succeeded`: every requested `Set`/`Clear` is `Written`/`Cleared` and no `KeptChanged`;
- `Partial`: some requested fields succeeded and some failed, or a `KeptChanged` warning exists;
- `Failed`: open/save/read-back failed for the whole file;
- `Skipped`: the file could not be read before writing and was not attempted.

`KeptChanged` is a warning, not a requested-write failure, but it makes the file `Partial` because
the "untouched values survive" contract was not fully met.

Comparison is exact after trim and multi-value join; there is no case folding. Format-normalised
values may therefore appear as `Mismatch`/`KeptChanged`; that is honest reporting and is covered by
the deferred runtime format matrix.

The report is a first-class UI state, not a toast. The editor shows:

- counts of succeeded/partial/failed/skipped files;
- per-file rows with their status;
- per-field statuses for failed requested fields and `KeptChanged` warnings.

A blanket "changes saved successfully" message is shown only when every requested field on every
attempted file is `Written` or `Cleared` and there are no skipped/partial files.

### UI

`BatchTagEditorActivity` extends `AbsBaseActivity` and hosts a Compose screen (rule 12). The screen
has:

- a top app bar with the selection count and close/back;
- a loading state while files are read, with progress;
- one row per `TagEditField`, showing the label, `Common` value or `Varies`/`Absent`, and a
  tri-state control: `Keep` (default), `Set`, `Clear`;
- `Set` reveals an input prefilled with the common value (blank for `Varies`/`Absent`); `Set` with a
  blank value is invalid and blocks save;
- `Clear` is visually destructive; choosing it is the only way to remove a value;
- a save bar showing "N fields on M files"; disabled when no `Set`/`Clear` exists or any `Set` is
  blank;
- since the 2026-11-14 follow-up, a cover card with the current cover preview and
  `Keep`/`Replace`/`Remove` chips (Photo Picker for `Replace`), plus a cover row in the save
  summary and the result report;
- a saving state with progress; back navigation is blocked while saving so the user cannot
  intentionally cancel mid-batch;
- a result state with the report; the screen does not auto-finish, so the user can inspect failures.
  Since the follow-up it also offers "Undo changes" when the session journal still holds the
  originals, with an undoing progress state and a restored/failed summary.

The MusicBrainz action and the online cover download stay out of this screen. The existing field
labels are reused; only new English strings for `Keep`/`Set`/`Clear`, `Varies`, the cover actions,
undo and report labels are added to `values/strings.xml`.

### Permission and write request

On save, API 30+ requests `MediaStore.createWriteRequest(contentResolver, uris)` for all selected
(and readable) URIs, matching the existing editor. A denied request means no writes and a
permission-denied result. API < 30 writes directly. If the platform rejects the request payload
(for example because the selection is too large), the error is shown and no file is written. No new
permission is added to the manifest.

### Legacy album/artist guard

`AlbumTagEditorActivity` and `ArtistTagEditorActivity` remain entity-scoped, not multi-select. The
2026-11-07 follow-up applies the minimal guard:

- `LegacyTagAggregator` reads every selected file with `MetadataReader(uri, readPictures = false)`
  and returns common values plus `varies`; no first file is treated as the source of truth.
- Both editors populate their fields from the aggregate, show `tag_editor_varies`, and build
  `dirtyPropertyMap()` from values that differ from the aggregated initial value.
- Album destructive fields (`TRACK_TOTAL`, `DISC_NUMBER`, `DISC_TOTAL`) and artist `DISC_TOTAL`
  require a confirmation dialog when the entity contains more than one file.
- The old conditional `put(ARTIST, albumArtist)` is removed; explicit `ARTIST` changes still flow
  through the dirty map.
- Untouched fields are no longer sent in the map.

The guard stays in the legacy sources, but since the 2026-11-14 follow-up the album/artist
single-entity tag actions launch the batch editor instead, so the legacy editors are no longer
reachable from menus. They are still declared until runtime parity evidence allows removal. AA-07
remains partially open only for the runtime format matrix.

## Risks / Trade-offs

- **Silent field loss is the main risk.** Read-back verification is required; a `savePropertyMap`
  boolean is not sufficient.
- **Format normalisation can look like a mismatch.** Exact trim/join comparison is intentional for
  milestone 1; the deferred runtime matrix will show which formats need tolerance.
- **`Keep` preservation depends on TagLib's current-map merge.** `Keep` omits the key, so the
  existing merge path preserves it; `KeptChanged` catches formats that still drop it. Unknown or
  custom frames outside the 17 fields are not verified in this milestone.
- **Rollback and undo (since 2026-11-14).** Each file is copied to
  `cacheDir/tag-write-backup/<session>/` before the destructive open; a hard failure restores the
  current file, and the result screen can undo every file written in the session. Backups are
  session-scoped and deleted when the editor closes; `Mismatch`/`KeptChanged` stay `Partial`.
  Process death still loses the report and the undo, and a cache clear removes the backups. The
  full ADR-051 contract (sidecars, hashes, process-death recovery) is not implemented.
- **Cooperative stop, not in-file cancellation.** Back is blocked while saving; stop is observed at
  the next file boundary and remaining files are reported cancelled. Process death is still
  possible.
- **500-song cap.** Larger selections are rejected; chunked permission flows are a follow-up.
- **Legacy editors remain entity-wide.** The guard removes first-file reads, untouched-field
  broadcasts and the conditional ARTIST clear, but an explicit change still applies to every song
  in the entity. Per-file values/routing remain open; this milestone does not claim AA-07 complete.
- **Totals and `PRODUCER`/`COPYRIGHT` remain non-standard in some formats.** The batch editor
  reports what TagLib actually persisted; remapping is a separate change.

## Open contract questions

These are resolved for this draft but need product confirmation before implementation:

- **Field scope:** all 17 text fields plus the batch cover replace/delete added in the
  2026-11-14 follow-up. A smaller first set is possible but would make the batch editor less than
  the single-song editor.
- **`Keep` verification:** `KeptChanged` is a warning and makes the file `Partial`, not a hard
  failure of the requested change. If untouched values must be guaranteed, treat it as a failure.
- **Batch limit:** `MAX_BATCH_SONGS = 500` is a safety bound, not a product requirement; confirm the
  number or replace it with chunked permission requests later.
- **Equality:** trim plus the existing `MetadataReader.merge` multi-value semantics, no case
  folding. Format normalisation may still surface as `Mismatch`/`KeptChanged`.
- **Legacy guard:** the aggregation/dirty-map/destructive-confirmation guard is applied and the
  single-entity actions now route to the batch editor (2026-11-14). The legacy editors remain
  declared but unreachable; only the runtime format matrix keeps AA-07 partially open.
- **Reuse:** reuse the `MetadataReader`/`MetadataWriter` data path and field semantics. The
  single-target `TagEditorViewModel`/`AbsTagEditorActivity` XML form is not reused as-is.
- **Blank semantics:** blank text never means clear; `Clear` is the only removal operation.

## Verification

- Static probe plan: exact source assertions in `probe-plan.md` (baseline plus post-implementation).
- Deferred runtime probes: mixed-format field matrix, common/varies, Keep/Clear, no first-file
  overwrite, read-only/corrupt file, 100+ selection, process death, legacy album/artist
  aggregation/guard behavior (all files, varies, dirty-only writes, destructive confirmation, no
  ARTIST clear).
- Source gates: ktlint on the v2 files retains the same pre-existing findings (it is not a
  compiler or a clean result); `openspec validate add-batch-tag-editor --strict` is rerun for this
  sync. Build gates: JDK 21 `:app:assembleGithubDebug` and `:app:lintGithubDebug` remain deferred.
- No app source or Gradle build is run in this artifact-only turn.

## Validation state (2026-11-11)

Source acceptance is bounded and does not imply an app build or runtime PASS.

- **V2 source:** `MetadataWriter.kt` `6531A98D...`; the other nine files are byte-identical to v1;
  V2 manifest `93936871...`; fresh before-to-v2 diff 581 lines.
- **Independent reviews:** C Spec `4B88AD41...` PASS-qualified; M Standards `5864360E...`.
- **P1/P2:** explicit `kotlin.Result<WriteResultInternal>` result type; legacy `write()` reuses
  `scanFiles()` so null-URI scan failures count as failed and reduce `scanned`. Host compiles are
  context only.
- **Live integration:** receipt `D49D1A89...`; all 10 live files match the reviewed v2 hashes;
  `MetadataWriter` live `6531A98D...`; no Gradle/ktlint/build/device action; HEAD unchanged.
- **Still open:** app-wide Android compile/build/APK and lint; runtime format matrix
  (MP3/FLAC/M4A/OGG/Opus/WAV/AIFF/WMA/MKA/WV; APE/DSF/DFF limits); read-only/corrupt/process-death/
  100+ tests; legacy aggregation runtime tests.

## Follow-up 2026-11-14: batch covers, session undo and routing

Implemented by `openspec/changes/tag-editor-finish`; the parent `tasks.md` items 7.6, 7.7a, 8.6 and
9.6 are closed at source, while device gates 6.5, 8.7 and 9.5 stay deferred.

- **Cover model.** `CoverOperation` (`Keep`/`Replace(bitmap)`/`Delete`) lives in
  `core/model/tag/TagEditCover.kt`. `MetadataWriter.writeBatch` embeds the JPEG cover, deletes all
  pictures or leaves them untouched, verifies the result by reading pictures back
  (`CoverWriteStatus`), and folds the cover into the per-file status (`Unchanged` + cover change =
  `Succeeded`; text success + cover mismatch = `Partial`; cover-only mismatch = `Failed`). Affected
  albums get their MediaStore album-art row refreshed.
- **UI.** The Compose editor adds a cover card with the first readable file's cover preview,
  Photo Picker replace, remove and keep, a cover row in the save summary, a cover line in the report
  and a per-file cover status row. Only main English strings are added.
- **Journal.** `BatchWriteJournal` copies each file's original bytes to
  `cacheDir/tag-write-backup/<uuid>/<songId>.bak` before the destructive `"rw"` open (2 GiB per file,
  4 GiB per session; stale sessions pruned after seven days). A copy failure aborts that file
  (`backup_failed`) without modifying it. A hard failure (exception, rejected save, failed read-back)
  restores the current file; partial/mismatch results keep the fields that persisted.
- **Undo.** The result screen offers "Undo changes" while the session backups exist. Undo restores
  every changed file with `"rwt"`, verifies the restored size, notifies/scans, drops the restored
  albums' art rows and reports restored/failed counts. API 30+ re-requests
  `MediaStore.createWriteRequest` for the undo URI set. Backups are deleted when the editor closes.
- **Routing.** `Album.onAlbumMenu` and `Artist.onArtistMenu` launch
  `BatchTagEditorActivity` with the entity's song IDs through the shared `launchBatchTagEditor`
  helper; composer-artist entries stay unavailable. The legacy album/artist editors remain declared
  but are no longer reachable from menus.
- **Not covered.** Hash/sidecar validation, `noBackupFilesDir`, free-space reserve, process-death
  `RECOVERY_NEEDED`, fail-closed pre-open identity checks (ADR-051 remainder), cross-file
  common/varies cover detection, Deezer cover download, artist images and the runtime format matrix.
