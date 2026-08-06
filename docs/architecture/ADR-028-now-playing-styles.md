# ADR-028: Now-Playing Styles, Cover Pager and Gestures

## Status

Accepted

## Context

The player screen is the app's signature surface. Users expect selectable layouts (default,
full-cover, gradient, plain, Material 3, expressive, peek), a swipeable cover pager with
transition effects, configurable gestures on the cover, and quick actions — all driven by the
same playback state and color scheme.

## Decision

- Model the seven player layouts in the `NowPlayingScreen` enum (Default, FullCover, Gradient,
  Plain, M3, Expressive, Peek), each mapping to a layout, button style
  (`NowPlayingButtonStyle`), supported color-scheme modes, transitions and feature flags
  (carousel, corner radius, cover lyrics).
- Implement each style as a pair of legacy fragments — root fragment extending
  `AbsPlayerFragment` (toolbar, gestures, color-scheme animation, dialogs) + controls fragment
  extending `AbsPlayerControlsFragment` (progress, transport, repeat/shuffle, slider) — plus a
  per-style `PlayerAnimator` subclass for entrance animations.
- Cover pager: `ViewPager` + `CoverPagerFragment` with:
  - `CustomFragmentStatePagerAdapter` (AOSP-derived, explicit `maxLifecycle` control,
    saved-state restore) instead of the deprecated framework adapter,
  - 9 `PageTransformer`s (`PlayerTransition`: Simple, Carousel, Depth, Cascading, Hinge,
    HorizontalFlip, VerticalFlip, Parallax, ZoomOut) — Carousel enabled per-style,
  - `AlbumCoverViewPager` gating swipe by the swipe-on-cover preference,
  - palette color extraction from the visible cover (`PaletteProcessor`, ADR-029) propagated
    to the player theme.
- Gestures: `PlayerGesturesController` maps tap / double-tap (left/right edge split at
  0.30/0.70 width) / long-press / fling to configurable `NowPlayingAction`s; cover actions
  (tap/double-tap/long-press) are per-gesture configurable with exclusivity enforcement.
- Render lyrics overlays on the cover when the style supports it (`CoverLyricsFragment`),
  with expand-to-full-lyrics navigation.
- Drive everything from `PlayerViewModel` flows (queue, current song, progress via
  `ProgressObserver` polling, color scheme), shared activity-scoped with the mini-player.

## Consequences

- Seven styles share one behavioral core; adding a style means implementing three small
  classes (fragment, controls, animator) plus a layout.
- The ViewPager-era components (custom adapter, transformers, gesture controller) are
  deliberately legacy-View and must remain stable until the player migrates to Compose.
- Gesture configurability gives users control but multiplies preference surface
  (ADR-009 keys).
- Palette-driven dynamic color makes the player feel per-album without storing per-song
  themes.
