# Tasks: Queue multi-item drag (follow-up D)

## 1. Policy

- [x] 1.1 Add pure `QueueBatchMovePolicy` for selection-size validation, drop direction,
  timeline block moves, relative order and old-to-new index mapping.
- [x] 1.2 Reject fewer than two selections, target-selected drops and invalid indices.
- [x] 1.3 Process selected items from the correct end when moving down/up.

## 2. Service and ViewModel

- [x] 2.1 Add the app-internal `Playback.MOVE_QUEUE_BLOCK` command and from/to extras.
- [x] 2.2 Add the service handler: validate, repeated `moveMediaItem`, capture current/position/
  stop index, remap stop/current, and `persistentStorage.saveState(true)`.
- [x] 2.3 Add `PlayerViewModel.moveQueueBlock`: resolve selected keys and target through
  `QueuePosition`, call the policy, dispatch move arrays and report failure.

## 3. Queue UI

- [x] 3.1 Add key-based selection state, long-press entry, tap toggle, select all/cancel and count.
- [x] 3.2 Enable handles only for selected rows with at least two selected; highlight drop targets.
- [x] 3.3 Disable selection/drag for a non-blank query or locked queue; clear selection on change.
- [x] 3.4 Preserve swipe and single-item drag outside selection.
- [x] 3.5 Add main English selection strings.

## 4. Documentation and OpenSpec

- [x] 4.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 4.2 Create this OpenSpec change.

## 5. Verification

- [x] 5.1 Run the exhaustive pure `QueueBatchMovePolicy` probe: **131,536 checks PASS** across
  11,760 actual move cases (permutations, subsets, targets, both directions, duplicates,
  relative order, block contiguity, old-to-new mapping and future/current stop-after remap).
- [x] 5.2 Run the static source probe: **79 checks PASS** (selection keys, long-press/tap,
  selected-only handles, search/lock disable, QueuePosition mapping, service command/registration,
  repeated moves, SET_STOP_POSITION-compatible future/current remap, full save, scope).
- [x] 5.3 Run ktlint and `git diff --check`: pass.
- [x] 5.4 Run `openspec validate add-queue-multi-drag --strict`: valid.
- [x] 5.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL in 5m 30s**,
  0 lint errors / 1,378 total issues (1,037 warning-level + 341 informational). The shared tree
  also contains the disjoint library-sort source. Final universal APK
  `C5212774A021B286885AF84A99CE7000B4A020BC833B7E69D2935AB97D187C64` (53,443,056 bytes, mtime
  `2026-11-03T03:02:09.3868717Z`), copied to `artifact/` with `SHA256.txt`. Five warning-level
  increases are the expected main-only multi-drag `MissingTranslation` warnings.
- [x] 5.6 Device runtime on C521: **FAIL — row-0 downward block**. qa26 / `emulator-5556`
  installed the exact hash. `input swipe` now proves the injection path: single-item drag passed,
  block-above passed, block-down from row 1 passed, but block-down from row 0 consistently left
  `QueueEntity` unchanged with `2 selected` still visible. Root cause: reorder callback mapped
  `LazyListItemInfo.index` through `currentQueueContentOffset`. Evidence:
  `gesture-diagnosis/C521-GESTURE-DIAGNOSIS-FAIL.md`,
  `runs/simple-*`, `runs/block-*`, `runs/block-source-*`. Closed by `queue-drag-fix`: 5.10
  removed that mapping from the block path and 5.12 passed block-down from row 0.
- [x] 5.7 Apply key-based reorder resolution (`QueueSong.key` via `playQueue.indexOfFirst`);
  ktlint PASS, static probe **21/21 PASS**, `git diff --check` PASS. No build at patch time.
- [x] 5.8 JDK 21 combined build `:app:assembleGithubDebug :app:lintGithubDebug`:
  **BUILD SUCCESSFUL in 3m 26s**, 0 lint errors / 1,393 warnings. Patched universal APK:
  `A9E620CF33287A527306D15D68C70E7FFE646FA2F3977711740A68996EF70DD7`,
  53,451,588 bytes, mtime `2026-11-02T20:46:01.4708605-08:00`, frozen in
  `artifact-patched/` with `SHA256.txt`.
- [x] 5.9 A9E key-based-patch retest: single, above and row-1 down passed, but row-0 down still
  failed (order unchanged, `2 selected` remained). Reorderable did not report an outside target
  for the first selected row. Superseded by 5.10. Closed by `queue-drag-fix`: block drags use
  the 5.10 key-resolved pointer path, and 5.12 passed block-down from row 0 on CB06.
- [x] 5.10 Add block-only custom pointer/drag path: selection mode with >=2 selected uses
  `detectDragGestures` on the selected row handle, source/item center from
  `listState.layoutInfo` by key, accumulated delta -> row under center -> key-resolved
  `blockDropTarget`, list scroll disabled while block-dragging, `moveQueueBlock` on drop.
  Single-item `draggableHandle` path unchanged. Static probe **38/38 PASS**; ktlint PASS.
- [x] 5.11 JDK 21 build of custom-block candidate: **BUILD SUCCESSFUL in 6m 37s**,
  0 lint errors / 1,395 warnings. APK
  `CB0693262BDC03F2FB18BEC5346492B4384692094A1322AC51223265D1C755DD`,
  53,452,728 bytes, mtime `2026-11-02T21:10:11.2784083-08:00`, frozen in
  `artifact-custom-block/` with `SHA256.txt`.
- [x] 5.12 CB06 device retest on qa26/5556: positive single drag, block-above, block-down
  row0 and row1 all **PASS**; selection cleared after every block drop; Epsilon Three stayed
  current at 12,000 ms paused. Final order `[1357,1355,1356,1355,1358,1359]`. Evidence:
  `gesture-diagnosis/runs/patched-20261102-211440/`, `CB06-PASS.md`.
- [x] 5.13 Cleanup: app and test package removed, task media and QA temp files removed,
  `emulator-5556` shut down, `adb devices` empty; `emulator-5554` untouched.
- [x] 5.15 Final integrated-build emulator retest (2026-11-14): selected rows 1–2 in the queue,
  dragged the row-0 handle downward, and verified `QueueEntity` order changed
  `[24,25,26] -> [26,24,25]` with the selection cleared. Evidence:
  `runtime/verify/results/evidence/24-selection2.*`, `25-after-drag-row0.*`. API 26
  `input` long-press injection was unreliable, so the pass is recorded on the API 30
  session; the row-0 block behavior is API-independent.
- [x] 5.14 Regenerate the CB06 freeze: `SOURCE_MANIFEST.csv` (14 entries incl. QueueScreen,
  strings, docs and this tasks file), `PROBE_MANIFEST.csv` (15 entries incl. the
  key-resolution probe/log, policy/source logs and ktlint log), `APK.txt` and `VERIFY.txt`;
  mismatches 0, APK hash matches CB06, app source older than APK.
