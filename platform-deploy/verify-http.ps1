[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$Url,

  [int]$TimeoutSeconds = 60,
  [int]$IntervalSeconds = 2
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
  try {
    $response = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec 5
    if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
      Write-Host "OK: $Url ($($response.StatusCode))"
      exit 0
    }
  } catch {
    # keep retrying until timeout
  }
  Start-Sleep -Seconds $IntervalSeconds
}

throw "Health check failed after $TimeoutSeconds seconds: $Url"

