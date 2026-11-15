# Queue re-shuffle

## Purpose

Add an explicit queue re-shuffle action that rebuilds the current queue's shuffle order without
changing the current track, position, queue identity or playback modes, and without reusing a
shuffle off/on toggle.

## ADDED Requirements

### Requirement: Dedicated re-shuffle command

The app SHALL expose an app-internal `Playback.RESHUFFLE_QUEUE` command that is separate from
`TOGGLE_SHUFFLE`. When invoked with shuffle enabled and a non-empty queue, the service SHALL
replace the current shuffle order with a fresh `ImprovedShuffleOrder` using a new random seed, the
current media-item index as its first index and the current media-item count as its length.

#### Scenario: Re-shuffle while playing
- **WHEN** the queue is non-empty, shuffle is on and the app invokes `RESHUFFLE_QUEUE`
- **THEN** the service assigns a fresh seeded `ImprovedShuffleOrder` whose first index is the
  current media item and whose length equals the queue size

#### Scenario: No toggle shortcut
- **WHEN** the re-shuffle command runs
- **THEN** it does not call `TOGGLE_SHUFFLE`, change `shuffleModeEnabled`, or toggle shuffle
  off and on

### Requirement: Preserve playback and queue identity

Re-shuffling SHALL preserve the current media item and its playback position as closely as
possible. It SHALL NOT change media-item IDs or their timeline order, `repeatMode`, the active
named-queue binding, or the named snapshot's item list/order. It SHALL NOT seek, move, remove or
clear queue items.

The standard `PersistentStorage.saveState()` path MAY refresh the active named queue's resume
metadata (`resume_index`, `resume_position_ms`, `repeat_mode`, `shuffle_enabled`, `updated_at`)
through the existing `PlaybackStateWriter`/`NamedQueueDao.updateResumeState` behavior
(ADR-041/ADR-045). That refresh is existing bookmark persistence, not a re-shuffle-specific
snapshot write; the named item list/order and active binding SHALL remain unchanged.

#### Scenario: Current item and position
- **WHEN** a track is playing or paused at a position and the queue is reshuffled
- **THEN** the same media item remains current at the same position, and only the shuffle
  traversal order changes

#### Scenario: Queue and modes unchanged
- **WHEN** the queue is reshuffled with repeat mode and an active named queue set
- **THEN** the queue IDs/order, repeat mode and active named-queue binding are unchanged; the
  active named snapshot's item list/order remains unchanged, while its resume metadata may refresh
  through the standard save path

#### Scenario: Standard resume-metadata refresh
- **WHEN** a re-shuffle runs while a named queue is active
- **THEN** the standard save path may update that named queue's resume index/position,
  repeat/shuffle fields and `updated_at`, but not its item list/order or the active binding

### Requirement: Persist the new shuffle order

The new order SHALL be persisted through the existing `PersistentStorage.saveState()` path
(ADR-012/ADR-045), which SHALL be the only persistence call in the re-shuffle path. The
implementation SHALL NOT call `NamedQueueDao`, `PlaybackStateWriter` or a snapshot replacement
directly, SHALL NOT write the shuffle-order preference directly, and SHALL NOT require a schema
migration or new dependency.

#### Scenario: Restore after persistence
- **WHEN** the new order has been persisted and playback state is restored
- **THEN** restoration uses the newly persisted shuffle order

#### Scenario: Persistence boundary
- **WHEN** the command persists the fresh order
- **THEN** the only persistence call is `persistentStorage.saveState()`; active named-queue resume
  metadata may refresh through the standard writer, with no explicit DAO or snapshot write

### Requirement: Shuffle-off and empty queue are explicit no-ops

With shuffle off or an empty queue, the command SHALL perform no mutation and SHALL NOT enable
shuffle implicitly. The ViewModel SHALL show an explicit message: shuffle-off feedback for
shuffle being off and an empty-queue message for an empty queue. The service SHALL return
`SessionError.ERROR_INVALID_STATE` for an invalid invocation.

#### Scenario: Shuffle off
- **WHEN** the user or a stale caller invokes re-shuffle while shuffle is off
- **THEN** no order changes, shuffle remains off, and the shuffle-off message is shown

#### Scenario: Empty queue
- **WHEN** re-shuffle is invoked with no media items
- **THEN** no order changes and the empty-queue message is shown

### Requirement: Queue overflow-menu-only surface

The action SHALL be surfaced only in the queue overflow menu, in both the Aurora and non-Aurora
queue appearances. It SHALL be hidden when the real queue is empty and disabled when shuffle is
off. It SHALL NOT be added to the player controls, the ended-queue footer, queue rows or the
multi-drag work.

#### Scenario: Menu availability
- **WHEN** the queue has items and shuffle is on
- **THEN** the overflow menu shows an enabled re-shuffle action

#### Scenario: Shuffle off or empty queue
- **WHEN** shuffle is off or the queue is empty
- **THEN** the overflow action is disabled (shuffle off) or hidden (empty queue), and no silent
  shuffle enable occurs

### Requirement: Scope is limited

Only main English strings SHALL be added. No dependency, Room schema/entity/DAO migration,
permission or translated-string change SHALL be introduced. Multi-drag, the end footer, the named
chain and other queue features SHALL NOT change.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** no dependency/schema/permission/translated-string change and no multi-drag, footer,
  named-chain or unrelated queue behavior change is present
