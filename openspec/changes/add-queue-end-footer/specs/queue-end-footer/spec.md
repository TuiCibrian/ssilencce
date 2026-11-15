# Queue end footer

## Purpose

Give a finished, non-empty queue a visible Replay/Clear footer backed by a real ended state,
without changing auto-clear, queue ordering, shuffle/repeat, named-queue chaining or multi-drag.

## ADDED Requirements

### Requirement: A real ended state

`PlayerViewModel` SHALL expose `isQueueEndedFlow` and report true only when playback is
`Player.STATE_ENDED` and the queue has at least one media item. It SHALL reset when playback
actually restarts or an effective seek/next/previous is issued from ended, and on timeline/
media-item transition or position-discontinuity events and when the queue becomes empty. A
no-op action at the end (next with no next item, forward at the end) SHALL NOT clear it.
`isPlaying == false` alone SHALL NOT mean ended.

#### Scenario: Finished last track
- **WHEN** the last item finishes with repeat off and a non-empty queue
- **THEN** `isQueueEndedFlow` becomes true

#### Scenario: Paused mid-track
- **WHEN** playback is paused while an item remains
- **THEN** `isQueueEndedFlow` is false

#### Scenario: Empty queue
- **WHEN** the queue has zero media items
- **THEN** `isQueueEndedFlow` is false

#### Scenario: User restarts playback
- **WHEN** the user issues a real Play/seek/next/previous from the ended state
- **THEN** the ended state resets only after the restart/seek is actually issued

#### Scenario: Queue row tap at ended
- **WHEN** the user taps a queue row after the footer appears
- **THEN** the mapped row is sought and playback starts, even though the player was
  `STATE_ENDED`; the footer hides only because playback actually restarts

#### Scenario: Queue row tap with stale mapping
- **WHEN** the row's mapped timeline index is outside the current media item count
- **THEN** no seek/play/reset is issued and the ended footer remains visible

#### Scenario: Generic Play at ended
- **WHEN** the user presses generic Play/toggle at an ended queue
- **THEN** the displayed current item (`position.current` mapped through `QueuePosition`, validated
  against the media item count) is sought and playback starts; a stale mapping falls back to the
  controller's current timeline index. Footer Replay still restarts at logical queue position 0

#### Scenario: Generic Play with shuffle
- **WHEN** shuffle is on, the displayed ended current item is Beta 2/2, and the logical current
  maps to a different timeline index than the first shuffled item
- **THEN** generic Play restarts Beta, not the first shuffled item

#### Scenario: No-op seek at ended
- **WHEN** the user presses Next at the last item or Forward at the end
- **THEN** the ended state and footer remain because no effective seek was issued

### Requirement: Footer only for an ended, non-empty queue

`QueueScreen` SHALL show a queue-end footer only when `isQueueEndedFlow` is true and the queue is
non-empty. The pinned search bar and locate-current affordance SHALL remain unchanged. The footer
SHALL show a title, a short explanation, Replay and Clear.

#### Scenario: Ended non-empty queue
- **WHEN** the queue is ended and still has items
- **THEN** the end footer is visible at the end of the queue

#### Scenario: Playing or paused
- **WHEN** the queue is playing or paused mid-track
- **THEN** no end footer is shown

#### Scenario: Empty queue
- **WHEN** the queue is empty
- **THEN** no end footer is shown and the existing empty state is preserved; `QueueFragment`
  navigateUp is best-effort and does not close this Compose sheet

### Requirement: Replay restarts the effective queue order

Footer Replay SHALL seek to the first item of the current effective queue order and start
playback. With shuffle off this is timeline index 0; with shuffle on it is the first item of the
current shuffled order, mapped through `QueuePosition`. The mapped index SHALL be validated against
the current media item count; an unset or out-of-range mapping SHALL fall back to timeline index 0.
Replay SHALL NOT change shuffle or repeat settings and SHALL NOT use generic Play/Pause as the
restart contract.

#### Scenario: Shuffle off
- **WHEN** the user taps Replay with shuffle off
- **THEN** playback seeks to timeline index 0 and starts

#### Scenario: Shuffle on
- **WHEN** the user taps Replay with shuffle on
- **THEN** playback seeks to the first item of the current shuffled order and starts, with
  shuffle still enabled

#### Scenario: Stale out-of-range mapping
- **WHEN** `QueuePosition` yields a positive first index that is outside the current media item
  count
- **THEN** replay falls back to timeline index 0 instead of passing an invalid seek position

#### Scenario: Repeat settings
- **WHEN** the user taps Replay
- **THEN** the current repeat mode is unchanged

### Requirement: Footer Clear is an explicit full clear

Footer Clear SHALL call `clearQueue(QueueClearingBehavior.RemoveAllSongs)`, independent of the
user's keep-current clear preference. The existing overflow Clear SHALL keep using the default
`ON_CLEAR_QUEUE_ACTION`. The existing `clear_queue_on_completion` behavior SHALL remain unchanged;
when it clears at completion the existing empty state is shown and no footer survives;
`QueueFragment` navigateUp is best-effort and no new close behavior is added.

#### Scenario: Footer Clear
- **WHEN** the user taps Clear in the ended footer
- **THEN** every queue item is removed, regardless of the keep-current preference

#### Scenario: Overflow Clear
- **WHEN** the user uses the existing overflow Clear
- **THEN** the configured keep-current/remove-all behavior still applies

#### Scenario: Auto-clear on completion
- **WHEN** `clear_queue_on_completion` is enabled
- **THEN** the service clears the queue, the existing empty state is shown and no footer is required

### Requirement: No queue-chain, re-shuffle or multi-drag

This change SHALL NOT add next/previous/first named-queue chaining, a re-shuffle command or
multi-item drag/selection. It SHALL NOT add dependencies, permissions, schema changes or
translated strings.

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no named-queue chain, re-shuffle, multi-drag, schema, dependency, permission or
  translated-string change is present
