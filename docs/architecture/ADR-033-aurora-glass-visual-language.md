# ADR-033: Aurora Glass visual language

## Status

Superseded by [ADR-035](ADR-035-restore-pre-aurora-ui.md) on 2026-10-03.

## Context

The app must adopt the visual language of the "Aurora Ondulante (glass · white accent)" design
reference (Open Design prototype) without adding features that do not exist in ssilencce and
without changing the existing functional feature set. The reference is dark-only by design,
while ssilencce supports four theme modes (Light / Dark / Black AMOLED / Follow System) plus an
optional Material You dynamic color mode (ADR-029) that currently defaults to on for Android
12+. The reference uses CSS backdrop-filter glass, which has no direct Android equivalent; and
it relies on per-page DOM animations that must not be replicated per-row (AGENTS.md rule 6).

## Decision

- **Design language:** Aurora Glass — near-black chroma-0 backgrounds with a soft white aurora
  glow, glass surfaces (translucent fill + 1px hairline border), and a single white accent
  (`#FFFFFF` in dark modes, near-black inverted accent in Light) used at most sparingly
  (≤2 strong hits per screen). Neutrals dominate; hairline borders replace heavy outlines.
- **Palette in all 4 modes:** the static Material 3 palettes in `ui/theme/Color.kt` and
  `res/values/colors.xml` (`md_theme_*`) become the Aurora palettes (dark/Light); Black AMOLED
  keeps its existing surface-container black overrides plus white hairlines; Follow System
  resolves to Light or Dark Aurora automatically. Role names are preserved so legacy themes
  (`styles_parents.xml`), `SsilencceTheme`, `PlayerColorScheme.AppTheme` and the Glance widgets
  all repaint from the same tokens without structural changes.
- **Material You becomes opt-in:** the `material_you` preference default flips from on
  (Android 12+) to off, so Aurora is the default visual on all devices. Users can still enable
  system wallpaper dynamic colors; `AppThemeToken` invalidation and `SsilencceTheme` behavior
  are unchanged.
- **Background animation:** a new `AuroraBackground` component at the shell root renders the
  static layered aurora (dark base, radial white glow, vertical gradient scrim) and, when
  enabled, an animated white serpentine S (two layers phase-offset π, 2D X undulation only —
  no 3D flips). New `background_animation` preference defaults to ON; it is a single
  view/Canvas (never per-row in legacy lists) that pauses when not visible and falls back to
  the static layer set when reduced-motion is requested (animator duration scale = 0) or the
  preference is off.
- **Shell & library restyle:** BottomNavigationView → translucent glass with white active
  item (accent-tint pill + 3px white caret with soft glow); miniplayer → glass pill with a
  2px white glow progress bar (squiggly seek bar remains a preference option); toolbars →
  title-fade dynamic headers where CollapsingToolbarLayout already exists; song rows add
  index / equalizer-mini (current-row only) / duration anatomy with white active highlight;
  grid covers get a white halo + circular white play overlay; album/artist detail headers get
  a 220dp halo cover, circular shuffle/search and a white wavy play; Home tiles become a 2×2
  glass quad; suggestion floors get uppercase `sec-head` labels and railed cards.
- **Scope boundary:** this language is visual only. No reference-only features are introduced
  (drawer, multi-queues, virtual keyboard, second accent hue); no new libraries are added;
  the player remains governed by ADR-028/ADR-029 (MaterialYou/SimpleColor/Vibrant/Blur modes
  untouched), restyled in later phases.

## Consequences

- The Aurora look is the default on every device; users on existing installs keep their saved
  `material_you` choice until they change it.
- Legacy View and Compose surfaces both repaint from one token set, keeping the two UI stacks
  consistent (ADR-026).
- Glass is simulated (alpha fills + hairlines) since Android has no equivalent of CSS
  backdrop-filter; real window blur is not used because it would only blur windows behind the
  app and would be battery-unfriendly.
- The serpentine animation is battery-conscious (single canvas, gated by visibility and the
  reduced-motion fallback); correctness of contrast under translucent layers is a regression
  risk and is verified per screenshot (emulator MCP) across all four theme modes.
