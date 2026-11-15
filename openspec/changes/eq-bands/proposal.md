# eq-bands

## Why

The equalizer still behaves like a fixed-configuration effect: the Precision
(`DynamicsProcessing`) engine only offers the legacy 5/10/15/32 layouts, changing the band count
discards the current curve instead of adapting it, and there is no explicit way to hand effects
over to the device's system/OEM equalizer. Cast and remote routes also cannot own an association
because they are not represented in `AudioDeviceType`.

## What Changes

- Offer **3, 5, 10, 15 and 31** ISO-style band layouts on the Precision engine, with 31 replacing
  the old non-standard 32-band layout. Legacy 32-band preferences/profiles are migrated by
  interpolating them to 31 bands.
- Resample (log-frequency interpolation) the current profile whenever the band count changes, so
  the curve is preserved instead of reset to flat.
- Map AutoEq imports onto the currently selected band count (existing point-cloud interpolation is
  reused).
- Keep the Legacy (`Equalizer`) engine on the band count/frequencies fixed by the device, and show
  the band-count selector disabled with an explanation.
- Add a third engine choice, **System equalizer**, that disables the in-app engine and hands the
  audio session to the system/OEM effect control panel
  (`ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL` plus open/close session broadcasts). The Equalizer
  screen shows a dedicated card with the panel action instead of the in-app controls.
- Add a **Cast / remote** output type mapped from MediaRouter TV/speaker/AVR/group route types so
  profiles can be associated with a cast route (exact route key included), reusing the existing
  per-output association resolver.
