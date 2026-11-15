# ADR-046: Named-Queue Outgoing Save Before Switch

## Status

Accepted on 2026-10-21. Supersedes only the outgoing-save ordering decision in ADR-041 and
amends the named-queue interaction with the ordered writer in ADR-045.

## Context

ADR-041 defined a named-queue switch as a single service command that saves the queue being left,
resolves the target, applies the target timeline and suppresses saves from the old timeline. That
decision allowed the target binding and timeline to be applied before the outgoing snapshot write
completed. If the outgoing write then failed, the latest edits to the departing queue could be lost
with no user-visible retry path.

The follow-up explicitly requires switching away from a named queue not to lose departing edits
when persistence fails. ADR-045 already requires immutable snapshots to be captured on Main and
written through the application-owned ordered writer; that transport remains unchanged.

## Decision

- Resolve the target first. Then, on Main, capture the departing named queue and enqueue its
  transactional snapshot through `PersistentStorage` and `PlaybackStateWriter`.
- Await the outgoing save off Main. Do not change the active binding or player timeline while the
  write is pending.
- If the outgoing write fails, abort the switch with `ERROR_UNKNOWN` before arming the target guard
  or changing binding/timeline. The old queue remains active and its edits remain in memory and
  retriable.
- Re-check the captured active binding, timeline, resume index, repeat mode and shuffle state after
  the await. If the departing timeline changed, recapture and retry the save, bounded to three
  attempts. If it remains unstable, abort the switch without applying the target.
- Only after a stable, successful outgoing save: arm the expected-target guard, bind the target,
  replace the timeline and run repeat/shuffle/prepare/play. Once the target timeline is applied,
  mark it committed so a later setup failure still reports success and immediate target edits are
  attributed to the target rather than the departing queue.
- Keep the switch Mutex, cancellation propagation, main-thread capture and off-main persistence
  rules from ADR-041 and ADR-045.

## Consequences

- Outgoing persistence failure now fails the switch instead of silently accepting potential edit
  loss. The user can retry the switch after the underlying storage problem clears.
- A timed-out or unstable departing timeline can cause the switch to abort rather than block
  indefinitely; this is an explicit bounded-retry trade-off.
- A first snapshot may already have been persisted before a retry. The retry writes the latest
  snapshot; if it fails, the old timeline remains active and the normal save path can still persist
  later edits. The remaining non-atomicity is documented rather than hidden.
- Post-commit target setup failures keep the target binding/timeline and report success, preserving
  the ADR-041 committed-switch behavior.
