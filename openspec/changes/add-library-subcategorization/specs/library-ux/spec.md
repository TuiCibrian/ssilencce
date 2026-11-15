# Library sub-categorization

## Purpose

Close the album composer and genre detail sub-categorization gaps with existing library patterns
and without new persistence.

## ADDED Requirements

### Requirement: Album composer filter

On API 30+, the album list SHALL offer a Filter by composer action when at least one album song has
a nonblank composer value. The dialog SHALL offer All composers followed by normalized composer
names, and selecting one SHALL filter the existing album grid to albums containing a matching
composer. Selecting All composers SHALL clear the filter. On API < 30 the action SHALL be hidden
and no metadata-scan fallback SHALL be used.

#### Scenario: Composer filter is available

- **WHEN** the app runs on API 30+ and albums contain composer-tagged songs
- **THEN** the album list offers Filter by composer with All composers and the normalized names

#### Scenario: Filter and clear

- **WHEN** the user selects a composer and later selects All composers
- **THEN** the album grid first shows only albums containing that composer's songs and then returns
  to the full album list

#### Scenario: Pre-30 device

- **WHEN** the app runs on API 26-29
- **THEN** the album list does not offer Filter by composer and does not scan metadata for composers

### Requirement: Genre detail subcategories

Genre detail SHALL offer a View action with Songs, Albums and Artists choices. Songs SHALL keep the
existing song list; Albums and Artists SHALL use the existing album/artist adapters and detail
screens, derived from the same genre song query. The selected subcategory SHALL survive
configuration recreation.

#### Scenario: Switch to albums

- **WHEN** the user chooses Albums in genre detail
- **THEN** the screen lists the albums represented by the genre's songs and tapping an album opens
  the existing album detail screen

#### Scenario: Switch to artists

- **WHEN** the user chooses Artists in genre detail
- **THEN** the screen lists the artists represented by the genre's songs and tapping an artist opens
  the existing artist detail screen

### Requirement: Subcategory actions follow the visible list

Sort menus, Play, Shuffle and quick search in genre detail SHALL operate on the visible
subcategory. The genre header count and duration SHALL remain based on the full genre song set.

#### Scenario: Play visible albums

- **WHEN** the user is viewing genre Albums and presses Play or Shuffle
- **THEN** playback uses the songs of the visible albums, while the header still shows the full
  genre song count and duration

#### Scenario: Search visible artists

- **WHEN** the user types a query while viewing genre Artists
- **THEN** only artists matching the query remain visible and the empty state is shown only when the
  full artist collection is non-empty

#### Scenario: Matching sort menu

- **WHEN** the user switches between Songs, Albums and Artists
- **THEN** the sort menu offers the corresponding song, album or artist sort keys and applies them
  to the visible list

### Requirement: Scope stays bounded

This change SHALL NOT add Android Auto or global-search subcategory entries, multi-composer
splitting, sectioned album headers, a Room schema migration, a dependency or an application version
change.

#### Scenario: Scope check

- **WHEN** the change is reviewed
- **THEN** no Android Auto, global-search, multi-composer, schema, dependency or version behavior is
  changed
