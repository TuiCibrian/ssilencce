# Tasks - integration compatibility

- [x] Ran `:app:lintGithubDebug` gate on the 28-feature checkpoint; it failed with 12 `UnsafeOptInUsageError` findings, all in `playback/extractor/ExtraFormatSupport.kt` (`ExtraMimeTypes` is `@UnstableApi`).
- [x] Fixed the blocking lint findings by opting the `ExtraFormatKind` enum and `ExtraFormatSupport.describe` into `UnstableApi` with `androidx.annotation.OptIn` (no behavior change).
- [x] Re-ran the lint gate: green (`BUILD SUCCESSFUL`, exit 0).
- [x] Added a GhostSearch entry to the lock-screen player: `LockScreenEntityLauncher` routes a new `GhostSearch` entity to `MainActivity`, which reuses the existing `openGhostSearch()` destination.
- [x] The entry is rendered only while `GhostMode.isEnabled`; it opens the existing GhostSearch screen so the existing PIN/locked-session checks run before any ghost song is listed.
- [x] Defaults unchanged: the shortcut is hidden when ghost mode is disabled and no preference was added.
- [x] Confirmed (no reimplementation) `merge-extra-formats` already filters supplementary scanner results through ghost folders in `songs()`/`songs(query)` and keeps explicit access in `ghostSongs()`/id lookups.
- [x] Confirmed (no reimplementation) both the primary `PlaybackService` media source and the secondary `CrossfadePlayer` use `ExtraExtractorsFactory`.
- [x] Compile gate green: `:app:compileGithubDebugKotlin` (exit 0).
- [x] Compile gate green: `:app:compileFdroidDebugKotlin` (exit 0).
- [x] Single self-review: no conflict markers, no unrelated edits, no tests/commits/version/translation/project-doc changes, nothing outside `_integration`.
- [x] Compatibility notes captured in `DOCS_DELTA.md`.
