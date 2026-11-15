# Design: Multiple named queues and queue-local search

## Context

`QueueEntity` is the single active queue snapshot (position key + media id) restored by
`PersistentStorage`. `PlayerViewModel` owns queue flows and calls the MediaController. The
Aurora queue screen already renders a queues-manager dialog and a search bar, both disabled.

## Goals / Non-Goals

**Goals:**
- Named queues survive restarts and can be created, renamed, deleted and switched.
- Switching preserves the current queue state and restores the target queue contents and
  position.
- Queue search filters the visible queue without changing playback or queue contents.

**Non-Goals:**
- End-of-queue automation between named queues, max-queue auto-deletion, or queue export.
- Persisting the exact shuffle permutation per named queue; shuffle mode is preserved and a new
  permutation is generated when needed. This limitation is documented.
- Replacing the active queue store (`QueueEntity`) with named queues.

## Decisions

- **Separate named-queue storage.** Add `NamedQueueEntity(id, name, createdAt, updatedAt,
  resumeIndex, resumePositionMs, repeatMode, shuffleEnabled)` and
  `NamedQueueItemEntity(queueId, order, mediaId)` with composite key `(queueId, order)`. The
  active queue remains in `QueueEntity` and the existing restore path.
- **Active binding.** Persist `activeNamedQueueId` and update that named queue's saved items and
  resume state whenever the active queue changes or playback is saved. An unsaved active queue
  keeps the binding unset.
- **Switching is explicit.** `PlayerViewModel.switchToNamedQueue(id)` persists the current
  binding first, loads the target rows, resolves media ids through the existing repository,
  replaces the queue with the target start index/position, restores repeat/shuffle modes, and
  sets the new binding. Missing media are skipped; an empty result leaves the current queue
  intact and reports a message.
- **Save current queue as named.** Snapshot the controller's media ids, current index, position,
  repeat and shuffle state in one transaction; duplicate names get a numbered suffix instead of
  being overwritten.
- **Queue-local search is view state.** The Compose queue screen filters its resolved
  `QueueSong` list by title/artist/album; taps map back through the original queue index. Search
  is disabled while reordering. The legacy dialog uses the same shared composable surface.
- **Availability update.** ADR-036 no longer lists named queues or queue search as unavailable;
  the disabled labels and future strings are removed from the active UI path.

## Risks / Trade-offs

- Queue switching replaces a large timeline; resolution is IO-bound but runs off the main thread.
- The exact shuffle permutation per queue is not stored; the mode is restored with a fresh
  permutation. Documented as a limitation.
- The active queue and named queues are two stores; updates happen through one repository path
  to keep them coherent.

## Migration Plan

- Room v7 to v8 creates the two named-queue tables additively; existing active-queue rows are
  untouched. No data is backfilled because no named queues existed.
