# DOCS_DELTA — eq-bands

## docs/FEATURES.md

### §2 `Equalizer` bullet

Replace:

> - **Equalizer** with two engines: classic `android.media.audiofx.Equalizer` and `DynamicsProcessing` (API 28+, multiband compressor/limiter); band counts/configuration follow the selected engine's capabilities.

with:

> - **Equalizer** with three engine choices: the classic `android.media.audiofx.Equalizer` (device-fixed bands, shown read-only), `DynamicsProcessing` (API 28+, multiband compressor/limiter) with selectable 3/5/10/15/31 ISO band layouts, and a **System equalizer** hand-off that disables the in-app engine and drives the OEM panel through the effect-control session broadcasts. Changing the band count interpolates the current curve in log-frequency space instead of resetting it, legacy 32-band data migrates to 31, and AutoEq imports map their point cloud onto the selected band count.

### §2 `EQ profiles` bullet

Append a sentence:

> Saved profiles keep their own band count; selecting a profile with a different count switches the engine to it, while changing the count resamples the applied curve. **Cast / remote** MediaRouter routes (TV, cast speaker, AVR and speaker groups) are available as a profile association type, including an exact hashed route key.

## docs/MISSING_FEATURES.md

Part 1 §4:

- Remove: “**Configurable band count** — 3 / 5 / 10 / 15 / 31 bands (ssilencce engines use a fixed band count).”
- Remove: “**Per-screencast-device EQ** (PRO).”
- Remove: “**Musicolet EQ vs System equalizer** engine choice.”

Part 2 §7 (Musicolet comparison):

- Replace “ssilencce: two engines (classic Equalizer + DynamicsProcessing) with fixed band counts,” with “ssilencce: three engine choices (classic Equalizer with device-fixed bands, DynamicsProcessing with 3/5/10/15/31 ISO bands and curve resampling, System/OEM panel hand-off),”.
- Replace the final line “- See Part 1 §4 (band count, per-BT device).” with “- See Part 1 §4 (per-BT device).”

## docs/CODE_MAP.md

Add entries:

- `core/model/equalizer/EqBandFrequencies.kt` — ISO-style center-frequency tables for the 3/5/10/15/31 band layouts (plus the legacy 32-band table used for migration), `closestBandCount` and a log-spaced fallback for foreign layouts.
- `core/model/equalizer/EqBandResampler.kt` — Log-frequency interpolation of a gain curve between band layouts; used by band-count changes, the legacy 32→31 migration and the `EqBandCapabilities` display fallback.
- `core/model/equalizer/SystemEqualizer.kt` — System/OEM equalizer hand-off contract: panel `Intent` (with optional audio session), effect-control open/close broadcast `Intent`s and panel availability probing.
- `ui/screen/equalizer/SystemEqualizerCard.kt` — Compose card shown for the System engine with the panel action and the unavailable message.

Update:

- `core/model/audiodevice/AudioDeviceType.kt` — add the `Cast` type mapped from MediaRouter TV/speaker/remote-speaker/AVR/group routes with a hashed route key.
- `core/model/equalizer/EqBandCapabilities.kt` — safe configuration/frequency lookup, closest-count resolution and resampled `getBands`.
- `core/model/equalizer/EqEngineMode.kt` — nullable effect UUID and the new `System` mode.
- `core/model/equalizer/EqState.kt` — `usesSystemEqualizer`; `isUsable` excludes the System mode.
- `playback/equalizer/EqualizerManager.kt` — resamples the current profile on band-count changes, migrates legacy 32-band prefs/profiles, normalizes the stored count to the engine's real layout, accepts legacy 32-band imports, and releases the in-app engine / opens the external session for the System mode.
- `playback/equalizer/engine/DynamicsProcessingEngine.kt` — 3/5/10/15/31 ISO layouts from `EqBandFrequencies` (31 replaced 32).
- `ui/screen/equalizer/EqualizerScreen.kt` — always-visible band-count selector (disabled with an explanation on the Legacy engine), System engine dialog entry/filtering, System card instead of the in-app cards, Aurora pages hidden for System.
- `ui/screen/equalizer/EqualizerViewModel.kt` — system panel availability/open via `SystemEqualizer`, opens the panel without a session before playback.

## AGENTS.md — Repository Code Map

- `core/model/` line: add “ISO band tables/resampler and system-equalizer hand-off”.
- `playback/equalizer/` line: add “3/5/10/15/31-band resampling and system EQ hand-off”.
- `ui/screen/equalizer/` line: add “system-equalizer card”.
- `core/model/audiodevice/` is covered by the existing `core/model/` line; mention “cast/remote MediaRouter route type” there.

## docs/agents/domain.md

No ADR was added or superseded; the ADR count and references stay unchanged.

## Decisions

- **31 replaces 32.** The Precision engine now advertises exactly 3/5/10/15/31 bands. The 31-band table is the standard 1/3-octave series (20 Hz–20 kHz); the previous 32-band table (16 Hz–20 kHz) is kept only as migration source data. Stored 32-band preferences and profiles are resampled to 31 on the next initialization, and a legacy 32-band profile imported from JSON is accepted by resampling it to 31.
- **Band resampling.** Gains are interpolated linearly in log-frequency space between the source and target center frequencies (clamped to the edge gains outside the source range). Changing the count replaces the applied profile with a Custom curve so the preset name no longer claims a different layout. Saved profiles keep their own band count; selecting one still switches the engine to that count (previous behavior).
- **Expert Mode no longer gates 31 bands.** `maxBandCountInNormalMode` is 31 so every advertised layout is selectable without Expert Mode; Expert Mode continues to gate the compressor/limiter. The Legacy engine's single device configuration is normalized into the stored band count so the UI always shows the real device count.
- **System engine resets the equalizer.** Selecting System uses the existing engine-switch reset (the dialog already warns that custom profiles are reset), releases the in-app engine, clears band capabilities and routes the session through the External type. The System card replaces the in-app cards; the EQ enable switch is hidden because the panel owns effects. The panel action opens without an audio-session extra before playback, so the OEM settings UI is still reachable.
- **System availability.** The engine dialog only offers System when a package resolves `ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL`; if a persisted System mode has no panel, the card reports it and the engine menu remains reachable so the user can switch back. No new permission or `<queries>` entry was needed (the manifest already declares the panel action).
- **Cast association detection.** `AudioDeviceType.Cast` maps MediaRouter `DEVICE_TYPE_TV`, `DEVICE_TYPE_SPEAKER`, `DEVICE_TYPE_REMOTE_SPEAKER`, `DEVICE_TYPE_AUDIO_VIDEO_RECEIVER` and `DEVICE_TYPE_GROUP` to one route type and keeps `isProduct = true`, so the selected route's hashed id can form an exact per-cast-device key. The observer still only considers connected live-audio routes; remote-playback-only casts decode on the receiver, so local EQ cannot affect them and no claim is made for that case.
- **Strings.** Only `values/strings.xml` was touched; new labels are untranslated. The obsolete `no_audio_ID` string is left in place (unused after the session-optional panel change).
