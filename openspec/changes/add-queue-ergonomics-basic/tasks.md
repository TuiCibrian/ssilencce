# Tasks: Basic queue ergonomics

## 1. Swipe removal

- [x] 1.1 Wrap queue rows in `SwipeToDismissBox` (right only) enabled only when unlocked, query
  blank and no reorder is active.
- [x] 1.2 Remove by original queue index, show a Snackbar with Undo and reinsert through
  `restoreRemovedPosition`.
- [x] 1.3 Keep the overflow removal fallback and leave multi-drag/end-of-queue out of scope.

## 2. Named queues

- [x] 2.1 Add pure `QueueNamePolicy` (100-queue cap, 80-char names, validation, suffix rule).
- [x] 2.2 Serialize create/replace/rename in `NamedQueueRepository`; add snapshot-only replace and
  optional rename replace target.
- [x] 2.3 Add Save/Rename duplicate choice dialog: Keep both (default), Replace existing with
  confirmation, Rename; no Merge.
- [x] 2.4 Add actual-name/limit/name-length messages and inline name-length validation.

## 3. Documentation and OpenSpec

- [x] 3.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 3.2 Create this OpenSpec change.

## 4. Verification

- [x] 4.1 Run the pure `QueueNamePolicy` probe.
- [x] 4.2 Run the static source probe for swipe guards, original indices, undo, duplicate policy,
  limits, no Merge/multi-drag/chaining and unchanged schema/deps.
- [x] 4.3 Run ktlint and `git diff --check`.
- [x] 4.4 Run `openspec validate add-queue-ergonomics-basic --strict`.
- [x] 4.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: BUILD SUCCESSFUL, 0 lint
  errors. Final universal APK
  `7F9BBD2FE0BB8B196AE211002BB223CA87DA4F3736184C392D87C6A18E85CF8E` (53,431,516 bytes,
  mtime `2026-11-03T00:12:46Z`), copied to `artifact/` with `SHA256.txt`.
- [x] 4.6 Deferred to runtime: device checks for swipe/undo, duplicate Save/Rename, limits and
  the queue regressions. No emulator was started by this change.
  **PASSED 2026-11-02 on 7F9B:** installed base hash matched. One swipe removed exactly one
  row; a second swipe during the Undo snackbar was ignored; Undo restored all four rows.
  Locked/query-nonblank guards kept the queue unchanged. Duplicate Keep/Replace/Rename
  semantics and active marker verified. 81-char and 100-queue caps held via UI/source.
  qa26 cleaned; 5556 shut down; 5554 untouched. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/queue-ergonomics-2026-11-02/RUNTIME.md`
  and `runtime/7F9B-PASS/`.
