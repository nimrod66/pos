[CmdletBinding()]
param(
    [string]$InstallDir,
    [string]$EnvFile,
    [string]$BackupPath,
    [string]$Passphrase
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "pilot-common.ps1")

$root = Resolve-PilotRoot -InstallDir $InstallDir
$docker = Resolve-DockerExecutable
Wait-ForDockerEngine -DockerExecutable $docker
$environmentPath = if ($EnvFile) {
    [IO.Path]::GetFullPath($EnvFile)
} else {
    Ensure-PilotEnvironment -Root $root
}
if (-not (Test-Path -LiteralPath $environmentPath -PathType Leaf)) {
    throw "Environment file not found: $environmentPath"
}
$composePath = Join-Path $root "docker-compose.pilot.yml"

if (-not $BackupPath) {
    Add-Type -AssemblyName System.Windows.Forms
    $dialog = New-Object System.Windows.Forms.OpenFileDialog
    $dialog.Title = "Select Pharmacy POS backup"
    $dialog.Filter = "Pharmacy POS backup (*.pposbackup)|*.pposbackup"
    $dialog.InitialDirectory = Join-Path (
        [Environment]::GetFolderPath("MyDocuments")
    ) "Pharmacy POS Backups"
    if ($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) {
        Write-Host "Restore cancelled."
        exit 0
    }
    $BackupPath = $dialog.FileName
}

$resolvedBackup = [IO.Path]::GetFullPath($BackupPath)
if (-not (Test-Path -LiteralPath $resolvedBackup -PathType Leaf)) {
    throw "Backup file not found: $resolvedBackup"
}
if (-not $Passphrase) {
    $Passphrase = Read-PilotBackupPassphrase
}

$confirmation = Read-Host "Type RESTORE to replace the current Pharmacy POS database"
if ($confirmation -cne "RESTORE") {
    Write-Host "Restore cancelled."
    exit 0
}

$temporaryDump = Join-Path $env:TEMP (
    "pharmacy-pos-restore-" + [Guid]::NewGuid().ToString("N") + ".dump"
)
$temporaryDump = [IO.Path]::GetFullPath($temporaryDump)
$tempRoot = [IO.Path]::GetFullPath($env:TEMP)
if (-not $temporaryDump.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "The restore workspace resolved outside the Windows temporary directory."
}
$containerFile = "pharmacy_pos_restore.dump"
$servicesStopped = $false

try {
    Write-Host "Verifying and decrypting backup..."
    Unprotect-PilotBackupFile `
        -InputPath $resolvedBackup `
        -OutputPath $temporaryDump `
        -Passphrase $Passphrase

    Write-Host "Creating a safety backup of the current database..."
    & (Join-Path $PSScriptRoot "backup-pilot.ps1") `
        -InstallDir $root `
        -EnvFile $environmentPath `
        -Passphrase $Passphrase

    Write-Host "Validating the selected PostgreSQL archive..."
    & $docker compose `
        --project-name pharmacy-pos-pilot `
        --env-file $environmentPath `
        --file $composePath `
        cp $temporaryDump "postgres:/tmp/$containerFile"
    if ($LASTEXITCODE -ne 0) {
        throw "Docker could not copy the selected backup into PostgreSQL."
    }
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @(
            "exec", "--no-TTY", "postgres", "sh", "-lc",
            ("pg_restore --list /tmp/$containerFile >/dev/null")
        )

    Write-Host "Stopping the application services..."
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @("stop", "frontend", "api")
    $servicesStopped = $true

    Write-Host "Restoring PostgreSQL..."
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @(
            "exec", "--no-TTY", "postgres", "sh", "-lc",
            (
                'export PGPASSWORD="$POSTGRES_PASSWORD"; ' +
                'dropdb --if-exists --maintenance-db=postgres -U pharmacy_pos pharmacy_pos && ' +
                'createdb -U pharmacy_pos -O pharmacy_pos pharmacy_pos && ' +
                'pg_restore -U pharmacy_pos -d pharmacy_pos --no-owner --no-privileges ' +
                '--exit-on-error /tmp/' + $containerFile
            )
        )

    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @("up", "--detach", "api", "frontend")
    $servicesStopped = $false
    Wait-ForHttpEndpoint -Url "http://localhost:9090/actuator/health"
    Wait-ForHttpEndpoint -Url "http://localhost:3000/login"
    Write-Host "Restore complete. Pharmacy POS is ready at http://localhost:3000"
} catch {
    if ($servicesStopped) {
        try {
            Invoke-PilotCompose `
                -DockerExecutable $docker `
                -Root $root `
                -EnvironmentPath $environmentPath `
                -Arguments @("up", "--detach", "api", "frontend")
        } catch {
            Write-Warning "The application services must be started manually."
        }
    }
    throw
} finally {
    try {
        Invoke-PilotCompose `
            -DockerExecutable $docker `
            -Root $root `
            -EnvironmentPath $environmentPath `
            -Arguments @("exec", "--no-TTY", "postgres", "rm", "-f", "/tmp/$containerFile")
    } catch {
        Write-Warning "The temporary container restore file could not be removed."
    }
    if (Test-Path -LiteralPath $temporaryDump) {
        Remove-Item -LiteralPath $temporaryDump -Force
    }
}
