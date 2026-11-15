#!/usr/bin/env python3
"""Generate edge-case WAV/ID3 fixtures for the WAV codec host probe."""

from __future__ import annotations

import argparse
import struct
from pathlib import Path

BASE = Path(__file__).resolve().parent
DEFAULT_SOURCE = Path(
    r"C:\Users\geode\AppData\Local\Temp\opencode\ssilencce-verify\replaygain-validation-2026-11-06\fixtures"
    r"\sine_1k_m20_stereo_10s_48k.wav"
)
FIXTURES = BASE / "fixtures"


def chunk(chunk_id: bytes, payload: bytes) -> bytes:
    return chunk_id + struct.pack("<I", len(payload)) + payload + (b"\x00" if len(payload) & 1 else b"")


def chunk_no_pad(chunk_id: bytes, payload: bytes) -> bytes:
    return chunk_id + struct.pack("<I", len(payload)) + payload


def syncsafe(value: int) -> bytes:
    return bytes(((value >> 21) & 0x7F, (value >> 14) & 0x7F, (value >> 7) & 0x7F, value & 0x7F))


def frame(frame_id: bytes, payload: bytes, flags: int = 0, version: int = 4) -> bytes:
    size = syncsafe(len(payload)) if version == 4 else struct.pack(">I", len(payload))
    return frame_id + size + struct.pack(">H", flags) + payload


def id3_tag(frames: list[bytes], version: int = 4, tag_flags: int = 0) -> bytes:
    body = b"".join(frames)
    return b"ID3" + bytes((version, 0, tag_flags)) + syncsafe(len(body)) + body


def id3_tag_declared(frames: list[bytes], declared_delta: int) -> bytes:
    body = b"".join(frames)
    return b"ID3" + bytes((4, 0, 0)) + syncsafe(len(body) + declared_delta) + body


def txxx(description: str, value: str) -> bytes:
    payload = b"\x00" + description.encode("latin-1") + b"\x00" + value.encode("latin-1")
    return frame(b"TXXX", payload)


def tit2(title: str, flags: int = 0, version: int = 4) -> bytes:
    return frame(b"TIT2", b"\x00" + title.encode("latin-1"), flags=flags, version=version)


def write_wav(name: str, chunks: list[bytes], trailing: bytes = b"") -> None:
    body = b"".join(chunks) + trailing
    data = b"RIFF" + struct.pack("<I", len(body) + 4) + b"WAVE" + body
    (FIXTURES / name).write_bytes(data)
    print(name, len(data))


def main() -> int:
    global FIXTURES
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=FIXTURES)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    args = parser.parse_args()

    FIXTURES = args.out
    FIXTURES.mkdir(parents=True, exist_ok=True)
    source = args.source.read_bytes()
    assert source[:4] == b"RIFF" and source[8:12] == b"WAVE"
    chunks = []
    offset = 12
    while offset + 8 <= len(source):
        chunk_id = source[offset:offset + 4]
        size = struct.unpack("<I", source[offset + 4:offset + 8])[0]
        payload = source[offset + 8:offset + 8 + size]
        chunks.append((chunk_id, payload))
        offset += 8 + size + (size & 1)
    fmt = chunk(b"fmt ", next(payload for cid, payload in chunks if cid == b"fmt "))
    data = chunk(b"data", next(payload for cid, payload in chunks if cid == b"data"))

    # ID3 chunk before fmt/data; the odd TXXX keeps the tag size odd.
    mid_tag = id3_tag([txxx("X", "abcd")])
    write_wav("midfile-id3.wav", [chunk(b"ID3 ", mid_tag), fmt, data])

    # Two ID3 chunks: read and write must both fail closed without touching the file.
    write_wav("multi-id3.wav", [chunk(b"ID3 ", id3_tag([tit2("One")])), chunk(b"ID3 ", id3_tag([tit2("Two")])), fmt, data])

    # Version-aware frame flags. v2.3 compression/encryption/grouping; v2.4 grouping/unsync/DLI.
    write_wav("flag-v23-compression.wav", [chunk(b"ID3 ", id3_tag([tit2("v23c", 0x0080, 3)], version=3)), fmt, data])
    write_wav("flag-v23-encryption.wav", [chunk(b"ID3 ", id3_tag([tit2("v23e", 0x0040, 3)], version=3)), fmt, data])
    write_wav("flag-v23-grouping.wav", [chunk(b"ID3 ", id3_tag([tit2("v23g", 0x0020, 3)], version=3)), fmt, data])
    write_wav("flag-v24-grouping.wav", [chunk(b"ID3 ", id3_tag([tit2("v24g", 0x0040, 4)], version=4)), fmt, data])
    write_wav("flag-v24-unsync.wav", [chunk(b"ID3 ", id3_tag([tit2("v24u", 0x0002, 4)], version=4)), fmt, data])
    write_wav("flag-v24-dli.wav", [chunk(b"ID3 ", id3_tag([tit2("v24d", 0x0001, 4)], version=4)), fmt, data])

    # Declared ID3 size smaller/larger than the chunk payload.
    write_wav("tag-size-under.wav", [chunk(b"ID3 ", id3_tag_declared([tit2("Under")], -4)), fmt, data])
    write_wav("tag-size-over.wav", [chunk(b"ID3 ", id3_tag_declared([tit2("Over")], 4)), fmt, data])

    # Final odd chunk without the required pad byte, and trailing garbage after a valid chunk.
    write_wav("odd-final-no-pad.wav", [fmt, chunk_no_pad(b"data", b"\x01\x02\x03")])
    write_wav("trailing-garbage.wav", [fmt, data], trailing=b"\x00\x00\x00")

    # ReplayGain aliases that the codec must normalize and remove on write.
    write_wav(
        "alias-itunes.wav",
        [
            chunk(
                b"ID3 ",
                id3_tag(
                    [
                        txxx("----:com.apple.iTunes:REPLAYGAIN_TRACK_GAIN", "1.11 dB"),
                        txxx("----:com.apple.iTunes:REPLAYGAIN_TRACK_PEAK", "0.11111111"),
                    ]
                ),
            ),
            fmt,
            data,
        ],
    )
    write_wav(
        "alias-r128.wav",
        [chunk(b"ID3 ", id3_tag([txxx("R128_TRACK_GAIN", "-1123"), txxx("REPLAYGAIN_TRACK_PEAK", "0.22222222")])), fmt, data],
    )
    write_wav(
        "alias-case.wav",
        [chunk(b"ID3 ", id3_tag([txxx("replaygain_track_gain", "1.33 dB"), txxx("replaygain_track_peak", "0.33333333")])), fmt, data],
    )
    write_wav(
        "duplicate-alias.wav",
        [
            chunk(
                b"ID3 ",
                id3_tag(
                    [
                        txxx("REPLAYGAIN_TRACK_GAIN", "1.44 dB"),
                        txxx("----:com.apple.iTunes:REPLAYGAIN_TRACK_GAIN", "1.44 dB"),
                    ]
                ),
            ),
            fmt,
            data,
        ],
    )

    # Resource bounds: a real tag above MAX_TAG_SIZE and more chunks than MAX_CHUNKS.
    huge_tag = id3_tag([txxx("BIG", "x" * (33 * 1024 * 1024))])
    write_wav("huge-tag.wav", [chunk(b"ID3 ", huge_tag), fmt, data])
    many = [chunk(b"JUNK", b"") for _ in range(10_001)]
    write_wav("many-chunks.wav", many + [fmt, data])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
