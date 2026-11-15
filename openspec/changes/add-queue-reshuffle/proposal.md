# Proposal: Queue re-shuffle (follow-up C)

## Why

Shuffle can be on, but there is no explicit action to rebuild the current queue's random order when
the existing order is no longer wanted. Toggling shuffle off and on is not a dedicated action and
changes playback-mode state. The end-footer and named-chain work deliberately left this as
follow-up C.

## What Changes

- Add a dedicated app-internal `Playback.RESHUFFLE_QUEUE` command.
- Build a fresh `ImprovedShuffleOrder` with a new random seed, `currentMediaItemIndex` as its first
  index and the current media-item count as its length.
- Preserve the current media item, playback position, queue IDs/order, repeat mode, the named
  snapshot's item list/order and the active named-queue binding; only the shuffle traversal order
  changes. The standard save path may refresh the active named queue's resume metadata
  (index/position/repeat/shuffle/updated_at), which is existing ADR-041/045 behavior.
- Persist the new order through the existing `PersistentStorage.saveState()` path (ADR-012/045),
  without writing preferences directly, adding an explicit DAO/snapshot write, or toggling shuffle
  off/on.
- Add a pure `QueueReshufflePolicy` for availability: shuffle must already be on and the queue must
  be non-empty.
- Shuffle off is an explicit no-op with a user message through the ViewModel; the command never
  enables shuffle implicitly.
- Surface the action only in the queue overflow menu (Aurora and non-Aurora). It is hidden for an
  empty queue and disabled while shuffle is off.
- Keep multi-drag, the end footer, named-queue chaining and all other queue features unchanged.
- Add no dependency, schema, permission or translated-string changes.

## Impact

- New: `playback/QueueReshufflePolicy.kt`, OpenSpec change and probes.
- Modified: `Playback.kt`, `PlaybackService.kt`, `PlayerViewModel.kt`, `QueueScreen.kt`, main
  English strings, `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`.
- Unchanged: Room schema/DAOs/entities, Gradle dependencies, queue order/IDs, repeat mode, named
  snapshot item lists/order and active binding (the standard save path may refresh the active named
  queue's resume metadata), end footer, named chain, multi-drag and translated string resources.
