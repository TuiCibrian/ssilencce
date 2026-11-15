# Tasks: Automatic statistics CSV

> Status 2026-11-02: source, docs and OpenSpec implemented. Candidate 06F0 was retired after
> runtime exposed doubled `auto-top-top-` file names; source now freezes the single-prefix
> `tracksName`/`artistsName` fix. New candidate: universal APK
> `93F3D4B80973817BB69C9388F87EA628BDBAE08CE121A98610C1500424F3AB58`. ktlint, 66-check
> period probe, 76-check source probe, OpenSpec strict and JDK 21 build/lint pass. qa26 runtime
> on 93F3 is the only remaining check; no source/APK changes after this freeze.

## 1. Period and range data

- [x] 1.1 Add pure `StatisticsCsvCadence`, `StatisticsCsvWindow` and `StatisticsCsvSchedule`
  for latest completed week/month/year, inclusive file end and exclusive query end.
- [x] 1.2 Add `PlayEventDao.topSongsBetween`/`topArtistsBetween` with
  `played_at >= :start AND played_at < :end` and a range-restricted artist denominator.
- [x] 1.3 Add `Repository`/`SmartRepository` range stats methods while leaving
  `ListeningPeriod`/all-time behavior unchanged.
- [x] 1.4 Add `StatisticsCsvExporter.prepareAutomatic` and auto file names using the existing
  `StatisticsCsvWriter`; keep manual `prepare`/`write` unchanged.

## 2. Schedule, store and worker

- [x] 2.1 Add `StatisticsCsvStore` with cadence/folder/result/per-cadence last keys/pending key.
- [x] 2.2 Add `StatisticsCsvScheduler` unique daily WorkManager work with battery/storage
  constraints and UPDATE policy.
- [x] 2.3 Add `StatisticsCsvWorker` latest/pending target selection, persisted-key gate,
  feature-owned `.partial` staging with rename-after-write, stale-staging cleanup,
  existing-file idempotency, zero-byte verification, NoData, handled failure/pending and
  cancellation cleanup.
- [x] 2.4 Add `StatisticsCsvController` SAF read/write persistence, configure/schedule/cancel
  and state refresh.

## 3. Settings and strings

- [x] 3.1 Add Compose `StatisticsCsvDialogFragment` and `StatisticsCsvPreference` in a new
  Advanced Statistics category.
- [x] 3.2 Add only main English strings to `values/strings.xml`.
- [x] 3.3 Add this OpenSpec change (proposal, design, spec, tasks, `.openspec.yaml`).

## 4. Documentation

- [x] 4.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP` for automatic cadence, completed
  windows, SAF/WorkManager behavior and no-backfill/no-retention rules.
- [x] 4.2 Keep manual CSV export and its documentation unchanged.

## 5. Verification

- [x] 5.1 Run ktlint check/format on changed Kotlin files and `git diff --check`.
- [x] 5.2 Run a fixed-clock pure period probe and a static source probe for range predicates,
  worker/scheduler/SAF, single-prefix auto names, staged `.partial` delivery, no empty CSV, no
  deps/permissions/schema/translated strings and unchanged manual export.
- [x] 5.3 Run `openspec validate add-automatic-statistics-csv --strict`.
- [x] 5.4 Combined JDK 21 build/lint: `BUILD SUCCESSFUL`; lint 0 errors / 1,339 warnings; candidate APK
  `93F3D4B80973817BB69C9388F87EA628BDBAE08CE121A98610C1500424F3AB58`; lint SARIF
  `21:33:54Z`, after the `21:27:01Z` filename-helper source.
- [x] 5.5 API26 runtime on candidate `93F3D4B8...` (completed windows, up to two files, staged
  `.partial` cleanup and rename, NoData, duplicate suppression, cadence/disable, manual export).
  **Passed on qa26/emulator-5556:** the installed base hash matched 93F3; the completed week
  `2026-10-26..2026-11-01` produced exactly two single-prefix CSVs (tracks 142 B / artists 74 B;
  contents, 3/1 plays and 75/25 artists verified). A seeded `.partial` was removed; a real second
  worker run with the weekly key set left both files byte-identical (no duplicate/rewrite);
  Monthly NoData recorded `last_result=NoData` / `last_monthly_key=monthly:2026-10`, wrote no
  monthly file and removed a seeded `.partial`. Manual export code/UI were unchanged and are
  covered by the 5.2 static probe, not re-run in this focused automatic pass. 06F0 remains
  retired. Evidence:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/automatic-statistics-csv-2026-11-02/runtime/retry/RESULTS.md`.
- [x] 5.6 Record changed files, hashes/deltas and probe output under
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/automatic-statistics-csv-2026-11-02`.

