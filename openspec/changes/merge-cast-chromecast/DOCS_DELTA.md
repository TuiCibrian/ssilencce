# Documentation delta - merge cast-chromecast

Apply `openspec/changes/cast-chromecast/DOCS_DELTA.md` together with the integration
corrections below. The corrections describe the merged code; where both disagree, this file
wins.

## Merge-specific corrections

- **Single media session and crossfade:** the session is built on the flavor `sessionPlayer`
  (`CastPlayer` facade). The local crossfade controller remains attached to the local player,
  but `isCrossfadeAllowed` also requires `!isCasting`; entering a remote Cast session cancels
  an in-flight crossfade. After disconnect, local scheduling resumes normally.
- **Pause path:** `pausePlayback()` still disarms `ResumeAutoplayPolicy`/audio focus, cancels
  any crossfade and calls `activePlayer.pause()`. The sleep timer pauses the receiver directly
  while casting; local fade/stop behavior is unchanged.
- **Merged transition handler:** a local crossfade commit is still reported as an automatic
  transition. While casting, local persistence, remembered-position restore, A-B/per-song
  params and repeat-pause policy are skipped; history/scrobbling, widgets and media-button
  layout keep following the active player.
- **Mute/preview and resume gates:** the `canStartPlayback`/`onPlaybackBlocked` gate and the
  `resumeAutoplayPolicy` route-resume flow are preserved; Bluetooth/headset auto-resume calls
  `activePlayer.play()` through `resumeAfterConnect()`.
- **Icon:** only one `res/drawable/ic_cast_24dp.xml` exists (the checkpoint icon already used by
  EQ output routing); the toolbar and Aurora overflow reuse it. No duplicate resource.
- **Flavor separation:** `github`/`playstore` compile `src/cast` and declare `media3-cast`
  (`playback` and the Cast options manifest); `fdroid` compiles `src/nocast` and declares no
  Cast dependency or metadata. `CastPlaybackProvider` is defined only by the selected flavor's
  source set.
- **Versions:** no version code/name or media3 version change; `media3-cast` reuses the existing
  `media3` version.

## docs/agents/domain.md and ADR numbering

The crossfade merge already claims **ADR-052**. Create the Cast ADR as
`docs/architecture/ADR-053-flavor-scoped-chromecast.md` (text from
`cast-chromecast/DOCS_DELTA.md` unchanged) and update the domain count to
"53 ADRs, ADR-001 through ADR-053". Mention both ADR-052 (dual-player crossfade) and ADR-053
(flavor-scoped Cast) in the key-stack-decisions paragraph.

## Decisions - merge-specific corrections

1. **Additive conflict resolution:** crossfade and Cast wiring coexist in `PlaybackService`;
   neither the crossfade nor the Cast side was dropped.
2. **Cast owns playback while remote:** local crossfade is suppressed and cancelled, but the
   media session, queue, history/scrobbling, widgets and notification stay on the single
   `sessionPlayer`; disconnect restores the local path.
3. **Local-only DSP:** EQ/replay gain/mono/A-B/per-song tempo and fade/volume controllers stay
   on the local player, consistent with the worker's DOCS_DELTA.
4. **Icon deduplication:** the pre-existing cast icon wins because EQ routing already references
   it; it is a standard Material cast glyph valid for the toolbar.
5. **Untouched lanes:** no tests, commits/history, translated strings, project docs, metadata or
   files outside `_integration`.
