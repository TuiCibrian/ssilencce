# Proposal: Headset button mapping

## Why

The playback service currently hard-codes one headset-button behavior: single press
Play/Pause, double press Next, triple press Previous, 4+ starts a new cycle, and every click
waits 300 ms. Users cannot remap gestures, change the multi-press window, or opt into a fourth
press action. Held buttons are treated as a single press and long-press mapping is absent.

The Media3 media-button callback already owns the two relevant keycodes, so this can be added
without a new receiver, service, permission or media-session architecture.

## What Changes

- Add a transport-only `HeadsetButtonAction` enum: Play/Pause, Next, Previous, Seek forward,
  Seek backward, Nothing.
- Add a pure `HeadsetButtonPolicy` and `HeadsetButtonController` for delay normalization,
  count-to-action mapping, immediate-dispatch decisions, long-press repeat handling and
  effective-delay calculation.
- Replace the service's fixed count `Runnable` with the controller while preserving today's
  defaults exactly: single Play/Pause, double Next, triple Previous, long Play/Pause, 4+ off,
  300 ms, triple immediate, hold-as-single.
- Add an opt-in 4+ Seek-forward action that performs one seek by the existing
  Rewind/Fast-forward amount; when enabled, triple waits for the configured delay.
- Add best-effort long-press mapping from key repeats. When the long action differs from the
  single action, raise the dispatch floor to 600 ms so the first repeat can cancel the pending
  click. Document the headset/AVRCP limitations.
- Add Preferences keys/getters/setters with code defaults and a reset-to-defaults helper.
- Add one Playback → Headphones settings row opening a Compose mapping dialog with four action
  rows, 4+ switch, 250/300/400/500/600 ms chips, OEM/AVRCP disclaimer, reset and close.
- Keep dedicated MEDIA_NEXT/PREVIOUS/FAST_FORWARD/REWIND events untouched. Keep only
  HEADSETHOOK and MEDIA_PLAY_PAUSE in the counted path.
- Add only main English strings. No dependencies, schema, migration, permission, receiver,
  service, translated strings or version change.

## Impact

- Affected source: `PlaybackService`, `Preferences`, settings XML, `PreferencesScreenFragment`,
  new action enum/policy/controller and Compose dialog.
- Affected docs: `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`; OpenSpec change only.
- Verification: source/OpenSpec/ktlint, compiled policy/controller probe, JDK21
  assemble+lint and a bounded API26 runtime passed. The runtime lease's
  `input keyevent --longpress 85` is intercepted by the system Assistant, so the
  hardware long-press branch could not be injected; the compiled controller probe covers
  distinct long-press and no-repeat fallback.
