# Design: Queue re-shuffle (follow-up C)

## Context

`PlaybackService` owns the ExoPlayer and already receives app-internal custom commands. The app
uses a custom `ImprovedShuffleOrder` (seeded permutation with JSON persistence) and
`PersistentStorage` already debounces and serializes the current shuffle order under
`SHUFFLE_ORDER` (ADR-012/045). `saveState()` is the existing debounced writer for queue,
position and shuffle order; when a named queue is active, its standard `PlaybackStateWriter` path
also refreshes that named queue's resume metadata (index/position/repeat/shuffle/updated_at). That
is existing ADR-041/045 behavior, not a re-shuffle-specific snapshot write. The queue screen has an
overflow menu in both the Aurora and non-Aurora appearances. No new persistence or schema is
needed.

## Goals / Non-Goals

**Goals:**

- One explicit command that creates a fresh shuffle order with a new seed.
- Preserve the current media item and playback position; change only shuffle traversal.
- Keep queue IDs/order, repeat mode, the active named-queue binding and the named snapshot's item
  list/order unchanged; the active snapshot's resume metadata may refresh through the standard
  save path.
- Persist the new order through the existing `PersistentStorage` path.
- Make shuffle-off an explicit no-op with feedback; never enable shuffle implicitly.
- Expose the action only in the queue overflow menu, hidden for an empty queue and disabled while
  shuffle is off.

**Non-Goals:**

- Multi-item drag, end-footer redesign, named-queue chaining changes.
- New dependencies, Room schema/migrations, permissions or translated strings.
- Toggling shuffle off/on as an implementation shortcut.
- Guaranteeing an identical playback position across an engine-level timeline rebuild; the
  command does not seek or replace the timeline.

## Decisions

### Dedicated command

`Playback.RESHUFFLE_QUEUE` is added to `APP_INTERNAL_COMMANDS`, so it is advertised and dispatched
only to the app UID like restore/named-queue/focus commands. The service handler is the only place
that mutates the shuffle order, which keeps controllers and UI from racing separate toggle calls.

### Fresh order starting at the current item

The handler builds:

`ImprovedShuffleOrder(firstIndex = player.currentMediaItemIndex, length = player.mediaItemCount,
randomSeed = Random.nextLong())`

`firstIndex` makes the new traversal start at the currently playing item, so the current media
item remains the same and its playback position is untouched. The order contains every timeline
index exactly once and uses a fresh seed, so the resulting traversal differs from the previous
order. The handler does not call `seekTo`, `moveMediaItem`, `removeMediaItem`, `clear`, or touch
`repeatMode`/`shuffleModeEnabled`.

### Persistence

After assigning `player.exoPlayer.shuffleOrder`, the handler calls
`persistentStorage.saveState()`, which is the only persistence call in the re-shuffle path. That
existing debounced path serializes the current `ImprovedShuffleOrder` through `SerializedOrder` and
writes `SHUFFLE_ORDER` alongside the queue and position; no direct preference write is added. If
the service is finished before the debounce, the existing `finish()` path persists the current
state.

Because a named queue may be active, the same standard path also refreshes that queue's resume
metadata through `PlaybackStateWriter`/`NamedQueueDao.updateResumeState`. This is existing
ADR-041/045 behavior and is not a re-shuffle-specific snapshot write: the named item list/order and
active binding remain unchanged. The handler adds no explicit `NamedQueueDao`,
`PlaybackStateWriter`, `updateResumeState`, `replaceItemsAndResume` or snapshot write.

### Availability and shuffle-off behavior

The pure `QueueReshufflePolicy.canReshuffle(shuffleEnabled, mediaItemCount)` returns true only when
shuffle is already enabled and the queue is non-empty. The service returns
`SessionError.ERROR_INVALID_STATE` for a shuffle-off or empty invocation and performs no mutation.
`PlayerViewModel.reshuffleQueue()` checks the same policy before sending the command and emits an
explicit message; the service's invalid-state result is translated to the same message, and other
failures use a failure message. Shuffle is never enabled as a side effect.

### Overflow-menu-only UI

Both queue overflow menus add one `MenuItem.Button.DropDown`:
visible only when the real queue is non-empty, enabled only when `QueueReshufflePolicy` allows it,
and wired to `PlayerViewModel.reshuffleQueue()`. The item is not added to the player controls, the
end footer, the queue rows or the multi-drag work. Toast feedback is observed through
`queueMessage`.

### Preserved behavior

Queue order/IDs, repeat mode, the active named-queue binding and the named snapshot's item
list/order are unchanged. The standard save path may refresh the active named queue's resume
metadata (index/position/repeat/shuffle/`updated_at`); this is existing bookmark persistence, not a
re-shuffle mutation. The named chain is untouched. The existing restore path loads the newly
persisted order. `SET_UNSHUFFLED_ORDER` and `QUEUE_NEXT_MODE` behavior are not modified; this
command is only available while shuffle is on.

## Risks / Trade-offs

- Replacing the shuffle order while playing could, in theory, affect engine traversal state; the
  command starts the order at the current index and never replaces the timeline. Device runtime
  verifies the position is retained.
- The persisted order write is debounced; the existing finish path flushes it. A whole-process
  kill immediately after the action is not guaranteed to persist (same as existing queue edits).
- The standard save path may bump the active named queue's `updated_at` and resume metadata even
  though only the shuffle order changed. This is accepted ADR-041/045 behavior; the item list/order,
  active binding and `created_at`-based manager order are unchanged.
- A disabled menu item cannot show a toast from the UI; the explicit no-op message covers the
  command/ViewModel path used by stale or programmatic invocations. The disabled state is the
  documented UI behavior when shuffle is off.
- No skip-to-next behavior is introduced: reshuffling does not change the current item, so the
  user can continue playback or manually skip.

## Verification

- Pure `QueueReshufflePolicy` probe: all four shuffle/count combinations.
- Static source probe: command constant/registration, handler guard, fresh seeded order with
  current index/length, single `shuffleOrder` assignment, `saveState()` as the only persistence
  call, no toggle/seek/timeline/repeat or explicit named-queue/snapshot writes, the documented
  standard `updateResumeState` refresh, ViewModel guards/messages, both overflow menus, main-only
  strings, and no dependency/schema changes.
- ktlint, `git diff --check`, OpenSpec strict.
- JDK 21 build and device runtime are deferred until explicitly requested; no emulator is started
  by this change.
