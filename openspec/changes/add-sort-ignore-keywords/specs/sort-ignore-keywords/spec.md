# Sort ignore-keywords

## Purpose

Add a user-editable list of leading words ignored by title/artist/album sort comparisons, without
changing the existing article switch or affecting search and displayed metadata.

## ADDED Requirements

### Requirement: Custom keywords extend article ignoring

The system SHALL ignore a user-defined leading keyword in title, artist and album sort comparisons
only while “Ignore articles when sorting” is enabled, in addition to the existing locale article
list. Matching SHALL be case-insensitive, SHALL require whitespace after the keyword, and SHALL
treat keyword text literally.

#### Scenario: Existing switch off
- **WHEN** “Ignore articles when sorting” is off and the user has stored custom keywords
- **THEN** sorting uses the raw title/artist/album text and the custom list has no effect

#### Scenario: Custom keyword applies
- **WHEN** article ignoring is on and the list contains “Los”
- **THEN** “Los Lobos” sorts as “Lobos” while built-in articles continue to apply

#### Scenario: Literal and bounded match
- **WHEN** a keyword contains regex characters or only appears inside a longer word
- **THEN** the characters are matched literally and a keyword without following whitespace does not
  strip part of that word

### Requirement: Keywords are persisted and editable

The system SHALL persist the keyword list as a string set in existing preferences, defaulting to
empty, and SHALL provide a Compose dialog to view, add and remove keywords. It SHALL trim and
deduplicate case-insensitively, reject blanks and over-long keywords, and report when the list is
full. The settings row SHALL show the count and SHALL be disabled while the article switch is off.

#### Scenario: Add and remove
- **WHEN** the user adds “Los” and later removes it
- **THEN** the stored set and the sort normalization change immediately, and the summary count
  updates

#### Scenario: Duplicate or invalid input
- **WHEN** the user enters a blank, duplicate or over-long keyword
- **THEN** the dialog keeps the list unchanged and shows the corresponding message

#### Scenario: Default behavior
- **WHEN** the stored list is empty
- **THEN** sorting behavior is identical to the existing built-in article handling

### Requirement: Scope is sorting only

Custom keywords SHALL be applied only by the shared sort normalizer and section-letter helper.
Search matching and displayed titles, artists and albums SHALL remain unchanged. The change SHALL
NOT add a database entity/migration, dependency, version change, translated string or dedicated
custom-keyword switch.

#### Scenario: Search is unaffected
- **WHEN** a custom keyword is stored and the user searches for the raw word
- **THEN** search results and displayed titles still use the original text

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no schema, dependency, version, translated-string or search-filter change is present
