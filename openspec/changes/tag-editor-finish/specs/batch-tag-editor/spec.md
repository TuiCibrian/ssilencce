# Batch tag editor — covers, undo and routing

## ADDED Requirements

### Requirement: Batch cover replace and delete

The batch editor SHALL show the current embedded cover of the first readable file and SHALL let the
user keep it (default), replace it with a Photo Picker image, or remove the embedded cover from
every written file. The writer SHALL verify the persisted cover by reading the pictures back and
SHALL report `Written`, `Deleted`, `Mismatch` or `NotAttempted` per file. A cover-only change SHALL
count as a change; a cover mismatch SHALL be reported per file and SHALL NOT be reported as full
success. The MediaStore album-art cache rows of the affected albums SHALL be refreshed after the
write.

#### Scenario: Replace cover on a selection
- **WHEN** the user picks an image and saves
- **THEN** every written file has the picked image embedded and the report shows `Cover written`
  for it

#### Scenario: Remove cover
- **WHEN** the user chooses Remove and saves
- **THEN** the embedded pictures of every written file are deleted and the report shows
  `Cover removed`

#### Scenario: Format without cover support
- **WHEN** a file rejects or loses the cover write
- **THEN** that file is reported with `Cover did not persist`, the run is not a full success, and
  the text-field results stay intact

#### Scenario: Keep the cover
- **WHEN** the user leaves the cover on Keep
- **THEN** no picture call is made for any file

### Requirement: In-place backup and failure rollback

Before a destructive write, the editor SHALL copy each file's original bytes to a session directory
under the app cache and SHALL leave the file untouched when the copy cannot be verified or exceeds
the caps (2 GiB per file, 4 GiB per session). When the write fails (exception, rejected save or
failed read-back) the current file SHALL be restored and the file SHALL be reported `Failed` with
the rollback outcome. Successfully written files before a failure SHALL remain changed.

#### Scenario: Save fails after the writer opened the file
- **WHEN** the tag writer rejects the save
- **THEN** the original bytes are restored and the file is reported `The write failed and the
  original file was restored.`

#### Scenario: Backup cannot be made
- **WHEN** the original bytes cannot be copied or exceed the cap
- **THEN** that file is not modified and is reported as failed with the backup message

#### Scenario: Restore fails
- **WHEN** the original bytes cannot be written back
- **THEN** the file is reported `The write failed and the original file could not be restored.`
  and no success is claimed

### Requirement: Session undo

After a write, the editor SHALL offer an undo action while the session backups exist. Undo SHALL
restore every file that was changed in the session, SHALL verify the restored size, SHALL rescan the
restored files, SHALL refresh the album-art rows of the restored albums, and SHALL report the
restored and failed counts. Backups SHALL be deleted when the editor session ends; undo SHALL NOT
survive the session.

#### Scenario: Undo a completed save
- **WHEN** the user taps Undo on the result screen
- **THEN** every changed file is restored to its original bytes and the screen shows the restored
  and failed counts

#### Scenario: Undo after the editor closed
- **WHEN** the editor session has ended
- **THEN** the backups are gone and undo is not offered

#### Scenario: Undo with partial restore failure
- **WHEN** one file cannot be restored
- **THEN** the remaining files are still restored and the failure is reported without claiming
  full success

### Requirement: Single-entity album and artist routing

The single-entity album and artist tag actions SHALL open the batch editor with the entity's song
IDs, deduplicated through the shared launch helper and bounded by the batch limit. Composer entries
SHALL keep the tag action unavailable. The legacy album/artist editors SHALL remain declared but
unreachable from menus.

#### Scenario: Tag an album from album detail
- **WHEN** the user chooses Tag editor on an album
- **THEN** the batch editor opens with that album's songs and their common values

#### Scenario: Tag an artist from the artist list
- **WHEN** the user chooses Tag editor on an artist
- **THEN** the batch editor opens with the artist's deduplicated songs

#### Scenario: Composer entry
- **WHEN** the artist entry is a composer
- **THEN** the tag action remains unavailable
