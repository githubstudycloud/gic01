[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$NewGroupId,

  [string]$OldGroupId = "com.test.platform",

  [string]$NewBasePackage,

  [string]$OldBasePackage = "com.test.platform",

  [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($NewBasePackage)) {
  $NewBasePackage = $NewGroupId
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host "Repo: $repoRoot"
Write-Host "groupId:      $OldGroupId  ->  $NewGroupId"
Write-Host "base package: $OldBasePackage  ->  $NewBasePackage"
if ($DryRun) {
  Write-Host "Mode: DRY RUN (no files will be modified)"
}

function WriteAllTextUtf8NoBom([string]$path, [string]$text) {
  $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($path, $text, $utf8NoBom)
}

$textFileExtensions = @(
  ".xml", ".java", ".kt", ".kts", ".gradle", ".md", ".properties", ".yml", ".yaml", ".toml"
)

$files =
  Get-ChildItem -Path $repoRoot -Recurse -File -Force |
  Where-Object {
    $full = $_.FullName
    if ($full -match "\\\\target\\\\") { return $false }
    if ($full -match "\\\\platform-thirdparty-repo\\\\") { return $false } # avoid vendor binaries/metadata surprises
    return $textFileExtensions -contains $_.Extension.ToLowerInvariant()
  }

$changed = 0
foreach ($file in $files) {
  $path = $file.FullName
  $original = [System.IO.File]::ReadAllText($path)

  $updated = $original
  if ($OldBasePackage -ne $NewBasePackage) {
    $updated = $updated.Replace($OldBasePackage, $NewBasePackage)
  }
  if ($OldGroupId -ne $NewGroupId) {
    $updated = $updated.Replace($OldGroupId, $NewGroupId)
  }

  if ($updated -ne $original) {
    $changed++
    if ($DryRun) {
      Write-Host "Would update: $path"
    } else {
      WriteAllTextUtf8NoBom -path $path -text $updated
      Write-Host "Updated: $path"
    }
  }
}

function MoveJavaPackageFolders([string]$sourceRoot) {
  $oldParts = $OldBasePackage.Split(".")
  $newParts = $NewBasePackage.Split(".")

  $oldRel = [System.IO.Path]::Combine($oldParts)
  $newRel = [System.IO.Path]::Combine($newParts)

  $oldDir = Join-Path $sourceRoot $oldRel
  if (!(Test-Path -LiteralPath $oldDir)) {
    return
  }

  $newDir = Join-Path $sourceRoot $newRel
  $newParent = Split-Path -Parent $newDir
  if (!$DryRun) {
    New-Item -ItemType Directory -Force -Path $newParent | Out-Null
  }

  if ($DryRun) {
    Write-Host "Would move: $oldDir -> $newDir"
  } else {
    Move-Item -LiteralPath $oldDir -Destination $newDir
    Write-Host "Moved: $oldDir -> $newDir"
  }
}

$javaRoots =
  Get-ChildItem -Path $repoRoot -Recurse -Directory -Force |
  Where-Object { $_.FullName -match "\\\\src\\\\(main|test)\\\\java$" }

foreach ($root in $javaRoots) {
  MoveJavaPackageFolders -sourceRoot $root.FullName
}

Write-Host "Done. Files changed: $changed"

