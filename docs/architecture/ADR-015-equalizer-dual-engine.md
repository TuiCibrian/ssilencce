# ADR-015: Equalizer with Dual Engines (Basic vs DynamicsProcessing)

## Status

Accepted

## Context

The classic `android.media.audiofx.Equalizer` is widely available but limited (no multiband
compressor, no limiter, vendor-dependent band counts). `android.media.audiofx.DynamicsProcessing`
(API 28+) supports MBC, limiter, more bands and a configurable pre-EQ. The app needs one
consistent EQ UI across devices with profiles per output device, import/export and AutoEq
support.

## Decision

- Introduce `EqualizerManager` as the single orchestrator of all effect state (EQ, bass boost,
  virtualizer, loudness gain, volume, balance, tempo, ReplayGain, compressor, limiter),
  persisting everything in the `equalizer` DataStore (ADR-009).
- Model engines behind an `EQEngine` abstraction with two implementations:
  - `BasicEQEngine` — wraps the classic `audiofx.Equalizer` (millibel band gains, no MBC/limiter).
  - `DynamicsProcessingEngine` — wraps `DynamicsProcessing` (API 28+): config builder,
    pre-EQ/gain, MBC + limiter, band layouts 5/10/15/32 with ISO frequencies,
    `VARIANT_FAVOR_FREQUENCY_RESOLUTION`, two channels.
- Auto-select the engine by SDK (`EqEngineMode.Auto`), allow manual override in settings.
- Support named profiles (`EqProfile`, JSON-serializable) associated with output device types
  (`AudioDeviceType`), auto-applied per connected device; plus custom profiles and AutoEq
  profiles imported from AutoEq TXT files (`GraphicEQ:` lines) with logarithmic gain
  interpolation.
- Gate effects when they would be pointless or harmful: EQ disabled under bit-perfect and
  audio-offload modes (ADR-016); pro mode unlocks compressor/limiter.
- Wrap every `audiofx` call in guarded, synchronized operations (`EQEngine.execOpCatching`)
  since `audiofx` fails unpredictably on vendor devices.

## Consequences

- One UI/state layer for all sound effects, decoupled from engine specifics via
  `EqBandCapabilities`.
- Modern devices get MBC/limiter; older devices still get a working graphic EQ.
- Device-specific profile auto-switching matches the perceived loudness to the output
  hardware (Bluetooth/USB/speaker).
- `audiofx` session-based effects add platform risk (effects can die with the audio session);
  the guarded wrappers contain that risk but are a recurring source of device-specific bugs.
