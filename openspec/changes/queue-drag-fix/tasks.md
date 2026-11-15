# Tasks: queue-drag-fix

- [x] Read the 5.6/5.9 reproduction notes and inspect every block-drag site in
  `ui/screen/queue/QueueScreen.kt`.
- [x] Confirm the inherited tree already contains the 5.10 block-only pointer path:
  `detectDragGestures` on selected row handles, source center from `layoutInfo` by
  `QueueSong.key`, accumulated delta, key-resolved `blockDropTarget`, and
  `moveQueueBlock` on drop.
- [x] Confirm no block-drag path depends on `LazyListItemInfo.index`,
  `currentQueueContentOffset` or the reorderable callback; the old offset math is absent
  from the block path.
- [x] Confirm 5.12 records a device PASS for block-down from row 0 on the CB06 APK, so the
  reported bug is already fixed. No duplicate implementation and no production source
  change were added.
- [x] Reconcile `add-queue-multi-drag/tasks.md` 5.6/5.9 as resolved by 5.10/5.12.
- [x] Record decisions and the docs delta in `DOCS_DELTA.md`.
- [x] Compile gate green (no source changes).
- [ ] Device retest deferred: covered by the existing 5.12 row-0 PASS; no code changed in
  this brief.
