[CmdletBinding()]
param(
  [ValidateSet("fast", "standard", "full")]
  [string]$Level = "standard",

  [ValidateSet("docker", "compose", "swarm", "k8s")]
  [string]$DeployMode = $(if ([string]::IsNullOrWhiteSpace($env:DEPLOY_MODE)) { "docker" } else { $env:DEPLOY_MODE }),

  [int]$JavaVersion = 21,
  [int]$HostPort = 18080,
  [string]$BaseUrl = $env:BASE_URL,
  [string]$HealthUrl = $env:HEALTH_URL,
  [string]$DockerContext = $env:DOCKER_CONTEXT,
  [string]$Image,
  [switch]$SkipCleanup,

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

function ExecNative([string]$file, [string[]]$argv) {
  & $file @argv
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code ${LASTEXITCODE}: $file $($argv -join ' ')"
  }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$deployScript = Join-Path $repoRoot "platform-deploy\\deploy.ps1"
$k6Script = Join-Path $repoRoot "platform-loadtest\\run-k6.ps1"
$depsAudit = Join-Path $repoRoot "scripts\\deps-age-audit.ps1"
$verifyHttp = Join-Path $repoRoot "platform-deploy\\verify-http.ps1"

RequireCommand mvn

$baseUrlInput = $BaseUrl
if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
  $BaseUrl = "http://localhost:$HostPort"
}
if ([string]::IsNullOrWhiteSpace($HealthUrl)) {
  $HealthUrl = "$BaseUrl/actuator/health/readiness"
}
$contextLabel = "<default>"
if (-not [string]::IsNullOrWhiteSpace($DockerContext)) {
  $contextLabel = $DockerContext
}

Write-Host "Release verify"
Write-Host "  Level:      $Level"
Write-Host "  DeployMode: $DeployMode"
Write-Host "  Java:       $JavaVersion"
Write-Host "  HostPort:   $HostPort"
Write-Host "  BaseUrl:    $BaseUrl"
Write-Host "  HealthUrl:  $HealthUrl"
Write-Host "  Context:    $contextLabel"
Write-Host "  Image:      $Image"
Write-Host "  Cleanup:    $(-not $SkipCleanup)"
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

  $needsDocker = ($DeployMode -ne "k8s") -or (-not $SkipK6)
  if ($needsDocker) {
    RequireCommand docker
  }

  $serviceName = "platform-sample-verify"
  $baseUrl = $BaseUrl

  if ($DeployMode -eq "k8s" -and [string]::IsNullOrWhiteSpace($baseUrlInput)) {
    throw "For -DeployMode k8s, provide -BaseUrl (ingress or port-forward URL) and -Image (registry image recommended)."
  }
  if ($DeployMode -eq "k8s" -and [string]::IsNullOrWhiteSpace($Image)) {
    throw "For -DeployMode k8s, provide -Image (registry image recommended)."
  }

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
    Write-Host "[3/5] Deploy ($DeployMode) + readiness gate..."
    if ($DeployMode -eq "k8s") {
      & $deployScript -Mode k8s -ServiceName $serviceName -Image $Image -DockerContext $DockerContext
      & $verifyHttp -Url $HealthUrl -TimeoutSeconds 120
    } else {
      & $deployScript -Mode $DeployMode -ServiceName $serviceName -JarPath "platform-sample-app/target/$($jar.Name)" -HostPort $HostPort -JavaVersion $JavaVersion -DockerContext $DockerContext -HealthUrl $HealthUrl
    }

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
      Write-Host "  ./platform-loadtest/k3d/lab.ps1"
      Write-Host "  ./platform-loadtest/run-k6.ps1 -Script ping-smoke -BaseUrl http://localhost:8080 ..."
    }
  } finally {
    if (-not $SkipCleanup) {
      $docker = @("docker")
      if (-not [string]::IsNullOrWhiteSpace($DockerContext)) {
        $docker += @("--context", $DockerContext)
      }

      switch ($DeployMode) {
        "docker" { & $docker rm -f $serviceName 2>$null | Out-Null }
        "compose" { & $docker compose -f (Join-Path $repoRoot "platform-deploy\\compose\\docker-compose.yml") --project-name $serviceName down -v 2>$null | Out-Null }
        "swarm" { & $docker stack rm $serviceName 2>$null | Out-Null }
        "k8s" {
          if (Get-Command helm -ErrorAction SilentlyContinue) {
            & helm uninstall $serviceName -n "default" 2>$null | Out-Null
          }
        }
      }
    }
  }
} finally {
  Pop-Location
}
