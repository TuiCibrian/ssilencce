# Scheduled backups

## Purpose

Provide an opt-in, battery-aware automatic backup that writes the existing backup archive into a
user-chosen folder without doing work while disabled.

## ADDED Requirements

### Requirement: Scheduled backups are opt-in and user-controlled

The system SHALL leave automatic backups disabled by default, SHALL require an explicit enable
action and a user-chosen destination folder, and SHALL stop all scheduled work when disabled.
The system SHALL support daily, weekly and every-30-days cadences. The 30-day option is labeled
"Every 30 days" because WorkManager periodic work cannot express calendar months.

#### Scenario: Disabled by default
- **WHEN** the user has never configured automatic backups
- **THEN** no backup work is scheduled or executed

#### Scenario: Enable without a folder
- **WHEN** the user enables scheduled backups without choosing a folder
- **THEN** the setting is rejected with a message and no work is scheduled

#### Scenario: Disable after enabling
- **WHEN** the user disables scheduled backups
- **THEN** the unique periodic work is cancelled and no further automatic backup runs

### Requirement: Scheduled backups are battery-aware and idempotent

The system SHALL run automatic backups only through WorkManager with a battery-not-low
constraint, SHALL optionally require charging, and SHALL skip a window already backed up. Window
keys SHALL use fixed-duration elapsed buckets aligned with the WorkManager interval so a DST
transition does not suppress a due run.

#### Scenario: Battery low
- **WHEN** the device battery is low at the scheduled window
- **THEN** the backup is deferred by WorkManager and no partial archive is left behind

#### Scenario: Window already completed
- **WHEN** the worker runs twice in the same schedule window
- **THEN** the second run performs no backup and reports the previous success

### Requirement: Scheduled backups reuse the manual archive contract

The system SHALL create the backup with the existing archive format and limits, SHALL write it
into the chosen tree with a unique timestamped name, and SHALL report failure without retrying
unboundedly.

#### Scenario: Successful run
- **WHEN** the worker runs with a valid folder and permission
- **THEN** a timestamped `.bmgbak` file appears in that folder and the last-run status records success

#### Scenario: Revoked folder permission
- **WHEN** the persisted folder permission is no longer usable
- **THEN** the run records a failure and does not create a fallback backup elsewhere
