# Shuffle Plus

## Why

Musicolet offers a richer shuffle model than ssilencce: a third `randomize` state that picks a
random next song each time instead of building a fixed shuffled order, configurable shuffle
behaviors, and queue actions that shuffle a multi-selection before playing or queuing it.
ssilencce currently has only binary shuffle and no multi-select shuffle actions.

## What Changes

- Add a persisted three-state shuffle model: `Off`, `Shuffle`, `Randomize`. The shuffle button
  cycles through the states enabled in settings; randomize advances to a random song on every
  transition without materializing a shuffled order.
- Add the playback preferences `Shuffle behavior` (`Shuffle all songs of each queue` /
  `Only shuffle songs below the current song`) plus the `Shuffle` and `Randomize` cycle
  switches.
- Add `Shuffle & play`, `Shuffle and play next` and `Shuffle and add to queue` to the
  multi-select action mode (`menu_media_selection.xml`), grouped under a `Shuffle and then…`
  submenu for the latter two.
- Keep the shuffled queue visible and reorderable: the queue screen already renders the effective
  play order and reorders the timeline, so this item needs no code change beyond documenting it.
