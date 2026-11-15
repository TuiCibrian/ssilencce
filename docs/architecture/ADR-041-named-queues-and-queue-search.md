# ADR-041: Named Queues and Queue-Local Search

## Status

Accepted on 2026-10-20. Supersedes the "keep one active queue" availability decision in
ADR-036 and amends ADR-012. The outgoing-save ordering is superseded by ADR-046; the remaining
named-queue and queue-search decisions remain in force.

## Context

The app persisted one active queue in `QueueEntity` and deliberately marked multiple queues and
queue search unavailable in the Aurora reference. The user authorized implementing both. The
queue UI already contained disabled affordances, and `QueueEntity` must remain the active
playback snapshot used by `PersistentStorage`.

## Decision

- Keep `QueueEntity` as the single active queue. Add separate `NamedQueueEntity` and
  `NamedQueueItemEntity` tables; a named queue is a persistent snapshot loaded into the active
  queue, not a second live timeline.
- Persist ordered media ids, resume index, position, repeat mode and shuffle-enabled state per
  named queue. Exact shuffle permutations are regenerated on switch; the mode is preserved.
- Store the active named-queue binding separately from the active queue. A switch is a single
  service command: the service saves the queue being left, resolves the target, applies the new
  timeline and binding on the main thread, and suppresses pending saves that still see the old
  timeline. Old saves can never be attributed to the newly selected queue.
- Add queue-local search as view state over the active queue. Filtering preserves the original
  indices so play, remove, stop and move actions target the same queue items.
- `NamedQueueRepository` owns creation/management; save/rename/delete/save-as are transactional.
  `PersistentStorage` submits active/outgoing snapshots through the shared serial writer, with
  item replacement and resume metadata in one transaction (ADR-045). Duplicate save names get
  a numeric suffix. The queue-switch command is app-UID-only.
- Update ADR-036 availability language: named queues and queue search are now implemented; the
  remaining unavailable items stay documented.

## Consequences

- Named queues survive restarts and preserve their position; switching saves the outgoing queue.
- The active and named stores must stay coherent; one service command and one repository own the
  transition.
- The exact shuffle permutation per named queue is not restored; the shuffle mode is. This is a
  documented limitation, not a hidden behavior.
- Queue-local search is a filter, not a second queue; clearing it restores the original order.
- Room version 8 adds both tables additively; no backfill is possible because no named queues
  existed.
