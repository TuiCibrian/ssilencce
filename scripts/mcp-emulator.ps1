#Requires -Version 5.1
<#
.SYNOPSIS
    Manage the android-remote-control-mcp server on the ssilencce-mcp emulator.
.EXAMPLE
    .\scripts\mcp-emulator.ps1 start
    .\scripts\mcp-emulator.ps1 stop
    .\scripts\mcp-emulator.ps1 status
#>
param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "stop", "status", "center")]
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

function Center-EmulatorWindow {
    Add-Type -AssemblyName System.Windows.Forms
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
    $proc = Get-Process | Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -match "ssilencce-mcp|Android Emulator" } | Select-Object -First 1
    if (-not $proc) { return }
    $rect = New-Object McpWin32+RECT
    [McpWin32]::GetWindowRect($proc.MainWindowHandle, [ref]$rect) | Out-Null
    $w = $rect.Right - $rect.Left
    $h = $rect.Bottom - $rect.Top
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $x = $bounds.X + [int](($bounds.Width - $w) / 2)
    $y = $bounds.Y + [int](($bounds.Height - $h) / 2)
    if ($y -lt 0) { $y = 0 }
    [McpWin32]::MoveWindow($proc.MainWindowHandle, $x, $y, $w, $h, $true) | Out-Null
    [McpWin32]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
}

function Test-Server {
    $token = Get-Token
    if (-not $token) { Write-Warning "ANDROID_MCP_TOKEN is not set; cannot test auth" }
    $body = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"mcp-emulator-script","version":"1.0"}}}'
    $response = curl.exe -s -o NUL -w "%{http_code}" -X POST $McpUrl -H "Authorization: Bearer $token" -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d $body
    return ($response -eq "200")
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
        Center-EmulatorWindow
        Write-Host "Emulator window centered on primary screen"
    }
}
