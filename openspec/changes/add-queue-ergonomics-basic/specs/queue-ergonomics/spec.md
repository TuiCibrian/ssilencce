# Basic queue ergonomics

## Purpose

Add safe swipe-right removal with undo, an explicit duplicate-name policy for saved queues, and
bounded named-queue count/name limits without changing playback-service behavior, queue search,
single-item drag, confirm-delete or the existing end-of-queue preference.

## ADDED Requirements

### Requirement: Swipe-right removal is guarded and reversible

The queue SHALL allow horizontal swipe-right removal only when the queue is unlocked, the queue
search query is blank and no reorder is active. Removal SHALL use the row's original queue index,
not a filtered position. A short Snackbar SHALL offer Undo; Undo SHALL reinsert the removed song at
the original logical position (or append if that position no longer exists). The existing
overflow-menu removal SHALL remain as the accessible fallback. Only one swipe removal SHALL be pending at a time; further swipes SHALL be ignored until the Undo Snackbar resolves. Left swipe and multi-item drag
SHALL NOT be added.

#### Scenario: Unlocked, no search
- **WHEN** the queue is unlocked, the query is blank and the user swipes a row right
- **THEN** that song is removed by its original queue index and a Snackbar with Undo appears

#### Scenario: Locked or searching
- **WHEN** the queue is locked, a search query is active or a drag is in progress
- **THEN** horizontal swipe removal is disabled

#### Scenario: One swipe at a time
- **WHEN** a swipe removal already has an active Undo Snackbar
- **THEN** additional swipe dismissals are ignored until that Snackbar resolves

#### Scenario: Undo
- **WHEN** the user taps Undo in the removal Snackbar
- **THEN** the song is inserted back at its original logical position, or at the end if the
  queue has since shortened past it

### Requirement: Duplicate names require an explicit choice

Saving or renaming a named queue to an existing case-insensitive name SHALL present Keep both
(default), Replace existing and Rename. Keep both SHALL use the existing suffix rule and report
the actual stored name. Replace SHALL require confirmation. On Save, Replace overwrites only the
selected named queue snapshot/resume state. On Rename, Replace deletes the selected existing
queue and renames the source queue to that name; if the deleted target held the active named-queue
marker it SHALL be cleared, while renaming the active source keeps its ID. Neither path SHALL
replace, clear or reorder the active playback queue. Rename SHALL return to validation. Merge SHALL NOT be added.

#### Scenario: Save duplicate name
- **WHEN** the user saves the current queue with a name that already exists
- **THEN** Keep both is offered by default, with Replace existing and Rename choices

#### Scenario: Keep both
- **WHEN** the user chooses Keep both
- **THEN** a new named queue is created with the existing suffix rule and the actual stored name
  is reported

#### Scenario: Replace existing
- **WHEN** the user chooses Replace existing and confirms
- **THEN** on Save only the chosen named queue snapshot/resume state is replaced; on Rename the
  selected duplicate is deleted and the current queue is renamed; the active playback queue is
  untouched and any active marker on the deleted duplicate is cleared

#### Scenario: Rename duplicate
- **WHEN** the user renames a queue to an existing name and chooses Keep both
- **THEN** the current queue receives the unique suffixed name and both queues remain

#### Scenario: Rename replace
- **WHEN** the user renames a queue to an existing name and chooses Replace existing
- **THEN** the selected existing queue is deleted, the source queue is renamed to its name, and
  the active named-queue marker is cleared only if the deleted target was active

### Requirement: Named-queue limits are explicit and enforced

The app SHALL enforce at most 100 named queues and at most 80 characters per name in the
repository and manager UI. Blank, too-long and duplicate/limit failures SHALL produce clear
errors. The app SHALL NOT silently auto-delete a queue. Replacing an existing queue SHALL not
count as creating a queue.

#### Scenario: Queue count cap
- **WHEN** 100 named queues exist and the user tries to create another
- **THEN** the save is rejected with a clear limit message and no queue is deleted automatically

#### Scenario: Name length
- **WHEN** the requested name exceeds 80 characters
- **THEN** the dialog shows an inline error and Save is disabled; the repository also rejects it

#### Scenario: Replace at cap
- **WHEN** the cap is reached and the user chooses Replace existing
- **THEN** the selected snapshot is replaced without changing the queue count

### Requirement: Existing queue behavior is preserved

Queue-local search and original-index mapping, single-item drag, named-queue switching, active
marker, confirm-before-delete, remove-others, clear-queue behavior and the existing
`clear_queue_on_completion` preference SHALL remain unchanged. End-of-queue chaining, re-shuffle,
jump-to-next/first and multi-item drag SHALL NOT be added by this change.

#### Scenario: Queue search
- **WHEN** the user filters the queue
- **THEN** existing filtering, index mapping and locate behavior are unchanged, and swipe remains
  disabled

#### Scenario: Scope check
- **WHEN** the change is reviewed
- **THEN** no Merge option, end-of-queue service command, multi-drag, schema, dependency,
  permission or translated-string change is present
