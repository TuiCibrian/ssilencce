# ADR-034: Compose now-playing player recompose

## Status

Superseded by [ADR-035](ADR-035-restore-pre-aurora-ui.md) on 2026-10-03.

## Context

Phase 1 (ADR-033) repainted the Aurora tokens, shell chrome and library surfaces. The
now-playing player remained on the legacy View/XML stack: 7 style fragments + 7 controls
fragments under `ui/screen/player/styles/*`, a `CoverPagerFragment` ViewPager with 10
transforms, custom views (`AlbumCoverViewPager`, `MusicSlider`, `MorphicIconButton`,
`SquigglyProgress`), gesture/marquee logic in `AbsPlayerFragment` and color transitions via
per-view `AnimatorSet` tint targets (`PlayerTintTarget`, `iconButtonTintTarget`,
`tintTarget`). The app's rule 12 requires new UI changes to favor Jetpack Compose, the app
is single-module hybrid (ADR-026), and the color-scheme model already lives in
`core/model/player/PlayerColorScheme` (ADR-028/029). Maintaining two player stacks (legacy
styles ever-restyled, plus any new Compose surfaces) is a dead end; the decision is to
recompose the player in full and retire the legacy machinery once all 7 styles are green.

## Decision

- **Full Compose recompose of the player**: one `ComposePlayerFragment` host replaces
  `chooseFragmentForTheme()` content; the style is selected by the existing
  `now_playing_screen` enum and rendered by dedicated composables in a new
  `ui/screen/player/compose/` package. All 7 styles (Default, FullCover, Gradient, Plain,
  M3, Expressive, Peek) are ported 1:1 — same settings, same behaviors, same feature set.
- **State and color sources unchanged**: `PlayerViewModel`, `MediaControllerOwner` and
  `PlayerColorScheme` (AppTheme / Simple Color / Vibrant / Material You) remain the source
  of truth; `PlayerColorScheme.isValid` unchanged; only the view layer is replaced.
- **Shared component library (Compose)**: `GlassSurface` (surface alpha + hairline border),
  `AuroraIconButton`/`AuroraPlayButton` (white accent, glow), `AuroraSlider` (4dp track +
  white glowing fill/thumb, custom — M3 Slider cannot express the glow), `CoverPager`
  (`HorizontalPager` + `graphicsLayer` per `PlayerTransition`), `MarqueeText` (infinite
  marquee gated by focus, no per-row animation), `PlayerGestures` (pointerInput mapping of
  the existing gesture preferences), blur background via `Modifier.blur()` gated by scheme +
  visibility. `EqBars`/`AnimatedEqBars` already exist and is reused.
- **Transition via `compose_player` preference (default ON)**: during the port the legacy
  stack stays available so any regression is a settings toggle away; the legacy stack,
  tint animators, transforms and dormant player drawables are deleted after the final style
  is verified (grep-confirmed no other consumers; `MusicSlider`/`MorphicIconButton`/
  `SquigglyProgress` are kept if still referenced — mini player, EqMiniView).
- **No feature additions**: the recompose maps existing functionality only (actions,
  favorites, queue/lyrics routing, up-next, sleep timer, Peek sheet height, cover
  transitions, marquee, gestures, blur modes). Reference-only player chrome is not added.

## Consequences

- The player becomes the flagship Compose surface and finally matches the Aurora reference
  (`.player--now`: halo cover, 7-icon row, 4dp glowing slider, 62dp white play).
- The legacy player code (≈14 fragments, cover layouts, transforms, tint machinery) is
  removed after verification — a substantial one-time deletion that shrinks the player
  stack to one host + composables.
- During the transition, two player stacks coexist behind `compose_player`; dual-stack is
  temporary and explicitly temporary (removed at cleanup).
- Compose blur (`RenderEffect`) replaces Coil-based blur: battery/perf risk is mitigated by
  gating (blur only when expanded panel + Blur scheme) and audited with the
  `compose-performance-audit` skill; fallback = alpha dim outside the Blur scheme.
- Gestures may feel subtly different on Compose: thresholds are ported from legacy; verify
  per configured gesture set on the emulator.

## Addendum (after cleanup)

The initial decision has been fully implemented and the legacy stack removed (all 7 styles,
`CoverPagerFragment` + `CustomFragmentStatePagerAdapter` + `AlbumCoverViewPager`, 10
transforms, `AbsPlayerFragment`/`AbsPlayerControlsFragment`, `PlayerAnimator`,
`PlayerGesturesController`, `PlayerTintTarget`, `MusicSlider`, `MorphicIconButton`,
`SquigglyProgress`, player/cover layouts, `preference_dialog_now_playing_screen*` layouts).
Notable follow-ups beyond the letter of the decision:

- The `compose_player` preference was **removed** (not just defaulted): with the legacy
  stack gone it had no function. `chooseFragmentForTheme()` always hosts
  `ComposePlayerFragment`, which dispatches on `Preferences.nowPlayingScreen`.
- No `GlassSurface` shared composable was added; player surfaces use
  `NowPlayingBackground`/scrims + existing `AuroraTokens` instead.
- `PlayerTransition` lost its `transformerFactory` (ViewPager machinery); `CoverPager`
  maps the enum directly to `graphicsLayer` values (AD-28/29-compatible).
- `SkipButtonTouchHandler` was extracted from `AbsPlayerControlsFragment` into its own file
  (mini player still uses it); `PlayerNavigation.kt` holds the `goToAlbum`/`goToArtist`/
  `goToGenre`/`goToDestination` helpers that lived in `AbsPlayerFragment`.
- FullCover/Gradient keep Coil-based blur via `NowPlayingBackground` gated by the Blur
  scheme, matching the legacy visual.
