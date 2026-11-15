#!/usr/bin/env python3
"""Independent mutagen/ffprobe checks for the WAV ID3v2 host probe outputs.

Version 2 adds a complete ReplayGain alias audit: standard, iTunes-prefixed, R128 and case
variants are all scanned, mapped to canonical fields and checked with their own units. A positive
output must contain exactly one standard alias per requested field and no R128/iTunes residue.
The ``--negative`` mode runs the same audit against a deliberately contaminated copy and requires
the audit to reject it.
"""

from __future__ import annotations

import argparse
import hashlib
import subprocess
from dataclasses import dataclass
from pathlib import Path

from mutagen.wave import WAVE

BASE = Path(__file__).resolve().parent
FIXTURES = BASE / "fixtures"
OUT = BASE / "out"
RESULTS: list[tuple[str, bool, str]] = []

STANDARD_KEYS = {
    "TXXX:REPLAYGAIN_TRACK_GAIN",
    "TXXX:REPLAYGAIN_TRACK_PEAK",
    "TXXX:REPLAYGAIN_ALBUM_GAIN",
    "TXXX:REPLAYGAIN_ALBUM_PEAK",
    "REPLAYGAIN_TRACK_GAIN",
    "REPLAYGAIN_TRACK_PEAK",
    "REPLAYGAIN_ALBUM_GAIN",
    "REPLAYGAIN_ALBUM_PEAK",
}

CANONICAL_FIELDS = {
    "REPLAYGAIN_TRACK_GAIN": "TRACK_GAIN",
    "R128_TRACK_GAIN": "TRACK_GAIN",
    "REPLAYGAIN_ALBUM_GAIN": "ALBUM_GAIN",
    "R128_ALBUM_GAIN": "ALBUM_GAIN",
    "REPLAYGAIN_TRACK_PEAK": "TRACK_PEAK",
    "REPLAYGAIN_ALBUM_PEAK": "ALBUM_PEAK",
}


@dataclass(frozen=True)
class AliasEntry:
    raw_key: str
    field: str
    values: tuple[str, ...]
    is_r128: bool


def check(name: str, condition: bool, detail: str = "") -> None:
    RESULTS.append((name, condition, detail))
    print(("PASS  " if condition else "FAIL  ") + name + (f": {detail}" if detail and not condition else ""))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def audio_md5(path: Path) -> str | None:
    result = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(path), "-map", "0:a:0", "-f", "md5", "-"],
        capture_output=True,
        text=True,
    )
    return result.stdout.strip() if result.returncode == 0 else None


def decode_ok(path: Path) -> bool:
    result = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(path), "-f", "null", "-"],
        capture_output=True,
        text=True,
    )
    return result.returncode == 0


def chunks(path: Path) -> list[tuple[str, int, int]]:
    data = path.read_bytes()
    result = []
    offset = 12
    while offset + 8 <= len(data):
        chunk_id = data[offset:offset + 4].decode("latin-1")
        size = int.from_bytes(data[offset + 4:offset + 8], "little")
        result.append((chunk_id, offset, size))
        offset += 8 + size + (size & 1)
    return result


def chunk_payload(path: Path, chunk_id: str) -> bytes:
    data = path.read_bytes()
    chunk = next(item for item in chunks(path) if item[0] == chunk_id)
    return data[chunk[1] + 8:chunk[1] + 8 + chunk[2]]


def riff_size_ok(path: Path) -> bool:
    data = path.read_bytes()
    return len(data) >= 12 and int.from_bytes(data[4:8], "little") == len(data) - 8


def is_rg_alias(key: str) -> bool:
    normalized = key.upper()
    return "REPLAYGAIN" in normalized or "R128" in normalized


def canonical_field(key: str) -> str | None:
    normalized = key.upper()
    if normalized.startswith("TXXX:"):
        normalized = normalized[5:]
    if normalized.startswith("----:COM.APPLE.ITUNES:"):
        normalized = normalized[len("----:COM.APPLE.ITUNES:"):]
    return CANONICAL_FIELDS.get(normalized)


def is_standard_key(key: str) -> bool:
    return key in STANDARD_KEYS


def parse_gain(raw: str, r128: bool) -> float | None:
    try:
        if r128:
            return int(raw.strip()) / 256.0 + 5.0
        cleaned = raw.strip()
        if cleaned.lower().endswith("db"):
            cleaned = cleaned[:-2].strip()
        return float(cleaned)
    except ValueError:
        return None


def parse_peak(raw: str) -> float | None:
    try:
        return float(raw.strip())
    except ValueError:
        return None


def alias_entries(path: Path) -> list[AliasEntry]:
    audio = WAVE(path)
    entries: list[AliasEntry] = []
    for key, value in (audio.tags or {}).items():
        raw = str(key)
        if not is_rg_alias(raw):
            continue
        field = canonical_field(raw)
        values = tuple(str(item) for item in (value.text if hasattr(value, "text") else [value]))
        entries.append(AliasEntry(raw, field or "UNKNOWN", values, "R128" in raw.upper()))
    return entries


def tags(path: Path) -> dict[str, list[str]]:
    audio = WAVE(path)
    output: dict[str, list[str]] = {}
    for key, value in (audio.tags or {}).items():
        if is_rg_alias(str(key)):
            continue
        output[str(key)] = [str(item) for item in (value.text if hasattr(value, "text") else [value])]
    return output


def rg(path: Path) -> dict[str, list[str]]:
    audio = WAVE(path)
    output: dict[str, list[str]] = {}
    for key, value in (audio.tags or {}).items():
        if is_rg_alias(str(key)):
            output[str(key)] = [str(item) for item in (value.text if hasattr(value, "text") else [value])]
    return output


def compare(
    label: str,
    original: Path,
    after: Path,
    expected: dict[str, str],
) -> None:
    after_entries = alias_entries(after)
    for field, expected_value in expected.items():
        canonical = canonical_field(f"TXXX:{field}")
        entries = [entry for entry in after_entries if entry.field == canonical]
        check(
            f"{label} {field} count",
            len(entries) == 1,
            f"entries={[(entry.raw_key, entry.values) for entry in entries]}",
        )
        if len(entries) == 1 and len(entries[0].values) == 1:
            entry = entries[0]
            if canonical in ("TRACK_GAIN", "ALBUM_GAIN") and entry.is_r128:
                expected_number = parse_gain(expected_value, False)
                actual_number = parse_gain(entry.values[0], True)
                value_ok = (
                    expected_number is not None
                    and actual_number is not None
                    and abs(expected_number - actual_number) <= 0.001
                )
            else:
                value_ok = entry.values[0].strip() == expected_value.strip()
            check(f"{label} {field} value", value_ok, f"actual={entry.values} expected={expected_value}")
        else:
            check(f"{label} {field} value", False, "missing or duplicate alias")

    actual_fields = {entry.field for entry in after_entries}
    expected_fields = {canonical_field(f"TXXX:{field}") for field in expected}
    check(
        f"{label} no unexpected RG keys",
        actual_fields == expected_fields,
        f"actual={actual_fields} expected={expected_fields}",
    )
    alias_keys = [entry.raw_key for entry in after_entries if not is_standard_key(entry.raw_key)]
    check(f"{label} no alias keys", not alias_keys, f"{alias_keys}")
    check(f"{label} non-RG metadata unchanged", tags(original) == tags(after), f"{tags(original)} != {tags(after)}")
    check(f"{label} audio MD5 unchanged", audio_md5(original) == audio_md5(after), f"{audio_md5(original)} != {audio_md5(after)}")
    check(f"{label} decodes", decode_ok(after))
    before_fmt = chunk_payload(original, "fmt ")
    before_data = chunk_payload(original, "data")
    after_fmt = chunk_payload(after, "fmt ")
    after_data = chunk_payload(after, "data")
    check(f"{label} fmt chunk unchanged", before_fmt == after_fmt)
    check(f"{label} data chunk unchanged", before_data == after_data)
    after_chunks = chunks(after)
    check(f"{label} one ID3 chunk", sum(1 for item in after_chunks if item[0].lower() == "id3 ") == 1)
    check(f"{label} RIFF size", riff_size_ok(after))


def run_negative(
    path: Path,
    original: Path,
) -> int:
    RESULTS.clear()
    compare(
        "negative",
        original,
        path,
        {"REPLAYGAIN_TRACK_GAIN": "2.68 dB", "REPLAYGAIN_TRACK_PEAK": "0.10000610"},
    )
    failures = [(name, detail) for name, ok, detail in RESULTS if not ok]
    if failures:
        print(f"NEGATIVE CONTROL PASS: verifier rejected {path}")
        for name, detail in failures:
            print(f"  {name}: {detail}")
        return 0
    print(f"NEGATIVE CONTROL FAIL: verifier accepted {path}")
    return 1


def main() -> int:
    global FIXTURES, OUT
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixtures", type=Path, default=FIXTURES)
    parser.add_argument("--out", type=Path, default=OUT)
    parser.add_argument("--negative", type=Path)
    parser.add_argument("--negative-original", type=Path)
    args = parser.parse_args()
    FIXTURES = args.fixtures
    OUT = args.out
    if args.negative:
        original = args.negative_original or (OUT / "no-id3" / "plain-wav.wav")
        return run_negative(args.negative, original)

    compare(
        "single",
        FIXTURES / "rg_wav_track.wav",
        OUT / "single" / "rg_wav_track.wav",
        {"REPLAYGAIN_TRACK_GAIN": "3.21 dB", "REPLAYGAIN_TRACK_PEAK": "0.12500000"},
    )
    compare(
        "no-id3",
        FIXTURES / "plain-wav.wav",
        OUT / "no-id3" / "plain-wav.wav",
        {"REPLAYGAIN_TRACK_GAIN": "2.68 dB", "REPLAYGAIN_TRACK_PEAK": "0.10000610"},
    )
    for index, track, peak in (("01", "2.68 dB", "0.10000610"), ("02", "7.99 dB", "0.05010986"), ("03", "-7.12 dB", "0.19952393")):
        compare(
            f"complete/{index}",
            FIXTURES / "wav_album" / f"{index}.wav",
            OUT / "complete" / f"{index}.wav",
            {
                "REPLAYGAIN_TRACK_GAIN": track,
                "REPLAYGAIN_TRACK_PEAK": peak,
                "REPLAYGAIN_ALBUM_GAIN": "-3.75 dB",
                "REPLAYGAIN_ALBUM_PEAK": "0.25000000",
            },
        )
    for index, track, peak in (("01", "3.21 dB", "0.12500000"), ("02", "3.21 dB", "0.12500000")):
        compare(
            f"partial/{index}",
            FIXTURES / "wav_partial" / f"{index}.wav",
            OUT / "partial" / f"{index}.wav",
            {"REPLAYGAIN_TRACK_GAIN": track, "REPLAYGAIN_TRACK_PEAK": peak},
        )
    check("partial third byte-identical", sha256(FIXTURES / "wav_partial" / "03.wav") == sha256(OUT / "partial" / "03.wav"))
    compare(
        "preserve/restore",
        FIXTURES / "rg_wav_track.wav",
        OUT / "preserve" / "rg_wav_track.wav",
        {
            "REPLAYGAIN_TRACK_GAIN": "3.21 dB",
            "REPLAYGAIN_TRACK_PEAK": "0.12500000",
            "REPLAYGAIN_ALBUM_GAIN": "-6.00 dB",
            "REPLAYGAIN_ALBUM_PEAK": "0.95000000",
        },
    )
    check(
        "positive single no R128 residual",
        not any("R128" in entry.raw_key.upper() for entry in alias_entries(OUT / "single" / "rg_wav_track.wav")),
    )
    # Mid-file ID3 chunk before fmt/data, after growth then shrink.
    mid_before = FIXTURES / "midfile-id3.wav"
    mid_after = OUT / "midfile" / "midfile-id3.wav"
    mid_rg = rg(mid_after)
    check("midfile track gain", mid_rg.get("TXXX:REPLAYGAIN_TRACK_GAIN") == ["3.21 dB"], f"{mid_rg}")
    check("midfile track peak", mid_rg.get("TXXX:REPLAYGAIN_TRACK_PEAK") == ["0.12500000"], f"{mid_rg}")
    check("midfile no album tags", not any("ALBUM" in key for key in mid_rg))
    check("midfile non-RG metadata", tags(mid_before) == tags(mid_after))
    check("midfile audio MD5", audio_md5(mid_before) == audio_md5(mid_after))
    check("midfile fmt payload", chunk_payload(mid_before, "fmt ") == chunk_payload(mid_after, "fmt "))
    check("midfile data payload", chunk_payload(mid_before, "data") == chunk_payload(mid_after, "data"))
    check("midfile one ID3 chunk", sum(1 for item in chunks(mid_after) if item[0].lower() == "id3 ") == 1)
    check("midfile RIFF size", riff_size_ok(mid_after))

    # iTunes/R128/case aliases are normalized to one standard TXXX per field.
    for name in ("alias-itunes.wav", "alias-r128.wav", "alias-case.wav", "duplicate-alias.wav"):
        compare(
            f"alias/{name}",
            FIXTURES / name,
            OUT / "alias" / name,
            {"REPLAYGAIN_TRACK_GAIN": "2.68 dB", "REPLAYGAIN_TRACK_PEAK": "0.10000610"},
        )
        alias_keys = {entry.raw_key for entry in alias_entries(OUT / "alias" / name)}
        check(
            f"alias/{name} no alias keys",
            all(is_standard_key(key) for key in alias_keys),
            f"{alias_keys}",
        )
        check(
            f"alias/{name} no R128 residual",
            not any("R128" in key.upper() for key in alias_keys),
            f"{alias_keys}",
        )

    # Unsupported variants must be byte-identical to their input.
    for name in (
        "multi-id3.wav",
        "flag-v23-compression.wav",
        "flag-v23-encryption.wav",
        "flag-v23-grouping.wav",
        "flag-v24-grouping.wav",
        "flag-v24-unsync.wav",
        "flag-v24-dli.wav",
        "tag-size-under.wav",
        "tag-size-over.wav",
        "odd-final-no-pad.wav",
        "trailing-garbage.wav",
        "huge-tag.wav",
        "many-chunks.wav",
    ):
        check(f"reject/{name} unchanged", sha256(FIXTURES / name) == sha256(OUT / "reject" / name))

    # Injected mid-write failure must restore the original bytes.
    midwrite_before = FIXTURES / "rg_wav_track.wav"
    midwrite_after = OUT / "midwrite" / "rg_wav_track.wav"
    check("midwrite restored", sha256(midwrite_before) == sha256(midwrite_after))
    check("midwrite RIFF size", riff_size_ok(midwrite_after))

    # Crash recovery from a leftover backup.
    crash_before = FIXTURES / "rg_wav_track.wav"
    crash_after = OUT / "crash-recovery" / "rg_wav_track.wav"
    check("crash recovery restored", sha256(crash_before) == sha256(crash_after))
    check("crash recovery RIFF size", riff_size_ok(crash_after))

    passed = sum(1 for _, condition, _ in RESULTS if condition)
    failed = len(RESULTS) - passed
    print(f"RESULT pass={passed} fail={failed}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
