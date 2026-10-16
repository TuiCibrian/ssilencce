## 1. Tokens & theme defaults

- [ ] 1.1 Replace `ui/theme/Color.kt` Light/Dark role values with the Aurora palette (dark: Chroma-0 near-black background family, white primary/onPrimary near-black, glass surface containers, translucent-white hairline outline/outlineVariant; light: inverted near-white background, near-black primary).
- [ ] 1.2 Update `res/values/colors.xml` (+`values-night`) `md_theme_*` values and Aurora-specific resources (glass fills, hairlines, glow) to match the Compose palette; keep `surfaceContainer*Black` and overlays.
- [ ] 1.3 Flip `material_you` default to false in `util/Preferences.kt` (`isMaterialYouTheme` default arg) and in `res/xml/preferences_screen_appearance.xml` switch `defaultValue`.
- [ ] 1.4 Build a debug APK to confirm the token swap compiles before moving on (`./gradlew :app:assembleGithubDebug`).

## 2. AuroraBackground component

- [ ] 2.1 Create `ui/component/compose/background/AuroraBackground.kt`: static layers (dark base gradient, radial white glow, vertical scrim) + serpentine S path drawing (two strokes, phase-offset π, X-only sinus displacement), all on one `Canvas`.
- [ ] 2.2 Implement animation loop (`withFrameNanos`) gated by: `Preferences.backgroundAnimation` (new pref), animator duration scale > 0, and shell visibility; cancel on dispose/absence.
- [ ] 2.3 Add `background_animation` preference (default true) + strings (only in `values/strings.xml`) in `res/xml/preferences_screen_appearance.xml` Appearance category.
- [ ] 2.4 Apply `AuroraBackground` at the shell root: `ComposeView` behind content in `sliding_music_panel_layout.xml` (+`layout-land`), make `fragment_container` background transparent, keep scrim overlay above the aurora and below the UI.

## 3. Shell chrome

- [ ] 3.1 BottomNavigationView restyle: translucent glass background + hairline top border, active-item white caret (3px, top, white glow) + accent-tint pill, white active icon/text and muted inactive items (selector tints); keep 80dp height and `tab_titles_mode` behavior; restyle `NavigationRailView` counterpart for landscape.
- [ ] 3.2 Miniplayer glass pill: translucent rounded container + hairline border, 2px horizontal progress bar with white glowing fill (on `fragment_mini_player.xml` + `MiniPlayerFragment`); keep wavy ring when `squiggly_seek_bar` is on.
- [ ] 3.3 Toolbar/AppBar restyle: translucent app bar surfaces, hairline separators, title fade tied to collapse progress in `TopAppBarLayout` collapsing mode (`collapsing_appbar_layout.xml`), search/icon buttons as glass pills.
- [ ] 3.4 Shell surface transparency pass: ensure toolbar overlays, miniplayer sheet and bottom nav retain legible contrast over the aurora (scrim + glass alphas) per theme mode.

## 4. Library lists & grids

- [ ] 4.1 Song row anatomy: extend `item_list.xml` with idx + duration nodes; bind in `SongAdapter`/`MediaEntryViewHolder` (position index, title/subtitle, duration, more); highlight playing row (white title + soft glow, accent index) via existing data-set + current-song flows.
- [ ] 4.2 `EqMiniView` (three-bar equalizer indicator with white glow) shown only on the playing row, cancelled/released on rebind and on scroll away.
- [ ] 4.3 Simple song rows (`item_song.xml`/`item_song_detailed.xml`): same anatomy (track number/idx, 40–46dp cover, duration) used on album/artist details.
- [ ] 4.4 Grids restyle (`item_grid.xml`, `item_card.xml`, `item_card_color.xml`, `item_grid_circle.xml`, `item_playlist.xml`): white circular play overlay with glow, white cover halo (soft outer glow drawable), hairline cover borders.
- [ ] 4.5 Album/artist detail header (`item_detail_header.xml` + `HeaderAdapter`): 220dp cover + white halo, centered layout, circular 44dp shuffle/search buttons, white wavy play (restyled `MorphicIconButton` with glow).
- [ ] 4.6 Top Tracks (`fragment_detail_list.xml` + `DetailListFragment`): centered header with Play/Shuffle pills (reuse `playAction`/`shuffleAction` slots).
- [ ] 4.7 Folders restyle: glass list rows + hairline separators in `FoldersListFragment`/`FolderDetailFragment`, no breadcrumb additions.

## 5. Home

- [ ] 5.1 Home quad: rebuild `abs_playlists.xml` as a 2x2 grid of glass cards (icon chip + label + subtitle) preserving the four button IDs bound by `HomeBinding`/`HomeFragment`.
- [ ] 5.2 Suggestion floors: restyle `item_suggestion.xml` (uppercase `sec-head` heading + circular go button) and rail cards (`item_album_gradient.xml`, `item_artist.xml`) to railed halos + overlay labels.
- [ ] 5.3 Remove/replace now-obsolete `HomeActionButton` style usage with the quad card styles (keep style resources tidy).

## 6. Verification

- [ ] 6.1 `./gradlew :app:assembleGithubDebug` and `./gradlew :app:lintGithubDebug` pass.
- [ ] 6.2 `/codecheck` pass (ktlint + skill review).
- [ ] 6.3 Emulator: install githubDebug APK, walk Home / Songs / Albums / Artists / album+artist detail / Top Tracks / Folders with screenshots; compare against reference screens (nav caret, miniplayer pill, row anatomy, halos, quads, headers).
- [ ] 6.4 Emulator: theme switching Light/Dark/Black/System + `background_animation` on/off + reduced-motion behavior; logcat scan for crashes; notification presence check.
- [ ] 6.5 Docs sync: `docs/CODE_MAP.md` (AuroraBackground, EqMiniView, new drawables, changed layouts), `docs/FEATURES.md` (background toggle + visual language), `docs/agents/domain.md` ADR list, `AGENTS.md` map; ADR-033 already written.
