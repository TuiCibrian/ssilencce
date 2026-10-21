## Purpose

Lets users export and selectively restore their app data (playlists, settings, synced lyrics cache, custom artist images) as a ZIP backup file, with honest success/failure reporting and bounded resource use on untrusted archives.

## ADDED Requirements

### Requirement: Backup archive contains all selected data

The system SHALL create a single ZIP archive containing the settings file, non-empty playlists as M3U entries, the synced-lyrics cache as a JSON entry when present, and custom artist images with their preference file when present. Entries written after the lyrics entry MUST remain part of the archive.

#### Scenario: Lyrics entry is followed by artist images
- **WHEN** a backup is created while the lyrics cache and custom artist images are both populated
- **THEN** the archive contains the lyrics JSON entry and every artist image entry, and the archive is readable end to end

#### Scenario: No lyrics cached
- **WHEN** a backup is created while the lyrics cache is empty
- **THEN** the archive is created successfully and contains the remaining selected entries

### Requirement: Backup creation failures are reported and contained

The system SHALL report a backup failure to the user and MUST NOT crash or leave the app in a broken state when any step of archive creation fails. Backup creation SHALL honour coroutine cancellation.

#### Scenario: Entry cannot be collected
- **WHEN** collecting an archive entry fails during backup creation
- **THEN** the user sees a failure message and the app keeps running

### Requirement: Restore does not trust declared ZIP sizes

The system SHALL evaluate ZIP entries only by the bytes actually read; entries whose declared size is unknown (‑1) MUST be accepted. The system MUST reject an archive whose lyrics entry exceeds 10 MB of actual bytes, any single entry exceeds 100 MB of actual bytes, the total actual bytes exceed 100 MB, or the archive contains more than 1000 entries.

#### Scenario: Backup produced by the app restores
- **WHEN** a backup created by the app is restored
- **THEN** restore completes without a size-related rejection

#### Scenario: Oversized lyrics entry
- **WHEN** an archive contains a lyrics entry larger than 10 MB
- **THEN** restore aborts with a failure message and no lyrics are imported

#### Scenario: Too many entries
- **WHEN** an archive contains more than 1000 entries
- **THEN** restore aborts with a failure message

### Requirement: Restore is selective and honest

The system SHALL let the user choose which content types to restore, starting with nothing selected, and SHALL process only the selected types. Restore failures SHALL show a failure message; successful restores SHALL show a success message. Restore SHALL honour coroutine cancellation.

#### Scenario: Nothing selected
- **WHEN** the user confirms restore with no content type selected
- **THEN** no data is changed and the operation reports that nothing was restored instead of announcing success

#### Scenario: Empty or non-backup archive
- **WHEN** the selected file is empty, is not a ZIP archive, or contains no entry recognized as backup content
- **THEN** restore reports a failure or a no-restorable-content warning and never announces success

#### Scenario: Selected content matched but nothing imported
- **WHEN** the archive contains a selected content entry but the entry yields no imported rows or files
- **THEN** restore reports that nothing was restored instead of announcing success

#### Scenario: Lyrics and artist images selected
- **WHEN** the user selects synced lyrics and artist images for an archive containing them
- **THEN** the lyrics cache rows and the artist image files are restored

### Requirement: Restore rejects unsafe entry names

The system MUST NOT write files whose entry basename is empty, `.`, `..`, or contains path separators when restoring artist images or their preference file.

#### Scenario: Traversal-style entry name
- **WHEN** an archive contains an artist-image entry whose basename is `..`
- **THEN** restore aborts with a failure message and no file is written outside the intended directory

### Requirement: Queue contents survive app restarts

The system SHALL persist the playback queue with one row per queue position, and SHALL restore the same sequence of media items, preserving songs that appear more than once. Persisted current position, repeat and shuffle state MUST remain compatible with existing saved playback state.

#### Scenario: Repeated song round-trip
- **WHEN** the queue contains the same song at two positions and the app process is restarted
- **THEN** the restored queue contains both occurrences in their original order

#### Scenario: Legacy database upgrade
- **WHEN** an existing v6 database is opened by the new version
- **THEN** the saved queue rows and their order are preserved and playback resumes with the same sequence

### Requirement: Release artifacts are never silently debug-signed

The system MUST refuse to build a Release artifact (`assemble*Release`, `bundle*Release`, `install*Release`) when no release keystore configuration exists, unless the developer explicitly opts in to a debug-signed local build. Debug builds, lint and IDE sync MUST remain usable without a keystore. A configured keystore file that does not exist MUST fail the build.

#### Scenario: Release requested without keystore
- **WHEN** a Release packaging task is requested without `keystore.properties`
- **THEN** the build fails with instructions to configure signing or to pass the explicit opt-in

#### Scenario: Explicit local opt-in
- **WHEN** the developer passes the documented opt-in while building Release without a keystore
- **THEN** the build succeeds with a warning that the artifact is debug-signed and non-production

#### Scenario: Debug and lint unaffected
- **WHEN** a debug build or lint task runs without any keystore configuration
- **THEN** the task succeeds
