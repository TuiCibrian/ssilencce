# Tasks: Composers and Album-Artists library tabs

## 1. Data

- [x] 1.1 Add pure `ComposerNamePolicy` for normalization, case-insensitive keys and stable IDs.
- [x] 1.2 Add nullable `Song.composerName`, read `MediaStore.MediaColumns.COMPOSER` only on API 30+.
- [x] 1.3 Add nullable `Artist.composerName`/`isComposer` and composer grouping/query methods.
- [x] 1.4 Expose composers through `Repository`, `LibraryViewModel` and `ReloadType`.

## 2. Tabs and navigation

- [x] 2.1 Add AlbumArtists/Composers categories with availability and persisted-list merging.
- [x] 2.2 Add nav destinations, MainActivity handling, titles/icons and main English strings.
- [x] 2.3 Add AlbumArtistListFragment/ComposerListFragment reusing the artist list base.
- [x] 2.4 Preserve existing Artists behavior when Album artists is hidden; separate it when visible.

## 3. Detail and actions

- [x] 3.1 Reuse ArtistDetailFragment with nullable `composer_name` and composer lookup.
- [x] 3.2 Hide composer-inapplicable tag-editor/add-home/play-info actions and skip bio/similar.
- [x] 3.3 Keep playback, shuffle, multi-select and song/album navigation consistent.

## 4. Documentation and OpenSpec

- [x] 4.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 4.2 Create this OpenSpec change.

## 5. Verification

- [x] 5.1 Run the pure ComposerNamePolicy probe: **15/15 PASS**, including whitespace, case and
  non-ASCII detail-filter matching.
- [x] 5.2 Run the static source probe: **67/67 PASS**, including the Kotlin-normalized composer
  detail path and absence of ASCII-only SQL `lower()`.
- [x] 5.3 Run ktlint (PASS) and `git diff --check` (PASS).
- [x] 5.4 Run `openspec validate add-library-composer-albumartist-tabs --strict` (valid).
- [x] 5.5 Shared candidate `2683786EE13F19B36584B80439F27C3AD4EDEAE6B1D145BECC7FFB862D92441D`
  (53,452,728 bytes, mtime `2026-11-03T05:30:55.8958982Z`): JDK 21
  `:app:assembleGithubDebug :app:lintGithubDebug`, **BUILD SUCCESSFUL in 4m 24s**, 0 lint errors /
  1,395 warnings. APK probe **26/26 PASS**; copied to `artifact/` with SHA-256; source/probe
  manifests generated and verified. Supersedes CB06 after the composer-detail whitespace/case/
  non-ASCII fix.
- [x] 5.6 Runtime/visual composer/album-artist check: **PASS (qualified)**. API 26 qa26 verified
  Composers hidden, Album artists separate from Artists, counts/detail/play/search, category
  enable/disable, `onlyAlbumArtists` fallback, queue end footer and reshuffle. API 36 task AVD
  verified the 2683786 composer-detail fix with `'  John   Williams '` / `'john williams'` and
  `'Édith Piaf'` / `'ÉDITH PIAF'`: list non-empty, normalized names, matching songs, detail search,
  play and hidden composer-inapplicable actions. Both task emulators were cleaned and shut down;
  evidence in `lib01-2026-11-02/LIB01-RUNTIME-RESULT.md`.
