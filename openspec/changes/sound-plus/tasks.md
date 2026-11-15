# Tasks

- [x] Read protocol, brief and project rules; audit existing sound/replaygain code.
- [x] Mono output processor (`playback/processor/MonoAudioProcessor.kt`) wired into the
      renderer chain and the EqualizerManager/DataStore state.
- [x] Mono output switch in the sound settings sheet (replaces the unavailable row).
- [x] Reverb model (`core/model/equalizer/ReverbState.kt`) with presets and custom params.
- [x] Reverb controller (`playback/effects/ReverbController.kt`) created/released with the
      audio session and gated by the EQ disable reasons.
- [x] Reverb state exposed through EqualizerManager/ViewModel and persisted in the EQ
      DataStore.
- [x] Reverb card (preset chips + custom parameter sliders) in the Equalizer screen.
- [x] ReplayGain: use-embedded-tags, clipping prevention, headroom and constant-volume
      mode added to the processor, state, DataStore and Equalizer screen.
- [x] Strings added to `values/strings.xml`.
- [x] `openspec/changes/sound-plus/DOCS_DELTA.md` written (including the surround skip).
- [x] Compile gate green.
- [x] Self-review of the working-tree diff.
