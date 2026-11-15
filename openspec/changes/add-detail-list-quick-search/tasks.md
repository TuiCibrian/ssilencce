# Tasks: Inline quick search on detail/secondary lists

## 1. Shared support

- [x] 1.1 Add pure `QuickSearchPolicy` for case-insensitive matching and no-match semantics.
- [x] 1.2 Add `QuickSearchHost` with Compose bar hosting, saved-state key, mini-player/navigation
  margin and 72 dp list padding.
- [x] 1.3 Add model field mapping (`DetailQuickSearch`) for songs, albums, artists and
  `ArtistPlayStat`.

## 2. Secondary screens

- [x] 2.1 Add the bar to `DetailListFragment` for all content types, with full snapshots,
  Top Artists stat filtering, filtered Play/Shuffle, no filtered navigate-up and saved query.
- [x] 2.2 Add songs-only filtering to album and artist detail, including section-header
  no-match and filtered header playback.
- [x] 2.3 Add filtering to genre, year and folder detail songs; folder includes file name.
- [x] 2.4 Add drag-safe playlist filtering: forced lock, filtered display list, full-order save
  overload, clear restoration and loaded guard.

## 3. Layout and documentation

- [x] 3.1 Add the bottom `ComposeView` `quick_search_bar` to the four detail layouts.
- [x] 3.2 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 3.3 Create this OpenSpec change.

## 4. Verification

- [x] 4.1 Run ktlint and `git diff --check`.
- [x] 4.2 Run the focused pure filter probe (17 checks).
- [x] 4.3 Run `openspec validate add-detail-list-quick-search --strict`.
- [x] 4.4 Run JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: BUILD SUCCESSFUL,
  0 lint errors; source probe 154 checks.
- [x] 4.5 Freeze source and record APK hash/size/mtime and evidence; universal APK
  `2C59CA115F79DEB4D1A85AC9A0DD2EBDA1F056BBB015D256950E6358D33A0756` (copy and SHA-256 in
  `artifact/`).
- [x] 4.6 Deferred to w5:pG runtime: visual/accessibility, rotation, large-list performance and
  playlist DB integrity checks. Do not start an emulator from this change.
  **Blocked 2026-11-02:** the requested 9CEC artifact was overwritten by E997 (which adds the
  landscape `quick_search_bar` fix); E997 was not tested per user. Empty-source semantics and
  Top Artists Play still need source fixes; runtime resumes on the next frozen candidate.
  Details:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/CANDIDATE-MISMATCH.md`.
  **2C59 runtime stopped 2026-11-02:** base hash matched, but Top Tracks rendered the
  quick-search placeholder at `[179,1605][499,1668]` underneath the bottom navigation starting
  at `y=1608`; taps are intercepted. A navigation-aware bar-margin source fix is incoming;
  2C59 is not final. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/2C59-OVERLAP.md`.
  **8519 runtime stopped 2026-11-02:** base hash matched, but Top Tracks measured
  `quick_search_bar=[0,1521][1080,1689]` against `auroraNavigation=[0,1605][1080,1794]`
  — an 84 px overlap. `barMargin` uses `getFabMargin()` (0 without a FAB), so the bar is
  not fully above the nav. A navigationHeight-based margin fix is incoming; 8519 is not final.
  Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/8519-OVERLAP.md`.
  **1858 runtime stopped 2026-11-02:** base hash matched, but the navigationHeight max fix still
  measured `quick_search_bar=[0,1521][1080,1689]` against
  `auroraNavigation=[0,1605][1080,1794]` — the same 84 px overlap. `navigationHeight` resolves
  to ~105 px while the actual Aurora surface is 189 px. No further checks were run; 1858 is not
  final. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/1858-OVERLAP.md`.
  **1475 runtime stopped 2026-11-02:** base hash matched. Mini-player hidden passes (bar
  `[0,1437][1080,1605]`, nav top `1605`, last row bottom `849`; Top Tracks/Album/Playlist/
  rotation/no-match). Mini-player visible fails: bar `[0,1337][1080,1505]` overlaps
  `mini_player_container` `[32,1437][1048,1589]` by 68 px, so the field is covered. List padding
  is correct (last row bottom `1417` above mini top `1437`). 1475 is not final. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/1475-MINIPLAYER.md`.
  **B96D runtime stopped 2026-11-02:** base hash matched. Hidden state passes. Visible state:
  bar `[0,1337][1080,1505]` overlaps mini-player `[32,1437][1048,1589]` by 68 px, and the
  last row ends at `1417`, overlapping bar top `1337` by 80 px. List padding (`nav + 72 dp`
  = 378 px) does not clear the bar. B96D is not final. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/B96D-PARTIAL.md`.
  **PASSED 2026-11-02 on 8767:** base hash matched. Hidden: bar `[0,1437][1080,1605]` ==
  nav top `1605`, last row bottom `849`. Visible: bar `[0,1248][1080,1416]`, mini-player
  `[32,1437][1048,1589]` (21 px gap, bar above mini); last list row bottom `849` above bar
  top. Top Tracks filter/no-match/clear, Playlist filter/drag-lock/full-list DB persistence,
  and landscape rotation (bar `[0,408][1794,576]` vs mini `[32,597][1762,749]`, query
  preserved) passed. qa26 cleaned; 5556 shut down; 5554 untouched. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/8767-PASS.md`.
  **FINAL PASS 2026-11-02 on 1029:** base hash matched. Hidden bar
  `[0,1437][1080,1605]` == nav top, last row `849`. Visible bar `[0,1248][1080,1416]`,
  mini-player `[32,1437][1048,1589]` (21 px gap), last row `849` above bar top. Top
  Tracks filter/no-match/clear, Playlist filter/drag-lock/full-list DB, and landscape
  rotation (bar `[0,408][1794,576]` vs mini `[32,597][1762,749]`, query preserved)
  passed. qa26 cleaned; 5556 shut down; 5554 untouched. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/detail-quick-search-2026-11-02/runtime/1029-PASS.md`.
