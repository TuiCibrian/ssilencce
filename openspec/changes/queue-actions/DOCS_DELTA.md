# Documentation delta — queue-actions

## docs/CODE_MAP.md

Add file entries:

- `playback/RepeatPolicy.kt` — Purpose: pure player-level repeat state machine. Responsibilities:
  map Media3 repeat modes plus the persisted load-next-and-pause flag to `Mode`
  (Off/All/One/LoadNextAndPause), compute the next state for the repeat button with the optional
  fourth state, map the state back to Media3 repeat modes and decide when an automatic item
  transition must load the next item paused. Inputs: Media3 repeat mode, active-policy flag and
  transition reason. Outputs: repeat mode transitions and a pause decision. Dependencies: Media3
  `Player`. Consumed by: `PlaybackService`, repeat controls (queue, Aurora, legacy/Expressive).
  Related files: `playback/PlaybackService.kt`, `util/Preferences.kt`.
- `data/local/repository/NamedQueueMaintenancePolicy.kt` — Purpose: pure named-queue maintenance
  rules. Responsibilities: append-merge media-id lists with an optional duplicate filter and pick
  the least recently used saved queue for cap eviction while protecting the active queue.
  Inputs: ordered media-id lists, saved queue entities, protected id. Outputs: merged list or
  eviction candidate. Consumed by: `NamedQueueRepository`. Related files:
  `data/local/repository/NamedQueueRepository.kt`.
- `ui/screen/queue/NamedQueueMergeDialog.kt` — Purpose: shared "Merge into…" target picker for
  saved queues. Responsibilities: list possible target queues, expose the skip-duplicates switch
  and dispatch the chosen target. Inputs: source summary, target summaries and callbacks.
  Outputs: merge callback with target id and duplicate preference. Consumed by: `QueueScreen`
  (queue manager dialog) and `QueuesFragment`. Related files:
  `data/local/repository/NamedQueueRepository.kt`.

Update file entries:

- `ui/screen/player/PlayerViewModel.kt` — add `repeatLoadNextPauseFlow`/`isRepeatLoadNextPauseActive`,
  `onRepeatLoadNextPauseChanged`, `pauseAfterSong`, `playAfterCurrentSong`,
  `playItemAfterCurrent`, `loadNextSongAndPause`, `mergeNamedQueues`; extend
  `saveCurrentQueueAs` with opt-in LRU eviction and eviction feedback.
- `playback/PlaybackService.kt` — add the player-level load-next-and-pause policy (pause at the
  start of each automatically loaded item, wrap to the first item paused after queue end), the
  four-state repeat cycle gated by the setting, and the repeat-policy custom event.
- `playback/Playback.kt` — add `EVENT_REPEAT_LOAD_NEXT_PAUSE_CHANGED` and
  `EXTRA_LOAD_NEXT_PAUSE_ACTIVE`.
- `data/local/repository/NamedQueueRepository.kt` — `create` returns
  `NamedQueueCreateResult` and supports LRU eviction at the cap; add `merge` returning
  `NamedQueueMergeResult`.
- `ui/screen/queue/QueueScreen.kt` — queue item menus expose pause-after-song, play-after-current
  and load-next-and-pause; repeat controls show the fourth state; manager dialog starts the merge
  flow.
- `ui/screen/queue/QueuesFragment.kt` — saved-queue rows start the merge flow.
- `ui/screen/MainActivity.kt` — observes stop-after and queue-action messages so feedback works
  from every screen, and forwards the repeat-policy event to the ViewModel.
- `ui/component/base/AbsPlayerFragment.kt` — handles pause-after-song and load-next-and-pause in
  the Now Playing menu.
- `ui/component/base/AbsPlayerControlsFragment.kt` — repeat icon/color reflects the fourth state.
- `ui/component/menu/MenuItemClickExt.kt` — single-song menus handle the three queue actions.
- `ui/screen/player/aurora/AuroraPlayerScreen.kt` — repeat state label/icon and the two new Now
  Playing overflow actions.
- `ui/screen/player/styles/expressivestyle/ExpressivePlayerFragment.kt` — repeat icon/color
  reflects the fourth state.
- `util/Preferences.kt` — add `repeatLoadNextAndPauseAvailable`, `repeatLoadNextAndPauseActive`
  and `autoDeleteOldestNamedQueue` with their keys; all default to off.

## docs/FEATURES.md

- In §1 `**Playback modes**`, replace the bullet with:
  `- **Playback modes** — Repeat (off / all / one, plus an optional fourth load-next-and-pause state enabled in Playback settings), shuffle (normal), play on startup (never / last position / first song), clear queue on completion. The player-level load-next-and-pause state is persisted separately from Media3 repeat mode; each automatically loaded item starts paused at 0 and the queue wraps to its first item paused at the end. Per-song menus, queue item menus and the Now Playing menu expose Pause after this song, Play after current song (moves an existing copy instead of duplicating it) and Load next song and pause.`
- In §7 `**Queue screen**`, after "Limits are 100 queues and 80 characters with an explicit error rather than auto-delete." replace that sentence with:
  `Limits are 100 queues and 80 characters with an explicit error, unless the opt-in Auto-delete oldest saved queue preference is on: then saving the 101st queue deletes the least recently used saved queue (the active saved queue is protected) and reports the removed name.`
  Then add after the duplicate-name sentence:
  `Saved queues offer Merge into…: the chosen target keeps its name, resume state and order, receives the source queue's songs appended, and can skip duplicate media ids; the source queue is kept and no new queue is created.`

## docs/MISSING_FEATURES.md

Remove:

- Part 1 §1 line: `- **Queue control actions** — "Load next song and pause", "Pause after this song", "Play after current song".`
- Part 1 §2 line: `- **Named-queue merge** - duplicate save/rename now offers Replace existing / Keep both / Rename; merging two saved queues remains deferred.`
- Part 1 §2 line: `- **Automatic oldest-queue deletion at the cap** - ssilencce enforces 100 named queues and rejects new saves with an explicit error; it does not delete a queue automatically.`
- Part 1 §3 line: `- **Repeat "load-next-and-pause"** mode.`

Reword in Part 2 §9 `Shuffle & repeat modes`:

- Replace the ssilencce line with: `- ssilencce: repeat/shuffle on/off plus an opt-in repeat load-next-and-pause state and unique smart shuffle modes (most played, favorites, forgotten, recently added, by-group, album/artist strategies).`
- Replace the trailing reference with: `- See Part 1 §3 (randomize, behaviors, shuffle queue).`

Reword in Appendix A `Sleep timer`:

- Replace "(ssilencce has countdown, exact HH:MM end time, fade-out, quit-after-end)" with
  "(ssilencce has countdown, exact HH:MM end time, fade-out, quit-after-end and a single-song pause-after action shared with the queue/Now Playing menus)".

## AGENTS.md

- Repository Code Map → `playback/` bullet: add `RepeatPolicy.kt` to the resilience/policy list,
  described as "pure four-state repeat policy (off/all/one/load-next-and-pause) with the
  pause-on-auto-transition rule".
- Repository Code Map → `data/local/repository/` bullet: add `NamedQueueMaintenancePolicy.kt`
  (append-merge with duplicate filter and least-recently-used eviction).
- Repository Code Map → `ui/screen/queue/` mention: include `NamedQueueMergeDialog.kt` and the
  merge action.
- Repository Code Map → `ui/screen/player/` bullet: mention the fourth repeat state and the three
  queue actions.

## docs/agents/domain.md

- ADR count is unchanged (no new ADR). No text change required.

## ADRs

No new ADR was required: the repeat state is a feature-level player policy (Media3 repeat mode
remains the source for its three native states), and the named-queue maintenance rules stay in the
existing repository layer without schema changes.

## Decisions

- **Now Playing menu scope:** "Play after current song" needs a selected song, so it is exposed in
  per-song menus and the queue item menu; the Now Playing menu exposes the two actions that are
  meaningful without another selection (Pause after this song, Load next song and pause). All
  three actions are available in the app.
- **"Load next song and pause" in per-song menus** is the global transport action (skip to the next
  queue item and pause at 0), not an action on the selected song.
- **"Pause after this song" outside the queue:** if the selected song is not queued at or after the
  current item, it is moved/inserted directly after the current item before arming stop-after, so
  the action is usable from library song menus. Queue rows arm their exact occurrence.
- **Queue end with the fourth repeat state:** the queue wraps to the first item paused at 0, which
  matches the repeat semantics while still loading a song before pausing. `Clear queue when
  playback finishes` still wins when enabled.
- **Least recently used:** `NamedQueueEntity.updatedAt` is the recency signal (it is refreshed when
  the saved queue is saved/renamed or its active playback state is persisted). The active saved
  queue is always protected from eviction.
- **Merge duplicate default:** the merge dialog defaults to skipping duplicate media ids; unchecking
  appends every source item.
- **No Room schema change:** merge rewrites only the target's existing item rows; eviction deletes
  existing rows. The DB version is untouched.
