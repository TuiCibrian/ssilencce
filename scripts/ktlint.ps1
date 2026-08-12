#Requires -Version 5.1
<#
.SYNOPSIS
    Run the project's ktlint (com.pinterest.ktlint:ktlint-cli) on one or more files.
    Uses the pinned project version; prefers the fat jar from the Gradle cache,
    falls back to a checksum-verified download under %LOCALAPPDATA%, and finally
    to the ktlintFormat/ktlintCheck Gradle task.
.EXAMPLE
    .\scripts\ktlint.ps1 format app\src\main\java\com\ssilencce\App.kt
    .\scripts\ktlint.ps1 check app\src\main\java\com\ssilencce\App.kt
    .\scripts\ktlint.ps1 version
#>
param(
    [Parameter(Position = 0)]
    [ValidateSet("format", "check", "version")]
    [string]$Action = "format",

    [Parameter(Position = 1, ValueFromRemainingArguments = $true)]
    [string[]]$Files
)

$ErrorActionPreference = "Stop"

$KtlintVersion = "1.8.0"
$LocalJarDir = Join-Path $env:LOCALAPPDATA "ssilencce\ktlint"
$LocalJar = Join-Path $LocalJarDir "ktlint-cli-$KtlintVersion-all.jar"
$CacheGlob = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\com.pinterest.ktlint\ktlint-cli\$KtlintVersion\*\ktlint-cli-$KtlintVersion-all.jar"

function Get-RepoRoot {
    $dir = $PSScriptRoot
    while ($dir) {
        if (Test-Path (Join-Path $dir ".git")) { return $dir }
        $dir = Split-Path $dir -Parent
    }
    throw "Not inside a git repository"
}

function Resolve-Jar {
    $cached = Get-ChildItem $CacheGlob -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cached) { return $cached.FullName }
    if (Test-Path $LocalJar) { return $LocalJar }

    New-Item -ItemType Directory -Path $LocalJarDir -Force | Out-Null
    $base = "https://repo1.maven.org/maven2/com/pinterest/ktlint/ktlint-cli/$KtlintVersion"
    $tmp = "$LocalJar.partial"
    Invoke-WebRequest -UseBasicParsing -Uri "$base/ktlint-cli-$KtlintVersion-all.jar" -OutFile $tmp
    $expected = (Invoke-WebRequest -UseBasicParsing -Uri "$base/ktlint-cli-$KtlintVersion-all.jar.sha256").Content.Trim().Split(" ")[0]
    $actual = (Get-FileHash $tmp -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        Remove-Item $tmp -Force
        throw "ktlint jar checksum mismatch: expected $expected, got $actual"
    }
    Move-Item $tmp $LocalJar
    return $LocalJar
}

$repo = Get-RepoRoot
Push-Location $repo
try {
    if ($Action -eq "version") {
        $jar = Resolve-Jar
        & java -jar $jar --version
        exit $LASTEXITCODE
    }

    $existing = @($Files | Where-Object { $_ -and (Test-Path $_) })
    if ($existing.Count -eq 0) { exit 0 }

    $jar = $null
    try { $jar = Resolve-Jar } catch { Write-Warning "ktlint jar unavailable, falling back to Gradle task: $($_.Exception.Message)" }

    if ($jar) {
        $ktlintArgs = @()
        if ($Action -eq "format") { $ktlintArgs += "--format" }
        $ktlintArgs += $existing
        & java -jar $jar @ktlintArgs
        exit $LASTEXITCODE
    }

    $task = if ($Action -eq "format") { ":app:ktlintFormat" } else { ":app:ktlintCheck" }
    & .\gradlew.bat $task
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
