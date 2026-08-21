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
$manifestPath = Join-Path $PSScriptRoot "pilot-release.json"
$definitionText = [IO.File]::ReadAllText($definition)
$versionMatch = [regex]::Match(
    $definitionText,
    '(?m)^#define MyAppVersion "(?<version>\d+\.\d+\.\d+)"\r?$'
)
if (-not $versionMatch.Success) {
    throw "The installer version could not be read from PharmacyPOS-Pilot.iss."
}
$manifest = [IO.File]::ReadAllText($manifestPath) | ConvertFrom-Json
if ($manifest.version -cne $versionMatch.Groups["version"].Value -or
    $manifest.tag -cne "v$($manifest.version)-pilot") {
    throw "pilot-release.json must match the installer version and v<version>-pilot tag."
}
if ($env:GITHUB_REF_TYPE -eq "tag" -and
    $env:GITHUB_REF_NAME -cne $manifest.tag) {
    throw "Git tag '$env:GITHUB_REF_NAME' does not match manifest tag '$($manifest.tag)'."
}

& $compiler $definition
if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup failed with exit code $LASTEXITCODE."
}

$installer = Join-Path $outputDirectory "PharmacyPOS-Pilot-Setup.exe"
if (-not (Test-Path -LiteralPath $installer)) {
    throw "Installer output was not created at $installer."
}

$checksumPath = "$installer.sha256"
$checksum = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash.ToLowerInvariant()
"$checksum  PharmacyPOS-Pilot-Setup.exe" |
    Set-Content -LiteralPath $checksumPath -Encoding ASCII

Get-Item -LiteralPath $installer, $checksumPath |
    Select-Object FullName, Length, LastWriteTime
