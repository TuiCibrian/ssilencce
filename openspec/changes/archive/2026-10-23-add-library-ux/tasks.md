# Tasks: Library history, search, export and shortcuts

> Status 2026-10-23: export replacement acceptance PASSED on APK `096C…B200` (gate 5): 6/6 current-APK cases, including fresh and same-name M3U8, M3U and PLS, exact names/MIME and preserved old bytes. Failure injection was helper-only; pre-Q and physical-device provider behavior remain unverified. Final-APK shortcut confirmation restored across landscape/portrait and added/updated `playlist-2`; stale-shortcut removal PASS (`playlist-3` for Test Playlist 2 no longer in `dumpsys shortcut` after UI deletion; DB/prefs restored). Gate-5 registry capacity/auto-eviction PASS: `maxShortcutsPerActivity=15`, four static shortcuts, `D=11`; the 12th push was accepted at rank 0 as `playlist-11` and `playlist-4` was evicted. The normal API 36 path reports success, not a capacity failure, so no limit Toast is required. The launcher menu showed only 2 of 11 dynamic shortcuts; this is recorded as a launcher display limitation, not an app rejection.

## 1. Selective history deletion (B)

- [x] 1.1 Expose batch history deletion through `Repository`/`SmartRepository` and `LibraryViewModel`.
- [x] 1.2 Add a history-only `Remove from history` action to the song overflow menu and to multi-select, with confirmation.
- [x] 1.3 Verify single and multi-select removal only deletes history rows; queue and play counts remain.
- [x] 1.4 Add English strings and update feature docs.

## 2. Search (G)

- [x] 2.1 Add a settings search entry point and an index over all preference sections.
- [x] 2.2 Navigate from a settings result to its section and scroll to the preference when possible.
- [x] 2.3 Add a reusable quick-search bar to `AbsRecyclerViewFragment` and filtering implementations for songs, albums, artists, playlists, genres, years and folders.
- [x] 2.4 Ensure quick search resets on tab change, data reload and empty query; keep selection and sorting behavior intact.

## 3. Playlist export formats (I)

- [x] 3.1 Refactor `M3UWriter` to support M3U, M3U8 and PLS while preserving the existing M3U output.
- [x] 3.2 Add a format chooser to single and multi-playlist export.
- [x] 3.3 Verify extension, encoding and content for all three formats; verify empty playlists do not create empty files.

## 4. Per-entity launcher shortcuts (J)

- [x] 4.1 Add a shortcut helper for albums, artists, folders and playlists with ids/paths and labels.
- [x] 4.2 Add confirmation actions to the entity menus and an `add to home screen` menu entry.
- [x] 4.3 Handle `com.ssilencce.shortcut.ENTITY` in `MainActivity`, resolving the entity and opening its queue; report unavailable entities.
- [x] 4.4 Report launcher capacity failures and remove stale dynamic shortcuts when an entity is deleted.
  - The `shortcut_limit_reached` Toast remains wired to `ShortcutHelper.add()` returning false; normal API 36 capacity auto-evicts and is not a failure branch.
  - Gate-5 capacity/auto-eviction PASS: at `maxShortcutsPerActivity=15` with four static shortcuts (`D=11`), the 12th push added `playlist-11` at rank 0 and evicted `playlist-4`; no crash.
  - Stale dynamic shortcut removal passed after UI playlist deletion (`playlist-3` absent from `dumpsys shortcut`); DB/prefs were restored.
  - Launcher-visible limitation: the current launcher showed 2 of 11 registry shortcuts and also omitted two static shortcuts; this is display/slot policy, not app rejection. Pre-Q and physical-device provider behavior remain unverified.

## 5. Integration and verification

- [x] 5.1 Add strings in `values/strings.xml` only and keep UI primarily Compose where new screens are added.
- [x] 5.2 Update CODE_MAP, FEATURES, MISSING_FEATURES and any affected ADR/AGENTS notes.
- [x] 5.3 Run ktlint on edited Kotlin files, then `:app:assembleGithubDebug` and `:app:lintGithubDebug` on JDK 21.
- [x] 5.4 Emulator-check history removal, settings search, one quick-search list, all export formats and one entity shortcut.
