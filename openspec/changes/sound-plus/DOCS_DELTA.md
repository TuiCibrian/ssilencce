# DOCS_DELTA — sound-plus

## docs/FEATURES.md

### `Audio processing` bullet (intro section)

Add after “Per-channel balance, ReplayGain … injected into `DefaultAudioSink`”:

- “a stereo/mono downmix processor (`MonoAudioProcessor`) that can be toggled at runtime without reconfiguring the sink”

### `Sound settings sheet` bullet

Replace with:

- “**Sound settings sheet** — Volume, per-channel balance, tempo, mono output, audio offload, float output and skip-silence controls reachable from the player; ReplayGain and reverb controls live in the Equalizer screen.”

### `ReplayGain` bullet

Append:

- “The ReplayGain card also exposes **Use embedded tags** (off: every song uses the without-tag preamp), **Prevent clipping** (peak limiting, default on), a **Headroom** slider (0–12 dB attenuation before limiting) and **Constant volume** (one constant gain for every song instead of per-track tag gains; default off). All four keep the previous behavior as their default.”

### `Equalizer / sound` feature list

Add:

- “**Mono output** — mixes 16-bit stereo PCM to a single signal on both channels; takes effect on the next buffer and is bypassed by multichannel/float output.”
- “**Reverb (experimental)** — an `EnvironmentalReverb` attached to the playback audio session with Small room / Medium room / Large room / Medium hall / Large hall / Plate presets and custom room level, room HF level, decay time, decay HF ratio, reflections level/delay, reverb level/delay, diffusion and density controls. It is created on demand, disabled for bit-perfect/audio-offload, and released when the audio session changes or the playback service stops.”

### `Capability-aware reference controls` bullet

Replace “crossfade, mono and song-count timer” with “crossfade and song-count timer”.

## docs/MISSING_FEATURES.md

Part 1 §4:

- Remove: “**Stereo/mono output** switch.”
- Keep but reword: “**Surround sound** effect.” → “**Dedicated surround-sound toggle** — the existing Android `Virtualizer` effect already provides stereo widening; no separate surround switch is needed.”
- Remove: “**Reverb effects** (experimental) — room level/HF, decay time/ratio, reflection level/delay, reverb level/delay, diffusion.”
- Remove: “**"Convert RG to constant volume"** — a non-RG preamp is implemented; conversion is not.”
- Reword: “**Headroom handling** and channel-balance warning.” → “**Channel-balance warning** — headroom handling is implemented; the balance warning is not.”

Part 2 §8 ReplayGain:

- In the Musicolet/ssilencce comparison, append: “ssilencce now also exposes use-embedded-tags, prevent-clipping, headroom and constant-volume controls.”
- Replace “Remaining: "convert RG to constant volume", use-embedded-tags toggle, skip-or-reload and user-facing headroom control.” with “Remaining: skip-or-reload.”

Comparison table (line ~158):

- Remove “It does not convert to constant volume.” and replace with “Both expose use-embedded-tags, clipping/headroom and constant-volume controls.”

## docs/AURORA_REFERENCE.md

Line “Audio crossfade, mono output and timers by number of songs.” → “Audio crossfade and timers by number of songs.”

## docs/CODE_MAP.md

Add entries:

- `playback/processor/MonoAudioProcessor.kt` — Runtime stereo→mono downmix `BaseAudioProcessor` (PCM16, ≤2 channels); keeps the output channel count so toggling does not reconfigure the sink. Injected between the balance and ReplayGain processors.
- `playback/effects/ReverbController.kt` — Owns the `EnvironmentalReverb` for the player audio session; creates/applies/presets/releases it, gated by effect availability and the EQ disable reasons.
- `core/model/equalizer/ReverbState.kt` — `ReverbPreset` (six presets + Custom), `ReverbSettings` (millibel/millisecond/per-mille parameters) and `ReverbState`.
- `ui/screen/equalizer/ReverbControls.kt` — Compose reverb card: experimental label, preset chips and custom parameter sliders.

Update:

- `playback/processor/ReplayGainAudioProcessor.kt` — note the `useEmbeddedTags`, `preventClipping`, `headroomDb` and `constantVolume` controls.
- `playback/equalizer/EqualizerManager.kt` — note mono/reverb state, DataStore keys and session-bound reverb lifecycle.
- `playback/renderer/SsilencceRenderersFactory.kt` — note processor order balance → mono → ReplayGain.
- `ui/screen/sound/SoundSettingsScreen.kt` — note the Mono output switch.
- `ui/screen/equalizer/EqualizerScreen.kt` — note reverb card and ReplayGain extras.

## AGENTS.md — Repository Code Map

- `playback/processor/` line: add “runtime stereo/mono downmix”.
- `playback/equalizer/` line: add “session-bound environmental reverb controller”.
- `ui/screen/equalizer/` line: add “reverb card (presets + custom parameters)”.
- `ui/screen/sound/` line: add “mono output switch”.
- `docs/agents/domain.md`: no ADR added; the ADR count and references stay unchanged.

## Decisions

- **Mono processor instead of `ChannelMixingAudioProcessor`.** Media3's
  `ChannelMixingAudioProcessor` requires all matrices before configuration, treats an identity
  matrix as an inactive processor and changes the output channel count when downmixing, so a
  runtime stereo/mono switch would need a sink reconfiguration (and can silently stay inactive
  on the current track). `MonoAudioProcessor` performs the same average-to-both-channels mix
  with a stable two-channel output format, giving an immediate, reconfigure-free toggle. It is
  restricted to 16-bit PCM and ≤2 input channels; multichannel content is left to the platform
  downmixer and float output bypasses the processor.
- **Reverb uses `EnvironmentalReverb` only.** `PresetReverb` cannot expose custom parameters,
  so the six presets are parameter bundles applied to one `EnvironmentalReverb`.
- **Sound preferences stay in the equalizer DataStore.** Mono/ReplayGain/reverb keys live in
  `EqualizerManager.Keys` alongside the related audio-processing keys instead of
  `util/Preferences.kt`; this is where every other sound-processing setting is persisted and
  observed. Defaults preserve existing behavior (mono/reverb/constant-volume off,
  use-embedded-tags/clipping-prevention on, headroom 0 dB).
- **Constant-volume semantics.** When enabled it applies the single “Constant gain” value
  (`preamp`) to every song and ignores per-track/album tags, so tagged and untagged songs use
  the same gain. It requires a non-Off ReplayGain mode, is off by default and is documented in
  the UI description.
- **Use-embedded-tags off.** Playback skips reading the file tags and uses the without-tag
  preamp for every song. Re-enabling it applies the tag gain from the next track transition;
  the previous track's cached gain is discarded, so no stale value is applied.
- **Surround.** Not implemented separately: the Equalizer already exposes Android's
  `Virtualizer`, which is the platform surround/stereo-widening effect.
- **Strings.** Only `values/strings.xml` was touched; the obsolete `aurora_mono` label is no
  longer referenced and the new labels are untranslated.
