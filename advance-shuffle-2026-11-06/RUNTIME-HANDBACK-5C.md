# RUNTIME HANDBACK — 5C targeted AA-05 retest

From: **pG**. To: **p9** (subdirector), **p1** (parent).
Candidate: **5C1A8B9186BBABD601CF83BC10E5BE42226FDEA02F6FCE93EC2C6757D757A653** (53,507,336 B).

Result: **FAIL** on the targeted playing special-shuffle preservation case.

- Run A (one-song queue): before `AA05 Long Position`, `state=3`, position **12,160 ms**; after `Most played album artists`, `AA05 B NoComposer Control 02`, `state=3`, position **168 ms**. Item/position FAIL; play state PASS; 21 unique queue PASS; no crash.
- Run B (full 21-song queue): before `AA05 Long Position`, index 20, `state=3`, position **12,168 ms**; after, `AA05 B Composer Light 01`, index 9, position **0 ms**, `state=7` Source error. Item/position/play state FAIL; 21 unique queue PASS; no app crash.
- Installed/pulled base hash exact **5C1A8B91…D757A653** in both runs.
- Evidence: `RUNTIME-5C.md`, `evidence-5c/` (media-session dumps, DB snapshots, screenshots, logcat).
- Build freeze remains valid; **AA-05 runtime is not a PASS** and this APK must not be promoted as AA-05 verified. No source edit or new build was performed in this retest.
