[CmdletBinding()]
param([string]$InstallDir)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "pilot-common.ps1")

try {
    $root = Resolve-PilotRoot -InstallDir $InstallDir
    $environmentPath = Ensure-PilotEnvironment -Root $root
    $docker = Resolve-DockerExecutable
    Wait-ForDockerEngine -DockerExecutable $docker
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @("ps")
    Wait-ForHttpEndpoint -Url "http://localhost:9090/actuator/health" -TimeoutSeconds 15
    Wait-ForHttpEndpoint -Url "http://localhost:3000/login" -TimeoutSeconds 15
    Write-Host "Frontend: http://localhost:3000"
    Write-Host "API health: http://localhost:9090/actuator/health"
    Write-Host "Status: ready"
} catch {
    Write-Error $_
    exit 1
}
