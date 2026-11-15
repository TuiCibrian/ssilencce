# Notification styles

## Why

The Media3 system notification only offers the platform layout. Users who want a compact
row, a large layout with a seek bar, or a big layout that also lists the next songs have no
option, and the notification always uses the system background. A custom Media3
`MediaNotification.Provider` can render RemoteViews layouts while keeping the existing
system style as the default choice.

## What Changes

- Add a notification style setting: system (Media3 default), small, big simple and big
  advanced. The system style remains the default so current behavior is unchanged.
- Implement a custom `MediaNotification.Provider` that renders RemoteViews for the three
  custom styles, with artwork, title/artist, transport buttons, a seek bar where the player
  supports seeking, and favorite/add-to-playlist actions.
- Big advanced lists the next three upcoming songs from the queue.
- Add an always-expanded option that uses the big layout for the collapsed notification too.
- Big layouts use the platform big content view, so they remain expandable on older
  Android versions.
- Add a configurable background: theme surface, cover-derived palette, or a custom color
  picked from a color sheet.
- Document that Android 13+ media controls may ignore custom RemoteViews and fall back to
  the system media style.
