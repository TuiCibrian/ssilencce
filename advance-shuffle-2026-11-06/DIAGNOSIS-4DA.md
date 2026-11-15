# DIAGNOSIS — 4DA AA-05 current-item/position loss

Read-only diagnosis. No source edit, build or runtime was performed for this document.

## Observed failure

Candidate `4DA13E67CA0CB7E9B387FC1656A644750B69F89D552B6668C7738F157D2ADCCC` (53,479,204 B), API 30, 21-song queue including a 30 s `AA05 Long Position` fixture.

| | Before switch | After `Most played album artists` |
|---|---|---|
| State | `3` playing | `3` playing |
| Current item | `AA05 Long Position` | `AA05 B NoComposer Control 02` |
| Position | `2,921 ms` | `0 ms` |
| Queue | 21 | 21 unique |
| Post-switch queue index | — | `AA05 Long Position` at index 5; `AA05 B NoComposer Control 02` at index 6 |

The intended current item is present in the new queue, at index 5, but playback landed on the next item (index 6) at position 0. This is a real index/position application failure, not a missing-item fallback.

## Code path

1. `PlayerViewModel.kt:1014-1016` captures `currentMediaId`, `currentPosition` and `wasPlaying`.
2. `PlayerViewModel.kt:1017-1022` **enables shuffle before replacing the timeline** and sends `Playback.SET_UNSHUFFLED_ORDER`.
3. `PlaybackService.kt:878-882` handles that command by setting:
   - `hasSetUnshuffledOrder = true`
   - `player.exoPlayer.shuffleOrder = UnshuffledShuffleOrder(player.mediaItemCount)`
   - The count here is the **old** queue (21). This is an order for the old timeline, not the new weighted list.
4. `PlayerViewModel.kt:1027-1039` computes `currentIndex` against the **new** `mediaItems` list, calls `controller.setMediaItems(mediaItems, currentIndex, currentPosition)`, `prepare()`, then `playWhenReady = wasPlaying`.
5. `PlaybackService.kt:704-726` handles the asynchronous `setMediaItems` callback:
   - At line 712, because `hasSetUnshuffledOrder == true`, it **does not create a new `ImprovedShuffleOrder`** for the new list.
   - At line 721 it attempts `(exoPlayer.shuffleOrder as? ImprovedShuffleOrder)?.playerIndex = startIndex`. The active order is `UnshuffledShuffleOrder`, so this is a **no-op**.
   - Line 724 clears `hasSetUnshuffledOrder` before the new list/order is actually bound to the requested start index.
6. `PlaybackService.kt:979-983` (`onTimelineChanged`) persists the resulting timeline. This records/confirms the post-advance state; it does not cause the loss.

## Root cause

**The start index is computed against the new weighted list, but it is applied while the player is already shuffle-enabled with an `UnshuffledShuffleOrder` built for the old queue; `onSetMediaItems` then skips binding the start index (only `ImprovedShuffleOrder` receives `playerIndex`) and clears the flag, so Media3 applies the saved 2,921 ms offset to the wrong item. That item is a 2 s fixture, finishes immediately, and playback advances to the next item at position 0.**

In other words: **ordering/state mismatch** — the new list index is passed through a MediaController command after shuffle/order state was changed for the old timeline, and the service only binds the index to `ImprovedShuffleOrder`, not the `UnshuffledShuffleOrder` it installed.

Secondary race (not the cause in this 30 s test, but real for short tracks): `currentPosition` is captured before the `SET_UNSHUFFLED_ORDER` round trip and before `setMediaItems` is applied. An auto-transition during that gap makes the captured ID/position stale.

## Minimal concrete fix

The safest minimal change is to apply the new timeline **while shuffle is off (or with the old shuffle state suspended)**, then restore shuffle/identity order after the start index and position are committed.

### `PlayerViewModel.kt:1017-1039`

- Capture `wasShuffle = controller.shuffleModeEnabled`.
- Do **not** send `SET_UNSHUFFLED_ORDER` or enable shuffle before `setMediaItems`.
- Temporarily set `controller.shuffleModeEnabled = false` (if it was on).
- `controller.setMediaItems(mediaItems, currentIndex, currentPosition)`.
- `controller.prepare()`.
- Send `SET_UNSHUFFLED_ORDER` **after** the new timeline is installed (so `PlaybackService.kt:880` uses the new `mediaItemCount`).
- Restore `controller.shuffleModeEnabled = wasShuffle` (or `true` for a special shuffle).
- Set `controller.playWhenReady = wasPlaying` last.

### `PlaybackService.kt:704-726` and `878-882`

- Do not build the order for the old queue in the command handler. Store a pending flag/start index, or build it in `onSetMediaItems` **after** the resolved `mediaItems` list is available:
  - `UnshuffledShuffleOrder(mediaItems.size)`, or
  - `ImprovedShuffleOrder(firstIndex = startIndex, length = mediaItems.size, randomSeed = Random.nextLong())`.
- When `hasSetUnshuffledOrder` is true, bind `startIndex` to the new order (do not rely on the `as? ImprovedShuffleOrder` no-op at line 721).
- Clear `hasSetUnshuffledOrder` only after the new list/order is installed.

This keeps the weighted order intact, preserves the current media item and position, and avoids applying a long-track offset to a short item.

### Optional hardening

`AdvancedForwardingPlayer.kt:26-37` and `334-351`: if `sequentialTimelineEnabled` is on, the position-discontinuity path can call `onClearUpcomingRange()` and replace the whole timeline. Verify the shuffle switch does not trip this path; if it can, suppress upcoming-range mutation during `openSpecialShuffle` or apply the preservation seek after the range cleanup completes.

## Evidence

- `evidence-4da/preservation_playing.json`
- `evidence-4da/ms_before_play_switch.txt`
- `evidence-4da/ms_after_play_switch.txt`
- `evidence-4da/db/before_play.db`, `db/after_play.db`
- `evidence-4da/after_play_switch.png`
- `evidence-4da/logcat.txt`
- `evidence-4da/preservation_paused.json` (paused attempt, `state=7` source error — inconclusive)
- `RUNTIME-4DA.md`
