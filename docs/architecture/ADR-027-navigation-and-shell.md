# ADR-027: Navigation and Single-Activity Shell

## Status

Accepted

## Context

The app is a single-window music player: a persistent bottom mini-player, a swipable
now-playing panel, library category tabs and detail screens. Navigation must support SafeArgs
type safety, deep links (open audio files, search intents, pickers), Android Auto media
browsing, and legacy icon-pack compatibility inherited from the CyanogenMod lineage.

## Decision

- Use Jetpack Navigation 2.9.8 with the SafeArgs plugin, declared in two graphs:
  - `graph_main.xml`: library categories, detail screens (album/artist/genre/year/folder/
    playlist), search, player-adjacent destinations (lyrics, queue, equalizer, sleep timer),
  - `graph_settings.xml`: nested `NavHostFragment` inside `SettingsFragment` with per-category
    destinations (ADR-026 preference screens).
- Drive library tabs from the configurable `CategoryInfo` list (visible categories, max 5,
    reorderable, persisted as JSON — ADR-009) rather than a hardcoded tab set.
- Single-activity shell: `MainActivity` extends `AbsSlidingMusicPanelActivity` which owns:
  - the sliding mini-player panel (`BottomSheetBehavior`; `STATE_HIDDEN` clears the queue),
  - the bottom navigation bar/rail with hold-to-search,
  - the now-playing container and player-style swapping (ADR-028),
  - back-press routing through `IBackConsumer` before default `navigateUp`.
- Preserve launcher compatibility with the `activity-alias` `.activities.MainActivity`
  (ADR-001) carrying the `MAIN`/`MUSIC_PLAYER`/`AUDIO_PLAYER`, `MEDIA_PLAY_FROM_SEARCH`,
  `VIEW audio/*` (content/file/http), `PICK` and cursor-dir intents plus searchable
  configuration and static shortcuts.
- Expose the library to Android Auto/external controllers through `LibraryProvider`
  (hierarchical `MediaIDs` paths with `:` separators) backed by the same repositories.

## Consequences

- Type-safe arguments via SafeArgs eliminate bundle-key drift across ~20 destinations.
- The activity-alias keeps icon packs and Eleven-era launcher entries working at the cost of a
  small manifest indirection.
- The shell owns global UI concerns (panel, tabs, insets, back) in one place; every library
  screen inherits mini-player margins and event handling from `AbsMainActivityFragment`.
- Deep-linking to a song requires resolving MediaStore URIs at startup (`MainActivity`
  intent handling) — a documented startup path with fallbacks for missing files.
