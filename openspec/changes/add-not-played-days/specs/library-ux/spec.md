# Not Recently Played threshold

## Purpose

Add an arbitrary N-day threshold to the Not Recently Played smart filter while preserving the
existing history interval presets and default.

## ADDED Requirements

### Requirement: Custom not-played threshold

The system SHALL allow the user to choose an arbitrary Not Recently Played window of 1–3650 days.
The choice SHALL be persisted in existing preferences and SHALL apply to both the smart list and
the Home Not Recently Played suggestion. This change SHALL NOT add a Room schema migration or a
dependency.

#### Scenario: Custom days
- **WHEN** the user opens the Not Recently Played smart list and chooses Custom days
- **THEN** entering a valid day count saves it, reloads the list with that cutoff and shows the
  effective threshold in the subtitle

#### Scenario: Invalid custom days
- **WHEN** the user enters blank, zero, 10000, 36500, 3651 or non-numeric text
- **THEN** the full input remains visible, the 1–3650 rule is shown and Save does not save

#### Scenario: Valid boundaries
- **WHEN** the user enters 1 or 3650
- **THEN** Save is enabled and that value is stored

### Requirement: Existing history interval remains the default

The system SHALL keep the existing history interval behavior when no custom day count is stored.
The stored default SHALL be the use-history-interval sentinel, and the history interval
ListPreference and its presets SHALL remain unchanged.

#### Scenario: Fresh or reset state
- **WHEN** the user has never chosen a custom day count, or chooses Use history interval
- **THEN** the Not Recently Played cutoff resolves through the existing history interval setting
  exactly as before

#### Scenario: Presets remain available
- **WHEN** the user changes the history interval preset in Settings
- **THEN** the default Not Recently Played behavior follows that preset unless a custom day count is
  active

### Requirement: Threshold UI lives in the smart-list dialog surface

The Not Recently Played smart list SHALL expose the threshold through its existing Compose dialog
surface. The threshold action SHALL be hidden for every other content type.

#### Scenario: Other smart lists
- **WHEN** the user opens History, Favorites, Top Tracks or another smart list
- **THEN** the Not played period action is not shown

#### Scenario: Threshold applies after reload
- **WHEN** the user saves a threshold from the smart-list dialog
- **THEN** the visible song list and subtitle update without leaving the screen

### Requirement: Scope stays bounded

This change SHALL NOT modify history cutoff semantics, add Android Auto or global-search threshold
UI, add a Room schema migration, a dependency or an application version change.

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no history ListPreference, schema, dependency or version behavior is changed
