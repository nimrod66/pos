# Creates a compressed Postgres backup of the pharmacy POS database.
# Usage:
#   powershell -File scripts\backup-db.ps1                     # default keep 14 days
#   powershell -File scripts\backup-db.ps1 -KeepDays 30
param(
    [int]$KeepDays = 14,
    [string]$OutDir = (Join-Path $PSScriptRoot "..\backups")
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outFile = Join-Path $OutDir "pharmacy_pos_$stamp.dump"

docker exec pharmacy-pos-pilot-postgres-1 pg_dump -U pharmacy_pos -d pharmacy_pos -Fc -f "/tmp/$stamp.dump"
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed" }
docker cp "pharmacy-pos-pilot-postgres-1:/tmp/$stamp.dump" $outFile
docker exec pharmacy-pos-pilot-postgres-1 rm "/tmp/$stamp.dump"

$size = "{0:N1} KB" -f ((Get-Item $outFile).Length / 1KB)
Write-Host "Backup written: $outFile ($size)"

# Retention: delete backups older than KeepDays
$cutoff = (Get-Date).AddDays(-$KeepDays)
Get-ChildItem $OutDir -Filter "pharmacy_pos_*.dump" |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    ForEach-Object { Write-Host "Pruning old backup: $($_.Name)"; Remove-Item $_.FullName }

Write-Host "Done. Restoring later:"
Write-Host "  powershell -File scripts\restore-db.ps1 -File `"$outFile`""
