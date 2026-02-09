[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$Script,

  [string]$BaseUrl = "http://localhost:8080",
  [int]$Vus = 10,
  [string]$Duration = "15s",

  [string]$K6Image = $env:PLATFORM_K6_IMAGE
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function RequireCommand([string]$name) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
    throw "Required command not found in PATH: $name"
  }
}

RequireCommand docker

if ([string]::IsNullOrWhiteSpace($K6Image)) {
  # Pin a version for reproducibility. Override with PLATFORM_K6_IMAGE if needed.
  $K6Image = "grafana/k6:0.49.0"
}

$scriptsDir = Join-Path $PSScriptRoot "k6"
$scriptPath = Join-Path $scriptsDir "$Script.js"

if (-not (Test-Path $scriptPath)) {
  throw "k6 script not found: $scriptPath"
}

Write-Host "Running k6:"
Write-Host "  Image:    $K6Image"
Write-Host "  Script:   $Script"
Write-Host "  BaseUrl:  $BaseUrl"
Write-Host "  VUs:      $Vus"
Write-Host "  Duration: $Duration"

docker run --rm `
  -v "${scriptsDir}:/scripts:ro" `
  -e "BASE_URL=$BaseUrl" `
  -e "VUS=$Vus" `
  -e "DURATION=$Duration" `
  $K6Image run "/scripts/$Script.js"
if ($LASTEXITCODE -ne 0) {
  throw "k6 failed with exit code $LASTEXITCODE"
}
