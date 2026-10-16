## Context

The app is a single-module hybrid (legacy View XML shell + Compose screens; ADR-026). The shell
(`sliding_music_panel_layout.xml`) is pure XML; the palette is static M3 (blue) with an optional
Material You path (ADR-029); the player is untouched in this phase. See proposal.md - Why for the
motivation and specs/aurora-theme/spec.md for the behavioral contract.

## Goals / Non-Goals

**Goals:**
- One token set that repaints legacy themes, `SsilencceTheme`, widgets and `PlayerColorScheme.AppTheme`
  without structural changes.
- Aurora look by default for fresh installs while Material You stays fully functional.
- A battery-conscious single-canvas animated background, gated by preference + visibility +
  reduced-motion.
- All library/Home restyle done inside existing view adapters and layouts (no screen rewrites).

**Non-Goals:**
- Player, queue, lyrics, EQ, search, settings restyle (later phases).
- Introducing real window blur / backdrop-filter equivalents, or any new dependency.
- Per-row animations anywhere (AGENTS.md rule 6).

## Decisions

- **Static palette swap instead of new palette objects**: the existing `*Light`/`*Dark` vals in
  `ui/theme/Color.kt` and the `md_theme_*` resources get Aurora values; `SsilencceTheme`,
  `styles_parents.xml`, widget themes and `PlayerColorScheme.AppTheme` keep their wiring, so one
  change repaints everything. Alternatives (adding a parallel palette + pref switch) rejected:
  it would fork the theming paths and leave widgets/legacy inconsistent.
- **White as M3 primary in dark, near-black primary in light**: M3 components then inherit the
  accent automatically; hairlines map to `outlineVariant` (translucent white `#0F`-`#18` in dark,
  translucent black in light) and `outline` (slightly stronger).
- **Material You default flip is a one-line change** in `Preferences.isMaterialYouTheme` (and the
  XML switch `defaultValue`); `AppThemeToken.isValid` and `SsilencceTheme` then naturally keep
  Aurora. Legacy `AppTheme.createAppTheme` seeded-dynamic path is untouched.
- **Glass is simulated with alpha fills + hairline borders** (no `RenderEffect`): Android has no
  CSS backdrop-filter, true window blur only blurs windows behind the app, and blur is
  battery-heavy. Surface alphas (~`E6`–`F2`-range containers, translucent nav fills) plus 1dp
  white hairline borders reproduce the look at negligible cost.
- **BottomNavigationView restyle via styles + drawables**: a color-selector
  (`itemIconTint`/`itemTextColor`), an accent-tint layer-list item background (checked state) and
  a foreground caret drawable aren't straightforward on material `NavigationBarItem`; instead we
  keep standard positioning but overlay visual accents via `itemActiveIndicator*` attributes and
  custom icon/glow. If `NavigationBarView` limits get in the way, the minimal fallback is a
  `LayerDrawable` item background + selector tints only (no caret) — decide during implementation
  against emulator screenshots.
- **Miniplayer pill + 2px bar**: replace the wavy `CircularProgressIndicator` layout node with a
  thin horizontal `ProgressBar` (custom 2dp track + white fill + glow layer-list) inside a
  hairline-bordered translucent rounded container; when `squiggly_seek_bar` is on, keep the wavy
  ring as today (preference unchanged).
- **AuroraBackground is a Compose `Canvas`** (rule 12) rendered by a `ComposeView` at the shell
  root behind the `CoordinatorLayout`; the host surface becomes transparent so the layers show
  through. Static version = base gradient + radial white glow + vertical scrim (as the reference's
  `.aurora-static`/dark overlay); animated version adds the reference's serpentine S: a path
  whose points displace in X only by `A·sin(k·y + φ+t)` for two phase-offset π layers, drawing
  white strokes at low alpha — trivial per-frame cost, no 3D transforms.
- **Animation gating**: run only when `background_animation` is true AND animator duration scale
  > 0 AND the activity is STARTED/visible; `withFrameNanos` loop owned by a `DisposableEffect`
  that cancels on dispose. This keeps battery cost near zero when paused.
- **Row anatomy on existing layouts**: extend `item_list.xml` (used for songs) and
  `item_song*.xml` with `time` (duration) + `idx` nodes bound by `MediaEntryViewHolder`; new
  `EqMiniView` (three bars, glow) shown on the playing row only (bind per position; never
  animated continuously — uses a slow phase animator only while that row is current, cancelled on
  rebind). Play overlay on grids: reuse existing `play` MaterialButton, restyled to white
  circular with glow.
- **Detail headers**: keep `HeaderAdapter`/`item_detail_header.xml` (shared by album/artist/
  playlist details) and restyle in place (220dp cover + halo layer, circular 44dp shuffle/search,
  wavy white play from the existing `MorphicIconButton` restyled via tint/glow drawable) — avoids
  forking three detail screens.
- **Home quad + rails**: rebuild `abs_playlists.xml` (4 MaterialButtons → 4 glass cards in a 2×2
  `GridLayout`; same IDs bound by `HomeBinding`) and restyle `item_suggestion.xml` +
  `item_album_gradient.xml` + `item_artist.xml` toward the reference cards/labels.
- **Top Tracks**: `DetailListFragment` + `fragment_detail_list.xml` already is a collapsing
  header; restyle to centered title + Play/Shuffle pills (reuses existing `playAction`/
  `shuffleAction` slots).
- **README of verification**: screenshot-driven comparisons vs the reference (`ssilencce-auroras.html`
  + bundled images) using the emulator MCP.

## Risks / Trade-offs

- [Translucent shell reveals content edges] → vertical scrim overlay (reference `.aurora-dark-overlay`)
  keeps contrast; verify per theme mode with screenshots.
- [NavBarView doesn't render a top caret with glow natively] → layer-list item background with the
  caret as an inset shape; if the result looks wrong on the emulator, drop the caret and keep
  tint+pill (still meets the spec's "white caret with soft glow" by using the indicator).
- [Row highlight/glow on legacy TextView uses shadows] → acceptable; use `setShadowLayer` on the
  title only, cheap for a single rebind.
- [`material_you` default flip doesn't affect existing installs] → intended; fresh installs (and
  users who toggle) get Aurora; documented in ADR-033.
- [Perf: 2px progress + glow drawables repaint at 60fps] → tiny full-width bars; the glow is a
  pre-rendered shape drawable, no bitmap scaling loops.

## Migration Plan

No data migration. Preference defaults change for fresh installs only; existing users keep saved
values. Rollback = revert the `surfaceColor`/tokens commit group; the `background_animation`
preference makes the animated background re-switchable without code change.

## Open Questions

None resolvable now: minor tuning (exact hairline alphas, glow radii) will be finalized against
emulator screenshots without changing the behavioral contract.
