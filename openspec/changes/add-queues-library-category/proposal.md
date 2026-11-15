# Proposal: Configurable Queues library category

## Why

Named queues can be saved, renamed, deleted and switched from the queue screen, but outside Aurora
there is no library-level place to browse them. Musicolet exposes queues as a library category, and
ssilencce already has the category infrastructure (`CategoryInfo`, `Preferences.libraryCategories`)
and the complete named-queue ViewModel API. The missing piece is a top-level browser that reuses
those existing pieces without adding persistence or new queue semantics.

## What Changes

- Add `CategoryInfo.Category.Queues` after `Composers`, so existing installs receive it invisible
  through the current category merge and the default five-tab order is preserved.
- Add a `nav_queues` fragment destination and a Compose `QueuesFragment` with:
  - a playing-queue row with its song count that opens the existing `nav_queue` dialog;
  - the saved named queues with their song counts and an active marker;
  - save, rename, delete and switch actions through the existing `PlayerViewModel` API.
- Treat `nav_queues` as a top-level destination in `MainActivity` (bottom navigation visible and
  last-page persistence through the existing category check).
- Exclude `Queues` from the Aurora drawer filter because Aurora already has a fixed Queue
  destination, and reject `nav_queues` as an Aurora start/last-page restore target through
  `AuroraStartDestinationPolicy`.
- Add main English strings, update the feature/gap/code-map docs and record this OpenSpec change.

## Impact

- Modified: `CategoryInfo`, `MainActivity`, `AuroraNavigation`, `graph_main.xml`, `strings.xml`,
  `FEATURES.md`, `MISSING_FEATURES.md`, `CODE_MAP.md`, `AGENTS.md`.
- New: `QueuesFragment.kt`; no new DAO, repository, Room entity, migration, permission or
  dependency.
- Unchanged: the `nav_queue` dialog, `QueueScreen`, named-queue storage and switching semantics,
  and the Aurora queue host.
