Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-PilotRoot {
    param([string]$InstallDir)

    if ($InstallDir) {
        return [System.IO.Path]::GetFullPath($InstallDir)
    }

    return [System.IO.Path]::GetFullPath(
        (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
    )
}

function Resolve-DockerExecutable {
    $command = Get-Command docker.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $bundledPath = "C:\Program Files\Docker\Docker\resources\bin\docker.exe"
    if (Test-Path -LiteralPath $bundledPath) {
        return $bundledPath
    }

    throw "Docker Desktop is required. Install and start Docker Desktop, then run Pharmacy POS Start again."
}

function Wait-ForDockerEngine {
    param(
        [string]$DockerExecutable,
        [int]$TimeoutSeconds = 180
    )

    & $DockerExecutable info *> $null
    if ($LASTEXITCODE -eq 0) {
        return
    }

    $desktopPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (Test-Path -LiteralPath $desktopPath) {
        Start-Process -FilePath $desktopPath -WindowStyle Hidden
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        Start-Sleep -Seconds 3
        & $DockerExecutable info *> $null
        if ($LASTEXITCODE -eq 0) {
            return
        }
    } while ((Get-Date) -lt $deadline)

    throw "Docker Desktop did not become ready within $TimeoutSeconds seconds. Start Docker Desktop and try again."
}

function Ensure-PilotEnvironment {
    param([string]$Root)

    $environmentPath = Join-Path $Root ".env.pilot"
    if (Test-Path -LiteralPath $environmentPath) {
        return $environmentPath
    }

    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    $password = [Convert]::ToBase64String($bytes).Replace("+", "A").Replace("/", "B").TrimEnd("=")

    @(
        "POSTGRES_PASSWORD=$password"
        "POS_SEED_DEMO_ENABLED=true"
        "SHOW_DEMO_ACCOUNTS=true"
        "FRONTEND_PORT=3000"
        "API_PORT=9090"
    ) | Set-Content -LiteralPath $environmentPath -Encoding ASCII

    return $environmentPath
}

function Invoke-PilotCompose {
    param(
        [string]$DockerExecutable,
        [string]$Root,
        [string]$EnvironmentPath,
        [string[]]$Arguments
    )

    $composePath = Join-Path $Root "docker-compose.pilot.yml"
    & $DockerExecutable compose `
        --project-name pharmacy-pos-pilot `
        --env-file $EnvironmentPath `
        --file $composePath `
        @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed with exit code $LASTEXITCODE."
    }
}

function Wait-ForHttpEndpoint {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 600
    )

    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromSeconds(5)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    try {
        do {
            try {
                $response = $client.GetAsync($Url).GetAwaiter().GetResult()
                try {
                    if ($response.IsSuccessStatusCode) {
                        return
                    }
                } finally {
                    $response.Dispose()
                }
            } catch {
                # The containers may still be starting.
            }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $deadline)
    } finally {
        $client.Dispose()
    }

    throw "Pharmacy POS did not become ready at $Url within $TimeoutSeconds seconds."
}
