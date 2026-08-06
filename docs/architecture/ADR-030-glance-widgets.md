# ADR-030: Home-Screen Widgets with Glance and Serialized Playback State

## Status

Accepted

## Context

Three home-screen widgets (responsive, card, full) must show the current track, artwork and
transport controls. Widgets are rendered by a separate process without access to the playback
service, so they need a snapshot of playback state plus a theme, persisted somewhere both
processes can read, and a way to trigger service actions.

## Decision

- Build the widgets with Glance 1.1.1 (`glance-appwidget`, `glance-appwidget-preview`,
  `glance-material3`) as three `GlanceAppWidget`s, all using `SizeMode.Exact`:
  - `SsilencceGlanceWidget`: responsive widget with 5 layouts (Classic, Simplified, Medium,
    Large, ExtraLarge),
  - `CardWidget`: framed cover + bottom controls bar,
  - `FullWidget`: cover with gradient scrim, progress and all controls
    (`showAllActions` when width ≥ 220 dp).
- Persist the snapshot with `PlaybackStateDefinition`, a custom `GlanceStateDefinition` that
  stores a JSON-serializable `PlaybackState` (title/artist/info/artwork bytes/state flags/
  repeat mode) in a typed DataStore file (`playback_state_v1.json`), written by
  `PlaybackService.buildPlaybackState` on playback changes and read leniently by the widgets.
- Share rendering helpers in `WidgetComponent` (album art decode + placeholder, control
  icons) and `WidgetExtension` (Dp→px, `WidgetTheme` from dynamic color schemes — ADR-029,
  and service intents).
- Route all widget interactions to `PlaybackService` via explicit intents/actions
  (play/pause, next/prev, shuffle toggle, repeat cycle, favorite toggle); taps on the widget
  open `MainActivity`.
- Register receivers in the manifest (`SsilencceGlanceWidgetReceiver`, `CardWidgetReceiver`,
  `FullWidgetReceiver`) with per-widget `widget_info` XML.

## Consequences

- Widget rendering never touches the playback process: cheap, crash-isolated updates.
- The snapshot DataStore is a second persistence of playback state next to
  `PersistentStorage` (ADR-012) — a deliberate, documented duplication limited to
  widget-consumable fields.
- Artwork is embedded as bytes in the snapshot (1024 px cap), trading a little file size for
  offline widget reliability.
- `SizeMode.Exact` gives responsive layouts but triggers full updates on size changes;
  layout selection is per size bucket.
