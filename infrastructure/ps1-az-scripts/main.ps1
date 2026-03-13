# main.ps1 - Orchestrator: load env, login, create RG, deploy Bicep
# Usage: pwsh -File docs/iac/ps1-az-scripts/main.ps1
# Dry-run: pwsh -File docs/iac/ps1-az-scripts/main.ps1 -WhatIf

param(
  [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'

# 1. Load environment variables
. (Join-Path $PSScriptRoot 'partial-scripts\load-envs.ps1')

# 2. Login with service principal
if (-not $Env:AZ_SP_ID -or -not $Env:AZ_SP_SECRET -or -not $Env:AZ_TENANT) {
  Write-Error "Missing AZ_SP_ID, AZ_SP_SECRET, or AZ_TENANT in .env"
  exit 1
}

Write-Host "Logging in as service principal..."
az login --service-principal `
  --username $Env:AZ_SP_ID `
  --password $Env:AZ_SP_SECRET `
  --tenant   $Env:AZ_TENANT

if ($Env:AZ_SUBSCRIPTION) {
  az account set --subscription $Env:AZ_SUBSCRIPTION
}

# 3. Create resource group (idempotent)
. (Join-Path $PSScriptRoot 'partial-scripts\create-rg.ps1')

# 4. Deploy Bicep template (or what-if dry run)
if ($WhatIf) {
  & (Join-Path $PSScriptRoot 'partial-scripts\deploy-bicep.ps1') -WhatIf
} else {
  . (Join-Path $PSScriptRoot 'partial-scripts\deploy-bicep.ps1')
}

Write-Host "Done."
