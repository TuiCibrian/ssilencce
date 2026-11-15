# Design: Harden widget accessibility and preserve appearance (AA-10)

## Context

The three Glance widgets (`SsilencceGlanceWidget`, `CardWidget`, `FullWidget`) share one playback
snapshot and one artwork-derived theme. ADR-050 adds per-widget `System`/`AlbumArt`/`Wallpaper`
appearance, a per-instance DataStore keyed by `appWidgetId`, 0-100 transparency with 0.35 surface
and 0.30 scrim alpha floors, a shared `WidgetEmptyState` with a compact form below 160 dp, and a
Compose configure/reconfigure activity. Those behaviors are already in source and must survive the
accessibility fix unchanged.

The acceptance packet `4E332456...` records two hard blockers:

1. media-control click targets below 48x48 dp because `.clickable` sits on the inner image;
2. dark `onTertiaryContainer` sourced from `lightColorScheme`.

Later source review found that the Large/ExtraLarge required-height model used 48 dp while the
layouts pass a 64 dp play target, and that ExtraLarge omits a 4 dp `additionalInfo` top inset. The
v4 candidate corrects both and makes the shared constants feed both the layout and the model. C
`04601B2C...` and K `F89E7C29...` are source-qualified, and the exact 5-file V4 set is now
live-integrated (receipt `803160B5...`). It remains unbuilt and runtime-pending; integration is not
a build, font-fit, TalkBack, contrast or any other runtime PASS.

## Goals / Non-Goals

**Goals:**

- every media-control click node is at least 48x48 dp, without removing actions or changing the
  layout family;
- all dark theme colors, including `onTertiaryContainer`, come from the dark scheme;
- responsive selection fits the declared sizes at 1.0/1.5/2.0 font scale, falling back to
  compact/Narrow families instead of clipping;
- preserve Wallpaper, per-instance settings/config/delete, transparency, empty state,
  provider/config and all actions;
- define the complete runtime acceptance matrix.

**Non-Goals:**

- new widget features, layouts, actions, dependencies, Room schema, permissions, version or
  translated strings;
- changes to playback, queue, AA-05 or AA-06 code;
- claiming source review as runtime or accessibility proof;
- archive or AA-10 completion.

## Decisions

### 48x48 dp click targets

`ShapeableControlIconGlance` and `CircularControlIconGlance` put the clickable modifier on an outer
container of at least 48x48 dp; the visual glyph keeps its 24/28/32 dp size. Compact/full layouts
that pass a 64 dp play target keep that size. The source proof must show no clickable modifier on
an inner image smaller than 48 dp and no removed control branch.

### Dark color scheme

Every `dark*Color` in `WidgetTheme()` is sourced from `darkColorScheme`; specifically
`darkOnTertiaryContainerColor` uses `darkColorScheme.onTertiaryContainer`. Light fields remain
unchanged. Runtime contrast is measured separately.

### Responsive height/font-scale model

Required heights are computed from `context.resources.configuration.fontScale` with:

- `WIDGET_LINE_HEIGHT_FACTOR = 1.2f`;
- `WIDGET_SELECTION_MARGIN_DP = 1f`;
- one required-height function per family (Classic, Simplified, Medium, Large, ExtraLarge, Narrow,
  Full);
- shared layout constants for Large/ExtraLarge: 104 dp art, 16 dp outer padding, 4 dp controller
  top padding, 64 dp play, 16/24 dp ExtraLarge outer padding, 24 dp section spacer and 4 dp
  ExtraLarge info inset.

The selector checks width first (ExtraLarge/Large at 320 dp, Medium at 240 dp, otherwise Narrow at
140 dp height), then the family's required height. Classic/Simplified fall to compact when their
required height plus the 1 dp margin does not fit. Full selects normal or compact from
`fullNormalRequiredHeight(fontScale)`.

Source-model consequences at 2.0: Large requires 253.6 dp and ExtraLarge 304.8 dp, so
`320x240`/`360x240` select Medium and `320x300`/`360x300` select Large. The v3 values 237.6/284.8
are rejected as a negative control.

The 1.2 line-height factor is an **assumption**. Actual Compose text metrics, font padding, line
spacing and the host's font-scale update are runtime gates. The 1 dp margin is not a guarantee.

### Preservation

- **Wallpaper:** keep `WidgetThemeMode.Wallpaper`, API 27+ `getWallpaperColors(FLAG_SYSTEM)`,
  per-widget cached ARGB, config re-derive, API 26/null fallback to album art then system, and no
  wallpaper permission.
- **Per-instance:** keep `appWidgetId` keys, theme/transparency/wallpaper cache, `onDelete` cleanup
  in all three widgets, and configure/reconfigure result behavior.
- **Transparency:** keep 0-100, opaque text/on-colors and the 0.35 surface / 0.30 scrim alpha
  floors. Those floors are implementation qualifiers, not a contrast guarantee and not a waiver of
  the original contrast requirement; the transparency matrix remains a runtime gate.
- **Empty state:** keep the shared `WidgetEmptyState`, whole-surface click, compact form below
  160 dp and the non-compact form; all three widgets keep using it.
- **Provider/config:** keep `android:configure`, `android:widgetFeatures="reconfigurable"`,
  `updatePeriodMillis=0`, sizes/resize modes and the existing config activity.
- **No feature removal:** keep every shuffle/repeat/favorite/previous/next/play branch and the
  `showAllActions = width >= 220.dp` rule.

## Runtime gates

These are mandatory and cannot be satisfied by the source model:

1. Measured `AccessibilityNodeInfo` bounds >=48 dp for every media control at 220x48/70/140/180/
   240/260/300, 240x140/148/176, 320x240/254.6/300/305.8 and 360x240/254.6/300/305.8, at font
   scale 1.0/1.5/2.0.
2. Launcher rounding at exact-fit gates: Medium 240 width, Large/XL 320 width, XL 300 height,
   compact 48 height, Simplified 73 height, Full 294 height and the 254.6/305.8 boundaries.
3. Actual text layout/line height/font padding at 1.0/1.5/2.0, including host font-scale updates.
4. TalkBack focus, announcement and activation for every control; no overlap or clipping at
   220 dp width.
5. Dark no-artwork placeholder contrast; transparency matrix at 0/50/100%. The 0.35/0.30 floors
   are implementation qualifiers, not measured contrast and not a waiver of the original contract.
6. Wallpaper derivation and fallback on API 26/live wallpaper/provider failure; per-instance
   independence, reboot persistence and reconfigure.
7. Delete/re-add cleanup in all three widgets.
8. Card and Full empty-state pages (Responsive 3A-1 already has historical evidence).
9. API 26/27 `reconfigurable` behavior.
10. All layout families and actions retained; no feature removal.

## Source estimates (not runtime proof)

The v4 geometry probe reports 101/101 checks, 0 width failures, 0 height/font-scale failures and
45 exact-fit gates. It parses the shared constants and models padding, art and 1.2 line heights.
It is a source dp model only; it is not a device measurement or a font-fit proof.

## Risks / Trade-offs

- The 1.2 line-height factor may under- or over-estimate real text; exact-fit rows need launcher
  measurement.
- Moving `.clickable` outward can change ripple/visual bounds; the visual glyph must stay
  unchanged and the hit area must not overlap neighbors.
- The host may cache an old font scale; a configuration update is a runtime gate.
- Wallpaper and transparency remain best-effort/heuristic, not guaranteed contrast.
- The widget V4 five-file set is live-integrated and source-reviewed; the separate CoverProvider/SmartRepository/arrays patches are not part of this change and are not in the 66CC APK. Any source change invalidates the reviewed hashes.
