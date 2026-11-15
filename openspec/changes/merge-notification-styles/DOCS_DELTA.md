# Documentation delta - merge notification-styles

Apply `openspec/changes/notification-styles/DOCS_DELTA.md` together with the integration
corrections below. The corrections describe the merged code; where both disagree, this file
wins.

## Merge-specific corrections

- **Single session, logical player:** the notification providers stay attached to the one
  `MediaSession` (Cast facade included). The progress ticker uses `activePlayer`, so it follows
  local or receiver playback and stops when the active player pauses.
- **Progress lifecycle:** the ticker is scheduled only when
  `Preferences.notificationStyle.showsSeekBar` is true (Big Simple/Big Advanced) and the active
  player is playing; it is removed on pause, on style/provider changes and on service destroy.
- **Preference handlers:** `GHOST_MODE_ENABLED` still refreshes the notification and widgets,
  while `NOTIFICATION_STYLE` reinstalls the provider and the background/always-expanded keys
  refresh the notification. A-B scheduling remains in `onIsPlayingChanged`.
- **GhostMode masking:** custom layouts replace the title/artist with the hidden-track labels,
  omit artwork, palette extraction and up-next entries for ghost songs. The Media3 system style
  masks its title/artist through `SsilencceMediaNotificationProvider` overrides; its artwork
  continues to use the provider's Media3 bitmap pipeline.
- **Default:** `NotificationStyle.System` remains the default; no behavior changes until the
  user opts into a custom style.

## docs/CODE_MAP.md - additional note

Update `playback/PlaybackService.kt`:

- Notes: add "notification progress ticker follows the active (local or Cast) player and stops
  on pause/destroy; ghost and notification preference handlers coexist".

Update `playback/notification/CustomLayoutMediaNotificationProvider.kt`:

- Notes: add "ghost songs are masked (title/artist, artwork, palette, up-next); custom layouts
  reuse the single session".

Update `playback/notification/SsilencceMediaNotificationProvider.kt`:

- Responsibilities: add "ghost-song title/artist masking overrides".

## Decisions - merge-specific corrections

1. **Additive conflict resolution:** Cast/session/activePlayer properties, both notification
   providers and both preference handler groups were kept.
2. **A-B scheduling:** only one `scheduleAbRepeat()` call remains in `onIsPlayingChanged` (the
   checkpoint location); the patch's extra call was dropped as redundant.
3. **Ghost masking:** custom layouts get full masking; the system Media3 style gets
   title/artist masking and keeps Media3's own artwork loading.
4. **Untouched lanes:** no versions, tests, translated strings, project docs, commits/history or
   edits outside `_integration`.
