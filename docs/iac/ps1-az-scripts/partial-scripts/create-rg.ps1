param(
  [string]$Name     = $Env:RG_NAME,
  [string]$Location = $Env:LOCATION
)

if (-not $Name)     { Write-Error "RG_NAME not set"; exit 1 }
if (-not $Location) { Write-Error "LOCATION not set"; exit 1 }
if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
  Write-Error "Azure CLI (az) not found"
  exit 1
}

$exists = az group exists --name $Name | ConvertFrom-Json
if ($exists) {
  Write-Host "Resource group '$Name' already exists - skipping."
} else {
  Write-Host "Creating resource group '$Name' in '$Location'..."
  az group create --name $Name --location $Location
}
