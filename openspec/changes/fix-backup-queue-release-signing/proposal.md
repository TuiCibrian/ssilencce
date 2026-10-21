## Why

The backup/restore round-trip is broken in both directions: writing the lyrics entry closed the ZIP archive early (breaking every following entry), and restore rejected every streamed entry because it checked the unreliable declared ZIP size (always -1), while the lyrics JSON could not even be serialized because `LyricsEntity` had no `@Serializable` serializer. The playback queue silently collapsed repeated songs on restore because rows were keyed by media id, and the saved shuffle order was discarded whenever filtering unavailable items shrank the restored queue. Release builds silently shipped debug-signed when `keystore.properties` was absent.

## What Changes

- **Backup creation**: write the lyrics JSON as UTF-8 bytes without closing the ZIP archive; close each entry explicitly; wrap the whole creation in a cancellation-aware try/catch with `Log.e` on failure.
- **Backup restore**: stop checking declared ZIP sizes; enforce per-entry and total limits (10 MB lyrics, 100 MB single entry, 100 MB total, 1000 entries) against the bytes actually read via a counting stream view that never closes the delegate; reject unsafe entry file names; propagate coroutine cancellation instead of toasting it; log failures.
- **Restore verification (follow-up)**: distinguish an empty/non-backup archive, a valid archive with no recognized entries, and a valid archive where the selected content restored nothing; report an explicit warning instead of the success message in those cases while keeping the existing limits, cancellation and ZIP hardening.
- **Lyrics serialization**: annotate `LyricsEntity` with `@Serializable` so the lyrics cache can be encoded into and decoded from backups.
- **Queue persistence**: move `QueueEntity` primary key from the media id to the queue position (`order`) and replace rows transactionally (clear + insert) so queues like A, B, A round-trip; Room schema v6 → v7 migration preserves existing rows and their order.
- **Shuffle restore**: compact the saved shuffle order through the saved-to-restored position map when unavailable items are filtered and validate the payload as a permutation; an undecodable or invalid payload disables shuffle for the restored session without emptying the queue, undecodable and non-empty invalid payloads clear the saved order, a missing order keeps the unshuffled fallback, and an empty restored queue preserves the previous shuffle setting for absent or empty valid orders; map the resume index by occurrence and reset position-in-track when the saved current item is gone.
- **Missing-item removal**: remove unavailable media items from the live timeline by `QueueItem.second` timeline indices (consecutive ranges, descending), not by shuffle-list positions.
- **Release signing**: without `keystore.properties`, refuse `assemble*Release` / `bundle*Release` / `install*Release` tasks unless the explicit local opt-in `-PallowDebugSigning=true` (or `SSILENCCE_ALLOW_DEBUG_SIGNING=true`) is passed; a configured `storeFile` must exist; opt-in artifacts are debug-signed and marked NON-PRODUCTION. Debug builds, lint and IDE sync stay usable without a keystore.
- **Documentation**: update CODE_MAP, FEATURES, MISSING_FEATURES, AGENTS, domain notes; add ADR-038 (queue position identity) and ADR-039 (explicit release signing); amend ADR-005 and ADR-012.

## Capabilities

### New Capabilities

- `backup-restore`: ZIP backup creation and selective restore with real-byte limits, unsafe-name rejection, cancellation and honest failure reporting.
- `queue-persistence`: position-keyed queue persistence that preserves repeated songs and the saved shuffle order across restarts and filtered restores, with a safe Room migration and transactional replacement.
- `release-signing`: explicit release signing policy that refuses non-production release artifacts unless deliberately requested.

### Modified Capabilities

None.

## Impact

Affected code: `app/src/main/java/com/ssilencce/util/BackupHelper.kt`, `data/local/room/LyricsEntity.kt`, `data/local/room/QueueEntity.kt`, `data/local/room/QueueDao.kt`, `core/SsilencceDatabase.kt`, `MainModule.kt`, `playback/PersistentStorage.kt`, `playback/ImprovedShuffleOrder.kt`, `ui/screen/player/PlayerViewModel.kt`, `app/build.gradle.kts`; docs under `docs/` and `AGENTS.md`. No engine, repository API, network, dependency, version, signing-key, contributor or translated-string changes; no test source sets added.
