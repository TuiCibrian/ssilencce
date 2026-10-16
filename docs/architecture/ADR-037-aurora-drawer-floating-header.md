# ADR-037: Aurora Drawer-Led Navigation and Floating Header Actions

## Status

Accepted on 2026-10-09. Refines the navigation surface of ADR-036 without changing the selectable Aurora appearance, the ribbon implementation, playback behavior or non-Aurora screens.

## Context

ADR-036 gave Aurora a configurable horizontal category strip on every library screen plus a scrim-dismissable drawer. The collaborator then asked for the reference chrome to be applied more literally: no upper horizontal category navigation on Home or anywhere else, the reference scroll treatment on the hamburger/search/overflow actions, the reference drawer groups with real destinations, a vector drawer mark, and equal-sized home action tiles. A read-only design reviewer produced a binding reference-to-native contract (`AURORA-NATIVE-CONTRACT.md`) from the immutable `ssilencce-auroras.html` and the Open Design `ssilencce` project; its measured findings (for example the subtitle tile being taller than its peer) are part of the acceptance basis.

Removing the category strip is a deliberate deviation from the reference's Songs-view tab strip: the collaborator explicitly requested removal "on Home and rest of app", and category switching remains available through the drawer and the primary navigation.

## Decision

- Retire the `AuroraCategoryTabs` injection everywhere; the base fragment keeps only the hamburger drawer entry for library destinations. Category/queue/search/EQ/settings navigation lives in the drawer and the six-destination primary navigation. The retired tab-contrast token stays in source with an explanatory comment and no callers; `AuroraDesign.muted` is untouched.
- Apply the reference header scroll treatment on Aurora library screens: the app bar is pinned, the title fades/translates/blurs with `(scrollTop - 6dp) / 28dp`, the action icons become floating glass pills (`#C716161A` with a `#17FFFFFF` stroke, ripple and soft elevation) and the header space collapses through the scrolling-view overlay. System animations disabled snap the state instead of interpolating; blur is skipped below API 31; 48 dp targets, content descriptions and insets are preserved.
- Restructure the drawer to the reference groups: Library (songs/albums/artists/folders/playlists plus the queue), Discover (most played, recently added, search), Sound & system (equalizer, settings). Most played and recently added reuse the existing detail-list destinations; genuinely unavailable capabilities stay honestly marked.
- Render the drawer header with the existing app-mark vector (`app_logo_splash`) inside a 46 dp rounded black box; no raster or mipmap is used for the drawer.
- Stretch the home action rows per row (`IntrinsicSize.Min` plus `fillMaxHeight`) so the shuffle tile with its song-count subtitle matches its peer, as the reference grid does.
- Preserve ADR-036's motion/battery gates, the ribbon rasters, the measured tab/foreground contrast fixes and all real playback data and actions.

## Consequences

- One navigation surface disappears; users reach categories through the drawer, and the reference's Songs-view tab strip is intentionally not reproduced (recorded here).
- The header scroll effect supports two compatible modes: simple `TopAppBarLayout` screens pin the bar and collapse the header space through the scrolling-view overlay, while `R.id.app_bar` collapsing screens keep their scroll flags and pinned toolbar and receive the pill treatment; layouts with neither structure are left untouched.
- The contract and this ADR keep the deviations explicit, so future fidelity passes can distinguish intentional chrome decisions from accidental drift.
