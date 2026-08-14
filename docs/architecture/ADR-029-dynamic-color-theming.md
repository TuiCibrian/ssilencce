# ADR-029: Dynamic Color and Theming (Material You, Palette, Player Schemes)

## Status

Accepted

## Context

The app must support static light/dark/black themes, system-follow mode, Material You dynamic
colors on Android 12+, and a player screen whose colors derive from the current album art.
Android's `DynamicColors` only covers system wallpaper extraction; per-album theming requires
cover-based palette extraction plus a custom tonal scheme generator that works on all API
levels (minSdk 26).

## Decision

- Theme model: `AppTheme.Mode` Light/Dark/Black/FollowSystem, resolved from preferences via
  `ContextExt.createAppTheme`; black theme uses `surfaceContainerBlack` overrides; system
  contrast (API 34) is respected where available.
- Static Material 3 palettes in `ui/theme/Color.kt` (light/dark pairs) as the default when
  dynamic color is off; Material You preference defaults to on for Android 12+ and is applied
  via `DynamicColors.applyToActivitiesIfAvailable` plus `DynamicColorsOptions` with a seed
  color.
- Generate custom tonal schemes with the `m3color` library (Kyant0, `Hct`/`Scheme*`) in
  `ui/theme/Monet.kt`: PaletteStyles (TonalSpot, Neutral, Vibrant, Expressive, Rainbow,
  FruitSalad, Monochrome, Fidelity, Content) used for both app schemes and widget themes
  (`PaletteStyle.Fidelity`, contrast 0.75).
- Extract cover colors with `PaletteProcessor`: m3color `QuantizerCelebi` + `Score` for the
  vibrant color plus androidx Palette region sampling for background/foreground, white/black
  filters, and 4.5:1 contrast guarantees for text colors; runs on `Dispatchers.Default`.
- Player theming: `PlayerColorScheme` with 5 modes (AppTheme, SimpleColor, VibrantColor,
  MaterialYou, Blur) driving `PlayerTheme`; cover-derived colors flow from the cover pager
  (ADR-028) and re-tint targets via `PlayerTintTarget`.
- Typography: bundle Google Sans Flex (7 weights) as the default family; allow importing
  custom TTF/OTF fonts (magic-byte validated) via settings.
- Apply theming to both stacks: `AbsThemeActivity` (legacy) and `SsilencceTheme`
  (Compose), and to Glance widgets via `WidgetExtension`/`WidgetTheme` built from the same
  dynamic schemes.

## Consequences

- Consistent M3 styling across legacy views, Compose screens and widgets from one color model.
- Per-album player colors work on all API levels because generation is local (m3color), not
  dependent on wallpaper APIs.
- Two palette sources (Celebi for vibrant, androidx Palette for regions) keep extraction
  quality high at the cost of two code paths in `PaletteProcessor`.
- Custom font import can impact readability; validation is defensive (magic bytes only).
