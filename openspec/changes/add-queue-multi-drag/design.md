# Design: Queue multi-item drag (follow-up D)

## Context

`QueueScreen` renders the queue in effective order: when shuffle is enabled, `Player.getQueueItems`
returns the shuffle traversal. `QueuePosition.indicesInTimeline` maps every effective position to
its original timeline index, and the existing single-item drag uses that mapping before calling
`moveMediaItem` (ADR-038). Queue items are keyed by `QueueSong.key` = `(song id, occurrence)` so
duplicate media IDs remain distinct. `AdvancedForwardingPlayer.moveMediaItem` keeps sequential
timeline/upcoming ranges consistent (ADR-013), and `PersistentStorage.saveState(true)` writes the
full ordered queue (ADR-045).

## Goals / Non-Goals

**Goals:**

- True long-press/tap selection, not action buttons for moving.
- Block move preserving relative order, with deterministic before/after drop semantics.
- Timeline-index resolution only; no filtered `visibleQueue` positions in mutations.
- Preserve current media item/position, stop-after and full persisted queue order.
- Disable selection/drag when searching or locked; keep swipe/single drag outside selection.
- Handle duplicate media IDs, shuffle on/off and sequential timeline mode.

**Non-Goals:**

- New Room schema/migration, dependency or permission.
- Changing swipe removal, single-item drag, end footer, re-shuffle or named-queue behavior.
- Redesigning the queue header/footer.
- Moving a filtered subset.

## Decisions

### Selection

`QueueScreen` keeps `selectedKeys: Set<Pair<Long, Int>>` where each value is
`QueueSong.key`. Long-press on a row enters selection (or toggles when already selecting); a tap
while selection is active toggles the row instead of playing it. The selection bar shows the count,
Select all and Cancel. One selected row is allowed as an intermediate state, but block drag is
enabled only with at least two. Selection is cleared when `queueLocked` or `queueQuery` is
non-blank; selection never acts on filtered rows.

### Drop semantics

The dragged row is always one of the selected keys. For a target effective row:

- moving downward (target index greater than the selected effective maximum) inserts the block
  **after** the target;
- moving upward (target index less than the selected effective minimum) inserts it **before** the
  target;
- a target inside the selected effective range (including a selected row) is a no-op.

This matches the existing single-row convention: the target is evaluated after the selected block
is removed, so downward drops land after the target and upward drops land before it. No silent
wrap-around is introduced.

### Timeline resolution and pure planner

The ViewModel resolves every selected effective position and the target effective position with
`QueuePosition.getIndexForPosition`. It builds a timeline-key list by placing each effective row's
key at its timeline index, then calls `QueueBatchMovePolicy.move(timelineKeys,
selectedTimelineIndices, targetTimelineIndex, dropAfter)`.

The planner:

- rejects fewer than two selections, invalid/selected targets and no-op final orders;
- removes the selected keys, inserts the block at the target's post-removal index and preserves
  relative order;
- computes a sequence of single-item moves, processing the block from the last item when moving
  down and from the first item when moving up;
- returns the final timeline keys and an old-to-new index map for stop/current remapping.

The policy is Android-free and exhaustively probeable.

### Service command and state remapping

`Playback.MOVE_QUEUE_BLOCK` is app-internal. It accepts parallel from/to index arrays, validates
length/range/count, and applies repeated `player.moveMediaItem(from, to)` calls in order. It
captures the old current index, position and stop-after index first. The old-to-new index map is
computed from the same move sequence and used to:

- remap the stop-after state with SET_STOP_POSITION semantics: if the remapped target is the
  current item, set `pauseAtEndOfMediaItems = true` and `stopIndex = -1`; if it is a future item,
  keep `pauseAtEndOfMediaItems = false` and set `stopIndex` to that remapped index;
- verify/restore the current index and position if the engine did not retain them (the command does
  not call `play`/`pause`, so play/pause state is preserved).

After all moves, `persistentStorage.saveState(true)` writes the final full queue (never a filtered
subset). The existing timeline listener rebuilds `queueFlow`/`QueuePosition` from the new order.

### UI and gesture coexistence

The selection bar is a LazyColumn item; `queueContentOffset` includes both the Aurora header and
the selection bar so reorder indices still map to effective rows. Selected rows use a selection
color and the current drop target is highlighted. Only selected rows show an enabled handle when
at least two are selected; other handles are disabled. Swipe is disabled while selecting. Outside
selection, the existing single-item drag and swipe paths are unchanged. Search/lock clear the
selection immediately.

### Duplicates, shuffle and sequential timeline

Keys include the occurrence count, so duplicate media IDs are distinct. The planner uses keys, not
media IDs or filtered positions. In shuffle mode the displayed order is the effective traversal;
the command maps it to original timeline indices through `QueuePosition`, so it never uses a
shuffle-list position as a queue mutation index. `AdvancedForwardingPlayer.moveMediaItem` is the
single move path, preserving sequential/upcoming-range behavior. Process recreation uses the
existing full-queue snapshot.

## Risks / Trade-offs

- The highest risk is mixed effective/timeline indices; the ViewModel and policy never use
  `visibleQueue` and the planner is exhaustively tested.
- With shuffle on, the effective display order is controlled by the active shuffle order; device
  QA must verify the resulting playback order and duplicate identity, not only IDs.
- Moving the current item or the stop target requires index remapping; the service computes a
  fresh old-to-new map from the exact move sequence.
- Pending persistence is debounced like all queue edits; a whole-process kill immediately after a
  drop is not guaranteed to flush.
- Dropping inside a non-contiguous selection range is intentionally a no-op; the user can drop
  before or after the selected range.

## Verification

- Pure `QueueBatchMovePolicy` probe: exhaustive permutations/selections/targets/directions,
  duplicate keys, relative order, contiguous block, old-to-new map and no-op cases.
- Static source probe: keyed selection, long-press/tap, selected-only handles, search/lock disable,
  QueuePosition timeline resolution, service command/registration, repeated single-item moves,
  stop/current remap, full `saveState(true)`, swipe/single-drag preservation, main-only strings,
  no schema/deps.
- ktlint, `git diff --check`, OpenSpec strict.
- JDK 21 build and device runtime (selection gestures, drops above/below/current, duplicates,
  shuffle on/off, sequential timeline, search/lock, process recreation) are deferred until
  explicitly requested; no emulator is started by this change.
