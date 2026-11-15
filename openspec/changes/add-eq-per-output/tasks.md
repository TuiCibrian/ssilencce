# Tasks: Per-output equalizer association (AA-04)

## 1. Association model and identity

- [x] 1.1 Keep `EqProfile.associations` as the broad type fallback.
- [x] 1.2 Add `associatedDeviceKeys` default-empty JSON field and equality support.
- [x] 1.3 Add opaque `AudioDeviceKeyPolicy` hashing.
- [x] 1.4 Add `AudioDevice.deviceKey` and populate it from MediaRouter/AudioDeviceInfo identity.
- [x] 1.5 Add a MediaRouter route callback for output-route changes.

## 2. Resolution and persistence

- [x] 2.1 Add pure `EqOutputAssociationPolicy` exact-key/type/no-match logic and overlap removal.
- [x] 2.2 Re-evaluate from audioDevice + profiles + ready EQ state.
- [x] 2.3 Prefer exact key, fall back to type, preserve current profile on no match.
- [x] 2.4 Skip redundant `setCurrentProfile` when the target is already current.
- [x] 2.5 Normalize overlapping type/key associations on save/edit.

## 3. UI

- [x] 3.1 Pass the current `AudioDevice` through profile saver/editor.
- [x] 3.2 Add a Compose “Use with this device” row when an exact key exists, keeping type rows.
- [x] 3.3 Add the main English current-device string.

## 4. Documentation and OpenSpec

- [x] 4.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 4.2 Create this OpenSpec change.

## 5. Verification

- [x] 5.1 Run the pure association/key policy probe: **23 checks PASS** (exact-over-type, type
  fallback, no match, overlap removal, hash determinism/no raw identity).
- [x] 5.2 Run the static source probe: **47 checks PASS** (identity population, route callback,
  profile JSON default, manager combine/normalization/idempotence, UI row, strings, scope).
- [x] 5.3 Run ktlint and `git diff --check`: pass.
- [x] 5.4 Run `openspec validate add-eq-per-output --strict`: valid.
- [x] 5.5 Combined JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL in
  4m 51s** after the LIB-03 `AlbumRepository.splitIntoAlbums` exposure landed. 0 lint errors /
  1,401 total issues (1,060 warning-level + 341 informational). Final universal APK
  `A20119EEDF6A05A381088D8B27D3E21D987BB7EE615F2BB584344FB93FB211C8` (53,455,984 bytes, mtime
  `2026-11-03T06:25:58.3042627Z`), copied to `artifact/` with `SHA256.txt`.
- [x] 5.6 Device runtime on qa26/API 26: **PASS (qualified)** on
  `A20119EE…211C8`. Verified EQ saver/editor, type association checkboxes, save persistence across
  force-stop/relaunch, overlap normalization on edit, current-device row absence for the
  type-only/unknown emulator output, and playback continuity across profile switch (same media
  item, position advanced, queue unchanged). Exact Bluetooth/USB identity and speaker type
  selection remain device-gated. Evidence: `eq-per-output-2026-11-02/AA04-RUNTIME-RESULT.md`.
