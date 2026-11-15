# Playlist import conflict policy

## Purpose

Import only the selected songs from a device playlist and make exact-name collisions an
explicit choice while preserving the existing non-interactive backup-restore merge path.

## ADDED Requirements

### Requirement: Import uses only the selected, deduplicated songs

The system SHALL import exactly the songs checked in the song picker, not the full source
playlist. Duplicate song IDs SHALL be deduplicated preserving the first occurrence before
counting and inserting. An empty selection SHALL show the existing not-imported message and
SHALL NOT open a conflict dialog or mutate the database.

#### Scenario: Subset selected
- **WHEN** a source playlist has three songs and the user unchecks one
- **THEN** the imported playlist contains exactly the two checked songs

#### Scenario: Duplicate source IDs
- **WHEN** the selected list contains the same song ID twice
- **THEN** the stored playlist contains that song once and the reported count reflects deduplication

#### Scenario: Empty selection
- **WHEN** the user confirms with no songs checked
- **THEN** the not-imported message is shown and no playlist or song is written

### Requirement: Exact name collisions are explicit

Conflict SHALL be detected only by exact, case-sensitive equality with the selected source
name, matching the existing DAO lookup. If no exact match exists, the import SHALL proceed
directly without showing the conflict dialog.

#### Scenario: No collision
- **WHEN** no playlist has the exact source name
- **THEN** a new playlist is created with the selected songs and the source name

#### Scenario: Exact collision
- **WHEN** at least one playlist has the exact source name
- **THEN** a Compose conflict dialog presents Keep both, Replace existing and Rename imported

#### Scenario: Case difference
- **WHEN** an existing playlist differs only by case
- **THEN** it is not treated as an exact collision by this policy

### Requirement: Keep both is the default and preserves the existing playlist

The conflict dialog SHALL preselect Keep both. Keep both SHALL create a new playlist with the
first free `"source 1"`..`"source 100"` name and the selected songs, leaving the existing
playlist and its contents untouched. When all 100 suffixes are taken, Keep both SHALL be
disabled and Rename SHALL remain available.

#### Scenario: Keep both creates a suffix
- **WHEN** `Name` exists and the user confirms the default Keep both
- **THEN** `Name 1` is created with the selected songs and `Name` is unchanged

#### Scenario: Next suffix
- **WHEN** `Name`, `Name 1` and `Name 2` exist
- **THEN** Keep both creates `Name 3`

#### Scenario: Suffix exhaustion
- **WHEN** `Name` and `Name 1` through `Name 100` exist
- **THEN** Keep both is disabled with an inline explanation and Rename can still be used

### Requirement: Replace existing is explicit, limited and transactional

Replace SHALL be offered only when exactly one playlist has the exact source name and that
playlist is not Favorites. It SHALL require an explicit selection, show the existing song count
and selected count, and SHALL replace only the playlist contents in one transaction. The
playlist ID, name, description and custom cover SHALL be preserved. If the target disappeared,
changed name or became ambiguous before or during the transaction, the operation SHALL abort
with a refreshed conflict/error and SHALL NOT partially replace.

#### Scenario: Replace success
- **WHEN** a single non-Favorites exact match exists and the user selects Replace and confirms
- **THEN** only the selected songs remain in that playlist and its ID and metadata are unchanged

#### Scenario: Replace blocked for Favorites
- **WHEN** the exact match is the Favorites playlist
- **THEN** Replace is disabled with a protected-playlist explanation

#### Scenario: Replace blocked for duplicates
- **WHEN** multiple playlists share the exact source name
- **THEN** Replace is disabled with an ambiguous-name explanation

#### Scenario: Replace conflict changed
- **WHEN** the target no longer matches during revalidation
- **THEN** no partial replacement occurs and the dialog is refreshed or an error is shown

### Requirement: Rename imported never mutates the existing playlist

Rename SHALL apply to the incoming playlist. The name field SHALL be prefilled with the
Keep-both name and SHALL be validated inline: confirmation is disabled for blank/whitespace-only
or duplicate names. On confirmation, the trimmed name SHALL be revalidated and a new playlist
with the selected songs SHALL be created transactionally; the existing playlist SHALL remain
untouched.

#### Scenario: Valid rename
- **WHEN** the user enters an unused trimmed name and confirms
- **THEN** a new playlist with that name and the selected songs is created and the existing playlist is unchanged

#### Scenario: Blank rename
- **WHEN** the field is blank or whitespace-only
- **THEN** an inline blank-name error is shown and Import is disabled

#### Scenario: Duplicate rename
- **WHEN** the trimmed name matches an existing playlist name
- **THEN** an inline duplicate-name error is shown and Import is disabled

#### Scenario: Concurrent name claim
- **WHEN** the name is taken between validation and creation
- **THEN** creation fails cleanly, the dialog stays open and an inline duplicate error is shown

### Requirement: Cancel has no side effects

Cancel, back and dismiss SHALL clear the pending conflict without creating, replacing or
renaming anything and SHALL NOT show a success message.

#### Scenario: Cancel conflict
- **WHEN** the user cancels the conflict dialog before confirming
- **THEN** no playlist row or song row changes and no success message appears

### Requirement: Create and replace are atomic and interactive name resolution is serialized

Creating a playlist and inserting its songs SHALL run in one Room transaction. Replacing
contents SHALL delete old songs and insert selected songs in one Room transaction. The
interactive import path SHALL serialize name resolution and mutation so two imports cannot
both resolve the same free name. This SHALL be achieved without a schema migration or a
unique-name index.

#### Scenario: Create insert failure
- **WHEN** inserting selected songs fails after the playlist row was inserted
- **THEN** the transaction rolls back and no orphan playlist remains

#### Scenario: Replace insert failure
- **WHEN** inserting replacement songs fails after the old songs were deleted
- **THEN** the transaction rolls back and the original songs remain

#### Scenario: Concurrent imports
- **WHEN** two interactive imports resolve names concurrently
- **THEN** the serialized resolver allows one name and the other revalidates to a free suffix or conflict state

### Requirement: Success reports the actual stored name

The success UI SHALL use the stored playlist name, not the source name, and SHALL report the
outcome and imported song count. Created and renamed imports SHALL identify the created name;
Replace SHALL identify the replaced playlist.

#### Scenario: Keep both result
- **WHEN** the source `Name` is stored as `Name 1`
- **THEN** the success message says `Name 1` and the selected song count

#### Scenario: Replace result
- **WHEN** an existing playlist is replaced
- **THEN** the success message identifies the existing stored name and the new song count

### Requirement: Backup restore keeps a separate non-interactive merge policy

Backup restore SHALL continue to merge resolvable songs into an exact-name existing playlist
without showing the conflict dialog or offering Keep both/Replace/Rename. This behavior SHALL
be documented as a separate policy. This change SHALL NOT route backup restore through the
interactive dialog.

#### Scenario: Backup restore collision
- **WHEN** a backup playlist has the same exact name as an existing playlist
- **THEN** resolvable backup songs are merged into the existing playlist non-interactively

#### Scenario: Backup restore no collision
- **WHEN** no existing playlist has the exact backup name
- **THEN** the playlist is created non-interactively with the resolvable songs
