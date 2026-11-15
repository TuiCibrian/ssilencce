# ADR-047: Live SharedPreferences Import for Settings Restore

## Status

Accepted on 2026-10-21. Amends ADR-043's backup/restore contract and ADR-045's
SharedPreferences-coherence consequence. Partially supersedes ADR-045 only for the handling of a
restored settings XML; the ordered-state, session-consent and same-UID command decisions in
ADR-045 remain in force.

## Context

The four-case restore matrix passed on disk: a valid app-generated backup restored Settings, the
file content survived a process restart, and invalid archives left durable data unchanged.
However, the running process kept the cached default `SharedPreferences` singleton while the
restore replaced `shared_prefs/<package>_preferences.xml` directly. The next preference edit could
therefore write the stale in-memory map back to disk and erase restored keys. Activity recreation
alone does not clear that process-wide cache.

The same cached instance is used by `MainModule`, `Preferences` and the Settings screen. Settings
restore is a deliberate user action, but it must not leave a live cache that is older than the
restored data. The restore path also accepts untrusted ZIP entries, so parsing must fail closed and
must finish before any preference mutation.

## Decision

- Settings restore is a live preference import, not a raw file replacement.
- Parse the complete untrusted preferences XML into a typed map before touching the live store.
  Accept only the standard SharedPreferences value types (string, int, long, float, boolean and
  string sets). Reject unknown tags, duplicate or empty names, malformed values, unknown set
  children, non-whitespace mid-map/trailing text, malformed XML and any plain DOCTYPE/DOCDECL/DTD
  before the root.
- Bound the import independently of the generic archive guard: 1 MiB settings entry, 1,000 keys,
  256 key characters, 64 KiB string/set items and 4,096 set members.
- Apply the whole map through the same default `SharedPreferences` instance used by the app:
  clear the editor, put every typed value, and commit synchronously on the IO dispatcher. Do not
  claim success unless the commit reports success.
- A malformed, unsupported or oversized settings entry returns failure without throwing: it is not
  counted as imported, the stream is drained under the generic guard, and later archive entries
  continue. Record that selected-entry failure separately. If other selected content imported, the
  result is `Partial` with an honest warning; zero imports retain the existing "nothing restored"
  result. Never show a plain success Toast when a selected entry was skipped. Archive-level limits
  still abort as before.
- On commit failure, attempt to restore the pre-restore snapshot and report failure. This rollback
  is best-effort; if the rollback commit also fails, no durable rollback is claimed.
- After a successful settings import, refresh the Settings UI by recreating the host activity. The
  live singleton is authoritative; a process restart is not required for correctness.
- Every directly affected `SharedPreferences.OnSharedPreferenceChangeListener` must guard its
  view/activity lifecycle before touching views. A whole-map commit can queue a listener callback
  before teardown; unregistering in `onDestroyView` cannot cancel a callback already posted, so the
  callback must no-op once its binding/view is gone. Normal change handling remains active while the
  view or activity exists; callbacks are not globally suppressed.
- Keep the existing archive entry, size, path-safety, cancellation and result-reporting behavior.
  A partial archive that imports Settings and later fails may leave the committed settings in
  place; the outer operation still reports failure and does not claim an all-or-nothing restore.
- Do not require byte-identical restored XML. Live-editor serialization may change the file bytes
  while preserving the typed whole-map semantics.

## Consequences

- A later Settings edit before process restart writes the restored map, not a stale one.
- Restored settings take effect in the same process, and the Settings screen is refreshed.
- Disk equality is semantic, not byte-for-byte; acceptance must compare typed values, live cache,
  unrelated-write retention and restart durability.
- A concurrent writer between the snapshot and commit can still be overwritten; restore is a
  deliberate whole-map operation and the window is small but not eliminated.
- Invalid or malicious preferences XML fails before mutation; plain DOCTYPE/DTD and external
  entity handling remain fail-closed. The parser behavior must be exercised on the built APK, not
  only in a host model; a cancellation racing into `commit()` can still commit, and a cancellation
  after commit can skip the immediate UI refresh while leaving the live map correct.
- Room and SharedPreferences remain separate stores; their commits are not one transaction, and a
  process death during the editor write is still limited by Android's SharedPreferences writer.
- A queued callback after view teardown is intentionally ignored by the guard; it is not a lost
  preference change, because the next view creation reads the live singleton.
- Partial results are explicit: a failed Settings entry no longer produces a success Toast merely
  because a later playlist or lyrics entry imported. Settings UI recreation remains gated on an
  actual settings import.
- Partial restores are explicitly reported rather than presented as full success; valid-only
  restores and existing zero-import outcomes keep their previous messages.
