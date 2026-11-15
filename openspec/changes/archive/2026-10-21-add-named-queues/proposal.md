# Proposal: Multiple named queues and queue-local search

## Why

The app deliberately kept a single active queue and marked multiple named queues and queue
search unavailable in Aurora (ADR-036). The user has now authorized implementing both. The queue
UI already contains disabled Aurora affordances for a queues manager and a queue search bar, so
the missing work is real persistence, switching semantics and filtering.

## What Changes

- Persist named queues with their ordered media ids, resume index, position, repeat mode and
  shuffle-enabled state.
- Add save-current-queue-as, rename, delete, switch, and remove-others actions backed by Room.
- Preserve each queue's playback position when switching; keep the active queue's stored state
  current.
- Enable queue-local search in the Aurora queue and the legacy queue dialog, filtering the
  visible queue by title/artist/album while keeping the original indices for playback actions.
- Update ADR-036 availability language and the Aurora disabled controls to real behavior.

## Impact

- Affected code: new Room entities/DAOs, `SsilencceDatabase` migration, `MainModule`, a new
  named-queue repository, `PlayerViewModel`, `QueueScreen`, `QueueFragment`, strings and
  documentation.
- Docs: ADR-036 amendment, ADR-012 amendment, CODE_MAP, FEATURES, MISSING_FEATURES.
