# Docs delta — open-intents

## Decisions

- External audio (`ACTION_VIEW` with `content`/`file` audio MIME types) and library previews
  play in a dedicated local ExoPlayer owned by `MiniPlayerViewModel`, not in the main
  `PlaybackService` queue. The main session is paused through a lightweight `MediaController`
  and resumed on close only when it was playing, the same media item is still current, and
  "Open in app" was not used. This keeps the queue untouched and avoids foreground-service
  semantics for a transient dialog; "Open in app" is the explicit hand-off to the main queue.
- `PlaybackService.isRunning` is an in-process flag used to avoid cold-starting the service
  (and its session-recovery path) when the popup has nothing to pause. There is no IPC.
- Audio MIME `VIEW` filters move from the `.activities.MainActivity` alias to
  `MiniPlayerActivity`; playlist/album/artist cursor filters, `http(s)` audio streams and all
  other intent filters remain on the alias. The popup finishes on `onStop` so a local player
  never runs without a foreground service.
- Resume/mute policies are fail-closed only when the user opts in; every new preference
  defaults to off and the existing "pause on zero volume" key is reused. The existing
  pause-on-zero resume threshold changes from `>= 0.1` to any non-zero step.
- "Only resume if paused by disconnect" tracks the pause cause; a manual/no-op pause clears
  it, and a playback start also clears it.
- "Prevent unwanted autoplay" rejects non-app `COMMAND_PLAY_PAUSE` requests for 5 seconds
  after a Bluetooth/headset connection (standard) or whenever no app activity is started
  (strict). The app UID and the media notification are always allowed. Media3 1.10.1
  deprecates `MediaSession.Callback.onPlayerCommandRequest`, but it is the only pre-execution
  reject hook; `onConnect` command filtering is static and cannot express the time/visibility
  policy, so the override is kept with a deprecation suppression.
- "Don't start playback when muted" gates every play request in
  `AdvancedForwardingPlayer`, defers it, and retries once the stream is unmuted. A play
  request that was already playing when the volume reached zero is paused and resumed by the
  existing pause-on-zero policy.
- Preview and external popup playback do not add queue/history entries and do not count as a
  play.

## docs/FEATURES.md

Add after the **Session recovery** bullet in section 1:

- **External-audio mini player** — Opening a `content:`/`file:` audio file from another app
  shows a Compose dialog-style player with play/pause, seek, close and **Open in app**. It plays
  in a local ExoPlayer so the main queue is not replaced; main playback is paused while it is
  open and restored on close unless Open in app hands the song to the main queue.
- **Song preview** — The song menu **Preview** action plays a song in the same popup without
  adding it to any queue. Main playback is paused and restored afterwards; preview playback is
  not counted in history or play statistics.
- **Resume policies** — **Only resume if paused by disconnect** prevents resume-on-connect
  after a manual pause. **Prevent unwanted autoplay** ignores play commands sent automatically
  by Bluetooth/car stacks for 5 seconds after a connection; its **Strict mode** ignores external
  play commands whenever the app is in the background.
- **Mute handling** — Pause on zero volume resumes at the first non-zero step, and **Don't
  start playback when muted** defers play requests until the media volume rises.

Replace the **Audio device integration** bullet with:

- **Audio device integration** (`AudioOutputObserver`) — Resume on headphone/Bluetooth connect,
  pause on disconnect, resume-only-if-paused-by-disconnect, unwanted-autoplay guard (standard
  and strict), pause at zero volume with resume at any non-zero step, don't-start-when-muted,
  system media output switcher (incl. MIUI/Samsung OEM intents), Xiaomi Mi Play detection and
  bit-perfect state.

## docs/MISSING_FEATURES.md

Remove these bullets from Part 1 §1:

- `- **Mute handling** — don't play when muted, pause when muted, resume when unmuted.`
- `- **Preview a song** without adding it to any queue.`
- `- **Resume policies** — "only resume if paused by disconnect", "prevent unwanted autoplay" (strict mode for buggy devices/cars).`

Replace Part 2 §2 ("Audio focus handling") with:

```markdown
## 2. Audio focus handling

- Musicolet: 4 levels — normal, ignore focus, ask before taking focus, resume after call.
- ssilencce: all four modes are wired through one controller (ADR-042, ADR-045), with a
  restorable request-bound Ask dialog; repeated play preserves consent, explicit/no-op pause
  and confirm/cancel/mode/release invalidate it. Strict autoplay policy for buggy car/device
  stacks is now implemented as an optional Bluetooth/car guard (standard or strict).
```

Replace Part 2 §3 ("Pause/resume on BT & USB") with:

```markdown
## 3. Pause/resume on BT & USB

- Musicolet: "only resume if paused by disconnect", "prevent unwanted autoplay" with strict
  mode for buggy devices/cars, resume on wired/BT connect.
- ssilencce: resume on connect, pause on disconnect, resume only if paused by disconnect,
  pause at zero volume with resume at any non-zero step, don't start playback when muted, and
  an unwanted-autoplay guard (5-second connect window or strict background blocking).
```

Remove the now-resolved "resume policies, strict mode" cross-reference line.

## docs/CODE_MAP.md

Add these entries next to the other `playback/` and `ui/screen/` entries:

- `playback/ResumeAutoplayPolicy.kt` — Connect, resume and external-autoplay policy state.
  - **Purpose:** Remember whether a disconnect caused the current pause and when the last audio route connected.
  - **Responsibilities:** Track `pausedByDisconnect`, evaluate "only resume if paused by disconnect", evaluate the 5-second standard autoplay guard and the strict background guard.
  - **Inputs:** `Preferences`, `AppVisibility`, elapsed realtime.
  - **Outputs:** `canResumeOnConnect()`, `shouldBlockExternalPlay()`, pause/start/connection notifications.
  - **Dependencies:** `util/Preferences.kt`, `util/AppVisibility.kt`, `android.os.SystemClock`.
  - **Consumed by:** `playback/PlaybackService.kt`.
  - **Notes:** Main-thread only; defaults keep all policies disabled.

- `ui/screen/miniplayer/MiniPlayerActivity.kt` — Dialog-style external-audio/preview player host.
  - **Purpose:** Host the Compose mini player in a floating activity and handle `ACTION_VIEW`/preview intents.
  - **Responsibilities:** Build `MiniPlayerRequest` from the intent, apply the app light/dark mode to `SsilencceTheme`, finish on outside touch/back/`onStop`, and launch `MainActivity` for "Open in app".
  - **Inputs:** `ACTION_VIEW` data URI or `MiniPlayerRequest.EXTRA_SONG`, app preferences.
  - **Outputs:** Compose screen, MainActivity `ACTION_VIEW` hand-off.
  - **Dependencies:** Compose, Koin, `MiniPlayerViewModel`, `MainActivity`, `Preferences`.
  - **Consumed by:** Android audio `VIEW` intents and `MiniPlayerActivity.previewIntent()` (song menu).
  - **Notes:** `excludeFromRecents`, `singleTop`, `Theme.Ssilencce.Dialog`; no foreground service.

- `ui/screen/miniplayer/MiniPlayerViewModel.kt` — Local ExoPlayer and main-session pause/restore.
  - **Purpose:** Play one external/preview track without touching the main queue.
  - **Responsibilities:** Resolve MediaStore/file URIs (fallback to `MediaMetadataRetriever`), own the local `ExoPlayer`, publish/seek/end state, gate start while muted, pause the main controller, restore it on close when safe, and stop on "Open in app".
  - **Inputs:** `MiniPlayerRequest`, `Repository`, app preferences.
  - **Outputs:** `StateFlow<MiniPlayerUiState>`, main-session pause/play and an open-in-app URI.
  - **Dependencies:** Media3 `ExoPlayer`/`MediaController`, `ProgressObserver`, `Repository`, `CoverProvider`.
  - **Consumed by:** `MiniPlayerActivity`, `MiniPlayerScreen`.
  - **Notes:** Uses `PlaybackService.isRunning` before connecting to the main session; no queue/history writes.

- `ui/screen/miniplayer/MiniPlayerScreen.kt` — Compose mini-player dialog content.
  - **Purpose:** Render play/pause, seek, artwork, mute feedback and "Open in app" for the popup.
  - **Responsibilities:** Loading/error/ready states, drag-safe slider, preview note, accessible labels.
  - **Inputs:** `MiniPlayerUiState` plus action callbacks.
  - **Outputs:** Compose UI.
  - **Dependencies:** Compose Material3, Coil `AsyncImage`.
  - **Consumed by:** `MiniPlayerActivity`.
  - **Notes:** All strings use `stringResource`; no `Context.getString` in composition.

- `ui/screen/miniplayer/MiniPlayerModels.kt` — Mini-player request/track/UI state models.
  - **Purpose:** Keep intent parsing and local media-item construction out of the Activity.
  - **Responsibilities:** `External`/`Preview`/`Unsupported` requests, `EXTRA_SONG`, `MiniPlayerTrack.toMediaItem()`, `MiniPlayerUiState`.
  - **Inputs:** `Intent`, `Song`, URI metadata.
  - **Outputs:** Media3 `MediaItem`, UI state.
  - **Dependencies:** Media3 common, `data/model/Song.kt`.
  - **Consumed by:** `MiniPlayerActivity`, `MiniPlayerViewModel`, `MiniPlayerScreen`.
  - **Notes:** None.

Update existing entries:
- `playback/PlaybackService.kt` — add `ResumeAutoplayPolicy`, `isRunning`, the muted play gate/deferred play, the external `COMMAND_PLAY_PAUSE` guard, the pause-cause tracking and the mute re-check on playback start to Responsibilities/Dependencies/Notes.
- `playback/AdvancedForwardingPlayer.kt` — add `canStartPlayback`/`onPlaybackBlocked` play gates.
- `core/audio/AudioOutputObserver.kt` — add `isMutedNow()`.
- `core/model/equalizer/SoundState.kt` — add `VolumeState.isMuted`.
- `util/Preferences.kt` — add `resume_only_if_paused_by_disconnect`, `prevent_unwanted_autoplay`, `prevent_unwanted_autoplay_strict`, `prevent_playback_when_muted`.
- `ui/component/menu/MenuItemClickExt.kt` — add the `action_preview` handler.
- `AndroidManifest.xml` — document the `MiniPlayerActivity` activity/filters and the audio `VIEW` filters moved off the MainActivity alias.

## AGENTS.md

In the `playback/` resilience line of the Repository Code Map, add:
`ResumeAutoplayPolicy` (resume-only-if-paused-by-disconnect, connect-window/strict external autoplay guard) and the muted play gate in `AdvancedForwardingPlayer`/`PlaybackService`.

In the `ui/screen/` section, add:
`miniplayer/` — Compose dialog-style external-audio/preview player backed by a local ExoPlayer and a paused/restored main session.

## docs/agents/domain.md

Bump the ADR count and add the new ADR reference. If ADR-052 is already used by another
lane, renumber the entry below to the next free number and update the count.

## ADR-053 — External-audio mini player and connect/mute policies

**Status:** Proposed

**Context:** Audio `VIEW` intents used to replace the current queue and open the full player.
There is no way to audition a song without queueing it, and Bluetooth/car stacks can start
playback unexpectedly or leave an inaudible player running at zero volume.

**Decision:** Add a dialog-style `MiniPlayerActivity` backed by a local ExoPlayer in
`MiniPlayerViewModel`. It pauses the main session through a `MediaController` and resumes it
on close when the same item is still current; "Open in app" hands the resolved URI to the
main app instead. Add `ResumeAutoplayPolicy` for "only resume if paused by disconnect" and a
standard/strict external autoplay guard, and gate play requests in
`AdvancedForwardingPlayer` while the media stream is muted.

**Consequences:** The main queue is preserved while the popup is open, and the popup does not
need a foreground service because it finishes on `onStop`. Connect policies need the pause
cause and a recent route-connection timestamp, which are process-local and reset on service
restart. The external command guard relies on a deprecated Media3 pre-execution callback; it
must be revisited if Media3 removes it.
