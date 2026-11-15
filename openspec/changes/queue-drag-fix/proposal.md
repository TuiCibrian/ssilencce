# Queue block-drag row-0 fix reconciliation

## Why

`openspec/changes/add-queue-multi-drag/tasks.md` still lists the 5.6/5.9 device failures
(row-0 downward block drag) as open. Those failures came from mapping
`LazyListItemInfo.index` through `currentQueueContentOffset`. Task 5.10 replaced that
path for block drags with a block-only `detectDragGestures` implementation, and 5.12
recorded a passing device retest that explicitly includes block-down from row 0. The
inherited tree contains that implementation unchanged.

## What Changes

- No production source change. Audit and document the inherited `QueueScreen` block-drag
  path: the source row center is read from `layoutInfo` by `QueueSong.key`, the accumulated
  drag delta selects the row under the block center, the drop target is key-resolved (never
  a LazyList index or content offset), and `moveQueueBlock` resolves effective positions to
  timeline indices through `QueuePosition` + `QueueBatchMovePolicy`.
- Close 5.6/5.9 in `add-queue-multi-drag/tasks.md` as resolved by 5.10 and the 5.12 device
  pass; keep the historical failure text for traceability.
- Defer a new device retest: no code changed, and the existing 5.12 evidence already covers
  row 0 downward. ADR-041 queue-search/index semantics are preserved.

## Impact

- Behavior: unchanged (the fix was already integrated).
- Code: none.
- Docs: a CODE_MAP note clarifying the block pointer path (applied later from
  `DOCS_DELTA.md`).
