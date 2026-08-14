#Requires -Version 5.1
<#
.SYNOPSIS
    Generate, push and scan synthetic test audio into the ssilencce-mcp emulator's MediaStore.
.DESCRIPTION
    Regenerates 4 tagged MP3s with ffmpeg, removes stale copies from the device,
    prunes stale MediaStore rows, pushes the files and triggers a per-file media scan.
    Idempotent: safe to run repeatedly, it never leaves duplicate MediaStore rows.
.EXAMPLE
    .\scripts\setup-test-media.ps1
#>
param()

$ErrorActionPreference = "Stop"
$Sdk = "C:\Users\geode\AppData\Local\Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$RemoteDir = "/sdcard/Music/ssilencce-test"
$LocalDir = Join-Path $env:TEMP "opencode\ssilencce-test\music"

function Invoke-Native {
    param([scriptblock]$Block)
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try { & $Block } finally { $ErrorActionPreference = $old }
}

function Get-OnlineDevice {
    $devices = & $Adb devices | Where-Object { $_ -match "^emulator-\d+\s+device$" }
    if ($devices) { return ($devices -split "\s+")[0] }
    throw "No online emulator. Start it with .\scripts\mcp-emulator.ps1 start first."
}

function Get-Ffmpeg {
    $cmd = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        "C:\Users\geode\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.2-full_build\bin\ffmpeg.exe",
        "C:\Users\geode\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg.Essentials_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-essentials_build\bin\ffmpeg.exe"
    )
    foreach ($c in $candidates) { if (Test-Path -LiteralPath $c) { return $c } }
    throw "ffmpeg not found. Install it or edit Get-Ffmpeg in this script."
}

function New-TestTracks {
    param([string]$Ffmpeg, [string]$OutDir)
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
    $tracks = @(
        @{ File = "01 Sunrise.mp3";  Freq = 440; Title = "Sunrise";   Artist = "Aurora Test"; AlbumArtist = "Aurora Test"; Album = "Morning Vibes"; Track = "1/3"; Genre = "Electronic"; Date = "2024"; Cover = $true },
        @{ File = "02 Sunset.mp3";   Freq = 523; Title = "Sunset";    Artist = "Aurora Test"; AlbumArtist = "Aurora Test"; Album = "Morning Vibes"; Track = "2/3"; Genre = "Electronic"; Date = "2024"; Cover = $false },
        @{ File = "03 Starlight.mp3"; Freq = 659; Title = "Starlight"; Artist = "Nova Band";   AlbumArtist = "Nova Band";   Album = "Night Drive";   Track = "1/2"; Genre = "Synthwave"; Date = "2023"; Cover = $false },
        @{ File = "04 Neon Sky.mp3";  Freq = 784; Title = "Neon Sky";  Artist = "Nova Band";   AlbumArtist = "Nova Band";   Album = "Night Drive";   Track = "2/2"; Genre = "Synthwave"; Date = "2023"; Cover = $false }
    )
    foreach ($t in $tracks) {
        $args = @("-y", "-f", "lavfi", "-i", "sine=frequency=$($t.Freq):duration=30", "-q:a", "8", "-id3v2_version", "3",
            "-metadata", "title=$($t.Title)",
            "-metadata", "artist=$($t.Artist)",
            "-metadata", "album_artist=$($t.AlbumArtist)",
            "-metadata", "album=$($t.Album)",
            "-metadata", "track=$($t.Track)",
            "-metadata", "genre=$($t.Genre)",
            "-metadata", "date=$($t.Date)")
        if ($t.Cover) {
            $args += @("-f", "lavfi", "-i", "color=c=0x8B5CF6:s=512x512:d=30", "-disposition:v", "attached_pic")
        }
        $args += (Join-Path $OutDir $t.File)
        Invoke-Native { & $Ffmpeg @args 2>$null | Out-Null }
        if (-not (Test-Path -LiteralPath (Join-Path $OutDir $t.File))) {
            throw "ffmpeg failed to produce $($t.File)"
        }
    }
}

function Clear-DeviceMedia {
    $device = Get-OnlineDevice
    Invoke-Native { & $Adb -s $device shell "rm -rf $RemoteDir" 2>$null | Out-Null }
    # Force a MediaStore rescan so stale rows for deleted files are pruned
    Invoke-Native { & $Adb -s $device shell "content call --uri content://media/ --method scan_volume --arg external_primary" 2>$null | Out-Null }
    Start-Sleep -Seconds 3
}

function Push-And-Scan {
    $device = Get-OnlineDevice
    Invoke-Native { & $Adb -s $device push $LocalDir $RemoteDir 2>$null | Out-Null }
    Get-ChildItem $LocalDir -Filter "*.mp3" | ForEach-Object {
        $urlName = $_.Name.Replace(" ", "%20")
        Invoke-Native { & $Adb -s $device shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$RemoteDir/$urlName" 2>$null | Out-Null }
    }
    Start-Sleep -Seconds 3
}

function Show-Verification {
    $device = Get-OnlineDevice
    Write-Host "MediaStore rows:"
    Invoke-Native { & $Adb -s $device shell "content query --uri content://media/external/audio/media --projection _display_name:artist:album_artist" }
    Write-Host ""
    Write-Host "Artists:"
    Invoke-Native { & $Adb -s $device shell "content query --uri content://media/external/audio/artists" }
}

$device = Get-OnlineDevice
$ffmpeg = Get-Ffmpeg
Write-Host "Using ffmpeg: $ffmpeg"
Write-Host "Generating test tracks..."
New-TestTracks -Ffmpeg $ffmpeg -OutDir $LocalDir
Write-Host "Clearing old media from $device..."
Clear-DeviceMedia
Write-Host "Pushing and scanning..."
Push-And-Scan
Write-Host "Done. App library should now show 4 songs / 2 albums / 2 artists."
Show-Verification
