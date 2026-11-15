# Batch tag editor

## Purpose

Add a true multi-song tag editor for the existing multi-select song menu: show common values and
per-field variation, default every field to `Keep`, apply explicit `Set`/`Clear` operations per
file through the existing `MetadataWriter` merge, and report persistence failures per file and per
field. This is the bounded first milestone of AA-07 plus the 2026-11-07 legacy aggregation/guard
follow-up and the 2026-11-08 per-file/cancellation follow-up. Batch covers, session undo/rollback,
in-place backup/restore and single-entity routing are added by the 2026-11-14 follow-up
(`openspec/changes/tag-editor-finish`); runtime evidence remains deferred.

## ADDED Requirements

### Requirement: Multi-song selection entry

The multi-select song menu SHALL expose a tag-editor action. Launching it SHALL pass distinct
selected song IDs and the editor SHALL load every selected song, not only the first. Duplicate IDs
SHALL be removed while preserving order. An empty selection or a selection over the batch limit
SHALL be rejected with a message and SHALL NOT write any file. The editor SHALL accept at least one
song; a one-song selection SHALL NOT replace the single-song editor.

#### Scenario: Select songs from different albums
- **WHEN** the user selects three songs from different albums and chooses the batch tag editor
- **THEN** all three files are read and represented in the editor

#### Scenario: Duplicate or missing songs
- **WHEN** the selection contains duplicate IDs or a song is deleted before the editor loads
- **THEN** duplicates are collapsed and the missing song is reported as skipped, not counted as
  written

#### Scenario: Empty or oversized selection
- **WHEN** the selection is empty or exceeds the batch limit
- **THEN** the editor is not started or shows a blocking limit message and no file is written

### Requirement: Common values and Varies

For every text field, the editor SHALL aggregate the values of all readable selected files and
SHALL display exactly one of: `Common(value)` when all readable files share the same normalized
value; `Absent` when all readable files have no value; or `Varies` otherwise, including mixed
absent/present values and two or more distinct values. Unreadable files SHALL be excluded from the
aggregate and reported separately. A file read failure SHALL NOT be hidden by a `Common` value.

#### Scenario: Different titles
- **WHEN** two selected files have different titles
- **THEN** the title field shows `Varies` and remains `Keep` by default

#### Scenario: Same genre
- **WHEN** every selected file has the same genre
- **THEN** the genre field shows that common value and remains `Keep` by default

#### Scenario: Mixed absence
- **WHEN** one file has lyrics and another has none
- **THEN** the lyrics field shows `Varies`, not `Common` with an empty value

#### Scenario: Unreadable file
- **WHEN** one selected file cannot be read
- **THEN** the other files are aggregated normally and the unreadable file is listed as skipped

### Requirement: Explicit per-field Keep, Set and Clear

Every field SHALL default to `Keep`. The user SHALL be able to choose `Set(value)` or `Clear`
explicitly per field. `Keep` SHALL NOT be included in the writer override, so each file's current
value survives. `Set` SHALL require a non-blank value; blank text SHALL NOT mean clear. `Clear`
SHALL be the only removal operation. Only fields explicitly set to `Set` or `Clear` SHALL be
written.

#### Scenario: Load without edits
- **WHEN** the editor finishes loading and the user saves without changing any field
- **THEN** no field is written and the save action is unavailable or reports no changes

#### Scenario: Change one field
- **WHEN** the title varies and the user sets only the genre
- **THEN** the plan contains only the genre and every file's title, artist, album and other fields
  remain unchanged

#### Scenario: Clear one field
- **WHEN** the user chooses `Clear` for the comment and leaves every other field at `Keep`
- **THEN** only the comment is removed on every attempted file and the other fields survive

#### Scenario: Blank Set
- **WHEN** the user chooses `Set` but leaves the value blank
- **THEN** save is blocked and the field is not treated as `Clear`

### Requirement: Reuse the existing writer and preserve untouched values

The batch save SHALL reuse `MetadataReader` field keys and `MetadataWriter`'s current-property-map
merge, ReplayGain handling and `TagLib.savePropertyMap` path. The writer SHALL apply the plan per
file, SHALL preserve every field not explicitly `Set` or `Clear`, and SHALL NOT write or delete
cover art in this milestone. The existing `MetadataWriter.write` path SHALL remain available for
the single-song, album and artist editors without changing their behavior.

#### Scenario: Different albums
- **WHEN** two selected files have different album values and the user sets only the genre
- **THEN** each file keeps its own album value after the write

#### Scenario: ReplayGain and cover
- **WHEN** a file has ReplayGain tags and embedded cover art and the user changes a text field
- **THEN** the ReplayGain values and cover art are not intentionally changed by the batch writer

#### Scenario: Legacy writer compatibility
- **WHEN** the single-song editor saves through the existing `write` path
- **THEN** its current behavior, including blank meaning clear, is unchanged

### Requirement: Per-file and per-field read-back reporting

After saving, the batch writer SHALL read the saved tags back and SHALL report a status for every
requested field on every attempted file: `Written`, `Cleared`, `Unsupported`, `Mismatch`,
`KeptChanged` or a file-level failure/skip. The UI SHALL show the per-file report and the failed
fields; it SHALL NOT show a blanket success message when any requested field did not persist. A
`Keep` value that changes or disappears SHALL be reported as `KeptChanged`.

#### Scenario: Unsupported field
- **WHEN** TagLib accepts the save but a requested field is absent on read-back
- **THEN** that field is reported as `Unsupported` and the file is not presented as fully saved

#### Scenario: One file fails
- **WHEN** one selected file is read-only or cannot be opened
- **THEN** the other files are still written and the failed file appears in the report

#### Scenario: All requested fields verified
- **WHEN** every requested field on every attempted file reads back as `Written` or `Cleared`
- **THEN** the UI may show a success summary

#### Scenario: Untouched value lost
- **WHEN** a field left at `Keep` has a different or missing value after the write
- **THEN** the file is reported as partial and the field is shown as `KeptChanged`

### Requirement: Permission and bounded execution

On API 30 and newer, the editor SHALL request write access for all selected URIs before writing.
Denial SHALL result in no writes and a permission-denied report. The editor SHALL enforce a bounded
selection size for the first milestone, SHALL write files sequentially with progress, and SHALL NOT
add a new permission. The user SHALL be able to stop at the next file boundary while saving, and
the result SHALL show the remaining files as cancelled. Back navigation SHALL remain blocked while
the write is active.

#### Scenario: Write permission denied
- **WHEN** the user denies the system write request
- **THEN** no file is written and the editor reports that permission was denied

#### Scenario: Large selection
- **WHEN** the selection exceeds the batch limit
- **THEN** the editor rejects it before reading or writing and explains the limit

#### Scenario: Progress
- **WHEN** the user confirms a batch save
- **THEN** the editor shows file progress and blocks back navigation until the write and report are
  complete

### Requirement: Per-file values

The batch editor SHALL keep the common `Keep`/`Set`/`Clear` operation per field and SHALL allow a
sparse per-song override for any field. A per-song `Keep` SHALL override a common `Set` for that
file only. A blank per-song `Set` SHALL block save. The writer and verifier SHALL use the effective
per-song plan, and the report SHALL identify the files that received overrides.

#### Scenario: Per-file title
- **WHEN** three selected songs receive different `TITLE` values and a common genre
- **THEN** each file gets its own title, the common genre is written to all attempted files, and
  untouched fields survive

#### Scenario: Per-file Keep against a common Set
- **WHEN** a common genre is set and one file is explicitly kept
- **THEN** only that file keeps its current genre and the other files receive the common value

#### Scenario: Blank per-file Set
- **WHEN** a per-file `Set` is blank
- **THEN** save is blocked and the blank value is not treated as `Clear`
### Requirement: Legacy album/artist aggregation guard

The existing album/artist editors SHALL read every selected file through `LegacyTagAggregator`,
SHALL show common values and `Varies`, and SHALL write only fields whose value differs from the
aggregated initial value. The album editor SHALL gate `TRACK_TOTAL`, `DISC_NUMBER` and
`DISC_TOTAL` writes behind confirmation when the entity contains more than one file. The artist
editor SHALL gate `DISC_TOTAL` writes the same way and SHALL NOT retain the old conditional
`put(ARTIST, albumArtist)` write that can clear the artist. Explicit `ARTIST` changes SHALL remain
possible through the dirty map. This guard SHALL NOT be presented as a full per-file batch editor;
single-entity routing and per-file values remain a follow-up.

#### Scenario: Legacy album with differing values
- **WHEN** the user opens the tag editor from a single album whose songs have different genres
- **THEN** the genre field shows `Varies`, only fields the user changes are written, and an
  untouched genre is not broadcast

#### Scenario: Legacy artist album-artist cleared
- **WHEN** the user clears the album-artist field in the artist editor
- **THEN** the artist value is not cleared as a side effect; only an explicit artist change updates
  `ARTIST`

#### Scenario: Legacy destructive totals
- **WHEN** the user changes a track total, disc number or disc total for an entity with more than
  one file
- **THEN** a confirmation explains the broadcast before any file is written

### Requirement: Scope and no schema/dependency/version changes

This milestone SHALL cover only the 17 text fields already exposed by the single-song editor.
Cover editing, online autofill, undo/rollback, new
dependencies, Room schema/entities/DAOs/migrations, app version changes and translated strings
SHALL NOT be part of it. Only main English strings MAY be added. The single-song editor SHALL
remain behaviorally unchanged. The album/artist editors SHALL apply the legacy aggregation guard
above; they SHALL NOT be presented as full per-file batch editors. AA-07 SHALL remain partially
open until a follow-up routes the single-entity actions to the batch editor and adds
covers/undo plus runtime evidence.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** no dependency, Room schema/version, app version, translated-string or new-permission
  change is present

#### Scenario: Legacy album/artist entry
- **WHEN** the user opens the tag editor from the single album or artist action
- **THEN** the editor aggregates every song's values, shows `Varies` where they differ, writes only
  changed fields, confirms destructive totals/disc changes, and does not clear `ARTIST` when
  `ALBUM_ARTIST` is empty

#### Scenario: Missing-features documentation
- **WHEN** the documentation is updated
- **THEN** `MISSING_FEATURES` describes the multi-select text-field editor and the legacy
  aggregation guard and per-file values as implemented and keeps cover batches, legacy routing,
  undo and the runtime format matrix as missing
