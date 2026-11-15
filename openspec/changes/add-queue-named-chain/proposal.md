# Proposal: Named-queue chain (follow-up B)

## Why

Named queues can be saved, switched, renamed and deleted, but there is no first/previous/next
chain. The end-footer work deliberately deferred named-queue chaining because it needs one stable
order and explicit boundary behavior. This change adds that bounded chain while preserving the
existing atomic switch path and manager behavior.

## What Changes

- Expose `createdAt` in `NamedQueueSummary` and use one stable `created_at ASC, id ASC` order for
  both the manager display and the chain.
- Add First/Previous/Next actions in the named-queue manager, enabled only when a named queue is
  active and the current queue is non-empty.
- Resolve ordered IDs, find the active queue and call the existing `switchToNamedQueue(target)`,
  preserving ADR-046 outgoing save and target resume position.
- Stop at first/last with clear messages; no wrap. First loads the first ordered queue.
- Stop on an empty/unavailable target with the existing message and no partial switch; after
  clear-on-completion the chain is inactive until a named queue is active again.
- Preserve direct manager switching, queue order, end-footer behavior and all other queue
  features.
- Do not add re-shuffle, multi-drag or end-footer changes.

## Impact

- Modified: `NamedQueueDao`/`NamedQueueSummary`, `NamedQueueRepository`, `PlayerViewModel`,
  `QueueScreen`, main English strings, queue docs.
- New: pure `NamedQueueChainPolicy.kt`.
- Unchanged: schema, dependencies, permissions, direct manager switch, atomic switch semantics,
  queue-end footer, re-shuffle and multi-drag.
