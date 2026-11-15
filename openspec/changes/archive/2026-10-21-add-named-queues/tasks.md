# Tasks: Multiple named queues and queue-local search

> Status 2026-10-21: the L2 pending-playlist/teardown repair and final switch ordering pass 16 external queue checks, and gate 5 passed. Final-APK runtime PASS covers saved-queue switching, duplicate-name rows, missing-media switching (only valid id retained), queue-local search/clear, and the corrected named-queue DB-failure acceptance (`failure-injection/execution-output-final2.txt`). Saved resume-position switching reuses the earlier accepted artifact (`subdirector-current-checkpoint.md`, `opencode-b-phase1-report.md`); the target resume mapping was not changed by C's outgoing-save delta, and the final APK did not rerun that one assertion. Ready to archive with that attribution.

## 1. Persistence

- [x] 1.1 Add `NamedQueueEntity` and `NamedQueueItemEntity` with composite item key and DAO operations.
- [x] 1.2 Add the Room v7 to v8 migration and register the DAO/repository in `MainModule`.
- [x] 1.3 Implement save-as, rename, delete, remove-others, list and replace-items transactionally.

## 2. Playback integration

- [x] 2.1 Persist the active named-queue binding and keep its items/resume state current on queue and playback saves.
- [x] 2.2 Implement switch: save current, resolve target media ids, replace queue with target index/position and restore modes; handle missing media safely.
- [x] 2.3 Save the current queue as a named queue with a unique name and duplicate suffixes.

## 3. UI

- [x] 3.1 Replace the disabled Aurora queues manager with save/rename/delete/switch/remove-others actions and honest states.
- [x] 3.2 Enable queue-local search in the Aurora and legacy queue surfaces, filtering by title/artist/album and mapping taps to original indices.
- [x] 3.3 Add English strings in `values/strings.xml` only; keep empty/error states explicit.

## 4. Documentation and verification

- [x] 4.1 Amend ADR-036, ADR-012, CODE_MAP, FEATURES, MISSING_FEATURES and AGENTS/domain notes.
- [x] 4.2 Verify saved queues, duplicate names, switching positions, missing media and queue search on the emulator. Final-APK: switch/duplicate/missing/search PASS; saved resume-position reuses earlier accepted evidence with the unchanged target mapping.
- [x] 4.3 Run ktlint, `:app:assembleGithubDebug` and `:app:lintGithubDebug` on JDK 21.
