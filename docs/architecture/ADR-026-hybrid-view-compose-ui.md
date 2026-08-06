# ADR-026: Hybrid UI — Legacy Views with Progressive Compose Adoption

## Status

Accepted

## Context

The codebase inherited a fully legacy View/XML UI (Fragments, ViewBinding, RecyclerView
adapters, PreferenceFragmentCompat screens, ~100 layouts, Material Components). The project
wants modern, testable, expressive UI while avoiding a risky big-bang rewrite; some new
features (lyrics, equalizer, queue) are naturally Compose-shaped.

## Decision

- Keep the legacy View layer as the running core: `AppCompatActivity`/`Fragment` hosts,
  ViewBinding, RecyclerView with specialized adapters (`AbsMultiSelectAdapter` + CAB
  multi-select, popup menus via `OnClickMenu`), `PreferenceFragmentCompat` with XML preference
  screens per category, and Material Components custom views (`MusicSlider`, `SquigglyProgress`,
  `MorphicIconButton`, `WidthFitSquareLayout`, `BaselineGridTextView`, ...).
- Adopt Jetpack Compose (Compose BOM 2026.06.01, Material3 1.5.0-alpha23 for the Expressive
  API set) for new and migrated screens, embedded through `ComposeView` hosts:
  - full screens: lyrics (`LyricsScreen`, editor, cover-lyrics overlay), equalizer
    (`EqualizerScreen`), sound settings sheet, sleep timer sheet, queue (`QueueScreen`), about,
    song detail bottom sheet, shuffle-mode bottom sheet, add-to-playlist dialog,
  - preference dialogs: `SingleSelectionDialog`, `SongClickActionPreferenceDialog`,
    `ClearQueueActionPreferenceDialog`, `ActionOnCoverPreferenceDialog`.
- Enforce by rule (AGENTS.md rule 12): *new UI additions must be Jetpack Compose*.
- Bridge state reactively: ViewModels are shared between legacy fragments and Compose screens
  (Koin activity-scoped); SharedPreferences becomes observable in Compose via
  `observeKeyAsState`; one-shot events use the `Event` wrapper + `ObserveAsEvent`.
- Keep a shared Compose component library (`ui/component/compose/`: dialogs, menus, sliders,
  lyrics views, preference items, decoration modifiers) so Compose screens stay consistent.

## Consequences

- Low-risk migration path: each screen can move to Compose independently while the legacy
  shell (ADR-027/028) remains stable.
- Two UI stacks coexist, so skills, theming (ADR-029) and state-bridging must be maintained in
  both; duplication risk is mitigated by the shared component library.
- Material3 alpha versions carry API churn risk; the version is pinned centrally in the
  version catalog.
- Legacy widgets (widgets, notification layouts, Android Auto templates) remain View-based by
  platform constraint — Compose adoption does not extend there.
