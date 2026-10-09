## Purpose

Provide the approved black-and-white Aurora interface natively while keeping the existing local music-player behavior, saved choices and platform integrations accessible.

## ADDED Requirements

### Requirement: Compatible appearance selection
The application SHALL enable Aurora for new installations and SHALL NOT overwrite existing appearance, category or player-style preferences. Users SHALL be able to switch back to the retained themes.

#### Scenario: Existing installation
- **WHEN** an installation with existing settings upgrades
- **THEN** its appearance and saved player style remain unchanged until Aurora is selected explicitly

#### Scenario: New installation
- **WHEN** the application initializes an empty preference store
- **THEN** Aurora is selected without deleting the alternative theme settings

### Requirement: Real navigation and capabilities
The interface SHALL expose Home, Queue, Player, Folders, Albums and More, preserve configurable library categories and connect supported actions to actual application behavior. Unsupported actions SHALL be visibly unavailable with an explanation.

#### Scenario: Library navigation
- **WHEN** the user selects a category from tabs or the drawer
- **THEN** the real category opens, with sorting, selection and its existing menus available

#### Scenario: Prototype-only action
- **WHEN** the interface presents multiple queues or manual lyrics synchronization
- **THEN** the control is unavailable and does not simulate a successful operation

### Requirement: Seven player styles retain behavior
Aurora SHALL adapt Default, FullCover, Gradient, Plain, M3, Expressive and Peek without introducing an eighth style. Playback, seeking, cover paging, supported cover lyrics, configured gestures, repeat, shuffle and additional actions SHALL remain accessible.

#### Scenario: Saved player style
- **WHEN** a user enables Aurora with a saved player style and cover gestures
- **THEN** that style and its supported gestures remain in effect

### Requirement: Reference fidelity and safe motion
The appearance SHALL use the selected reference's black surfaces, white accents, translucent borders, typography and screen anatomy rather than the navy brand variant. Motion SHALL stop when not visible, when system animations are disabled and in battery saver. Actionable controls SHALL have at least 48 dp touch targets.

#### Scenario: Reduced motion
- **WHEN** system animation scale is zero or battery saver is enabled
- **THEN** the ribbon is static and playback controls continue to work

### Requirement: Honest data and verification
The interface SHALL display real library/playback data and preserve loading, empty, missing-data, error and permission states. Verification reports SHALL distinguish build/lint results from device and performance checks that have not been performed.

#### Scenario: Empty library or queue
- **WHEN** no real songs are available
- **THEN** the interface displays an empty state rather than sample songs, durations or covers
