# Tasks: Playlist import conflict policy

> Status 2026-11-02: complete on final candidate `5F6243579E55F85B38F55DFAF9E7300F3A472E7B883932F8A7CFCA80654EA42E` (53,397,296 B, `2026-11-02 11:28:55`); probe and API26 runtime PASS.

## 1. Policy and data layer

- [x] 1.1 Add pure `PlaylistImportNameResolver` for Keep-both suffix 1..100 and trimmed rename validation (blank/duplicate).
- [x] 1.2 Add DAO exact-name/by-ID queries and repository `database.withTransaction` create+insert and replace-contents operations; preserve replace target metadata.
- [x] 1.3 Forward transactional operations through `PlaylistRepository`/`Repository`; keep exact case-sensitive lookup and no schema/unique-index change.
- [x] 1.4 Extend `ImportResult` with actual stored name, outcome and imported song count.

## 2. ViewModel and dialog

- [x] 2.1 Fix the selected-subset bug and deduplicate selected songs by ID with first occurrence preserved.
- [x] 2.2 Add serialized pending-import state: no conflict imports directly; exact conflict exposes existing matches, counts, Keep-both default and Replace eligibility.
- [x] 2.3 Implement Keep both (default, suffix exhaustion disables), Replace (single exact non-Favorites, transactional revalidation) and Rename (new playlist, existing untouched, inline blank/duplicate).
- [x] 2.4 Implement Compose conflict dialog with selectable semantics, disabled applying state, inline errors and cancel/no-mutation behavior.
- [x] 2.5 Use actual stored names in success messages and refresh playlists after commit.
- [x] 2.6 Keep backup restore on its separate non-interactive merge path and document it; do not call the dialog.

## 3. Strings and documentation

- [x] 3.1 Add only main English strings to `values/strings.xml`.
- [x] 3.2 Update FEATURES, MISSING_FEATURES and CODE_MAP, including the separate backup-merge policy.
- [x] 3.3 Add this OpenSpec change and validate strictly.

## 4. Verification

- [x] 4.1 Run ktlint format/check and `git diff --check` (PASS).
- [x] 4.2 Add/run a focused compiled probe for resolver names/validation (PASS).
- [x] 4.3 Build `:app:assembleGithubDebug :app:lintGithubDebug` on JDK 21 with `--no-daemon` (PASS, 2m 53s).
- [x] 4.4 Report final APK SHA-256/size/mtime and freeze playlist source (5F62…, 53,397,296 B, 11:28:55).
- [x] 4.5 API26 runtime: subset import, Keep both suffix, Replace ID/metadata/contents, Rename, Cancel — PASS.
- [x] 4.6 Clean qa26 only, preserve `emulator-5554`, write IMPLEMENTATION/RUNTIME/DELTA and update these tasks.
