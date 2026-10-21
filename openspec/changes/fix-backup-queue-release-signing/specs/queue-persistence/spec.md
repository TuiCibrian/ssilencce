## Purpose

Keeps the playback queue, including repeated songs, intact across restarts by persisting one row per queue position and replacing persisted rows transactionally.

## ADDED Requirements

### Requirement: Queue rows are keyed by position

The persisted queue SHALL store exactly one row per queue position (its index), independent of the media id, and replace the saved queue transactionally so no stale rows survive.

#### Scenario: Repeated song saved
- **WHEN** the queue is A, B, A and playback state is saved
- **THEN** the persisted rows represent three positions with media ids A, B, A in order

### Requirement: Queue restores duplicates in order

Restoring persisted queue rows SHALL reproduce the same media item sequence, including repeated media ids, and the existing saved position, repeat and shuffle state SHALL continue to apply.

#### Scenario: Restart with duplicated entry
- **WHEN** the app restarts after saving a queue containing the same song twice
- **THEN** the queue shown and played matches the saved sequence, with both occurrences present

### Requirement: Existing databases migrate without data loss

Opening a database created by the previous schema version SHALL migrate to the new queue schema while preserving existing queue rows and their order.

#### Scenario: Migration from previous version
- **WHEN** a database containing queue rows from the previous schema is opened
- **THEN** the same rows and order are present after migration and startup is not disrupted

### Requirement: Saved shuffle order is compacted on filtered restore

When restore filters unavailable media items, the saved shuffle order SHALL be remapped through the saved-to-restored position map so surviving positions keep their relative shuffled sequence, and the payload SHALL be validated as a permutation. An undecodable or invalid payload SHALL disable shuffle for the restored session without emptying the queue; an undecodable payload and an invalid payload for a non-empty restored queue SHALL also clear the saved order. A missing saved order SHALL keep the unshuffled fallback. An empty restored queue SHALL preserve the previous behavior: an absent or empty valid order keeps shuffle enabled with a zero-length order, while an inconsistent non-empty order falls back safely.

#### Scenario: Missing track in shuffled queue
- **WHEN** a saved shuffled queue contains items A, B, C and the media for B is unavailable on restore
- **THEN** the restored queue contains A and C in saved queue order, the shuffle order is a valid permutation of the two restored positions, and A and C keep their relative shuffled sequence; playback resumes at the saved occurrence when it survived, or at its replacement with position-in-track reset when it did not

#### Scenario: Undecodable shuffle order disables shuffle
- **WHEN** the saved shuffle payload cannot be decoded
- **THEN** the persisted order is cleared, shuffle mode is disabled for the restored session, and the restored queue remains complete and playable

#### Scenario: Invalid shuffle order keeps queue
- **WHEN** the restored queue is non-empty and the saved shuffle payload is decodable but is not a valid permutation for the saved positions
- **THEN** the persisted order is cleared, shuffle mode is disabled for the restored session, and the restored queue remains complete and playable

#### Scenario: Empty restored queue preserves shuffle setting
- **WHEN** the restored queue is empty and the saved order is absent or an empty valid order
- **THEN** a zero-length order is installed and shuffle remains enabled

#### Scenario: Empty restored queue with inconsistent order
- **WHEN** the restored queue is empty and the saved order is non-empty
- **THEN** the order falls back to null safely and the player remains stable

### Requirement: Unavailable items are removed by timeline index

Live removal of unavailable media items SHALL use each item's timeline/window index, group consecutive timeline indices into ranges, and remove those ranges in descending order so that every unavailable occurrence is removed exactly once, including repeated media ids, regardless of shuffle mode. Before removing, the pass SHALL revalidate its timeline snapshot and SHALL abandon both the removals and the subsequent queue/position publication when the snapshot is stale.

#### Scenario: Missing track while shuffle is enabled
- **WHEN** a media item becomes unavailable while shuffle is enabled and its timeline index differs from its position in the shuffled queue list
- **THEN** exactly that timeline item is removed, no unrelated item is removed, and the queue and position flows reflect the remaining timeline

#### Scenario: Missing track while shuffle is disabled
- **WHEN** a media item becomes unavailable while shuffle is disabled
- **THEN** the same timeline-index removal behavior applies and the remaining queue order is unchanged

#### Scenario: Timeline changes during missing-item lookup
- **WHEN** the player timeline changes between the queue snapshot and the main-thread removal step
- **THEN** the pass removes nothing and does not publish its stale queue or position flows; the next timeline-event pass rebuilds from the new snapshot
