# Design: Per-output equalizer association (AA-04)

## Context

`EqProfile` is stored as JSON in the existing equalizer DataStore. It already carries a
`Set<AudioDeviceType>` for broad output matching, and `ProfileAssociationView` exposes those types.
`EqualizerManager` currently listens only for audio-device emissions, resolves the first matching
type profile and applies it. `AudioDevice` has no public identity; all A2DP devices are one type.

## Goals / Non-Goals

**Goals:**

- Keep type associations as the fallback for speaker, wired headset, Bluetooth, USB, HDMI and Aux.
- Add exact per-device association where the OS exposes an identity, especially paired Bluetooth.
- Re-evaluate correctly when profiles/state load and when routes change.
- Preserve the current default: no matching profile leaves the current profile unchanged.
- Apply profile changes without pausing, seeking or rebuilding playback.
- Persist in the existing profile JSON; no schema or dependency.

**Non-Goals:**

- New EQ engines, band counts, presets, or audio-effect features.
- Per-screencast-device EQ.
- Storing raw Bluetooth MAC addresses or showing them in the UI.
- Changes to playback, queue, named queues or unrelated settings.

## Decisions

### Association model

- `EqProfile.associations: Set<AudioDeviceType>` stays as broad fallback.
- Add `@SerialName("device_keys") associatedDeviceKeys: Set<String> = emptySet()`.
- `AudioDevice` gains `deviceKey: String?`; product-like routes/devices derive it from route ID or
  `AudioDeviceInfo.address` (API 28+) / product name. `AudioDeviceKeyPolicy` hashes
  `type:identity` with SHA-256 and keeps a type prefix; raw identity is never stored.
- Built-in speaker/wired headset have no reliable identity and use type matching.

### Resolution

`EqOutputAssociationPolicy` is Android-free and receives type names and keys:

1. exact key match wins;
2. otherwise the type association is used;
3. otherwise null, and the manager preserves the current profile.

Overlapping type/key claims are removed from other profiles on save/edit. The existing default
behavior is explicitly preserved for unassociated outputs.

### Re-evaluation

`EqualizerManager` combines `audioDevice`, `eqProfiles` and a ready `eqState` (debounced), then
resolves and applies only if the target differs from the current profile. This fixes the
profile-load/startup race and re-evaluates after saves/edits.

`AudioOutputObserver` registers a `MediaRouter.Callback` for selected/route changes in addition to
audio-device add/remove and headset plug, so selecting an output updates the device flow.

### Playback safety

Applying a profile changes only EQ state/engine; it does not call play, pause, seek or queue
operations. Already-current profiles are skipped. A profile with a different band count may
reconfigure the effect engine, but playback continues.

### UI

Keep the existing type checkbox list. When the current output has an exact key, add a
“Use with this device: <name>” row above the type rows in profile saver/editor. The current output
name is already shown in the equalizer header. Main English string only.

### Persistence / compatibility

The new profile field has a default empty set, so old JSON profiles and exports decode unchanged.
The key set is included in equality/hash and export/import. No Room schema, migration or dependency.

## Risks / Trade-offs

- Route/address identity may vary by OEM or connection; type fallback remains.
- Hashing avoids raw address exposure but opaque keys can change if an OEM changes route IDs;
  profiles then fall back to type.
- No match preserves the current profile, which can leave a previously matched profile active on
  an unassociated output (existing behavior).
- Applying a profile can briefly reconfigure the EQ engine; playback must remain uninterrupted.
- Imported profiles with overlapping associations are normalized on the next save/edit; existing
  imported duplicates may remain until then.

## Verification

- Pure `EqOutputAssociationPolicy`/`AudioDeviceKeyPolicy` probe: exact-over-type, type fallback,
  no match, type/key overlap removal, hashing/determinism/no raw identity.
- Static source probe: identity population, route callback, profile JSON default, manager combine,
  normalization, skip-if-current, UI current-device row, main-only strings, no schema/deps.
- ktlint, `git diff --check`, OpenSpec strict.
- Build/device runtime deferred until LIB-03/AA-04 tree settles and the lease is granted.
