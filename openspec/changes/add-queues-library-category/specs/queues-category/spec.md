# Queues library category

## Purpose

Expose the active playback queue and the saved named queues as an opt-in top-level library
category outside Aurora, reusing the existing named-queue storage and ViewModel commands without
adding a new schema, repository or dependency.

## ADDED Requirements

### Requirement: Queues category is configurable and hidden by default

The system SHALL expose a `Queues` library category after the existing categories. The category
SHALL be appended invisible for existing installations through the current library-category merge,
SHALL remain hidden for a fresh default configuration because only the first five available
categories are visible, and SHALL preserve the stored order of categories that already exist.

#### Scenario: Existing installation upgrade
- **WHEN** an installation with an existing `libraryCategories` list starts with the new version
- **THEN** Queues is appended at the end with `visible = false` and the previous categories keep
  their order and visibility

#### Scenario: Fresh installation
- **WHEN** the app starts with the default category configuration
- **THEN** the first five categories remain visible and Queues is not shown

#### Scenario: User enables Queues
- **WHEN** the user enables Queues in the library category configuration and saves it
- **THEN** Queues appears in the non-Aurora library navigation and opens the Queues browser

### Requirement: Browser lists the playing queue and saved queues

The Queues browser SHALL show a playing-queue row with the current queue size and SHALL list every
saved named queue with its name and song count. It SHALL mark the active saved queue and SHALL show
an empty message when no named queues exist.

#### Scenario: Active saved queue
- **WHEN** a saved queue is the active named queue
- **THEN** its row shows the active marker and the playing-queue row opens the existing queue
  dialog

#### Scenario: Empty saved list
- **WHEN** no named queues are saved
- **THEN** the browser shows the empty message and still shows the playing-queue row

#### Scenario: Empty playing queue
- **WHEN** the current playback queue is empty
- **THEN** the playing-queue row cannot open the queue dialog and saving is disabled

### Requirement: Browser reuses existing named-queue commands

The browser SHALL save, rename, delete and switch named queues through the existing
`PlayerViewModel` and `NamedQueueRepository` path. Duplicate save/rename names SHALL use the
existing keep-both/suffix policy (`QueueNamePolicy.uniqueName`) and SHALL report the final stored
name; blank or over-long names SHALL fail without creating or renaming a queue. It SHALL NOT add a
Room entity, DAO, migration, repository implementation, permission or dependency. Switching a
saved queue SHALL use the existing atomic switch semantics.

#### Scenario: Save the current queue
- **WHEN** the playing queue is non-empty and the user confirms a valid name
- **THEN** the existing save command stores the snapshot and the saved list refreshes

#### Scenario: Rename or delete a saved queue
- **WHEN** the user confirms a rename or deletion
- **THEN** the existing command updates or removes that saved queue and the list refreshes

#### Scenario: Switch to a saved queue
- **WHEN** the user taps a saved queue
- **THEN** the browser disables the saved rows while the existing switch command runs and opens
  the queue dialog only when the switch succeeds; on failure the current queue is unchanged and
  the existing message is shown

#### Scenario: Duplicate name
- **WHEN** a save or rename uses a name that already exists
- **THEN** the existing keep-both policy stores a unique suffixed name and reports it as saved or
  renamed (`named_queue_saved_as`/`named_queue_renamed_as`)

#### Scenario: Invalid name
- **WHEN** a save or rename is blank or over-long
- **THEN** the existing validation message is shown and no saved queue is created or renamed

### Requirement: Queue dialog remains separate

The new `nav_queues` destination SHALL be a top-level fragment. The existing `nav_queue` SHALL
remain a dialog destination hosting `QueueFragment`; the browser SHALL navigate to it instead of
replacing or modifying it.

#### Scenario: Open the playing queue
- **WHEN** the user taps the playing-queue row
- **THEN** the existing `nav_queue` dialog opens with the current queue

#### Scenario: Existing queue entry points
- **WHEN** the mini-player or player opens the queue through `nav_queue`
- **THEN** the existing dialog and Aurora full-screen host behave as before

### Requirement: Aurora keeps its fixed Queue destination

The Aurora library drawer SHALL exclude the Queues category and SHALL keep its existing fixed
Queue destination. Aurora SHALL never restore or start on `nav_queues`, including a last-page
value saved by the classic UI; the fallback SHALL be the first visible non-Queues category (Home
if none). The non-Aurora navigation and the category configuration SHALL continue to offer Queues.

#### Scenario: Aurora drawer
- **WHEN** Aurora appearance is active and the library drawer is opened
- **THEN** Queues is not listed in the drawer, while the fixed Queue item remains available

#### Scenario: Aurora start and last-page restore
- **WHEN** Aurora is active and the persisted last page is `nav_queues`, or the first visible
  category is Queues
- **THEN** Aurora starts/resumes on the first visible non-Queues category (or Home), and never on
  `nav_queues`

#### Scenario: Non-Aurora navigation
- **WHEN** Aurora appearance is inactive and Queues is enabled
- **THEN** the Queues category is available in the normal library navigation
