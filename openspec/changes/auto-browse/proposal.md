# Android Auto browsing and controls

## Why

The Media3 library exposed by `LibraryProvider` only surfaces songs, albums, artists,
genres, playlists and top tracks, and it only follows the user's visible library
categories. Android Auto therefore cannot browse folders, album artists separately,
composers or the current queue, which leaves a parity gap with Musicolet. The session
also lacks dedicated Auto controls for fast forward, rewind, favorite and shuffle.

## What Changes

- Add browsable roots for folders (flat and hierarchical), album artists, composers
  (API 30+), playlists and the current playback queue.
- Keep the existing songs/albums/artists/playlists/genres roots and their visibility
  behavior, but always expose the roots required by this change so Auto browsing is
  not hidden by a phone-only category preference.
- Resolve folder, composer and current-queue media IDs back to playable queue contents
  for Android Auto playback.
- Add Auto media button preferences for seek back/seek forward, favorite and shuffle,
  alongside the existing repeat/shuffle preferences, and keep them in sync with the
  favorite/shuffle/repeat state.
- Add custom session commands for seek back/forward so the buttons follow the
  configured seek intervals and short-track policy.
