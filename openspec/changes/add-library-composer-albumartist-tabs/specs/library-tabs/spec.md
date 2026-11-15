# Library composer and album-artist tabs

## Purpose

Add separate Album artists and Composers library categories while preserving existing artist
behavior when the new tabs are not enabled.

## ADDED Requirements

### Requirement: Separate Album artists category

The system SHALL provide an Album artists library category that lists album artists using the
existing album-artist queries and opens the existing album-artist detail/playback flow.

#### Scenario: Album artists tab
- **WHEN** the user enables Album artists and opens it
- **THEN** the list contains album artists and tapping one opens album-artist detail with its
  albums, songs and playback actions

#### Scenario: Separation when visible
- **WHEN** Album artists is visible
- **THEN** the Artists tab lists regular artists and does not show the old “Only album artists”
  toggle

#### Scenario: Legacy behavior when hidden
- **WHEN** Album artists is hidden
- **THEN** the Artists tab retains the existing `onlyAlbumArtists` behavior and toggle

### Requirement: Composers category from MediaStore

On API 30+, the system SHALL provide a Composers category built from
`MediaStore.MediaColumns.COMPOSER`, listing nonblank composer names with song and album counts
derived from their songs. On API 26–29 the category SHALL be unavailable and SHALL NOT use a
metadata-scan fallback.

#### Scenario: Composer list
- **WHEN** the user opens Composers on API 30+ with composer-tagged songs
- **THEN** composer names are listed with song/album counts and in-tab quick search works

#### Scenario: Composer detail
- **WHEN** the user opens a composer
- **THEN** albums, songs, Play and Shuffle use the same artist detail/playback behavior

#### Scenario: Pre-30 device
- **WHEN** the app runs on API 26–29
- **THEN** Composers is not offered and no metadata scan is attempted

### Requirement: Composer actions are safe

Composer rows and detail SHALL NOT offer artist tag editing, add-to-home shortcuts, play-info or
Last.fm biography/similar-artist actions, which do not apply to composer names. Song playback,
shuffle, multi-select and song/album navigation SHALL remain available.

#### Scenario: Composer menu
- **WHEN** the user opens a composer row menu or composer detail menu
- **THEN** tag-editor, add-to-home and play-info actions are absent, while song actions remain

### Requirement: Category persistence is compatible

New categories SHALL be appended invisible to existing persisted category lists, preserving the
existing order and original first five visible tabs. The change SHALL NOT add a Room schema
migration, dependency, version change or translated strings.

#### Scenario: Existing install
- **WHEN** an existing user opens library settings after the update
- **THEN** Album artists and Composers are available but initially invisible, and their previous
  tabs/order are unchanged

### Requirement: Scope stays bounded

Android Auto entries, dedicated global-search filters and multi-composer splitting SHALL remain
out of scope for this change.

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no Android Auto, search-filter or multi-composer behavior is changed
