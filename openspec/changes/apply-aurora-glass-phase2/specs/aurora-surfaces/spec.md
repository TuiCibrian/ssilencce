## Purpose

Defines the Aurora Glass restyle of the remaining surfaces after Phase 1: the queue, lyrics,
EQ, search and settings screens, and the full Compose recompose of the now-playing player
(7 styles) including its transition preference, shared component library and the retirement
of the legacy player machinery.

## ADDED Requirements

### Requirement: Aurora queue surface

The queue screen SHALL render with Aurora Glass styling: the header pill and list surfaces
use translucent glass fills (surface container + alpha) with 1dp hairline borders; rows
render with rounded 12dp shape, and the currently playing row SHALL use a 1dp white accent
hairline border, accent-tint fill, white accent title/index/duration and a glass 3-dot chip.
Reordering (drag handles), the eq-mini indicator on the playing cover, the shuffle/repeat/
lock toggles and the go-to-current action SHALL keep working.

#### Scenario: Current row highlight

- **WHEN** a row is the currently playing queue entry
- **THEN** the row shows a 1dp accent hairline border with accent-tint fill, the title
  renders in the accent color with index/duration also accented, and an equalizer indicator
  remains visible over its cover

#### Scenario: Reordering unchanged

- **WHEN** the user drags a row to reorder the queue
- **THEN** the operation behaves exactly as before the restyle (no new animations or
  behavioral changes)

### Requirement: Aurora lyrics surface

The lyrics screen SHALL render with the Aurora visual language: the background scrim uses
the Aurora dark scrim token in place of any hardcoded overlay color, editor chrome
(NavigationBar/top bars) renders with hairline separators and glass fills, and the edit
action is a glass circle with a hairline. All user lyrics preferences (font size, text
style, karaoke mode, background blur effect) SHALL keep working unchanged.

#### Scenario: Settings preserved

- **WHEN** a user changes a lyrics setting (e.g. karaoke on)
- **THEN** the setting keeps working exactly as before and is unaffected by the restyle

### Requirement: Aurora EQ surface

The equalizer screen SHALL render with Aurora Glass styling: titled/switch cards use
translucent glass fills + 1dp hairlines, waveform accents use the theme primary (white in
Aurora dark), and selector pills match the prototype chrome. Equalizer behavior, presets,
A-b switching and the sound tab SHALL be unchanged.

### Requirement: Aurora search surface

The legacy search screen SHALL be restyled in place: the search field becomes a glass pill
with a 2dp accent underline, chips render as outline pills with accent active text, and
result rows inherit the Aurora list anatomy from Phase 1. Multi-selection via the context
action bar SHALL keep working.

### Requirement: Aurora settings surface

The legacy settings screens SHALL be restyled in place to the Aurora `.set-row` anatomy:
glass list surfaces with hairline dividers, icon chips as circular glass tiles, right-aligned
muted value text, and uppercase section headings. All preferences SHALL remain functional
and navigable; the now-playing screen picker dialog SHALL be restyled to Aurora rows.

### Requirement: Compose player recompose

The now-playing player SHALL be recomposed to Jetpack Compose and SHALL render the style
selected by the existing `now_playing_screen` setting. All 7 styles (Default, FullCover,
Gradient, Plain, M3, Expressive, Peek) SHALL be available with the same behaviors: cover
paging with the configured transition, gestures (taps, double-tap, flings per gesture
settings), marquee title behavior, color scheme modes (AppTheme / Simple Color / Vibrant /
Material You), blur background mode, quick actions, favorites, up-next preview, queue/
lyrics routing, sleep timer and Peek sheet height switching. A `compose_player`
preference SHALL exist during the transition (default ON). The legacy player fragments,
controls fragments, cover pager, transforms and tint animators SHALL be removed after all
styles are ported and verified.

#### Scenario: Style selection preserved

- **WHEN** a user selects a different player style (e.g. Expressive) in settings
- **THEN** the recomposed player renders that style with the same visual hierarchy and
  interactions

#### Scenario: Cover transition preserved

- **WHEN** the user swipes the cover pager
- **THEN** the configured `PlayerTransition` (carousel/depth/flip/stack/parallax/zoom/
  hinge) is applied with equivalent motion

#### Scenario: Gesture settings preserved

- **WHEN** the user performs taps/double-tap/fling gestures on the player
- **THEN** the actions configured in the gesture settings are triggered as before

#### Scenario: Color scheme modes preserved

- **WHEN** the player's color scheme mode is AppTheme / Simple Color / Vibrant / Material You
- **THEN** the player surfaces take colors from the configured scheme with smooth
  transitions, matching the legacy behavior

#### Scenario: Transition backdoor

- **WHEN** the `compose_player` preference is disabled during the transition
- **THEN** the legacy player stack renders instead of the Compose player

## MODIFIED Requirements

### Requirement: Aurora player surface (extension of aurora-theme)

The player surfaces SHALL be Aurora-styled as part of the recompose: Default follows the
prototype `.player--now` (cover with halo glow, centered meta, 7-icon action row, 4dp
AuroraSlider with white glowing thumb, 62dp white play button with glow, muted transport
siblings); M3/Plain/Gradient/FullCover/Expressive/Peek keep their hierarchy but with Aurora
tokens (glass fills, white accent, hairlines, glow) instead of pre-Aurora surfaces.

#### Scenario: Aurora transport

- **WHEN** a track is loading/playing/paused on the Default style
- **THEN** the play button is a 62dp white circle with a soft glow, the slider track is 4dp
  with a white glowing fill and thumb, and transport siblings render muted with accent on
  the active one

## Requirement: Feature scope unchanged (extension)

The recompose SHALL NOT add features: no drag-back gestures, no new quick actions, no
alternative title layouts, no reference-only player chrome. All existing player
functionality SHALL map 1:1 from legacy to Compose.
