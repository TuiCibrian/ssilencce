#!/usr/bin/env python3
"""Create negative-control WAV copies with residual ReplayGain aliases.

The source must be a verified positive output (for example the `no-id3/plain-wav.wav` probe
output with 2.68 dB / 0.10000610). Each destination is a copy with one extra alias; the verifier
must reject every copy. The frozen production output is never modified.
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from mutagen.id3 import TXXX
from mutagen.wave import WAVE


def add_tag(
    source: Path,
    destination: Path,
    description: str,
    value: str,
) -> None:
    shutil.copy2(source, destination)
    audio = WAVE(destination)
    if audio.tags is None:
        audio.add_tags()
    audio.tags.add(TXXX(encoding=3, desc=description, text=[value]))
    audio.save()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)

    add_tag(args.source, args.out / "r128-matching.wav", "R128_TRACK_GAIN", "-594")
    add_tag(args.source, args.out / "r128-mismatch.wav", "R128_TRACK_GAIN", "-1123")
    add_tag(
        args.source,
        args.out / "itunes-duplicate.wav",
        "----:com.apple.iTunes:REPLAYGAIN_TRACK_GAIN",
        "2.68 dB",
    )
    add_tag(args.source, args.out / "r128-album-residual.wav", "R128_ALBUM_GAIN", "-594")
    print(f"negative controls written to {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
