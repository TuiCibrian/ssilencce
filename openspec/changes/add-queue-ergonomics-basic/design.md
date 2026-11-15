# Design: Basic queue ergonomics

## Context

`QueueScreen` renders the active queue with `QueueSong.key`-stable rows, original indices in
`visibleQueue`, single-item `ReorderableItem` drag, a queue lock preference and a queue-local
query. Named queues are persisted snapshots in `NamedQueueRepository`; saving currently calls
`NamedQueueRepository.create` with an automatic suffix and does not expose duplicate choices or
limits. The active playback queue is separate (`QueueEntity`), so replacing a named snapshot must
not clear or reorder the live queue.

## Goals / Non-Goals

**Goals:**

- One safe swipe affordance with undo and original-index removal.
- Explicit Keep both / Replace / Rename for duplicates, with actual-name feedback.
- Repository-enforced named-queue count/name limits with clear errors.
- Preserve existing queue search, confirm-delete, single drag and all existing behavior.

**Non-Goals:**

- End-of-queue chaining, re-shuffle, jump to next/first queue.
- Multi-item drag/selection.
- Merge of named queues.
- Schema, DAO, dependency, permission or playback-service changes.

## Decisions

### Swipe-right removal and undo

Each queue row is wrapped in a Material3 `SwipeToDismissBox` with only
`SwipeToDismissBoxValue.StartToEnd` enabled. The gesture is enabled only when
`!queueLocked && queueQuery.isBlank() && reorderInfo == null && reorderingQueue == null`.
The callback uses the `withIndex` original queue index, calls the existing
`removePosition(index)`, then shows a Snackbar with Undo.

Undo calls `PlayerViewModel.restoreRemovedPosition(originalIndex, song)` which maps the original
logical position through the current `QueuePosition` mapping and inserts the media item before
that position; if the original index is now past the end, it appends. The existing overflow
remove action is untouched and remains the accessible fallback. No multi-drag is added.

### Named-queue naming policy

`QueueNamePolicy` is a pure object:

- `MAX_NAMED_QUEUES = 100`, `MAX_NAME_LENGTH = 80`, `DEFAULT_NAME = Queue`.
- trim/normalize, blank/length/duplicate validation (case-insensitive, except current name).
- `uniqueName` implements the existing `Name (2)`, `Name (3)` suffix rule, truncating the base
  so the suffix fits the length cap.
- `canCreate(count)` enforces the count cap.

`NamedQueueRepository` serializes create/replace/rename with a `Mutex` (no unique DB index is
added):

- `create` rejects the cap and blank/too-long names, then stores the unique suffix name.
- `replace(queueId, snapshot)` overwrites only the named queue snapshot/resume state and keeps
  the existing stored name; the active playback queue is untouched.
- `rename(queueId, requestedName, replaceQueueId)` validates the name. With a replace target, it
  deletes the target and renames the source to the requested name; without a target it stores
  the unique/requested name. Both paths run in one `database.withTransaction`.

### Manager UI

`AuroraQueuesManagerDialog` keeps the existing Save/Rename dialogs and adds a duplicate choice
dialog with Keep both (confirm), Replace existing (opens a confirmation), Rename (returns to
the name dialog) and Cancel. On Save, Replace updates the selected named queue snapshot by id. On
Rename, Replace deletes the selected existing queue and renames the current queue to that name;
the active marker is cleared only when the deleted target was active, while renaming the active
source keeps its ID. It never touches the active playback queue. "Keep both" suffixes the current queue name.

`QueueNameDialog` enforces the 80-character cap inline (`isError` + supporting text) and disables
Save for blank/too-long names. Repository exceptions map to clear toast messages, including the
100-queue limit and name-length error. No auto-delete is performed.

### Message transport

`PlayerViewModel.namedQueueMessage` changes from a resource id to a small
`NamedQueueMessage(resId, args)` event so Save/Rename can report the actual stored name
(`Saved as …`, `Replaced …`, `Renamed to …`). QueueScreen formats the event with `getString`.

### Preserved behavior

Queue search and original-index mapping, single-item drag, confirm-delete/remove-others, active
marker, named-queue switching, clear-queue behavior and the existing end-of-queue preference are
unchanged. No playback-service command is added.

## Risks / Trade-offs

- Swipe removal of the current item changes playback as the existing overflow removal does; undo
  reinserts at the original logical position but does not force playback to jump back. This is
  the same removal semantics as the existing action.
- Only one swipe removal is pending at a time; further swipes are ignored until the Undo
  Snackbar resolves. The short undo window is best-effort.
- Replace on Rename deletes the explicitly chosen duplicate and renames the source queue; it
  does not merge songs and never touches the live queue. If the deleted target was the active
  named queue, its marker is cleared.
- Naming is case-insensitive but the stored display casing is the requested/actual name.
- The cap is enforced in the repository, so alternate callers cannot bypass the UI.

## Verification

- Pure JVM `QueueNamePolicy` probe: trimming, blank/too-long/duplicate validation, suffix
  increments, truncation, count cap.
- Static source probe: swipe guard/original index/undo/fallback; repository mutex/limit/name
  enforcement; Save/Rename duplicate choices; actual-name messages; no Merge/multi-drag/chaining;
  unchanged manifest/deps/schema.
- ktlint, `git diff --check`, OpenSpec strict. Gradle/device are deferred until the detail-search
  runtime lease closes.
