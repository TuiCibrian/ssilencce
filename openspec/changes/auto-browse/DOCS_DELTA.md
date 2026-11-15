# Docs delta - auto-browse

## Decisions

- The new Auto roots (folders, album artists, composers and current queue) are always
  added to the Auto library root, even when the matching phone library category is hidden.
  Auto is a separate browsing surface and a hidden phone tab should not remove a browsable
  type that this change is meant to expose. Existing roots, including playlists, still
  follow the visible-category preference, and duplicate roots are skipped by media id.
  Playlists were already browsable before this change, so they are not force-added.
- Folder browsing follows the existing `hierarchyFolderView` preference. Flat mode lists
  only folders that contain songs and shows their direct songs; hierarchical mode starts
  at the mounted storage volumes (falling back to the flat list when no volume is
  reported) and shows subfolders plus direct songs. Selecting a song queues that folder's
  direct songs, matching the visible list. Folder media ids embed a URL-safe Base64 folder
  path because media-id paths are colon-separated.
- Composers are exposed only on API 30+ because `MediaStore` composer data is unavailable
  below Android 11. Composer media ids use the synthetic stable composer id, not the raw
  name, so names containing the path separator cannot break the media-id path.
- Seek forward/rewind buttons are hidden while `isSeekControlsAvailable()` is false for the
  current track, matching the media notification. If an Auto controller still sends the
  seek command for a short track, it falls back to next/previous like the headset mapping.
- The current queue root is a snapshot of the playback timeline. Selecting an item looks
  the original media id up in the live queue so the whole queue plays from that position;
  if the item was removed meanwhile, the request is rejected instead of playing a wrong
  position.
- Auto button preferences use the existing `setMediaButtonPreferences` mechanism rather
  than a custom layout, so Android Auto and Auto companion controllers receive fast
  forward, rewind, favorite, shuffle and the existing repeat preference in the same
  surface. No new preference keys are required.

## docs/FEATURES.md

In **9. UI, Themes & System**, replace:

- `- **Android Auto** - Hierarchical media browsing via media IDs and Automotive app descriptor.`

with:

- **Android Auto** - Hierarchical media browsing via media IDs and the Automotive app
  descriptor, with roots for songs, albums, artists, album artists, composers (API 30+),
  genres, playlists, folders (flat or hierarchical) and the current playback queue. Auto
  media button preferences expose fast forward/rewind, favorite, shuffle and repeat;
  seek buttons follow the configured interval and short-track threshold.

## docs/MISSING_FEATURES.md

In **14. Android Auto**, replace:

- `- ssilencce: hierarchical browsing via media IDs and automotive descriptor.`

with:

- ssilencce: hierarchical browsing via media IDs and automotive descriptor, including
  folders (flat or hierarchical), album artists, composers (API 30+), playlists and the
  current playback queue; Auto buttons cover FF/Rewind, favorite, shuffle and repeat.
- Remaining gap: no exported album-art ContentProvider for external clients.

## docs/CODE_MAP.md

Replace the `playback/Playback.kt` opening line and notes with:

- `playback/Playback.kt` - Constants for custom MediaSession commands/events
  (shuffle/toggle/re-shuffle, seek forward/back, queue block move, repeat, favorite,
  playback restore, audio-focus ask/confirm/cancel, named-queue switch, stop position).
- **Notes:** Includes SET_UNSHUFFLED_ORDER, SET_STOP_POSITION, SWITCH_NAMED_QUEUE,
  RESHUFFLE_QUEUE, MOVE_QUEUE_BLOCK (+ from/to extras), SEEK_FORWARD/SEEK_BACK and
  token-bound CONFIRM/CANCEL/GET_AUDIO_FOCUS_STATE. Restore, named-queue switch,
  re-shuffle, block move and focus state/consent are advertised and dispatched only to the
  app UID (ADR-045).

Replace the `playback/library/LibraryProvider.kt` entry with:

- `playback/library/LibraryProvider.kt` - Resolves MediaItems for playback (including AAOS
  paths, folders, composers, the current queue and search results) via the repository.
- **Purpose:** Bridge repository data into MediaLibrary browsing/playback.
- **Responsibilities:** getMediaItemsForPlayback, getMediaItemsForAAOSPlayback,
  getChildren/getItem/search; folder flat/hierarchical listings; composer and current
  queue resolution; always-available Auto roots.
- **Inputs:** MediaItem lists, mediaId paths, search query, current queue snapshot,
  Repository.
- **Outputs:** Resolved/playable MediaItems, browsable children, searchResult.
- **Dependencies:** Repository, MediaIDs, MediaPathCodec, CoverProvider URIs,
  Preferences.
- **Consumed by:** PlaybackService.
- **Related files:** MediaIDs.kt, MediaPathCodec.kt, PlaybackService.kt.
- **Notes:** AAOS paths handle Android Auto controllers; internal SEARCH prefix. Folder
  ids embed URL-safe Base64 paths. Composers require API 30+.

Replace the `playback/library/MediaIDs.kt` entry with:

- `playback/library/MediaIDs.kt` - Constants/helpers for hierarchical media ID paths
  (ROOT, SONGS, ALBUMS, FOLDERS, COMPOSERS, CURRENT_QUEUE...).
- **Purpose:** Canonical media ID path scheme.
- **Responsibilities:** Category constants; getPathId/splitPath/isPath helpers with ":"
  separator.
- **Inputs:** parentId, mediaId (Long/String), path strings.
- **Outputs:** Path IDs and split segments.
- **Dependencies:** None.
- **Consumed by:** LibraryProvider, PlaybackService, MainActivity.
- **Related files:** LibraryProvider.kt, MediaPathCodec.kt.
- **Notes:** RECENT_SONGS uses "HISTORY"; LAST_ADDED/FAVORITES for shortcuts. FOLDERS,
  COMPOSERS and CURRENT_QUEUE are Auto browsing roots.

Add after the MediaIDs entry:

- `playback/library/MediaPathCodec.kt` - URL-safe Base64 codec for media-ID path
  components.
- **Purpose:** Embed arbitrary file paths and nested media ids in one colon-free
  media-ID component.
- **Responsibilities:** encode/decode UTF-8 strings with Base64 URL_SAFE, NO_WRAP and
  NO_PADDING; null on malformed input.
- **Inputs:** Raw path/media-id strings.
- **Outputs:** Encoded component or decoded string.
- **Dependencies:** android.util.Base64.
- **Consumed by:** LibraryProvider.
- **Related files:** MediaIDs.kt, LibraryProvider.kt.
- **Notes:** Media-ID paths are colon-separated, so raw Android paths cannot be embedded
  directly.

- `playback/library/AutoMediaButtons.kt` - Android Auto media button preferences.
- **Purpose:** Build the FF/Rewind, favorite and shuffle buttons shown by Auto
  controllers.
- **Responsibilities:** Stateful favorite heart; shuffle on/off icon; seek buttons omitted
  for tracks below the seek-controls threshold; custom session commands for every button.
- **Inputs:** Context, hasTimeline, seekControlsAvailable, favorite state, shuffle state.
- **Outputs:** Ordered CommandButton list.
- **Dependencies:** Media3 CommandButton/SessionCommand, Playback command constants,
  R.string.
- **Consumed by:** PlaybackService.
- **Related files:** Playback.kt, PlaybackService.kt, MediaIDs.kt.
- **Notes:** Repeat is appended by PlaybackService because it depends on the repeat
  policy.

In the `playback/PlaybackService.kt` notes, add:

- Auto controllers receive media button preferences built from `AutoMediaButtons` plus
  the repeat button; `refreshMediaButtonCustomLayout()` refreshes them on favorite,
  shuffle, repeat, play-state and media-item changes. `onGetChildren` and
  `onSetMediaItems` capture the current queue on the application thread before the IO
  lookup so the current-queue root never touches the player off-thread.

## docs/agents/domain.md

No change (no new ADR or doc file).

## AGENTS.md

No change to the Code Map summary is required beyond the existing `playback/library/`
summary.
