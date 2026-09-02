## Context

Phase 1 (ADR-033) repainted tokens, shell and library. `queue`/`lyrics`/`eq` are already
Compose and mostly token-driven; `search` and `settings` are legacy XML; the player is a
deep legacy subsystem (7 style fragments + 7 controls fragments, custom views
`AlbumCoverViewPager`, `MusicSlider`, `MorphicIconButton`, `SquigglyProgress`, 10 ViewPager
transforms, tint `AnimatorSet` machinery in `AbsPlayerFragment`/`AbsPlayerControlsFragment`,
marquee logic and color-scheme animation).

## Goals / Non-Goals

**Goals:**
- Complete Aurora coverage: queue/lyrics/EQ as token-level restyles; search/settings
  restyled in place (Phase 1 precedent: no screen rewrites for legacy).
- Full Compose recompose of the player per rule 12, 1:1 feature/behavior preservation.
- The player remains governed by `PlayerViewModel`/`PlayerColorScheme` (ADR-028/029).

**Non-Goals:**
- Queue search / multi-queues / virtual keyboard / second accent (reference-only features).
- Per-row animations (rule 6); no new dependencies.
- Rewriting search/settings to Compose (later decision; restyle only here).

## Decisions

- **Quick wins first**: queue → lyrics → EQ → search → settings. Pure token/styling work
  with small diffs, independently verifiable, then the big player chunk last (matches the
  human decision; each block gates on build+lint+emulator).
- **Queue** is restyled via its existing Compose state model: current-row detection replaces
  the `primaryContainer` surface with an Aurora accent-hairline variant (`glassBorder`)
  built from tokens; rows get `Surface` shape copied from `item_list` (12dp) and glass fills
  `surfaceContainerLowest.copy(alpha=α)` so the aurora shows through. No logic changes; the
  `q-controls` anchor row (play/count/duration) is kept as-is if present, else the existing
  header/up-next anatomy stands.
- **Lyrics**: replace literal scrim color with token (`MaterialTheme.colorScheme.surface`)
  + alpha via the existing blur overlay composable; hairline the editor bars; glass circle
  edit FAB. Settings stored as data remain untouched (they are read in the same paths).
- **EQ**: cards (`TitledCard`, `SwitchCard`) gain glass/hairline variants by adding a
  preferred `CardDefaults` (or `Surface` swap) inside those shared composables — this also
  benefits any other screen using them (consistency). Waveform `BoxWithConstraints` amp
  color stays `primary`.
- **Search/settings restyle in place**, same technique as Phase 1: theme attrs +
  drawable/layer-list resources + minor layout edits. `fragment_search.xml` gets the
  pill-field/underline/chips via existing `SearchView` or TextInputLayout styling (decide
  inside the file; keep IDs intact). Settings rows: a color selector for text/icon, a
  circular icon-chip drawable and a hairline divider drawable applied through
  `preferenceTheme`/`itemLayout` attrs; `sec-head` via section preference layout theme.
- **Player recompose — host strategy:** `chooseFragmentForTheme()` in
  `AbsSlidingMusicPanelActivity` swaps to one `ComposePlayerFragment` once; inside, the
  style enum picks the composable. Keeps sheet wiring, panel height switching (Peek),
  `MediaControllerOwner` and ViewModels untouched. The legacy stack is NOT deleted in the
  same commit as the port — the new `compose_player` preference gates it so regressions are
  a settings toggle away until the final cleanup.
- **Shared blocks under `ui/screen/player/compose/`** packaged as public composables so
  other Compose screens can reuse: `GlassSurface` (surface copy alpha + hairline border +
  shape), `AuroraIconButton`, `AuroraPlayButton`, `AuroraSlider` (custom 4dp track +
  glow, since M3 Slider styling can't produce glowing thumb), `CoverPager`
  (`HorizontalPager` + per-transition `graphicsLayer`), `MarqueeText`, `EqBars` (reuse
  existing), `PlayerGestures` (pointerInput per configured `GestureType`).
- **Color / tint transitions:** replace per-view `AnimatorSet` tint targets with
  `animateColorAsState` per role; `PlayerColorScheme` (already in `core/model/player`)
  remains the model. The `PlayerTintTarget`/`iconButtonTintTarget` extensions and their
  legacy consumers die with the legacy cleanup; `tintTarget` extensions for library rows
  (new in Phase 1) stay.
- **Blur background:** Compose `AsyncImage` + `Modifier.blur()` (RenderEffect) behind the
  cover, gated as today by the blur setting. Fallback for API < 31 or blur-disabled: alpha
  dim instead (same as legacy behavior outside the Blur scheme). Battery note: blur only
  when the panel is expanded and the scheme requests it; no continuous repaint loops beyond
  standard compose.
- **Marquee:** `MarqueeText` animates only when the element is focused/long-selected ==
  legacy `setMarquee` trigger; infinite animation with `MutableInteractionSource`
  focus, cancels on dispose. No per-row animation application (rule 6).
- **Cleanup order:** all 7 styles green → delete legacy `styles/*` fragments+layouts,
  `AbsPlayerControlsFragment`, `AbsPlayerFragment` (verify no other consumers:
  `PlayerGesturesController`, `PlayerAnimator`, `cover/page`, `CoverLyricsFragment` helpers),
  old player drawables/themes only used there; grep before each delete; keep
  `MusicSlider`/`MorphicIconButton`/`SquigglyProgress` only if still referenced
  (EqMiniView uses them — verify).

## Risks / Trade-offs

- [Full player port is large] → staged: host+gated switch first, one style at a time,
  screenshot-compare per style against the prototype; legacy remains until the final block.
- [Gestures may feel different in Compose] → port the exact action mapping from
  `PlaylistGestureController` semantics in `PlayerGesturesController` (actions per tap zone
  come from prefs); verify on emulator per configured set; fallback: adjust thresholds.
- [Compose blur perf] → gate by scheme + panel visibility; audit with
  `compose-performance-audit` skill; legacy Blur scheme also used Coil blur, comparable.
- [Sheet interplay] → `AbsSlidingMusicPanelActivity` sheet callbacks (expand/collapse,
  Peek height) drive the Compose player via a SharedFlow-ish callback the fragment
  exposes; keep interface thin.
- [Settings restyle can break a11y contrast] → keep text colors token-based, verify
  contrast per theme mode on emulator; no new colors.

## Migration Plan

No data migration. `compose_player` is a new preference (default ON for fresh + migrated
users via `Preferences` default). Transition rollback = set pref OFF (legacy stack returns)
until the cleanup deletes the legacy stack; afterwards rollback = revert the delete commits.
`now_playing_screen` enum values unchanged.

## Open Questions

- Exact alpha values for glass fills on each screen — finalized against emulator
  screenshots, no contract change.
- Whether to keep the `compose_player` setting entry after cleanup (likely remove: the
  Compose player becomes the only player) — decide at cleanup, mirrors the handoff.
