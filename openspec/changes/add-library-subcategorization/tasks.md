# Tasks: Library sub-categorization

## 1. Data and pure helpers

- [x] 1.1 Add pure `GenreSubcategory` (Songs, Albums, Artists) with saved-state name restoration.
- [x] 1.2 Add `GenreContent` carrying the full song snapshot plus derived albums and artists.
- [x] 1.3 Add `Repository.genreContent()` using one `songsByGenre()` query, `splitIntoAlbums()` and
  `artistsForSongs()`; expose `AlbumRepository.splitIntoAlbums()` and
  `ArtistRepository.artistsForSongs()` through their interfaces.
- [x] 1.4 Add dedicated `AlbumSortMode.GenreAlbums` and `ArtistSortMode.GenreArtists` modes.

## 2. Genre detail subcategories

- [x] 2.1 Replace the song-only view-model with content loading and add the Compose View dialog.
- [x] 2.2 Switch between `SongAdapter`, `AlbumAdapter` and `ArtistAdapter` using existing item
  layouts and detail navigation.
- [x] 2.3 Apply matching sort modes, Play/Shuffle, quick search and empty states to the visible
  subcategory; keep the header on the full genre song set.
- [x] 2.4 Save and restore the selected subcategory.

## 3. Album composer filter

- [x] 3.1 Add the album-list Filter by composer action, visible only on API 30+ when normalized
  composer data exists.
- [x] 3.2 Offer All composers plus normalized names in the existing Compose choice dialog and
  filter the existing album grid.
- [x] 3.3 Save and restore the selected composer filter.

## 4. Documentation and OpenSpec

- [x] 4.1 Create OpenSpec `add-library-subcategorization` proposal, design, spec and tasks
  (strict validation: valid).
- [x] 4.2 Update `docs/FEATURES.md`, `docs/MISSING_FEATURES.md` and `docs/CODE_MAP.md`.

## 5. Verification

- [x] 5.1 Run ktlint on all changed Kotlin files: **PASS** (`logs/ktlint-check.log`,
  exit 0).
- [x] 5.2 Run `git diff --check`: **PASS** (`logs/git-diff-check.log`, exit 0).
- [x] 5.3 Run the pure `GenreSubcategory` probe: **10/10 PASS**
  (`logs/pure-probe.log`).
- [x] 5.4 Run the static source probe: **78/78 PASS** (`logs/source-probe.log`); the
  `AlbumRepository.splitIntoAlbums()` interface override also passes the default-argument
  inheritance probe (`logs/default-inheritance.log`).
- [x] 5.5 Run `openspec validate add-library-subcategorization --strict`: **valid**
  (`logs/openspec-validate.log`).
- [x] 5.6 Shared AA-04 + LIB-03 candidate `A20119EEDF6A05A381088D8B27D3E21D987BB7EE615F2BB584344FB93FB211C8`
  (53,455,984 bytes, mtime `2026-11-03T06:25:58.3042627Z`): JDK 21 assemble/lint
  **BUILD SUCCESSFUL in 4m 51s**, 0 lint errors / 1,401 warnings. Same APK verified in the frozen
  AA-04 artifact and Gradle output, copied to `artifact/`, APK integrity **17/17 PASS**, manifests
  regenerated and verified.
- [x] 5.7 Emulator/visual check: **PASS (qualified)** on qa26/API 26, candidate
  `A20119EE…211C8`. Verified genre Songs/Albums/Artists switching, full-genre header, album/artist
  counts, sort menu and reorder, quick search, 5-song play queue, and API 26 composer-filter
  absence. API 30+ composer filtering was not rerun in the resumed pass (no stable task API 30+
  AVD); source probe covers it. Evidence: `library-subcategories-2026-11-02/RUNTIME-RESULT.md`.
