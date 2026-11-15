# Queue actions, load-next-and-pause repeat, and named-queue maintenance

## Why

The player already supports a single "stop after this track" action from the queue, but the
same playback intents are not reachable from song menus or the Now Playing menu. Repeat has
only the three Media3 states, so users cannot ask the player to load each next song paused.
Named queues can be saved, renamed, switched and deleted, but cannot be merged, and reaching
the 100-queue limit only shows an error.

## What Changes

- Expose **Pause after this song**, **Play after current song** (queued next without creating a
  duplicate) and **Load next song and pause** (next item loaded paused at 0) from per-song menus,
  the queue item menu and the Now Playing menu.
- Add an optional fourth repeat state, **Load next song and pause**, implemented as a
  player-level policy (Media3 repeat mode stays OFF while it is active). It is persisted, appears
  in the repeat-button cycle only when the user enables it in Playback settings (default off), and
  is also available through the queue repeat menu label.
- Add **Merge into…** in the named-queue UI: append one saved queue's songs to another, with an
  optional duplicate-skipping mode. The source queue is kept and the 100-queue cap is unchanged.
- Add an opt-in preference (default off) that makes saving the 101st named queue delete the least
  recently used queue instead of failing with the limit error. The active saved queue is protected
  from eviction.
