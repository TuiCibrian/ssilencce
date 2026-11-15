$ErrorActionPreference = "Stop"
$out = Join-Path $PSScriptRoot "..\fixtures\QA"
New-Item -ItemType Directory -Force $out | Out-Null
$base20 = Join-Path $out "_base20.wav"
$base90 = Join-Path $out "_base90.wav"
if (-not (Test-Path $base20)) { ffmpeg -y -hide_banner -loglevel error -f lavfi -i "sine=frequency=440:duration=20" -c:a pcm_s16le $base20 }
if (-not (Test-Path $base90)) { ffmpeg -y -hide_banner -loglevel error -f lavfi -i "sine=frequency=330:duration=90" -c:a pcm_s16le $base90 }

function New-Mp3($dir, $name, $title, $artist, $album, $track, $disc, $albumArtist, $composer, $date, $src) {
    $dest = Join-Path $out $dir
    New-Item -ItemType Directory -Force $dest | Out-Null
    $file = Join-Path $dest $name
    ffmpeg -y -hide_banner -loglevel error -i $src -c:a libmp3lame -b:a 160k -id3v2_version 3 -metadata title="$title" -metadata artist="$artist" -metadata album="$album" -metadata track="$track" -metadata disc="$disc" -metadata album_artist="$albumArtist" -metadata composer="$composer" -metadata date="$date" $file
}

New-Mp3 "Album Alpha" "01 - Alpha One.mp3" "Alpha One" "Alpha Artist" "Alpha Album" "1/2" "1/1" "Alpha Album Artist" "Alpha Composer" "2024" $base20
New-Mp3 "Album Alpha" "02 - Alpha Two.mp3" "Alpha Two" "Alpha Artist" "Alpha Album" "2/2" "1/1" "Alpha Album Artist" "Alpha Composer" "2024" $base20
New-Mp3 "Album Beta" "01 - Beta One.mp3" "Beta One" "Beta Artist" "Beta Album" "1/2" "1/1" "Beta Album Artist" "Beta Composer" "2023" $base20
New-Mp3 "Album Beta" "02 - Beta Two.mp3" "Beta Two" "Beta Artist" "Beta Album" "2/2" "1/1" "Beta Album Artist" "Beta Composer" "2023" $base20
New-Mp3 "MultiDisc\Disc 1" "01 - MD One.mp3" "MD One" "MD Artist" "QA Multi Disc" "1/2" "1/2" "QA Multi Artist" "MD Composer" "2022" $base20
New-Mp3 "MultiDisc\Disc 1" "02 - MD Two.mp3" "MD Two" "MD Artist" "QA Multi Disc" "2/2" "1/2" "QA Multi Artist" "MD Composer" "2022" $base20
New-Mp3 "MultiDisc\Disc 2" "01 - MD Three.mp3" "MD Three" "MD Artist" "QA Multi Disc" "1/2" "2/2" "QA Multi Artist" "MD Composer" "2022" $base20
New-Mp3 "MultiDisc\Disc 2" "02 - MD Four.mp3" "MD Four" "MD Artist" "QA Multi Disc" "2/2" "2/2" "QA Multi Artist" "MD Composer" "2022" $base20
New-Mp3 "Long" "01 - Long Track.mp3" "Long Track" "Long Artist" "Long Album" "1/1" "1/1" "Long Artist" "" "2021" $base90

$formats = Join-Path $out "Formats"
New-Item -ItemType Directory -Force $formats | Out-Null
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a flac -metadata title="QA FLAC" -metadata artist="QA Formats" -metadata album="QA Format Album" -metadata track="1/1" -metadata disc="1/1" (Join-Path $formats "qa.flac")
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a aac -b:a 160k -f ipod -metadata title="QA M4A" -metadata artist="QA Formats" -metadata album="QA Format Album" -metadata track="2/2" -metadata disc="1/1" (Join-Path $formats "qa.m4a")
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a libvorbis -q:a 5 -f ogg -metadata title="QA OGG" -metadata artist="QA Formats" -metadata album="QA Format Album" (Join-Path $formats "qa.ogg")
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a libopus -b:a 128k -f opus -metadata title="QA OPUS" -metadata artist="QA Formats" -metadata album="QA Format Album" (Join-Path $formats "qa.opus")
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a pcm_s16le -metadata title="QA WAV" -metadata artist="QA Formats" -metadata album="QA Format Album" (Join-Path $formats "qa.wav")
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a pcm_s16be -f aiff -metadata title="QA AIFF" -metadata artist="QA Formats" -metadata album="QA Format Album" (Join-Path $formats "qa.aiff")
ffmpeg -y -hide_banner -loglevel error -i $base20 -c:a wmav2 -b:a 128k -f asf -metadata title="QA WMA" -metadata artist="QA Formats" -metadata album="QA Format Album" (Join-Path $formats "qa.wma")
ffmpeg -y -hide_banner -loglevel error -f lavfi -i "color=c=0x7b2ff7:s=500x500" -frames:v 1 (Join-Path $formats "cover.jpg")

$lrc = @"
[00:00.00]Alpha line one
[00:04.00]Alpha line two
[00:08.00]Alpha line three
[00:12.00]Alpha line four
[00:16.00]Alpha line five
"@
Set-Content -Path (Join-Path $out "Album Alpha\01 - Alpha One.lrc") -Value $lrc -Encoding UTF8

Remove-Item $base20, $base90 -Force -ErrorAction SilentlyContinue
Get-ChildItem $out -Recurse -File | Select-Object FullName, Length
