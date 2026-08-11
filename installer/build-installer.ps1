[CmdletBinding()]
param([string]$IsccPath)

$ErrorActionPreference = "Stop"
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$outputDirectory = Join-Path $repositoryRoot "outputs\installer"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$candidates = @(
    $IsccPath,
    $env:ISCC_PATH,
    (Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\ISCC.exe"),
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    "C:\Program Files\Inno Setup 6\ISCC.exe",
    "C:\ProgramData\chocolatey\bin\ISCC.exe"
) | Where-Object { $_ }

$compiler = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $compiler) {
    throw "Inno Setup 6 was not found. Install it or pass -IsccPath."
}

$definition = Join-Path $PSScriptRoot "PharmacyPOS-Pilot.iss"
& $compiler $definition
if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup failed with exit code $LASTEXITCODE."
}

$installer = Join-Path $outputDirectory "PharmacyPOS-Pilot-Setup.exe"
if (-not (Test-Path -LiteralPath $installer)) {
    throw "Installer output was not created at $installer."
}

Get-Item -LiteralPath $installer | Select-Object FullName, Length, LastWriteTime
