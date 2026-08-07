# Smoke test: ssilencce on the emulator

Reproducible end-to-end test used to confirm the app still works after agent audits
(`gha-security-review`, `code-security`, `code-review`, `openspec-verify-change` +
`openspec-onboard`, `compose-performance-audit` + `android-accessibility`,
`gradle-build-performance`, `xml-to-compose-migration`). Run it **before** an audit
(baseline) and **after** each audited skill to detect regressions.

## Prerequisites

- Emulator AVD `ssilencce-mcp` (API 36) with the MCP bridge up:
  `.\scripts\mcp-emulator.ps1 start` (starts emulator, forwards tcp:8080, centers the window).
- App installed (debug build): `com.ssilencce.debug`, version `1.4.0-beta.3.debug`
  (versionCode 1400103). If absent: `.\gradlew :app:assembleGithubDebug` then
  `adb install -r app\build\outputs\apk\github\debug\*.apk`.
- Test media in MediaStore: `.\scripts\setup-test-media.ps1` (idempotent; regenerates the
  4 synthetic MP3s and rescans). **Do not** skip the `album_artist` tag — the Artists tab
  is empty when `onlyAlbumArtists` is on (default) and files only carry `artist`.
- Agent cannot read screenshots: drive the UI through the accessibility node tree
  (`android_get_screen_state` / `android_find_nodes` by text or content-desc).

Expected library after setup: 4 songs (Neon Sky, Starlight, Sunrise, Sunset), 2 albums
(Morning Vibes 2024, Night Drive 2023), 2 artists (Aurora Test, Nova Band).

## Smoke test checklist

Mark PASS/FAIL per step; any FAIL = regression to fix before archiving the audit.

1. **Launch** — `open_app com.ssilencce.debug`; MainActivity focused, no crash.
2. **Home** — `nav_home`: Top Tracks / Last added / History / Shuffle buttons + sections
   "Top albums" and "Recent artists" and "Recent albums", each with the 2 albums / 2 artists.
3. **Songs** — `nav_songs`: 4 rows with correct `title` and `0:30 • <artist>` text.
4. **Playback** — tap a song row: mini player appears (`mini_player_container`) with the
   song title; `songCurrentProgress` advances between two `get_screen_state` calls.
5. **Full player** — swipe up from the mini player: cover pager (`view_pager`),
   seek bar (`Value, <ms>`), transport buttons (`repeatButton`/`previousButton`/
   `nextButton`/`shuffleButton`), `queueInfo` "Next • <song> by <artist>".
6. **Transport** — tap `nextButton`: title changes and queueInfo advances; let a 30 s
   track end: auto-advance to the next song; at queue end `queueInfo` shows "List end".
7. **Lyrics** — `Show lyrics` action: "No lyrics found" + lyrics editor button; button
   toggles to "Hide lyrics".
8. **Player menu** — `More options`: Sleep timer, Equalizer, Add to playlist, Go to,
   Tag editor, Web Search, Share, Delete from device, Details.
9. **Equalizer** — open Equalizer: header (Back/External EQ/More), Enable EQ toggle,
   Profile, Bass Boost, Graphic EQ 10-band with per-band dB values; toggle Enable EQ;
   back out; no crash, playback unaffected.
10. **Albums** — `nav_albums`: 2 album cards; open one: header (title/subtitle), Play /
    Shuffle / Search actions, "Songs • 2" list with track numbers.
11. **Artists** — `nav_artists`: 2 artist cards "1 album • 2 songs". (Empty list here =
    media missing `album_artist`; rerun `setup-test-media.ps1`.)
12. **Playlists** — `nav_playlists` + header `More options` → New playlist: dialog with
    name/description fields, Cancel/Create; create "Test Playlist"; card appears with
    "0 songs".
13. **Settings** — Home header `action_settings`: Appearance, Now playing, Lyrics,
    Playback, Library, Network, Advanced, About ("ssilencce 1.4.0-beta.3.debug").
14. **Player style** — Settings → Now playing → Style: dialog with preview pager +
    color scheme/transition spinners; swipe to "Full Cover", OK; `Style` summary shows
    "Full Cover"; reopen and restore "Normal".
15. **Media notification** — with playback active, `android_open_notifications`:
    notification "Sunrise / Aurora Test", category transport, actions (previous, pause/
    play, next, repeat, shuffle); execute Pause → action flips to "Play"; resume.
    Note: the "next" action only appears while there is a next item in the queue
    (Media3 dynamic commands) — test with an active multi-song queue, not at "List end".
16. **Logcat** — `android_get_device_logs` (level E, package `com.ssilencce.debug`):
    empty. Also scan for ANR/Skipped frames (Choreographer) during the Compose screens
    (step 13/14/9) — cheap compose-performance signal.

## Mapping to audited skills

| Skill | What the smoke test covers |
|---|---|
| gradle-build-performance | Step 1 (APK install) plus `./gradlew :app:assembleGithubDebug` and `:app:lintGithubDebug` must succeed unchanged |
| gha-security-review / code-security | Steps 1-16: normal flows still work after CI/hardening changes; build + lint pass |
| code-review | Whole checklist as the functional "spec" axis |
| openspec-verify-change / openspec-onboard | Rerun the checklist for flows touched by the change under verification |
| compose-performance-audit | Steps 2, 9, 13, 14: Compose screens (Home, Equalizer, Settings, style dialog) render without jank/ANR; step 16 |
| android-accessibility | Throughout: interactive nodes expose content-description (e.g. "Play/Pause", "Shuffle all", "Back", "More options"); switches and seek bars are announced in the node tree |
| xml-to-compose-migration | Compose screens from the migration still render: Home, Equalizer, Settings, lyrics overlay (steps 2, 7, 9, 13) |

## Notes / gotchas

- Detail screens (album, Last added, ...) hide the bottom nav bar; press back first.
- Player sheet can cover the bottom nav when partially expanded — swipe down to collapse.
- `find_nodes` node IDs change between snapshots; prefer text/content-desc lookups.
- The "Android Remote Control MCP" ongoing notification is expected in the shade.
- Replacing files in place does **not** refresh MediaStore metadata — always go through
  `setup-test-media.ps1` (rm + `scan_volume` prune + push + per-file broadcast).
- Never change the app version while testing (rule 8); About must read 1.4.0-beta.3.debug.
