# ADR-064: Custom Media Notification Provider

## Status

Accepted.

## Context

Media3's `DefaultMediaNotificationProvider` always places previous/play-pause/next first and
only appends overflow buttons from media-button preferences. The product requires a fully
configurable compact/expanded layout including removing those transport buttons, a stateful
favorite action and close/add-to-playlist alternatives. The lock screen and notification share the
same session metadata, so the artwork toggle has to be applied to the player's media items.

## Decision

Subclass `DefaultMediaNotificationProvider`, override `getMediaButtons` and
`addNotificationActions`, and build buttons from a persisted `NotificationButtonLayout`. Send
non-player actions as session commands handled by `PlaybackService`. Apply the artwork preference
by rewriting media-item metadata through the player's in-place `replaceMediaItems` API at queue
load, restore and preference-change time.

## Consequences

- The notification surface no longer depends on Media3's default button ordering, and the same
  layout also drives compact-view indices.
- The provider must keep its command availability in sync with the session, and every new
  notification action needs a session command handler.
- Hiding lock-screen artwork also hides notification artwork because both read the same metadata;
  a future independent notification artwork path would require custom RemoteViews and is out of
  scope.
- Upcoming songs are not supported by the media-style system notification. The compact and
  expanded defaults match the previous notification, so existing users see no change.
- The compact list is capped at three and always kept a subset of the expanded list (max five).

## Related

- ADR-028 (now-playing styles), ADR-045 (session commands), ADR-065 (custom notification styles).
- `openspec/changes/notification-plus/`.
