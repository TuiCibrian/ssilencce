# Tasks: Album-artist and composer most-played shuffle strategies

## 1. Policy and model

- [x] 1.1 Add pure `ShuffleGroupKeyPolicy` with album-artist fallback and unknown composer group.
- [x] 1.2 Propagate `Song.composerName` through `ExpandedSong` without changing existing callers.

## 2. Shuffle modes

- [x] 2.1 Add `MostPlayedAlbumArtists` and `MostPlayedComposers` enum entries, icons and main
  English titles/descriptions.
- [x] 2.2 Add both weighted branches to `ShuffleManager` using the existing `ln(sum + 1)` formula
  and `1.0` base weight.
- [x] 2.3 Preserve all existing modes, enum order, weighting and failure fallback.

## 3. UI

- [x] 3.1 Rely on the existing Compose shuffle sheet's `SpecialShuffleMode.entries`; no new screen
  or composable is required.

## 4. Documentation and OpenSpec

- [x] 4.1 Create this OpenSpec proposal, design, spec and tasks.
- [x] 4.2 Update `docs/FEATURES.md`, `docs/MISSING_FEATURES.md` and `docs/CODE_MAP.md`.

## 5. Verification

- [x] 5.1 Run the pure grouping-key/availability probe: **16/16 PASS**.
- [x] 5.2 Run the static source probe: **81/81 PASS**.
- [x] 5.3 Run ktlint and `git diff --check`: PASS.
- [x] 5.4 Run `openspec validate add-advance-shuffle-strategies --strict`: valid.
- [x] 5.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: **BUILD SUCCESSFUL**, **3994F4F4** `3994f4f4ccc1e79f61f665a118e9de5116805ae51882a15c1dc0b70e3c25b327` (53,478,676 bytes, mtime `2026-11-07T05:53:24.9030222Z`), build 2m 37s, 0 lint errors / 1,443 warnings. Runtime verification remains pending; no emulator was used.

## 6. Review fix — composer platform availability

- [x] 6.1 Add `ShuffleModeAvailabilityPolicy` (SDK >= 30) and mark Most played composers as
  composer-dependent.
- [x] 6.2 Filter composer mode from the advanced shuffle sheet below API 30 while keeping
  album-artist mode available.
- [x] 6.3 Make the manager fall back to a plain shuffle for a stale composer mode below API 30.
- [x] 6.4 Update the OpenSpec scenario, FEATURES and MISSING_FEATURES with API30+ composer
  availability.
- [x] 6.5 Add pure availability and static probe coverage: **16/16 pure**, **81/81 static**.
- [ ] 6.6 Build/freeze the post-review source. The 3994F4F4 APK predates the availability
  correction; no new candidate is claimed until a fresh freeze/build.

## 7. Runtime fix — special-shuffle queue preservation (SUPERSEDED 2026-11-10)

> Rejected by the 2026-11-10 product decision. The items below are historical implementation
> records for the superseded preservation experiment. Do not execute 7.5; the original restart
> contract in section 8 is authoritative.

- [x] 7.1 Capture the current media id, playback position and `playWhenReady` before rebuilding
  the special-shuffle queue.
- [x] 7.2 If the current media id is present in the new queue, restore its new index and position;
  otherwise start at index 0 / position 0. Restore `playWhenReady` instead of forcing playback.
  Leave the non-special `openShuffle()` path unchanged.
- [x] 7.3 Add a static source probe for the preservation path and the unchanged non-special path.
- [x] 7.4 Update the OpenSpec scenario, design, FEATURES and CODE_MAP note.
- [ ] 7.5 Device re-run on the next frozen campaign: verify current item, position and play/pause
  state before/after a special-shuffle switch.

## 8. Original restart contract — 2026-11-10 decision

- [x] 8.1 Remove current-item/position/play-intent capture, mapping, seek and `playWhenReady`
  restoration from `openSpecialShuffle`; install the new weighted queue at item 0 with
  `setMediaItems(mediaItems, true)` and `play()`.
- [x] 8.2 Restore the plain `SET_UNSHUFFLED_ORDER` command and remove pending-order state,
  timeline application and AA05Shuffle debug diagnostics from `PlaybackService`.
- [x] 8.3 Remove the preservation-only command extras from `Playback` and the AA05Shuffle
  diagnostics/tag from `PlayerViewModel`.
- [x] 8.4 Amend the OpenSpec proposal/design/spec/tasks and add the dated decision record; add a
  source/pure probe for restart-at-zero.
- [ ] 8.5 Independent Spec and Standards/security review of this final delta.
- [x] 8.6 Fresh frozen build and targeted runtime gate: new queue, start at 0, play; classify the
  historical Source error separately. **emulator-qa 2026-11-14 PASS** on the final-gate x86_64
  APK (`E5FC0E1B1F81F180F16E1463FC71E2AE56298A1E98432FCDB019E1331F48D01A`): before the switch
  Long Track was paused at 20.712 s (active item id 4); choosing Most played produced a
  13-item weighted queue, `state=3` playing, `position=0`, active item id 0 and metadata
  `MD One`; no Source error occurred. Evidence:
  `runtime/verify/results/evidence/41-shuffle-sheet.*`, `42-special-shuffle-after.png`.
