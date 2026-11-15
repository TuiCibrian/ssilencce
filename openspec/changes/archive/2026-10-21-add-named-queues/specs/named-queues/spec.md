# Named queues and queue search

## Purpose

Let users keep multiple persistent named playback queues, switch between them without losing
their position, and search within the active queue.

## ADDED Requirements

### Requirement: Named queues persist

The system SHALL let the user save the active queue under a name and SHALL persist its ordered
media ids, resume position, repeat mode and shuffle-enabled state across restarts.

#### Scenario: Save and restart
- **WHEN** the user saves the active queue as Road Trip and restarts the app
- **THEN** Road Trip is listed with its original item order and saved resume position

### Requirement: Named queues can be managed

The system SHALL support switching, renaming, deleting and removing other named queues, SHALL
preserve the current queue's state before a switch, and SHALL keep the current queue intact when
a target cannot be resolved.

#### Scenario: Switch queues
- **WHEN** the user switches from queue A at 3:20 to queue B saved at 12:10
- **THEN** queue B loads and resumes at its saved position, and queue A's position is saved

#### Scenario: Empty target
- **WHEN** every item of the target queue is missing from the device
- **THEN** the current queue remains unchanged and the user sees a message

### Requirement: Queue-local search filters the active queue

The system SHALL provide search within the active queue that filters by title, artist and album,
SHALL map actions back to the original queue indices, and SHALL NOT modify queue contents.

#### Scenario: Filter and play
- **WHEN** the user filters the queue for a title and taps a result
- **THEN** the corresponding original queue item plays

#### Scenario: Clear search
- **WHEN** the user clears the queue search
- **THEN** the full queue is visible again in its original order
