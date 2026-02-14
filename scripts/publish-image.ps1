[CmdletBinding()]
param(
  [string]$AppModule = "platform-sample-app",
  [int]$JavaVersion = 21,

  [Parameter(Mandatory = $true)]
  [string]$ImageRepo,

  [string]$ImageTag,
  [switch]$Push,
  [string]$DockerContext = $env:DOCKER_CONTEXT,

  [string]$ServiceName = "platform-sample",
  [string]$Namespace = "default",
  [string]$Release
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

function ExecNative([string]$file, [string[]]$argv) {
  & $file @argv | Out-Host
  RequireLastExitCode "$file $($argv -join ' ')"
}

RequireCommand mvn
RequireCommand docker

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot
try {
  if ([string]::IsNullOrWhiteSpace($ImageTag)) {
    if (Get-Command git -ErrorAction SilentlyContinue) {
      $sha = (git rev-parse --short HEAD 2>$null)
      if ([string]::IsNullOrWhiteSpace($sha)) { $sha = "dev" }
      $ImageTag = "dev-$sha"
    } else {
      $ImageTag = "dev"
    }
  }
  if ([string]::IsNullOrWhiteSpace($Release)) {
    $Release = $ServiceName
  }

  $image = "$ImageRepo`:$ImageTag"

  $docker = @("docker")
  if (-not [string]::IsNullOrWhiteSpace($DockerContext)) {
    $docker += @("--context", $DockerContext)
  }

  Write-Host "Publish image"
  Write-Host "  AppModule:   $AppModule"
  Write-Host "  JavaVersion: $JavaVersion"
  Write-Host "  Image:       $image"
  Write-Host "  Push:        $Push"
  Write-Host "  Context:     $(if ([string]::IsNullOrWhiteSpace($DockerContext)) { '<default>' } else { $DockerContext })"

  Write-Host ""
  Write-Host "[1/3] Build app jar..."
  ExecNative mvn @("-q", "-pl", $AppModule, "-am", "-DskipTests", "package")

  $jar = Get-ChildItem (Join-Path $repoRoot "$AppModule\\target") -Filter "$AppModule-*.jar" |
    Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

  if ($null -eq $jar) {
    throw "Jar not found under $AppModule/target"
  }

  $jarRel = "$AppModule/target/$($jar.Name)"
  Write-Host "  Jar: $jarRel"

  Write-Host ""
  Write-Host "[2/3] Build image..."
  & $docker build `
    -f (Join-Path $repoRoot "platform-deploy\\docker\\Dockerfile.jvm") `
    --build-arg "JAVA_VERSION=$JavaVersion" `
    --build-arg "JAR_FILE=$jarRel" `
    -t $image `
    $repoRoot | Out-Host
  RequireLastExitCode "docker build"

  $repoDigest = ""
  if ($Push) {
    Write-Host ""
    Write-Host "[3/3] Push image..."
    & $docker push $image | Out-Host
    RequireLastExitCode "docker push"

    $repoDigest = (& $docker image inspect --format '{{index .RepoDigests 0}}' $image 2>$null) | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($repoDigest)) {
      & $docker pull $image | Out-Null
      $repoDigest = (& $docker image inspect --format '{{index .RepoDigests 0}}' $image 2>$null) | Select-Object -First 1
    }
  } else {
    Write-Host ""
    Write-Host "[3/3] Push skipped (use -Push)."
  }

  Write-Host ""
  Write-Host "Result:"
  Write-Host "  Image(tag):    $image"
  if (-not [string]::IsNullOrWhiteSpace($repoDigest)) {
    Write-Host "  Image(digest): $repoDigest"
  }

  Write-Host ""
  Write-Host "Deploy examples:"
  Write-Host "  ./platform-deploy/deploy.ps1 -Mode docker -ServiceName $ServiceName -Image $image"
  Write-Host "  ./platform-deploy/deploy.ps1 -Mode swarm -ServiceName $ServiceName -Image $image"
  Write-Host "  ./platform-deploy/deploy.ps1 -Mode k8s -ServiceName $ServiceName -Image $image -Namespace $Namespace -Release $Release"
  if (-not [string]::IsNullOrWhiteSpace($repoDigest)) {
    Write-Host ""
    Write-Host "Recommended (immutable digest):"
    Write-Host "  ./platform-deploy/deploy.ps1 -Mode k8s -ServiceName $ServiceName -Image $repoDigest -Namespace $Namespace -Release $Release"
    Write-Host "  ./platform-deploy/deploy.ps1 -Mode swarm -ServiceName $ServiceName -Image $repoDigest"
  }
} finally {
  Pop-Location
}
