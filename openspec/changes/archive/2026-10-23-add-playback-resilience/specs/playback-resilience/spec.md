# Playback resilience

## Purpose

Add the missing playback capabilities for long-form listening, interruption handling, quick
system access and process-restart recovery while staying within Android platform limits.

## ADDED Requirements

### Requirement: Quick Settings tile reflects and controls playback

The system SHALL expose a Quick Settings tile that is active while playing, inactive while
paused with a loaded queue, and unavailable otherwise; tapping it SHALL toggle play/pause. The
tile MUST connect only while listening and release its controller when hidden.

#### Scenario: Playing
- **WHEN** playback is active and the tile is visible
- **THEN** the tile is active and tapping it pauses playback

#### Scenario: No session
- **WHEN** no playback queue is loaded
- **THEN** the tile is unavailable rather than falsely active

### Requirement: Long-form tracks resume per track

The system SHALL optionally remember the last position of each track and resume it when the same
track starts again, SHALL ignore positions too close to the start or end, and SHALL clear the
remembered position when the track completes.

#### Scenario: Resume an audiobook chapter
- **WHEN** the user enables per-track positions, pauses a long track at 40 percent and plays it again later
- **THEN** playback resumes at the saved position

#### Scenario: Seek then skip
- **WHEN** the user seeks within a long track and immediately skips to another item
- **THEN** the saved position reflects the seek, not the last periodic sample

#### Scenario: Completion resets
- **WHEN** a track finishes normally
- **THEN** its remembered position is cleared and the next start begins at zero

### Requirement: Audio focus behavior is configurable

The system SHALL support normal, ignore, ask-before-taking and resume-after-call focus modes,
SHALL pause on transient loss, and SHALL NOT silently take focus from another audible app in ask
mode when no confirmation UI is available.

#### Scenario: Resume after call
- **WHEN** the user selects resume after call and a transient focus loss ends
- **THEN** playback resumes without a manual press

#### Scenario: Ask mode
- **WHEN** another app is audibly playing and the user starts playback in ask mode
- **THEN** the app asks for confirmation before taking focus or stays paused

### Requirement: Active sessions recover after a service restart

The system SHALL persist whether playback was active, SHALL optionally restore and resume the
queue after a system service/process restart, and MUST NOT claim to bypass force-stop.

#### Scenario: Service restart
- **WHEN** the system recreates the playback service after a process kill and recovery is enabled
- **THEN** the saved queue and position are restored and playback resumes once

#### Scenario: Paused before service destruction
- **WHEN** the user pauses and the playback service is destroyed before the debounced state save fires
- **THEN** recovery stays paused instead of resuming the queue

#### Scenario: Recovery disabled
- **WHEN** the system recreates the service and recovery is disabled
- **THEN** the queue is restored but playback remains paused
