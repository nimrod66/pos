#Requires -Version 5.1
# brutal-runner.ps1 — Master test runner for the Pharmacy POS brutal suite.
# Aggregates results from all brutal test scripts and optional regression tests,
# then generates machine-readable (JSON) and human-readable (MD) reports.

param(
    [string]$Group = "all",
    [int]$ChaosSeed = 0,
    [switch]$IncludeRegression,
    [switch]$SkipCrashTests
)

$ErrorActionPreference = "Continue"
$scriptDir = $PSScriptRoot
$startTime = Get-Date

# ─────────────────────────────────────────────────────────
# Result aggregation
# ─────────────────────────────────────────────────────────

$script:AllResults = [System.Collections.ArrayList]::new()
$script:ScriptSummaries = [System.Collections.ArrayList]::new()
$script:CriticalFailures = [System.Collections.ArrayList]::new()

# ─────────────────────────────────────────────────────────
# Run a regression test (standalone scripts with their own counters)
# ─────────────────────────────────────────────────────────

function Run-RegressionTest($name, $scriptPath, $expectedPass) {
    Write-Host ""
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor DarkCyan
    Write-Host "  REGRESSION: $name" -ForegroundColor DarkCyan
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor DarkCyan

    if (-not (Test-Path $scriptPath)) {
        Write-Host "  SKIP: $scriptPath not found" -ForegroundColor Yellow
        [void]$script:ScriptSummaries.Add(@{
            name = $name; type = "regression"; total = 0; pass = 0; fail = 0; blocked = 0
            passRate = 0; skipped = $true; duration = 0
        })
        return
    }

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $output = & powershell -ExecutionPolicy Bypass -File $scriptPath 2>&1
    $exitCode = $LASTEXITCODE
    $sw.Stop()

    $passCount = 0
    $failCount = 0
    foreach ($line in $output) {
        $lineStr = "$line"
        if ($lineStr -match "PASSED:\s*(\d+)") { $passCount = [int]$Matches[1] }
        if ($lineStr -match "FAILED:\s*(\d+)") { $failCount = [int]$Matches[1] }
        if ($lineStr -match "^\s*PASS\s") { $passCount++ }
        if ($lineStr -match "^\s*FAIL\s") { $failCount++ }
    }

    $total = $passCount + $failCount
    $passRate = if ($total -gt 0) { [math]::Round(($passCount / $total) * 100, 1) } else { 0 }
    $passed = ($exitCode -eq 0)

    $color = if ($passed) { "Green" } else { "Red" }
    Write-Host ("  Result: {0} passed, {1} failed (exit={2})" -f $passCount, $failCount, $exitCode) -ForegroundColor $color

    if (-not $passed) {
        $failLines = @($output | Where-Object { "$_" -match "FAIL" } | ForEach-Object { "$_" })
        [void]$script:CriticalFailures.Add(@{
            script = $name; type = "regression"
            detail = "Exit code $exitCode, $failCount failure(s)"
            failures = $failLines
        })
    }

    [void]$script:ScriptSummaries.Add(@{
        name = $name; type = "regression"; total = $total; pass = $passCount; fail = $failCount; blocked = 0
        passRate = $passRate; skipped = $false; exitCode = $exitCode; duration = $sw.ElapsedMilliseconds
    })
}

# ─────────────────────────────────────────────────────────
# Run a brutal test script (subprocess, reads its JSON output)
# ─────────────────────────────────────────────────────────

function Run-BrutalTest($name, $scriptPath) {
    Write-Host ""
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor Cyan
    Write-Host "  BRUTAL: $name" -ForegroundColor Cyan
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor Cyan

    if (-not (Test-Path $scriptPath)) {
        Write-Host "  SKIP: $scriptPath not found" -ForegroundColor Yellow
        [void]$script:ScriptSummaries.Add(@{
            name = $name; type = "brutal"; total = 0; pass = 0; fail = 0; blocked = 0
            passRate = 0; skipped = $true; duration = 0
        })
        return
    }

    $resultFile = Join-Path $scriptDir ($name -replace '\.ps1$', '-results.json')
    if (Test-Path $resultFile) { Remove-Item $resultFile -Force }

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $output = & powershell -ExecutionPolicy Bypass -File $scriptPath 2>&1
    $exitCode = $LASTEXITCODE
    $sw.Stop()

    # Print script output to console
    foreach ($line in $output) {
        Write-Host "  $line" -ForegroundColor DarkGray
    }

    # Try to read the JSON results file written by the script
    $passCount = 0; $failCount = 0; $blockedCount = 0; $total = 0; $passRate = 0
    $results = @()

    if (Test-Path $resultFile) {
        try {
            $json = Get-Content $resultFile -Raw | ConvertFrom-Json
            $passCount = [int]$json.totalPass
            $failCount = [int]$json.totalFail
            $blockedCount = [int]$json.totalBlocked
            $total = [int]$json.total
            $passRate = [double]$json.passRate
            if ($json.results) { $results = @($json.results) }
        } catch {
            Write-Host "  WARN: Could not parse $resultFile" -ForegroundColor Yellow
        }
    }

    # Fallback: parse output for pass/fail counts
    if ($total -eq 0) {
        foreach ($line in $output) {
            $lineStr = "$line"
            if ($lineStr -match "PASSED:\s*(\d+)") { $passCount = [int]$Matches[1] }
            if ($lineStr -match "FAILED:\s*(\d+)") { $failCount = [int]$Matches[1] }
            if ($lineStr -match "BLOCKED:\s*(\d+)") { $blockedCount = [int]$Matches[1] }
        }
        $total = $passCount + $failCount + $blockedCount
        $passRate = if ($total -gt 0) { [math]::Round(($passCount / $total) * 100, 1) } else { 0 }
    }

    $color = if ($failCount -eq 0) { "Green" } else { "Red" }
    Write-Host ("  Result: {0} pass, {1} fail, {2} blocked ({3}%)" -f $passCount, $failCount, $blockedCount, $passRate) -ForegroundColor $color

    [void]$script:ScriptSummaries.Add(@{
        name = $name; type = "brutal"; total = $total; pass = $passCount; fail = $failCount; blocked = $blockedCount
        passRate = $passRate; skipped = $false; duration = $sw.ElapsedMilliseconds
    })

    # Collect individual results
    foreach ($r in $results) {
        [void]$script:AllResults.Add($r)
    }

    # Track critical failures
    if ($failCount -gt 0) {
        $failures = @($results | Where-Object { $_.status -eq "FAIL" } | ForEach-Object { "$($_.group): $($_.name) — $($_.detail)" })
        [void]$script:CriticalFailures.Add(@{
            script = $name; type = "brutal"
            detail = "$failCount failure(s) out of $total scenarios"
            failures = $failures
        })
    }
}

# ─────────────────────────────────────────────────────────
# Phase 1: Regression tests (optional)
# ─────────────────────────────────────────────────────────

if ($IncludeRegression) {
    Write-Host ""
    Write-Host "############################################################" -ForegroundColor Magenta
    Write-Host "  PHASE 1: REGRESSION TEST SUITE" -ForegroundColor Magenta
    Write-Host "############################################################" -ForegroundColor Magenta

    Run-RegressionTest "smoke-test.ps1" (Join-Path $scriptDir "smoke-test.ps1") 43
    Run-RegressionTest "authz-attack-test.ps1" (Join-Path $scriptDir "authz-attack-test.ps1") 29
    Run-RegressionTest "statemachine-test.ps1" (Join-Path $scriptDir "statemachine-test.ps1") 22
}

# ─────────────────────────────────────────────────────────
# Phase 2: Brutal test scripts
# ─────────────────────────────────────────────────────────

Write-Host ""
Write-Host "############################################################" -ForegroundColor Magenta
Write-Host "  PHASE 2: BRUTAL TEST SUITE" -ForegroundColor Magenta
Write-Host "############################################################" -ForegroundColor Magenta

$brutalScripts = @(
    @{ name = "brutal-concurrency.ps1";    skip = $false },
    @{ name = "brutal-crash-recovery.ps1";  skip = $SkipCrashTests },
    @{ name = "brutal-cash.ps1";            skip = $false },
    @{ name = "brutal-shifts.ps1";          skip = $false },
    @{ name = "brutal-inventory.ps1";       skip = $false },
    @{ name = "brutal-authz.ps1";           skip = $false },
    @{ name = "brutal-returns.ps1";         skip = $false },
    @{ name = "brutal-lan.ps1";             skip = $false },
    @{ name = "brutal-reconciliation.ps1";  skip = $false }
)

foreach ($entry in $brutalScripts) {
    if ($entry.skip) {
        Write-Host ""
        Write-Host "  SKIP: $($entry.name) (SkipCrashTests)" -ForegroundColor Yellow
        [void]$script:ScriptSummaries.Add(@{
            name = $entry.name; type = "brutal"; total = 0; pass = 0; fail = 0; blocked = 0
            passRate = 0; skipped = $true; duration = 0
        })
        continue
    }
    Run-BrutalTest $entry.name (Join-Path $scriptDir $entry.name)
}

$totalDuration = (Get-Date) - $startTime

# ─────────────────────────────────────────────────────────
# Aggregate totals
# ─────────────────────────────────────────────────────────

$grandTotal = 0; $grandPass = 0; $grandFail = 0; $grandBlocked = 0
foreach ($s in $script:ScriptSummaries) {
    $grandTotal   += $s.total
    $grandPass    += $s.pass
    $grandFail    += $s.fail
    $grandBlocked += $s.blocked
}
$grandPassRate = if ($grandTotal -gt 0) { [math]::Round(($grandPass / $grandTotal) * 100, 1) } else { 0 }

# ─────────────────────────────────────────────────────────
# Reliability scores
# ─────────────────────────────────────────────────────────

function Get-CategoryScore($pattern) {
    $matches = @($script:AllResults | Where-Object { $_.group -match $pattern -or $_.name -match $pattern })
    if ($matches.Count -eq 0) { return @{ score = 100; total = 0; pass = 0; fail = 0 } }
    $p = @($matches | Where-Object { $_.status -eq "PASS" }).Count
    $f = @($matches | Where-Object { $_.status -eq "FAIL" }).Count
    $t = $p + $f
    $score = if ($t -gt 0) { [math]::Round(($p / $t) * 100, 1) } else { 100 }
    return @{ score = $score; total = $t; pass = $p; fail = $f }
}

$reliability = @{
    transactionIntegrity = Get-CategoryScore "Cash|Reconcil|Rec[0-9]"
    paymentIntegrity     = Get-CategoryScore "Cash|Payment|Refund|Mpesa"
    inventoryIntegrity   = Get-CategoryScore "Inventory|Stock|Inv[0-9]"
    authorization        = Get-CategoryScore "Authz|Lan[0-9]|CSRF|Session"
    recovery             = Get-CategoryScore "Crash|Recovery|Restart"
}

# ─────────────────────────────────────────────────────────
# Release gate verdict
# ─────────────────────────────────────────────────────────

$releaseBlockers = @()

foreach ($s in ($script:ScriptSummaries | Where-Object { $_.type -eq "regression" -and -not $_.skipped })) {
    if ($s.fail -gt 0) {
        $releaseBlockers += "Regression '$($s.name)' has $($s.fail) failure(s)"
    }
}

foreach ($cf in $script:CriticalFailures | Where-Object { $_.type -eq "brutal" }) {
    $releaseBlockers += "Brutal '$($cf.script)': $($cf.detail)"
}

foreach ($key in $reliability.Keys) {
    if ($reliability[$key].score -lt 80 -and $reliability[$key].total -gt 0) {
        $releaseBlockers += "Reliability '$key' score $($reliability[$key].score)% is below 80% threshold"
    }
}

$verdict = if ($releaseBlockers.Count -gt 0) { "BLOCK" } else { "ALLOW WITH DOCUMENTATION" }

# ─────────────────────────────────────────────────────────
# Generate JSON report
# ─────────────────────────────────────────────────────────

$jsonReport = @{
    generatedAt    = (Get-Date -Format "o")
    durationMs     = [int]$totalDuration.TotalMilliseconds
    parameters     = @{
        Group             = $Group
        ChaosSeed         = $ChaosSeed
        IncludeRegression = [bool]$IncludeRegression
        SkipCrashTests    = [bool]$SkipCrashTests
    }
    summary        = @{
        total       = $grandTotal
        passed      = $grandPass
        failed      = $grandFail
        blocked     = $grandBlocked
        passRate    = $grandPassRate
    }
    scripts        = $script:ScriptSummaries.ToArray()
    reliability    = $reliability
    criticalFailures = $script:CriticalFailures.ToArray()
    releaseGate     = @{
        verdict   = $verdict
        blockers  = $releaseBlockers
    }
    results        = $script:AllResults.ToArray()
}

$jsonPath = Join-Path $scriptDir "brutal-report.json"
$jsonReport | ConvertTo-Json -Depth 10 | Out-File -FilePath $jsonPath -Encoding UTF8

# ─────────────────────────────────────────────────────────
# Generate Markdown report
# ─────────────────────────────────────────────────────────

$md = [System.Text.StringBuilder]::new()
[void]$md.AppendLine("# Pharmacy POS — Brutal Test Report")
[void]$md.AppendLine("")
[void]$md.AppendLine("**Generated:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
[void]$md.AppendLine("**Duration:** $([int]$totalDuration.TotalSeconds)s")
[void]$md.AppendLine("")

[void]$md.AppendLine("## Executive Summary")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Metric | Value |")
[void]$md.AppendLine("|--------|-------|")
[void]$md.AppendLine("| Total Scenarios | $grandTotal |")
[void]$md.AppendLine("| Passed | $grandPass |")
[void]$md.AppendLine("| Failed | $grandFail |")
[void]$md.AppendLine("| Blocked | $grandBlocked |")
[void]$md.AppendLine("| Pass Rate | $grandPassRate% |")
[void]$md.AppendLine("| Release Gate | **$verdict** |")
[void]$md.AppendLine("")

[void]$md.AppendLine("## Script Results")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Script | Type | Total | Pass | Fail | Blocked | Rate | Duration |")
[void]$md.AppendLine("|--------|------|-------|------|------|---------|------|----------|")
foreach ($s in $script:ScriptSummaries) {
    $status = if ($s.skipped) { "SKIP" } elseif ($s.fail -gt 0) { "FAIL" } else { "PASS" }
    $dur = if ($s.duration) { "$([int]($s.duration / 1000))s" } else { "-" }
    [void]$md.AppendLine("| $($s.name) | $($s.type) | $($s.total) | $($s.pass) | $($s.fail) | $($s.blocked) | $($s.passRate)% | $dur |")
}
[void]$md.AppendLine("")

[void]$md.AppendLine("## Reliability Scores")
[void]$md.AppendLine("")
[void]$md.AppendLine("| Category | Score | Scenarios | Pass | Fail |")
[void]$md.AppendLine("|----------|-------|-----------|------|------|")
foreach ($key in $reliability.Keys) {
    $r = $reliability[$key]
    [void]$md.AppendLine("| $key | $($r.score)% | $($r.total) | $($r.pass) | $($r.fail) |")
}
[void]$md.AppendLine("")

if ($script:CriticalFailures.Count -gt 0) {
    [void]$md.AppendLine("## Critical Failures")
    [void]$md.AppendLine("")
    foreach ($cf in $script:CriticalFailures) {
        [void]$md.AppendLine("### $($cf.script)")
        [void]$md.AppendLine("- **Type:** $($cf.type)")
        [void]$md.AppendLine("- **Detail:** $($cf.detail)")
        if ($cf.failures -and $cf.failures.Count -gt 0) {
            [void]$md.AppendLine("- **Failures:**")
            foreach ($f in $cf.failures) {
                [void]$md.AppendLine("  - $f")
            }
        }
        [void]$md.AppendLine("")
    }
}

[void]$md.AppendLine("## Release Gate")
[void]$md.AppendLine("")
[void]$md.AppendLine("**Verdict: $verdict**")
[void]$md.AppendLine("")
if ($releaseBlockers.Count -gt 0) {
    [void]$md.AppendLine("### Blockers")
    [void]$md.AppendLine("")
    foreach ($b in $releaseBlockers) {
        [void]$md.AppendLine("- $b")
    }
    [void]$md.AppendLine("")
}

$mdPath = Join-Path $scriptDir "brutal-report.md"
$md.ToString() | Out-File -FilePath $mdPath -Encoding UTF8

# ─────────────────────────────────────────────────────────
# Console executive summary
# ─────────────────────────────────────────────────────────

Write-Host ""
Write-Host "############################################################" -ForegroundColor White
Write-Host "  BRUTAL TEST SUITE — EXECUTIVE SUMMARY" -ForegroundColor White
Write-Host "############################################################" -ForegroundColor White
Write-Host ""
Write-Host ("  Total Scenarios:  {0}" -f $grandTotal) -ForegroundColor White
Write-Host ("  Passed:           {0}" -f $grandPass) -ForegroundColor Green
Write-Host ("  Failed:           {0}" -f $grandFail) -ForegroundColor $(if ($grandFail -gt 0) { "Red" } else { "Green" })
Write-Host ("  Blocked:          {0}" -f $grandBlocked) -ForegroundColor $(if ($grandBlocked -gt 0) { "Yellow" } else { "Green" })
Write-Host ("  Pass Rate:        {0}%" -f $grandPassRate) -ForegroundColor $(if ($grandPassRate -ge 90) { "Green" } elseif ($grandPassRate -ge 70) { "Yellow" } else { "Red" })
Write-Host ""
Write-Host "  Reliability Scores:" -ForegroundColor White
foreach ($key in $reliability.Keys) {
    $r = $reliability[$key]
    $color = if ($r.score -ge 90) { "Green" } elseif ($r.score -ge 70) { "Yellow" } else { "Red" }
    Write-Host ("    {0,-28} {1}%" -f $key, $r.score) -ForegroundColor $color
}
Write-Host ""

$verdictColor = if ($verdict -eq "BLOCK") { "Red" } else { "Green" }
Write-Host "  Release Gate: $verdict" -ForegroundColor $verdictColor
Write-Host ""

if ($releaseBlockers.Count -gt 0) {
    Write-Host "  Blockers:" -ForegroundColor Red
    foreach ($b in $releaseBlockers) {
        Write-Host "    - $b" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host "  Reports:" -ForegroundColor DarkGray
Write-Host "    $jsonPath" -ForegroundColor DarkGray
Write-Host "    $mdPath" -ForegroundColor DarkGray
Write-Host ""
Write-Host "############################################################" -ForegroundColor White

if ($verdict -eq "BLOCK") { exit 1 }
exit 0
