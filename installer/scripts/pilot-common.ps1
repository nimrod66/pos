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
        "MPESA_CONSUMER_KEY="
        "MPESA_CONSUMER_SECRET="
        "MPESA_PASSKEY="
        "MPESA_SHORTCODE=174379"
        "MPESA_ENVIRONMENT=sandbox"
        "MPESA_CALLBACK_URL="
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

function Get-PilotHardwareConnectorPath {
    param([string]$Root)

    return [System.IO.Path]::GetFullPath(
        (Join-Path $Root "connectors\PharmacyPOS-Hardware-Connector.exe")
    )
}

function Get-PilotHardwareConnectorProcess {
    param([string]$Root)

    $statePath = Join-Path $Root "hardware-connector-process.json"
    if (-not (Test-Path -LiteralPath $statePath)) {
        return $null
    }

    try {
        $state = [IO.File]::ReadAllText($statePath) | ConvertFrom-Json
        $process = Get-Process -Id ([int]$state.processId) -ErrorAction Stop
        $expectedPath = Get-PilotHardwareConnectorPath -Root $Root
        $actualPath = [System.IO.Path]::GetFullPath($process.Path)
        if ($actualPath -cne $expectedPath) {
            return $null
        }
        return $process
    } catch {
        return $null
    }
}

function Start-PilotHardwareConnector {
    param(
        [string]$Root,
        [string]$EnvironmentPath
    )

    $existing = Get-PilotHardwareConnectorProcess -Root $Root
    if ($existing) {
        return $existing
    }

    $executable = Get-PilotHardwareConnectorPath -Root $Root
    if (-not (Test-Path -LiteralPath $executable)) {
        Write-Warning "The hardware connector is not bundled. The POS will run, but local peripherals will remain offline."
        return $null
    }

    $connectorRoot = Join-Path $Root "connectors"
    $configPath = Join-Path $connectorRoot "hardware_config.json"
    $logPath = Join-Path $Root "hardware-connector.log"
    $errorLogPath = Join-Path $Root "hardware-connector-error.log"
    $previousConfig = $env:POS_HARDWARE_CONFIG
    $previousOrigins = $env:POS_CONNECTOR_ALLOWED_ORIGINS
    try {
        $env:POS_HARDWARE_CONFIG = $configPath
        $frontendPort = "3000"
        if ($EnvironmentPath -and (Test-Path -LiteralPath $EnvironmentPath)) {
            $portLine = Get-Content -LiteralPath $EnvironmentPath |
                Where-Object { $_ -match '^FRONTEND_PORT=' } |
                Select-Object -Last 1
            if ($portLine) {
                $candidatePort = ($portLine -split '=', 2)[1].Trim()
                if ($candidatePort -match '^\d{1,5}$') {
                    $frontendPort = $candidatePort
                }
            }
        }
        $env:POS_CONNECTOR_ALLOWED_ORIGINS =
            "http://localhost:$frontendPort,http://127.0.0.1:$frontendPort"
        $process = Start-Process `
            -FilePath $executable `
            -WorkingDirectory $connectorRoot `
            -WindowStyle Hidden `
            -RedirectStandardOutput $logPath `
            -RedirectStandardError $errorLogPath `
            -PassThru
    } finally {
        $env:POS_HARDWARE_CONFIG = $previousConfig
        $env:POS_CONNECTOR_ALLOWED_ORIGINS = $previousOrigins
    }

    @{
        processId = $process.Id
        executable = $executable
        startedAt = (Get-Date).ToUniversalTime().ToString("o")
    } | ConvertTo-Json | Set-Content -LiteralPath (
        Join-Path $Root "hardware-connector-process.json"
    ) -Encoding ASCII

    return $process
}

function Stop-PilotHardwareConnector {
    param([string]$Root)

    $statePath = Join-Path $Root "hardware-connector-process.json"
    $process = Get-PilotHardwareConnectorProcess -Root $Root
    if ($process) {
        Stop-Process -Id $process.Id -Force
        $process.WaitForExit(10000)
    }
    if (Test-Path -LiteralPath $statePath) {
        Remove-Item -LiteralPath $statePath -Force
    }
}

function ConvertFrom-PilotSecureString {
    param([Security.SecureString]$Value)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Read-PilotBackupPassphrase {
    param([switch]$Confirm)

    $first = ConvertFrom-PilotSecureString -Value (
        Read-Host "Backup passphrase (at least 12 characters)" -AsSecureString
    )
    if ($first.Length -lt 12) {
        throw "The backup passphrase must contain at least 12 characters."
    }
    if ($Confirm) {
        $second = ConvertFrom-PilotSecureString -Value (
            Read-Host "Confirm backup passphrase" -AsSecureString
        )
        if ($first -cne $second) {
            throw "The backup passphrases do not match."
        }
    }
    return $first
}

function New-PilotDerivedKeys {
    param(
        [string]$Passphrase,
        [byte[]]$Salt
    )

    $derive = New-Object Security.Cryptography.Rfc2898DeriveBytes(
        $Passphrase,
        $Salt,
        200000
    )
    try {
        $material = $derive.GetBytes(64)
        $encryptionKey = New-Object byte[] 32
        $authenticationKey = New-Object byte[] 32
        [Array]::Copy($material, 0, $encryptionKey, 0, 32)
        [Array]::Copy($material, 32, $authenticationKey, 0, 32)
        return @($encryptionKey, $authenticationKey)
    } finally {
        $derive.Dispose()
    }
}

function Protect-PilotBackupFile {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [string]$Passphrase
    )

    $magic = [Text.Encoding]::ASCII.GetBytes("PPOSBK01")
    $salt = New-Object byte[] 16
    $iv = New-Object byte[] 16
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($salt)
        $random.GetBytes($iv)
    } finally {
        $random.Dispose()
    }

    $keys = New-PilotDerivedKeys -Passphrase $Passphrase -Salt $salt
    $aes = [Security.Cryptography.Aes]::Create()
    $aes.KeySize = 256
    $aes.Mode = [Security.Cryptography.CipherMode]::CBC
    $aes.Padding = [Security.Cryptography.PaddingMode]::PKCS7
    $aes.Key = $keys[0]
    $aes.IV = $iv

    $output = [IO.File]::Create($OutputPath)
    try {
        $output.Write($magic, 0, $magic.Length)
        $output.Write($salt, 0, $salt.Length)
        $output.Write($iv, 0, $iv.Length)
        $encryptor = $aes.CreateEncryptor()
        $crypto = New-Object Security.Cryptography.CryptoStream(
            $output,
            $encryptor,
            [Security.Cryptography.CryptoStreamMode]::Write,
            $true
        )
        try {
            $input = [IO.File]::OpenRead($InputPath)
            try {
                $input.CopyTo($crypto)
            } finally {
                $input.Dispose()
            }
            $crypto.FlushFinalBlock()
        } finally {
            $crypto.Dispose()
            $encryptor.Dispose()
        }
    } finally {
        $output.Dispose()
        $aes.Dispose()
    }

    $hmac = New-Object Security.Cryptography.HMACSHA256(,$keys[1])
    try {
        $source = [IO.File]::OpenRead($OutputPath)
        try {
            $signature = $hmac.ComputeHash($source)
        } finally {
            $source.Dispose()
        }
    } finally {
        $hmac.Dispose()
    }
    $append = [IO.File]::Open($OutputPath, [IO.FileMode]::Append)
    try {
        $append.Write($signature, 0, $signature.Length)
    } finally {
        $append.Dispose()
    }
}

function Unprotect-PilotBackupFile {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [string]$Passphrase
    )

    $file = [IO.File]::OpenRead($InputPath)
    try {
        if ($file.Length -lt 73) {
            throw "The selected file is not a valid Pharmacy POS backup."
        }
        $magic = New-Object byte[] 8
        $salt = New-Object byte[] 16
        $iv = New-Object byte[] 16
        [void]$file.Read($magic, 0, $magic.Length)
        [void]$file.Read($salt, 0, $salt.Length)
        [void]$file.Read($iv, 0, $iv.Length)
        if ([Text.Encoding]::ASCII.GetString($magic) -ne "PPOSBK01") {
            throw "The selected file is not a Pharmacy POS backup."
        }
        $signedLength = $file.Length - 32
        $file.Position = $signedLength
        $actualSignature = New-Object byte[] 32
        [void]$file.Read($actualSignature, 0, $actualSignature.Length)
    } finally {
        $file.Dispose()
    }

    $keys = New-PilotDerivedKeys -Passphrase $Passphrase -Salt $salt
    $hmac = New-Object Security.Cryptography.HMACSHA256(,$keys[1])
    $source = [IO.File]::OpenRead($InputPath)
    try {
        $remaining = $signedLength
        $buffer = New-Object byte[] 81920
        while ($remaining -gt 0) {
            $requested = [Math]::Min($buffer.Length, $remaining)
            $read = $source.Read($buffer, 0, $requested)
            if ($read -le 0) {
                throw "The backup ended unexpectedly."
            }
            [void]$hmac.TransformBlock($buffer, 0, $read, $null, 0)
            $remaining -= $read
        }
        [void]$hmac.TransformFinalBlock((New-Object byte[] 0), 0, 0)
        $expectedSignature = $hmac.Hash
    } finally {
        $source.Dispose()
        $hmac.Dispose()
    }

    $difference = 0
    for ($index = 0; $index -lt 32; $index++) {
        $difference = $difference -bor ($actualSignature[$index] -bxor $expectedSignature[$index])
    }
    if ($difference -ne 0) {
        throw "The passphrase is incorrect or the backup file has been changed."
    }

    $cipherPath = "$OutputPath.cipher"
    $source = [IO.File]::OpenRead($InputPath)
    $cipher = [IO.File]::Create($cipherPath)
    try {
        $source.Position = 40
        $remaining = $signedLength - 40
        $buffer = New-Object byte[] 81920
        while ($remaining -gt 0) {
            $requested = [Math]::Min($buffer.Length, $remaining)
            $read = $source.Read($buffer, 0, $requested)
            if ($read -le 0) {
                throw "The encrypted backup ended unexpectedly."
            }
            $cipher.Write($buffer, 0, $read)
            $remaining -= $read
        }
    } finally {
        $source.Dispose()
        $cipher.Dispose()
    }

    $aes = [Security.Cryptography.Aes]::Create()
    $aes.KeySize = 256
    $aes.Mode = [Security.Cryptography.CipherMode]::CBC
    $aes.Padding = [Security.Cryptography.PaddingMode]::PKCS7
    $aes.Key = $keys[0]
    $aes.IV = $iv
    try {
        $decryptor = $aes.CreateDecryptor()
        $encrypted = [IO.File]::OpenRead($cipherPath)
        $crypto = New-Object Security.Cryptography.CryptoStream(
            $encrypted,
            $decryptor,
            [Security.Cryptography.CryptoStreamMode]::Read
        )
        $output = [IO.File]::Create($OutputPath)
        try {
            $crypto.CopyTo($output)
        } finally {
            $output.Dispose()
            $crypto.Dispose()
            $encrypted.Dispose()
            $decryptor.Dispose()
        }
    } finally {
        $aes.Dispose()
        if (Test-Path -LiteralPath $cipherPath) {
            Remove-Item -LiteralPath $cipherPath -Force
        }
    }
}
