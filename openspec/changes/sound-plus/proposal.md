# sound-plus

## Why

The sound-processing stack still lacks several practical listening controls that other
players expose: a mono downmix for single-speaker/one-earbud listening, an environment
reverb for headphone listening, and the remaining ReplayGain controls (embedded-tag
switch, clipping/headroom control and constant-volume conversion).

## What Changes

- Add a **Mono output** switch backed by a new PCM16 audio processor in
  `playback/processor/` injected next to the balance and ReplayGain processors.
- Add an **EnvironmentalReverb** controller attached to the player audio session, with
  six presets and custom room/decay/reflection/reverb/diffusion/density parameters,
  exposed from the Equalizer screen. The effect is created, enabled and released with the
  audio session.
- Add ReplayGain extras in the Equalizer screen: **Use embedded tags**, **Prevent
  clipping**, a **Headroom** slider and a **Constant volume** mode with its own gain.
- Surround: covered by the existing Android `Virtualizer` effect; no second surround
  toggle is added (documented in DOCS_DELTA).
