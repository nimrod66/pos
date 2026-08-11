[CmdletBinding()]
param(
    [string]$InstallDir,
    [switch]$SkipBuild,
    [switch]$NoOpen
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "pilot-common.ps1")

$root = Resolve-PilotRoot -InstallDir $InstallDir
$logPath = Join-Path $root "pharmacy-pos-install.log"

Start-Transcript -LiteralPath $logPath -Append | Out-Null
try {
    Write-Host "Preparing Pharmacy POS Pilot..."
    $docker = Resolve-DockerExecutable
    Wait-ForDockerEngine -DockerExecutable $docker
    $environmentPath = Ensure-PilotEnvironment -Root $root

    $arguments = @("up", "--detach")
    if (-not $SkipBuild) {
        $arguments += "--build"
    }

    Write-Host "Starting PostgreSQL, the API, and the frontend. The first installation can take several minutes."
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments $arguments

    Wait-ForHttpEndpoint -Url "http://localhost:9090/actuator/health"
    Wait-ForHttpEndpoint -Url "http://localhost:3000/login"

    Write-Host "Pharmacy POS Pilot is ready at http://localhost:3000"
    if (-not $NoOpen) {
        Start-Process "http://localhost:3000"
    }
} catch {
    Write-Error $_
    exit 1
} finally {
    Stop-Transcript | Out-Null
}
