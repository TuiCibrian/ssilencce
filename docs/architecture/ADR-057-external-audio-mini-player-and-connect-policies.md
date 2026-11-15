# ADR-057: External-Audio Mini Player and Connect/Mute Policies

## Status

Accepted.

## Context

Audio `VIEW` intents used to replace the current queue and open the full player. There is no way
to audition a song without queueing it, and Bluetooth/car stacks can start playback unexpectedly
or leave an inaudible player running at zero volume.

## Decision

Add a dialog-style `MiniPlayerActivity` backed by a local ExoPlayer in `MiniPlayerViewModel`. It
pauses the main session through a `MediaController` and resumes it on close when the same item is
still current; "Open in app" hands the resolved URI to the main app instead. Add
`ResumeAutoplayPolicy` for "only resume if paused by disconnect" and a standard/strict external
autoplay guard, and gate play requests in `AdvancedForwardingPlayer` while the media stream is
muted.

## Consequences

- The main queue is preserved while the popup is open, and the popup does not need a foreground
  service because it finishes on `onStop`.
- Connect policies need the pause cause and a recent route-connection timestamp, which are
  process-local and reset on service restart.
- The external command guard relies on a deprecated Media3 pre-execution callback; it must be
  revisited if Media3 removes it.
- Preview and external popup playback do not add history entries and do not count as plays.
- Every new policy defaults to off, preserving existing behavior.

## Related

- ADR-011 (Media3 playback engine), ADR-042 (playback resilience), ADR-045 (session consent).
- `openspec/changes/open-intents/`.
