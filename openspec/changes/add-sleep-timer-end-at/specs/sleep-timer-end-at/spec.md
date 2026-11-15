# Sleep timer exact end time

## Purpose

Let users choose an exact local wall-clock sleep-timer end time while preserving the existing
countdown timer, cancel, fade-out and finish-last-song behavior.

## ADDED Requirements

### Requirement: The sleep timer offers duration and exact end-time modes

The system SHALL offer `Duration` and `End at` modes in the Compose sleep-timer sheet. The
`End at` mode SHALL use a Compose Material 3 24-hour time picker, SHALL display the selected
local `HH:mm`, and SHALL show `Ends at HH:mm` while its timer is running. Controls SHALL be
disabled while the timer is running. English labels SHALL remain in the main
`values/strings.xml` only.

#### Scenario: End at mode is selected
- **WHEN** the user selects End at while the timer is idle
- **THEN** a Compose time input opens with the persisted or next-full-hour local time and
  the sheet shows the selected `HH:mm`

#### Scenario: Duration mode remains available
- **WHEN** the user selects Duration
- **THEN** the existing 5-180 minute slider and presets remain available and schedule the
  same countdown behavior as before

#### Scenario: Running controls
- **WHEN** a sleep timer is running
- **THEN** mode and time controls are disabled and the countdown, plus `Ends at HH:mm` for
  end-at mode, remain visible

### Requirement: End at resolves the next future occurrence

The system SHALL schedule an end-at time strictly after the current instant. A selected time
later today SHALL schedule today; a time equal to or earlier than now SHALL schedule
tomorrow. Minute boundaries SHALL account for seconds, and 23:59 → 00:00 SHALL roll into the
next day. The system SHALL use standard `java.time` zone rules for DST gaps and overlaps
rather than mutating clocks. The next-full-hour default SHALL use the next local wall-clock
hour and zone resolution and SHALL be strictly future. In a DST overlap the earlier-offset
occurrence is chosen; if that instant is not after now, the next calendar day is used. In a
DST gap the selected local time shifts forward. For America/New_York on 2026-11-01, both
01:00-04:00 and 01:30-04:00 SHALL default to 02:00-05:00, strictly future.

#### Scenario: Future minute today
- **WHEN** now is 10:15:30 and the selected time is 10:16
- **THEN** the scheduled delay is 30 seconds

#### Scenario: Equal or past time
- **WHEN** the selected local time is equal to or earlier than now today
- **THEN** the next day's occurrence is scheduled

#### Scenario: Midnight rollover
- **WHEN** now is 23:59 and the selected time is 00:00
- **THEN** the timer targets the next day's 00:00

#### Scenario: DST gap and overlap
- **WHEN** the selected local time falls in a fixed zone's spring-forward gap or fall-back
  overlap
- **THEN** `ZonedDateTime.of` zone rules resolve the target deterministically, and the helper
  documents the resulting gap-forward/earlier-offset behavior

#### Scenario: Next full hour across fall-back
- **WHEN** the local time is 01:00-04:00 or 01:30-04:00 in America/New_York on 2026-11-01
- **THEN** the default next full hour is 02:00-05:00 and its resolved instant is strictly
  after now

### Requirement: Last confirmed end-time selection is persisted

The system SHALL persist the last confirmed mode, end-at hour and end-at minute in the
existing `sleep_timer` DataStore. If no saved end time exists, the initial default SHALL be
the next full hour. A dismissed picker or canceled timer SHALL not confirm a new selection.

#### Scenario: First use
- **WHEN** the user opens End at without a saved value
- **THEN** the initial time is the next full hour

#### Scenario: Reopen after setting
- **WHEN** the user previously set a timer in End at mode
- **THEN** the confirmed mode and HH:mm are restored from DataStore

#### Scenario: Cancel a picker
- **WHEN** the user opens the time picker and dismisses it without confirming
- **THEN** the previously confirmed values and any running timer are unchanged

### Requirement: API26 end-at acceptance is verified

The implementation SHALL be accepted on an API26 device with continuous test audio: an
End-at timer targeting the next minute SHALL pause playback at that target and stop running,
and a canceled End-at timer SHALL not pause later. The unchanged Top Tracks CSV source SHALL
also be smoke-verified on the same lease.

#### Scenario: End-at pause on API26
- **WHEN** continuous playback is running and an End-at timer reaches its next-minute target
- **THEN** playback pauses and the timer is no longer running

#### Scenario: Cancel on API26
- **WHEN** an End-at timer is set and then canceled before its target
- **THEN** playback continues and no alarm remains

### Requirement: Existing timer behavior remains intact

The system SHALL keep the existing sleep-timer engine path, exact-alarm permission handling,
cancel, fade-out and finish-last-song behavior. End at SHALL only replace the delay passed to
the existing scheduler. Exact playback pause SHALL remain verifiable with finish-last-song
disabled.

#### Scenario: End-at timer fires
- **WHEN** an end-at timer is scheduled with finish-last-song off
- **THEN** playback stops at the alarm and the timer is no longer running

#### Scenario: Cancel end-at timer
- **WHEN** a running end-at timer is canceled
- **THEN** no later pause occurs and the timer is no longer running

#### Scenario: Finish-last-song remains
- **WHEN** finish-last-song is enabled for an end-at timer
- **THEN** the existing defer-to-track-end behavior is preserved after the alarm fires
