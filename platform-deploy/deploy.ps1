[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [ValidateSet("docker", "compose", "k8s", "baremetal")]
  [string]$Mode,

  [Parameter(Mandatory = $true)]
  [string]$ServiceName,

  [string]$Image,
  [string]$JarPath,

  [int]$HostPort = 8080,
  [int]$ContainerPort = 8080,

  [int]$JavaVersion = 21,

  [string]$HealthUrl,
  [int]$HealthTimeoutSeconds = 60,

  [string]$Namespace = "default",
  [string]$Release,

  [string]$ChartPath = (Join-Path $PSScriptRoot "helm/platform-service"),
  [string]$ComposePath = (Join-Path $PSScriptRoot "compose/docker-compose.yml")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function RequireCommand([string]$name) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
    throw "Required command not found in PATH: $name"
  }
}

function GetRepoRoot() {
  return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function GetRelativePath([string]$basePath, [string]$targetPath) {
  $baseUri = [System.Uri]::new(($basePath.TrimEnd("\") + "\"))
  $targetUri = [System.Uri]::new((Resolve-Path $targetPath).Path)
  # Dockerfile COPY expects forward slashes even on Windows.
  return $baseUri.MakeRelativeUri($targetUri).ToString()
}

if ([string]::IsNullOrWhiteSpace($HealthUrl)) {
  $HealthUrl = "http://localhost:$HostPort/actuator/health/readiness"
}
if ([string]::IsNullOrWhiteSpace($Release)) {
  $Release = $ServiceName
}

if ([string]::IsNullOrWhiteSpace($Image) -and -not [string]::IsNullOrWhiteSpace($JarPath)) {
  RequireCommand docker
  $repoRoot = GetRepoRoot
  $dockerfile = Join-Path $PSScriptRoot "docker/Dockerfile.jvm"
  $imageTag = "$ServiceName:local"
  $jarRel = GetRelativePath -basePath $repoRoot -targetPath (Join-Path $repoRoot $JarPath)

  Write-Host "Building image: $imageTag"
  Write-Host "  Dockerfile: $dockerfile"
  Write-Host "  JAR:        $jarRel"
  docker build `
    -f $dockerfile `
    --build-arg "JAVA_VERSION=$JavaVersion" `
    --build-arg "JAR_FILE=$jarRel" `
    -t $imageTag `
    $repoRoot | Out-Host

  $Image = $imageTag
}

switch ($Mode) {
  "docker" {
    RequireCommand docker
    if ([string]::IsNullOrWhiteSpace($Image)) {
      throw "For -Mode docker, provide -Image or -JarPath."
    }

    Write-Host "Deploying via Docker: $ServiceName ($Image)"
    docker rm -f $ServiceName 2>$null | Out-Null
    docker run -d --name $ServiceName -p "$HostPort`:$ContainerPort" $Image | Out-Null

    & (Join-Path $PSScriptRoot "verify-http.ps1") -Url $HealthUrl -TimeoutSeconds $HealthTimeoutSeconds
  }

  "compose" {
    RequireCommand docker
    if ([string]::IsNullOrWhiteSpace($Image)) {
      throw "For -Mode compose, provide -Image or -JarPath."
    }

    $env:PLATFORM_IMAGE = $Image
    $env:PLATFORM_PORT = "$HostPort"

    Write-Host "Deploying via Docker Compose: $ServiceName ($Image)"
    docker compose -f $ComposePath --project-name $ServiceName up -d | Out-Host

    & (Join-Path $PSScriptRoot "verify-http.ps1") -Url $HealthUrl -TimeoutSeconds $HealthTimeoutSeconds
  }

  "k8s" {
    RequireCommand helm
    RequireCommand kubectl
    if ([string]::IsNullOrWhiteSpace($Image)) {
      throw "For -Mode k8s, provide -Image (a registry image is recommended)."
    }

    $repo = $Image
    $tag = "latest"
    if ($Image -match "^(.+):([^/]+)$") {
      $repo = $Matches[1]
      $tag = $Matches[2]
    }

    Write-Host "Deploying via Helm: $Release ($repo:$tag) ns=$Namespace"
    helm upgrade --install $Release $ChartPath `
      -n $Namespace --create-namespace `
      --set "image.repository=$repo" `
      --set "image.tag=$tag" `
      --set "service.port=$ContainerPort" | Out-Host

    kubectl rollout status "deployment/$Release" -n $Namespace --timeout "$HealthTimeoutSeconds`s" | Out-Host
    Write-Host "K8s rollout OK. (Optional) verify via your ingress or port-forward."
  }

  "baremetal" {
    $unit = Join-Path $PSScriptRoot "systemd/platform-service.service"
    Write-Host "Bare-metal template: $unit"
    Write-Host "Copy, edit placeholders, then: systemctl enable --now <service>"
  }
}
