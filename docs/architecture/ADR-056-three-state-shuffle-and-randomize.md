# ADR-056: Three-State Shuffle and Randomize

## Status

Accepted.

## Context

Media3 exposes only a boolean shuffle mode plus a `ShuffleOrder`. The app needs Musicolet's
third randomize mode, a persisted state and configurable shuffle order behavior without changing
the queue identity or the persisted queue format.

## Decision

- Model shuffle as `ShuffleModeState` (Off / Shuffle / Randomize). The player keeps
  `shuffleModeEnabled` as the Media3 projection; the app state is derived from the live order type
  and synchronized to the UI through a same-process holder plus a custom event/query command.
- Implement Randomize with a dynamic `ShuffleOrder` that picks a random next index per navigation
  and caches the pick per source index. It never materializes a permutation, so there is no shuffled
  queue order to persist or display; the queue screen remains in timeline order and stays
  reorderable.
- Persist the state in the playback-state preferences (`shuffle_state`) with the legacy
  `shuffle_mode` boolean as a migration fallback. Named queues keep their existing boolean shuffle
  flag and therefore restore standard shuffle, not randomize.
- Build standard shuffle orders with an optional ordered prefix to support "only shuffle songs
  below the current song". The default remains "shuffle all songs of each queue".
- The shuffle button cycles through the states enabled in Settings. Randomize is a bonus state; its
  order never ends with repeat off unless the below-current behavior is selected, because a random
  walk has no fixed last item.

## Consequences

- Randomize cannot be represented in the saved fixed shuffle order, and resuming a named queue or
  a session saved during randomize falls back to standard shuffle.
- The randomize order does not guarantee every song is played before a repeat. The queue screen
  shows the underlying timeline order in randomize mode.
- Reshuffle is unavailable while randomize is active and reports that there is no order to rebuild.
- The default shuffle state remains Off, so existing playback behavior is unchanged.

## Related

- ADR-018 (smart shuffle and play stats), ADR-041 (named queues), ADR-045 (ordered state).
- `openspec/changes/shuffle-plus/`.
