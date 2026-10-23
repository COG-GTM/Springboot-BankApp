$ErrorActionPreference = 'Stop'

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$startedAt = Get-Date
$currentStep = 'Initialization'
$matrixBackup = $null
$temporaryDirectory = $null
$setupError = $null

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

function Refresh-PathFromRegistry {
    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = "$machinePath;$userPath"
}

function Get-JavaMajorVersion {
    $javaCommand = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        $javaCommand = Get-Command 'java' -ErrorAction SilentlyContinue
    }
    if (-not $javaCommand) {
        return 0
    }

    $result = Invoke-Native -FilePath $javaCommand.Source -Arguments @('-version') -Step 'Check Java version' -Quiet
    if ($result.Output -match 'version\s+"(?<major>\d+)') {
        return [int]$Matches.major
    }
    return 0
}

function Get-NodeMajorVersion {
    $nodeCommand = Get-Command 'node.exe' -ErrorAction SilentlyContinue
    if (-not $nodeCommand) {
        $nodeCommand = Get-Command 'node' -ErrorAction SilentlyContinue
    }
    if (-not $nodeCommand) {
        return 0
    }

    $result = Invoke-Native -FilePath $nodeCommand.Source -Arguments @('-v') -Step 'Check Node version' -Quiet
    if ($result.Output -match 'v?(?<major>\d+)') {
        return [int]$Matches.major
    }
    return 0
}

try {
    Set-Location $repoRoot

    $currentStep = 'JDK 17'
    Write-Host "==> $currentStep"
    if ((Get-JavaMajorVersion) -lt 17) {
        $wingetCommand = Get-Command 'winget.exe' -ErrorAction SilentlyContinue
        if (-not $wingetCommand) {
            throw 'FAILED: Install Windows Package Manager, then rerun this setup.'
        }
        $null = Invoke-Native -FilePath $wingetCommand.Source -Arguments @(
            'install', '--id', 'EclipseAdoptium.Temurin.17.JDK', '--exact',
            '--accept-source-agreements', '--accept-package-agreements', '--silent'
        ) -Step 'Install JDK 17'
        Refresh-PathFromRegistry
        if ((Get-JavaMajorVersion) -lt 17) {
            throw 'FAILED: JDK 17 was not available after installation.'
        }
    }

    $currentStep = 'Node 20'
    Write-Host "==> $currentStep"
    if ((Get-NodeMajorVersion) -ne 20) {
        $wingetCommand = Get-Command 'winget.exe' -ErrorAction SilentlyContinue
        if (-not $wingetCommand) {
            throw 'FAILED: Install Windows Package Manager, then rerun this setup.'
        }
        $null = Invoke-Native -FilePath $wingetCommand.Source -Arguments @(
            'install', '--id', 'OpenJS.NodeJS.20', '--exact',
            '--accept-source-agreements', '--accept-package-agreements', '--silent'
        ) -Step 'Install Node 20'
        Refresh-PathFromRegistry
        if ((Get-NodeMajorVersion) -ne 20) {
            throw 'FAILED: Node 20 was not available after installation.'
        }
    }

    $currentStep = 'Docker Desktop and MySQL image'
    Write-Host "==> $currentStep"
    $dockerCommand = Get-Command 'docker.exe' -ErrorAction SilentlyContinue
    if (-not $dockerCommand) {
        throw 'FAILED: Docker Desktop is missing. Install it with winget install --id Docker.DockerDesktop -e, start Docker Desktop, then rerun this setup.'
    }

    $dockerInfo = Invoke-Native -FilePath $dockerCommand.Source -Arguments @('info') -Step 'Check Docker Desktop' -AllowedExitCodes @(0, 1) -Quiet
    if ($dockerInfo.ExitCode -ne 0) {
        throw 'FAILED: Docker Desktop is installed but not running. Start Docker Desktop, then rerun this setup.'
    }

    $dockerPull = Invoke-Native -FilePath $dockerCommand.Source -Arguments @('pull', 'mysql:8.0') -Step 'Pull MySQL image' -AllowedExitCodes @(0, 1) -Quiet
    if ($dockerPull.ExitCode -ne 0) {
        $cachedImage = Invoke-Native -FilePath $dockerCommand.Source -Arguments @('image', 'inspect', 'mysql:8.0') -Step 'Check cached MySQL image' -AllowedExitCodes @(0, 1) -Quiet
        if ($cachedImage.ExitCode -ne 0) {
            throw 'FAILED: Could not pull mysql:8.0 and no cached image is available.'
        }
        Write-Host 'MySQL image pull failed, using the cached mysql:8.0 image.'
    }

    $currentStep = 'Maven mirror'
    Write-Host "==> $currentStep"
    $mavenDirectory = Join-Path $HOME '.m2'
    $settingsPath = Join-Path $mavenDirectory 'settings.xml'
    if (-not (Test-Path -LiteralPath $mavenDirectory)) {
        New-Item -Path $mavenDirectory -ItemType Directory -Force | Out-Null
    }

    $hasMirror = $false
    if (Test-Path -LiteralPath $settingsPath) {
        $hasMirror = [bool](Select-String -LiteralPath $settingsPath -SimpleMatch '<mirror>' -Quiet)
    }
    if (-not $hasMirror) {
        Write-Host 'Maven Central rate-limits cloud VMs, configuring the GCS mirror.'
        if (Test-Path -LiteralPath $settingsPath) {
            Copy-Item -LiteralPath $settingsPath -Destination "$settingsPath.bak" -Force
        }
        $settingsXml = @'
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>gcs-central</id>
      <name>Google Cloud Storage Maven Central mirror</name>
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
'@
        Set-Content -LiteralPath $settingsPath -Value $settingsXml -Encoding UTF8
    }

    $env:MVNW_REPOURL = 'https://maven-central.storage-download.googleapis.com/maven2'
    $wrapperPath = '.\mvnw.cmd'
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot 'mvnw.cmd'))) {
        throw 'FAILED: mvnw.cmd is missing from the repository.'
    }

    $currentStep = 'Maven dependency pre-warm'
    Write-Host "==> $currentStep"
    $null = Invoke-Native -FilePath $wrapperPath -Arguments @('-B', '-q', 'dependency:go-offline') -Step $currentStep

    $currentStep = 'Maven package build'
    Write-Host "==> $currentStep"
    $null = Invoke-Native -FilePath $wrapperPath -Arguments @('-B', '-q', '-DskipTests', 'package') -Step $currentStep
    $jarPath = Join-Path $repoRoot 'target\bankapp-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw 'FAILED: The package build did not create target/bankapp-0.0.1-SNAPSHOT.jar.'
    }

    $currentStep = 'Maven offline package build'
    Write-Host "==> $currentStep"
    $null = Invoke-Native -FilePath $wrapperPath -Arguments @('-B', '-q', '-o', '-DskipTests', 'package') -Step $currentStep

    $currentStep = 'Playwright dependencies'
    Write-Host "==> $currentStep"
    $npmCommand = Get-Command 'npm.cmd' -ErrorAction Stop
    $npxCommand = Get-Command 'npx.cmd' -ErrorAction Stop
    Push-Location (Join-Path $repoRoot 'e2e')
    try {
        $null = Invoke-Native -FilePath $npmCommand.Source -Arguments @('ci') -Step 'Install npm dependencies'
        $null = Invoke-Native -FilePath $npxCommand.Source -Arguments @('playwright', 'install', 'chromium') -Step 'Install Playwright Chromium'
    }
    finally {
        Pop-Location
    }

    $currentStep = 'Initial demo reset'
    Write-Host "==> $currentStep"
    & (Join-Path $repoRoot 'demo\start-demo.ps1')

    $currentStep = 'Playwright smoke suite'
    Write-Host "==> $currentStep"
    $previousJsonOutput = $env:PLAYWRIGHT_JSON_OUTPUT_NAME
    $env:PLAYWRIGHT_JSON_OUTPUT_NAME = 'results.json'
    Push-Location (Join-Path $repoRoot 'e2e')
    try {
        $null = Invoke-Native -FilePath $npxCommand.Source -Arguments @('playwright', 'test', '--reporter=dot,html,json') -Step $currentStep
    }
    finally {
        Pop-Location
        if ($null -eq $previousJsonOutput) {
            Remove-Item Env:PLAYWRIGHT_JSON_OUTPUT_NAME -ErrorAction SilentlyContinue
        }
        else {
            $env:PLAYWRIGHT_JSON_OUTPUT_NAME = $previousJsonOutput
        }
    }

    $currentStep = 'Playwright result count'
    Write-Host "==> $currentStep"
    $resultsPath = Join-Path $repoRoot 'e2e\results.json'
    $playwrightStats = (Get-Content -LiteralPath $resultsPath -Raw | ConvertFrom-Json).stats
    if ($playwrightStats.expected -ne 54 -or $playwrightStats.unexpected -ne 0) {
        throw "FAILED: Expected 54 passed and 0 failed, got $($playwrightStats.expected) passed and $($playwrightStats.unexpected) failed."
    }
    Write-Host "Playwright results: $($playwrightStats.expected) passed, $($playwrightStats.unexpected) failed"

    $currentStep = 'Coverage matrix generation'
    Write-Host "==> $currentStep"
    $matrixPath = Join-Path $repoRoot 'qa\coverage-matrix.md'
    $temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ([Guid]::NewGuid().ToString())
    New-Item -Path $temporaryDirectory -ItemType Directory -Force | Out-Null
    $matrixBackup = Join-Path $temporaryDirectory 'coverage-matrix.md'
    Copy-Item -LiteralPath $matrixPath -Destination $matrixBackup

    Push-Location (Join-Path $repoRoot 'e2e')
    try {
        $null = Invoke-Native -FilePath $npmCommand.Source -Arguments @('run', 'matrix') -Step $currentStep
    }
    finally {
        Pop-Location
    }

    $matrixContent = Get-Content -LiteralPath $matrixPath -Raw
    if ($matrixContent -notmatch '(?m)^- Unmapped: 0$' -or $matrixContent -notmatch '(?m)^- Manual cases: 60$') {
        throw 'FAILED: Coverage matrix must show 60 manual cases and 0 unmapped cases.'
    }

    $gitCommand = Get-Command 'git.exe' -ErrorAction Stop
    $matrixDiff = Invoke-Native -FilePath $gitCommand.Source -Arguments @('diff', '--quiet', 'HEAD', '--', 'qa/coverage-matrix.md') -Step 'Compare coverage matrix' -AllowedExitCodes @(0, 1) -Quiet
    if ($matrixDiff.ExitCode -ne 0) {
        throw 'FAILED: Generated coverage matrix differs from the committed file.'
    }
    Copy-Item -LiteralPath $matrixBackup -Destination $matrixPath -Force

    $currentStep = 'Final demo reset'
    Write-Host "==> $currentStep"
    & (Join-Path $repoRoot 'demo\start-demo.ps1')
}
catch {
    $setupError = $_.Exception.Message
}
finally {
    if ($matrixBackup -and (Test-Path -LiteralPath $matrixBackup)) {
        try {
            Copy-Item -LiteralPath $matrixBackup -Destination (Join-Path $repoRoot 'qa\coverage-matrix.md') -Force
        }
        catch {
            if (-not $setupError) {
                $setupError = "Could not restore qa/coverage-matrix.md: $($_.Exception.Message)"
            }
        }
    }
    if ($temporaryDirectory -and (Test-Path -LiteralPath $temporaryDirectory)) {
        try {
            Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
        }
        catch {
            if (-not $setupError) {
                $setupError = "Could not remove the temporary setup directory: $($_.Exception.Message)"
            }
        }
    }
}

if ($setupError) {
    if ($setupError -notmatch '^FAILED:') {
        $setupError = "FAILED: $currentStep. $setupError"
    }
    [Console]::Error.WriteLine($setupError)
    exit 1
}

$elapsed = [int][Math]::Floor(((Get-Date) - $startedAt).TotalSeconds)
Write-Host "Setup completed in ${elapsed}s."
Write-Host '=================================================='
Write-Host 'READY: Manual QA plan to automated Playwright suite'
Write-Host 'First command:  cd e2e && npx playwright test'
Write-Host 'First line:     "This is the bank''s 60-row manual regression plan. Devin turned every row into a tracked outcome."'
Write-Host '=================================================='
