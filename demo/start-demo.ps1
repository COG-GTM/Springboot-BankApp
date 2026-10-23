param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$startedAt = Get-Date
$currentStep = 'Initialization'
$failureAlreadyPrinted = $false

function Invoke-Native {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)]
        [string]$Step,
        [int[]]$AllowedExitCodes = @(0),
        [switch]$Quiet
    )

    $nativeOutput = @(& $FilePath @Arguments 2>&1)
    $nativeExitCode = $LASTEXITCODE
    $outputText = ($nativeOutput | ForEach-Object { "$_" }) -join [Environment]::NewLine

    if (-not $Quiet) {
        foreach ($line in $nativeOutput) {
            Write-Host "$line"
        }
    }

    if ($AllowedExitCodes -notcontains $nativeExitCode) {
        if ($outputText) {
            throw "FAILED: $Step (exit $nativeExitCode): $outputText"
        }
        throw "FAILED: $Step (exit $nativeExitCode)"
    }

    return [pscustomobject]@{
        ExitCode = $nativeExitCode
        Output   = $outputText
    }
}

function Test-PortOpen {
    param(
        [string]$HostName,
        [int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne(250, $false)) {
            return $false
        }
        $client.EndConnect($connection)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Write-AppLogTail {
    param([string]$Path)

    if (Test-Path -LiteralPath $Path) {
        Get-Content -LiteralPath $Path -Tail 40
    }
}

try {
    Set-Location $repoRoot
    $jarPath = Join-Path $repoRoot 'target\bankapp-0.0.1-SNAPSHOT.jar'
    $appLog = Join-Path $repoRoot 'demo\logs\app.log'
    $mysqlTimeout = 90

    if ($Build) {
        $currentStep = 'Offline Maven build'
        $wrapperPath = '.\mvnw.cmd'
        if (-not (Test-Path -LiteralPath (Join-Path $repoRoot 'mvnw.cmd'))) {
            throw 'FAILED: mvnw.cmd is missing from the repository.'
        }
        $null = Invoke-Native -FilePath $wrapperPath -Arguments @('-B', '-q', '-o', '-DskipTests', 'package') -Step $currentStep
    }
    elseif (-not (Test-Path -LiteralPath $jarPath)) {
        throw 'FAILED: jar missing, run demo/setup-demo.ps1 first.'
    }

    $currentStep = 'Stop the previous app'
    $oldJavaProcesses = @(Get-CimInstance -ClassName Win32_Process -Filter "Name='java.exe'" | Where-Object {
        $_.CommandLine -like '*bankapp-0.0.1-SNAPSHOT.jar*'
    })
    foreach ($process in $oldJavaProcesses) {
        Stop-Process -Id $process.ProcessId -Force
    }

    $portFree = $false
    for ($attempt = 0; $attempt -lt 15; $attempt++) {
        if (-not (Test-PortOpen -HostName '127.0.0.1' -Port 8080)) {
            $portFree = $true
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not $portFree) {
        $currentStep = 'Wait for port 8080'
        [Console]::Error.WriteLine('FAILED: Port 8080 did not become free within 15 seconds.')
        Write-AppLogTail -Path $appLog
        $failureAlreadyPrinted = $true
        throw 'FAILED: Port 8080 did not become free within 15 seconds.'
    }

    $currentStep = 'Reset MySQL container'
    $dockerCommand = Get-Command 'docker.exe' -ErrorAction Stop
    $existingContainer = Invoke-Native -FilePath $dockerCommand.Source -Arguments @('container', 'inspect', 'bankapp-mysql') -Step 'Check existing MySQL container' -AllowedExitCodes @(0, 1) -Quiet
    if ($existingContainer.ExitCode -eq 0) {
        $null = Invoke-Native -FilePath $dockerCommand.Source -Arguments @('rm', '-f', 'bankapp-mysql') -Step 'Remove existing MySQL container'
    }
    $null = Invoke-Native -FilePath $dockerCommand.Source -Arguments @(
        'run', '-d', '--name', 'bankapp-mysql',
        '-e', 'MYSQL_ROOT_PASSWORD=Test@123',
        '-e', 'MYSQL_DATABASE=bankappdb',
        '-p', '127.0.0.1:3306:3306',
        'mysql:8.0',
        '--default-authentication-plugin=mysql_native_password'
    ) -Step 'Start MySQL container' -Quiet

    $mysqlReady = $false
    for ($attempt = 0; $attempt -lt $mysqlTimeout; $attempt++) {
        $mysqlCheck = Invoke-Native -FilePath $dockerCommand.Source -Arguments @(
            'exec', 'bankapp-mysql', 'mysql', '-uroot', '-pTest@123', '-e', 'select 1', 'bankappdb'
        ) -Step 'Wait for MySQL' -AllowedExitCodes @(0, 1) -Quiet
        if ($mysqlCheck.ExitCode -eq 0) {
            $mysqlReady = $true
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not $mysqlReady) {
        $currentStep = 'Wait for MySQL'
        [Console]::Error.WriteLine("FAILED: MySQL did not become ready within $mysqlTimeout seconds.")
        $null = Invoke-Native -FilePath $dockerCommand.Source -Arguments @('logs', '--tail', '40', 'bankapp-mysql') -Step 'Read MySQL logs'
        $failureAlreadyPrinted = $true
        throw "FAILED: MySQL did not become ready within $mysqlTimeout seconds."
    }

    $currentStep = 'Start the app'
    $logsDirectory = Join-Path $repoRoot 'demo\logs'
    New-Item -Path $logsDirectory -ItemType Directory -Force | Out-Null
    $appErrorLog = Join-Path $logsDirectory 'app.err.log'
    $javaCommand = Get-Command 'java.exe' -ErrorAction Stop
    $previousDataSourceUrl = $env:SPRING_DATASOURCE_URL
    $env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/bankappdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
    try {
        $appProcess = Start-Process -FilePath $javaCommand.Source -ArgumentList @('-jar', ('"{0}"' -f $jarPath)) `
            -WorkingDirectory $repoRoot -RedirectStandardOutput $appLog -RedirectStandardError $appErrorLog -PassThru
    }
    finally {
        if ($null -eq $previousDataSourceUrl) {
            Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
        }
        else {
            $env:SPRING_DATASOURCE_URL = $previousDataSourceUrl
        }
    }
    Set-Content -LiteralPath (Join-Path $logsDirectory 'app.pid') -Value $appProcess.Id -Encoding ASCII

    $appReady = $false
    $appDeadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $appDeadline) {
        $remainingSeconds = [Math]::Ceiling(($appDeadline - (Get-Date)).TotalSeconds)
        $requestTimeout = [int][Math]::Max(1, [Math]::Min(5, $remainingSeconds))
        try {
            $response = Invoke-WebRequest -Uri 'http://localhost:8080/login' -TimeoutSec $requestTimeout -UseBasicParsing
            if ([int]$response.StatusCode -eq 200) {
                $appReady = $true
                break
            }
        }
        catch {
        }
        Start-Sleep -Seconds 1
    }
    if (-not $appReady) {
        $currentStep = 'Wait for app login page'
        [Console]::Error.WriteLine('FAILED: App did not serve /login within 120 seconds.')
        Write-AppLogTail -Path $appLog
        Write-AppLogTail -Path $appErrorLog
        $failureAlreadyPrinted = $true
        throw 'FAILED: App did not serve /login within 120 seconds.'
    }

    $currentStep = 'Seed demo accounts'
    foreach ($username in @('demo_sender', 'demo_payee')) {
        $status = 0
        try {
            $response = Invoke-WebRequest -Uri 'http://localhost:8080/register' -Method Post `
                -Body @{ username = $username; password = 'Passw0rd!' } `
                -MaximumRedirection 0 -TimeoutSec 10 -UseBasicParsing
            $status = [int]$response.StatusCode
        }
        catch {
            if ($_.Exception.Response) {
                $status = [int]$_.Exception.Response.StatusCode
            }
            else {
                throw
            }
        }
        if ($status -ne 302) {
            throw "FAILED: Registration for $username returned HTTP $status, expected 302."
        }
    }

    $currentStep = 'Read branch and revision'
    $gitCommand = Get-Command 'git.exe' -ErrorAction Stop
    $branch = (Invoke-Native -FilePath $gitCommand.Source -Arguments @('-C', $repoRoot, 'branch', '--show-current') -Step 'Read current branch' -Quiet).Output.Trim()
    $shortSha = (Invoke-Native -FilePath $gitCommand.Source -Arguments @('-C', $repoRoot, 'rev-parse', '--short', 'HEAD') -Step 'Read current revision' -Quiet).Output.Trim()
    $elapsed = [int][Math]::Floor(((Get-Date) - $startedAt).TotalSeconds)
    Write-Host "RESET OK: empty bankappdb, app up at http://localhost:8080 from ${branch}@${shortSha} (${elapsed}s)"
}
catch {
    $failure = $_.Exception.Message
    if (-not $failureAlreadyPrinted) {
        if ($failure -notmatch '^FAILED:') {
            $failure = "FAILED: $currentStep. $failure"
        }
        [Console]::Error.WriteLine($failure)
    }
    throw $failure
}
