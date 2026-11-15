# Proposal: Queue end footer (follow-up A)

## Why

The queue sheet cannot distinguish a finished queue from a paused one. When
`clear_queue_on_completion` is off, the ended queue remains but offers no visible Replay or Clear
action. The audit splits end-of-queue work into a small UI/ViewModel footer (A), named-queue
chaining (B), re-shuffle (C) and multi-drag (D); this change implements only A.

## What Changes

- Add `isQueueEndedFlow` to `PlayerViewModel`, true only when playback is `STATE_ENDED` and the
  queue has media items; reset on play, seek, timeline/media-item changes and empty queue.
- Add a compact queue-end footer in `QueueScreen` shown only when ended and non-empty.
- Replay restarts from logical queue position 0 (the first item of the current shuffled order when
  shuffle is on) and starts playback without changing shuffle/repeat.
- Footer Clear uses `QueueClearingBehavior.RemoveAllSongs` explicitly; the existing overflow Clear
  keeps respecting the user's keep-current preference.
- Preserve `clear_queue_on_completion` and the existing empty state. `QueueFragment` navigateUp is
  best-effort and does not close this Compose sheet; no close behavior is added.
- Do not add named-queue chaining, re-shuffle or multi-drag.

## Impact

- New: pure `QueueEndPolicy.kt`.
- Modified: `PlayerViewModel.kt`, `QueueScreen.kt`, main English strings, queue docs.
- Unchanged: Room schema, dependencies, permissions, PlaybackService auto-clear semantics,
  named-queue switching, re-shuffle and multi-drag.
