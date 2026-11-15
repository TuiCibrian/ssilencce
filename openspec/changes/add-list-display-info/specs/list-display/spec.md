# Song-list display

## Purpose

Give song lists an explicit title display mode and an optional date-modified suffix while
preserving the existing default behavior and avoiding layout or schema changes.

## ADDED Requirements

### Requirement: Song lists expose an explicit title mode

The system SHALL let the user choose Follow sort order, Song title or File name for song-row
titles. The default SHALL be Follow sort order, which SHALL preserve the existing behavior of
showing the file name only while the list is sorted by File name. The choice SHALL be stored in
existing preferences and SHALL apply to general, smart, playlist, album, artist, genre, year,
folder and generic detail song rows rendered by `SongAdapter`, album/artist detail rows rendered
by `SimpleSongAdapter`, and global-search song rows rendered by `SearchAdapter`.

#### Scenario: Follow sort order
- **WHEN** the title mode is Follow sort order and a list is sorted by File name
- **THEN** rows show the file name; with any other sort key they show the song title

#### Scenario: Always song title
- **WHEN** the title mode is Song title
- **THEN** rows show the song title regardless of the active sort key, including File name sorting

#### Scenario: Always file name
- **WHEN** the title mode is File name
- **THEN** rows show the file name regardless of the active sort key, including smart lists with no
  sort mode

#### Scenario: Selection and section text agree
- **WHEN** a song row is selected or the FastScroller popup is shown
- **THEN** the selection name and title-based section text use the same resolved display title

#### Scenario: Global search follows the mode
- **WHEN** the title mode is Song title or File name and global search returns songs
- **THEN** the search song rows use the selected display title and the same selection name

### Requirement: Optional date modified suffix

The system SHALL offer a Show date modified in lists switch, default off. When enabled, a song row
that already has a secondary text view SHALL append a localized date-only modification label to
that text. The label SHALL be derived from the existing `Song.dateModified`/`rawDateModified`
MediaStore value; no metadata read or schema change SHALL be performed while binding rows.

#### Scenario: Date suffix enabled
- **WHEN** the switch is on and a list or global-search song row has a secondary text view
- **THEN** the secondary text ends with the localized date-only modification label

#### Scenario: Date suffix disabled
- **WHEN** the switch is off, including on a fresh install
- **THEN** the secondary text is unchanged from the previous behavior

#### Scenario: Rows without secondary text
- **WHEN** a compact album/artist or Home suggestion row has no secondary text view
- **THEN** the row is unchanged and the date is not shown there

### Requirement: Scope stays bounded

This change SHALL NOT add a Room schema or migration, a dependency, a layout or an application
version change. It SHALL only add main English resources and SHALL leave translated string files
untouched.

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no database, dependency, layout, version or translated resource is modified
