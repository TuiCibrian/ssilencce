# Tasks — fix-backup-queue-release-signing

> Status 2026-10-21: 24/26 tasks complete. Open: 3.4 production-signed Release (no keystore), 6.1 physical-device profiling. Keep the change active; do not treat emulator/helper evidence as production or physical acceptance.

## 1. Backup and restore

- [x] 1.1 Write the lyrics JSON without closing the ZIP (byte write + closeEntry) and reject nothing based on declared sizes; enforce real-byte limits (10 MB lyrics / 100 MB entry / 100 MB total / 1000 entries) with a counting stream view.
- [x] 1.2 Add cancellation (`ensureActive` per entry/line, chunked copy) with `CancellationException` rethrow, unsafe-basename rejection and `Log.e` diagnostics on the three catch-alls.
- [x] 1.3 Make `LyricsEntity` `@Serializable` so the lyrics cache encodes/decodes in backups (`LyricsEntity$$serializer` generated).
- [x] 1.4 Verify on JVM: round-trip with lyrics followed by an artist image, skipped-entry continuation, declared size -1 acceptance, 10 MB/100 MB/entry-count limits, unsafe names — `C:\Users\geode\AppData\Local\Temp\opencode\ssilencce-verify\backup-probe-output.txt` (all passed).
- [x] 1.5 Verify cancellation semantics (cancelled context throws `CancellationException`, active passes) — `…\ssilencce-verify\cancellation-probe-output.txt` (passed).
- [x] 1.6 Validate the crafted fixture against the compiled serializer — `…\ssilencce-verify\decode-probe-output.txt` (`DECODE_OK`, `ENCODE_OK`).
- [x] 1.7 On the emulator (Android runtime): app-produced archive restores without rejection; hand-crafted lyrics + artist-image archive restores (lyrics row inserted, image file written); over-10 MB lyrics entry rejected with `IOException: Backup entry exceeds its size limit` both when selected and when unselected (drain path) — `…\ssilencce-verify\logcat-overlimit.txt`, `…\logcat-drain-overlimit.txt`, `…\db-run5.db`, `…\db-overlimit3.db`, `…\db-run6.db` (success path re-verified after the drain fix).

## 2. Queue persistence

- [x] 2.1 Key `QueueEntity` on the queue position and replace rows transactionally (`clearQueue` + `saveQueue` in one `@Transaction`).
- [x] 2.2 Add `MIGRATION_6_7` (new table with `PRIMARY KEY(\`order\`)`, `INSERT OR REPLACE … ORDER BY \`order\``, drop, rename) and register it; bump Room to v7.
- [x] 2.3 SQLite probe: v6 collapse reproduced, migration preserves rows/order, DDL matches Room's generated fresh-install DDL, A,B,A kept, repeated replacement stable — `…\ssilencce-verify\queue-probe-output.txt` (all passed).
- [x] 2.4 Emulator migration + duplicate round-trip: existing v6 DB migrated (rows `22,21,19,20` preserved, `user_version` 7), saved A,A queue restored after cold start with both occurrences visible in the queue UI.
- [x] 2.5 Compact the saved shuffle order through the saved-to-restored position map on restore instead of discarding it when unavailable media items are filtered; validate the payload as a permutation, disable shuffle for the session on undecodable/invalid data without emptying the queue, keep the unshuffled fallback when no order is saved, preserve the empty-queue behavior, map the resume index by occurrence, and reset position-in-track when the saved current item is gone.
- [x] 2.6 Remove unavailable media items from the live timeline by `QueueItem.second` timeline indices (group consecutive timeline ranges, remove descending) instead of shuffle-list positions; capture one timeline snapshot, revalidate identity/count on Main, and abandon stale passes before removal/publication.
- Evidence (2.5): `C:\Users\geode\AppData\Local\Temp\opencode\ssilencce-verify\ShuffleRemapJvmProbe.java` — 6012 checks against the compiled `SerializedOrder.remap`; `C:\Users\geode\AppData\Local\Temp\opencode\ssilencce-verify\shuffle-remap-probe.py` — 13948 model checks.
- ktlint format + check on ImprovedShuffleOrder.kt, PersistentStorage.kt, PlayerViewModel.kt: exit 0.
- :app:assembleGithubDebug: BUILD SUCCESSFUL (JDK 21, 20s, configuration cache reused).
- :app:lintGithubDebug: BUILD SUCCESSFUL (JDK 21, 2m15s).
- Compiled JVM probe ShuffleRemapJvmProbe.java: 6012 checks passed against the actual compiled SerializedOrder.remap.
- Python model/property probe shuffle-remap-probe.py: 13948 checks passed.
- openspec validate fix-backup-queue-release-signing --strict --no-interactive: Change is valid.

## 3. Release signing

- [x] 3.1 Refuse `assemble*Release` / `bundle*Release` / `install*Release` without keystore configuration unless `-PallowDebugSigning=true` / `SSILENCCE_ALLOW_DEBUG_SIGNING=true`; add the `releaseUnconfigured` validate-signing backstop; require an existing `storeFile` when configured.
- [x] 3.2 Verify refusals (`assembleGithubRelease`, `bundleGithubRelease`, `installGithubRelease`, abbreviated `aGR`, configuration-cache repeat) and the backstop (`validateSigningGithubRelease`); verify debug/lint usable without keystore (`assembleGithubDebug`, `lintGithubRelease`, `help`).
- [x] 3.3 Opt-in Release build succeeds with the NON-PRODUCTION warning; `apksigner verify --print-certs` shows `CN=Android Debug` (verified locally, artifact hashes recorded in the receipt).
- [ ] 3.4 Production-signed Release — blocked: no release keystore available locally (`keystore.properties` absent); CI injects it from repository secrets. Rechecked 2026-10-23: no `keystore.properties`, no `.jks`/`.keystore` under the repo, no signing env vars; no replacement key generated and debug signing is not counted (`runtime/verify/results/BATCH2-AVAILABILITY.md`).

## 4. Documentation and tracking

- [x] 4.1 Update CODE_MAP, FEATURES, MISSING_FEATURES, AGENTS, domain notes; add ADR-038 (queue position identity) and ADR-039 (explicit release signing); amend ADR-005/ADR-012 (and the stale updater note in ADR-023).
- [x] 4.2 OpenSpec change recorded with proposal, specs (backup-restore, queue-persistence, release-signing), design and tasks.
- [x] 4.3 Run ktlint on every changed Kotlin/KTS file and the Debug lint gate; capture the repo-wide ktlint baseline (19 pre-existing violations outside this change) — `…\ssilencce-verify\ktlint-baseline.txt`.
- [x] 4.4 Skill-based review pass (security, standards/spec; accessibility/performance confirmed not applicable to this diff). Findings addressed: skipped/unselected entries are drained through the counting stream (critical; JVM regression probes + emulator log evidence), directory entries are skipped, entry classification requires a segment boundary, unknown JSON fields are tolerated on decode, backup output-stream open failures are reported through the logging/toasting catch-all, and resume-position recovery uses an occurrence mapping applied whenever the queue shrank (same-id duplicate gate case covered; Spec reviewer re-validated both the original counterexample and the gate case, 13/13 external probe cases pass). The optional `package*Release` guard-prefix hardening was deliberately skipped (the `releaseUnconfigured` validate-signing backstop already covers it and the prefix would gate lint helper tasks). The previously flagged shuffle-order discard is addressed by 2.5, and live missing-item removal now captures one timeline snapshot, revalidates identity/count on Main, and abandons stale passes before removal/publication (2.6); both are checked with the passing ktlint, assembleGithubDebug, lintGithubDebug and JVM/Python probe receipt recorded in section 2.

## 5. Restore verification follow-up

- [x] 5.1 Track ZIP entries, recognized entries and actually imported content; reject empty/non-ZIP archives; warn when no recognized or no selected content was restored; keep the success message only for a non-empty restore.
- [x] 5.2 Add strings for the new warnings in `values/strings.xml` only; preserve byte limits, cancellation and unsafe-name handling.
- [x] 5.3 Verify with an external JVM probe for the summary logic and emulator checks for empty, non-backup, unrelated-ZIP and valid archives. Evidence: summary probes `PROBE_OK 31 checks` (`ssilencce-restore-probe/probe-output.txt`) plus `PLAYLIST_PROBE_OK 20 checks` (`ssilencce-restore-probe/playlist-probe-output.txt`); gate-5 real-UI matrix **PASS 4/4** (empty ZIP, non-backup ZIP, unrelated non-ZIP, valid app-generated archive restoring the changed rotation-lock setting) with per-case UI/log/data evidence and five-file cleanup hashes in `followup-2026-10-21/runtime/verify/results/RESULTS.md`.
- [x] 5.4 Update this change's spec/tasks and the affected feature documentation.

## 6. Physical acceptance (open)

- [ ] 6.1 Physical-device Release frame-timing/battery profiling (implement-native-aurora-reference task 4.4) — blocked: only an emulator is connected; emulator evidence cannot satisfy physical acceptance. No acceptance claimed. Rechecked 2026-10-23: no physical USB device was present, only task-owned emulators; remains externally blocked (`runtime/verify/results/BATCH2-AVAILABILITY.md`).
