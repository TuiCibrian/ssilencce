# Tasks: Batch tag editor (AA-07, bounded first milestone)

Implementation is complete in source and the reviewed v2 10-file set is live-integrated. No Gradle
build, APK or device run was performed; the build and runtime probes below remain intentionally
unchecked, and AA-07 is not complete.

2026-11-14: `openspec/changes/tag-editor-finish` implements batch covers, session undo/rollback,
in-place backup/restore and single-entity routing (7.6, 7.7a, 8.6, 9.6). Device/runtime gates 6.5,
8.7 and 9.5 stay deferred.

## 1. Field model and plan

- [x] 1.1 Add `core/model/tag/TagEditField.kt` with exactly the 17 text fields from the single-song
  editor, using `MetadataReader` property keys and existing label resources; retain the legacy
  `CONDUCTOR` -> `PRODUCER` and `PUBLISHER` -> `COPYRIGHT` mapping; do not add a cover field.
- [x] 1.2 Add input-kind metadata (`TEXT`, `NUMBER`, `MULTILINE`) for the Compose controls.
- [x] 1.3 Add `core/model/tag/TagEditPlan.kt` with `FieldOperation.Keep`/`Set`/`Clear`, `Keep` as
  the default, and `toPropertyOverrides()` that omits `Keep`, trims `Set` values and maps `Clear` to
  null.
- [x] 1.4 Reject blank `Set` values in plan validation; `Clear` is the only removal operation.
- [x] 1.5 Add per-file/per-field report types (`BatchWriteReport`, `FileWriteReport`,
  `FieldWriteStatus`, `FileWriteStatus`) without Room or dependency changes.

## 2. Writer and verifier

- [x] 2.1 Refactor `MetadataWriter.writePropertyMap(fd, overrides)` so the per-file override map is
  a parameter; preserve the current-property-map merge, ReplayGain canonicalisation, blank removal
  and `TagLib.savePropertyMap` call.
- [x] 2.2 Add `MetadataWriter.writeBatch(context, contents, plan, onProgress)` that loops
  sequentially, applies only `Set`/`Clear`, notifies each URI, scans successful paths and maps scan
  callbacks by path.
- [x] 2.3 Keep `write(context, target)` as a compatibility wrapper with its current all-fields
  behavior; do not change the single-song, album or artist editors in this milestone.
- [x] 2.4 Ensure `writeBatch` never calls `picture`, `pictureDeleted`, `createPicture` or
  `createPictureThumbFile` and never creates/deletes album art.
- [x] 2.5 Add read-back verification (for example `data/local/TagWriteVerifier.kt` or a
  `MetadataWriter` helper): `Set` -> `Written`/`Unsupported`/`Mismatch`, `Clear` ->
  `Cleared`/`Mismatch`, `Keep` -> `Kept`/`KeptChanged`, with file-level failed/skipped status.
- [x] 2.6 Normalize comparisons by trimming and joining multi-values with `"; "`; do not case-fold.
- [x] 2.7 No new dependency, Room entity/DAO/schema/migration, app version or permission.

## 3. ViewModel and Compose UI

- [x] 3.1 Add `BatchTagEditorViewModel` taking distinct song IDs, resolving them through
  `Repository.songById`, enforcing the batch limit and reporting missing/deleted songs as skipped.
- [x] 3.2 Read every selected file with `MetadataReader(uri, readPictures = false)` sequentially;
  retain only normalized field values; exclude unreadable files from aggregation and list them.
- [x] 3.3 Aggregate each field as `Common(value)`, `Varies` or `Absent`; mixed absent/present is
  `Varies`.
- [x] 3.4 Add a Compose `BatchTagEditorScreen` with one row per field, `Keep` default, explicit
  `Set`/`Clear`, common/varies/absent display, numeric/multiline input, save summary, progress and
  the per-file/per-field result report.
- [x] 3.5 Add `BatchTagEditorActivity` extending `AbsBaseActivity`, hosting the Compose screen,
  requesting `MediaStore.createWriteRequest` on API 30+ at save time, and writing nothing when the
  request is denied.
- [x] 3.6 Block back navigation while saving; retain the report after completion instead of
  auto-finishing.
- [x] 3.7 Add only main English strings to `values/strings.xml`; do not touch translated
  `values-*` files.
- [x] 3.8 Do not add cover, MusicBrainz/autofill or per-file value controls to this screen.

## 4. Entry point and wiring

- [x] 4.1 Add `action_tag_editor` to `res/menu/menu_media_selection.xml`, reusing
  `@string/action_tag_editor` and the existing edit icon.
- [x] 4.2 Handle `R.id.action_tag_editor` in `List<Song>.onSongsMenu`: dedupe IDs, enforce the
  batch limit and launch `BatchTagEditorActivity` with `EXTRA_SONG_IDS: LongArray`; do not pass a
  list of `Song` or `EditTarget.Content` objects.
- [x] 4.3 Declare `BatchTagEditorActivity` in `AndroidManifest.xml` with the existing tag-editor
  label; add no permission.
- [x] 4.4 Register `BatchTagEditorViewModel` in `MainModule.kt` with Koin.
- [x] 4.5 Keep the single-entity handlers on the legacy album/artist editors, but apply the
  follow-up guard: both read every selected file through `LegacyTagAggregator`, show `Varies`,
  write only dirty fields, confirm destructive totals/disc writes, and never write the old
  conditional empty `ARTIST`. Multi-selected albums/artists continue to reach the batch path
  through `List<Album>.onAlbumsMenu`/`List<Artist>.onArtistsMenu` -> `onSongsMenu`.

## 5. Documentation

- [x] 5.1 Record the applied legacy aggregation/guard in this change's proposal/design/spec and
  keep the remaining routing/per-file residual explicit; AA-07 remains partially open until the
  single-entity actions are routed to the batch editor (or the legacy editors are removed) and
  per-file values/covers/undo/runtime evidence are complete.
- [x] 5.2 Update `docs/FEATURES.md` with the multi-select text-field batch editor.
- [x] 5.3 Update `docs/CODE_MAP.md` for every new or significantly changed source file.
- [x] 5.4 Add an ADR only if implementation changes the writer/data-layer contract beyond this
  design; otherwise the design is the contract and no ADR is required.
- [x] 5.5 Keep `MISSING_FEATURES` honest: cover batches, per-file values in the legacy entity
  editors, legacy routing (the aggregation/dirty-map/destructive-confirmation guard is applied),
  the runtime format matrix, undo/rollback and in-place write backup/restore remain missing.

## 6. Verification

- [x] 6.1 Run every static assertion in `probe-plan.md` and attach the output:
  `probes/static-probe.ps1` **PASS 121 / FAIL 0** after the 2026-11-08 follow-up checks. The 2026-11-07 rebaseline replaces the old
  “legacy unchanged” S6 row with S6a–S6i for aggregation, dirty-field writes, destructive
  confirmation, ARTIST-clear removal and batch-path preservation.
- [x] 6.2 Run ktlint and `git diff --check`: ktlint PASS; `git diff --check` PASS.
- [x] 6.3 Run `openspec validate add-batch-tag-editor --strict`: valid.
- [ ] 6.4 Run JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug` only when explicitly requested;
  no build is run in this turn.
- [ ] 6.5 Runtime probes: MP3/FLAC/M4A/OGG/Opus/WAV/APE field matrix; common/varies; Keep/Clear; no
  first-file overwrite; read-only/corrupt file; 100+ selection; process death; legacy album/artist
  aggregation/guard behavior (all files, varies, dirty-only writes, destructive confirmation, no
  ARTIST clear); cover and ReplayGain untouched. Not run in this turn. **Deferred: device gate.**

## 7. Legacy aggregation/guard follow-up (2026-11-07)

- [x] 7.1 Add `LegacyTagAggregator` and aggregate every selected file in the album/artist editors.
- [x] 7.2 Write only fields that differ from the aggregated initial values.
- [x] 7.3 Show `Varies` hints and gate destructive totals/disc writes behind confirmation.
- [x] 7.4 Remove the conditional `ARTIST` clear while retaining explicit `ARTIST` rename through
  the dirty map.
- [x] 7.5 Keep the multi-select batch editor as the safe multi-song path; single-entity actions
  continue to use the guarded legacy editors.
- [x] 7.6 Route single-entity album/artist actions to the batch editor. Both `Album.onAlbumMenu`
  and `Artist.onArtistMenu` now launch `BatchTagEditorActivity` with the entity's song IDs through the
  shared `launchBatchTagEditor` helper (dedupe, id filter, 500-song limit); composer-artist entries
  stay unavailable. The legacy editors remain declared but are no longer reachable from menus
  (`openspec/changes/tag-editor-finish`).
- [x] 7.7a Per-file values (section 8) and batch covers + session undo (`tag-editor-finish`) are
  delivered: cover replace/delete with read-back status, `BatchWriteJournal` original-bytes backup,
  current-file rollback on hard failure, session undo with MediaStore rescan and album-art refresh.
- [ ] 7.7b Runtime legacy tests L1–L7 — **deferred: device gate** (see 6.5/8.7/9.5).

## 8. Source follow-up 2026-11-08: per-file values and cooperative cancellation

- [x] 8.1 Add `BatchTagEditPlan` with sparse per-song overrides; a per-song `Keep` overrides a common
  `Set`, and the effective plan is validated for blank `Set` values.
- [x] 8.2 Make `MetadataWriter.writeBatch` use the effective per-file plan, report `UNCHANGED` for
  files with no operations, and emit `CANCELLED` rows when the user stops or the caller cancels.
- [x] 8.3 Add `prepareJob`/`writeJob`, `ensureActive()` at file boundaries, `cancelPrepare()`,
  `stopWriting()` and a pre-save skipped-file list to `BatchTagEditorViewModel`.
- [x] 8.4 Add the per-file Compose dialog, stop action, skipped-file disclosure, aggregate preview
  cap, mapped error text and destructive Clear chip.
- [x] 8.5 Re-run `probes/static-probe.ps1` (**PASS 121 / FAIL 0**), ktlint and `git diff --check`.
- [x] 8.6 Batch covers, session undo, single-entity routing to the batch editor and in-place write
  backup/restore are delivered by `openspec/changes/tag-editor-finish`: `CoverOperation`
  replace/delete with read-back status and album-art refresh, `BatchWriteJournal` cache-dir backup
  with fail-closed copies and current-file rollback, session undo from the result screen, and
  album/artist single-entity actions routed to the batch editor.
- [ ] 8.7 Runtime tests for the legacy aggregation guard, per-file values, stop/cancel, read-only
  files and ReplayGain/cover preservation remain unrun. **Deferred: device gate.**

## 9. Independent review and live integration (2026-11-11)

- [x] 9.1 C Spec re-review of v2: `4B88AD41...` PASS-qualified; P1 result type and P2 legacy null
  scan closed at source.
- [x] 9.2 M Standards `5864360E...`; accepted for bounded source.
- [x] 9.3 Exact 10-file live integration: receipt `D49D1A89...`; all live hashes match V2 manifest
  `93936871...`; `MetadataWriter.kt` `6531A98D...`; before/after copies retained; no Gradle/hook.
- [ ] 9.4 App-wide Android compile/build/APK and lint gate.
- [ ] 9.5 Runtime safety/format matrix, read-only/corrupt/process-death/100+ and legacy aggregation
  tests. **Deferred: device gate.**
- [x] 9.6 Batch covers, session undo/rollback, single-entity routing and in-place write
  backup/restore delivered at source by `openspec/changes/tag-editor-finish`; runtime evidence is
  tracked by 9.5.
- [ ] 9.7 Archive AA-07 only after 9.4-9.6 are implemented or explicitly accepted.
