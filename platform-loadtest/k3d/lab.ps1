[CmdletBinding()]
param(
  [string]$ClusterName = "platform-lab",
  [string]$Namespace = "platform-lab",
  [string]$Release = "platform-sample-app",
  [int]$Replicas = 3,
  [int]$JavaVersion = 21
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
Write-Host "  kubectl --context $kubeContext -n $Namespace port-forward svc/$Release 8080:8080"

