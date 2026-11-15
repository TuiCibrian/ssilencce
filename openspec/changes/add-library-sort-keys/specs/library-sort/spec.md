# Library sort keys

## Purpose

Complete the source-confirmed extra sort keys for song and file lists without changing the
existing locale-aware, article-aware or numeric-aware ordering behavior.

## ADDED Requirements

### Requirement: Song and file lists expose full-path sorting

The system SHALL offer a Path sort key on the all-songs, folder-songs and file-browser lists,
SHALL order songs and folders by their full file path with the shared locale collator, and SHALL
persist the selection per list like the other sort keys.

#### Scenario: Sort all songs by path
- **WHEN** the user selects Path in the all-songs sort menu
- **THEN** the songs are ordered by their full file path and the Path item remains checked
  after the screen is recreated

#### Scenario: Folder songs by path
- **WHEN** the user selects Path in a folder's song sort menu
- **THEN** that folder's songs are ordered by their full file path

#### Scenario: File browser by path
- **WHEN** the user selects Path in the file-browser sort menu
- **THEN** folders and songs are each ordered by their full file path while folders remain
  listed before songs

### Requirement: General song lists expose track-number sorting

The system SHALL offer the existing Track sort key on the all-songs and folder-songs lists and
SHALL reuse the current track-number ordering and descending toggle.

#### Scenario: Sort all songs by track number
- **WHEN** the user selects Track in the all-songs sort menu
- **THEN** songs are ordered by track number with the existing title tiebreak

#### Scenario: Descending track order
- **WHEN** Track is selected and Descending is enabled
- **THEN** the track-number order is reversed

### Requirement: Existing sorting behavior is preserved

The system SHALL keep existing filename, date-modified, numeric, descending and
article-ignoring behavior and SHALL NOT remove existing sort-menu entries.

#### Scenario: Natural number order remains
- **WHEN** titles contain numbers such as 2 and 10
- **THEN** the shared collator continues to compare them numerically rather than lexically

#### Scenario: Article-aware title order remains
- **WHEN** article-ignoring sort is enabled and titles begin with The/A/El
- **THEN** the leading article continues to be ignored for title/artist/album ordering

#### Scenario: Path-sorted popup is not misleading
- **WHEN** a song list is ordered by Path
- **THEN** the FastScroller popup is blank instead of showing a title letter that does not match
  the path order

### Requirement: Out-of-scope sort work stays deferred

The system SHALL NOT add play-statistics sort keys or a custom ignore-keyword whitelist as part
of this change.

#### Scenario: Play-statistics keys remain absent
- **WHEN** the user opens a song sort menu after this change
- **THEN** date-last-played and most/least-played are not offered

#### Scenario: Whitelist preference remains unchanged
- **WHEN** the user opens library settings after this change
- **THEN** the existing ignore-articles switch is unchanged and no custom keyword editor appears
