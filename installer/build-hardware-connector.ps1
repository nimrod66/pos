[CmdletBinding()]
param([string]$PythonExecutable = "python")

$ErrorActionPreference = "Stop"
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$connectorRoot = Join-Path $repositoryRoot "connectors"
$outputRoot = Join-Path $repositoryRoot "outputs\pyinstaller"
$distRoot = Join-Path $connectorRoot "dist"
$entryPoint = Join-Path $connectorRoot "hardware_server.py"

& $PythonExecutable -c "import PyInstaller, flask, serial, usb"
if ($LASTEXITCODE -ne 0) {
    throw "Python connector build dependencies are missing. Install connectors/requirements.txt and PyInstaller first."
}

New-Item -ItemType Directory -Force -Path $outputRoot, $distRoot | Out-Null
& $PythonExecutable -m PyInstaller `
    --noconfirm `
    --clean `
    --onefile `
    --name "PharmacyPOS-Hardware-Connector" `
    --distpath $distRoot `
    --workpath (Join-Path $outputRoot "work") `
    --specpath $outputRoot `
    $entryPoint
if ($LASTEXITCODE -ne 0) {
    throw "The hardware connector build failed with exit code $LASTEXITCODE."
}

$connector = Join-Path $distRoot "PharmacyPOS-Hardware-Connector.exe"
if (-not (Test-Path -LiteralPath $connector)) {
    throw "The hardware connector executable was not created at $connector."
}

Get-Item -LiteralPath $connector | Select-Object FullName, Length, LastWriteTime
