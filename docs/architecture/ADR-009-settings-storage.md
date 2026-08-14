# ADR-009: Settings Storage — SharedPreferences Primary, DataStore for Specific Domains

## Status

Accepted

## Context

The app exposes ~100 user settings (theme, player style, gestures, shuffle, sort modes, network
features, scrobbling, cutoffs). The codebase (inherited from Booming Music) reads and writes
settings from many places: activities, fragments, the playback service, adapters, and Compose
screens. A few subsystems additionally need reactive, atomic preferences with higher write
frequency or structured state.

## Decision

- Use `SharedPreferences` as the primary settings store, accessed exclusively through the typed
  `object Preferences : KoinComponent` facade (`util/Preferences.kt`) with:
  - typed getters/setters, ~100 named key constants,
  - kotlinx-serialization for structured values (e.g. `libraryCategories`),
  - enums persisted by name or ordinal,
  - a change-listener registration API consumed by activities/fragments.
- Keep `SharedPreferences` observable from Compose via `observeKeyAsState` (produceState +
  `OnSharedPreferenceChangeListener`) where Compose screens need reactive values.
- Use Jetpack DataStore (Preferences) for state that changes frequently or is consumed
  reactively and must not block the main thread:
  - `equalizer` DataStore — all EQ/bass/tempo/volume/balance/ReplayGain/device-profile state
    (`EqualizerManager`),
  - `sleep_timer` DataStore — timer options and state (`SleepTimerViewModel`),
  - widget playback state is persisted through a custom typed DataStore via
    `GlanceStateDefinition` (`PlaybackStateDefinition`, ADR-030).
- Migrate progressively: a `TODO migrate to DataStore Preferences` exists for the queue-lock
  preference in `QueueScreen`; SharedPreferences remains the default for classic settings.

## Consequences

- One typed façade means preferences are self-documenting and refactorable.
- The two-store split is intentional: blocking XML reads for the hot equalizer path would
  hurt audio UX, and DataStore's `Flow` fits reactive audio state.
- SharedPreferences' lack of type safety is contained in `Preferences`; `observeKeyAsState`
  supports primitives only.
- Dual storage adds a small mental overhead when adding a new setting: choose the store
  consciously (perf/battery-sensitive or reactive → DataStore, classic UI setting →
  SharedPreferences).
