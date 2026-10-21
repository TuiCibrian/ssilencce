# ADR-038: Queue Identity by Position

## Status

Accepted on 2026-10-16. Refines the storage mechanism of ADR-012 (playback queue persistence and restoration).

## Context

The persisted queue (`QueueEntity`) used the media id as its primary key. Two consequences followed:

- A queue containing the same song more than once (for example A, B, A) could not be saved faithfully: rows with the same media id collided and the duplicate silently collapsed on save. After a restart the restored queue differed from what the user had built.
- `QueueDao.replaceQueue` combined a save with a `removeNotIn` prune, which only worked while row identity was the media id.

The queue is fundamentally an ordered list of positions, not a set of songs; identity must therefore be the position in the queue.

## Decision

- `QueueEntity` is keyed by `order` (queue position) as its `@PrimaryKey`; `id` (mediaId) becomes a plain column. The same media id may appear on several rows.
- `QueueDao.replaceQueue` becomes a true replace-all inside a single `@Transaction`: `clearQueue()` followed by `saveQueue()`. The `removeNotIn` prune is removed, since a full rewrite cannot leave stale rows behind.
- Room schema version moves 6→7 with `MIGRATION_6_7`: create `QueueEntity_new` with `PRIMARY KEY(\`order\`)`, copy `INSERT OR REPLACE INTO QueueEntity_new (id, order) SELECT id, order FROM QueueEntity ORDER BY \`order\``, drop the old table and rename. Existing rows and their order are preserved. The migration is registered in `MainModule.kt` alongside the earlier migrations.
- Ordered reads keep using `ORDER BY \`order\``.

## Consequences

- Duplicated songs (A, B, A) now round-trip through save/restore unchanged; queue positions are preserved after restart.
- Every save rewrites the whole table (still debounced at 500 ms by `PersistentStorage`). The previous collapse/prune behaviour is gone; the rewrite is simpler and always consistent with the live queue.
- Anything that implicitly assumed one row per media id (for example mapping a song id back to a saved row) must account for duplicates.
- The migration is in place and lossless for existing installs; no data is dropped from the persisted queue.
