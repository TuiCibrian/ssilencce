# Docs delta — shuffle-plus

## docs/FEATURES.md

### Section 2 — Playback (replace the "Playback modes" bullet)

- **Playback modes** - Repeat, three-state shuffle (off / shuffle / randomize) with a configurable
  button cycle, play on startup (never / last position / first song), clear queue on completion.
  Randomize selects a random next song on every transition instead of materializing a shuffled
  order; it persists across restarts and has no fixed play order, so the queue screen keeps showing
  timeline order while randomize is active. The shuffle button cycles only through the states
  enabled in Settings > Playback > Shuffle mode (standard shuffle and randomize switches); if both
  are disabled the cycle keeps standard shuffle reachable. Standard shuffle can be configured to
  shuffle all songs of the queue or only the songs below the current one.

### Section 2 — Playback (add after the "Queue behavior" bullet)

- **Shuffle actions** - Long-press multi-select in song, album, artist, year, folder and search
  lists offers Shuffle play plus a Shuffle and then… submenu with Shuffle and play next and Shuffle
  and add to queue; the selected songs are randomized before being played, queued next or appended.
  The queue overflow action Reshuffle queue remains available only for the fixed shuffle order and
  reports that randomize has no order to rebuild.

## docs/MISSING_FEATURES.md

### Part 1, section 3 — remove these bullets

- **Randomize mode** (third shuffle state beyond on/off).
- **Shuffle behaviors** - "only shuffle below the current song", "shuffle all songs of each queue".
- **"Shuffle and then."** and **"shuffle and play"** from multi-select.

Keep **Shuffle queue visible & reorderable** out of the missing list as well (it was already
implemented before this change; verify with the queue screen in shuffle mode, which renders the
effective play order and supports drag reordering).

### Part 2, section 9 — replace the comparison bullet

- ssilencce now matches on/off/randomize, per-queue and below-current shuffle behaviors, a visible
  and reorderable shuffled queue, multi-select shuffle actions and repeat load-next-and-pause;
  the remaining difference is the broader smart-shuffle strategy set ssilencce already has.

## docs/CODE_MAP.md

Add entries:

- `core/model/shuffle/ShuffleModeState.kt` — Purpose: persisted three-state shuffle model (off /
  shuffle / randomize), string values used by the playback-state preferences and IPC. No
  dependencies beyond the enum itself. Consumed by `PlaybackService`, `PersistentStorage`,
  `PlayerViewModel`, player and queue UI.
- `core/model/shuffle/ShuffleBehavior.kt` — Purpose: shuffle order construction behavior
  (`AllSongs` / `BelowCurrent`), mapped from the `shuffle_behavior` preference. Consumed by
  `Preferences`, `PlaybackService` and `PersistentStorage`.
- `playback/RandomizeShuffleOrder.kt` — Purpose: dynamic `ShuffleOrder` that picks a random
  destination per navigation instead of a fixed permutation; caches the pick per source index and
  supports the forward-only "below current" behavior. Inputs: timeline length, below-current flag,
  random source. Outputs: next/previous/first/last indices and clones for insert/remove/move/set.
  Dependencies: media3 `ShuffleOrder`, `kotlin.random.Random`. Consumed by `PlaybackService` and
  `PersistentStorage`.
- `playback/ShuffleCyclePolicy.kt` — Purpose: pure shuffle-button cycle over the enabled states.
  Consumed by `PlaybackService`.
- `playback/ShuffleStateHolder.kt` — Purpose: process-wide, service-written shuffle state so UI
  consumers can distinguish randomize from a fixed order without an IPC round trip. Consumed by
  `PlaybackService` and `PlayerViewModel`.

Update entries:

- `playback/ImprovedShuffleOrder.kt` — add the optional `keepPrefix` mode (ordered prefix, shuffled
  tail) used by the "only shuffle songs below the current song" behavior; clones preserve it.
- `playback/PersistentStorage.kt` — persists the tri-state shuffle (`shuffle_state`, legacy
  `shuffle_mode` fallback) and restores a `RandomizeShuffleOrder` when randomize was active.
- `playback/PlaybackService.kt` — owns the shuffle state machine (`setShuffleState`,
  `syncShuffleState`, state broadcast/get command), the randomize order and the cycle-aware toggle.
- `playback/QueueReshufflePolicy.kt` — reshuffle is unavailable while randomize is active.
- `ui/screen/player/PlayerViewModel.kt` — exposes `shuffleStateFlow`, uses timeline order for the
  queue in randomize mode, reports the real random next song and maps reshuffle errors.
- `ui/component/base/AbsPlayerControlsFragment.kt` — shuffle control shows the randomize icon for
  the randomize state.
- `ui/screen/queue/QueueScreen.kt` — state-aware shuffle icon/labels and reshuffle availability;
  queue order and reordering unchanged.
- `ui/component/menu/MenuItemClickExt.kt` — handles Shuffle and play next / Shuffle and add to
  queue.
- `util/Preferences.kt` — new keys and typed accessors (`shuffle_behavior`,
  `shuffle_standard_enabled`, `shuffle_randomize_enabled`).
- `res/menu/menu_media_selection.xml` — Shuffle play and Shuffle and then… CAB entries.

## AGENTS.md — Repository Code Map

- Under `core/model/`, mention `shuffle/ShuffleModeState` and `ShuffleBehavior` (three-state
  shuffle model and order behavior).
- Under `playback/`, add `RandomizeShuffleOrder` (dynamic random next), `ShuffleCyclePolicy` and
  `ShuffleStateHolder`.

## docs/agents/domain.md

- Change "51 ADRs, ADR-001 through ADR-051" to "52 ADRs, ADR-001 through ADR-052".
- Append to the ADR summary: "ADR-052 adds the persisted three-state shuffle model (off / shuffle /
  randomize), the configurable button cycle and the prefix-preserving/randomize order policies."

## Proposed ADR-052

File: `docs/architecture/ADR-052-three-state-shuffle-and-randomize.md`

Status: Accepted

Context: Media3 exposes only a boolean shuffle mode plus a `ShuffleOrder`. The app needs Musicolet's
third randomize mode, a persisted state and configurable shuffle order behavior without changing
the queue identity or the persisted queue format.

Decision:
- Model shuffle as `ShuffleModeState` (Off / Shuffle / Randomize). The player keeps
  `shuffleModeEnabled` as the Media3 projection; the app state is derived from the live order type
  and synchronized to the UI through a same-process holder plus a custom event/query command.
- Implement Randomize with a dynamic `ShuffleOrder` that picks a random next index per navigation
  and caches the pick per source index. It never materializes a permutation, so there is no shuffled
  queue order to persist or display; the queue screen remains in timeline order and stays
  reorderable.
- Persist the state in the playback-state preferences (`shuffle_state`) with the legacy
  `shuffle_mode` boolean as a migration fallback. Named queues keep their existing boolean shuffle
  flag and therefore restore standard shuffle, not randomize.
- Build standard shuffle orders with an optional ordered prefix to support "only shuffle songs
  below the current song". The default remains "shuffle all songs of each queue".
- The shuffle button cycles through the states enabled in Settings. Randomize is a bonus state; its
  order never ends with repeat off unless the below-current behavior is selected, because a random
  walk has no fixed last item.

Consequences: Randomize cannot be represented in the saved fixed shuffle order, and resuming a
named queue or a session saved during randomize falls back to standard shuffle. The randomize order
does not guarantee every song is played before a repeat. The queue screen shows the underlying
timeline order in randomize mode.

## Decisions

1. The two named behaviors ("Shuffle all songs of each queue" and "Only shuffle songs below the
   current song") are mutually exclusive options of one `shuffle_behavior` list preference, keeping
   the existing behavior (`all_songs`) as the default. Two independent switches would allow
   contradictory combinations.
2. Randomize is enabled by default so the third state is reachable from the shuffle button; the
   `shuffle_randomize_enabled` and `shuffle_standard_enabled` switches implement "cycles only
   through enabled states". If both switches are off, standard shuffle stays reachable so the
   button cannot become a no-op. The shuffle state itself still starts Off, so default playback
   behavior is unchanged.
3. With the all-songs behavior, randomize has no natural end and keeps picking a next song with
   repeat off; with the below-current behavior it walks forward and ends at the last timeline item.
   This follows from having no fixed order.
4. "Shuffled queue visible & reorderable" was already implemented: the queue list is built from the
   effective shuffle order and drag moves are mapped back to timeline indices. No code change was
   needed; the only new caveat is that randomize shows timeline order because it has no play order.
5. Named queues store a boolean shuffle flag only (no Room schema change was allowed by the brief),
   so a named queue saved during randomize restores standard shuffle.

