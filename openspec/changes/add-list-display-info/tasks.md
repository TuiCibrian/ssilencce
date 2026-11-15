# Tasks: Song-list title mode and date modified suffix

## 1. Policy and preferences

- [x] 1.1 Add pure `SongTitleMode` and `SongListDisplayPolicy` with FollowSort, Title and FileName
  title resolution plus date-suffix appending.
- [x] 1.2 Add `song_title_mode` (default `FollowSort`) and `show_list_date_modified` (default
  `false`) preference accessors and keys.
- [x] 1.3 Add a date-only `Song.listDateModified(context)` formatter.

## 2. Adapter integration

- [x] 2.1 Resolve row titles, selection names and section popup text through the policy.
- [x] 2.2 Append the localized date suffix in the base secondary text and the SimpleSongAdapter
  override, gated by the preference.
- [x] 2.3 Leave compact and Home layouts without a secondary text view unchanged.
- [x] 2.4 Apply the same title policy, selection name and optional date suffix to global-search
  SONG rows in `SearchAdapter`.

## 3. Settings resources

- [x] 3.1 Add the title-mode ListPreference and date-modification switch under Appearance > Display.
- [x] 3.2 Add main English strings and preference arrays only; do not edit translated resources.

## 4. Documentation and OpenSpec

- [x] 4.1 Create this OpenSpec proposal, design, spec and tasks.
- [x] 4.2 Update `docs/FEATURES.md`, `docs/MISSING_FEATURES.md`, `docs/CODE_MAP.md` and
  `AGENTS.md`.

## 5. Verification

- [x] 5.1 Run the pure policy probe: **12/12 PASS**.
- [x] 5.2 Run the static source probe: **75/75 PASS** (includes `SearchAdapter`).
- [x] 5.3 Run ktlint and `git diff --check`: PASS.
- [x] 5.4 Run `openspec validate add-list-display-info --strict`: valid.
- [x] 5.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL in 3m 58s**, 0 lint errors / 1,437 warnings. New candidate **E2A712E8** `e2a712e885927f7709b13b0f91867e7aa27f4340a770d81ab71c3934c626704b` (53,475,256 bytes, mtime `2026-11-07T05:15:26.0553737Z`), frozen under `artifact/`. The candidate includes the pK-frozen **AA-06 ReplayGain WIP**, which is not feature-complete and was not runtime-verified here. Build-input snapshot: `freeze/BUILD_INPUTS_MANIFEST.csv` (1,027 files). Frozen manifest set under `freeze/`: SOURCE_MANIFEST.csv, PROBE_MANIFEST.csv, APK.txt and VERIFICATION.txt; verification reports **0 mismatches**. No emulator was used.

- [x] 5.6 Runtime on qa26/API 26: prior candidate **EC102F** passed the full matrix (FollowSort/Title/FileName, date toggle, persistence, compact/Home boundaries, queue/search smoke). Candidate **E2A** passed the global-search re-test: FileName mode showed the file name with date suffix, Title mode showed the song title with suffix, and long-press selection names matched in both modes. Evidence: `list-display-2026-11-02/RUNTIME-RESULT.md`. No full E2A matrix and no AA-06 testing were performed.
