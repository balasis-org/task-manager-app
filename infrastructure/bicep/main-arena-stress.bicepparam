using './main.bicep'

// ── Arena Stress Testing Deployment ──────────────────────────
// Deploys the arena environment in STRESS mode.
// See main-arena.bicepparam.example for the full template.
//   - Spring profile: prod-arena-stress,DataLoader
//   - WAF IP rate limits: disabled (k6 shares a single source IP)
//   - BlockAllTraffic: enabled (tester adds own IP whitelist manually)

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
param wafGlobalRateLimit = 0
param wafAuthRateLimit = 0
param enableObservabilityAlerts = false
