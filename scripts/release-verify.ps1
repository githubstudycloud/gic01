[CmdletBinding()]
param(
  [ValidateSet("fast", "standard", "full")]
  [string]$Level = "standard",

  [int]$JavaVersion = 21,
  [int]$HostPort = 18080,

  [switch]$SkipDocker,
  [switch]$SkipK6,
  [switch]$SkipPython
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function RequireCommand([string]$name) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
    throw "Required command not found in PATH: $name"
  }
}

function ExecNative([string]$file, [string[]]$args) {
  & $file @args
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code $LASTEXITCODE: $file $($args -join ' ')"
  }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$deployScript = Join-Path $repoRoot "platform-deploy\\deploy.ps1"
$k6Script = Join-Path $repoRoot "platform-loadtest\\run-k6.ps1"
$depsAudit = Join-Path $repoRoot "scripts\\deps-age-audit.ps1"

RequireCommand mvn

Write-Host "Release verify"
Write-Host "  Level:      $Level"
Write-Host "  Java:       $JavaVersion"
Write-Host "  HostPort:   $HostPort"
Write-Host "  SkipDocker: $SkipDocker"
Write-Host "  SkipK6:     $SkipK6"
Write-Host "  SkipPython: $SkipPython"

Push-Location $repoRoot
try {
  if ($Level -eq "fast") {
    Write-Host ""
    Write-Host "[1/2] Maven unit tests..."
    ExecNative mvn @("-q", "test")

    Write-Host ""
    Write-Host "[2/2] Dependency age audit (direct runtime deps)..."
    & $depsAudit
    if ($LASTEXITCODE -ne 0) {
      throw "Dependency age audit failed with exit code $LASTEXITCODE"
    }
    return
  }

  Write-Host ""
  Write-Host "[1/5] Maven verify (unit + IT)..."
  ExecNative mvn @("-q", "-Pit", "verify")

  Write-Host ""
  Write-Host "[2/5] Dependency age audit (direct runtime deps)..."
  & $depsAudit
  if ($LASTEXITCODE -ne 0) {
    throw "Dependency age audit failed with exit code $LASTEXITCODE"
  }

  if ($SkipDocker) {
    Write-Host ""
    Write-Host "Docker steps skipped."
    return
  }

  RequireCommand docker

  $serviceName = "platform-sample-verify"
  $baseUrl = "http://localhost:$HostPort"

  # Ensure we have a sample app jar to build the Docker image from.
  $jar = Get-ChildItem (Join-Path $repoRoot "platform-sample-app\\target") -Filter "platform-sample-app-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

  if ($null -eq $jar) {
    Write-Host ""
    Write-Host "[3/5] Building sample app jar..."
    ExecNative mvn @("-q", "-pl", "platform-sample-app", "-DskipTests", "package")
    $jar = Get-ChildItem (Join-Path $repoRoot "platform-sample-app\\target") -Filter "platform-sample-app-*.jar" |
      Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
      Sort-Object LastWriteTime -Descending |
      Select-Object -First 1
  }
  if ($null -eq $jar) {
    throw "Sample app jar not found under platform-sample-app/target"
  }

  try {
    Write-Host ""
    Write-Host "[3/5] Docker deploy + readiness gate..."
    & $deployScript -Mode docker -ServiceName $serviceName -JarPath "platform-sample-app/target/$($jar.Name)" -HostPort $HostPort -JavaVersion $JavaVersion

    if (-not $SkipK6) {
      Write-Host ""
      Write-Host "[4/5] k6 smoke..."
      & $k6Script -Script ping-smoke -BaseUrl $baseUrl -Vus 10 -Duration "10s"
      & $k6Script -Script lock-contention -BaseUrl $baseUrl -Vus 20 -Duration "10s"
    } else {
      Write-Host ""
      Write-Host "[4/5] k6 skipped."
    }

    if (-not $SkipPython) {
      RequireCommand python
      Write-Host ""
      Write-Host "[5/5] Python black-box tests..."

      Push-Location (Join-Path $repoRoot "platform-test-python")
      try {
        $venvPy = Join-Path (Get-Location) ".venv\\Scripts\\python.exe"
        if (-not (Test-Path $venvPy)) {
          ExecNative python @("-m", "venv", ".venv")
          ExecNative $venvPy @("-m", "pip", "install", "-r", "requirements.txt")
        }

        $env:PLATFORM_BASE_URL = $baseUrl
        ExecNative $venvPy @("-m", "pytest")
      } finally {
        Pop-Location
      }
    } else {
      Write-Host ""
      Write-Host "[5/5] Python tests skipped."
    }

    if ($Level -eq "full") {
      Write-Host ""
      Write-Host "Full level note: for cluster verification, run:"
      Write-Host "  ./platform-loadtest/kind/lab.ps1"
      Write-Host "  ./platform-loadtest/run-k6.ps1 -Script ping-smoke -BaseUrl http://localhost:8080 ..."
    }
  } finally {
    docker rm -f $serviceName 2>$null | Out-Null
  }
} finally {
  Pop-Location
}
