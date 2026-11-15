# Design: Song-list title mode and date modified suffix

## Context

The read-only LIB-11 audit confirmed:

- `SongAdapter.getSongTitle()` chose `fileName` only for `SortKey.FileName`; smart lists with a
  null sort mode and album/artist detail lists had no filename choice.
- `SongAdapter.getSongText()` and the `SimpleSongAdapter` override own different secondary-text
  branches, so a base-only date change would miss album/artist detail.
- `Song` already exposes MediaStore-backed `dateAdded` and `rawDateModified`; date modified sorting
  already exists, so no schema or database field is needed.
- `MetadataField` is Now Playing-specific, performs metadata reads and has no date-modified field;
  it is not suitable for list binding.
- Compact `item_song.xml` and Home `item_image.xml` have no secondary text view. The minimal
  no-layout-change implementation can only append the date where `text` exists.
- `SearchAdapter` rendered SONG rows directly from `song.title`/`song.songInfo()`, bypassing the
  policy; a post-build spec review added it to the same title/date path.

## Goals / Non-Goals

**Goals:**

- Make title mode an explicit, persisted display choice independent of sorting.
- Preserve the current filename-on-File-name-sort behavior as the default.
- Show a localized date-only modification suffix in every song row that already has secondary text.
- Keep selection names and section popups consistent with the displayed title.

**Non-Goals:**

- A third text line or changes to compact/Home row layouts.
- A configurable multi-field list extra-info picker.
- Reusing `DISPLAY_EXTRA_INFO` (Now Playing only) for lists.
- Room, dependency, version or translated-string changes.
- A preference observer in every adapter.

## Decisions

### Three-state title mode

`FollowSort` preserves the old behavior exactly. `Title` always uses `song.title`; `FileName`
always uses `song.fileName`. The enum is stored by name in `song_title_mode`, and the
`ListPreference` entry values are the enum names, matching the existing enum preference helper.

### Date-only suffix

`DataExt.listDateModified()` formats `Song.dateModified` with
`DateFormat.getDateInstance(DateFormat.MEDIUM)` using the current resource locale. The adapter wraps
it with the main English `list_date_modified` string (`Date modified: %1$s`) and appends it to the
existing text through `SongListDisplayPolicy.appendDateModified()`. Only the existing secondary
text view is used.

### Shared adapter helper

`SongAdapter.appendListDateModified()` reads `Preferences.showListDateModified` at bind time. The
base adapter and the `SimpleSongAdapter` override both call it, so album/track/year detail text and
the general lists stay consistent. `getSongTitle()` resolves through the same policy for row titles,
selection names and popup text. The File name branch of the FastScroller popup still reflects the
File name sort key. `SearchAdapter` has no sort mode, so its SONG rows resolve the policy with a
null sort key and append the same date suffix through `SongListDisplayPolicy.appendDateModified()`.

### Settings and refresh

Both controls live in the existing XML Appearance > Display category. Existing lists may not
rebind merely by returning from Settings; a changed mode takes effect on the next bind or list
reload. Adding a listener to every adapter would be disproportionate for this bounded change.

## Risks / Trade-offs

- A date suffix makes the secondary line longer on narrow screens; it is opt-in and date-only.
- Rows without a secondary text view cannot show the date without a layout change, which is
  explicitly deferred.
- `FollowSort` keeps the legacy coupling until the user opts into an explicit mode.

## Verification

- Pure probe for `SongListDisplayPolicy` title modes and date append behavior.
- Static source probe for preference keys/defaults, adapter wiring, settings resources, docs,
  OpenSpec and absence of schema/dependency/version/layout edits.
- `ktlint`, `git diff --check` and `openspec validate add-list-display-info --strict`.
- No Gradle build in this pass; runtime verification is deferred until a build lease is granted.
