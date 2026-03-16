#!/usr/bin/env pwsh
# Uploads default-images/ to Azure Blob Storage.
#   Local  (Azurite):  ./push-default-images.ps1 local
#   Remote (Azure):    ./push-default-images.ps1 remote -StorageAccount <name>

param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('local', 'remote')]
    [string]$Target,

    [Parameter()]
    [string]$StorageAccount
)

$ErrorActionPreference = 'Stop'

$repoRoot  = Split-Path -Parent $PSScriptRoot
$imageDir  = Join-Path $repoRoot 'default-images'
$container = 'default-images'

if (-not (Test-Path $imageDir)) {
    Write-Error "Image folder not found: $imageDir"
}

if ($Target -eq 'local') {
    # azurite running in Docker, credentials match backend-blob-compose.yml
    $conn = 'DefaultEndpointsProtocol=http;AccountName=account1;AccountKey=key1;BlobEndpoint=http://127.0.0.1:10000/account1;'

    az storage container create `
        --name $container `
        --connection-string $conn 2>$null | Out-Null

    az storage blob upload-batch `
        --source $imageDir `
        --destination $container `
        --connection-string $conn `
        --overwrite
}
else {
    if (-not $StorageAccount) {
        Write-Error 'Remote target requires -StorageAccount <name>.'
    }

    # verify or trigger az login(no secrets stored)
    $acct = az account show 2>$null | ConvertFrom-Json
    if (-not $acct) {
        Write-Host 'Not logged in — opening az login...'
        az login
    }
    else {
        Write-Host "Logged in as $($acct.user.name) ($($acct.name))"
    }

    az storage container create `
        --name $container `
        --account-name $StorageAccount `
        --auth-mode login 2>$null | Out-Null

    az storage blob upload-batch `
        --source $imageDir `
        --destination $container `
        --account-name $StorageAccount `
        --auth-mode login `
        --overwrite
}

Write-Host "Done — uploaded default images to '$container' ($Target)."
