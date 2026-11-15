# Design: Album-artist and composer most-played strategies

## Context

The existing smart shuffle path is:

- `SpecialShuffleMode` enumerates the sheet entries and resource ids.
- `ShuffleModeBottomSheet` renders every enum entry and calls
  `PlayerViewModel.openSpecialShuffle(allSongs, mode)`.
- `ShuffleManager.applySmartShuffle()` expands songs with play stats, then applies the selected
  weight function.
- `OpenShuffleMode` controls shuffle-on-open behavior and is not involved in choosing a smart
  strategy; `PlaybackService` only transports the resulting queue/shuffle state.
- `ExpandedSong` copies `Song` fields but did not pass `composerName`, so the composer tag was lost
  before weighting.

## Goals / Non-Goals

**Goals:**

- Add album-artist and composer most-played strategies analogous to the existing artist/album modes.
- Keep every existing mode, enum order and weight formula unchanged.
- Use `Song.albumArtistName` and `Song.composerName`, with deterministic fallbacks.
- Keep the Compose shuffle sheet unchanged by relying on `SpecialShuffleMode.entries`.
- Keep the original special-shuffle replacement contract: start the new weighted queue at position
  0 and play.

**Non-Goals:**

- Preserving the current item, position or paused/playing intent across a special-shuffle switch.
- A configurable per-entity sort-rule editor.
- Genre/folder strategy changes.
- Room, dependency, version or translated-string changes.
- A Gradle build or runtime run in this source pass.

## Decisions

### New modes

`MostPlayedAlbumArtists` and `MostPlayedComposers` are inserted after `MostPlayedAlbums`. Existing
entries keep their relative order. Titles are "Most played album artists" and "Most played
composers"; descriptions mirror the existing "Favors songs from your…" wording.

### Group keys

`ShuffleGroupKeyPolicy.albumArtist()` trims the album-artist tag and falls back to the song artist
when it is blank, then to a shared unknown key. `composer()` trims the composer tag and uses the
same unknown key for missing/blank values. All songs remain in the shuffle.

### Weighting

Both modes sum the group's `ExpandedSong.playCount`, apply `ln(sum + 1.0)`, and use
`1.0 + groupWeight` as the per-song weight. This is the same formula and base weight as the
existing `MostPlayedArtists`/`MostPlayedAlbums` branches. The generic `weightedShuffle()` remains
the only ordering implementation.

### Composer platform gate

Composer tags are only exposed by the library on Android 11/API 30+, matching the Composers
category. `ShuffleModeAvailabilityPolicy` is a pure gate: the Compose sheet filters out composer-
dependent modes below API 30, while album-artist mode remains available. If a stale persisted
composer mode reaches the manager below API 30, it falls back to a plain shuffle instead of
silently producing a uniform composer bucket.

### Composer propagation

`ExpandedSong` gains an `override val composerName` parameter with a default of null and passes
`song.composerName` from its convenience constructor. Existing named-argument construction keeps
working, while the composer strategy now sees the real tag.

### Special-shuffle restart contract (2026-11-10)

The 2026-11-10 product decision selects the original weighted-shuffle behavior. `openSpecialShuffle()`
builds the weighted list, enables shuffle, requests the identity order with a plain
`SET_UNSHUFFLED_ORDER`, then installs the new timeline with `setMediaItems(mediaItems, true)`,
prepares it and calls `play()`. It does not capture or restore the current media item, position or
`playWhenReady`; a paused player also starts the new weighted queue at position 0 and plays. The
non-special `openShuffle()` path is unchanged.

The later 4DA/5C preservation requirement and its pending-order/post-order-seek implementation are
superseded. The historical 5C preservation FAIL remains a FAIL, not a PASS. The separate 5C
`state=7` Source error is retained as an independent playback failure and is not waived by the
contract decision.

## Risks / Trade-offs

- `MostPlayedArtists` already prefers `albumArtistName` when present, so tagged songs can receive
  similar weighting in both artist modes. Existing behavior is intentionally preserved; the new
  mode is explicit and uses a string fallback rather than `artistId` for untagged songs.
- Songs without album-artist/composer tags are grouped under one unknown key instead of being
  dropped.

## Verification

- Pure probe for `ShuffleGroupKeyPolicy` (trim, blank fallbacks, unknown composer group, key
  stability).
- Static source probe for enum resources, ShuffleManager branches and formula, ExpandedSong
  composer propagation, strings, docs/OpenSpec and scope limits.
- ktlint, `git diff --check` and `openspec validate add-advance-shuffle-strategies --strict`.
- No Gradle build in this pass.
