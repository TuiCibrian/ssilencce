## Purpose

Defines the Aurora Glass visual language for ssilencce: the token palette used by all four theme
modes, the glass shell chrome, the restyled library and Home surfaces, and the layered /
animated aurora background with its settings-controlled behavior.

## ADDED Requirements

### Requirement: Aurora palette in all theme modes

The system SHALL apply the Aurora Glass palette (near-black chroma-0 backgrounds, glass surface
tones, white hairline dividers, white primary accent in dark modes and near-black inverted
accent in Light) to every theme mode: Light, Dark, Black AMOLED and Follow System. Black AMOLED
SHALL keep its existing pure-black surfaces and white hairlines. The existing Material You
option SHALL keep working and repaint the app with wallpaper-derived colors when enabled.

#### Scenario: Fresh install in Dark mode

- **WHEN** the app is freshly installed (no saved preferences) and run in Dark mode with
  Material You disabled
- **THEN** the background is near-black Chroma 0, primary controls render in white with
  near-black foreground content, hairlines render as translucent white, and no blue hue is
  present in the theme

#### Scenario: Light mode invert

- **WHEN** the app is run in Light mode with static Aurora palette
- **THEN** the background is near-white, primary accent is near-black, and hairline dividers
  are translucent black

#### Scenario: Black AMOLED

- **WHEN** the app is run in Black AMOLED mode
- **THEN** surfaces are pure black with the existing `surfaceContainerBlack` family and white
  hairline dividers remain visible

#### Scenario: Material You re-enabled

- **WHEN** a user enables Material You on Android 12+
- **THEN** the system wallpaper palette replaces the Aurora accent and surfaces, and the
  remaining Aurora tokens do not apply

### Requirement: Material You defaults to off

A fresh install SHALL have the `material_you` preference set to off so the Aurora static palette
is the default visual on all devices; users SHALL be able to enable it from the appearance
settings on devices that support dynamic color.

#### Scenario: Fresh install default

- **WHEN** a user installs the app fresh on an Android 12+ device without changing settings
- **THEN** the app renders with the Aurora static palette rather than system wallpaper colors

### Requirement: Glass shell chrome

The app shell SHALL use Aurora glass chrome: a translucent bottom navigation bar with a white
3px underscored caret and soft white glow on the active item, an active-item tint background,
and neutral (muted) inactive items; the miniplayer SHALL be a translucent glass pill containing
a 2px track with white glowing progress fill; the synthetic or collapsing app-bar SHALL fade its
title proportionally to scroll collapse. Squiggly seek-bar visualization SHALL remain available
as a preference option. The bottom navigation height and label-mode preference SHALL remain
unchanged.

#### Scenario: Active navigation item

- **WHEN** the user switches to a library tab
- **THEN** the active item shows the white caret with soft glow, a tinted pill background and
  white icon/text, while inactive items remain muted

#### Scenario: Miniplayer progress

- **WHEN** a track is playing and the miniplayer is collapsed
- **THEN** a 2px horizontal progress bar with white glowing fill shows playback progress, and
  the pill container is translucent with a hairline border

#### Scenario: Collapsing app bar title

- **WHEN** the user scrolls content under a library app bar in collapsing mode
- **THEN** the title fades out as the app bar collapses and fades back in when it expands

### Requirement: Restyled library and Home surfaces

Song lists SHALL render rows with an index, cover (40–46dp), title/subtitle, duration and
more-menu, and the row of the currently playing track SHALL highlight with the white accent and
a soft glow plus a small animated equalizer indicator; album/artist detail headers SHALL render
a 220dp cover with a white halo and circular shuffle/search buttons next to a white wavy play
button; Top Tracks SHALL render a centered header with Play/Shuffle pills; folder browsers SHALL
use the same Aurora list styling; Home SHALL render a 2x2 glass quad of entry tiles and
uppercase section headings with horizontal railed cards.

#### Scenario: Playing song row

- **WHEN** the currently playing track is visible in a song list
- **THEN** its title renders white with a soft glow, the index renders in the accent color, and
  an equalizer indicator appears over the cover while it keeps playing

#### Scenario: Album detail header

- **WHEN** the user opens an album or artist detail screen
- **THEN** the header shows a 220dp cover with a soft white halo, a circular shuffle button, a
  white wavy play button and a circular search button

#### Scenario: Home tiles

- **WHEN** the user opens Home
- **THEN** the four entry tiles (Top Tracks, Last Added, History, Shuffle) render as a 2x2 grid
  of translucent glass cards with an icon chip, label and subtitle

#### Scenario: Home suggestion floor

- **WHEN** Home shows a suggestion floor
- **THEN** the floor heading is uppercase with a circular "open" button and the cards are
  horizontally railed with white halo covers and overlay labels

#### Scenario: Top Tracks header

- **WHEN** the user opens Top Tracks
- **THEN** the header is centered and provides Play and Shuffle action pills

### Requirement: Aurora background with animation preference

The app SHALL render an Aurora background at the shell root consisting of a dark base, a soft
white radial glow and a vertical gradient scrim, plus — when animation is enabled — a white
serpentine S strip drawn as two layers phase-offset by π with 2D horizontal undulation only.
A `background_animation` preference SHALL default to ON; when off, only the static layered
background SHALL render; the animation SHALL stop when the shell is not visible or when the
animator duration scale is 0 (reduced-motion); the shell surfaces SHALL remain legible over the
background in all theme modes.

#### Scenario: Toggle off

- **WHEN** the user disables `background_animation` in appearance settings
- **THEN** the static layered background renders and the serpentine layer is not drawn

#### Scenario: Reduced motion

- **WHEN** the animation preference is ON but animator duration scale is 0
- **THEN** the static layered background renders and no animation frames are drawn

#### Scenario: App backgrounded

- **WHEN** the shell is not visible
- **THEN** the serpentine animation does not run

### Requirement: Feature scope unchanged

The repaint SHALL NOT add features that exist only in the design reference: no navigation
drawer, no multiple queue support, no virtual keyboard, no second accent hue (only white/near-
black), no new acknowledgements/ratings UI. All existing settings, actions and navigation
behavior SHALL keep working.

#### Scenario: No drawer

- **WHEN** the user browses any library screen
- **THEN** no drawer or drawer affordance is shown; navigation remains the bottom bar (or rail
  in landscape) and dialog destinations
