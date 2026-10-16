## Purpose

Provide the reference Aurora chrome: drawer-led category navigation, floating header actions with the reference scroll treatment, a vector brand mark and equalized home action tiles, without changing playback behavior or non-Aurora screens.

## ADDED Requirements

### Requirement: Drawer-led category navigation
Aurora library screens SHALL NOT show the horizontal category strip anywhere. Categories SHALL remain reachable through the scrim-dismissable drawer and the primary navigation, and the drawer SHALL present the reference groups Library, Discover and Sound & system with real destinations.

#### Scenario: Library screen without the strip
- **WHEN** any Aurora library screen is shown
- **THEN** no horizontal category strip is rendered and the hamburger opens the library drawer

#### Scenario: Drawer destination
- **WHEN** the user selects an available drawer entry such as songs, albums, artists, folders, playlists, queue, search, equalizer or settings
- **THEN** the corresponding real destination opens with its existing menus and actions

#### Scenario: Unavailable capability
- **WHEN** the drawer presents a genuinely unavailable capability
- **THEN** the row is announced as disabled and does not simulate a successful operation

### Requirement: Floating header actions
The hamburger, search and overflow actions SHALL remain on screen while the page scrolls. The header title SHALL fade, translate and blur with the scroll distance, the action icons SHALL adopt the reference floating glass treatment, and the header space SHALL collapse as the content scrolls under it.

#### Scenario: Scrolling a library screen
- **WHEN** the user scrolls an Aurora screen with an app bar and a scrolling content view
- **THEN** the title fades away and the action icons stay visible as floating pills over the content

#### Scenario: Reduced motion
- **WHEN** system animations are disabled
- **THEN** the header switches between its top and floating states without interpolation or blur

#### Scenario: Unsupported layout
- **WHEN** a screen has no app-bar plus scrolling-view pair
- **THEN** it is left unchanged

### Requirement: Equal home action tiles
The Home action rows SHALL render all tiles of a row at the same height, including the shuffle tile with its song-count subtitle, and SHALL keep the real shuffle action.

#### Scenario: Home rows
- **WHEN** the Home actions render with a real library count
- **THEN** both tiles of each row share the same height and the shuffle tile runs the existing shuffle action

### Requirement: Vector brand mark
The drawer header SHALL draw the app mark from the existing vector resource inside a 46 dp rounded black box together with the app name and a short subtitle; no raster or mipmap asset SHALL be used for the drawer mark.

#### Scenario: Drawer header
- **WHEN** the drawer opens
- **THEN** the vector mark, the app name and the subtitle are visible and readable

### Requirement: Preserved Aurora behavior
The refinement SHALL keep the ribbon rasters, motion and battery gates, the measured tab/foreground contrast token, real playback data and actions, and SHALL leave non-Aurora screens unchanged.

#### Scenario: Aurora disabled
- **WHEN** the user disables the Aurora appearance
- **THEN** the classic interface, its own navigation and all playback behavior remain available
