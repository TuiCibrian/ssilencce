## 1. Quick wins: Queue

- [ ] 1.1 `QueueScreen.kt`: glass header/pill fills + hairline separators; keep `SmallHeader` + toggles.
- [ ] 1.2 Current row: 1dp accent hairline + accent-tint fill + 12dp shape, accent title/index/duration; 3-dot glass chip.
- [ ] 1.3 Verify reorder + eq-mini + FAB unchanged; build+lint; emulator screenshot vs reference `.q-row.active`.

## 2. Quick wins: Lyrics

- [ ] 2.1 Replace hardcoded scrim with Aurora token in lyrics overlay (`.kts/` in `ui/screen/lyrics/`, `ui/component/compose/lyrics/`).
- [ ] 2.2 Hairline editor chrome + glass edit FAB.
- [ ] 2.3 Verify lyrics settings (font/karaoke/blur) still work; build+lint; emulator check.

## 3. Quick wins: EQ

- [ ] 3.1 Glass+hairline variants for `TitledCard`/`SwitchCard` (shared composables).
- [ ] 3.2 Align selector pills/waveform accents to prototype (token-only).
- [ ] 3.3 Verify presets/A-B/sound tab unchanged; build+lint; emulator screenshot vs `.eq-*`.

## 4. Quick wins: Search

- [ ] 4.1 `fragment_search.xml`: glass pill field + 2dp accent underline; chips → outline pills with accent active text (IDs intact).
- [ ] 4.2 Confirm result rows inherit Phase 1 anatomy + CAB multi-select still works; build+lint; emulator check.

## 5. Quick wins: Settings

- [ ] 5.1 Preference row theme: glass list + hairline dividers + icon chips + uppercase `sec-head` (via `preferenceTheme`/`itemLayout` styles).
- [ ] 5.2 Restyle `NowPlayingScreenPreferenceDialog` (and any legacy items) to Aurora rows.
- [ ] 5.3 Verify navigation + all prefs functional in Dark/Light/Black; build+lint; emulator check.

## 6. Player scaffolding + shared blocks

- [ ] 6.1 New `ui/screen/player/compose/` package: `GlassSurface`, `AuroraIconButton`, `AuroraPlayButton`, `AuroraSlider`, `EqBars` reuse, `MarqueeText`, `PlayerGestures`, `CoverPager`.
- [ ] 6.2 `ComposePlayerFragment` host + `compose_player` preference (default ON) + XML entry; `chooseFragmentForTheme` swap in `AbsSlidingMusicPanelActivity`.
- [ ] 6.3 Default style port (flagship `.player--now`): cover pager + halo, centered meta, 7-icon row, AuroraSlider + times, transport, inline-lyrics toggle row.
- [ ] 6.4 Verify: all 4 theme modes, gestures, transitions, marquee, blur, fav, up-next, sleep timer, Peek height — emulator screenshots vs reference.

## 7. Remaining player styles

- [ ] 7.1 M3 + Plain ports (card cover, radius pref, pill play).
- [ ] 7.2 Gradient + FullCover ports (gradient bg, full-bleed cover+scrim).
- [ ] 7.3 Expressive + Peek ports (row header + segmented transport; compact + ring play + up next).
- [ ] 7.4 Verify all 7 styles per scheme/gesture/transition; screenshots each; build+lint.

## 8. Cleanup + picker

- [ ] 8.1 Restyle now-playing picker dialog; decide `compose_player` entry fate (likely remove).
- [ ] 8.2 Delete legacy style fragments/controls, `CoverPagerFragment`/`ImageFragment`, `PlayerGesturesController`/`PlayerAnimator`, unused transforms/drawables/styles; grep each before delete.
- [ ] 8.3 Final build+lint (lint errors fail); `/codecheck`.

## 9. Verification

- [ ] 9.1 `:app:assembleGithubDebug` + `:app:lintGithubDebug` pass at every block.
- [ ] 9.2 `/codecheck` pass.
- [ ] 9.3 Emulator walk: queue/lyrics/EQ/search/settings + all 7 styles × themes; logcat scan; notification check.
- [ ] 9.4 Docs: ADR-034, CODE_MAP, FEATURES, MISSING_FEATURES, domain.md, AGENTS.md.
