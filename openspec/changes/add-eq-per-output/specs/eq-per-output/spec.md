# Per-output equalizer association

## Purpose

Associate saved equalizer profiles with output types and, where the OS exposes an identity, exact
output devices, without changing playback or adding schema/dependencies.

## ADDED Requirements

### Requirement: Type fallback is preserved

Saved profiles SHALL retain broad `AudioDeviceType` associations for speaker, wired headset,
Bluetooth, USB, HDMI and Aux. A device without an exact key SHALL continue to match by type.

#### Scenario: Speaker
- **WHEN** the built-in speaker is active and a profile is associated with `BuiltinSpeaker`
- **THEN** that profile is selected by type

#### Scenario: Wired headset
- **WHEN** a wired headset is active and a profile is associated with `Headset`
- **THEN** that profile is selected by type

#### Scenario: Bluetooth type fallback
- **WHEN** a Bluetooth device has no exact key and a profile is associated with `BluetoothA2dp`
- **THEN** that profile is selected by type

### Requirement: Exact device association

Profiles SHALL support an optional set of opaque device keys, defaulting to empty. Keys SHALL be
derived from the selected MediaRouter route or `AudioDeviceInfo` identity, hashed, and MUST NOT
store or display raw Bluetooth addresses. Existing profiles/exports without keys SHALL remain
valid.

#### Scenario: Two Bluetooth devices
- **WHEN** two paired Bluetooth devices have different keys and profiles are associated with each
- **THEN** each device selects its own profile

#### Scenario: Legacy profile
- **WHEN** an imported or existing profile has no device keys
- **THEN** it remains valid and matches by type only

#### Scenario: Opaque key
- **WHEN** a device key is persisted
- **THEN** it contains a type prefix and a hash, not the raw address

### Requirement: Exact wins and no match preserves

Resolution SHALL prefer an exact device key, then fall back to the output type. If neither matches,
the current profile SHALL remain unchanged. Overlapping type/key associations SHALL be normalized
on save/edit so resolution is deterministic.

#### Scenario: Exact over type
- **WHEN** a Bluetooth key matches one profile and `BluetoothA2dp` matches another
- **THEN** the exact-key profile wins

#### Scenario: No association
- **WHEN** no profile matches the current output
- **THEN** the current profile remains active

#### Scenario: Normalization
- **WHEN** a profile is saved/edited with a type or key already claimed elsewhere
- **THEN** the other profile loses that overlapping claim

### Requirement: Re-evaluate on device/profile/state changes

The manager SHALL combine audio-device, profile and ready EQ-state changes, and SHALL react to
MediaRouter selected-route changes. It SHALL apply a resolved profile only when it differs from the
current profile.

#### Scenario: Startup order
- **WHEN** the audio device is known before profiles finish loading
- **THEN** the association is applied after profiles become ready

#### Scenario: Route selection
- **WHEN** the user selects a different output route
- **THEN** the current device flow updates and the matching profile is evaluated

#### Scenario: Redundant application
- **WHEN** the resolved profile is already current
- **THEN** no profile write/engine application is repeated

### Requirement: Playback is not disrupted

Applying or switching a profile SHALL NOT pause, seek, restart or rebuild the playback queue. Only
EQ state/profile/engine values MAY change.

#### Scenario: Device switch while playing
- **WHEN** the output changes during playback
- **THEN** playback continues and the matching EQ profile is applied

### Requirement: Scope is limited

No Room schema, dependency, permission or translated-string change SHALL be introduced. New UI
SHALL be Compose and use main English strings only. Existing type checkboxes and default profile
behavior SHALL remain.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** no schema/dependency/permission/translated-string change or playback behavior change is
  present
