# Proposal: Queue multi-item drag (follow-up D)

## Why

The queue supports single-item drag reordering, but there is no way to select several songs and
move them as one block. Musicolet exposes multi-select block drag; this is the remaining queue
reorder gap and the highest-risk part of the queue sequence. The existing queue positions are
Room rows keyed by order (ADR-038), and every queue mutation must preserve duplicate media IDs,
current/stop positions and the ordered persistence path (ADR-013/045).

## What Changes

- Add a true queue selection mode keyed by `QueueSong.key` (media ID + occurrence):
  long-press enters selection, tap toggles, with count, Select all and Cancel actions.
- Drag any selected row's handle when at least two rows are selected; move the selected block as
  one unit and preserve relative order.
- Define drop semantics: the block is inserted before the target when moving upward and after the
  target when moving downward; a drop inside the selected effective range is a no-op.
- Resolve selected effective/shuffle positions to original timeline indices through
  `QueuePosition`; never mutate using filtered visible positions.
- Add a pure `QueueBatchMovePolicy` that computes the final timeline order and a sequence of
  single-item moves; process selected items from the correct end.
- Add the app-internal `Playback.MOVE_QUEUE_BLOCK` command, executing repeated `moveMediaItem`
  calls, remapping the stop-after index and current index, preserving play/pause/position, and
  saving the final full queue through `PersistentStorage.saveState(true)`.
- Disable selection/drag when the queue is locked or the search query is non-blank. Swipe removal
  and single-item drag remain available outside selection.
- Handle duplicate media IDs through occurrence-aware keys, shuffled queues through timeline
  mapping, sequential timeline mode through the existing `AdvancedForwardingPlayer` move path,
  and process recreation through the existing full-queue persistence.
- Add no schema, dependency, permission or translated-string changes; add only main English
  selection strings.

## Impact

- New: `playback/QueueBatchMovePolicy.kt`, OpenSpec change and probes.
- Modified: `Playback.kt`, `PlaybackService.kt`, `PlayerViewModel.kt`, `QueueScreen.kt`, main
  English strings, `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`.
- Unchanged: Room schema/entities/DAOs/migrations, dependencies, swipe behavior, single-item drag
  outside selection, end footer, re-shuffle, named queues and translated resources.
