## 1. Chrome implementation

- [x] 1.1 Retire the Aurora category strip injection; keep the hamburger entry and primary navigation.
- [x] 1.2 Implement the reference header scroll treatment (pinned actions, title fade/translate/blur, glass pills, collapsing space, reduced-motion snap).
- [x] 1.3 Restructure the drawer groups with real destinations and the vector app mark.
- [x] 1.4 Equalize the home action tiles per row.
- [x] 1.5 Confirmed on the final installed build via Android Emulator MCP (Home, drawer, Album detail top/scrolled, queue, player). Lossless 1080x2400 ADB companions captured on the same artifact: `refine-chrome-final-drawer-clean.png` (motion on), `refine-chrome-final-album-top-clean.png`, `refine-chrome-final-album-scrolled-clean.png`, `refine-chrome-final-queue-flat.png` (animator 0), `refine-chrome-final-queue-moving.png` (motion on), `refine-chrome-final-nowplaying-clean.png` (Full Cover), `refine-chrome-final-nowplaying-glass-clean.png` (Gradient). MCP navigation frames: `refine-chrome-final-mcp-00..15`.

## 2. Verification and documentation

- [x] 2.1 ktlint format/check on the changed files.
- [x] 2.2 Debug + Release + lint build with JDK 21 on the frozen shared source. BUILD SUCCESSFUL 3m 51s (exit 0) after the scoped crash fix; lint 0 errors / 1124 warnings; Debug x86_64 `4CCF9A05…38CA` = on-device base.apk; Release x86_64 `C7318210…E770`.
- [x] 2.3 Strict OpenSpec validation (`refine-aurora-chrome` and `implement-native-aurora-reference`).
- [x] 2.4 ADR-037, CODE_MAP, FEATURES and the reviewer contract recorded.
- [x] 2.5 Reviewer final visual acceptance received (PASSED, 2026-10-14; queue contrast item closed; no rebuild needed).
- [x] 2.6 Pre-halo epoch: fresh user-requested invocation on 2026-10-14 (BUILD SUCCESSFUL 14s, exit 0; identical hashes; installed and re-hash-verified) with the `refine-chrome-20261014-0845-*` demonstration.
- [x] 2.7 Halo polish (`aurora-player`, `AuroraPlayerScreen.kt`) reviewed and rebuilt: Debug `784F911F…CC61` = installed, Release `553D5668…A4E6`; gate BUILD SUCCESSFUL 4m 49s, lint 0 errors / 1129 warnings; final captures `refine-chrome-20261014-0908-*` (companions + MCP frames 04..07); reviewer source review PASSED and the FullCover halo re-profile was requested on the new companion.
- [x] 2.8 Reviewer halo visual re-profile CONFIRMED on the post-halo companion (bands gone; max edge step 2; ~27/255 edge contribution; cover unchanged); final delivery cleared.

## 3. Queue and Now Playing recreation (aurora-player scope, consolidated here)

- [x] 3.1 Queue chrome: reference header, queue selector + queues-manager dialog, play/pause state, next-title badge, disabled named-queue actions and an explicit unavailable search row (`QueueScreen.kt`, `QueueFragment.kt`).
- [x] 3.2 Now Playing: square centered cover with a smooth pre-blurred cached bitmap halo (bounded cache, single draw, no per-frame blur) and the FullCover glass backdrop (`AuroraPlayerScreen.kt`).
- [x] 3.3 Scoped queue contrast tokens (`AuroraQueueCaptionColor` #D0D0D0, `AuroraQueueArtistColor` #DCDCDC) verified by the reviewer; `AuroraDesign` untouched.
- [x] 3.4 Device frames on the final installed build: queue flat (animator 0) and queue moving (motion on) companions, Full Cover and Gradient player companions, all bound to `4CCF9A05…38CA`.
