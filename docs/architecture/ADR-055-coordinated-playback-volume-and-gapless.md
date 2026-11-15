# ADR-055: Coordinated Playback Volume and Gapless Gate

## Status

Accepted.

## Context

Playback previously wrote `player.volume` from three places: the equalizer `volumeState`
collector, `AudioFocusController` ducking, and the sleep-timer `ValueAnimator`. Adding
configurable fade-in/out would make those writers overwrite each other. Media3 1.10.1 also
exposes no public switch for gapless trimming.

## Decision

Introduce `PlaybackVolumeController` as the only writer of `player.volume`; it multiplies
equalizer volume, an audio-focus duck factor and a fade factor. Add
`PlaybackFadeController` for 0–4000 ms ramps, with pause committed only after fade-out.
Sleep-timer fade-out uses the same controller. Wrap `DefaultAudioSink` in
`GaplessGateAudioSink`, which strips encoder delay/padding when the gapless toggle is off.

## Consequences

- The three volume concerns can no longer clobber each other, and the sleep-timer fade no longer
  bypasses the equalizer/duck state.
- Fade ramps add a short Main-thread coroutine (40 ms steps) because ExoPlayer commands must run
  on its application thread.
- Gapless changes apply at the next audio-sink configure, not retroactively. The delegating sink
  must be kept in sync with future `AudioSink` interface methods.
- Fade and gapless preferences default to the existing behavior (fade off, gapless on), so
  current playback is unchanged until the user opts in.

## Related

- ADR-014 (audio processing pipeline), ADR-015 (equalizer engines), ADR-019 (sleep timer),
  ADR-042 (playback resilience).
- `openspec/changes/playback-transport/`, `openspec/changes/merge-sleep-timer-plus/`.
