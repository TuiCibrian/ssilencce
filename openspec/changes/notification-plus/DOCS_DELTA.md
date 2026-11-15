# Docs delta — notification-plus

## Decisions

- The Media3 notification provider is subclassed (`SsilencceMediaNotificationProvider`) and
  `getMediaButtons`/`addNotificationActions` are overridden. Media3's default provider always
  inserts previous/play-pause/next and appends only extra "overflow" buttons, so a truly
  configurable arrangement (including removing previous/next) is impossible through
  `setMediaButtonPreferences` alone. Custom buttons send session commands
  (`TOGGLE_FAVORITE`, `TOGGLE_SHUFFLE`, `CYCLE_REPEAT`, `ADD_TO_PLAYLIST`, `CLOSE_NOTIFICATION`).
- The default button layout is exactly the previous notification: compact
  previous/play-pause/next; expanded previous/play-pause/next/repeat/shuffle. Favorite, FF, RW,
  close and add-to-playlist are opt-in from the layout editor, so defaults preserve current
  behavior. The favorite button is available on both compact and expanded lists.
- "Replace Close with Add to playlist" is a separate switch (default off). It swaps any Close
  entry for an Add-to-playlist action when the notification is built; Add-to-playlist can also be
  selected directly in the layout editor. Add-to-playlist brings the app forward and opens the
  existing `AddToPlaylistDialog` for the current song; close pauses playback and stops the
  service (the notification disappears).
- Up to 3 compact and 5 expanded buttons are allowed. The compact list is always kept a subset of
  the expanded list because the platform compact view indexes into the expanded actions.
- "Broadcast album art to system lock screen" defaults on. When off, artwork is removed from the
  media-item session metadata; the lock screen **and** the media notification lose the cover
  because both surfaces read the same Media3 session metadata. No custom RemoteViews are used, so
  system media controls are preserved. Re-enabling restores the cover URI for numeric song media
  ids. Metadata-only replacements use ExoPlayer's in-place `replaceMediaItems` update, so the
  current audio source is not reloaded.
- The notification channel name passed to the provider is now `playing_notification_name`
  instead of the description string. The channel ID is unchanged, so existing installs keep
  their channel; only fresh installs see the corrected name.
- **Upcoming songs are skipped.** `Notification.MediaStyle` and the Media3 `MediaNotification`
  API render only title/text/subtitle/artwork/actions; there is no supported upcoming-list slot,
  and custom RemoteViews are not allowed for media-style system notifications. A custom
  non-media notification would lose platform media controls, so it was not implemented.

## docs/FEATURES.md

Add under **9. UI, Themes & System**, after the **Widgets** bullet:

- **Media notification** — Custom Media3 notification provider with a configurable compact
  (up to 3) and expanded (up to 5) arrangement of previous, next, play/pause, favorite, FF, RW,
  close, shuffle and repeat; a stateful favorite heart; a Close/Add-to-playlist slot with a
  replace option; and a lock-screen artwork toggle that omits artwork from session metadata.
  Defaults match the previous notification (previous/play-pause/next plus repeat/shuffle).

## docs/MISSING_FEATURES.md

In Part 1 §12 remove:

- `- **Replace close button with "add to playlists"**.`
- `- **Favorite button** in the notification.`
- `- **Broadcast album art to system lock screen** (toggle).`

Replace:

- `- **Upcoming songs** in the expanded notification.`
- `- Configurable **compact/expanded button arrangement** and background color.`

with:

- **Upcoming songs** in the expanded notification — not implementable with the Android media
  style; the platform renders only title/text/artwork/actions.
- Notification **background color**; the compact/expanded button arrangement is implemented.

In Part 2 §15 replace the ssilencce line with:

```markdown
- ssilencce: custom Media3 notification provider with configurable compact/expanded buttons
  (previous, next, play/pause, favorite, FF, RW, close, shuffle, repeat), a favorite toggle,
  Close/Add-to-playlist replacement and an optional lock-screen artwork broadcast. Custom
  notification sizes/styles, always-expanded, notification background colors and upcoming
  songs remain missing; the system media style cannot render an upcoming list.
```

## docs/CODE_MAP.md

Add under `core/model/player/`:

- `core/model/player/NotificationButton.kt` — Notification button model and layout policy.
  - **Purpose:** Define the available notification actions and keep compact/expanded layouts valid.
  - **Responsibilities:** Stable stored values, default layout, parse/serialize, slot editing, subset/limit normalization.
  - **Inputs:** Preference strings from `Preferences`.
  - **Outputs:** `NotificationButtonLayout` for the provider and settings editor.
  - **Dependencies:** Android string resources only.
  - **Consumed by:** `SsilencceMediaNotificationProvider`, `NotificationButtonLayoutDialog`, `Preferences`.
  - **Related files:** `Preferences.kt`, `preferences_screen_playback.xml`.
  - **Notes:** Compact is capped at 3 and must be a subset of the expanded list (max 5).

Add under `playback/` (new `notification/` package):

- `playback/notification/SsilencceMediaNotificationProvider.kt` — Configurable Media3 notification provider.
  - **Purpose:** Render the user-selected notification actions instead of Media3's fixed transport order.
  - **Responsibilities:** Build command buttons (built-in and custom session commands), filter unavailable commands, assign compact-view indices, add actions to the notification.
  - **Inputs:** `NotificationButtonLayout`, favorite/seek state providers, `Player.Commands`.
  - **Outputs:** Media3 `CommandButton` list and compact-view indices.
  - **Dependencies:** Media3 session/notification APIs, `Playback` session commands, `RepeatPolicy`.
  - **Consumed by:** `PlaybackService.setMediaNotificationProvider`.
  - **Related files:** `NotificationButton.kt`, `PlaybackService.kt`.
  - **Notes:** Custom buttons send session commands, so `PlaybackService.onConnect` exposes them to in-process controllers.

- `playback/notification/NotificationArtworkPolicy.kt` — Lock-screen artwork toggle.
  - **Purpose:** Omit or restore artwork in the player's media-item session metadata.
  - **Responsibilities:** Strip `artworkUri`/`artworkData`, restore the song cover URI for numeric media ids, apply to item lists.
  - **Inputs:** `MediaItem`s and `Preferences.notificationBroadcastAlbumArt`.
  - **Outputs:** Updated `MediaItem` metadata.
  - **Dependencies:** Media3 `MediaItem`, `CoverProvider`.
  - **Consumed by:** `PlaybackService`, `PersistentStorage`.
  - **Related files:** `PlaybackService.kt`, `PersistentStorage.kt`.
  - **Notes:** Metadata-only updates use `replaceMediaItems` and do not reload the audio source.

Update existing entries:
- `playback/PlaybackService.kt` — custom notification provider, notification action refresh,
  favorite state, add-to-playlist/close commands, artwork policy on set/add/restore/transition.
- `playback/PersistentStorage.kt` — applies the artwork policy to restored queue items.
- `util/Preferences.kt` — notification layout, replace-close and album-art keys.
- `ui/screen/MainActivity.kt` — handles the add-to-playlist notification action.
- `ui/screen/settings/PreferencesScreenFragment.kt` — notification button editor and summary.
- `ui/component/preferences/dialog/NotificationButtonLayoutDialog.kt` (new) — Compose editor for
  the compact/expanded notification button slots, with reset to defaults.

## AGENTS.md

In the Repository Code Map, `core/model/` add `player/NotificationButton.kt` (notification
button layout model/policy); in `playback/` add `notification/` (custom Media3 notification
provider and lock-screen artwork policy); in `ui/screen/settings/` mention the notification
button editor dialog and preference rows.

## ADR — Custom media notification provider

**Status:** Proposed (assign the next free ADR number when applying; another change may already
claim ADR-052).

**Context:** Media3's `DefaultMediaNotificationProvider` always places previous/play-pause/next
first and only appends overflow buttons from media-button preferences. The product requires a
fully configurable compact/expanded layout including removing those transport buttons, a
stateful favorite action and close/add-to-playlist alternatives. The lock screen and notification
share the same session metadata, so the artwork toggle has to be applied to the player's media
items.

**Decision:** Subclass `DefaultMediaNotificationProvider`, override `getMediaButtons` and
`addNotificationActions`, and build buttons from a persisted `NotificationButtonLayout`. Send
non-player actions as session commands handled by `PlaybackService`. Apply the artwork preference
by rewriting media-item metadata through the player's in-place `replaceMediaItems` API at queue
load, restore and preference-change time.

**Consequences:** The notification surface no longer depends on Media3's default button ordering,
and the same layout also drives compact-view indices. The provider must keep its command
availability in sync with the session, and every new notification action needs a session command
handler. Hiding lock-screen artwork also hides notification artwork because both read the same
metadata; a future independent notification artwork path would require custom RemoteViews and is
out of scope.
