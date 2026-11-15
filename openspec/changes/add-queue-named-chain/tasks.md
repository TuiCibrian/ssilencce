# Tasks: Named-queue chain (follow-up B)

## 1. Stable order and policy

- [x] 1.1 Expose `createdAt` on `NamedQueueSummary`; use `created_at ASC, id ASC` for both
  summariesFlow and summaries without schema changes.
- [x] 1.2 Add pure `NamedQueueChainPolicy` for First/Previous/Next, boundaries and no-active.
- [x] 1.3 Add repository `orderedQueueIds()` from the same summaries query.

## 2. ViewModel and UI

- [x] 2.1 Add active/empty guards and First/Previous/Next actions that reuse
  `switchToNamedQueue`.
- [x] 2.2 Add manager First/Previous/Next controls while preserving direct row switching.
- [x] 2.3 Add main English chain/boundary/no-active strings.

## 3. Documentation and OpenSpec

- [x] 3.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 3.2 Create this OpenSpec change.

## 4. Verification

- [x] 4.1 Run the pure `NamedQueueChainPolicy` probe (19 checks), including exact active-queue
  identity, reorder/modified/count mismatches, first/previous/next boundaries and no-active.
- [x] 4.2 Run the static source probe (53 checks), including identity loading/comparison,
  deleted/empty target messages, active/empty guards and unified createdAt/id summaries ordering.
- [x] 4.3 Run ktlint and `git diff --check`.
- [x] 4.4 Run `openspec validate add-queue-named-chain --strict`.
- [x] 4.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: BUILD SUCCESSFUL, 0 lint
  errors (1,026 warnings). Final universal APK
  `44967BE1755B7CB47C698CD2380B5B0AEFA522BEB27C85EE1B5803248EB3201A` (53,437,036 bytes,
  mtime `2026-11-03T01:35:54Z`), copied to `artifact/` with `SHA256.txt`.
- [x] 4.6 Device runtime on qa26 / `emulator-5556` (2026-11-02): **PASSED** on 44967.
  Manager order `created_at ASC, id ASC` (A, B, C); renaming B to B2 changed only `updated_at`
  and did not move it. Active B Next -> C restored C's saved index 1 / 15,000 ms; Previous -> B
  restored the ~18.2 s ADR-046 outgoing position; Previous -> A loaded A; first/last boundaries
  showed the correct no-wrap messages; First loaded A. Active B with timeline A and a
  reordered B snapshot both showed `This queue is not the active saved queue.` with no switch.
  Deleted active B showed unavailable with no switch; empty target D showed the empty message
  with C intact; auto-cleared single-item E showed the no-active message. Ended non-empty A
  still showed the footer; no re-shuffle or multi-drag controls appeared. Direct manager rows
  still switch. `emulator-5554` was untouched; qa26/`emulator-5556` was cleaned and shut down.
  Evidence: `44967-PASS.md`, `runtime/44967-PASS/` and `runtime/evidence/`.
