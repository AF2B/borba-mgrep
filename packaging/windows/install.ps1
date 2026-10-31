# Installs mgrep for the current user and adds it to the user PATH.
# MGREP_DIR chooses another folder; MGREP_SKIP_PATH leaves PATH untouched.
$ErrorActionPreference = 'Stop'

$source = Join-Path $PSScriptRoot 'mgrep.exe'
if ($env:MGREP_DIR) {
    $target = $env:MGREP_DIR
} else {
    $target = Join-Path $env:LOCALAPPDATA 'borba-mgrep'
}

New-Item -ItemType Directory -Force -Path $target | Out-Null
Copy-Item -Force -Path $source -Destination (Join-Path $target 'mgrep.exe')

if (-not $env:MGREP_SKIP_PATH) {
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $entries = @($userPath -split ';' | Where-Object { $_ })
    if ($entries -notcontains $target) {
        $updated = ($entries + $target) -join ';'
        [Environment]::SetEnvironmentVariable('Path', $updated, 'User')
    }
}

Write-Host "Installed mgrep to $target"
Write-Host 'Open a new terminal and run: mgrep --help'
