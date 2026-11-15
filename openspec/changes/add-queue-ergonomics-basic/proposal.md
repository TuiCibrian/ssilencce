# Proposal: Basic queue ergonomics

## Why

The queue supports single-item drag reordering, queue-local search and named-queue snapshots,
but removing a row requires the overflow menu, duplicate named-queue names are silently
suffixed without a choice, and named queues have no count or name-length limits. The audit
separates these bounded UI/data gaps from the larger queue-timeline work (end-of-queue
chaining, re-shuffle/jump, multi-item drag), which remains out of scope.

## What Changes

- Add swipe-right-to-remove to `QueueScreen` only when the queue is unlocked, the search query
  is blank and no reorder is active. Remove by the row's original queue index, show a snackbar
  with Undo, and keep the existing overflow removal as the accessible fallback.
- Add an explicit named-queue duplicate policy on Save/Rename:
  - Keep both (default) uses the existing case-insensitive suffix rule and reports the actual
    stored name.
  - Replace existing requires confirmation and overwrites only the named snapshot; the active
    playback queue is never replaced or cleared.
  - Rename returns to the name field with validation.
  - No Merge.
- Enforce a maximum of 100 named queues and 80 characters per name in both the repository and
  the manager UI, with clear errors and no auto-delete.
- Preserve existing named queues, queue search, confirm-before-delete, single-item drag,
  clear-queue behavior and the existing end-of-queue preference.

## Impact

- Modified: `QueueScreen.kt`, `PlayerViewModel.kt`, `NamedQueueRepository.kt`,
  `values/strings.xml`, queue docs.
- New: pure `QueueNamePolicy.kt`.
- Unchanged: Room schema/entities, DAO, playback service, dependencies, permissions,
  end-of-queue behavior and multi-drag. No Merge is added.
