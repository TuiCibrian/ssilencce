## Purpose

Ensures release artifacts can never silently ship signed with a shared debug certificate: release signing must be configured deliberately or the build refuses, while local development flows stay usable.

## ADDED Requirements

### Requirement: Release packaging requires configured signing or explicit consent

The build SHALL fail a Release packaging request (`assemble*Release`, `bundle*Release`, `install*Release`) when no keystore configuration is present, unless the developer passes the documented explicit opt-in for a local debug-signed build. The keystore file referenced by a present configuration MUST exist.

#### Scenario: Release without keystore configuration
- **WHEN** a Release packaging task runs and no keystore configuration exists and no opt-in was passed
- **THEN** the build fails before producing any Release artifact, with instructions to configure signing or opt in

#### Scenario: Configured keystore file missing
- **WHEN** a keystore configuration exists but its keystore file does not
- **THEN** the build fails with a message naming the missing keystore file

### Requirement: Debug, lint and configuration stay usable

Debug builds, lint tasks and IDE/Gradle configuration SHALL continue to work without any keystore configuration or opt-in.

#### Scenario: Debug build without keystore
- **WHEN** a debug build runs without keystore configuration
- **THEN** it succeeds

#### Scenario: Lint of a Release variant without keystore
- **WHEN** a Release lint task runs without keystore configuration
- **THEN** it runs and reports normally

### Requirement: Opt-in artifacts are marked non-production

When the explicit opt-in is used without keystore configuration, the build SHALL emit a warning that the produced artifacts are debug-signed and MUST NOT be distributed.

#### Scenario: Opt-in build warns
- **WHEN** a Release packaging task runs with the explicit debug-signing opt-in and no keystore configuration
- **THEN** the build succeeds and logs a non-production warning
