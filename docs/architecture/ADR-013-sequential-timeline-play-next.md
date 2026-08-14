# ADR-013: Sequential Timeline and "Play Next" via AdvancedForwardingPlayer

## Status

Accepted

## Context

The app offers both "add to queue" (append) and "play next" (insert after current) semantics,
plus a sequential-timeline mode where shuffled queues behave predictably. Stock Media3
`ForwardingPlayer` does not distinguish "next" from appended items, so the app must track which
items are upcoming and keep the real timeline aligned when the user reorders or the player
advances.

## Decision

- Wrap the ExoPlayer in `AdvancedForwardingPlayer`, a `ForwardingPlayer` subclass that adds
  upcoming-range queue semantics:
  - marks MediaItems with `__is_upcoming__` / `__normal_index__` extras to distinguish
    play-next entries from regular queue entries,
  - realigns or clears upcoming ranges on position discontinuities and move operations,
  - honors the `QUEUE_NEXT_MODE` preference to drive sequential-timeline behavior.
- All queue mutations (play next, append, move, remove) go through this wrapper so the
  timeline state machine has a single owner.
- `PlaybackExtension` provides the conversions between `Song`/`MediaItem` (the `Song` travels
  inside `MediaItem.localConfiguration.tag`) and timeline traversal helpers reused by
  `PlayerViewModel`, `LibraryProvider` and `PersistentStorage`.

## Consequences

- "Play next" is reliable even while the player auto-advances (the wrapper re-tags items on
  discontinuities), which stock Media3 does not give for free.
- The wrapper couples queue semantics to the player internals; timeline bugs are localized but
  subtle (extras tagging + listener bookkeeping).
- `PersistentStorage` (ADR-012) can snapshot the effective timeline because mutations are
  centralized.
- Sequential-timeline mode (queue-next) is a preference-driven behavior layered on top of the
  same wrapper, keeping one code path.
