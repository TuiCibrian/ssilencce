# Lyrics tools

## Why

The lyrics screen is read-only apart from the plain-text editor. Users cannot easily quote or
share a few lines, cannot override which lyrics source is used for an individual song without
editing or deleting lyrics, and cannot create synchronized (timestamped) LRC lyrics in the app
when a song only has plain text.

## What Changes

1. **Line selection on the full lyrics screen.** Long-press a lyric line to enter a selection
   mode; tap more lines to toggle them, then Copy (clipboard) or Share (ACTION_SEND text). Works
   for both synced and plain lyrics; selection is reset when the song changes.
2. **Per-song lyrics source choice.** A lyrics-source action on the full lyrics screen lets the
   user keep the automatic priority, force embedded lyrics, force downloaded lyrics, or pick an
   `.lrc`/`.ttml` document through SAF. The choice is persisted per song id in a small JSON file
   in app storage (no Room schema change) and applied when resolving lyrics. Picked documents keep
   a persisted read permission.
3. **In-app synchronized LRC creation.** The lyrics editor gains Edit/Sync modes. Sync mode shows
   the lyric lines, plays the current song, assigns the current playback position to the target
   line when the user taps "Stamp", and adjusts each stamped line by ±100 ms. The result can be
   saved as embedded synced lyrics (existing metadata writer path) or exported as an `.lrc` file
   next to the audio file (direct write when allowed, SAF `CreateDocument` otherwise; a SAF export
   also becomes the song's chosen lyrics file).
