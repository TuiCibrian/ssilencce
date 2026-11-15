# Headset button mapping

## Purpose

Make the existing headset single/double/triple press handler configurable, with a bounded
multi-press delay, optional 4+ Seek forward, and best-effort long-press mapping, while keeping
today's defaults and dedicated media keys unchanged.

## ADDED Requirements

### Requirement: Defaults preserve current behavior

Out of the box, the system SHALL behave like the current fixed handler: single press
Play/Pause, double press Next, triple press Previous, long press Play/Pause, 4+ off, and a
300 ms multi-press delay. Triple SHALL dispatch immediately when 4+ is off. A fourth press
after a dispatched triple SHALL start a new cycle.

#### Scenario: Default single, double and triple
- **WHEN** no headset mapping has been configured
- **THEN** one press toggles Play/Pause after 300 ms, two presses go Next after 300 ms, and three presses go Previous immediately

#### Scenario: Default fourth press
- **WHEN** 4+ is off and the user presses four times
- **THEN** the third press has already dispatched Previous and the fourth starts a new single cycle

#### Scenario: Default long press
- **WHEN** a held button reports key repeats while long press equals single press
- **THEN** the repeats are consumed and the pending single press still performs Play/Pause

### Requirement: Gestures use a safe transport-only action set

Single, double, triple and long press SHALL each be configurable to Play/Pause, Next,
Previous, Seek forward, Seek backward or Nothing. The persisted model SHALL be a transport-only
enum, not `NowPlayingAction`. Persisting invalid enum names SHALL fall back to defaults without
crashing. Nothing SHALL consume the event and perform no action.

#### Scenario: Remap a gesture
- **WHEN** the user selects Seek backward for double press
- **THEN** two presses perform one Seek backward by the existing Rewind/Fast-forward amount

#### Scenario: Disable a gesture
- **WHEN** a gesture is set to Nothing
- **THEN** the event is consumed, no playback action occurs, and the counter resets

### Requirement: Multi-press delay is bounded and persisted

The dialog SHALL offer 250, 300, 400, 500 and 600 ms. The default SHALL be 300 ms. Stored
values outside that set SHALL normalize to 300 ms. The setting SHALL use the existing default
SharedPreferences and code defaults for existing installs.

#### Scenario: Supported delay
- **WHEN** the user chooses 500 ms
- **THEN** single and double dispatch after 500 ms, and the persisted value is `"500"`

#### Scenario: Invalid delay
- **WHEN** a malformed or unsupported delay is stored
- **THEN** the getter returns 300 ms and the service does not crash

### Requirement: Optional 4+ Seek forward

A boolean `Four or more presses: Seek forward` SHALL be off by default. When off, triple SHALL
dispatch immediately and a fourth press SHALL start a new cycle. When on, triple SHALL wait for
the effective delay; a fourth press SHALL perform exactly one Seek forward and reset, and a
fifth press SHALL start a new cycle. Seek forward SHALL use the existing Rewind/Fast-forward
amount, not continuous fast-forward.

#### Scenario: 4+ enabled
- **WHEN** 4+ is on and the user presses four times
- **THEN** one Seek forward occurs and the count resets

#### Scenario: 4+ disabled
- **WHEN** 4+ is off and the user presses three times
- **THEN** Previous dispatches immediately and no Seek forward occurs

### Requirement: Long press is best-effort and configurable

Long press SHALL be detected from key repeats (`repeatCount > 0`). A repeat SHALL never count as
an extra press. When the long action differs from the single action, the effective dispatch
delay SHALL be at least 600 ms so the first repeat can cancel the pending click. When the long
action equals the single action, the configured delay SHALL be used and hold remains a single
press. If the headset/stack does not report repeats, the press SHALL fall back to the scheduled
single/double/triple action after the effective delay. A long-press action SHALL not fire a
second time for the same held button and SHALL not fire after an immediate triple dispatch.

#### Scenario: Distinct long press
- **WHEN** long press is Seek backward and a repeat arrives during the guard window
- **THEN** the pending click is canceled and Seek backward runs once

#### Scenario: No repeat reported
- **WHEN** the headset sends only a down/up with no repeat
- **THEN** the configured single/double/triple action runs after the effective delay

### Requirement: Service integration preserves dedicated keys and safe lifecycle

Only `KEYCODE_HEADSETHOOK` and `KEYCODE_MEDIA_PLAY_PAUSE` SHALL use the counter. Dedicated
MEDIA_NEXT, MEDIA_PREVIOUS, MEDIA_FAST_FORWARD and MEDIA_REWIND SHALL continue to fall through
to Media3 unchanged. Non-target keycodes SHALL call the superclass. A missing player SHALL be a
no-op. Playback-state changes SHALL NOT cancel a pending headset gesture. Preference changes and
service destruction SHALL cancel the pending callback and reset count/long-press state. The
handler SHALL never execute duplicate actions from one burst.

#### Scenario: Dedicated Next
- **WHEN** the headset sends KEYCODE_MEDIA_NEXT
- **THEN** Media3 handles it normally and the counted mapping does not intercept it

#### Scenario: Playback state changes mid-window
- **WHEN** playback pauses or starts while a double-press is pending
- **THEN** the pending gesture still dispatches

#### Scenario: Preference change mid-burst
- **WHEN** a headset preference changes before the pending click fires
- **THEN** the callback is removed, the counter/latch reset, and no stale action runs

### Requirement: Settings expose the mapping with compatibility disclosure

Playback → Headphones SHALL provide one row opening a Compose dialog. The dialog SHALL show the
four gesture rows, optional 4+ switch, delay chips, Reset to defaults, Close, and a disclaimer
that headset/Android/OEM/AVRCP behavior may ignore these mappings and that long press works
only when a held key is reported. Changes SHALL persist immediately; reopening SHALL show the
stored values. Reset SHALL restore the defaults.

#### Scenario: Reopen settings
- **WHEN** the user changes double press to Seek forward and reopens the dialog
- **THEN** Seek forward is shown as selected

#### Scenario: Reset
- **WHEN** the user activates Reset to defaults
- **THEN** single/double/triple/long return to Play/Pause, Next, Previous, Play/Pause; 4+ is off and delay is 300 ms

### Requirement: No destructive scope expansion

The change SHALL NOT add a dependency, schema/migration, permission, receiver, service,
translated string or version change. It SHALL NOT remap dedicated Next/Previous to
FF/Rewind, add continuous fast-forward, or use `NowPlayingAction` as the persisted headset
model.

#### Scenario: Dedicated-key override remains unsupported
- **WHEN** the user expects a dedicated Next button to become Seek forward
- **THEN** the mapping does not change that key and the documentation states the remaining gap
