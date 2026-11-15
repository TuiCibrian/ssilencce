# emulator-qa progress — FINAL

Updated: 2026-11-14 17:55 local
Status: **complete** — see `emulator-qa-report.md` and `bugs.md`.

## Device / build

- AVD `ssilencce-qa26`, API 26 x86_64, 540x960, software rendering.
- `com.ssilencce.debug` `1.4.0-beta.3.debug` (1400103).
- x86_64 APK SHA-256 `E5FC0E1B1F81F180F16E1463FC71E2AE56298A1E98432FCDB019E1331F48D01A`.
- Universal APK SHA-256 `72F502C80D7529902A3AAF6F64FA9DF7FF52076012227A5D868D29FD343C7EB0`.
- Existing data preserved; release app untouched; backups under `backup/`.

## Completed

- All 28 briefs covered by the matrix in `emulator-qa-report.md`
  (PASS / PARTIAL / BLOCKED / N-A API); no claim was made from compilation alone.
- Named gates: advance-shuffle 8.6 PASS; queue-multi-drag row-0 retest PASS;
  queues-category build/freeze PASS and category picker verified;
  ReplayGain feasible matrix PASS (album/partial/corrupt/zero/cancel) with
  read-only genuinely unavailable on API 26 FUSE; batch matrix PASS for
  MP3/FLAC/M4A/OGG/Opus, WAV honestly unsupported, WMA failed+restored;
  widget gate PARTIAL (providers, Card bind/config/empty state, playing controls
  with 48 dp labelled targets at 1.0 and 2.0 font scale; full 6.1–6.10 matrix
  not completed).
- One product bug filed: extra-format same-file duplicate indexing
  (`bugs.md` BUG-1).

## Host-blocked / unverified (with actions in the report)

- API 30/36-only positive branches (qemu APPCRASH on this host).
- Full widget 6.1–6.10 size/font/contrast matrix.
- Queues browser opening (category present; nav slots not reconfigured).
- ReplayGain write-failure with enforceable read-only storage.
- APE/DSF/DFF fixtures.
- Physical device, Cast receiver, external display, Android Auto, BT/OEM,
  release keystore.

No product code was edited during QA; only evidence, fixtures and OpenSpec
task ticks (advance-shuffle 8.6, queues 4.8, queue-multi-drag 5.15).
