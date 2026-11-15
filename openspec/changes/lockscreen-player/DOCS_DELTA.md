# Documentation delta

## docs/FEATURES.md

Add at the end of section 7 (Now Playing Screen):

- **Lock screen player** (`Settings > Lock screen player`, disabled by default) — an opt-in
  full-screen Compose player shown over the system keyguard with `setShowWhenLocked` /
  `setTurnScreenOn`. It provides cover, title/artist/album, seek bar, shuffle/prev/play-pause/
  next/repeat, the playing queue (tap to jump) and lyrics (synced highlighting/seek-to-line or
  plain text) panels, a landscape cover/controls split, blurred-cover or user-picked wallpaper,
  and artist/folder/genre/Favorites shortcuts that open the matching library detail screen.
  Cover gestures reuse `PlayerGesturesController` (tap, double-tap edges/center, horizontal
  flings). An upward flick dismisses it. The screen-off trigger is a dynamic receiver registered
  by `PlaybackService` only while playback is active (playing or playWhenReady), so it costs
  nothing on an idle or paused queue. Options: skip when the device is unlocked (default on) and
  skip when paused for more than one minute (default on); "Preview lock screen player" bypasses
  both. A "Lock screen permissions" guide reuses the OEM detection from `PowerManagementGuide`
  and exposes overlay permission status, app notification settings, the battery guide and app
  details.

## docs/MISSING_FEATURES.md

Remove section 10 (Musicolet Lock Screen & screen saver):

- Delete the `## 10. Musicolet Lock Screen & screen saver` heading and its single bullet
  `- **Custom full-screen player over the system lock screen** ... (Entirely absent from
  ssilencce.)`; renumber the following sections (11 Quick access, 12 Notifications, ...) down by
  one.

Do not change `- **Set album art as lock-screen wallpaper** (own lock screen).` in section 9:
that is the system-wallpaper action, which remains absent.

## docs/CODE_MAP.md

Add:

- `app/src/main/java/com/ssilencce/core/model/lockscreen/LockScreenWallpaper.kt` — wallpaper
  enum (blurred cover / custom image).
- `app/src/main/java/com/ssilencce/ui/screen/lockscreen/LockScreenPlayerActivity.kt` — opt-in
  keyguard-overlay activity, showWhenLocked/turnScreenOn flags, preview intent, media-controller
  connection, favorite-playlist/genre resolution for shortcuts and USER_PRESENT dismissal.
- `app/src/main/java/com/ssilencce/ui/screen/lockscreen/LockScreenPlayerScreen.kt` — Compose lock
  screen UI: wallpaper, cover gestures, seek bar, transport/repeat/shuffle, queue and lyrics
  panels, shortcuts and upward-flick dismissal.
- `app/src/main/java/com/ssilencce/ui/screen/lockscreen/LockScreenTrigger.kt` —
  `LockScreenPlaybackClock` in-memory pause clock, `LockScreenLaunchPolicy` and the dynamic
  `LockScreenScreenOffReceiver`.
- `app/src/main/java/com/ssilencce/ui/screen/lockscreen/LockScreenEntityLauncher.kt` — intents
  that route artist/folder/genre/playlist detail requests to `MainActivity`.
- `app/src/main/java/com/ssilencce/ui/screen/settings/LockScreenPreferencesFragment.kt` —
  lock-screen settings screen (enable, preview, wallpaper picker, behavior switches, guide) and
  the OEM permission guide screen.
- `app/src/main/java/com/ssilencce/util/oem/LockScreenPermissionGuide.kt` — overlay/notification
  system intents, permission status and OEM-aware lock-screen steps.
- `app/src/main/res/xml/preferences_screen_lock_screen.xml` — lock screen player settings layout.
- `app/src/main/res/xml/preferences_screen_lock_screen_permissions.xml` — lock screen permission
  guide layout.

Update:

- `playback/PlaybackService.kt` — registers/unregisters the screen-off receiver only while
  playback is active (playWhenReady/isPlaying) and feeds `LockScreenPlaybackClock`.
- `ui/screen/MainActivity.kt` — handles `LockScreenEntityLauncher.ACTION_OPEN_ENTITY` and
  navigates to the artist/folder/genre/playlist detail destinations.
- `util/Preferences.kt` — new keys `lock_screen_player_enabled`, `lock_screen_wallpaper`,
  `lock_screen_wallpaper_uri`, `lock_screen_skip_when_unlocked`, `lock_screen_skip_when_paused`,
  `lock_screen_gestures`, `lock_screen_shortcuts`.
- `ui/screen/settings/SettingsScreen.kt`, `res/navigation/graph_settings.xml`,
  `res/xml/preferences.xml` — new Lock screen player settings destination and home entry.
- `app/src/main/AndroidManifest.xml`, `res/values/styles.xml` — `LockScreenPlayerActivity`
  (excludeFromRecents, showWhenLocked, turnScreenOn) and its black window background style.

## AGENTS.md

Update the Repository Code Map:

- `core/model/` — add "lockscreen/" (wallpaper enum).
- `playback/` — add "dynamic screen-off trigger (only while playback is active) feeding the
  lock screen player".
- `ui/screen/` — add `lockscreen/` (full-screen keyguard overlay activity, Compose player, queue
  and lyrics panels, wallpaper, cover gestures and entity shortcuts).
- `ui/screen/settings/` — add the lock-screen settings and permission-guide fragments.
- `util/oem/` — add `LockScreenPermissionGuide`.
- `res/` — add the two lock-screen preference XML screens and the lock-screen style/activity.

## docs/agents/domain.md

No change: no new ADR and the ADR count is unchanged. The feature follows ADR-006 (Koin), ADR-010
(pragmatic single-module layering), ADR-027 (Navigation safeargs) and ADR-036/ADR-037 (Aurora is
untouched; the new screen is a standalone Compose activity).

## ADR

No new architectural decision: a standalone Compose activity over the keyguard, a dynamic
receiver owned by the existing playback service, and the existing preference/navigation
infrastructure. The activity does not depend on MainActivity's fragment stack except for the
explicit entity-shortcut intents.

## Decisions

- `lock_screen_player_enabled` defaults to **false** and every trigger path checks it, so default
  behavior is unchanged.
- The screen-off receiver is registered while `player.playWhenReady || player.isPlaying`, which
  covers buffering and transient audio-focus pauses without staying registered after the user
  pauses. This keeps the "registered only while playing" battery rule while making the
  "paused > 1 min" option meaningful for transient pauses.
- `LockScreenPlaybackClock` tracks `playWhenReady` rather than `isPlaying`, so slow buffering is
  not mistaken for a user pause.
- "Skip when the device is unlocked" defaults to **true**; users without a secure lock can turn it
  off to see the player on screen-off.
- The preview action bypasses the enabled/unlocked/paused conditions (it is a settings preview,
  not a trigger), and the activity shows a preview badge.
- The playlist shortcut opens the **Favorites** playlist because the playback queue does not
  retain its source playlist; this is a real playlist and remains stable across queue changes.
- The custom wallpaper uses a persisted `OpenDocument` URI with `takePersistableUriPermission`;
  if it cannot be read, the blurred-cover background remains visible underneath.
- Landscape is supported by a cover/controls `Row`; rotation still follows the app's existing
  rotation-lock preference.
- API 26 uses the deprecated window flags for show-when-locked/turn-screen-on; API 27+ uses
  `setShowWhenLocked`/`setTurnScreenOn`.
- No Room schema change and no new dependency.
