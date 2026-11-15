# Tasks: Add path and general song-list track sort keys

## 1. Source audit

- [x] 1.1 Audit `MISSING_FEATURES` §5/§10 against `SortKey`, `SortItem`, `SortMode`,
  `SortCollator`, `normalizeForSorting`, adapters and preferences.
- [x] 1.2 Confirm filename, date modified and numeric collation already exist; record track-menu
  coverage and the absent path key.
- [x] 1.3 Keep the custom ignore-keyword whitelist out of scope (LIB-05) and record play-stat
  keys as a follow-up.

## 2. Implementation

- [x] 2.1 Add `SortKey.Path` (`path_key`) and `KeySortItem.Path` with the English menu title and
  id.
- [x] 2.2 Add Path to AllSongs, FolderSongs and AllFiles; sort by `filePath` with the shared
  locale collator, including file-browser folders and songs.
- [x] 2.3 Add the existing Track key to AllSongs and FolderSongs.
- [x] 2.4 Preserve existing keys, menu order, descending behavior, persistence, article
  normalization and ICU numeric collation.

## 3. Documentation

- [x] 3.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 3.2 Create this OpenSpec change and record the deferred keys/whitelist.

## 4. Verification

- [x] 4.1 Run the static source probe over the key/menu/comparator/resources.
- [x] 4.2 Run `git diff --check`.
- [x] 4.3 Run `openspec validate add-library-sort-keys --strict`.
- [x] 4.4 Shared build/lint candidate: **C5212774A021B286885AF84A99CE7000B4A020BC833B7E69D2935AB97D187C64**
  (shared with D, no duplicate library build). JDK 21 `:app:assembleGithubDebug
  :app:lintGithubDebug`: **BUILD SUCCESSFUL in 5m 30s**, 0 lint errors / 1,378 warnings.
  Universal APK: 53,443,056 bytes, mtime `2026-11-03T03:02:09.3868717Z`.
- [x] 4.5 Verified the exact APK with `probes/apk_sort_verify.ps1`: **21/21 checks PASS**
  (`SortKey.Path`/`path_key`, `KeySortItem$Path`, path resources, AllSongs/FolderSongs/AllFiles
  menu entries, `filePath` comparators, SongAdapter Path mapping, shared multi-drag classes).
  Copied to `artifact/` with SHA-256 and APK info; generated and verified
  `freeze/SOURCE_MANIFEST.csv` (14 entries) and `freeze/PROBE_MANIFEST.csv` (18 entries).
- [x] 4.6 Runtime/visual sort-menu check on qa26 / `emulator-5556` (2026-11-02): **PASSED** on
  C521. All Songs menu shows Path and Track list (plus existing keys, no play-stat/whitelist
  options). Path ascending order, Track ascending, Track descending, numeric File name,
  Date modified tiebreak, and article normalization were observed. Path and Track orders
  survived force-stop/relaunch (`song_sort_order=path_key`/`track_key`, descending false).
  Folder detail Path set `folder_song_sort_order=path_key` with The Beta before Epsilon Three;
  hierarchy file browser Path set `file_sort_order=path_key`. `emulator-5554` untouched;
  qa26/`emulator-5556` cleaned and shut down. Evidence: `C521-LIBRARY-PASS.md` and
  `runtime/C521-LIBRARY-PASS/`.
