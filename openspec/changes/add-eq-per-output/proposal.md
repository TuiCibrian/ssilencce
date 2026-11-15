# Proposal: Per-output equalizer association (AA-04)

## Why

Equalizer profiles already have broad output-type associations (speaker, wired headset, Bluetooth
A2DP, USB, HDMI, Aux), but every Bluetooth device shares one `BluetoothA2dp` type. Two paired
Bluetooth headphones therefore cannot keep separate EQ profiles. The association path also has
source-confirmed gaps: overlapping associations can be saved, profile loading can race the initial
audio-device emission, and route changes are not observed.

## What Changes

- Retain `EqProfile.associations` as the broad speaker/headset/Bluetooth/wired/other fallback.
- Add `associatedDeviceKeys: Set<String> = emptySet()` for exact output identity, serialized with a
  default so existing profiles and exports remain valid.
- Derive opaque hashed keys from the selected MediaRouter route or `AudioDeviceInfo` identity;
  never store raw Bluetooth addresses.
- Resolve exact key first, then type; no match preserves the current profile (existing default).
- Re-evaluate association when audio device, profiles or EQ state loads/changes.
- Register a MediaRouter callback for selected-route changes.
- Normalize overlapping type/key associations on save/edit and skip redundant profile application.
- Add a Compose “Use with this device” row in profile saver/editor when an exact key is available;
  keep the existing type rows.
- No Room schema, dependency, permission or translated-string change.

## Impact

- Modified: `EqProfile.kt`, `AudioDevice.kt`, `AudioOutputObserver.kt`, `EqualizerManager.kt`,
  `EqualizerScreen.kt`, `EqualizerViewModel.kt`, `strings.xml`, `FEATURES`, `MISSING_FEATURES`,
  `CODE_MAP`.
- New: `AudioDeviceKeyPolicy.kt`, `EqOutputAssociationPolicy.kt`, OpenSpec change and probes.
- Unchanged: playback queue/transport, EQ engines/database, profile JSON backward compatibility.
