[CmdletBinding()]
param([string]$InstallDir)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "pilot-common.ps1")

try {
    $root = Resolve-PilotRoot -InstallDir $InstallDir
    Stop-PilotHardwareConnector -Root $root
    $environmentPath = Ensure-PilotEnvironment -Root $root
    $docker = Resolve-DockerExecutable
    Wait-ForDockerEngine -DockerExecutable $docker
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @("down")
    Write-Host "Pharmacy POS and the hardware connector stopped. Database volumes were retained."
} catch {
    Write-Error $_
    exit 1
}
