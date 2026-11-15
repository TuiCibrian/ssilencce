# Advance shuffle strategies

## Purpose

Complete the requested Advance Shuffle delta by adding album-artist and composer most-played
strategies without changing any existing shuffle mode.

## ADDED Requirements

### Requirement: Album-artist and composer most-played modes

The system SHALL offer Most played album artists and Most played composers in the existing advanced
shuffle sheet. Each mode SHALL group songs by the normalized corresponding tag, SHALL sum the
group's play counts, SHALL apply the same `ln(sum + 1.0)` weighting and `1.0` base weight as the
existing artist/album strategies, and SHALL return every input song exactly once. Most played
composers SHALL be available only on Android 11/API 30+ because composer tags are only exposed by
the library from that version, matching the Composers category; Most played album artists SHALL
remain available on every supported API.

#### Scenario: Most played album artists
- **WHEN** the user selects Most played album artists
- **THEN** songs from album artists with more aggregated plays are favored, and songs without an
  album-artist tag remain eligible through their song artist

#### Scenario: Most played composers
- **WHEN** the user selects Most played composers
- **THEN** songs from composers with more aggregated plays are favored, and songs without a
  composer tag share one unknown group instead of being dropped

#### Scenario: Missing play statistics
- **WHEN** the selected songs have no play-count rows
- **THEN** every group receives the same base weight and the full song set remains available to
  the shuffle

#### Scenario: Composer availability below API 30
- **WHEN** the device runs below Android 11/API 30 and the advanced shuffle sheet is opened
- **THEN** Most played composers is hidden while Most played album artists remains available

#### Scenario: Stale composer mode below API 30
- **WHEN** a stale persisted Most played composers mode reaches the manager below API 30
- **THEN** the manager falls back to a plain shuffle instead of using a uniform composer bucket

### Requirement: Existing shuffle behavior is preserved

The system SHALL keep all existing `SpecialShuffleMode` entries, their relative order, icons,
titles, descriptions, weighting and fallback behavior. `OpenShuffleMode`, playback queue handling
and per-open shuffle behavior SHALL remain unchanged.

#### Scenario: Existing modes unchanged
- **WHEN** the user opens the advanced shuffle sheet after this change on API 30+
- **THEN** all previous modes are still present in their previous relative order and the two new
  modes appear after Most played albums; below API 30 only the composer mode is omitted

#### Scenario: Failure fallback unchanged
- **WHEN** a weighted strategy throws
- **THEN** the manager still falls back to a plain shuffled list

### Requirement: Special-shuffle queue replacement follows the original restart contract

The 2026-11-10 product decision selects the original weighted-shuffle behavior and rejects the
later 4DA/5C preservation requirement. When a special shuffle replaces the queue, the system SHALL
build the new weighted queue, SHALL start at position 0, and SHALL play. It SHALL NOT preserve the
previous current item, playback position or play/pause intent. The historical 5C preservation FAIL
remains a historical FAIL and is never relabeled PASS; its separate Source error remains a distinct
playback failure.

#### Scenario: Playing track is replaced
- **WHEN** the user selects a special shuffle while a track is playing
- **THEN** the new weighted queue starts at its first item at position 0 and plays

#### Scenario: Paused player is replaced
- **WHEN** the user selects a special shuffle while playback is paused
- **THEN** the new weighted queue starts at its first item at position 0 and plays

#### Scenario: Historical preservation evidence
- **WHEN** the 4DA/5C preservation evidence is reviewed
- **THEN** the 5C FAIL is retained as historical evidence for the rejected contract, not treated
  as a PASS or as a regression against the original contract

### Requirement: Scope stays bounded

This change SHALL NOT add a Room schema or migration, a dependency, an application version change,
a new Compose screen or any translated string. Only main English resources SHALL be added.

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no database, dependency, version, layout or translated resource is modified and the
  existing Compose sheet renders the new modes from the enum
