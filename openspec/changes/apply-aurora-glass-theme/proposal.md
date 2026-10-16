## Status

Implementation reverted on 2026-10-03 by
[ADR-035](../../../docs/architecture/ADR-035-restore-pre-aurora-ui.md). This proposal and its
design, specs and tasks are historical records, not current implementation targets. Resuming
this design requires a new explicit request.

## Why

ssilencce must adopt the visual language of the approved "Aurora Ondulante (glass · white accent)"
design prototype. The current blue M3 palette, opaque surfaces and wavy M3 chrome no longer match
the desired near-black glass aesthetic with a single white accent. This phase delivers the
tokens, shell chrome and library surfaces; player, queue, lyrics, EQ and settings restyling
follow in later phases.

## What Changes

- **Aurora token set**: static Material 3 palettes (`ui/theme/Color.kt`, `res/values/colors.xml`,
  `values-night`, `styles_parents.xml`) become Aurora — near-black chroma-0 backgrounds, glass
  surface tones, white hairlines, white primary accent (near-black inverted accent in Light).
  Role names are preserved; Black AMOLED overrides remain.
- **Material You becomes opt-in**: the `material_you` preference default flips from true
  (Android 12+) to false so Aurora is the default look on all devices.
- **Shell chrome**: BottomNavigationView styled as translucent glass with a white 3px caret +
  soft glow on the active item (labels still controlled by the existing `tab_titles_mode`
  preference, current 80dp height kept); miniplayer becomes a glass pill with a 2px white glow
  progress bar (squiggly seek bar remains a preference option); toolbar/AppBar restyled with
  title fade on collapsing headers.
- **Library & Home**: song rows gain index / equalizer-mini (playing row only) / duration
  anatomy with white active highlight + glow; grid covers get white halo + circular play
  overlay; album/artist detail headers get 220dp halo cover, circular shuffle/search, white
  wavy play; Top Tracks gets a centered header with Play/Shuffle pills; folders restyled
  (breadcrumbs untouched — none added); Home tiles become a 2×2 glass quad and suggestion
  floors get uppercase `sec-head` headings + railed cards.
- **Aurora background**: new `AuroraBackground` component (static layered aurora + animated
  white serpentine S on a single canvas, two layers phase-offset π) applied at the shell root;
  new `background_animation` preference (default ON); static fallback when the preference is
  off or reduced motion is requested (animator duration scale = 0); animation pauses when the
  shell is not visible.
- No reference-only features are added (no drawer, no multi-queues, no virtual keyboard, no
  second accent hue) and the feature set is unchanged. **BREAKING (visual only)**: default
  themed look changes for fresh installs.

## Capabilities

### New Capabilities
- `aurora-theme`: Aurora Glass visual language — tokens in all four theme modes, Material You
  opt-in default, glass shell chrome, restyled library/Home surfaces, and the `AuroraBackground`
  layered/Serpentine background behind the shell with the `background_animation` preference.

### Modified Capabilities
- None (no existing capability specs; this introduces the first visual-language specification).

## Impact

- Theme infra: `ui/theme/Color.kt`, `ui/theme/Theme.kt` (unchanged behavior), `res/values/colors.xml`,
  `res/values-night/colors.xml`, `res/values/styles_parents.xml`, `res/values/styles.xml`,
  `res/values-night-v31/styles.xml` (BlackThemeOverlay).
- Preference change: `util/Preferences.kt` (`material_you` default), `res/xml/preferences_screen_appearance.xml`
  new `background_animation` switch; strings only in `values/strings.xml`.
- Shell: `sliding_music_panel_layout.xml` (+ `layout-land`), `fragment_mini_player.xml`,
  `MiniPlayerFragment.kt`, `AbsSlidingMusicPanelActivity.kt`.
- Library: `item_list.xml`, `item_song.xml`, `item_song_detailed.xml`, `item_grid*.xml`,
  `item_card*.xml`, `item_album*.xml`, `item_artist.xml`, `item_detail_header.xml`,
  `fragment_detail_list.xml`, `abs_playlists.xml`, `item_suggestion.xml`,
  adapters `SongAdapter.kt`, `SimpleSongAdapter.kt`, `MediaEntryViewHolder.kt`,
  `HomeFragment.kt`, `HomeAdapter.kt`, `DetailAdapters.kt`, `TopAppBarLayout.kt` + its layouts.
- New: `AuroraBackground.kt` (Compose), `EqMiniView` (legacy row indicator), nav-item and
  progress drawables.
- Docs: `docs/architecture/ADR-033-aurora-glass-visual-language.md` (written), `docs/CODE_MAP.md`,
  `docs/FEATURES.md`, `docs/agents/domain.md`, `AGENTS.md`.
