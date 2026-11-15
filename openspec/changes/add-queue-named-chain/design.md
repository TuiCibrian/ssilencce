# Design: Named-queue chain (follow-up B)

## Context

`NamedQueueSummary` currently exposes `updatedAt` and the manager lists summaries ordered by
`updated_at DESC`. `updatedAt` changes on save/replace/rename, so it is unsuitable for
next/previous semantics. `NamedQueueEntity` already has `createdAt`; the DAO can project it
without a schema migration. `PlayerViewModel.switchToNamedQueue(id)` already performs the
ADR-046 atomic outgoing save and target resume restore, so the chain only needs deterministic
target selection and active-queue guards.

## Goals / Non-Goals

**Goals:**

- One deterministic `created_at ASC, id ASC` order for both manager display and chain.
- Active-only First/Previous/Next with no wrap and clear boundary errors.
- Reuse the atomic named-queue switch and target resume position.
- Keep clear-on-completion, auto-clear and direct manager switching unchanged.

**Non-Goals:**

- Re-shuffle, multi-drag, end-footer changes.
- Schema/migration, new dependencies or permissions.
- Auto-saving the current unsaved queue before chaining.
- Wrapping from last to first or first to last.

## Decisions

### One stable order

`NamedQueueSummary` gains `createdAt`. Both `summariesFlow` and `summaries` project
`q.created_at AS createdAt` and order by `q.created_at ASC, q.id ASC`. The manager receives the
same summariesFlow; the chain uses `NamedQueueRepository.orderedQueueIds()` from the same
`dao.summaries()` query, so display and chain cannot diverge. `queues()` may keep any internal
order because it is not used for display or chain semantics.

### Pure chain policy

`NamedQueueChainPolicy.matchesTimeline(namedMediaIds, timelineMediaIds)` requires exact list
equality (IDs, order and count). `NamedQueueChainPolicy.resolve(orderedIds, activeId, direction)`
returns:

- `Target(id)` for First/Previous/Next when a target exists;
- `Boundary` when Previous is at the first id or Next is at the last;
- `NoActive` when the active id is null or absent from the ordered list.

First always resolves the first id (even when already active), so the explicit "jump to first"
action reloads the first saved queue. No modulo/wrap is used.

### ViewModel flow

`switchToFirstNamedQueue`, `switchToPreviousNamedQueue` and `switchToNextNamedQueue` share a
private chain coroutine:

1. Read `_activeNamedQueueId` (fallback `ACTIVE_NAMED_QUEUE_ID`).
2. Require a controller with `mediaItemCount > 0`; this makes the chain inactive after
   clear-on-completion/empty queue until a named queue is active again.
3. Load the active named snapshot and the ordered ids from the repository. Compare the active
   snapshot's ordered media IDs with the controller timeline media IDs, order and count. A
   missing snapshot or any mismatch stops with a clear message and no switch. This is O(n) only
   when a chain action is invoked, not on queue updates.
4. Resolve via the pure policy.
5. On Target, call the existing `switchToNamedQueue(id)`; on Boundary/NoActive, emit the matching
   message and perform no switch.

`switchToNamedQueue` remains the only transition path. It already checks the target exists and is
non-empty, saves the outgoing named queue atomically through ADR-046 and restores the target's
resume index/position. If the target is empty/unavailable it emits the existing message and no
partial switch occurs.

### Manager UI

`AuroraQueuesManagerDialog` shows First/Previous/Next text buttons when `activeQueueId != null`
and passes three callbacks to the ViewModel. The direct row switch is unchanged. The manager list
order is the same stable summaries order. Boundary/no-active feedback uses toast messages.

### Preserved behavior

`clear_queue_on_completion` and the end-footer remain untouched. After auto-clear the queue is
empty, so the chain reports no active queue; switching via the manager makes it active and usable
again. Direct manager switching, queue ordering, swipe, search, limits and duplicate policy are
unchanged.

## Risks / Trade-offs

- `createdAt` ties are broken by id, giving a deterministic order without a new column.
- The chain stops at boundaries rather than wrapping; this is explicit and tested.
- An empty/unavailable target stops the chain and keeps the current queue; it does not skip.
- The active marker is the existing app-level identity; queue edits after a named switch still
  count as that active named queue until another switch/clear.

## Verification

- Pure `NamedQueueChainPolicy` probe: first/previous/next, boundaries, no active/missing id,
  single-queue, stable order.
- Static source probe: createdAt projection, one `created_at ASC, id ASC` order in both summaries
  queries, repository same source, active/empty guards, policy resolution, atomic switch reuse,
  manager buttons/direct switch, messages, no schema/deps.
- ktlint, `git diff --check`, OpenSpec strict. Build/runtime deferred until explicitly requested.
