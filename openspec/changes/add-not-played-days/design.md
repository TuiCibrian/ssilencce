# Design: Arbitrary Not Recently Played threshold

## Context

`RealSmartRepository.notRecentlyPlayedSongs()` starts from the full song list, removes every song
that appears in playback history, then adds back songs whose history rows are older than
`Preferences.getHistoryCutoff(context).interval`. That preset (default `this_month`) is the only
threshold, and `ContentType.NotRecentlyPlayed` is rendered by the generic `DetailListFragment`
smart list with no threshold control.

The existing history interval UI remains the source of the presets (`today`, `yesterday`,
`this_week`, `this_month`, `past_three_months`, `this_year`) and of the default behavior.

## Goals / Non-Goals

**Goals:**

- Allow an arbitrary N-day Not Recently Played window.
- Preserve the existing history interval behavior when no custom value is stored.
- Persist the choice in existing SharedPreferences with no schema change.
- Put the control in the existing smart-list Compose dialog surface.
- Keep the Home suggestion and the smart list consistent.

**Non-Goals:**

- Changing the history interval ListPreference or its presets.
- A new settings-screen preference or a Room migration.
- Android Auto/global-search threshold UI.
- Changing the `HistoryDao` history-limit semantics.

## Decisions

### Default sentinel

`NotPlayedDaysPolicy.USE_HISTORY_INTERVAL = 0` is the stored default. `Preferences.getNotPlayedCutoff`
returns `getHistoryCutoff(context)` for `0`, so existing installs and the unset preference keep the
current behavior. A positive value produces a `Cutoff` whose description is an English plural
("N days") and whose interval is `now - N * 24h`.

### Validation and arithmetic

`NotPlayedDaysPolicy` owns the pure rules: `normalizeDays` accepts `0` or clamps positive values to
1–3650, `parseDays` accepts only 1–3650, and `cutoffMillis`/`effectiveCutoffMillis` keep the
date arithmetic out of the fragment and repository. The dialog keeps the raw typed text; validation
rejects blank, non-numeric, zero and values above 3650 without truncating or clamping the field, and
Save stays disabled. The UI uses the same policy for validation.

### Repository

`RealSmartRepository` keeps the existing history-query shape and only changes the cutoff source to
`Preferences.getNotPlayedCutoff(context).interval`. Never-played songs and old-history songs keep
their existing semantics.

### Smart-list UI

`DetailListFragment` shows a menu action only for `ContentType.NotRecentlyPlayed`. The Compose
`NotPlayedDaysDialogFragment` offers:

- Use history interval (the current preset description), or
- Custom days, with a numeric field and 1–3650 validation.

The result is persisted, then `LibraryViewModel.loadNotRecentlyPlayedSongs()` reloads the same
LiveData. The list subtitle displays the effective threshold. The result listener is registered in
`onCreate`, matching the existing period-dialog pattern.

## Risks / Trade-offs

- Custom days use fixed 24-hour arithmetic; this is intentionally simpler than calendar-boundary
  presets and does not change the preset path.
- `HistoryDao` still limits the history IDs considered to 100; the custom window narrows or widens
  the cutoff but does not change that existing query limit.
- The Home suggestion refreshes through its existing reload path; it has no inline dialog.
