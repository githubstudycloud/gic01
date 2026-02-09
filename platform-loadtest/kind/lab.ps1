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

RequireCommand kind
RequireCommand docker
RequireCommand kubectl
RequireCommand helm
RequireCommand mvn

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..")).Path
$kindConfig = Join-Path $PSScriptRoot "kind-config.yaml"
$chartPath = Join-Path $repoRoot "platform-deploy\\helm\\platform-service"
$dockerfile = Join-Path $repoRoot "platform-deploy\\docker\\Dockerfile.jvm"

$clusters = kind get clusters
if ($clusters -notcontains $ClusterName) {
  Write-Host "Creating kind cluster: $ClusterName"
  kind create cluster --name $ClusterName --config $kindConfig | Out-Host
} else {
  Write-Host "Kind cluster already exists: $ClusterName"
}

Write-Host "Building sample app jar..."
mvn -q -pl platform-sample-app -DskipTests package

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

Write-Host "Loading image into kind: $image"
kind load docker-image $image --name $ClusterName | Out-Host

$repo = "platform-sample-app"
$tag = "local"

Write-Host "Deploying via Helm: $Release ($repo:$tag) ns=$Namespace replicas=$Replicas"
helm upgrade --install $Release $chartPath `
  -n $Namespace --create-namespace `
  --set "replicaCount=$Replicas" `
  --set "image.repository=$repo" `
  --set "image.tag=$tag" `
  --set "service.port=8080" | Out-Host

kubectl rollout status "deployment/$Release" -n $Namespace --timeout "120s" | Out-Host

Write-Host ""
Write-Host "Next (port-forward the service):"
Write-Host "  kubectl -n $Namespace port-forward svc/$Release 8080:8080"

