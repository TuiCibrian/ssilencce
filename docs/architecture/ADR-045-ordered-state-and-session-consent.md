# ADR-045: Ordered Playback State and Session Consent

## Status

Accepted. Amends ADR-012, ADR-041 and ADR-042. Partially superseded by ADR-047 only for the
Settings-restore SharedPreferences cache/write-back contract; all ordered-state, session-consent
and same-UID command decisions below remain in force.

## Context

A queue edit can be followed immediately by pause or service destruction. Cancelling a
debounced task must not discard its playlist-save intent, and independent final writes must
not be overtaken by older writes. The exported media service also serves external controllers:
ordinary transport controls are public, but saved-queue switching and confirmation of an
in-app audio-focus prompt are not external contracts. A pending prompt must survive activity
recreation without granting stale consent.

## Decision

- Capture player values, media ids and the named-queue binding on Main. Keep playlist-save
  intent while debouncing; pause immediately enqueues any pending playlist snapshot.
- Send immutable snapshots through one application-owned `PlaybackStateWriter` channel.
  Normal saves, outgoing named-queue saves and the final service snapshot share this order.
  Use the existing transactional named/global replacement DAO methods. Resume-only saves
  update only resume metadata. No database write or blocking wait runs on Main.
- Before releasing the player, `PersistentStorage.finish()` enqueues a final full snapshot
  and ignores subsequent release callbacks. Restoration awaits a channel barrier before
  reading the saved queue, including after a fast service recreation in the same process.
- Advertise and dispatch restore, named-queue switch and audio-focus state/consent commands
  only for controllers with the app's UID. Keep standard media, Auto and Bluetooth controls
  and the existing public repeat/shuffle/favorite commands available externally.
- Intercept explicit play, pause and `setPlayWhenReady` intent in `AdvancedForwardingPlayer`.
  Repeated play preserves a pending Ask without rearming it; even a no-op explicit pause
  clears consent. Focus-driven internal pauses use the wrapped ExoPlayer directly.
- Give each Ask a unique request id. Confirm/cancel commands must match that live id.
  A no-argument Compose `DialogFragment` retains its request through recreation, and the
  activity reconciles it with authoritative session state on connection/resume. Lifecycle
  teardown does not count as cancellation. Focus-state events target same-UID controllers.

## Consequences

- Queue edits survive cancellation of service-owned debounce jobs; older in-flight writes
  cannot replace a newer final snapshot. This is service-lifecycle safety, not synchronous
  process-kill durability: Android can still kill the whole process before queued IO drains.
- Room and SharedPreferences remain separate stores; their commits are not one transaction.
- A stale/restored prompt cannot confirm another request, and a disconnected UI never implies
  consent. Public media transport remains usable without access to app-private commands.
