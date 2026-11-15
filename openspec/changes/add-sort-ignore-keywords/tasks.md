# Tasks: User-editable sort ignore-keywords

## 1. Policy and storage

- [x] 1.1 Add pure `IgnoreKeywordPolicy`/`SortNormalizer` for built-in plus custom leading
  keywords, sanitization, add/remove validation and limits.
- [x] 1.2 Add `Preferences.sortingIgnoreKeywords` backed by a `StringSet`; default empty.
- [x] 1.3 Gate custom keywords on the existing article toggle; no dedicated switch.

## 2. Sorting integration

- [x] 2.1 Reuse one normalizer per `SortMode.sorted()` call and in `SortCollator.sortedByMediaName`.
- [x] 2.2 Apply the same keyword set to `asSectionName` section letters.
- [x] 2.3 Leave search, raw titles and displayed metadata untouched.

## 3. Settings UI

- [x] 3.1 Add the dependent `CustomDialogPreference` row and Compose add/remove dialog.
- [x] 3.2 Add count summary, validation errors and main English strings only.
- [x] 3.3 Route the row from `PreferencesScreenFragment`.

## 4. Documentation and OpenSpec

- [x] 4.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 4.2 Create this OpenSpec change.

## 5. Verification

- [x] 5.1 Run the pure policy probe: **21/21 checks PASS**.
- [x] 5.2 Run the static source probe: **54/54 checks PASS**; ktlint check PASS.
- [x] 5.3 Run `git diff --check` (PASS) and `openspec validate add-sort-ignore-keywords --strict` (valid).
- [x] 5.4 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL in 2m 54s**,
  0 lint errors / 1,390 warnings. APK `2B2349CB0D8A8B997B4D666774BDD42F6DE6A302C7E29DD9297CCB0722096BD3`
  (53,449,852 bytes, mtime `2026-11-03T03:49:33.0414443Z`); APK keyword probe 17/17; copied to
  `artifact/` with SHA-256; `freeze/SOURCE_MANIFEST.csv` and `freeze/PROBE_MANIFEST.csv` generated
  and verified.
- [x] 5.5 Blank-input feedback: Add remains enabled for blank input, `IgnoreKeywordAddResult.Blank`
  maps to `sorting_ignore_keywords_blank`, and the source probe covers the reachable blank path.
- [x] 5.6 Runtime/visual check on qa26 / `emulator-5556` (2026-11-02): **PASSED** on 2B2349.
  Blank Add shows `Enter a keyword.` and does not add; duplicate and 41-char inputs are rejected;
  40 chars are accepted. With `Zebra` stored and the article toggle on, `Zebra Gamma` sorts as
  `Gamma` while the displayed title and search stay raw; removing it restores literal order.
  Toggle off disables the row and ignores custom keywords. `Zebra` and the custom-ordered result
  survived force-stop/relaunch. A/An/The and numeric behavior remained intact. The 50-count UI
  batch was stopped by direction; the source/policy probes cover the 50-cap (21/21 and 52/52).
  `emulator-5554` untouched; qa26/`emulator-5556` cleaned and shut down.
  Evidence: `2B23-PASS.md`, `runtime/2B23-PASS/`.
