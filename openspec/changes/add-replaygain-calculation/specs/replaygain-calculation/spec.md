# ReplayGain calculation

## Purpose

Let users measure audio loudness offline and persist standard ReplayGain tags so unlabelled
libraries can use album/track normalization without leaving ssilencce.

## ADDED Requirements

### Requirement: Track measurement

The system SHALL measure integrated loudness and linear peak from decoded audio using ITU-R
BS.1770-4 / EBU R128 K-weighting, 400 ms blocks with a 100 ms hop, an absolute -70 LUFS gate
and a -10 LU relative gate. It SHALL derive track gain from the -18 LUFS ReplayGain 2.0
reference and SHALL clamp the result to a safe range. The decoder SHALL compare decoded frames
with the container's declared duration after subtracting any declared encoder delay/padding and
SHALL report a materially short or partial decode as a per-file failure.

#### Scenario: Normal track
- **WHEN** a track with measurable audio is scanned
- **THEN** a finite track gain and a linear track peak are produced

#### Scenario: Silence or short track
- **WHEN** a track is digital silence or shorter than one 400 ms block
- **THEN** the track is reported as not measurable and no unbounded or infinite gain is written

#### Scenario: Truncated decode
- **WHEN** a file decodes materially fewer frames than its declared duration or reports a
  partial frame
- **THEN** the file is reported as failed and no ReplayGain tags are written for it

#### Scenario: Truncated or corrupt decode
- **WHEN** a file decodes materially fewer frames than its declared duration or reports a partial
  audio frame
- **THEN** the file is reported as failed and no ReplayGain tags are written for it

#### Scenario: Cancellation
- **WHEN** the user cancels during analysis
- **THEN** decoding stops promptly and no album result is completed from a partial set

### Requirement: Album grouping

The scanner SHALL group selected songs by album. It SHALL compute album gain from the
combined gated block energy of every track in the album, not by averaging track gains, and
SHALL write album tags only when the complete album is selected and every track was measured.
A partial album selection SHALL produce track tags only.

#### Scenario: Complete album
- **WHEN** every track of an album is selected and measured
- **THEN** all tracks receive the same album gain and album peak

#### Scenario: Partial album
- **WHEN** only some tracks of an album are selected
- **THEN** those tracks receive track tags and no fabricated album tags

#### Scenario: One track fails
- **WHEN** one track of a complete album cannot be measured
- **THEN** album tags are not written for that album and the failure is reported

### Requirement: Tag persistence

The writer SHALL persist ReplayGain values in a format-aware way: standard `REPLAYGAIN_*`
tags for MP3/FLAC/OGG and RIFF WAV (ID3v2 `TXXX`), MP4/M4A
`----:com.apple.iTunes:REPLAYGAIN_*` freeform atoms (written through TagLib's standard property
names), and Opus `R128_TRACK_GAIN`/`R128_ALBUM_GAIN` for Opus. WAV SHALL use an explicit RIFF
`ID3 ` chunk reader/writer rather than relying on TagLib's WAV `savePropertyMap`, which can return
true without persisting in the app process. The writer SHALL replace calculated values rather
than append duplicates, preserve non-ReplayGain tags, and verify the saved values by reopening
the file with a fresh read descriptor. If the container's property map still cannot persist the
values, the write SHALL fail closed with an explicit not-persisted reason and SHALL NOT report
success.

#### Scenario: Standard format
- **WHEN** a FLAC, MP3, OGG or WAV track is written
- **THEN** its standard ReplayGain tags contain the measured values exactly once; WAV stores
  them as ID3v2 `TXXX` frames in the RIFF container

#### Scenario: WAV container
- **WHEN** a WAV track is written
- **THEN** the `fmt `/`data` chunks, audio payload and non-ReplayGain tags are unchanged, the RIFF
  size is updated, and the file has exactly one `ID3 ` chunk with exactly one
  `TXXX:REPLAYGAIN_*` frame per field

#### Scenario: WAV repeated replacement
- **WHEN** the same WAV is scanned again with new values
- **THEN** the old ReplayGain `TXXX` frames are replaced, not duplicated, and the audio payload is
  unchanged

#### Scenario: WAV complete and partial album
- **WHEN** a complete WAV album is selected
- **THEN** all tracks receive the same `ALBUM_GAIN`/`ALBUM_PEAK`; a partial selection writes only
  `TRACK_GAIN`/`TRACK_PEAK` and leaves unselected tracks untouched

#### Scenario: WAV mid-file ID3 chunk
- **WHEN** the WAV `ID3 ` chunk precedes `fmt `/`data`
- **THEN** growth or shrink shifts the following chunks, preserves `fmt `/`data` payloads and
  audio bytes, and updates the RIFF size

#### Scenario: WAV unsupported ID3 flags
- **WHEN** a WAV ID3 tag uses version-incompatible compression/encryption/grouping, v2.4
  unsynchronisation or a data-length indicator
- **THEN** the write fails closed and the original file is restored from the staged backup

#### Scenario: WAV write failure
- **WHEN** an I/O failure or injected mid-write failure occurs
- **THEN** the writer restores the original bytes from the staged backup and reports failure

#### Scenario: MP4
- **WHEN** an M4A track is written
- **THEN** the values are stored under the iTunes freeform ReplayGain keys
  (`----:com.apple.iTunes:REPLAYGAIN_*`) exactly once

#### Scenario: Opus
- **WHEN** an Opus track is written
- **THEN** its R128 gains are the Q7.8 representation of the measured gain

#### Scenario: Read-back failure
- **WHEN** TagLib saves but a fresh read does not contain the values
- **THEN** the file is reported as failed and the UI does not claim success

#### Scenario: Persistence unavailable
- **WHEN** the container's property map still cannot persist ReplayGain tags
- **THEN** the file is reported as failed with an explicit persistence reason, and no success is
  shown

### Requirement: Existing tags survive ordinary edits

Ordinary tag editing SHALL preserve canonical ReplayGain tags. The calculation path SHALL be
able to replace them explicitly.

#### Scenario: Edit title
- **WHEN** a user changes a title or cover on a file with ReplayGain tags
- **THEN** the ReplayGain tags remain present

#### Scenario: Explicit replacement
- **WHEN** a calculation writes new values for a file
- **THEN** the old ReplayGain values are replaced, not duplicated

### Requirement: Presence-aware playback

The ReplayGain model SHALL distinguish a missing value from a measured zero value. A present
0.00 dB tag SHALL use the with-tag preamp and SHALL remain visible in song details.

#### Scenario: Zero dB tag
- **WHEN** a file has a measured 0.00 dB track gain
- **THEN** Track mode applies the with-tag preamp and does not treat the tag as missing

#### Scenario: Missing tag
- **WHEN** a file has no track gain
- **THEN** the without-tag preamp remains in effect

### Requirement: User flow and permissions

The calculation flow SHALL be available from the song selection menu, SHALL show a
confirmation, progress, cancellation and a result summary, and SHALL request write access
before modifying files. The result SHALL list each failed song title and reason. It SHALL use
Compose and main English strings only.

#### Scenario: Permission denied
- **WHEN** the user denies write access
- **THEN** no file is modified and the UI reports the denial

#### Scenario: Successful scan
- **WHEN** the scan completes
- **THEN** the UI reports the number of updated and failed songs

#### Scenario: Failed songs
- **WHEN** one or more songs fail
- **THEN** the result lists each failed title and its failure reason

### Requirement: Tag aliases and malformed values

The reader SHALL resolve conflicting aliases as standard `REPLAYGAIN_*` over iTunes freeform over
Opus R128, using the same priority as the writer. A blank, malformed, non-numeric, NaN or infinite
gain/peak SHALL be treated as missing rather than as a measured 0 dB or 1.0 peak. A valid measured
`0.00 dB` or `0` peak SHALL remain present.

#### Scenario: Conflicting aliases
- **WHEN** a file carries standard, iTunes and R128 values for the same field
- **THEN** the standard value is used and the lower-priority aliases do not override it

#### Scenario: Malformed value
- **WHEN** a gain or peak tag is blank or cannot be parsed as a finite number
- **THEN** the field is treated as missing and the without-tag preamp path applies

#### Scenario: Measured zero
- **WHEN** a file carries `0.00 dB` or a zero peak
- **THEN** the value is present and is not treated as a missing tag

### Requirement: Album write failure is all-or-nothing

For a complete album, the writer SHALL capture the previous album tags before writing and SHALL
suppress new album gain/peak for the whole album if any track write fails. Already album-tagged
tracks SHALL be restored to their previous album state, and remaining tracks MAY receive track
tags only. A rollback failure SHALL be reported, not hidden.

#### Scenario: One track cannot be written
- **WHEN** a complete album is scanned but one track's tag write fails
- **THEN** no track is left with the newly computed album gain/peak and the failure is reported

#### Scenario: Snapshot unavailable
- **WHEN** the previous album tags cannot be captured for a complete album
- **THEN** album tags are withheld for that album and track tags still proceed

### Requirement: Cancellation reports actual progress

When a scan is cancelled after some work completed, the result SHALL remain `cancelled=true` and
SHALL preserve the completed/failed counts available at cancellation. It SHALL NOT report a
successful completion or reset the counts to zero.

#### Scenario: Cancel mid-album
- **WHEN** the user cancels after at least one track finished
- **THEN** the result shows cancelled with the last known completed/failed counts and no album
  tags from an incomplete write

### Requirement: Oversized peak still limits gain

Playback SHALL apply peak limiting for every finite positive peak, including peaks greater than
1.0; a non-finite or non-positive peak SHALL not constrain gain.

#### Scenario: Peak above one
- **WHEN** a tag contains a peak of 2.0 and a positive gain
- **THEN** playback limits the applied gain to about -6.02 dB instead of ignoring the peak

### Requirement: No new platform surface

The change SHALL NOT add a Gradle dependency, Room schema/migration, version change or
translated-string change. The scanner SHALL use the existing MediaStore-backed library and
TagLib reader/writer.

#### Scenario: Scope review
- **WHEN** the change is reviewed
- **THEN** no dependency, schema, version or translated-string change is present
