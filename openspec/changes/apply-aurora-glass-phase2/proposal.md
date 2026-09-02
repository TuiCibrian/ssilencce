## Why

Phase 1 delivered the Aurora Glass tokens, shell chrome and library surfaces (ADR-033). The
player, queue, lyrics, EQ, search and settings still render with pre-Aurora styling, so the
app feels inconsistent the moment the same token set reaches the rest of the surface. This
phase restyles the remaining surfaces: the quick wins (queue, lyrics, EQ, search, settings)
first, then a full Compose recompose of the 7-style player — the reference design is
player-centric (`.player--now`, `.np-*`), so the flagship surface must adopt it fully.

## What Changes

- **Queue** (`ui/screen/queue/`) — Aurora anatomy: current row gets a 1dp accent hairline
  border + accent-tint fill + rounded 12dp, title/index/duration in accent (white); rows and
  header pill use translucent glass fills over the aurora with hairline separators; 3-dot
  chips become small glass squares; reordering, eq-mini on cover, go-to-current FAB unchanged.
- **Lyrics** (`ui/screen/lyrics/`, `ui/component/compose/lyrics/`) — Aurora scrim token
  replaces the hardcoded blur scrim; editor chrome gets hairlines; edit FAB becomes a glass
  circle. User settings (font, karaoke, blur) keep working.
- **EQ** (`ui/screen/equalizer/`) — cards and selector pills get glass fills + 1dp hairlines;
  waveform accent deltas aligned to the prototype; behavior untouched.
- **Search** (legacy XML) — restyled in place (no screen rewrite): glass pill search field
  with 2dp accent underline, chips as outline pills with accent active text; results inherit
  the Phase 1 Aurora rows; `SearchAdapter` CAB multi-select preserved.
- **Settings** (legacy `PreferenceFragmentCompat`) — restyled in place: Aurora `.set-row`
  anatomy (glass list, hairline dividers, icon chip circles, muted right-aligned values),
  uppercase `sec-head` group headers; `NowPlayingScreenPreferenceDialog` restyled to Aurora.
- **Player — full Compose recompose**: a new Compose player host in `player_container`
  renders the selected style from `Preferences.nowPlayingScreen`; all 7 styles (Default,
  FullCover, Gradient, Plain, M3, Expressive, Peek) are ported as Compose composables;
  shared building blocks (`GlassSurface`, `AuroraSlider`, `AuroraIconButton`,
  `AuroraPlayButton`, `CoverPager`, gesture handlers, marquee, blur background) replace the
  legacy custom-view machinery; `PlayerViewModel`/`PlayerColorScheme` stay the state source;
  legacy style fragments, cover pager, transforms and tint animators are removed once all
  styles are green behind a new `compose_player` preference (default ON, with the legacy
  stack kept until the port is finished).
- The reference-only `compose_player` backdoor is removed at cleanup; no features added.

## Capabilities

### New Capabilities
- `aurora-surfaces`: Aurora Glass styling for queue, lyrics, EQ, search and settings —
  glass fills, hairlines and white-accent highlights on the already-Compose screens, and
  in-place restyle of the legacy search/settings hierarchies (no screen rewrites, no
  behavior change).

### Modified Capabilities
- `aurora-theme` (extended): player surfaces adopt the Aurora visual language through a
  Compose recompose; the `compose_player` preference (default ON) enables the new player
  during the transition.

## Impact

- Compose screens restyled (token-only): `UiScreenQueue`, `QueueScreen`, `LyricsScreen`,
  `LyricsEditorScreen`, `EqualizerScreen`, shared cards in `ui/component/compose/`.
- Legacy restyled: `fragment_search.xml`, `SearchFragment.kt`, `SearchAdapter.kt`,
  `res/xml/preferences_*.xml`, `SettingsFragment.kt`, `PreferencesScreenFragment.kt`,
  `NowPlayingScreenPreferenceDialog` + item/resource styles.
- Player: new `ui/screen/player/compose/` package (host + 7 styles + shared blocks);
  removed legacy player fragments/layouts/`CoverPagerFragment`/`ImageFragment`/
  `PlayerGesturesController`/`PlayerAnimator`/ViewPager transforms + dormant drawables.
- Preference: `util/Preferences.kt` new `compose_player` switch (default ON);
  `res/xml/preferences_screen_player.xml` entry (kept through the transition, removed at
  cleanup if no longer useful).
- Docs: `docs/architecture/ADR-034-aurora-player-compose-recompose.md` (new),
  `docs/CODE_MAP.md`, `docs/FEATURES.md`, `docs/agents/domain.md`, `AGENTS.md`.
