# Tasks: Maximum song-duration filter (AA-03)

## 1. Preference and policy

- [x] 1.1 Add `maximum_song_duration` default 0 and `Preferences.maximumSongDuration`.
- [x] 1.2 Add pure `SongDurationFilterPolicy` with 0..7200 s/60 s constants, range validation and
  inclusive minimum/maximum selection fragments.
- [x] 1.3 Apply both fragments only in `SongRepository.makeSongCursor`, with an explicit
  `skipDurationFilter` guard used by queue, ID/path/file-provider, device-playlist and unknown-genre
  resolution paths.

## 2. UI

- [x] 2.1 Add the maximum-duration SeekBar after the minimum row (0..7200 s, 60 s step, value shown).
- [x] 2.2 Add main English title/summary and invalid-range strings.
- [x] 2.3 Validate min/max in `LibraryPreferencesFragment` and reject invalid changes with a message.

## 3. Documentation and OpenSpec

- [x] 3.1 Update `FEATURES`, `MISSING_FEATURES` (genre residual) and `CODE_MAP`.
- [x] 3.2 Create this OpenSpec change.

## 4. Verification

- [x] 4.1 Run the pure `SongDurationFilterPolicy` probe: **19 checks PASS** (off, max-only,
  min+max, inclusive boundaries, invalid/defensive ranges, no Android imports).
- [x] 4.2 Run the static source probe: **53 checks PASS** (preference/XML/defaults, policy
  application, explicit `skipDurationFilter` call sites for queue/ID/path/file-provider/
  device-playlist/unknown-genre, playlist/genre exclusions, validation, docs and scope).
- [x] 4.3 Run ktlint and `git diff --check`: pass.
- [x] 4.4 Run `openspec validate add-max-duration-filter --strict`: valid.
- [x] 4.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL in 4m 58s**,
  0 lint errors / 1,393 total issues (1,052 warning-level + 341 informational). Final universal APK
  `174C915187DB15430E84A99C08880C0B347E06C8E157B49A6D455EF63F85ED4E` (53,451,588 bytes, mtime
  `2026-11-03T04:30:14.2698463Z`), copied to `artifact/` with `SHA256.txt`.
- [x] 4.6 Device runtime on qa26 / `emulator-5556` against
  `174C915187DB15430E84A99C08880C0B347E06C8E157B49A6D455EF63F85ED4E`: **PASS (qualified)**.
  Verified max 0/off, max-only library/album filtering, inclusive min+max boundaries, invalid-range
  rejection, excluded-folder composition, unknown durations, queue and named-queue long-song
  immunity, device playlist membership, Room playlist immunity, backup content/path, delete cleanup
  and force-stop/relaunch persistence. Backup restore ID remap remains a source-probe residual, not
  a runtime failure. Evidence: `174C-RUNTIME-RESULT.md`, `RUNTIME.md`, `runtime/ui/`.
