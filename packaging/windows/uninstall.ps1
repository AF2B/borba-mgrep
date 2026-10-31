# Removes the mgrep installed by install.ps1 and cleans the user PATH.
# MGREP_DIR must match the folder used at install time, if one was set.
$ErrorActionPreference = 'Stop'

if ($env:MGREP_DIR) {
    $target = $env:MGREP_DIR
} else {
    $target = Join-Path $env:LOCALAPPDATA 'borba-mgrep'
}

$executable = Join-Path $target 'mgrep.exe'
if (Test-Path $executable) {
    Remove-Item -Force -Path $executable
}

if ((Test-Path $target) -and -not (Get-ChildItem -Force -Path $target)) {
    Remove-Item -Force -Path $target
}

if (-not $env:MGREP_SKIP_PATH) {
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $entries = @($userPath -split ';' | Where-Object { $_ -and $_ -ne $target })
    [Environment]::SetEnvironmentVariable('Path', ($entries -join ';'), 'User')
}

Write-Host "Removed mgrep from $target"
