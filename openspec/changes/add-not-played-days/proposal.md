# Proposal: Arbitrary Not Recently Played threshold

## Why

Not Recently Played currently uses the history interval preset as its only cutoff. Users cannot
ask for an arbitrary window such as "not played in the last 90 days", and the smart list exposes no
control for changing the threshold.

## What Changes

- Add a persisted `not_played_days` preference. `0` (the default) keeps the existing history
  interval behavior exactly; a positive value is a custom N-day window.
- Resolve the effective cutoff through a pure `NotPlayedDaysPolicy`, clamped to 1–3650 days.
- Use the effective cutoff in `RealSmartRepository.notRecentlyPlayedSongs()`, so both the Home
  suggestion and the Not Recently Played smart list follow it.
- Add a Compose dialog to the existing smart-list menu: use the current history interval or enter a
  custom day count. Applying it persists the value and reloads the list; the subtitle shows the
  effective threshold.
- Add main English strings/plurals. No Room schema, dependency or version change.
- Out of scope: changing the history playlist cutoff semantics, Android Auto/global-search filter
  UI, and any new settings-screen preference.

## Impact

- New: `NotPlayedDaysPolicy`, `NotPlayedDaysDialogFragment`, OpenSpec change.
- Modified: preferences, smart repository, library view-model, detail smart list, menu, strings,
  docs.
- Existing history interval presets and defaults remain the fallback.
