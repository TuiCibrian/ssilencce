## Why

The collaborator requested a stricter application of the approved Aurora reference chrome: remove the upper horizontal category navigation on Home and across the app, keep the hamburger/search/overflow actions with the reference scroll treatment, restructure the drawer to the reference groups with real destinations, replace the drawer brand mark with the vector app mark, and equalize the home action tiles. A read-only design reviewer bound the implementation to the immutable golden `ssilencce-auroras.html` and the Open Design `ssilencce` project (contract `AURORA-NATIVE-CONTRACT.md`).

## What Changes

- Retire the `AuroraCategoryTabs` strip everywhere; the hamburger drawer entry and the primary navigation remain the category surfaces (ADR-037).
- Apply the reference header scroll treatment to Aurora library screens: pinned bar, title fade/translate/blur, floating glass action pills, collapsing header space, reduced-motion snap.
- Restructure the drawer into the reference groups (Library / Discover / Sound & system) with real destinations, the vector app mark plus title/subtitle, and honest unavailable rows.
- Stretch the home action rows per row so the Shuffle tile with its song-count subtitle matches its peers.
- Record the decisions and evidence in ADR-037, CODE_MAP, FEATURES and this change.

## Capabilities

### New Capabilities

- `aurora-chrome`: Drawer-led navigation, floating header actions and equalized home tiles for the optional Aurora appearance.

### Modified Capabilities

None at the archived spec level. The retired category strip was an implementation choice; the capability delta adds `aurora-chrome` and ADR-037 records the refinement.

## Impact

Aurora-only UI source (`AuroraNavigation.kt`, `AuroraHomeActions.kt`, `AuroraLegacyStyling.kt`, new `AuroraHeaderScroll.kt`, `AbsMainActivityFragment.kt`, `AbsSlidingMusicPanelActivity.kt`), main `values/strings.xml`, docs and OpenSpec records. No engine, database, network, dependency, version, signing, contributor or translated-string changes. Queue and player files are owned by the separate aurora-player scope.
