# Design: Headset button mapping

## Context

`PlaybackService.onMediaButtonEvent` currently intercepts only `KEYCODE_HEADSETHOOK` and
`KEYCODE_MEDIA_PLAY_PAUSE`. It counts `ACTION_DOWN` events with `repeatCount == 0`, dispatches
immediately at three presses, otherwise posts a 300 ms runnable. The runnable maps 1/2/3 to
Play-Pause/Next/Previous. `updateEqualizerSessionState` removes the runnable on
`EVENT_IS_PLAYING_CHANGED`, which can drop a pending multi-press. `onDestroy` removes it.

The app already has a Playback → Headphones settings category, `CustomDialogPreference`
routing, and Compose preference dialogs. `App` seeds XML defaults only on a fresh preference
store, so every new key needs a code default.

## Goals / Non-Goals

**Goals:**
- Preserve today's default behavior exactly.
- Make single/double/triple/long press configurable from a safe transport-only set.
- Normalize a bounded 250-600 ms delay with a 300 ms default.
- Add opt-in 4+ one-shot Seek forward.
- Keep dedicated Next/Previous/FF/Rewind keys with Media3.
- Keep callback ownership and cleanup safe across playback-state, preference and lifecycle
  changes.
- Keep policy/controller pure and probeable.

**Non-Goals:**
- Remapping dedicated Next/Previous to FF/Rewind.
- Continuous fast-forward or a new seek-duration setting.
- Detecting a physical headset, device filtering, Assistant override or AVRCP translation.
- Changing notification/car button layout, media-session architecture or the Now Playing action
  enum.
- New permissions, receivers, services, schema, dependencies or tests.

## Decisions

### Transport-only action model

`HeadsetButtonAction` is a pure enum with six values: `PlayPause`, `Next`, `Previous`,
`SeekForward`, `SeekBackward`, `Nothing`. It intentionally excludes `NowPlayingAction` UI and
destructive actions. The settings UI maps each value to an existing action string; persistence
uses `HeadsetButtonAction.name`.

### Pure policy and controller

`playback/HeadsetButtonPolicy.kt` contains no Android imports:

- `HeadsetButtonConfig`: single/double/triple/long actions, `fourPlusSeekForward`, normalized
  `delayMs`, and `longPressIsDistinct` (`long != single`).
- `HeadsetButtonPolicy`: delay constants/values, `normalizeDelay` (only 250/300/400/500/600,
  otherwise 300), `actionForCount`, `shouldDispatchImmediately`, and `effectiveDelayMs`
  (`max(configured, 600)` only when long press is distinct).
- `HeadsetButtonController`: click count and long-press latch. It returns a
  `HeadsetButtonDecision`:
  - `onDown(config)` increments, returns an immediate `Dispatch` when the count reaches the
    legacy/4+ threshold, otherwise `Schedule(effectiveDelay)`.
  - `onRepeat(config)` dispatches the long action only when it is distinct, no long action has
    fired and a click is pending; otherwise consumes the repeat. Repeats never become clicks.
  - `onScheduledDispatch(config)` consumes the pending count and returns its action.
  - `reset()` clears the count and latch on preference change/destroy.
  Immediate dispatch resets the controller, so a fourth press after a triple starts a new
  cycle. After a dispatched action, a late repeat cannot fire a long action because no click is
  pending.

### Service integration

`PlaybackService` reads a fresh `HeadsetButtonConfig` from `Preferences` per event
(SharedPreferences is in-memory). It keeps one main-thread `Runnable` and the controller:
- non-target keycodes fall through to `super.onMediaButtonEvent`;
- `ACTION_DOWN repeatCount == 0` calls `onDown`;
- `ACTION_DOWN repeatCount > 0` calls `onRepeat`;
- `ACTION_UP` is consumed;
- `Dispatch` removes the pending callback and executes; `Schedule` removes and reposts;
  `Consumed` leaves a pending scheduled click alone (hold-as-single default).
- `PlayPause` uses `pausePlayback()` for the pause branch to preserve audio-focus ownership;
  Next/Previous use `seekToNext`/`seekToPrevious`; Seek forward/back use `seekForward`/`seekBack`
  (the existing seek increment); Nothing is a no-op.
- If the player is not initialized, dispatch is a no-op.
- Any headset preference change removes the callback and resets the controller, so a burst
  cannot mix old and new settings.
- `onDestroy` resets the controller/callback. `updateEqualizerSessionState` no longer cancels
  headset clicks; playback-state changes must not drop a pending gesture.

### Defaults and timing matrix

| Config | 1 | 2 | 3 | 4 |
|---|---|---|---|---|
| Default (long same, 4+ off) | single after 300 | double after 300 | triple immediately | new single |
| Long same, 4+ on | single after delay | double after delay | triple after delay | Seek forward immediately |
| Long distinct, 4+ off | single after max(delay,600) | double after max(delay,600) | triple immediately | new single |
| Long distinct, 4+ on | single after max(delay,600) | double after max(delay,600) | triple after max(delay,600) | Seek forward immediately |

Default long press is Play/Pause, equal to single, so the guard is not applied and a held
button remains today's single press. Choosing a different long action opts into the 600 ms
floor. If the headset does not report repeats, the same press falls back to the scheduled
single/double/triple action.

### Preference keys and defaults

Default SharedPreferences, no DataStore/Room:

| Key | Type | Default |
|---|---|---|
| `headset_single_click_action` | enum name | `PlayPause` |
| `headset_double_click_action` | enum name | `Next` |
| `headset_triple_click_action` | enum name | `Previous` |
| `headset_long_press_action` | enum name | `PlayPause` |
| `headset_four_plus_seek_forward` | Boolean | `false` |
| `headset_click_delay_ms` | String | `"300"` |

`Preferences` exposes typed getters/setters and `resetHeadsetButtonMapping()`. Invalid enum
names fall back through `enumValue`; delay strings normalize to 300 unless they are exactly one
of the five supported values.

### Settings UI

A `CustomDialogPreference` row in Playback → Headphones opens
`HeadsetButtonMappingDialog` (ComposeView in a `DialogFragment`). The dialog shows:
- title and OEM/AVRCP/long-press disclaimer;
- Single/Double/Triple/Long rows opening a nested radio list of the six actions;
- a whole-row 4+ switch using the existing Rewind/Fast-forward amount;
- 250/300/400/500/600 ms `FilterChip`s in a `selectableGroup`;
- Reset to defaults and Close.

Persistence is immediate. The fragment updates the row summary on open and on preference
changes. The settings search index picks up the titled row automatically. Action rows use
radio semantics and 48 dp targets; the switch uses `Role.Switch`.

### Compatibility and safety

Only HEADSETHOOK/PLAY_PAUSE are counted; a headset that emits dedicated Next/Previous or
explicit Play/Pause bypasses the counter. AVRCP/OEM stacks may translate presses differently,
debounce, or not report repeats; Assistant/calls/other sessions may own the button. Long press
is best-effort and documented. The app makes no universal-support claim and does not filter by
device.
