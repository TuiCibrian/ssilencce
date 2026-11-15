# Tasks: Configurable Queues library category

## 1. Category and navigation

- [x] 1.1 Add `CategoryInfo.Category.Queues` after Composers with the existing nav id, title and
  queue icon; verify the existing merge appends it invisible and preserves order.
- [x] 1.2 Add the `nav_queues` fragment destination and keep `nav_queue` as the unchanged dialog.
- [x] 1.3 Add `R.id.nav_queues` to the MainActivity top-level destination list so bottom
  navigation and last-page persistence behave like other library categories.
- [x] 1.4 Exclude `CategoryInfo.Category.Queues` from the Aurora drawer.

## 2. Queues browser

- [x] 2.1 Add the main English `queues_label` and `queues_saved_label` strings.
- [x] 2.2 Add `QueuesFragment` with the playing-queue row, saved-queue list, active marker,
  save/rename/delete dialogs and switch action.
- [x] 2.3 Reuse `NamedQueueSummary`, `queueFlow`, `namedQueuesFlow`,
  `activeNamedQueueIdFlow`, `namedQueueMessage` and the existing named-queue commands; open
  `nav_queue` from the browser.
- [x] 2.4 Apply the shared status-bar, window-inset and bottom-navigation margin treatment.

## 3. Documentation and OpenSpec

- [x] 3.1 Update `docs/FEATURES.md`, remove the resolved gap from
  `docs/MISSING_FEATURES.md`, and add the fragment/category entries to `docs/CODE_MAP.md`.
- [x] 3.2 Update the AGENTS.md repository map line for `queue/`.
- [x] 3.3 Create this OpenSpec change with proposal, design, spec and tasks.

## 4. Verification

- [x] 4.1 Run the static source probe covering category append/default visibility, nav routing,
  fragment API usage, Aurora drawer/start exclusion, unchanged `nav_queue`,
  duplicate-name/switch wording and no DAO/schema/dependency additions: **62/62 PASS**
  (`logs/source-probe.log`).
- [x] 4.2 Run ktlint on changed Kotlin files: **PASS**, exit 0 (`logs/ktlint-check.log`).
- [x] 4.3 Run `openspec validate add-queues-library-category --strict`: **valid**
  (`logs/openspec-validate.log`).
- [x] 4.4 Build the combined candidate: **BUILD SUCCESSFUL**, **3994F4F4** `3994f4f4ccc1e79f61f665a118e9de5116805ae51882a15c1dc0b70e3c25b327` (53,478,676 bytes, mtime `2026-11-07T05:53:24.9030222Z`), build 2m 37s, 0 lint errors / 1,443 warnings. Device/visual verification remains pending; no emulator was used.
- [x] 4.5 Review fix: exclude `nav_queues` from Aurora start/last-page restore, keep the
  Aurora fixed Queue destination and classic Queues, add `AuroraStartDestinationPolicy` probe
  coverage and documentation.
- [x] 4.6 Run the pure Aurora start/last-page policy probe: **7/7 PASS**
  (`probes/policy/run.log`).
- [x] 4.7 P3 handback: duplicate save/rename wording now describes the repository keep-both/suffix
  policy (`QueueNamePolicy.uniqueName`) and the final-name messages; the switch waits for success,
  disables rows while switching and opens `nav_queue` only on success.
- [ ] 4.8 Build/freeze the post-review source. The 3994F4F4 APK predates the Aurora
  start/last-page and switch/duplicate corrections; no new candidate is claimed until a fresh
  freeze/build.
