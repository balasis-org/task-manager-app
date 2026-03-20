using './main.bicep'

// ── Arena Security Testing Deployment ────────────────────────
// Deploys the arena environment in SECURITY mode.
// See main-arena.bicepparam.example for the full template.
//   - Spring profile: prod-arena-security,DataLoader
//   - WAF rate limits: PRODUCTION values (WAF is tested surface)
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

// ── Arena-security-specific overrides ────────────────────────
param springProfilesActive = 'prod-arena-security,DataLoader'
param enableWafBlockAll = true
param enableFdCaching = false          // disable CDN caching — security tests must verify raw origin responses
param wafGlobalRateLimit = 2000    // production value — WAF is tested surface
param wafAuthRateLimit = 200       // production value — WAF is tested surface
param enableObservabilityAlerts = false
