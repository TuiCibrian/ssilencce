# Tasks — eq-bands

- [x] Centralize ISO band frequency tables (3/5/10/15/31) and add a log-frequency resampler.
- [x] Extend `EqBandCapabilities` with safe configuration lookup and resampled band display.
- [x] Switch `DynamicsProcessingEngine` to the 31-band ISO layout; keep capabilities on 3/5/10/15/31.
- [x] Resample the current profile when the user changes the band count.
- [x] Migrate legacy 32-band preference/profile data to 31 bands.
- [x] Normalize the preferred band count to the engine's real configuration (Legacy engine).
- [x] Add `EqEngineMode.System` with system/OEM panel detection and in-app engine shutdown.
- [x] Send open/close effect-control session broadcasts for the System engine.
- [x] Equalizer UI: system-equalizer card, system engine dialog entry, disabled band selector with
      explanation for the Legacy engine.
- [x] Add `AudioDeviceType.Cast` (MediaRouter TV/speaker/AVR/group routes) with a cast icon/string.
- [x] Add/update English strings next to the related equalizer/audio-device strings.
- [x] Write DOCS_DELTA.md with FEATURES / MISSING_FEATURES / CODE_MAP / AGENTS.md updates.
- [x] Compile gate passes.
