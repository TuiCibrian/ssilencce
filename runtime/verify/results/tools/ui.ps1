param(
    [Parameter(Mandatory=$true)][string]$Action,
    [string]$Arg1 = "",
    [string]$Arg2 = "",
    [string]$Arg3 = "",
    [string]$Arg4 = "",
    [string]$Serial = "emulator-5554"
)

$ErrorActionPreference = "Continue"
$adb = "C:\Users\geode\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$root = Split-Path -Parent $PSScriptRoot
$evidence = Join-Path $root "evidence"
New-Item -ItemType Directory -Force $evidence | Out-Null
$tmp = Join-Path $env:TEMP "ssilencce-ui.xml"

function Invoke-Adb([string[]]$AdbArgs) {
    & $adb -s $Serial @AdbArgs 2>&1
}

function Get-UiXml([switch]$Save, [string]$Name = "ui") {
    Invoke-Adb @("shell", "uiautomator", "dump", "/sdcard/ui.xml") | Out-Null
    & $adb -s $Serial pull /sdcard/ui.xml $tmp 2>$null | Out-Null
    if (-not (Test-Path $tmp)) { throw "uiautomator dump failed" }
    if ($Save) { Copy-Item $tmp (Join-Path $evidence "$Name.xml") -Force }
    return [xml](Get-Content -Raw -Path $tmp)
}

function Get-Bounds([string]$b) {
    if ($b -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        return @([int]$Matches[1], [int]$Matches[2], [int]$Matches[3], [int]$Matches[4])
    }
    return @(0,0,0,0)
}

function Get-MatchNodes([xml]$doc, [string]$pattern
    , [switch]$IncludeText, [switch]$IncludeDesc, [switch]$IncludeId) {
    $nodes = $doc.SelectNodes("//node")
    $result = @()
    foreach ($n in $nodes) {
        $text = $n.GetAttribute("text")
        $desc = $n.GetAttribute("content-desc")
        $id = $n.GetAttribute("resource-id")
        $hit = $false
        if ($IncludeText -or (-not $IncludeText -and -not $IncludeDesc -and -not $IncludeId)) {
            if ($text -and $text -match $pattern) { $hit = $true }
        }
        if ($IncludeDesc) { if ($desc -and $desc -match $pattern) { $hit = $true } }
        if ($IncludeId) { if ($id -and $id -match $pattern) { $hit = $true } }
        if ($hit) {
            $clickable = $n
            while ($clickable -and $clickable.Name -eq "node" -and $clickable.GetAttribute("clickable") -ne "true") {
                $clickable = $clickable.ParentNode
            }
            $result += [pscustomobject]@{
                Text = $text
                Desc = $desc
                Id = $id
                Class = $n.GetAttribute("class")
                Bounds = $n.GetAttribute("bounds")
                Clickable = ($clickable -and $clickable.Name -eq "node" -and $clickable.GetAttribute("clickable") -eq "true")
                ClickBounds = if ($clickable -and $clickable.Name -eq "node") { $clickable.GetAttribute("bounds") } else { "" }
                Node = $n
            }
        }
    }
    return $result
}

function Tap-Node($node) {
    $b = Get-Bounds $node.ClickBounds
    if ($b[2] -eq 0) { $b = Get-Bounds $node.Bounds }
    $x = [int](($b[0] + $b[2]) / 2)
    $y = [int](($b[1] + $b[3]) / 2)
    Invoke-Adb @("shell", "input", "tap", "$x", "$y") | Out-Null
    return "tap $x $y ($($node.Text)$($node.Desc))"
}

function Save-Screenshot([string]$Name) {
    $out = Join-Path $evidence "$Name.png"
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = "-s $Serial exec-out screencap -p"
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $p = [System.Diagnostics.Process]::Start($psi)
    $ms = New-Object System.IO.MemoryStream
    $p.StandardOutput.BaseStream.CopyTo($ms)
    $p.WaitForExit()
    [System.IO.File]::WriteAllBytes($out, $ms.ToArray())
    return $out
}

switch ($Action.ToLower()) {
    "find" {
        $doc = Get-UiXml
        $nodes = Get-MatchNodes $doc $Arg1 -IncludeText -IncludeDesc -IncludeId
        $i = 0
        foreach ($n in $nodes) {
            Write-Output ("[{0}] text='{1}' desc='{2}' id='{3}' class='{4}' clickable={5} bounds={6} clickBounds={7}" -f $i, $n.Text, $n.Desc, $n.Id, $n.Class, $n.Clickable, $n.Bounds, $n.ClickBounds)
            $i++
        }
        if ($i -eq 0) { Write-Output "NO MATCH for '$Arg1'" }
    }
    "tap" {
        $doc = Get-UiXml
        $nodes = @(Get-MatchNodes $doc $Arg1 -IncludeText -IncludeDesc -IncludeId)
        if ($nodes.Count -eq 0) { Write-Output "NO MATCH for '$Arg1'"; exit 2 }
        $idx = 0
        if ($Arg2 -ne "") { $idx = [int]$Arg2 }
        if ($idx -ge $nodes.Count) { Write-Output "index $idx out of range ($($nodes.Count) matches)"; exit 2 }
        Write-Output (Tap-Node $nodes[$idx])
    }
    "tapxy" {
        Invoke-Adb @("shell", "input", "tap", $Arg1, $Arg2) | Out-Null
        Write-Output "tap $Arg1 $Arg2"
    }
    "swipe" {
        $ms = 400
        if ($Arg4 -ne "") { $ms = [int]$Arg4 }
        Invoke-Adb @("shell", "input", "swipe", $Arg1, $Arg2, $Arg3, $Arg4, "$ms") | Out-Null
        Write-Output "swipe $Arg1 $Arg2 $Arg3 $Arg4 $ms"
    }
    "key" {
        Invoke-Adb @("shell", "input", "keyevent", $Arg1) | Out-Null
        Write-Output "key $Arg1"
    }
    "type" {
        Invoke-Adb @("shell", "input", "text", $Arg1) | Out-Null
        Write-Output "type $Arg1"
    }
    "shot" {
        Write-Output (Save-Screenshot $Arg1)
    }
    "dump" {
        $doc = Get-UiXml -Save -Name $Arg1
        Write-Output "saved $Arg1.xml"
    }
    "wait" {
        $deadline = (Get-Date).AddSeconds([int]$Arg2)
        while ((Get-Date) -lt $deadline) {
            try {
                $doc = Get-UiXml
                $nodes = @(Get-MatchNodes $doc $Arg1 -IncludeText -IncludeDesc -IncludeId)
                if ($nodes.Count -gt 0) { Write-Output "FOUND '$Arg1' after wait"; exit 0 }
            } catch { }
            Start-Sleep -Milliseconds 800
        }
        Write-Output "TIMEOUT waiting for '$Arg1'"
        exit 3
    }
    "back" {
        Invoke-Adb @("shell", "input", "keyevent", "4") | Out-Null
        Write-Output "back"
    }
    "home" {
        Invoke-Adb @("shell", "input", "keyevent", "3") | Out-Null
        Write-Output "home"
    }
    "windows" {
        Invoke-Adb @("shell", "dumpsys", "window", "windows") | Select-String -Pattern "mCurrentFocus|mFocusedApp"
    }
    "activity" {
        Invoke-Adb @("shell", "dumpsys", "activity", "activities") | Select-String -Pattern "mResumedActivity|topResumedActivity" | Select-Object -First 2
    }
    default {
        Write-Output "Unknown action: $Action"
        exit 1
    }
}
