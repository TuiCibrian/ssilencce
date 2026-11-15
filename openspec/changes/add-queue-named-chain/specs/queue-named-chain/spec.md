# Named-queue chain

## Purpose

Add a bounded First/Previous/Next chain over saved queues with one stable order, active-queue
guards, explicit boundaries and the existing atomic switch, without changing the queue end
footer, re-shuffle or multi-drag.

## ADDED Requirements

### Requirement: One stable named-queue order

`NamedQueueSummary` SHALL expose `createdAt`. The manager display and the chain SHALL both use the
same `created_at ASC, id ASC` order. This order SHALL be derived from the existing column without
a Room schema migration, and the chain SHALL use the same summaries query as the manager.

#### Scenario: Manager and chain order
- **WHEN** multiple named queues exist
- **THEN** the manager lists them oldest-created first with id as the tie-breaker, and the chain
  uses that same order

### Requirement: Chain actions require an active named queue

First/Previous/Next SHALL act only when `ACTIVE_NAMED_QUEUE_ID` is set and the current queue is
non-empty. The app SHALL load the active named snapshot and compare its media IDs, order and count
with the current controller timeline; any mismatch SHALL disable the action with a clear message
and no switch. A missing/deleted active snapshot SHALL also disable with a clear message. The
current queue being empty (including after clear-on-completion) SHALL make the chain inactive
until a named queue is active again. Chain actions SHALL NOT auto-save an unsaved current queue.

#### Scenario: No active named queue
- **WHEN** no named queue is active
- **THEN** the chain reports that a saved queue must be opened and performs no switch

#### Scenario: Unrelated current queue
- **WHEN** the active id is set but the current timeline is a different queue
- **THEN** the action stops with the not-active-saved-queue message and no switch

#### Scenario: Modified or reordered active queue
- **WHEN** the current timeline's media IDs/count/order no longer match the active snapshot
- **THEN** the action stops with the not-active-saved-queue message and no switch

#### Scenario: Active id deleted
- **WHEN** the active named queue row has been deleted but the id remains set
- **THEN** the action stops with a clear unavailable message and no switch

#### Scenario: Auto-cleared queue
- **WHEN** clear-on-completion leaves the current queue empty while an active id remains
- **THEN** the chain is inactive with a clear message until a named queue is switched/loaded

### Requirement: First/Previous/Next resolve in stable order

The app SHALL resolve ordered queue ids, find the active id and choose:
first = first id; previous = id before active; next = id after active. First SHALL load the first
ordered queue. Previous at the first id and Next at the last id SHALL stop with a clear boundary
message and SHALL NOT wrap. A missing/unknown active id SHALL report no active queue and SHALL NOT
switch.

#### Scenario: Next from middle
- **WHEN** the active queue is B in order A, B, C
- **THEN** Next loads C

#### Scenario: Previous at first
- **WHEN** the active queue is A in order A, B, C
- **THEN** Previous stops with the first-boundary message and no switch/wrap

#### Scenario: Next at last
- **WHEN** the active queue is C in order A, B, C
- **THEN** Next stops with the last-boundary message and no switch/wrap

#### Scenario: First
- **WHEN** the user chooses First
- **THEN** the first ordered queue is loaded

### Requirement: Reuse atomic switch and preserve target resume

A resolved target SHALL be loaded through the existing `switchToNamedQueue(target)` path, which
performs the ADR-046 atomic outgoing save and restores the target's saved index/position. An empty
or unavailable target SHALL produce the existing message and SHALL NOT partially switch or skip
to another queue.

#### Scenario: Empty target
- **WHEN** the resolved next/previous/first queue has no playable songs
- **THEN** an empty-queue message is shown and the current queue remains active

#### Scenario: Switch failure
- **WHEN** the atomic switch reports failure
- **THEN** the existing unavailable message is shown and the current queue remains intact

### Requirement: Manager and footer behavior are preserved

Direct manager switching and its stable display order SHALL remain available. The queue end
footer, clear-on-completion, swipe/search/limits/duplicate policy, re-shuffle and multi-drag
SHALL NOT change. No schema, dependency, permission or translated-string change SHALL be added.

#### Scenario: Direct manager switch
- **WHEN** the user taps a saved-queue row
- **THEN** it switches directly as before, independent of the chain actions

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no end-footer change, re-shuffle, multi-drag, schema, dependency, permission or
  translated-string change is present
