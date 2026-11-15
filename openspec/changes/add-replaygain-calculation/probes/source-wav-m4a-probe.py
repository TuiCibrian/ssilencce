#!/usr/bin/env python3
"""Static source probe for the AA-06 WAV/MP4 ReplayGain capability patch.

Run from the repository root or directly:
    python openspec/changes/add-replaygain-calculation/probes/source-wav-m4a-probe.py

The probe performs no build and no device access. It verifies that the source contains the
container support, the encoder-delay guard and the fail-closed read-back checks.
"""

from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
SOURCE = REPO / "app" / "src" / "main" / "java" / "com" / "ssilencce" / "data" / "local" / "replaygain"
DECODER = (SOURCE / "ReplayGainDecoder.kt").read_text(encoding="utf-8")
TAGS = (SOURCE / "ReplayGainTags.kt").read_text(encoding="utf-8")
WRITER = (SOURCE / "ReplayGainTagWriter.kt").read_text(encoding="utf-8")
VERSIONS = (REPO / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")

checks: list[tuple[str, bool]] = []


def check(name: str, condition: bool) -> None:
    checks.append((name, condition))


# Decoder: AAC/MP4 encoder priming is removed from the expected frame count.
check("decoder reads KEY_ENCODER_DELAY", "MediaFormat.KEY_ENCODER_DELAY" in DECODER)
check("decoder reads KEY_ENCODER_PADDING", "MediaFormat.KEY_ENCODER_PADDING" in DECODER)
check("decoder passes delay to validateDecodedDuration", "encoderDelaySamples = encoderDelaySamples" in DECODER)
check("decoder passes padding to validateDecodedDuration", "encoderPaddingSamples = encoderPaddingSamples" in DECODER)
check("decoder computes container frames", "containerFrames = expectedDurationUs * sampleRate" in DECODER)
check("decoder subtracts trimmed frames", "expectedFrames = containerFrames - trimmedFrames" in DECODER)
check("decoder clamps trim to container", ".coerceIn(0L, containerFrames)" in DECODER)

# Writer: WAV and MP4 are supported with TagLib-native property names.
check("writer has Wav format", "TagFormat.Wav" in WRITER)
check("writer has Mp4 format", "TagFormat.Mp4" in WRITER)
check("writer has no Itunes format", "TagFormat.Itunes" not in WRITER)
check("writer writes standard keys for WAV", "TagFormat.Standard, TagFormat.Wav" in WRITER)
check("writer calls mp4Properties", "ReplayGainTags.mp4Properties" in WRITER)
check("writer does not call itunesProperties", "itunesProperties" not in WRITER)
check("writer has no WAV guard", "unsupportedContainerMessage" not in WRITER)
check("writer has no WAV unsupported message", "Unsupported container for ReplayGain tags" not in WRITER)
check("writer detects audio/wav", 'audio/wav' in WRITER)
check("writer detects audio/x-wav", 'audio/x-wav' in WRITER)
check("writer reports WAV persistence failure", "WAV ReplayGain tags were not persisted" in WRITER)
check("writer reports M4A persistence failure", "M4A ReplayGain freeform tags were not persisted" in WRITER)

# Read-back: one alias per field and suffix-based matching.
check("writer verifies by suffix", "ReplayGainTags.suffixOf(key)" in WRITER)
check("writer requires exactly one alias", "aliases.size != 1" in WRITER)
check("writer still verifies values", "valuesMatch(key, expectedValue, actualValue)" in WRITER)

# Tag constants: MP4 uses standard names that TagLib maps to iTunes freeform atoms.
check("tags has mp4Properties", "fun mp4Properties(" in TAGS)
check("tags has no itunesProperties", "fun itunesProperties(" not in TAGS)
check("mp4Properties delegates to standardProperties", "standardProperties(trackGainDb, trackPeak, albumGainDb, albumPeak)" in TAGS)
check("reader alias prefix retained", 'ITUNES_PREFIX = "----:com.apple.iTunes:"' in TAGS)

# Scope: no new dependency and the existing TagLib version is unchanged.
check("taglib version unchanged", 'taglib = "1.0.6"' in VERSIONS)
check("taglib module unchanged", 'taglib = { module = "io.github.kyant0:taglib"' in VERSIONS)

failed = [name for name, ok in checks if not ok]
for name, ok in checks:
    print(f"{'PASS' if ok else 'FAIL'}  {name}")
print(f"RESULT {len(checks) - len(failed)}/{len(checks)} PASS")
if failed:
    raise SystemExit(1)
