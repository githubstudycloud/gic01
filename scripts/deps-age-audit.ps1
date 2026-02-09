[CmdletBinding()]
param(
  [int]$MaxAgeYears = 3,
  [switch]$IncludeTransitive,
  [switch]$Vendor
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function RequireCommand([string]$name) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
    throw "Required command not found in PATH: $name"
  }
}

RequireCommand mvn
RequireCommand python

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script = Join-Path $PSScriptRoot "deps-age-audit.py"

$argsList = @(
  $script,
  "--max-age-years", "$MaxAgeYears"
)
if ($IncludeTransitive) { $argsList += "--include-transitive" }
if ($Vendor) { $argsList += "--vendor" }

Write-Host "Running dependency age audit..."
Write-Host "  MaxAgeYears:        $MaxAgeYears"
Write-Host "  IncludeTransitive:  $IncludeTransitive"
Write-Host "  Vendor:             $Vendor"

python @argsList
if ($LASTEXITCODE -ne 0) {
  throw "Dependency age audit failed with exit code $LASTEXITCODE"
}
