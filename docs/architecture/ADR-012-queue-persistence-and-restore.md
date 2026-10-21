# ADR-012: Playback Queue Persistence and Restoration

## Status

Accepted

## Context

Users expect the queue, current position and shuffle order to survive process death and service
restarts. Media3 does not persist queue state itself; the queue can be large (thousands of
items), so it must be stored efficiently and restored asynchronously without blocking startup.

## Decision

- Persist the queue in Room as `QueueEntity` rows keyed by position (`order` is the primary
  key, `id` the mediaId column), so the same song can appear more than once; `QueueDao`'s
  `replaceQueue` clears and re-inserts the whole table transactionally (ADR-038).
- Persist the current index, stop position and shuffle order in SharedPreferences under
  `playback_state` (and `shuffle_order` for the order).
- Implement `ImprovedShuffleOrder`, a custom Media3 `ShuffleOrder` (`@UnstableApi`) with:
  - a shuffled `IntArray` + inverse index,
  - deterministic seeding via a persisted random seed,
  - an atomic current-index, and kotlinx-JSON serialization (`SerializedOrder`) so the exact
    shuffle state survives restarts,
  - `SerializedOrder.remap(savedToRestored)`, which compacts that permutation when restore
    filters unavailable queue positions and rejects invalid payloads.
- Restore through `PersistentStorage`:
  - asynchronous restoration with a `RestorationState` machine (`waitForRestoration`,
    `waitForMediaItems`),
  - saves debounced at 500 ms, with playlist intent retained across pause and an immediate final
    immutable snapshot on service teardown; one application-owned writer orders all queue
    writes and restoration waits for earlier writes (ADR-045),
  - saves skipped while restoring,
  - one occurrence-aware saved-to-restored map drives both the resume index and the saved
    shuffle order; surviving positions keep their relative shuffled sequence,
  - the saved shuffle order is validated as a permutation; a legacy seed-only payload is
    regenerated at the restored length and a missing order keeps the unshuffled fallback,
    while an undecodable or invalid payload disables shuffle for that session; undecodable
    payloads and invalid payloads for non-empty queues also clear the saved order; the
    restored queue is never emptied by a shuffle-order failure,
  - an empty restored queue preserves the previous setting: an absent or empty valid order
    keeps shuffle enabled with a zero-length order, while an inconsistent non-empty order
    falls back safely,
  - when the saved current item is gone, playback resumes at the item that took its place,
    with position-in-track reset to 0.
- Live removal of unavailable media items (`PlayerViewModel`) groups `QueueItem.second`
  timeline indices and removes consecutive ranges in descending order; shuffle-list
  positions are never passed to `removeMediaItems`. The pass captures one `queueTimeline`
  snapshot before reading the queue and revalidates timeline identity plus `mediaItemCount`
  on Main; a stale pass removes nothing and returns before publishing queue/position flows,
  leaving the timeline-event pass to rebuild. Non-removal passes retain their pre-existing
  transient publication behavior.
- `AdvancedForwardingPlayer` (ADR-013) is the single mutation point that feeds persistence,
  keeping the persisted timeline consistent with what the user sees.

## Consequences

- Restart/resume is seamless: the same queue order, shuffled state and position come back
  after a kill, a Bluetooth disconnect, or a fresh service start.
- A shrunken queue keeps the shuffle sequence of its surviving items; a corrupt shuffle
  payload disables shuffle for that session instead of losing the restored queue.
- Stale live-cleanup passes abandon removals and flow publication; ordinary queue rebuilds
  retain the pre-existing transient publication behavior when the timeline changes during
  the IO lookup.
- Two stores (Room + SharedPreferences) must be kept consistent; `PersistentStorage` is the
  only writer of both, which contains the risk.
- Deterministic shuffle with a persisted seed means "shuffle" is reproducible across restarts —
  a deliberate UX choice for music players.
- `@UnstableApi` surface in `ImprovedShuffleOrder` must be revisited on Media3 upgrades.
