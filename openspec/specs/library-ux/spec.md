# library-ux Specification

## Purpose
Close the documented library gaps for selective history deletion, settings and list search,
playlist export formats and per-entity launcher shortcuts.
## Requirements
### Requirement: History can be deleted per song and per selection

The system SHALL let the user remove one song or a multi-selection from playback history, SHALL
confirm before deleting a multi-selection, and MUST leave playback queues, play counts and media
files unchanged.

#### Scenario: Remove one song
- **WHEN** the user chooses Remove from history on a song in the history screen
- **THEN** that song's history row disappears and the rest of the history remains

#### Scenario: Remove a selection
- **WHEN** the user selects several history songs and confirms removal
- **THEN** only those history rows are deleted

#### Scenario: Queue unaffected
- **WHEN** a song is removed from history while it is in the playback queue
- **THEN** the queue still contains the song and playback state is unchanged

### Requirement: Settings can be searched

The system SHALL provide a search entry point in settings that matches preference titles and
summaries across all sections and navigates to the matching section.

#### Scenario: Find a setting
- **WHEN** the user searches for "audio focus"
- **THEN** the result lists the Audio focus section and selecting it opens that section

### Requirement: Library lists support inline quick search

The system SHALL provide a quick-search field on the top-level songs, albums, artists,
playlists, genres, years and folders lists that filters the currently loaded list by its visible
title/metadata and clears back to the full list when the query is emptied.

#### Scenario: Filter songs
- **WHEN** the user types part of a title in the songs list quick search
- **THEN** only matching songs remain visible and clearing the field restores the full list

### Requirement: Playlists export as M3U, M3U8 or PLS

The system SHALL offer M3U, M3U8 and PLS export for one or more playlists, SHALL use the correct
extension and format body, and SHALL report failure when nothing can be written.

#### Scenario: Export M3U8
- **WHEN** the user exports a non-empty playlist as M3U8
- **THEN** a `.m3u8` file with the extended M3U body is written

#### Scenario: Export PLS
- **WHEN** the user exports a non-empty playlist as PLS
- **THEN** a `.pls` file with numbered File/Title/Length entries is written

### Requirement: Specific entities can be added as launcher shortcuts

The system SHALL let the user add a launcher shortcut for an album, artist, folder or playlist
after a confirmation, SHALL open that entity's queue when the shortcut is launched, and SHALL
report when the launcher cannot accept another shortcut.

#### Scenario: Album shortcut
- **WHEN** the user confirms adding an album shortcut and launches it
- **THEN** the app opens and plays that album's songs

#### Scenario: Entity no longer exists
- **WHEN** a shortcut points to a deleted playlist or missing folder
- **THEN** the app reports that the shortcut target is unavailable and does not crash

