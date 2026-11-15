# ADR-065: Custom Notification Styles

## Status

Accepted.

## Context

Media3's system style cannot show custom layouts, an upcoming-song list, a seek bar with custom
colors, or a cover-derived background. Musicolet offers small / big simple / big advance
notification styles. Android media notifications are platform-owned: custom RemoteViews are
honored on older versions, while Android 13+ may replace them with system media controls. The
existing system Media3 notification must remain the default so current behavior is unchanged.

## Decision

Add a persisted `NotificationStyle` (System / Small / BigSimple / BigAdvanced, default System).
For custom styles install `CustomLayoutMediaNotificationProvider`, a `MediaNotification.Provider`
that builds RemoteViews with `DecoratedMediaCustomViewStyle`, reuses the notification-plus
compact/expanded layout and session commands, and adds seek-bar, upcoming-song and
background-color behavior. Background colors come from the app theme, the cover palette, or a
stored custom ARGB color. Big styles set a custom big content view so they remain expandable on
older Android versions; always-expanded uses the big layout as the collapsed view. Progress
refreshes run on the main thread only while a big style is playing.

## Consequences

- The default notification path is unchanged. Custom styles depend on the platform honoring custom
  media layouts, so Android 13+ may silently fall back to the system media controls; the setting
  summary documents this.
- The custom provider must keep its actions in sync with available session commands and cache
  artwork/palette work off the main thread.
- Always-expanded layouts may be clamped by Android 11+ notification height limits.
- Ghost songs are masked in custom layouts (title, artist, artwork, palette and up-next), matching
  the system-style title/artist masking.
- The progress ticker follows the active local or Cast player and stops on pause, provider change
  or service destroy.

## Related

- ADR-030 (Glance widgets), ADR-053 (flavor-scoped Chromecast), ADR-060 (ghost mode),
  ADR-064 (custom media notification provider).
- `openspec/changes/notification-styles/`, `openspec/changes/merge-notification-styles/`.
