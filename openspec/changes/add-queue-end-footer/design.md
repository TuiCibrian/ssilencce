# Design: Queue end footer (follow-up A)

## Context

Media3 reports `Player.STATE_ENDED` after the last item finishes. `PlayerViewModel.onEvents`
already observes playback-state and timeline events, but only publishes `isPlayingFlow`, which
is also false while paused. `QueueScreen` has a pinned search bar and a normal LazyColumn; there
is no queue-level ended affordance. `clear_queue_on_completion` is handled by the service and
must remain unchanged. Footer Clear must not reuse the user's keep-current clear preference.

## Goals / Non-Goals

**Goals:**

- A real ended state that is not inferred from `isPlaying == false`.
- A footer only for an ended, non-empty queue.
- A dedicated Replay restart and an explicit full Clear.
- Preserve auto-clear, the existing empty state (best-effort navigateUp; no new close behavior),
  repeat/shuffle and existing overflow behavior.

**Non-Goals:**

- Named-queue next/previous/first chaining.
- Re-shuffle command.
- Multi-item drag/selection.
- Playback service commands or schema/dependency changes.

## Decisions

### Ended state

`QueueEndPolicy.isEnded(playbackStateEnded, mediaItemCount)` returns true only when the state is
`STATE_ENDED` and the item count is positive. `PlayerViewModel` owns
`_isQueueEndedFlow`/`isQueueEndedFlow`:

- `onEvents` recomputes it on playback-state/is-playing/play-when-ready events and resets it on
  timeline/media-item transitions and position discontinuity.
- Explicit resets happen only after a restart or effective seek is issued: play/toggle at ended
  restarts the **displayed current item** (`position.current` mapped through `QueuePosition`,
  validated against `mediaItemCount`, falling back to the controller's current timeline index),
  `playSongAt` validates the mapped row against `mediaItemCount`, seeks and plays regardless of
  `STATE_READY`, and next/previous/forward/seek-to guard no-op cases (no next item, no previous,
  forward at the end, same ended position). `playMediaId` relies on the timeline-change reset
  rather than clearing early.
- An empty queue therefore never reports ended.

### Footer

`QueueScreen` collects `isQueueEndedFlow`. A single LazyColumn `item(key = "queue_end_footer")`
is emitted only when `isQueueEnded && playQueue.isNotEmpty()`. It uses a card with
`Queue finished`, a short summary, Replay and Clear. The pinned search bar and locate-current
button remain unchanged.

### Replay semantics

Replay uses logical queue position 0 and does not change repeat or shuffle settings:

```kotlin
val firstIndex = QueueEndPolicy.replayTimelineIndex(position.getIndexForPosition(0))
controller.seekToDefaultPosition(firstIndex)
controller.play()
```

"First" is the first item of the current effective queue order. With shuffle off that is timeline
index 0; with shuffle on it is the first item of the current shuffled order (mapped through
`QueuePosition`). The mapped index is validated against `controller.mediaItemCount`; an unset or
out-of-range value falls back to timeline index 0, avoiding `IllegalSeekPositionException` from a
stale position snapshot. The generic Play/Pause action is not used as the replay contract: at an
ended queue it restarts the displayed current item (logical `position.current` mapped through
`QueuePosition`; under shuffle this may differ from the raw controller timeline index), while
footer Replay restarts at logical position 0. An invalid mapped current falls back to the
controller's current timeline index, then to 0.

### Clear semantics

The footer Clear calls `clearQueue(QueueClearingBehavior.RemoveAllSongs)`, explicitly bypassing
the user's keep-current preference. The existing overflow Clear still calls `clearQueue()` with
the default preference. Clearing produces an empty queue and the existing empty state;
`QueueFragment` navigateUp is best-effort and does not close this Compose sheet, so no new close
behavior is added. `clear_queue_on_completion` remains service-owned and untouched; when it
clears at completion the queue is empty and the footer cannot survive.

### Repeat / shuffle

The footer only appears when Media3 is genuinely ended. Repeat all/one normally prevents that
state; if ended does occur, Replay keeps the current repeat/shuffle settings and only seeks/plays.
No named queue is loaded, no shuffle order is rebuilt and no multi-drag is added.

## Risks / Trade-offs

- Replay of the current shuffled order may not equal timeline index 0; this is intentional and
  documented in the footer action and code.
- `STATE_ENDED` can repeat; the flow is idempotent and the footer is item-keyed.
- Auto-clear on completion leaves the existing empty state; the Compose sheet may remain open
  (pre-existing behavior) and no close behavior is added.
- If `QueuePosition` is stale, replay falls back to timeline index 0 rather than doing nothing.

## Verification

- Pure `QueueEndPolicy` probe: ended/non-empty, paused, empty, footer visibility, replay mapping.
- Static source probe: ended flow/resets, row-tap-at-ended, generic Play/toggle restart,
  no-op-next/forward guards, footer guard, replay seek/play, explicit full clear, preserved
  auto-clear/existing empty state (no new close behavior), no named chain/re-shuffle/multi-drag,
  unchanged deps/schema.
- ktlint, `git diff --check`, OpenSpec strict. Gradle/device deferred until the lease is available.
