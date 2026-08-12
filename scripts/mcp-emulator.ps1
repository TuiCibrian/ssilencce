#Requires -Version 5.1
<#
.SYNOPSIS
    Manage the android-remote-control-mcp server on the ssilencce-mcp emulator.
    The emulator window is auto-centered on new launches; manual moves are respected.
.EXAMPLE
    .\scripts\mcp-emulator.ps1 start
    .\scripts\mcp-emulator.ps1 stop
    .\scripts\mcp-emulator.ps1 status
    .\scripts\mcp-emulator.ps1 center
    .\scripts\mcp-emulator.ps1 watch
    .\scripts\mcp-emulator.ps1 install-watch
    .\scripts\mcp-emulator.ps1 uninstall-watch
#>
param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "stop", "status", "center", "watch", "install-watch", "uninstall-watch")]
    [string]$Action = "status",
    [switch]$KillEmulator
)

$ErrorActionPreference = "Stop"
$Sdk = "C:\Users\geode\AppData\Local\Android\Sdk"
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"
$AvdName = "ssilencce-mcp"
$Pkg = "com.danielealbano.androidremotecontrolmcp"
$Trampoline = "$Pkg/com.danielealbano.androidremotecontrolmcp.services.mcp.AdbServiceTrampolineActivity"
$McpUrl = "http://localhost:8080/mcp"
$WatchTask = "ssilencce-emulator-watch"
$CenterTolerance = 20
$RecenteringGrace = 30

function Get-Token {
    $token = [Environment]::GetEnvironmentVariable("ANDROID_MCP_TOKEN", "User")
    if (-not $token) { $token = $env:ANDROID_MCP_TOKEN }
    return $token
}

function Get-OnlineDevice {
    $devices = & $Adb devices | Where-Object { $_ -match "^emulator-\d+\s+device$" }
    if ($devices) { return ($devices -split "\s+")[0] }
    return $null
}

function Wait-ForBoot {
    & $Adb wait-for-device | Out-Null
    $booted = ""
    for ($i = 0; $i -lt 120; $i++) {
        $booted = (& $Adb shell getprop sys.boot_completed 2>$null).Trim()
        if ($booted -eq "1") { return }
        Start-Sleep -Seconds 5
    }
    throw "Timed out waiting for emulator boot"
}

function Test-Server {
    $token = Get-Token
    if (-not $token) { Write-Warning "ANDROID_MCP_TOKEN is not set; cannot test auth" }
    $body = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"mcp-emulator-script","version":"1.0"}}}'
    $response = curl.exe -s -o NUL -w "%{http_code}" -X POST $McpUrl -H "Authorization: Bearer $token" -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d $body
    return ($response -eq "200")
}

function Ensure-Win32Types {
    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    if (-not ('McpWin32' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public class McpWin32 {
    [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool bRepaint);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    public struct RECT { public int Left, Top, Right, Bottom; }
}
'@
    }
}

function Get-EmulatorWindowProc {
    return Get-Process | Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -match "ssilencce-mcp|Android Emulator" } | Select-Object -First 1
}

function Should-Recenter([int]$left, [int]$top, [int]$w, [int]$h, $bounds) {
    if ($left -le 130 -and $top -le 130) { return $true }
    if ($top -lt -50 -or $left -lt -50) { return $true }
    if ($left + $w -le 50 -or $top + $h -le 50 -or $left -ge $bounds.Right - 50 -or $top -ge $bounds.Bottom - 50) { return $true }
    $cx = $bounds.X + [int](($bounds.Width - $w) / 2)
    $cy = $bounds.Y + [int](($bounds.Height - $h) / 2)
    return ([Math]::Abs($left - $cx) -le 120 -and [Math]::Abs($top - $cy) -le 120)
}

function Center-EmulatorWindow {
    [CmdletBinding()]
    param([switch]$Force)
    Ensure-Win32Types
    $proc = Get-EmulatorWindowProc
    if (-not $proc) { return $false }
    $rect = New-Object McpWin32+RECT
    if (-not [McpWin32]::GetWindowRect($proc.MainWindowHandle, [ref]$rect)) { return $false }
    $w = $rect.Right - $rect.Left
    $h = $rect.Bottom - $rect.Top
    if ($w -lt 100 -or $h -lt 100) { return $false }
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    if ($w -ge $bounds.Width - 20 -and $h -ge $bounds.Height - 20) { return $false }
    if (-not $Force -and -not (Should-Recenter $rect.Left $rect.Top $w $h $bounds)) { return $false }
    $targetX = $bounds.X + [int](($bounds.Width - $w) / 2)
    $targetY = $bounds.Y + [int](($bounds.Height - $h) / 2)
    if ($targetY -lt $bounds.Y) { $targetY = $bounds.Y }
    $centered = ([Math]::Abs($rect.Left - $targetX) -le $CenterTolerance -and [Math]::Abs($rect.Top - $targetY) -le $CenterTolerance)
    if ($Force -or -not $centered) {
        [McpWin32]::MoveWindow($proc.MainWindowHandle, $targetX, $targetY, $w, $h, $true) | Out-Null
        [McpWin32]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
    }
    return (-not $centered)
}

switch ($Action) {
    "start" {
        if (-not (Get-OnlineDevice)) {
            Write-Host "Starting emulator $AvdName..."
            Start-Process -FilePath $Emulator -ArgumentList "-avd", $AvdName, "-no-snapshot", "-no-boot-anim" -WindowStyle Normal
            Wait-ForBoot
        }
        & $Adb shell am start -n $Trampoline --es action start | Out-Null
        Start-Sleep -Seconds 6
        & $Adb forward tcp:8080 tcp:8080 | Out-Null
        Center-EmulatorWindow
        if (Test-Server) { Write-Host "MCP server OK at $McpUrl" } else { Write-Warning "MCP server did not answer yet; retry 'status' in a few seconds" }
    }
    "stop" {
        if (Get-OnlineDevice) {
            & $Adb shell am start -n $Trampoline --es action stop | Out-Null
            & $Adb forward --remove tcp:8080 2>$null | Out-Null
            if ($KillEmulator) { & $Adb emu kill | Out-Null; Write-Host "Emulator stopped" }
            else { Write-Host "MCP server stopped (emulator left running)" }
        } else {
            Write-Host "No device online"
        }
    }
    "status" {
        $device = Get-OnlineDevice
        if ($device) { Write-Host "Device: $device" } else { Write-Host "Device: offline" }
        & $Adb forward --list
        if (Test-Server) { Write-Host "MCP server: RUNNING ($McpUrl)" } else { Write-Host "MCP server: stopped" }
    }
    "center" {
        if (Center-EmulatorWindow -Force) { Write-Host "Emulator window centered on primary screen" }
        else { Write-Host "No emulator window found (or already centered)" }
    }
    "watch" {
        Write-Host "Watch mode active: centering NEW emulator windows only. Ctrl+C to stop."
        $knownStart = $null
        $knownHandle = [IntPtr]::Zero
        $graceUntil = $null
        while ($true) {
            try {
                $proc = Get-EmulatorWindowProc
                if ($proc) {
                    $start = $proc.StartTime
                    $handle = [int64]$proc.MainWindowHandle
                    if ($null -eq $knownStart) {
                        $knownStart = $start
                        $knownHandle = $handle
                    } elseif ($start -ne $knownStart -or $handle -ne $knownHandle) {
                        $knownStart = $start
                        $knownHandle = $handle
                        $graceUntil = (Get-Date).AddSeconds($RecenteringGrace)
                        Write-Host "New emulator window detected; centering..."
                        [void](Center-EmulatorWindow -Force)
                    } elseif ($null -ne $graceUntil) {
                        if ((Get-Date) -ge $graceUntil) { $graceUntil = $null }
                        else { [void](Center-EmulatorWindow -Force) }
                    }
                } elseif ($null -ne $knownStart) {
                    $knownStart = $null
                    $knownHandle = [IntPtr]::Zero
                    $graceUntil = $null
                    Write-Host "Emulator window closed; waiting for next launch..."
                }
            } catch {
                Write-Warning "watch iteration failed: $_"
            }
            Start-Sleep -Seconds 3
        }
    }
    "install-watch" {
        $runKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run"
        $cmd = "powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$(Join-Path $PSScriptRoot 'mcp-emulator.ps1')`" watch"
        New-ItemProperty -Path $runKey -Name $WatchTask -Value $cmd -PropertyType String -Force | Out-Null
        Write-Host "Watch installed in HKCU Run (starts at login, no admin needed)"
    }
    "uninstall-watch" {
        Remove-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run" -Name $WatchTask -ErrorAction SilentlyContinue
        Write-Host "Watch removed from HKCU Run"
    }
}
