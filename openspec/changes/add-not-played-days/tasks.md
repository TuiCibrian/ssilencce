# Tasks: Arbitrary Not Recently Played threshold

## 1. Pure policy and persistence

- [x] 1.1 Add `NotPlayedDaysPolicy` with the use-history sentinel, 1–3650 validation/clamping and
  cutoff arithmetic.
- [x] 1.2 Add the `not_played_days` preference and `Preferences.getNotPlayedCutoff()`, falling back
  to the existing history interval for the sentinel.

## 2. Smart filtering and UI

- [x] 2.1 Use `getNotPlayedCutoff()` in `RealSmartRepository.notRecentlyPlayedSongs()`.
- [x] 2.2 Expose a reloadable Not Recently Played LiveData from `LibraryViewModel`.
- [x] 2.3 Add the Compose `NotPlayedDaysDialogFragment` with history-interval and custom-day
  choices, numeric validation and a fragment result.
- [x] 2.4 Show the action only for `ContentType.NotRecentlyPlayed`, persist the result, reload the
  list and show the effective threshold in the subtitle.
- [x] 2.5 Add the menu item and main English strings/plurals.

## 3. Documentation and OpenSpec

- [x] 3.1 Create OpenSpec `add-not-played-days` proposal, design, spec and tasks (strict
  validation: valid).
- [x] 3.2 Update `docs/FEATURES.md`, `docs/MISSING_FEATURES.md` and `docs/CODE_MAP.md`.

## 4. Verification

- [x] 4.1 Run ktlint on all changed Kotlin files: **PASS** (`logs/ktlint-check.log`).
- [x] 4.2 Run `git diff --check`: **PASS** (`logs/git-diff-check.log`).
- [x] 4.3 Run the pure `NotPlayedDaysPolicy` probe: **22/22 PASS**
  (`logs/pure-probe.log`).
- [x] 4.4 Run the static source probe: **68/68 PASS** (`logs/source-probe.log`).
- [x] 4.5 Run `openspec validate add-not-played-days --strict`: **valid**
  (`logs/openspec-validate.log`).
- [x] 4.6 Build shared AA-09 + LIB-13 candidate `84DAE5A43920D0A4548BD84E14AD291D43DC43A98CC6FFD564CA76C413002899`
  (53,462,628 bytes, mtime `2026-11-04T00:15:48.2526480Z`): JDK 21 assemble/lint
  **BUILD SUCCESSFUL in 4m 56s**, **0 lint errors / 1,413 warnings**. Copied to `artifact/`;
  APK integrity **18/18 PASS**; manifests regenerated and verified. AA-09 source unchanged.
- [x] 4.7 Runtime/visual verification on qa26/API 26 with 84DA: **PASS (qualified)**.
  Blank/non-numeric/0/10000/36500 showed the 1–3650 error with no save; boundaries 1 and 3650
  saved/applied; custom 1 day persisted after relaunch; Use history interval restored
  `This month` fallback. Evidence: `lib13-not-played-2026-11-04/LIB13-RUNTIME-RESULT.md`.
