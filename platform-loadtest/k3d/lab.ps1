[CmdletBinding()]
param(
  [string]$ClusterName = "platform-lab",
  [string]$Namespace = "platform-lab",
  [string]$Release = "platform-sample-app",
  [int]$Replicas = 3,
  [int]$JavaVersion = 21,

  [switch]$RunSmoke,
  [int]$LocalPort = 18080,
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

function RequireLastExitCode([string]$what) {
  if ($LASTEXITCODE -ne 0) {
    throw "$what failed with exit code $LASTEXITCODE"
  }
}

RequireCommand k3d
RequireCommand docker
RequireCommand kubectl
RequireCommand helm
RequireCommand mvn

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
$chartPath = Join-Path $repoRoot "platform-deploy\\helm\\platform-service"
$dockerfile = Join-Path $repoRoot "platform-deploy\\docker\\Dockerfile.jvm"
$kubeContext = "k3d-$ClusterName"
$verifyHttp = Join-Path $repoRoot "platform-deploy\\verify-http.ps1"
$k6Script = Join-Path $repoRoot "platform-loadtest\\run-k6.ps1"

$clusterList = k3d cluster list | Out-String
if ($clusterList -notmatch "(?m)^$([regex]::Escape($ClusterName))\\s") {
  Write-Host "Creating k3d cluster: $ClusterName"
  k3d cluster create $ClusterName --agents 2 --servers 1 --wait | Out-Host
  RequireLastExitCode "k3d cluster create"
} else {
  Write-Host "k3d cluster already exists: $ClusterName"
}

Write-Host "Building sample app jar..."
mvn -q -pl platform-sample-app -DskipTests package
RequireLastExitCode "mvn package"

$jar = Get-ChildItem (Join-Path $repoRoot "platform-sample-app\\target") -Filter "platform-sample-app-*.jar" |
  Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1

if ($null -eq $jar) {
  throw "Sample app jar not found under platform-sample-app/target"
}

$image = "platform-sample-app:local"

Write-Host "Building docker image: $image"
docker build `
  -f $dockerfile `
  --build-arg "JAVA_VERSION=$JavaVersion" `
  --build-arg "JAR_FILE=platform-sample-app/target/$($jar.Name)" `
  -t $image `
  $repoRoot | Out-Host
RequireLastExitCode "docker build"

Write-Host "Importing image into k3d: $image"
k3d image import -c $ClusterName $image | Out-Host
RequireLastExitCode "k3d image import"

$repo = "platform-sample-app"
$tag = "local"

Write-Host "Deploying via Helm: $Release ($repo:$tag) ns=$Namespace replicas=$Replicas context=$kubeContext"
helm upgrade --install $Release $chartPath `
  --kube-context $kubeContext `
  -n $Namespace --create-namespace `
  --set "replicaCount=$Replicas" `
  --set "image.repository=$repo" `
  --set "image.tag=$tag" `
  --set "service.port=8080" | Out-Host
RequireLastExitCode "helm upgrade"

kubectl --context $kubeContext rollout status "deployment/$Release" -n $Namespace --timeout "120s" | Out-Host
RequireLastExitCode "kubectl rollout status"

Write-Host ""
Write-Host "Next (port-forward the service):"
Write-Host "  kubectl --context $kubeContext -n $Namespace port-forward svc/$Release $($LocalPort):8080"

if (-not $RunSmoke) {
  return
}

$baseUrl = "http://localhost:$LocalPort"
$healthUrl = "$baseUrl/actuator/health/readiness"

Write-Host ""
Write-Host "RUN_SMOKE enabled"
Write-Host "  BaseUrl:   $baseUrl"
Write-Host "  HealthUrl: $healthUrl"
Write-Host "  SkipK6:    $SkipK6"
Write-Host "  SkipPython:$SkipPython"

Write-Host ""
Write-Host "Starting port-forward..."
$pfOut = [System.IO.Path]::GetTempFileName()
$pfErr = [System.IO.Path]::GetTempFileName()
$pfArgs = @("--context", $kubeContext, "-n", $Namespace, "port-forward", "svc/$Release", "$($LocalPort):8080")
$pf = Start-Process -FilePath "kubectl" -ArgumentList $pfArgs -NoNewWindow -PassThru -RedirectStandardOutput $pfOut -RedirectStandardError $pfErr

try {
  & $verifyHttp -Url $healthUrl -TimeoutSeconds 120

  if (-not $SkipK6) {
    Write-Host ""
    Write-Host "k6 smoke..."
    & $k6Script -Script ping-smoke -BaseUrl $baseUrl -Vus 10 -Duration "10s"
  } else {
    Write-Host ""
    Write-Host "k6 skipped."
  }

  if (-not $SkipPython) {
    RequireCommand python

    Write-Host ""
    Write-Host "Python black-box tests..."
    Push-Location (Join-Path $repoRoot "platform-test-python")
    try {
      $venvPy = Join-Path (Get-Location) ".venv\\Scripts\\python.exe"
      if (-not (Test-Path $venvPy)) {
        python -m venv .venv
        RequireLastExitCode "python -m venv"
        & $venvPy -m pip install -r requirements.txt | Out-Host
        RequireLastExitCode "pip install"
      }

      $env:PLATFORM_BASE_URL = $baseUrl
      & $venvPy -m pytest | Out-Host
      RequireLastExitCode "pytest"
    } finally {
      Pop-Location
    }
  } else {
    Write-Host ""
    Write-Host "Python tests skipped."
  }
} finally {
  if ($null -ne $pf -and -not $pf.HasExited) {
    Stop-Process -Id $pf.Id -Force -ErrorAction SilentlyContinue
  }
  Remove-Item -Force -ErrorAction SilentlyContinue $pfOut, $pfErr
}
