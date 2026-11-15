# Widget accessibility and appearance

## Purpose

Track the original AA-10 widget contract and its acceptance gates: accessible media controls,
dark-theme contrast, font-scale-aware responsive layouts, and preservation of per-widget
appearance, transparency, empty state, configuration and all actions. Source-qualified candidates
are not runtime proof, and this change does not claim AA-10 completion.

## ADDED Requirements

### Requirement: Media controls have 48x48 dp click targets

Every interactive media control in the three widgets SHALL have a clickable node of at least
48x48 dp. The clickable modifier SHALL be on an outer container or overlay that owns the full hit
area, not on an inner image smaller than 48 dp. Visual glyph sizes MAY remain 24/28/32 dp, and the
layout family SHALL NOT be changed to create room.

#### Scenario: Classic previous/play/next
- **WHEN** the Classic layout is shown at a supported size
- **THEN** each previous, play/pause and next control has a measured clickable node of at least
  48x48 dp and activates its media action

#### Scenario: Compact play target
- **WHEN** a compact or full layout uses a 64 dp play target
- **THEN** the clickable node covers at least that 64 dp target and no inner image owns a smaller
  clickable area

#### Scenario: No action removal
- **WHEN** the 48 dp hit areas are added
- **THEN** every shuffle, repeat, favorite, previous, play/pause and next branch remains present
  and no control is merged, hidden or removed

### Requirement: Dark theme uses the dark color scheme

The dark widget theme SHALL source every `dark*Color`, including `onTertiaryContainer`, from
`darkColorScheme`. No dark field SHALL be sourced from `lightColorScheme`. Light fields SHALL
remain unchanged.

#### Scenario: No-artwork placeholder in dark mode
- **WHEN** the widget shows the no-artwork placeholder in dark mode
- **THEN** the placeholder foreground and background come from the dark scheme and remain visibly
  distinct

#### Scenario: Dark field audit
- **WHEN** the theme assignments are reviewed
- **THEN** every dark field is traceable to `darkColorScheme`, with no light-scheme source

### Requirement: Responsive layouts fit at supported font scales

The responsive selector SHALL choose a layout family whose required height fits the widget height
at font scale 1.0, 1.5 and 2.0, using compact or Narrow fallbacks instead of clipping. Required
heights SHALL use the actual text sizes, padding, art and play-target constants of each family,
including the 64 dp Large/ExtraLarge play target and the ExtraLarge 4 dp info inset. The source
model's line-height factor and selection margin are assumptions recorded in design, not measured
text proof.

#### Scenario: Large/ExtraLarge at 2.0
- **WHEN** the widget is 320x240 or 360x240 at font scale 2.0
- **THEN** the selector chooses Medium, because Large requires more than 240 dp

#### Scenario: ExtraLarge fallback at 2.0
- **WHEN** the widget is 320x300 or 360x300 at font scale 2.0
- **THEN** the selector chooses Large, because ExtraLarge requires more than 300 dp

#### Scenario: Compact Classic/Simplified
- **WHEN** a short widget cannot fit the full Classic or Simplified required height
- **THEN** the compact form is selected without dropping any action

### Requirement: Wallpaper appearance is preserved

The Wallpaper theme SHALL remain available with the original behavior: API 27+ public
`getWallpaperColors(FLAG_SYSTEM)`, a per-widget cached ARGB value, re-derivation when Wallpaper is
selected in configuration, API 26/null fallback to album art then system colors, and no wallpaper
permission.

#### Scenario: Wallpaper available
- **WHEN** the user selects Wallpaper on API 27+ and the provider returns colors
- **THEN** the widget uses the derived theme and caches it per `appWidgetId`

#### Scenario: Wallpaper unavailable
- **WHEN** the device is API 26, the provider returns null or extraction fails
- **THEN** the widget falls back to album art and then system colors without failing

### Requirement: Per-instance settings, configuration and deletion are preserved

Theme, transparency and wallpaper cache SHALL remain keyed by `appWidgetId`. Configure and
reconfigure SHALL remain available on all three providers, settings SHALL survive process death,
and deleting a widget SHALL remove its stored settings.

#### Scenario: Independent widgets
- **WHEN** two widget placements have different theme/transparency settings
- **THEN** each keeps its own appearance after reboot and playback updates

#### Scenario: Reconfigure
- **WHEN** the user reconfigures an existing widget
- **THEN** the new settings apply to that placement only

#### Scenario: Delete and re-add
- **WHEN** a widget is deleted and a new one is added
- **THEN** the old settings are cleaned up and the new placement receives a new `appWidgetId`

### Requirement: Transparency remains available without a false contrast guarantee

The per-widget 0-100 transparency control SHALL remain, with opaque text/on-colors and the
existing 0.35 surface / 0.30 scrim alpha floors. Those floors are implementation qualifiers, not a
measured contrast guarantee and not a waiver of the original contrast requirement.

#### Scenario: Opaque and transparent extremes
- **WHEN** transparency is set to 0% and 100%
- **THEN** the widget renders both states and keeps text/on-colors opaque

#### Scenario: Contrast evidence
- **WHEN** the transparency matrix is run
- **THEN** contrast is measured at representative values; the alpha floors alone are not recorded
  as a contrast PASS

### Requirement: Empty state is preserved

The shared `WidgetEmptyState` SHALL remain used by all three widgets when the title is blank and
artwork is absent. It SHALL open `MainActivity`, keep the whole surface clickable, omit the icon
below 160 dp with 14/12 sp title/action text, and keep the icon+title+action form at 160 dp and
above. A paused title-bearing track SHALL keep the normal layout.

#### Scenario: Responsive empty page
- **WHEN** the Responsive widget has no title and no artwork
- **THEN** the empty state shows the title and action inside the widget and opens the app

#### Scenario: Short empty page
- **WHEN** the empty widget is shorter than 160 dp
- **THEN** the icon is omitted and both the compact title and action remain visible

#### Scenario: Card and Full empty pages
- **WHEN** the Card or Full widget has no title and no artwork
- **THEN** the same shared empty state is shown and remains clickable

### Requirement: Provider and configuration behavior is preserved

All three provider XMLs SHALL keep `android:configure`,
`android:widgetFeatures="reconfigurable"`, `updatePeriodMillis=0`, their sizes and resize modes.
The existing Compose configuration activity SHALL remain the only configuration path.

#### Scenario: First add
- **WHEN** a widget is added for the first time
- **THEN** the configuration activity opens and saves settings with `RESULT_OK`

#### Scenario: Cancel configuration
- **WHEN** the user cancels configuration
- **THEN** the activity returns `RESULT_CANCELED` without saving

### Requirement: No feature or scope regression

The change SHALL NOT remove a layout family or media action, change playback/queue behavior, add a
dependency, Room schema, permission, version change or translated string, or modify AA-05/AA-06
source.

#### Scenario: Layout/action audit
- **WHEN** the source is reviewed
- **THEN** Classic, Simplified, Medium, Large, ExtraLarge, Narrow, Full and Card remain, with their
  documented action counts and no removed branch

#### Scenario: Scope audit
- **WHEN** the diff is reviewed
- **THEN** it is limited to widget UI source and contains no dependency, schema, permission,
  version, translated-string or playback/queue change

### Requirement: Runtime acceptance evidence is mandatory

AA-10 SHALL NOT be marked complete, integrated or PASS from source review alone. Acceptance SHALL
require measured runtime evidence for click bounds, TalkBack, contrast, wallpaper fallback,
per-instance behavior, empty states, API 26/27 configuration, transparency, exact-fit launcher
rounding and actual text metrics at 1.0/1.5/2.0 font scale.

#### Scenario: Source-only review
- **WHEN** only the source model and source reviews exist
- **THEN** the change remains source-qualified and no AA-10 PASS or completion is claimed

#### Scenario: Missing runtime gate
- **WHEN** any mandatory runtime gate is unrun or inconclusive
- **THEN** it remains an explicit open gate and is not waived by an implementation qualifier

#### Scenario: Archive
- **WHEN** runtime gates, build/lint and exact live hashes are complete
- **THEN** archive may be considered separately; this tracking bundle does not pre-authorize it
