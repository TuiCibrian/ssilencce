# Library maximum-duration filter

## Purpose

Add a library-only maximum song-duration filter that composes with the existing minimum filter,
persists as a normal preference and leaves queue resolution, playlists and favorites untouched.

## ADDED Requirements

### Requirement: Maximum-duration preference

The app SHALL expose `maximum_song_duration` with default **0 = off/unlimited**, stored in seconds.
The setting SHALL accept 0..7200 seconds in 60-second steps and SHALL be persisted in default
SharedPreferences. The maximum SHALL be inclusive.

#### Scenario: Default off
- **WHEN** the user has never changed the maximum
- **THEN** the library query is unchanged and includes songs of any duration allowed by the other
  filters

#### Scenario: Maximum enabled
- **WHEN** the maximum is set to a positive value
- **THEN** songs longer than that value are excluded from repository-backed library queries

### Requirement: Minimum and maximum compose

The maximum predicate SHALL be applied in `SongRepository.makeSongCursor` together with the
existing minimum predicate. Both bounds SHALL be inclusive and combined with `AND`, along with
`BASE_SELECTION` and whitelist/blacklist selections. A maximum of 0 SHALL add no predicate.

#### Scenario: Both bounds
- **WHEN** minimum is 15 s and maximum is 600 s
- **THEN** only songs with duration from 15 through 600 seconds are shown

#### Scenario: Maximum off
- **WHEN** maximum is 0 and minimum is 15 s
- **THEN** only the minimum predicate is applied

#### Scenario: Excluded folders
- **WHEN** a whitelist or blacklist is active
- **THEN** duration predicates and folder predicates apply together with `AND`

### Requirement: Invalid ranges are rejected

When the maximum is enabled, the settings UI SHALL reject a maximum below the minimum and a minimum
above the maximum, showing a main-English validation message instead of persisting an empty-range
configuration. A maximum of 0 SHALL accept any minimum.

#### Scenario: Max below min
- **WHEN** minimum is 60 s and the user tries to set maximum to 30 s
- **THEN** the change is rejected and the range message is shown

#### Scenario: Min above max
- **WHEN** maximum is 300 s and the user tries to set minimum to 600 s
- **THEN** the change is rejected and the range message is shown

### Requirement: Scope is library-only

The maximum SHALL NOT filter queue resolution (`songsByMediaItems`), `songByMediaItem`,
`songByFilePath`, `song(songId)`, file-provider URI resolution, the device-playlist
`SortedCursorUtil` path, the unknown-genre list, device or Room playlists, favorites, named-queue
snapshots or delete cleanup. `makeSongCursor` SHALL expose an explicit `skipDurationFilter` guard,
and those call sites SHALL pass it rather than relying on literal query differences. Genre detail
lists, genre search and genre counts currently bypass the shared cursor and SHALL remain a
documented residual rather than being refactored by this change.

#### Scenario: Queue resolution
- **WHEN** a queued song is longer than the maximum
- **THEN** it still resolves and remains in the queue because `songsByMediaItems` skips the duration
  filter

#### Scenario: Unknown genre
- **WHEN** the unknown-genre list contains a song outside the duration range
- **THEN** it remains in that list because the unknown-genre path skips the duration filter

#### Scenario: ID and path resolution
- **WHEN** a song is resolved by MediaItem, file path, song ID or file-provider URI
- **THEN** the duration bounds are skipped and the song still resolves even if it is outside the
  library range

#### Scenario: Device playlist ordering
- **WHEN** a saved device playlist is resolved through `SortedCursorUtil`
- **THEN** its membership/order is unaffected by duration bounds

#### Scenario: Delete cleanup
- **WHEN** delete cleanup resolves a song by ID
- **THEN** the duration bounds are skipped and the cleanup can find the song

#### Scenario: Saved playlist/favorite
- **WHEN** a saved playlist or favorite contains a song longer than the maximum
- **THEN** it remains in that saved collection

#### Scenario: Genre residual
- **WHEN** a genre detail list or genre count is opened
- **THEN** it retains its existing raw query behavior and is not claimed as duration-filtered

### Requirement: Scope is limited

Only main English strings SHALL be added. No dependency, Room schema/entity/DAO migration,
permission or translated-string change SHALL be introduced.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** no schema/dependency/permission/translated-string change or unrelated feature change is
  present
