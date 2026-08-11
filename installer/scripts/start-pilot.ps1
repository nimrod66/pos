[CmdletBinding()]
param(
    [string]$InstallDir,
    [switch]$NoOpen
)

$script = Join-Path $PSScriptRoot "install-pilot.ps1"
$parameters = @{
    InstallDir = $InstallDir
    SkipBuild = $true
}
if ($NoOpen) {
    $parameters.NoOpen = $true
}

& $script @parameters
exit $LASTEXITCODE
