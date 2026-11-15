# Docs delta — crossfade

## FEATURES.md

Section **1. Playback & Audio** — add after the **Controls** bullet:

- **Crossfade** — Optional 0–12 s equal-power overlap between tracks (default off). A secondary ExoPlayer pre-buffers the next queue item, both players run complementary volume ramps, and the main player commits the queue transition before the outgoing track ends; the media session, queue, persistence, widgets, notifications and scrobbling keep using the single main player. Optional crossfade on manual skip (capped at 4 s). Automatically skipped between consecutive tracks of the same album while gapless playback is enabled (configurable). Suppressed for repeat-one, A-B repeat, stop-after-current-song, sleep-timer stop-at-end and repeat-load-next-and-pause.

Section **9. UI, Themes & System** — replace the **Capability-aware reference controls** bullet:

- Old: `Tags/composers, manual lyrics timing, QR sharing, crossfade, mono and song-count timer remain visibly unavailable, not simulated features.`
- New: `Tags/composers, manual lyrics timing, QR sharing, mono and song-count timer remain visibly unavailable, not simulated features. Crossfade is implemented with real controls in the Sound sheet.`

## MISSING_FEATURES.md

Part 1 → **1. Playback** — remove:

- `- **Crossfade** between tracks (PRO).`

Leave the screencast bullet in Part 2 unchanged: it already describes crossfade as an app feature that must keep working during screencast.

## AURORA_REFERENCE.md

Line 32 — remove `Audio crossfade` from the unavailable list:

- Old: `- Audio crossfade, mono output and timers by number of songs.`
- New: `- Mono output and timers by number of songs.`

## CODE_MAP.md

Add these per-file entries (keep the existing Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed by / Related files / Notes format):

- `core/model/MediaItemExtras.kt` — Purpose: shared `Bundle` extra keys attached to media-item metadata. Responsibilities: album-id and disc-number keys used by the gapless-album policy. Inputs: none. Outputs: string constants. Dependencies: none. Consumed by: `data/model/Song.kt`, `playback/crossfade/GaplessAlbumDetector.kt`. Related files: `Song.toMediaItem`. Notes: extras survive queue persistence and the "upcoming" timeline wrappers.
- `playback/crossfade/CrossfadeController.kt` — Purpose: dual-player crossfade state machine. Responsibilities: schedule a one-shot preload before the end of the current item, pre-buffer the next queue item on a secondary ExoPlayer, run equal-power outgoing/incoming ramps, commit the main-player transition at the secondary position, hand off and release the secondary, abort on pause/seek/timeline/speed/queue changes, intercept manual "next" commands. Inputs: crossfade config, player state, incoming ReplayGain, mirrored processor state. Outputs: main-player seeks, crossfade volume multipliers, commit callback. Dependencies: Media3 `ExoPlayer`, `PlaybackVolumeController`, `CrossfadePlayer`, `GaplessAlbumDetector`. Consumed by: `PlaybackService`. Related files: `AdvancedForwardingPlayer`, `PlaybackFadeController`. Notes: runs on the main/application looper; only one-shot handler messages, no polling.
- `playback/crossfade/CrossfadePlayer.kt` — Purpose: factory and holder for the secondary pre-buffering ExoPlayer. Responsibilities: mirror the main renderer stack (balance, mono, ReplayGain, ALAC selector, FFmpeg extension, extractors, skip-silence, float output), own a dedicated audio-priority playback thread, share the main audio session id, run with `WAKE_MODE_NONE` and no audio focus. Inputs: context, audio session id, renderer options. Outputs: `ExoPlayer` plus processor instances. Dependencies: `SsilencceRenderersFactory`. Consumed by: `CrossfadeController`. Related files: `SsilencceRenderersFactory`. Notes: released only when the playback service is destroyed.
- `playback/crossfade/GaplessAlbumDetector.kt` — Purpose: detect album-continuous transitions. Responsibilities: same non-zero album id plus the immediate successor track (same disc) or track 1 of the next disc; unknown track numbers never count as continuous. Inputs: current and next `MediaItem`. Outputs: Boolean. Dependencies: `MediaItemExtras`. Consumed by: `CrossfadeController`. Related files: `Song.toMediaItem`. Notes: prevents crossfade from defeating gapless playback.
- `ui/screen/sound/CrossfadeSettingsCard.kt` — Purpose: Compose crossfade controls in the Sound sheet. Responsibilities: duration slider (0–12 s, 500 ms steps), crossfade-on-skip switch, keep-gapless-albums switch; reads/writes the same SharedPreferences keys as the classic playback screen. Inputs: SharedPreferences. Outputs: preference writes. Dependencies: `TitledCard`, `LabeledSwitch`, `IconifiedSliderTrack`. Consumed by: `SoundSettingsScreen`. Related files: `preferences_screen_playback.xml`. Notes: uses `stringResource`, no `context.getString`.

Update existing entries:
- `data/model/Song.kt` — `toMediaItem` now attaches album id and disc number metadata extras.
- `playback/PlaybackService.kt` — owns and wires `CrossfadeController`; maps a crossfade commit seek to an automatic transition for play counts/scrobbles/sleep timer; cancels crossfade on pause, sleep timer, A-B repeat, previous and stop-at-end; builds the secondary player and mirrors processor state.
- `playback/AdvancedForwardingPlayer.kt` — added `onSeekToNextRequested` (manual skip crossfade) and `onSeekToPreviousRequested` hooks.
- `playback/PlaybackVolumeController.kt` — added the crossfade multiplier and `currentOutputVolume`.
- `playback/processor/BalanceAudioProcessor.kt` — added `balanceSnapshot()` for mirroring into the secondary player.
- `util/Preferences.kt` — added `CROSSFADE_DURATION`, `CROSSFADE_ON_MANUAL_SKIP`, `CROSSFADE_GAPLESS_ALBUMS`, their accessors and `MAX_CROSSFADE_DURATION_MS`.

## AGENTS.md

Repository Code Map → `playback/`:

- Add: `playback/crossfade/` — `CrossfadeController` (dual-player equal-power crossfade, manual-skip interception, main-player commit), `CrossfadePlayer` (secondary ExoPlayer mirroring renderers and audio session, no wake lock/focus) and `GaplessAlbumDetector` (same-album consecutive-track policy).
- Update the `core/model/` bullet: `... plus CategoryInfo, ComposerNamePolicy, GenreSubcategory, MediaEvent, MediaItemExtras, NotPlayedDaysPolicy, PaletteColor, GridViewType.`

## docs/agents/domain.md

- Line 7: change `- `docs/architecture/ADR-*.md` - 51 ADRs, ADR-001 through ADR-051` to `- `docs/architecture/ADR-*.md` - 52 ADRs, ADR-001 through ADR-052`.
- Append to the consumer-rules paragraph: `ADR-052 adds the dual-player crossfade, keeping queue/session ownership on the main player and treating a crossfade commit as an automatic transition.`

## New ADR — create `docs/architecture/ADR-052-dual-player-crossfade.md`

```markdown
# ADR-052: Dual-Player Crossfade

## Status

Accepted.

## Context

The playback engine uses a single ExoPlayer per `PlaybackService`; the MediaSession, queue,
persistence, widgets, notifications and scrobbling all observe that one player. Users want a
configurable overlap between tracks, which a single ExoPlayer cannot produce: it can only
play one media item at a time, and its gapless path is the opposite of an overlap.

## Decision

Introduce an opt-in crossfade (0–12 s, default off) implemented with a secondary ExoPlayer:

- The main player keeps owning the queue, MediaSession, persistence, audio focus, wake lock,
  widgets, notifications, scrobbling and sleep timer. It stays the only logical player.
- A lazily created secondary `CrossfadePlayer` mirrors the renderer stack (balance, mono,
  ReplayGain, ALAC selector, extractor/seek options, skip-silence, float output), uses its own
  audio-priority playback thread, shares the main player's audio session id and runs with
  `WAKE_MODE_NONE` and audio focus disabled.
- Before the outgoing track ends, `CrossfadeController` pre-buffers the next queue item on the
  secondary player. At the fade boundary both players run complementary equal-power volume
  ramps (the main player's ramp is a new multiplier in `PlaybackVolumeController`, composed
  with the existing base/duck/fade multipliers).
- The controller commits by seeking the main player to the incoming item at the position the
  secondary already reached, then fades the secondary out over a short handoff and clears its
  media items. The queue therefore advances normally and all observers keep working.
- A commit reaches the player as a seek; the service maps that single transition to
  `MEDIA_ITEM_TRANSITION_REASON_AUTO` so play counts, Last.fm/ListenBrainz scrobbles and
  sleep-timer song counting match a normal queue advance.
- Crossfade is suppressed for repeat-one, A-B repeat, stop-after-current-song, sleep-timer
  stop-at-end, repeat-load-next-and-pause, and for same-album consecutive tracks while gapless
  playback is enabled (preference, default on).
- Manual "next" is intercepted through `AdvancedForwardingPlayer`; when the optional manual-skip
  crossfade is enabled it uses the same engine with the overlap capped at 4 seconds. A repeated
  skip aborts immediately.

## Consequences

- Two audio pipelines can exist during the overlap. Equalizer, reverb and virtualizer remain
  effective because both players share the audio session; ReplayGain for the incoming track is
  resolved off the main thread and applied before or during the fade.
- The secondary player is kept allocated after first use to avoid decoder setup cost on every
  track; it holds no wake lock, audio focus or media items while idle.
- Any pause, seek, queue/timeline change, speed change or incompatible stop policy aborts the
  crossfade and restores normal main-player volume.
- Crossfade stays off by default, so existing gapless behavior is unchanged for users who do
  not enable it.
```

## Decisions

- Manual-skip overlap is capped at 4 seconds even when the configured crossfade is longer, so
  pressing Next always responds promptly.
- The gapless-album opt-out applies to both automatic and manual crossfades.
- Crossfade duration 0 means off; the feature is off by default and preserves current behavior.
- No Room schema change.
