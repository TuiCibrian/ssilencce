# AA-05 targeted runtime retest — frozen candidate 5C1A8B91

Candidate: **5C1A8B9186BBABD601CF83BC10E5BE42226FDEA02F6FCE93EC2C6757D757A653**
Size: **53,507,336 B**
APK mtime: **2026-11-07T18:28:06.4779774-08:00**
Device: **emulator-5560** (qa30, API 30) only. 5554/5562 untouched.
No source edits; no 4DA runtime reuse as new-candidate evidence.

## Identity — PASS

- Installed/pulled base APK SHA-256: **exact match** `5C1A8B9186BBABD601CF83BC10E5BE42226FDEA02F6FCE93EC2C6757D757A653`.
- Pulled size: **53,507,336 B**.
- Test-only minimum duration set to 1 s; 21 MediaStore rows; app library showed **21 songs**.
- Position fixture: `AA05 Long Position` (30 s). MediaStore IDs vary by scan (215 in run A, 247 in run B).

## Test A — one-song queue before switch — FAIL

Before **Most played album artists**, playing:

| Observation | Before | After |
|---|---|---|
| Play state | `state=3` playing | `state=3` playing |
| Current item | `AA05 Long Position` | `AA05 B NoComposer Control 02` |
| Position | **12,160 ms** | **168 ms** |
| Queue | 1 item | 21 items |
| Queue unique | — | **true** |
| Long fixture present | yes | yes |

- Same current item: **FAIL**.
- Position preserved: **FAIL** (Δ −11,992 ms).
- Play/pause state preserved: **PASS**.
- Full unique queue: **PASS** (21 distinct IDs).
- No crash: **PASS** (`pid 4326`, no FATAL/ANR).

## Test B — full 21-song queue before switch — FAIL

Before **Most played album artists**, playing `AA05 Long Position` (index 20, position **12,168 ms**, `state=3`):

After: `AA05 B Composer Light 01` (index 9, position **0 ms**, `state=7` **Source error**).

- Same current item: **FAIL**.
- Position preserved: **FAIL**.
- Play/pause state preserved: **FAIL** (`3` → `7`).
- Full unique queue: **PASS** (21 distinct IDs; the long fixture remains in the post-switch set).
- No crash: **PASS** — process alive (`pid 3598`), no `FATAL EXCEPTION` / `ANR in`; the failure is a playback source error, not an app crash.

## Verdict

**Targeted AA-05 retest: FAIL.** The build/lint freeze is valid, but the frozen APK still loses the current item and position when switching special shuffle modes while playing; the full-queue run also ends in a playback source error. **Do not promote this APK as AA-05 runtime-verified.** No source edit or new build was performed in this retest.

## Evidence

`evidence-5c/`:
- `installed-base-5c.apk`, `installed-base-5c-full.apk` (hash-verified)
- Run A: `ms_before2.txt`, `ms_after2.txt`, `preservation_playing2.json`, `before2.png`, `after2.png`, `db/before2.db*`, `db/after2.db*`, `logcat2.txt`, `test2.log`
- Run B: `ms_before_full.txt`, `ms_after_full.txt`, `preservation_playing_full.json`, `before_full.png`, `after_full.png`, `db/before_full.db*`, `db/after_full.db*`, `logcat_full.txt`, `test_full.log`
- Fixtures/library: `mediastore.txt`, `home.xml`, `songs_full.xml`, `player_full.xml`, `sheet_full.xml`
