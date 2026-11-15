# Queue multi-item drag

## Purpose

Add true multi-selection and block drag reordering to the queue without using filtered positions,
without losing duplicate songs or current/stop playback state, and without changing swipe,
single-item drag, the end footer, re-shuffle or named queues.

## ADDED Requirements

### Requirement: Key-selection mode

The queue SHALL support a selection mode keyed by `QueueSong.key`, not by position in a filtered
list. A long-press on a row SHALL enter selection (or toggle the row when already selecting); a tap
while selection is active SHALL toggle that row instead of playing it. The selection bar SHALL show
the selected count and SHALL offer Select all and Cancel. Block drag SHALL require at least two
selected rows; one selected row MAY remain an intermediate state.

#### Scenario: Enter and extend selection
- **WHEN** the user long-presses a row, then taps another row
- **THEN** both rows are selected and the selection count is shown

#### Scenario: Toggle and cancel
- **WHEN** the user taps a selected row or chooses Cancel
- **THEN** that row is deselected or the whole selection is cleared

#### Scenario: Duplicate songs
- **WHEN** the queue contains the same media ID more than once
- **THEN** each occurrence is selected independently through its `QueueSong.key`

### Requirement: Block drag and drop semantics

Dragging any selected row's handle with at least two selected rows SHALL move the selected block as
one unit and preserve the relative order of the selected rows. The block SHALL be inserted before
the target when moving upward and after the target when moving downward, evaluated after removing
the selected block. A drop on a selected row or inside the selected effective range SHALL be a
no-op. Dropping outside the block SHALL make the selected rows contiguous at the target.

#### Scenario: Move downward
- **WHEN** selected rows above a target are dropped on that target
- **THEN** the block is inserted after the target with relative order preserved

#### Scenario: Move upward
- **WHEN** selected rows below a target are dropped on that target
- **THEN** the block is inserted before the target with relative order preserved

#### Scenario: Drop inside selection
- **WHEN** the drop target is a selected row or lies inside the selected effective range
- **THEN** the queue order does not change

### Requirement: Timeline resolution and disabled states

Selected positions and the drop target SHALL be resolved to original timeline indices through
`QueuePosition`; filtered `visibleQueue` positions SHALL NOT be used for queue mutations. Selection
and block drag SHALL be disabled when `queueQuery` is non-blank or the queue is locked. Swipe
removal and single-item drag SHALL continue to work outside selection.

#### Scenario: Filtered queue
- **WHEN** a search query is active
- **THEN** selection is cleared/disabled and no mutation uses a filtered position

#### Scenario: Locked queue
- **WHEN** the queue lock is on
- **THEN** selection and drag handles are disabled

#### Scenario: Outside selection
- **WHEN** no rows are selected
- **THEN** swipe removal and single-item drag behave as before

### Requirement: Repeated move command with state preservation

A block drop SHALL dispatch an app-internal `Playback.MOVE_QUEUE_BLOCK` command with the planned
single-item move indices. The service SHALL validate the move arrays, apply repeated
`moveMediaItem` calls, preserve the current track's play/pause state and position, remap the
stop-after index and current index through the old-to-new map, and persist the final full queue via
`PersistentStorage.saveState(true)`. The stop-after remap SHALL match `SET_STOP_POSITION`: a
remapped target equal to the current item SHALL set `pauseAtEndOfMediaItems = true` and
`stopIndex = -1`; a future remapped target SHALL set `pauseAtEndOfMediaItems = false` and
`stopIndex` to that target. The command SHALL NOT reset the queue or use a filtered subset.

#### Scenario: Current item preserved
- **WHEN** the block includes or crosses the current item
- **THEN** the same media item remains current and playing/paused state and position are retained

#### Scenario: Future stop-after target
- **WHEN** a future stop-after target is remapped by a block move
- **THEN** `stopIndex` follows the target and `pauseAtEndOfMediaItems` remains false so playback
  stops only when that future item is reached

#### Scenario: Stop-after target becomes current
- **WHEN** the remapped stop-after target is the current item
- **THEN** `stopIndex` is cleared and `pauseAtEndOfMediaItems` is true for the current item's end

#### Scenario: Final order persisted
- **WHEN** the block drop completes
- **THEN** the full queue order is saved and the queue/position flows reflect the new timeline

### Requirement: Shuffle, duplicates and sequential timeline

The implementation SHALL handle duplicate media IDs through occurrence-aware keys, shuffle on/off
through effective-to-timeline mapping, and sequential timeline mode through the existing
`AdvancedForwardingPlayer.moveMediaItem` path. Process recreation SHALL use the persisted full
queue; no filtered subset or partial order SHALL be persisted.

#### Scenario: Shuffled queue
- **WHEN** shuffle is on and a block is dragged
- **THEN** selected effective rows are resolved through `QueuePosition` before timeline moves

#### Scenario: Sequential timeline
- **WHEN** queue-next/sequential timeline mode is enabled
- **THEN** the move path uses the existing forwarding player so upcoming ranges stay consistent

#### Scenario: Process recreation
- **WHEN** the process is recreated after a block move
- **THEN** the restored queue contains the final full order with duplicate positions intact

### Requirement: Scope is limited

Only main English selection strings SHALL be added. No dependency, Room schema/entity/DAO
migration, permission or translated-string change SHALL be introduced. The end footer, re-shuffle,
named queues and unrelated queue behavior SHALL NOT change.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** no dependency/schema/permission/translated-string change and no unrelated queue feature
  change is present
