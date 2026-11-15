# Static probe plan: add-batch-tag-editor

Status: executed. The runnable probe is `probes/static-probe.ps1`; it reports **PASS 93 / FAIL 0**
against the implemented source, including the 2026-11-07 legacy aggregation/guard rebaseline. The
assertions below are the contract behind that script. These are static checks; runtime probes are
listed in section 4.

Use `rg -n` (or the OpenCode grep tool) from the repository root. Line numbers in the baseline
table are anchors from the 2026-11-06 audit and may shift; the patterns/symbols are the contract.

## 1. Baseline assertions (pre-implementation, confirmed by the audit)

These were true before implementation. They describe the gap the change fills and the legacy
behavior that must remain documented as residual. They are historical anchors, not current-tree
expectations; the implemented tree intentionally differs where the change adds the batch path.

| ID | File | Exact assertion | Expected now |
|---|---|---|---|
| B1 | `app/src/main/java/com/ssilencce/ui/screen/tageditor/TagEditorViewModel.kt` | `MetadataReader(target.first.uri, target.hasArtwork)` exists in `loadContent` | Present: single-file read |
| B2 | `app/src/main/java/com/ssilencce/data/local/MetadataWriter.kt` | `for (content in target.contents)` and `writePropertyMap(fd)` are present | Present: one map applied to every content |
| B3 | `app/src/main/java/com/ssilencce/data/local/MetadataWriter.kt` | `contents.isNotEmpty() && scanned == contents.size` is the `WriteResult.isSuccess` body | Present: no field verification |
| B4 | `app/src/main/java/com/ssilencce/ui/screen/tageditor/AlbumTagEditorActivity.kt` | `propertyMap` contains `ALBUM`, `ALBUM_ARTIST`, `PRODUCER`, `COPYRIGHT`, `GENRE`, `YEAR`, `TRACK_TOTAL`, `DISC_NUMBER`, `DISC_TOTAL` and exactly 9 entries | Present: album broadcast writes totals/disc number |
| B5 | `app/src/main/java/com/ssilencce/ui/screen/tageditor/ArtistTagEditorActivity.kt` | `put(MetadataReader.ARTIST, albumArtist)` is inside the conditional and `put(MetadataReader.DISC_TOTAL, ...)` is unconditional | Present: conditional artist write and disc-total broadcast |
| B6 | `app/src/main/res/menu/menu_media_selection.xml` | `@+id/action_tag_editor` is absent | Absent: no multi-select tag action |
| B7 | `app/src/main/java/com/ssilencce/ui/component/menu/MenuItemClickExt.kt` | `List<Song>.onSongsMenu` has no `R.id.action_tag_editor` branch | Absent: multi-select cannot launch a tag editor |
| B8 | `app/src/main/java/com/ssilencce/data/local/MetadataWriter.kt` | `currentProperties.forEach`, `newProperties.putAll(replayGainProperties)` and the `isNullOrBlank()` removal branch are present in `writePropertyMap` | Present: reusable merge/preserve path |

## 2. Post-implementation assertions

### 2.1 Field model and plan

| ID | File | Exact assertion | Pass condition |
|---|---|---|---|
| F1 | `app/src/main/java/com/ssilencce/core/model/tag/TagEditField.kt` | Enum entries are exactly `TITLE`, `ALBUM`, `ARTIST`, `ALBUM_ARTIST`, `COMPOSER`, `CONDUCTOR`, `PUBLISHER`, `GENRE`, `YEAR`, `TRACK_NUMBER`, `TRACK_TOTAL`, `DISC_NUMBER`, `DISC_TOTAL`, `LYRICS`, `LYRICIST`, `ARRANGER`, `COMMENT` | 17 entries, no cover/artwork entry |
| F2 | same | Property keys use `MetadataReader.*` constants; `CONDUCTOR` uses `MetadataReader.PRODUCER`, `PUBLISHER` uses `MetadataReader.COPYRIGHT`, `YEAR` uses `MetadataReader.YEAR`, totals use `MetadataReader.TRACK_TOTAL`/`DISC_TOTAL` | Each key resolves to the expected constant |
| F3 | same | `labelRes` uses existing `R.string.title`, `album`, `artist`, `album_artist`, `composer`, `conductor`, `publisher`, `genre`, `year`, `track`, `track_total`, `disc`, `disc_total`, `lyrics`, `lyricist`, `arranger`, `comment` | 17 labels, no new translated resources |
| F4 | `app/src/main/java/com/ssilencce/core/model/tag/TagEditPlan.kt` | `FieldOperation` has `Keep`, `Set(value)` and `Clear`; `Keep` is the default/empty plan | Three operations; default is Keep |
| F5 | same | `toPropertyOverrides()` omits `Keep`, trims `Set`, maps `Clear` to null | No Keep key reaches the writer |
| F6 | same | Blank `Set` is rejected/invalid | `Set("")` cannot produce an override |

### 2.2 Writer and verifier

| ID | File | Exact assertion | Pass condition |
|---|---|---|---|
| W1 | `app/src/main/java/com/ssilencce/data/local/MetadataWriter.kt` | `writePropertyMap` accepts an `overrides: Map<String, String?>` parameter | Per-file overrides, not shared mutable `properties` |
| W2 | same | `writeBatch(context, contents, plan, onProgress)` exists and calls `writePropertyMap(fd, overrides)` inside the content loop | Every file gets the plan |
| W3 | same | `writeBatch` contains no `picture(`, `pictureDeleted(`, `createPicture(` or `createPictureThumbFile(` call | No cover writes/deletes in batch |
| W4 | same | `write(context, target)` still exists and still uses the legacy all-fields map | Old editors remain compatible |
| W5 | same | The current-property merge, ReplayGain canonicalisation and `TagLib.savePropertyMap` call remain | Reuse, not duplication |
| W6 | `app/src/main/java/com/ssilencce/data/local/TagWriteVerifier.kt` or `MetadataWriter.kt` | After save, `TagLib.getMetadata` is called again and the saved property map is compared | Read-back verification exists |
| W7 | same | Statuses include `Written`, `Cleared`, `Unsupported`, `Mismatch`, `Kept` and `KeptChanged` | Per-file/per-field reporting exists |
| W8 | same | `Set` absent -> `Unsupported`; `Set` different -> `Mismatch`; `Clear` still present -> `Mismatch`; `Keep` before/after differ -> `KeptChanged` | Failure classes are distinguishable |
| W9 | same | File status is `Succeeded`/`Partial`/`Failed`/`Skipped` and is not derived from scan count alone | No blanket success from `savePropertyMap(true)` |

### 2.3 ViewModel and aggregation

| ID | File | Exact assertion | Pass condition |
|---|---|---|---|
| V1 | `app/src/main/java/com/ssilencce/ui/screen/tageditor/BatchTagEditorViewModel.kt` | Class exists and accepts distinct song IDs (`LongArray`/`List<Long>`) | True multi-song target |
| V2 | same | No `target.first`, `contents.first()` or equivalent single-file shortcut | Reads every selected song |
| V3 | same | Resolves IDs through `Repository.songById` and marks missing/deleted songs skipped | No crash or silent drop |
| V4 | same | Uses `MetadataReader(uri, readPictures = false)` for every readable song | Reuses reader, no cover memory |
| V5 | same | Aggregates with the same `MetadataReader.merge` value shown in the UI; represents `Common(value)`, `Varies`, `Absent`; mixed absent/present -> `Varies` | Common/varies contract matches the displayed value |
| V6 | same | Enforces `MAX_BATCH_SONGS` and rejects empty/oversized selections before writing | Bounded execution |
| V7 | same | Builds `EditTarget.Content(song.id, song.uri, song.data)` without adding `EditTarget.Type.MultipleSongs` | No enum/exhaustive-when break |

### 2.4 Compose UI

| ID | File | Exact assertion | Pass condition |
|---|---|---|---|
| U1 | `app/src/main/java/com/ssilencce/ui/screen/tageditor/BatchTagEditorScreen.kt` | Compose screen renders one row per `TagEditField` with `Keep` default and explicit `Set`/`Clear` controls | Explicit per-field intent |
| U2 | same | Common value, `Varies` and `Absent` are visible; `Set` prefill uses the common value or blank | Common values plus varies |
| U3 | same | `Set` with blank value blocks save; `Clear` is separate | Blank is not clear |
| U4 | same | Save summary counts `Set`/`Clear` fields and files; progress is shown during write | No one-shot toast |
| U5 | same | Result state lists per-file and per-field failures; no unconditional `changes_saved_successfully` | Honest reporting |
| U6 | same | No cover picker/preview, MusicBrainz or per-file value controls | Milestone scope |
| U7 | `app/src/main/java/com/ssilencce/ui/screen/tageditor/BatchTagEditorActivity.kt` | Extends `AbsBaseActivity`, hosts the Compose screen, requests `MediaStore.createWriteRequest` on API 30+ only on save | Permission flow matches existing editor |
| U8 | same | Back navigation is blocked while saving; report remains visible after completion | No intentional mid-write cancel |

### 2.5 Entry point and wiring

| ID | File | Exact assertion | Pass condition |
|---|---|---|---|
| E1 | `app/src/main/res/menu/menu_media_selection.xml` | `@+id/action_tag_editor` exists, reusing `@string/action_tag_editor` | Multi-select action present |
| E2 | `app/src/main/java/com/ssilencce/ui/component/menu/MenuItemClickExt.kt` | `List<Song>.onSongsMenu` has an `R.id.action_tag_editor` branch | Action handled |
| E3 | same | Branch launches `BatchTagEditorActivity` with `EXTRA_SONG_IDS` as a `LongArray`; no `List<Song>`/`List<EditTarget.Content>` extra | Bounded binder payload |
| E4 | same | Branch dedupes IDs and enforces the batch limit before launch | Same bounds as the editor |
| E5 | `app/src/main/AndroidManifest.xml` | `BatchTagEditorActivity` is declared with `@string/action_tag_editor`; no new `uses-permission` | Activity registered, no permission |
| E6 | `app/src/main/java/com/ssilencce/MainModule.kt` | `viewModel { (ids: LongArray) -> BatchTagEditorViewModel(...) }` or equivalent Koin registration | DI wired |
| E7 | same | Existing `TagEditorViewModel` registration remains | Single editor unaffected |

### 2.6 Scope and legacy guard

| ID | Check | Pass condition |
|---|---|---|
| S1 | `git diff --name-only -- gradle/libs.versions.toml app/build.gradle.kts` | Empty (no dependency/version change) |
| S2 | `git diff -U0 app/build.gradle.kts` filtered for `versionCode`/`versionName` | Empty |
| S3 | `SsilencceDatabase.kt` diff has no `version =` change; `git status --porcelain -- app/src/main/java/com/ssilencce/data/local/room` has no `??` entries | No Room schema version change and no new Room files (pre-existing AA-05/AA-06 edits may still appear as modified tracked files) |
| S4 | `git diff --name-only -- "app/src/main/res/values-*"` | Empty (no translated strings) |
| S5 | `git diff -U0 app/src/main/AndroidManifest.xml` filtered for `uses-permission` | Empty (no new permission) |
| S6a | `LegacyTagAggregator.kt` | Aggregator exists and reads every selected URI with `MetadataReader(uri, readPictures = false)`; no first-file shortcut |
| S6b | `AlbumTagEditorActivity.kt` | Calls `aggregateTagValues(viewModel.uris, ALBUM_FIELDS)` and returns `dirtyPropertyMap()` from `propertyMap` with per-field `addIfChanged` |
| S6c | `ArtistTagEditorActivity.kt` | Calls `aggregateTagValues(viewModel.uris, ARTIST_FIELDS)` and returns `dirtyPropertyMap()` from `propertyMap` with per-field `addIfChanged` |
| S6d | Both legacy activities | No `target.first` read; fields are populated from the aggregate and `tag_editor_varies` is shown via `setVariesHint` |
| S6e | Legacy activities and aggregator | Legacy editors use `TagEditField` property keys, not raw `MetadataReader.*` maps; the aggregator reads `field.propertyKey` |
| S6f | `AlbumTagEditorActivity.kt` | `DESTRUCTIVE_FIELDS` includes `TagEditField.TRACK_TOTAL`, `DISC_NUMBER`, `DISC_TOTAL` and the destructive confirmation string is present |
| S6g | `ArtistTagEditorActivity.kt` | `DESTRUCTIVE_FIELDS = setOf(TagEditField.DISC_TOTAL)` and the destructive confirmation string is present |
| S6h | `ArtistTagEditorActivity.kt` | The old `put(MetadataReader.ARTIST, albumArtist)` conditional and `isSingleSong` are absent; `addIfChanged(TagEditField.ARTIST)` retains explicit rename |
| S6i | `MenuItemClickExt.kt`, `BatchTagEditorActivity.kt`, `BatchTagEditorViewModel.kt` | Single-entity actions still launch the guarded legacy editors; the multi-select action still launches the batch editor with `EXTRA_SONG_IDS` and resolves IDs through `Repository.songById` |
| S7 | `docs/MISSING_FEATURES.md` | States that the multi-select text editor exists and that cover batches, per-file values, legacy routing (not the applied guard) and the runtime format matrix remain missing |
| S8 | `docs/FEATURES.md` and `docs/CODE_MAP.md` | New batch editor and new files are documented |

> **2026-11-07 rebaseline:** the original S6 row asserted that the legacy album/artist editors
> were unchanged (first-file read, nine-field/conditional broadcast). That is no longer the
> intended contract. S6a–S6i assert the new guard instead: aggregate every file, show `Varies`,
> write only changed fields, confirm destructive totals/disc writes, remove the conditional
> `ARTIST` clear, and keep the multi-select batch path as the safe multi-song route. Single-entity
> routing/per-file values remain open follow-up gaps.

## 3. Exact command sketch

Run the packaged probe first:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File openspec/changes/add-batch-tag-editor/probes/static-probe.ps1
```

The individual checks are:

```bash
# Entry point and wiring
rg -n "@\+id/action_tag_editor" app/src/main/res/menu/menu_media_selection.xml
rg -n "R\.id\.action_tag_editor|BatchTagEditorActivity|EXTRA_SONG_IDS" \
  app/src/main/java/com/ssilencce/ui/component/menu/MenuItemClickExt.kt
rg -n "BatchTagEditorActivity" app/src/main/AndroidManifest.xml
rg -n "BatchTagEditorViewModel" app/src/main/java/com/ssilencce/MainModule.kt

# Field model and plan
rg -n "enum class TagEditField|MetadataReader\.(TITLE|ALBUM|ARTIST|ALBUM_ARTIST|COMPOSER|PRODUCER|COPYRIGHT|GENRE|YEAR|TRACK_NUMBER|TRACK_TOTAL|DISC_NUMBER|DISC_TOTAL|LYRICS|LYRICIST|ARRANGER|COMMENT)" \
  app/src/main/java/com/ssilencce/core/model/tag/TagEditField.kt
rg -n "sealed (class|interface) FieldOperation|data object Keep|data class Set|data object Clear|toPropertyOverrides" \
  app/src/main/java/com/ssilencce/core/model/tag/TagEditPlan.kt

# Writer and verifier
rg -n "fun writeBatch|writePropertyMap\(fd, overrides\)|TagLib\.getMetadata" \
  app/src/main/java/com/ssilencce/data/local/MetadataWriter.kt \
  app/src/main/java/com/ssilencce/data/local/TagWriteVerifier.kt

# Batch UI must not touch covers
rg -n "picture\(|pictureDeleted\(|createPicture|createPictureThumbFile" \
  app/src/main/java/com/ssilencce/ui/screen/tageditor/Batch*.kt \
  app/src/main/java/com/ssilencce/data/local/TagWriteVerifier.kt
# Expected: no matches in the batch path.

# Scope guards
git diff --name-only -- gradle/libs.versions.toml app/build.gradle.kts
git diff -U0 -- app/src/main/java/com/ssilencce/core/SsilencceDatabase.kt | rg "version\s*="
git status --porcelain -- app/src/main/java/com/ssilencce/data/local/room | rg "^\?\?"
git diff --name-only -- "app/src/main/res/values-*"
git diff -U0 app/src/main/AndroidManifest.xml | rg "uses-permission"
```

If `rg` is unavailable, use the equivalent PowerShell `Select-String -Pattern` checks. A check
passes only when the expected match/absence is observed in the stated file, not merely somewhere in
the repository.

## 4. Runtime probes (deferred, not static)

Run these after implementation with real fixture files; do not claim AA-07 closed from static
checks alone.

1. **Field matrix:** MP3, FLAC, M4A, OGG, Opus, WAV, APE, WMA, DSF and DFF samples; set every field
   through the batch editor; read back with `ffprobe`/`mediainfo` and TagLib; record standard,
   freeform, non-standard and dropped fields.
2. **Common/varies:** two songs with different titles/genres/album artists; confirm `Varies` and
   default `Keep`; set one field and confirm the others survive.
3. **Keep vs clear:** set one field to `Clear` and leave another `Keep`; confirm only the selected
   field changes on every file.
4. **No first-file overwrite:** select songs from different albums; confirm no value is silently
   taken from the first song.
5. **Partial failure:** make one file read-only/corrupt; confirm the other files finish and the
   report lists the failed file and fields.
6. **Unsupported field:** use a format that drops a requested field (for example a non-standard total
   on M4A/ID3); confirm `Unsupported`/`Mismatch` is shown instead of success.
7. **Large selection:** 100+ songs; verify progress, the selection cap, permission request and
   report size.
8. **Process death:** kill the activity/process mid-write; confirm no false success is reported and
   the next read reflects what is actually on disk.
9. **Legacy editors:** multi-disc album and artist catalogue; confirm all files are aggregated,
   `Varies` is visible, only changed fields are written, destructive totals/disc writes are
   confirmation-gated, and the artist `ARTIST` value is never cleared by an empty album artist.
   Routing/per-file values remain open.
10. **Cover/ReplayGain:** confirm batch text edits do not intentionally change embedded covers or
    ReplayGain values.
