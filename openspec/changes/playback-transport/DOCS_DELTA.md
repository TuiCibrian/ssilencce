# Docs delta — playback-transport

## Decisions

- Fade ramps run as a `serviceScope` (Main looper) coroutine, not on the dedicated
  `Ssilencce-ExoPlayer` thread: ExoPlayer requires `setVolume` on its application thread,
  which is the service process Main thread. A 40 ms step for at most 4 s is negligible work.
- Gapless is disabled by wrapping `DefaultAudioSink` in `GaplessGateAudioSink`, which strips
  `Format.encoderDelay`/`encoderPadding` (and passes 0 to `setOffloadDelayPadding`) when the
  toggle is off. Media3 1.10.1 has no public gapless switch. The change applies from the next
  audio-sink configure (normally the next track or session), not to an already configured
  stream.
- `AudioFocusController` no longer writes `player.volume` directly; ducking, equalizer volume
  and fades are multiplied by `PlaybackVolumeController` so they cannot overwrite each other.
- The legacy `rewind_with_back` preference is migrated at app start into
  `previous_button_action` (`rewind_with_back=false` → `previous`); it is still read as a
  fallback. The legacy `seek_interval` is copied into both `seek_forward_interval` and
  `seek_back_interval`, and remains a read fallback.
- `AdvancedForwardingPlayer.seekToPreviousMediaItem()` now follows the previous-button
  preference as well, so the notification/Android Auto "previous item" command restarts the
  current song after 5 s instead of always changing track. The UI `seekToPrevious()` guard
  no longer blocks a restart on the first queue item.
- Fast-forward/rewind is exposed as a long-press gesture (all XML player styles, Aurora) and
  as cover double-tap/gesture actions. When the current track is below the configured
  minimum duration, the long-press action/label is removed in Aurora, the view-model seek
  methods no-op, dedicated headset FF/RW key events are consumed and mapped to
  next/previous, and configured headset Seek forward/backward actions also map to
  next/previous.
- `AudioFocusController` arms the same fade-in before its internal play calls when the user
  confirms the Ask dialog or playback resumes on focus gain, so those flows are not abrupt.
- The media-session `Player.Commands` are intentionally not filtered for the minimum-duration
  gate; no player style or shipped notification exposes separate seek buttons. Documented
  here instead of changing controller command availability.

## docs/FEATURES.md

Add to **1. Playback & Audio** after the "Session recovery" bullet:

- **Fade-in/fade-out** — Optional 0–4000 ms volume ramps on play and pause (0 = off). A single
  `PlaybackVolumeController` composes the equalizer volume, audio-focus ducking and the fade
  multiplier, and the sleep-timer fade-out uses the same ramp. Pause is applied only when the
  fade-out completes; play/pause commands cancel an in-progress ramp.
- **Gapless playback toggle** — On by default; when off, `GaplessGateAudioSink` strips the
  encoder delay/padding (including offload delay/padding) so the untrimmed audio is played.

Replace the **Controls** bullet with:

- **Controls** — Previous-button behavior (restart the current song after 5 s or always play
  previous; applies to UI, notification and headset), independent fast-forward and rewind
  durations (5–60 s, legacy seek interval migrated), fast-forward/rewind only for long audio
  (off / 10 / 20 / 30 / 60 minutes; short-track headset seek falls back to next/previous),
  mini-player swipe-to-skip, extra controls, adaptive/circle/animated buttons.

## docs/MISSING_FEATURES.md

Remove these bullets from Part 1 §1:

- `- **Gapless playback** — experimental toggle (default on).`
- `- **Fade-in on play / fade-out on pause** — configurable duration (0–4000 ms each).`
- `- **"FF/Rewind only for long audio (audiobook/podcast)"** option.`

Replace `- **Previous-button behavior** — ...` with:

- **Previous-button behavior** — implemented as restart-after-5-seconds / always-previous and
  applied to UI, notification and headset; the dedicated Next/Previous-override-to-FF/Rewind
  mapping remains missing.

Remove from Part 1 §9:

- `- **FF/Rewind button visibility** — always / only for long audio.`

Replace Part 2 §4 ("FF/Rewind & track navigation") with:

```markdown
## 4. FF/Rewind & track navigation

- Musicolet: configurable FF/Rewind durations, next/prev button override to FF/Rewind for
  headsets/remotes, FF/Rewind visible only for long audio.
- ssilencce: independent fast-forward/rewind durations, long-audio-only gating with
  next/previous fallback for headset seek commands, previous-button restart/always-previous
  behavior, and configurable headset single/double/triple/long-press mapping with optional
  4+ Seek forward; the remaining gap is dedicated Next/Previous override to FF/Rewind.
```

## docs/CODE_MAP.md

Add entries next to the other `playback/` entries:

- `playback/PlaybackVolumeController.kt` — Single owner of the player output volume.
  - **Purpose:** Combine equalizer volume, audio-focus ducking and playback fade factors.
  - **Responsibilities:** Hold base/duck/fade multipliers and write their product to `Player.volume`.
  - **Inputs:** Equalizer `volumeState`, `AudioFocusController` duck changes, fade controller ramps.
  - **Outputs:** Player volume updates.
  - **Dependencies:** Media3 `Player`.
  - **Consumed by:** `PlaybackService`, `PlaybackFadeController`, `AudioFocusController`.
  - **Related files:** `PlaybackFadeController.kt`, `AudioFocusController.kt`, `PlaybackService.kt`.
  - **Notes:** All calls are on the player application thread (Main).

- `playback/PlaybackFadeController.kt` — Fade-in on play and fade-out on pause.
  - **Purpose:** Run short volume ramps and defer pause until fade-out completes.
  - **Responsibilities:** Arm silent start, ramp from actual playback start, delay/commit pause, cancel on newer play/pause/media-item transitions, expose a sleep-timer fade-out path.
  - **Inputs:** Duration providers, `PlaybackVolumeController`, play/pause requests from `AdvancedForwardingPlayer`.
  - **Outputs:** Volume-ramp steps; deferred pause commits.
  - **Dependencies:** Kotlin coroutines, `SystemClock`, Media3 player callbacks.
  - **Consumed by:** `PlaybackService`, `AdvancedForwardingPlayer` hooks.
  - **Related files:** `PlaybackVolumeController.kt`, `AdvancedForwardingPlayer.kt`, `SleepTimer.kt`.
  - **Notes:** 40 ms steps, service-scope cancellation; 0 ms keeps the legacy immediate pause.

- `playback/renderer/GaplessGateAudioSink.kt` — Delegating `AudioSink` that disables gapless trimming.
  - **Purpose:** Provide a gapless playback toggle on Media3 1.10.1, which has no public switch.
  - **Responsibilities:** Strip `Format.encoderDelay`/`encoderPadding` on `configure` and zero `setOffloadDelayPadding` when gapless is disabled; pass every other sink call through.
  - **Inputs:** Delegate `AudioSink`, gapless preference provider.
  - **Outputs:** Unchanged or untrimmed configure calls.
  - **Dependencies:** Media3 `AudioSink`, `DefaultAudioSink`, `Format`, `AudioCapabilities`.
  - **Consumed by:** `SsilencceRenderersFactory`.
  - **Related files:** `SsilencceRenderersFactory.kt`, `preferences_screen_playback.xml`.
  - **Notes:** Applies from the next configure (next track/session); offload delay/padding is zeroed too.

Update existing entries:
- `playback/PlaybackService.kt` — add `PlaybackVolumeController`/`PlaybackFadeController` ownership, fade prefs, split seek intervals, long-audio seek gating, previous-button mode and gapless provider to Responsibilities/Dependencies/Notes.
- `playback/AdvancedForwardingPlayer.kt` — add play/pause fade hooks, `pauseWithoutFade()`, `previousButtonRestartsTrack` and `seekToPreviousMediaItem()` preference routing.
- `playback/AudioFocusController.kt` — ducking now delegates to `PlaybackVolumeController`; no direct `player.volume` writes; internal confirmed-Ask/focus-gain play calls arm the fade-in through an `onPlaybackStart` callback.
- `playback/renderer/SsilencceRenderersFactory.kt` — wraps `DefaultAudioSink` in `GaplessGateAudioSink` with the gapless provider.
- `util/Preferences.kt` — new transport keys and legacy migration.
- `ui/screen/player/PlayerViewModel.kt` — seek methods honor the long-audio minimum.
- `App.kt` — calls `Preferences.migrateLegacyPlaybackPreferences()` after first-run defaults.

## AGENTS.md

In the `playback/` section of the Repository Code Map, extend the queue/playback sweep with:
`PlaybackVolumeController` + `PlaybackFadeController` (fade-in/out and duck/equalizer volume composition), and `renderer/GaplessGateAudioSink` (gapless trimming toggle).

## docs/agents/domain.md

Bump the ADR count from the current value to include ADR-052 (next free number; adjust if a
different ADR is created first).

## ADR-052 — Coordinated playback volume and gapless gate

**Status:** Accepted

**Context:** Playback previously wrote `player.volume` from three places: the equalizer
`volumeState` collector, `AudioFocusController` ducking, and the sleep-timer `ValueAnimator`.
Adding configurable fade-in/out would make those writers overwrite each other. Media3 1.10.1
also exposes no public switch for gapless trimming.

**Decision:** Introduce `PlaybackVolumeController` as the only writer of `player.volume`; it
multiplies equalizer volume, an audio-focus duck factor and a fade factor. Add
`PlaybackFadeController` for 0–4000 ms ramps, with pause committed only after fade-out.
Sleep-timer fade-out uses the same controller. Wrap `DefaultAudioSink` in
`GaplessGateAudioSink`, which strips encoder delay/padding when the gapless toggle is off.

**Consequences:** The three volume concerns can no longer clobber each other, and the
sleep-timer fade no longer bypasses the equalizer/duck state. Fade ramps add a short
Main-thread coroutine (40 ms steps) because ExoPlayer commands must run on its application
thread. Gapless changes apply at the next audio-sink configure, not retroactively. The
delegating sink must be kept in sync with future `AudioSink` interface methods.
