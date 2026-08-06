# ADR-016: High-Resolution and Bit-Perfect Audio Output

## Status

Accepted

## Context

A music player audience includes USB-DAC users who want bit-perfect output (no resampling,
volume bypass) and users who want power-saving audio offload or float output. Android's audio
stack resamples by default; bit-perfect requires opting into `AudioTrack.setPreferredMixerAttributes`
on USB devices (API 34+), and effects (equalizer) must not fight those paths.

## Decision

- Implement `AudioOutputObserver` as the central audio-environment observer:
  - a `BroadcastReceiver` + `AudioDeviceCallback` pair exposing StateFlows for the current
    output device, system volume and bit-perfect state,
  - USB bit-perfect configuration via `setPreferredMixerAttributes` (API 34+, USB devices only,
    matching sample rate/channel count),
  - `availableBitPerfectDevices` and `BitPerfectState` (Active/Inactive with encoding/sample-rate
    labels) consumed by sound settings UI.
- Add user-facing audio mode toggles persisted in the `equalizer` DataStore: bit-perfect audio,
  audio offload, float output, and skip silence.
- Disable EQ/effect application when bit-perfect or audio offload is active (effects would
  break the requirement or be ignored), surfaced in the sound settings screen.
- Manage playback state transitions around the audio device: resume/pause on Bluetooth A2DP
  connect/disconnect (`BluetoothExt`), respecting the `RESUME_ON_BLUETOOTH_CONNECT`
  preference.
- Provide the system output switcher entry: `SystemMediaControlResolver` opens the OEM-aware
  output switcher (MiPlay/OneUI intents, SystemUI `LAUNCH_MEDIA_OUTPUT_DIALOG` on 12–13,
  `MediaRouter2.showSystemOutputSwitcher` on 14+).

## Consequences

- Bit-perfect playback is achievable on modern USB DACs — a differentiator for audiophile
  users, at the cost of platform-conditional behavior.
- The effect gating rule is enforced centrally in `EqualizerManager`, preventing "silent EQ"
  support requests.
- OEM switcher support requires device-specific intent handling that can decay across OEM
  ROMs; the resolver degrades to the SystemUI/media-router fallback.
- The audio environment is now observable reactive state; UI (sound settings, equalizer
  screen) renders device and bit-perfect status live.
