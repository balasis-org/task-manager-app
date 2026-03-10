param(
  [string]$ResourceGroup = $Env:RG_NAME,
  [string]$TemplateFile  = (Join-Path $PSScriptRoot '..\..\bicep\main.bicep'),
  [string]$ParamFile     = (Join-Path $PSScriptRoot '..\..\bicep\main.bicepparam'),
  [switch]$WhatIf
)

if (-not $ResourceGroup) { Write-Error "RG_NAME not set"; exit 1 }

if (-not (Test-Path $TemplateFile)) {
  Write-Error "Bicep template not found: $TemplateFile"
  exit 1
}

$params = @(
  '--resource-group', $ResourceGroup,
  '--template-file',  $TemplateFile
)

if (Test-Path $ParamFile) {
  # .bicepparam uses 'using' directive — no @ prefix, and it implies the template
  $params = @(
    '--resource-group', $ResourceGroup,
    '--parameters',     $ParamFile
  )
  Write-Host "Deploying with param file: $ParamFile"
} else {
  Write-Host "No param file found - deploying with defaults only."
}

# Secure params now handled via readEnvironmentVariable() in .bicepparam

if ($WhatIf) {
  Write-Host "Running what-if against resource group '$ResourceGroup'..."
  az deployment group what-if @params
} else {
  Write-Host "Deploying Bicep to resource group '$ResourceGroup'..."
  az deployment group create @params
}
