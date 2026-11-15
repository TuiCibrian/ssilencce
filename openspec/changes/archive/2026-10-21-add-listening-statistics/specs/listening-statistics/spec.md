# Listening statistics

## Purpose

Provide honest, period-aware play statistics for songs and artists, using newly recorded play
events and clearly labeling the limits of pre-existing aggregate data.

## ADDED Requirements

### Requirement: Play events are recorded

The system SHALL record one play event per counted play with the song id, artist name and
timestamp, in the same transaction as the aggregate play-count increment. Skipped tracks SHALL
NOT create play events.

#### Scenario: Counted play
- **WHEN** a play reaches the existing play-count threshold
- **THEN** one event row and one aggregate count increment are committed together

### Requirement: Most Played supports periods

The system SHALL offer this week, this month, last 12 months, this year and all time for Top
Tracks, SHALL order songs by recorded plays in the selected period, and SHALL fall back to
aggregate counts for all time.

#### Scenario: This week
- **WHEN** the user selects this week
- **THEN** only songs with play events since the start of the current week are listed, ordered by play count

#### Scenario: Period without events
- **WHEN** the selected period has no recorded events
- **THEN** the screen shows an honest empty state and does not fabricate counts from older data

### Requirement: Artist statistics include percentages

The system SHALL show artist play counts and their percentage of total plays in the selected
period, and SHALL label all-time statistics as aggregate data.

#### Scenario: Artist percentage
- **WHEN** an artist has 18 of 100 recorded plays in the period
- **THEN** the artist row shows 18 plays and 18 percent
