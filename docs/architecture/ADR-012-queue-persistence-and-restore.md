# ADR-012: Playback Queue Persistence and Restoration

## Status

Accepted

## Context

Users expect the queue, current position and shuffle order to survive process death and service
restarts. Media3 does not persist queue state itself; the queue can be large (thousands of
items), so it must be stored efficiently and restored asynchronously without blocking startup.

## Decision

- Persist the queue in Room as `QueueEntity` rows (`id` mediaId + `order`), managed by
  `QueueDao` with transactional replace, ordered reads and pruning of missing ids.
- Persist the current index, stop position and shuffle order in SharedPreferences under
  `playback_state` (and `shuffle_order` for the order).
- Implement `ImprovedShuffleOrder`, a custom Media3 `ShuffleOrder` (`@UnstableApi`) with:
  - a shuffled `IntArray` + inverse index,
  - deterministic seeding via a persisted random seed,
  - an atomic current-index, and kotlinx-JSON serialization (`SerializedOrder`) so the exact
    shuffle state survives restarts.
- Restore through `PersistentStorage`:
  - asynchronous restoration with a `RestorationState` machine (`waitForRestoration`,
    `waitForMediaItems`),
  - saves debounced at 500 ms,
  - saves skipped while restoring, and the restored shuffle order length is validated.
- `AdvancedForwardingPlayer` (ADR-013) is the single mutation point that feeds persistence,
  keeping the persisted timeline consistent with what the user sees.

## Consequences

- Restart/resume is seamless: the same queue order, shuffled state and position come back
  after a kill, a Bluetooth disconnect, or a fresh service start.
- Two stores (Room + SharedPreferences) must be kept consistent; `PersistentStorage` is the
  only writer of both, which contains the risk.
- Deterministic shuffle with a persisted seed means "shuffle" is reproducible across restarts —
  a deliberate UX choice for music players.
- `@UnstableApi` surface in `ImprovedShuffleOrder` must be revisited on Media3 upgrades.
