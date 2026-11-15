# Documentation delta — queue-drag-fix

## Decisions

- The row-0 downward block-drag bug is **already fixed** in the inherited tree by
  `add-queue-multi-drag` 5.10. Block drags no longer go through the reorderable callback
  that mapped `LazyListItemInfo.index` through `currentQueueContentOffset`; a block-only
  `detectDragGestures` path reads the source row center from `listState.layoutInfo` by
  `QueueSong.key`, accumulates the drag delta, key-resolves the row under it and calls
  `moveQueueBlock`.
- The 5.7/5.9 key-based reorderable path is not reinstated for block drags. It remains the
  single-item path; 5.10 is the working block path and 5.12 records a device PASS for
  block-down from row 0 (plus single drag, block-above and row-1 down).
- No production code changed in this brief. A new device retest is **deferred**: the
  implementation is unchanged from the CB06 APK already tested in 5.12.
- ADR-041 semantics are unchanged: UI selection/target indices are effective (possibly
  shuffled/filtered) positions, `PlayerViewModel.moveQueueBlock` maps them to timeline
  indices through `QueuePosition`, and `QueueBatchMovePolicy` inserts the block before the
  target when moving up and after it when moving down. Queue-local search still preserves
  original indices.

## docs/CODE_MAP.md

Update the `ui/screen/queue/QueueScreen.kt` **Notes** entry. Append:

- "Block drag with two or more selected rows uses a block-only `detectDragGestures` path:
  the dragged row center is resolved from `listState.layoutInfo` by `QueueSong.key`, the
  accumulated delta selects the row under it, and `blockDropTarget` is key-resolved (never
  a LazyList index or `queueContentOffset`); `userScrollEnabled` is disabled while the
  block drag is active. The single-item path keeps the reorderable handle."

## docs/FEATURES.md

No change. The Queue screen bullet already documents key-based multi-selection, block drag,
drop direction and ADR-041 index mapping.

## docs/MISSING_FEATURES.md

No change.

## AGENTS.md / docs/agents/domain.md / ADRs

No change. No architectural decision was made.
