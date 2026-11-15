# Documentation delta - integration compatibility

## Lint opt-in fix

`playback/extractor/ExtraFormatSupport.kt`:

- `ExtraFormatKind` and `ExtraFormatSupport.describe` are annotated with
  `androidx.annotation.OptIn(UnstableApi::class)` because the container MIME constants live in
  the `@UnstableApi` `ExtraMimeTypes` object. No behavior changed; this only satisfies the
  project's `UnsafeOptInUsageError` lint gate.

## Lock-screen GhostSearch entry

- `ui/screen/lockscreen/LockScreenEntityLauncher.kt`: new `GhostSearch` entity type and
  `openGhostSearch(context)` helper.
- `ui/screen/MainActivity.kt`: `handleLockScreenEntity` handles `GhostSearch` by calling the
  existing `openGhostSearch()` navigation, so the existing GhostSearch destination and its PIN
  gate own all content checks.
- `ui/screen/lockscreen/LockScreenPlayerActivity.kt`: passes `GhostMode.isEnabled` and the
  launch callback; closes the lock screen after a successful hand-off.
- `ui/screen/lockscreen/LockScreenPlayerScreen.kt`: shows a ghost-search icon in the top bar
  only when ghost mode is enabled. The lock screen itself never lists ghost songs.

Update `docs/CODE_MAP.md` notes:

- `ui/screen/lockscreen/LockScreenPlayerScreen.kt`: add "optional ghost-search entry when Ghost
  mode is enabled; delegates to GhostSearch so the existing PIN gate applies".
- `ui/screen/lockscreen/LockScreenEntityLauncher.kt`: add "also routes GhostSearch openings".
- `ui/screen/MainActivity.kt`: add "lock-screen GhostSearch requests reuse `openGhostSearch()`".

## Confirmed existing integrations (no code change)

- `merge-extra-formats` already applies ghost-folder filtering to supplementary scanner results
  in the normal song list/search and keeps explicit GhostSearch/id/file-path access.
- `merge-extra-formats` already wires `ExtraExtractorsFactory` into both the primary
  `PlaybackService` media source and `CrossfadePlayer`.

## Decisions

- No new preference: the lock-screen ghost entry follows the existing `ghostModeEnabled`
  switch.
- No ghost contents are surfaced on the lock screen; the button only deep-links to the guarded
  GhostSearch destination.
