[CmdletBinding()]
param(
    [string]$CurrentVersion = "0.2.0",
    [string]$Repository = "Mark-Gachau/pos",
    [string]$ManifestUri
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

function ConvertTo-PilotVersion {
    param([string]$Value)

    $match = [regex]::Match($Value, '(?i)(\d+)\.(\d+)\.(\d+)')
    if (-not $match.Success) {
        throw "Version '$Value' is not a supported pilot version."
    }
    return New-Object Version(
        [int]$match.Groups[1].Value,
        [int]$match.Groups[2].Value,
        [int]$match.Groups[3].Value
    )
}

$current = ConvertTo-PilotVersion -Value $CurrentVersion
Write-Host "Checking Pharmacy POS releases..."
if (-not $ManifestUri) {
    $ManifestUri = "https://raw.githubusercontent.com/$Repository/main/installer/pilot-release.json"
}
if (Test-Path -LiteralPath $ManifestUri -PathType Leaf) {
    $manifest = [IO.File]::ReadAllText([IO.Path]::GetFullPath($ManifestUri)) |
        ConvertFrom-Json
} else {
    $manifest = Invoke-RestMethod `
        -Uri $ManifestUri `
        -Headers @{ "User-Agent" = "Pharmacy-POS-Pilot-Updater" } `
        -UseBasicParsing
}
if (-not $manifest.version -or
    $manifest.tag -notmatch '(?i)^v\d+\.\d+\.\d+-pilot' -or
    $manifest.installerAsset -notmatch '^[A-Za-z0-9._-]+\.exe$' -or
    $manifest.checksumAsset -notmatch '^[A-Za-z0-9._-]+\.sha256$') {
    throw "The Pharmacy POS update manifest is invalid."
}

$latest = ConvertTo-PilotVersion -Value $manifest.version
if ($latest -le $current) {
    Write-Host "Pharmacy POS $CurrentVersion is up to date."
    exit 0
}

$downloadDirectory = Join-Path (
    [Environment]::GetFolderPath("UserProfile")
) "Downloads\Pharmacy POS Updates"
[void](New-Item -ItemType Directory -Path $downloadDirectory -Force)
$versionLabel = $manifest.tag -replace '[^A-Za-z0-9._-]', '_'
$installerPath = Join-Path $downloadDirectory "PharmacyPOS-$versionLabel-Setup.exe"
$checksumPath = "$installerPath.sha256"
$releaseRoot = "https://github.com/$Repository/releases/download/$($manifest.tag)"

Write-Host "Downloading $($manifest.tag)..."
Invoke-WebRequest `
    -Uri "$releaseRoot/$($manifest.installerAsset)" `
    -OutFile $installerPath `
    -UseBasicParsing
Invoke-WebRequest `
    -Uri "$releaseRoot/$($manifest.checksumAsset)" `
    -OutFile $checksumPath `
    -UseBasicParsing

$checksumText = [IO.File]::ReadAllText($checksumPath)
$checksumMatch = [regex]::Match($checksumText, '(?i)\b[a-f0-9]{64}\b')
if (-not $checksumMatch.Success) {
    [IO.File]::Delete($installerPath)
    [IO.File]::Delete($checksumPath)
    throw "The published checksum file is invalid. The update was deleted."
}
$expectedHash = $checksumMatch.Value.ToUpperInvariant()
$actualHash = (Get-FileHash -LiteralPath $installerPath -Algorithm SHA256).Hash
if ($actualHash -cne $expectedHash) {
    [IO.File]::Delete($installerPath)
    [IO.File]::Delete($checksumPath)
    throw "Update verification failed. The downloaded files were deleted."
}

Write-Host "Verified Pharmacy POS $($manifest.tag)."
Write-Host "Back up the pharmacy database before installing an update."
$confirmation = Read-Host "Type INSTALL to open the verified installer"
if ($confirmation -cne "INSTALL") {
    Write-Host "Update downloaded but not started: $installerPath"
    exit 0
}

Start-Process -FilePath $installerPath
