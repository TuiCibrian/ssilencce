# Tasks: Selective backup/restore types (AA-09)

## 1. Archive model and backup creation

- [x] 1.1 Add pure `BackupArchivePolicy` for archive paths, entry classification, play-count merge,
  newer-position selection and named-queue id/name/cap planning.
- [x] 1.2 Add serializable `PlayCountBackupEntry`, `NamedQueueBackupEntry` and
  `TrackPositionBackupEntry` DTOs plus entity mappings.
- [x] 1.3 Add DAO bulk-read/bulk-upsert methods for play counts and track positions.
- [x] 1.4 Write `Favorites/favorites.m3u` and exclude the localized Favorites playlist from the
  generic `Playlists/` set.
- [x] 1.5 Write `statistics/play_counts.json`, `queues/named_queues.json` and
  `positions/track_positions.json`, omitting empty lists.
- [x] 1.6 Clean the temporary Favorites cache directory after creation.

## 2. Restore semantics

- [x] 2.1 Add Favorites, Play counts, Queues and Per-file settings restore kinds and
  `BackupContent` categories.
- [x] 2.2 Restore Favorites by merging only missing songs into the current localized playlist;
  keep old `Playlists/Favorites.m3u` archives on the Playlists path.
- [x] 2.3 Merge play counts by max play/skip/time, preserving existing metadata and bulk-upserting.
- [x] 2.4 Restore named queues by preserving/replacing ids, enforcing the 100-queue cap, resolving
  name collisions and reporting cap/DB failures.
- [x] 2.5 Restore track positions only when the backup `updated_at` is strictly newer.
- [x] 2.6 Treat empty JSON lists as no-ops; record malformed selected JSON entries as per-entry
  failures without aborting later entries.
- [x] 2.7 Keep bounded-stream, entry-count and ZIP structural failures as archive-wide aborts.
- [x] 2.8 Extend restore accounting so mixed queue batches and per-entry failures produce honest
  `Partial`/`NothingImported` outcomes.

## 3. UI and resources

- [x] 3.1 Extend the existing selective-restore picker through `BackupContent.entries`; no new
  screen.
- [x] 3.2 Add the four main English category labels and update the backup share disclosure.
- [x] 3.3 Leave translated string files, build files, permissions and Room schema untouched.

## 4. Documentation and OpenSpec

- [x] 4.1 Update `docs/FEATURES.md`, `docs/MISSING_FEATURES.md` and `docs/CODE_MAP.md`.
- [x] 4.2 Add ADR-048 and update `docs/agents/domain.md`.
- [x] 4.3 Create this OpenSpec change.

## 5. Verification

- [x] 5.1 Run the pure `BackupArchivePolicy` probe: **35 checks PASS** (legacy/new classification,
  favorites separation, max merge, newer position, queue cap/name/id planning).
- [x] 5.2 Run the static source probe: **100 checks PASS** (archive wiring, selective branches,
  restore semantics, favorites insertion, DAO/DTO support, strings/disclosure, docs/OpenSpec,
  scope).
- [x] 5.3 Run project ktlint `format` and `check` on all touched Kotlin files: pass.
- [x] 5.4 Run `openspec validate add-backup-restore-types --strict`: valid.
- [x] 5.5 Combined JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL in
  4m 24s**. The first attempt found one AA-09 compile error (`Repository` exposes
  `insertSongsInPlaylist`, not `insertSongs`); after that single source fix the build passed.
  Lint: **0 errors / 1,413 total issues** (1,072 warning-level + 341 informational). Final
  universal APK `730F5EC756B87681E1472567F774C8359214F9CB2DF3E494100B02EE55AA57AE`
  (53,462,628 bytes, mtime `2026-11-04T00:01:00.7233955Z`), copied to `artifact/` with
  `SHA256.txt`.
- [x] 5.6 Device runtime: **PASS (qualified)** on qa26/API 26. C73 legacy build
  `C73EB2359AEA81AF3F983313C82F450983473D0A1AC73C658AF17FE70F7A9956` verified old
  `Playlists/Favorites.m3u` merges into localized Favorites with no literal playlist, new
  `Favorites/` path still works, 100-queue cap accepts existing/rejects the 101st new queue,
  mixed restore reports Partial, and empty/malformed JSON leaves duplicate queue names unchanged
  without crashing. Prior C73/84DA run also covered Favorites, play counts, queues, per-file
  positions (newer/older) and independent category restores. Pure 14/14, static 12/12,
  ktlint/diff/OpenSpec PASS, build 1m 25s. Evidence:
  `aa09-backup-types-2026-11-04/AA09-LEGACY-RUNTIME-RESULT.md` and frozen `freeze/` + `artifact/`.
