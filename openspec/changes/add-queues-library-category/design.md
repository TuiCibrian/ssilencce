# Design: Configurable Queues library category

## Context

`Preferences.libraryCategories` stores the visible/ordered categories. Its merge path walks
`CategoryInfo.Category.entries`, keeps existing entries in their stored order and appends every
available category that is not already present with `visible = false`. The first-run default marks
only the first five available categories visible (Home, Songs, Albums, Artists, Playlists).

Named queues already have a complete data path: `NamedQueueRepository` exposes
`summariesFlow()` and `activeNamedQueueIdFlow` is backed by preferences; `PlayerViewModel` exposes
`saveCurrentQueueAs`, `renameNamedQueue`, `deleteNamedQueue`, `switchToNamedQueue` and the
`NamedQueueMessage` event. `QueueScreen` already owns the advanced manager (duplicate chooser,
First/Previous/Next, remove-others, queue-local search) and the `nav_queue` dialog host.

Aurora has a fixed primary Queue destination and a library drawer derived from
`Preferences.libraryCategories`; its `nav_queue` destination is replaced by the full-screen
`AuroraQueueFragment`.

## Goals / Non-Goals

**Goals:**

- Make queues browsable from the non-Aurora library navigation as an opt-in category.
- Reuse the existing named-queue data and commands; no new storage layer.
- Keep `nav_queue` (the queue screen) unchanged and separate from the new browser.
- Preserve existing category ordering and the default five visible tabs.
- Keep the change buildable without a schema, dependency or permission change.

**Non-Goals:**

- Reimplementing the duplicate-name chooser, queue chain, remove-others or queue-local search in
  the browser; those remain in `QueueScreen`.
- Changing named-queue switching semantics or storage.
- Adding a Queue entry to the Aurora drawer.
- Adding wallpaper-based theming or other unrelated UI work.

## Decisions

### Category position and default visibility

`Queues` is appended after `Composers`. The existing merge appends it as invisible for upgrades,
and the default visibility rule (`index < MAX_VISIBLE_CATEGORIES`) leaves it hidden for fresh
installs because it is the last available entry. No migration is needed and stored category order
is preserved.

### One browser, existing queue screen

`nav_queues` is a new fragment destination hosting `QueuesFragment`. The playing-queue row and
each saved-queue row open the existing `nav_queue` dialog after the required action. The browser
does not duplicate `QueueScreen`; it is a library index for the current queue and saved queues.

### Reuse the ViewModel API

The browser observes `queueFlow`, `namedQueuesFlow` and `activeNamedQueueIdFlow`, and calls
`saveCurrentQueueAs`, `renameNamedQueue`, `deleteNamedQueue` and `switchToNamedQueue`. Messages
from `namedQueueMessage` are shown as toasts. This intentionally uses the raw save/rename path, so
duplicate and limit errors are reported by the existing ViewModel instead of adding another
duplicate-choice UI to this minimal browser.

### Aurora exclusion

`AuroraNavigation` filters `CategoryInfo.Category.Queues` from its library drawer. Aurora keeps
its fixed Queue destination and `AuroraQueueFragment`; the category remains available to the
non-Aurora navigation and to the existing preferences category configuration.

### Insets and layout

The fragment extends `AbsMainActivityFragment`, applies the shared window-inset helper and
bottom-navigation margin, and renders a status-bar-padded Compose list. This matches the other
top-level list screens without adding a new app bar or layout resource.

## Risks / Trade-offs

- **Duplicate names:** the browser uses the repository's existing keep-both/suffix policy
  (`QueueNamePolicy.uniqueName`); duplicate save/rename names are auto-suffixed and reported as
  saved/renamed (`named_queue_saved_as`/`named_queue_renamed_as`). The richer Replace/Rename
  chooser remains in `QueueScreen` for direct manager edits; blank, over-long and limit errors are
  toasts.
- **Switch timing:** switching a saved queue is asynchronous; the browser disables the saved rows
  while the switch runs and opens the queue dialog only on success. A failure keeps the current
  queue and shows the existing message.
- **Category placement:** the category is last, so users with five visible categories must
  deselect/reorder in Settings to see it. This follows the existing maximum-five policy.
- **Aurora reachability:** the category is intentionally hidden in Aurora;
  `AuroraStartDestinationPolicy` rejects `nav_queues` as an Aurora start/last-page destination, so
  a saved classic last page cannot resume there. Aurora keeps its fixed `nav_queue` host; classic
  keeps the Queues category.

## Verification

- Static source probes for category merge/default visibility, `nav_queues` routing, fragment API
  usage, Aurora exclusion, unchanged `nav_queue` dialog and the absence of DAO/schema/dependency
  additions.
- ktlint on changed Kotlin files and `openspec validate add-queues-library-category --strict`.
- Build and runtime checks remain pending until the AA-05/AA-06 source freeze and build lease are
  available.
