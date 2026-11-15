# Selective backup/restore types

## Purpose

Extend the single ZIP backup with source-confirmed favorites, play counts, named queues and
per-file resume positions while preserving old archive compatibility, selective restore and honest
failure reporting.

## ADDED Requirements

### Requirement: Extended archive categories

The backup archive SHALL include a dedicated `Favorites/favorites.m3u` entry, a
`statistics/play_counts.json` entry, a `queues/named_queues.json` entry and a
`positions/track_positions.json` entry when the corresponding data exists. New archives SHALL
exclude the localized Favorites playlist from the generic `Playlists/` set. Existing archive paths
SHALL remain unchanged.

#### Scenario: New backup with all data
- **WHEN** a user creates a backup with favorites, play counts, named queues and resume positions
- **THEN** the archive contains the four new entries and Favorites does not appear under `Playlists/`

#### Scenario: Empty data
- **WHEN** a category has no rows or queues
- **THEN** its JSON entry is omitted, and an existing empty Favorites playlist may be written as a valid M3U header

### Requirement: Favorites restore and legacy archives

Restore SHALL expose Favorites as an independent category. It SHALL resolve the current localized
Favorites playlist, create it only when songs must be added, and avoid duplicating songs already
present. Archives that keep Favorites under `Playlists/` SHALL keep their legacy behavior.

#### Scenario: New archive, Favorites selected
- **WHEN** the user selects Favorites from a new archive
- **THEN** missing favorite songs are merged into the current Favorites playlist

#### Scenario: New archive, Playlists only
- **WHEN** the user selects Playlists but not Favorites from a new archive
- **THEN** Favorites is not restored

#### Scenario: Old archive, Playlists selected
- **WHEN** an old archive stores Favorites as `Playlists/Favorites.m3u` and the user selects Playlists
- **THEN** the favorites playlist is restored under the current localized name

### Requirement: Play-count merge

Play-count restore SHALL merge `timePlayed`, `playCount` and `skipCount` by maximum against the
current row, SHALL preserve current song metadata when the row exists, and SHALL bulk-upsert new
rows from the backup snapshot.

#### Scenario: Local count is higher
- **WHEN** the local play count is 10 and the backup count is 4
- **THEN** the restored value remains 10

#### Scenario: Backup count is higher
- **WHEN** the local play count is 4 and the backup count is 10
- **THEN** the restored value becomes 10

#### Scenario: New song row
- **WHEN** the backup contains a play-count row for a song not present locally
- **THEN** the row is inserted with the backup values

### Requirement: Named-queue restore

Named-queue restore SHALL preserve a usable backup id, replace the same id transactionally,
enforce `QueueNamePolicy.MAX_NAMED_QUEUES` for new ids and resolve name collisions with unique
names. Blank ids and new queues above the cap SHALL be reported as selected-entry failures. Restore
SHALL NOT change the active queue binding.

#### Scenario: Same id
- **WHEN** a backup queue id already exists
- **THEN** that queue and its items are replaced without consuming another slot

#### Scenario: Name collision
- **WHEN** a new queue name matches another queue name
- **THEN** the restored name receives the existing unique suffix

#### Scenario: Queue cap
- **WHEN** 100 queues exist and the backup contains a new queue id
- **THEN** that queue is skipped and the batch reports a failure

#### Scenario: Mixed batch
- **WHEN** some queue entries restore and another is rejected by the cap
- **THEN** the restore reports an import plus a failure (`Partial`)

### Requirement: Per-file position restore

Per-file position restore SHALL insert missing media ids and replace an existing row only when the
backup `updated_at` is strictly newer than the local value.

#### Scenario: Newer backup
- **WHEN** the backup position timestamp is newer
- **THEN** the local position is replaced

#### Scenario: Local newer or equal
- **WHEN** the backup position timestamp is older or equal
- **THEN** the local position is kept

### Requirement: Structural verification and failure reporting

Empty JSON lists SHALL be valid no-ops. Malformed selected JSON entries SHALL be recorded as
selected-entry failures without aborting later entries. ZIP structure, entry-count and
bounded-stream size failures SHALL still abort the restore.

#### Scenario: Malformed JSON entry
- **WHEN** a selected play-count entry is not valid JSON
- **THEN** the entry is logged and counted as a failure, and later entries continue

#### Scenario: Oversized entry
- **WHEN** an entry exceeds its bounded-stream limit
- **THEN** the restore aborts instead of treating it as a per-entry format failure

#### Scenario: Nothing selected
- **WHEN** no categories are selected
- **THEN** the existing `NothingSelected` outcome is returned

### Requirement: Scope remains limited

The change SHALL NOT add a Room schema/migration, dependency, permission or translated-string
change. New UI SHALL reuse the existing Compose/legacy selective-restore picker and use main
English strings only.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** build files, the Room database and non-English strings are unchanged, and no new screen is required
