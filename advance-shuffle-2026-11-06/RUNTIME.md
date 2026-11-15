# AA-05 runtime — advance shuffle strategies

Candidate: **0C423B6D** (`0C423B6DE814C41803C22B79184017A6A68203BE7442061BDE4845D9F430A211`, 53,478,676 bytes)
Task device: **emulator-5560 only** (never 5554/5562). API 30 (`ssilencce-qa30`) for the matrix, then API 26 (`ssilencce-qa26`) on the same port for the composer gate. No app source or build was edited.

## 0. Resume point and blockers

A host/server restart killed the emulators mid-run. The exact resume point was: **album-artist 30/30 runs already captured**; composer `C_H` partially seeded at `70=6, 71=6, 72=5`, `N_H` not seeded, queue `70–72`. After reboot of `ssilencce-qa30` on 5560, the queue was cleared (play counts preserved), `N_H` was seeded to `78=13, 79=9`, and the 30 composer runs were executed. No album-artist run was repeated.

Blockers encountered: transient `uiautomator dump ... could not get idle state`, a host build file overwritten during the restart (not used — the preserved pulled base APK was used), and a `Source error` during the late mode-switch capture caused by emulator shutdown. These are called out where they affect a verdict.

## 1. Identity gate — PASS

| Step | Result |
|---|---|
| Pre-install package on 5560 | `3994F4F4CCC1E79F61F665A118E9DE5116805AE51882A15C1DC0B70E3C25B327` (old candidate) |
| Installed APK | `0C423B6DE814C41803C22B79184017A6A68203BE7442061BDE4845D9F430A211` |
| Pulled installed base (`pm path` → `adb pull`) | **exact match** `0C423B6D…A211`, 53,478,676 B |
| Recheck after server/emulator restart | **exact match** `0C423B6D…A211`, 53,478,676 B |
| API 26 install from the preserved pulled base | **exact match** `0C423B6D…A211`, 53,478,676 B |

Evidence: `evidence/preinstall-base.apk`, `evidence/installed-base.apk`, `evidence/installed-base-postreboot.apk`, `evidence/installed-base-api26-fixed.apk`.

Note: the host path `app/build/outputs/apk/github/debug/…universal.apk` was overwritten by another build (`D3C7824A…`, 53,479,204 B) during the server restart. It was **not** used. API 26 was installed from the preserved pulled base APK whose SHA-256 is the frozen `0C423B6D…`.

## 2. Fixtures and library — PASS

20 tagged MP3s were generated on the host (`fixture-manifest.json`) and pushed to `/sdcard/Music/AA05`; MediaStore was scanned; the app library showed **20 songs** before playback. Fixtures are 2.038 s tones so real play-count loops are practical; the app’s minimum-duration test setting was set to **1 s** (default 15 s would hide them). This setting is test-only and documented below.

| Group | Songs | Tags |
|---|---|---|
| `AA_H` | 64,65,66 | album artist `AA05 AlbumArtist Heavy` |
| `AA_L` | 67,68,69 | album artist `AA05 AlbumArtist Light` |
| `F_H` | 62,63 | **blank album artist**, artist `AA05 Fallback Heavy` |
| `F_C` | 60,61 | blank album artist, artist `AA05 Fallback Control` |
| `C_H` | 70,71,72 | composer `AA05 Composer Heavy` |
| `C_L` | 73,74,75 | composer `AA05 Composer Light` |
| `N_H` | 78,79 | **no composer**, high plays |
| `N_C` | 76,77 | **no composer**, zero plays |
| neutral | 60–79 remainder | `AA05 SetA Neutral` / `AA05 SetB Neutral` |

MediaStore confirmed composer/album-artist tags (including `NULL` for omitted/whitespace values). Evidence: `songs_verify1/2/3.xml`, `albumartists.xml`, `composers.xml`, `api26_mediastore_inventory.txt`, `api26_home3.xml`.

## 3. Real play-count differences — PASS

Play counts were created by **actual ExoPlayer playback**, not by DB seeding: filtered group queues were played through repeatedly and the app’s own `PlaybackService.onMediaItemTransition` wrote `PlayCountEntity`. Counts were read back from `music_database.db`.

Phase A (album-artist):
`62=13, 63=12, 64=14, 65=25, 66=25`; all light/control songs `60,61,67,68,69 = 0`.

Phase B (composer):
`70=6, 71=6, 72=5, 78=14, 79=11`; `73,74,75,76,77 = 0` at the start of the 30 runs.

Evidence: `evidence/db/snap_albumartist_counts_before.db`, `snap_composer_counts_before.db`, `nh_counts.txt`, `seed_nh_log*.txt`.

Method detail: group weights are `1.0 + ln(group_play_sum + 1.0)`, matching `ShuffleManager`. A 2-song queue played to the end and was restarted with Previous+Play; counts are genuine transition counts. Shuffle runs pause the player within ~1.2 s of starting to limit play-count drift; the pre-run snapshot is used for the model. Skip counts changed, play counts did not change materially except the unknown composer group during the 30 composer runs (documented in §4).

## 4. Statistical matrix — PASS

30 real shuffles per strategy. Queue order was pulled from `QueueEntity` after each run; rank 1 = first. Significance is a one-sided sign test on the 30 per-run mean-rank differences (heavy should rank earlier). The weighted model is a 20,000-run simulation of the same `ln(sum+1)`/1.0-base policy.

### Album-artist mode

| Comparison | Observed mean rank | Sign test | Weighted-model fit |
|---|---|---|---|
| `AA_H` vs `AA_L` | **6.611 vs 14.511** | **30/30**, p = **9.313e-10** | sim 5.311 vs 12.120; diff −7.900 vs −6.809, z = −0.298 |
| `F_H` vs `F_C` (blank album artist → artist fallback) | **7.650 vs 14.033** | **25/30**, p = **1.625e-4** | sim 5.986 vs 12.125; z = −0.052 |
| `AA_H` vs `F_C` | 6.611 vs 14.033 | **29/30**, p = **2.887e-08** | — |

Conclusion: heavy album-artist groups are favored; blank album-artist songs follow their **artist** fallback; the effect is consistent with the intended weighted policy.

### Composer mode

| Comparison | Observed mean rank | Sign test | Weighted-model fit |
|---|---|---|---|
| `C_H` vs `C_L` | **7.767 vs 11.956** | **24/30**, p = **7.155e-4** | sim 6.787 vs 12.635; z = 0.422 |
| `N_C` vs `C_L` (no-composer unknown group) | **5.550 vs 11.956** | **28/30**, p = **4.340e-07** | sim 6.399 vs 12.635; z = −0.040 |
| `N_H` vs `N_C` (same unknown group) | 5.700 vs 5.550 | 14/30, p = 0.708 | diff +0.150, z = 0.029 |

Conclusion: composer-heavy songs are favored; **all no-composer songs share one unknown group** (zero-play `N_C` ranks like high-play `N_H`, not like the light composer group). Composer play counts drifted upward during the 30 runs (`76=8,77=7,78=22,79=19` after), which only strengthens the unknown group; the pre-run snapshot was used for the model.

### Every eligible song exactly once

All **60/60** captured shuffles (30 album-artist + 30 composer) returned exactly 20 items and a permutation of the 20 eligible MediaStore IDs (60–79). No duplicates, no drops. Evidence: `queues_albumartist.json`, `queues_composer.json`.

Stats: `stats_albumartist.txt/.json`, `stats_composer.txt/.json`.

## 5. Mode switching preservation — FAIL (special modes); toggle INCONCLUSIVE

Runtime capture (`mode_switch.json`): while **playing** (`state=3`), the current item was `AA05 B NoComposer Control 02` (index 3 in its queue). Selecting **Most played album artists** from the shuffle sheet changed the queue and current item to `AA05 B NoComposer Heavy 02` (index **0**), still `state=3`. Current item and queue position were **not preserved**; the new strategy always rebuilds the queue from all songs and starts at its first item. Position 0→0 in this capture is not discriminating, and another capture ended in `state=7` (“Source error”) during the emulator shutdown, so position/play-state preservation is not established.

A separate shuffle on/off toggle capture (`mode_switch_coords.json`) kept the same item/index, but both snapshots were already in `state=7` (source error), so that check is **INCONCLUSIVE** rather than PASS.

This is a real runtime FAIL against the stated “switching modes preserves current item, position and play state” criterion; it is not inferred from source.

## 6. API 30 vs API 26 composer gate — PASS

- **API 30 (qa30, 5560):** the actual shuffle sheet contains both `Most played album artists` and `Most played composers`; composer was selected for all 30 composer runs. Evidence: `shuffle_sheet2.xml`, `sheet_modes_scrolled.xml`, `queues_composer.json`.
- **API 26 (qa26, 5560):** with a 20-song library, the actual shuffle sheet shows `Most played album artists` and **no** `Most played composers`; the sheet was scrolled to the bottom and the string is absent. Evidence: `api26_home3.xml`, `api26_songs.xml`, `api26_sheet.xml`, `api26_sheet2.xml`, `api26_sheet.png`, `api26_sheet2.png`. The API 26 installed base also hash-matches `0C423B6D…`.

## 7. Harness QA — PASS

The harness lives only at `advance-shuffle-2026-11-06/tools/` (no copy in the repo root). A self-test was added for the ElementTree truth-value hazard: a found childless `<node>` is falsy under Python 3.13, so all checks use explicit `is not None`. `selftest` returns PASS for the childless-leaf case and the fallback case. Evidence: `harness_selftest.xml`, `harness_selftest_result.json`. The old truth-value logic did not contribute any result above; every result here was re-run with the corrected parser.

## 8. Limitations

- Fixtures are 2.038 s and the app’s minimum-duration setting was set to 1 s for the test. This is a test setting, not a source change.
- Play counts are real but the 30 shuffle runs briefly start playback before the pause; the pre-run count snapshot is used for weights, and drift is reported.
- The mode-switch capture was affected by the emulator/server shutdown (`Source error`); the current-item change is unambiguous, position/play-state preservation is not.
- 5554, 5556 and 5562 were never used or modified.

## 9. Evidence index

- Identity: `installed-base.apk`, `installed-base-postreboot.apk`, `installed-base-api26-fixed.apk`.
- Fixtures/library: `fixture-manifest.json`, `songs_verify1/2/3.xml`, `albumartists*.xml`, `composers.xml`, `api26_home3.xml`.
- Real counts: `evidence/db/snap_albumartist_counts_before.db`, `snap_composer_counts_before.db`, `nh_counts.txt`.
- Shuffle orders: `queues_albumartist.json`, `queues_composer.json`.
- Statistics: `stats_albumartist.txt/.json`, `stats_composer.txt/.json`.
- Mode switch: `mode_switch.json`, `special_mode_switch_runtime.json`, `mode_switch_coords.json`, `ms_*.txt`, `switch_*.png`.
- API 26 gate: `api26_sheet.xml`, `api26_sheet2.xml`, `api26_sheet2.png`, `api26_logcat.txt`.
- Harness: `harness_selftest_result.json`.

## 10. Cleanup

After the measurements, **only emulator-5560** was cleaned: app data cleared/uninstalled, `/sdcard/Music/AA05` removed, temporary prefs removed, emulator stopped. 5554/5556/5562 were untouched.
