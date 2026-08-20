[CmdletBinding()]
param(
    [string]$InstallDir,
    [string]$EnvFile,
    [string]$OutputDirectory,
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

if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path (
        [Environment]::GetFolderPath("MyDocuments")
    ) "Pharmacy POS Backups"
}
$backupDirectory = [IO.Path]::GetFullPath($OutputDirectory)
[void](New-Item -ItemType Directory -Path $backupDirectory -Force)

if (-not $Passphrase) {
    $Passphrase = Read-PilotBackupPassphrase -Confirm
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$containerFile = "pharmacy_pos_$timestamp.dump"
$temporaryDump = [IO.Path]::GetFullPath((Join-Path $backupDirectory "$containerFile.tmp"))
$backupPath = [IO.Path]::GetFullPath(
    (Join-Path $backupDirectory "pharmacy_pos_$timestamp.pposbackup")
)
if (-not $temporaryDump.StartsWith($backupDirectory, [StringComparison]::OrdinalIgnoreCase) -or
    -not $backupPath.StartsWith($backupDirectory, [StringComparison]::OrdinalIgnoreCase)) {
    throw "The backup output resolved outside the selected backup directory."
}
$backupComplete = $false

try {
    Write-Host "Creating PostgreSQL backup..."
    Invoke-PilotCompose `
        -DockerExecutable $docker `
        -Root $root `
        -EnvironmentPath $environmentPath `
        -Arguments @(
            "exec", "--no-TTY", "postgres", "sh", "-lc",
            ('PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U pharmacy_pos -d pharmacy_pos -Fc -f /tmp/' + $containerFile)
        )

    & $docker compose `
        --project-name pharmacy-pos-pilot `
        --env-file $environmentPath `
        --file $composePath `
        cp "postgres:/tmp/$containerFile" $temporaryDump
    if ($LASTEXITCODE -ne 0) {
        throw "Docker could not copy the database backup to Windows."
    }

    Write-Host "Encrypting and signing backup..."
    Protect-PilotBackupFile `
        -InputPath $temporaryDump `
        -OutputPath $backupPath `
        -Passphrase $Passphrase

    Write-Host "Backup complete: $backupPath"
    Get-Item -LiteralPath $backupPath | Select-Object FullName, Length, LastWriteTime
    $backupComplete = $true
} finally {
    try {
        Invoke-PilotCompose `
            -DockerExecutable $docker `
            -Root $root `
            -EnvironmentPath $environmentPath `
            -Arguments @("exec", "--no-TTY", "postgres", "rm", "-f", "/tmp/$containerFile")
    } catch {
        Write-Warning "The temporary container backup could not be removed."
    }
    if (Test-Path -LiteralPath $temporaryDump) {
        Remove-Item -LiteralPath $temporaryDump -Force
    }
    if (-not $backupComplete -and (Test-Path -LiteralPath $backupPath)) {
        [IO.File]::Delete($backupPath)
    }
}
