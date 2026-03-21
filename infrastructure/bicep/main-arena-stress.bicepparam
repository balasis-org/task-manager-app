using './main.bicep'

// ── Arena Stress Testing Deployment ──────────────────────────
// Deploys the arena environment in STRESS mode.
// See main-arena.bicepparam.example for the full template.
//   - Spring profile: prod-arena-stress,DataLoader
//   - WAF IP rate limits: disabled (k6 shares a single source IP)
//   - BlockAllTraffic: enabled (tester adds own IPv4 + IPv6 /64 whitelist manually)

// Core
param location = 'italynorth'
param cognitiveServicesLocation = 'westeurope'
param projectName = 'taskmanager-arena'
param tags = {
  project: 'MyTeamTasks-Arena'
  managedBy: 'bicep'
}

// SQL
param sqlAdminLogin = 'taskmanageradmin'
param sqlAdminPassword = readEnvironmentVariable('SQL_ADMIN_PASSWORD', '')
param sqlDatabaseName = 'taskmanagerdb'

// App Service
param appServicePlanName = 'taskmanager-arena-plan'
param appServiceInstanceCount = 2

// Easy Auth
param authAppClientId = readEnvironmentVariable('ARENA_AUTH_CLIENT_ID', '')
param authAppTenantId = readEnvironmentVariable('ARENA_AUTH_TENANT_ID', '')

// Deployer RBAC (get your object ID: az ad signed-in-user show --query id -o tsv)
param deployerPrincipalId = readEnvironmentVariable('DEPLOYER_PRINCIPAL_ID', '')

// ── Arena-specific overrides ─────────────────────────────────
param springProfilesActive = 'prod-arena-stress,DataLoader'
param enableWafBlockAll = true
param enableEmailServices = false       // NoOp email beans — ACS not needed
param wafGlobalRateLimit = 0
param wafAuthRateLimit = 0
param enableObservabilityAlerts = false

// Stress header auth — WAF allows requests with matching X-Stress-Key + X-Stress-Nonce headers.
// Generate secrets: $key = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 32 | % {[char]$_})
// Set env vars: $env:STRESS_PASSPHRASE_KEY = $key; $env:STRESS_PASSPHRASE_NONCE = $nonce
// Give both values to the stress tester for k6 --env flags.
param stressPassphraseKey = readEnvironmentVariable('STRESS_PASSPHRASE_KEY', '')
param stressPassphraseNonce = readEnvironmentVariable('STRESS_PASSPHRASE_NONCE', '')

// Dev-auth app-level gate — k6 sends this as X-Dev-Auth-Key header.
// The same value must be passed to k6 via --env DEV_AUTH_KEY=<value>.
param devAuthSecret = readEnvironmentVariable('DEV_AUTH_SECRET', '')
