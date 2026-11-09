# ROZKAAM Firebase Emulator Persistent Startup Script
# Automatically restores latest export, backs up data, and exports on clean exit.
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectId = "rozkaam-70984"
$repoRoot = Split-Path -Parent $PSScriptRoot
$exportRoot = Join-Path $repoRoot ".firebase-data\exports"
$backupRoot = Join-Path $repoRoot ".firebase-data\backups"
$canonicalExport = Join-Path $exportRoot "canonical"
$emulatorPorts = @(4000, 5001, 8080, 9099, 9199)

Set-Location $repoRoot
New-Item -ItemType Directory -Path $exportRoot -Force | Out-Null
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

# Check if ports are already occupied
$occupiedPorts = foreach ($port in $emulatorPorts) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        $process = Get-Process -Id $listener.OwningProcess -ErrorAction SilentlyContinue
        [pscustomobject]@{
            Port = $port
            Process = if ($process) { $process.ProcessName } else { "unknown" }
        }
    }
}
if ($occupiedPorts) {
    $details = ($occupiedPorts | ForEach-Object { "$($_.Port) ($($_.Process))" }) -join ", "
    Write-Warning "Emulator ports are already in use: $details."
    Write-Host "Emulators are already running or a previous process did not exit."
    Write-Host "To export running emulator data immediately, run: firebase emulators:export `".firebase-data/exports/canonical`" --force"
    exit 0
}

# Locate latest valid export directory
$validExports = @(
    Get-ChildItem -Path $exportRoot -Directory -Force |
    Where-Object {
        Test-Path (Join-Path $_.FullName "firebase-export-metadata.json")
    } |
    Sort-Object LastWriteTime -Descending
)

$importTarget = $null
if (Test-Path (Join-Path $canonicalExport "firebase-export-metadata.json")) {
    $importTarget = $canonicalExport
} elseif ($validExports.Count -gt 0) {
    $importTarget = $validExports[0].FullName
}

# Backup import data prior to launch
if ($importTarget) {
    $backupName = "backup-{0}" -f [DateTime]::UtcNow.ToString("yyyyMMdd-HHmmss")
    $backupDest = Join-Path $backupRoot $backupName
    Write-Host "Creating safety backup: $importTarget -> $backupDest"
    Copy-Item -Path $importTarget -Destination $backupDest -Recurse -Force
}

$exportTarget = $canonicalExport
$firebaseCommand = Get-Command firebase -ErrorAction Stop

$firebaseArgs = @(
    "emulators:start",
    "--project", $projectId,
    "--only", "auth,firestore,functions,storage",
    "--export-on-exit", $exportTarget
)

if ($importTarget) {
    $firebaseArgs += @("--import", $importTarget)
    Write-Host "Restoring emulator data from: $importTarget"
} else {
    Write-Warning "No existing emulator data found. Starting fresh."
}

Write-Host "Emulator data will be saved on exit to: $exportTarget"
Write-Host "Press Ctrl+C to cleanly stop emulators and save state."

& $firebaseCommand.Source @firebaseArgs
$firebaseExitCode = $LASTEXITCODE

if (Test-Path (Join-Path $exportTarget "firebase-export-metadata.json")) {
    [System.IO.File]::WriteAllText(
        (Join-Path $exportTarget "project-id.txt"),
        $projectId,
        [System.Text.Encoding]::ASCII
    )
    Write-Host "✔ Emulator state successfully preserved to $exportTarget."
}

exit $firebaseExitCode
