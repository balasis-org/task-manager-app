// main.bicep — MyTeamTasks Azure Infrastructure
// Phase 1: Base (identity, monitoring, storage, SQL, Redis, KV, ACR, Content Safety, Text Analytics, ACS, RBAC)
// Phase 2: Compute (App Service Plan, Web App, Container App Jobs)
// Phase 3: Front Door (WAF, routes, rule sets)

// --- Parameters: Phase 1 ---

@description('Primary Azure region')
param location string

@description('Region for Cognitive Services — Content Safety + Text Analytics (limited regional availability)')
param cognitiveServicesLocation string = 'westeurope'

@description('Project name used as prefix for resource names')
param projectName string

@description('Common tags applied to all resources')
param tags object = {}

// SQL
@description('SQL Server administrator login')
param sqlAdminLogin string

@secure()
@description('SQL Server administrator password')
param sqlAdminPassword string

@description('SQL database name')
param sqlDatabaseName string

// NOTE: storageAccountName, redisName, keyVaultName, acrName are now auto-generated
// using uniqueString() to avoid "name already in use" errors. See the variables section.

// --- Parameters: Phase 2 (Compute) ---

@description('App Service Plan name')
param appServicePlanName string

// NOTE: webAppName is now auto-generated using uniqueString(). See the variables section.

@description('Number of App Service Plan instances')
param appServiceInstanceCount int = 2

// Easy Auth (App Service Authentication)
@description('Client ID of the TaskManager-Auth App Registration (manually created in Entra ID)')
param authAppClientId string

@description('Entra ID tenant ID for the TaskManager-Auth App Registration')
param authAppTenantId string

// --- Parameters: Phase 3 (Front Door) ---

// NOTE: frontDoorName is now auto-generated using uniqueString(). See the variables section.

@description('Custom domain hostname (e.g. www.myteamtasks.net). Leave empty to skip.')
param customDomainHost string = ''

@description('Blocks all traffic by default. For arena deploys the tester adds a higher-priority IP whitelist rule manually.')
param enableWafBlockAll bool = false

@description('Override image for Container App Jobs on first deploy (ACR is empty). Leave blank after CI/CD has pushed images.')
param containerImageOverride string = ''

@description('Comma-separated Spring Boot profiles (e.g., prod-azuresql or prod-arena-stress,DataLoader)')
param springProfilesActive string = 'prod-azuresql'

@description('Per-IP req/5min on /*. 0 disables the rule (for arena stress testing).')
param wafGlobalRateLimit int = 2000

@description('Per-IP req/5min on /api/auth/*. 0 disables the rule (for arena stress testing).')
param wafAuthRateLimit int = 200

@description('Enable Front Door CDN caching on static asset and image routes. Disable for security testing where unprocessed origin responses must be verified.')
param enableFdCaching bool = true

@description('Deploy email infrastructure (ACS + email services). Disable for arena — NoOp email beans make them unnecessary.')
param enableEmailServices bool = true

// --- Parameters: Stress Header Auth ---

@description('Value for X-Stress-Key header (WAF allow rule). Only used with stress profiles. Set via STRESS_PASSPHRASE_KEY env var.')
param stressPassphraseKey string = ''

@description('Value for X-Stress-Nonce header (WAF allow rule). Only used with stress profiles. Set via STRESS_PASSPHRASE_NONCE env var.')
param stressPassphraseNonce string = ''

// --- Parameters: Observability ---

@description('Email address for Azure alert notifications (budget, origin health). Leave empty to skip alert resources.')
param alertEmailAddress string = ''

@description('Monthly budget in the billing currency for this resource group. 0 = no budget alert deployed.')
param monthlyBudgetAmount int = 0

@description('Deploy observability alerts and diagnostics (budget, origin health, WAF logs). Disable for arena.')
param enableObservabilityAlerts bool = true

@description('Auto-filled start date for the monthly budget (1st of current month). Do not override.')
param budgetStartDate string = utcNow('yyyy-MM-01')

// --- Parameters: Deployer RBAC ---

@description('Object ID of the deploying user (az ad signed-in-user show --query id). Grants Key Vault Secrets Officer + Storage Blob Data Contributor so the deployer can seed secrets and push default images. Leave empty to skip.')
param deployerPrincipalId string = ''

@description('Object ID of the CI/CD service principal (GitHub Actions). Grants Storage Blob Data Contributor for frontend deployments. Leave empty to skip.')
param cicdPrincipalId string = ''

// --- Variables ---

var useCustomDomain = customDomainHost != ''

// Deterministic 13-char hash based on the resource group — guarantees globally unique names
var suffix = uniqueString(resourceGroup().id)

// Auto-generated globally unique names (no more "name already in use")
var storageAccountName = toLower('${take(projectName, 10)}${take(suffix, 13)}')
var redisName            = '${projectName}-redis-${take(suffix, 8)}'
var keyVaultName         = '${take(projectName, 14)}-kv-${take(suffix, 6)}'
var acrName              = toLower(replace('${take(projectName, 30)}${take(suffix, 13)}', '-', ''))
var webAppName           = '${projectName}-app-${take(suffix, 8)}'
var frontDoorName        = '${projectName}-fd-${take(suffix, 8)}'
var sqlServerName        = '${projectName}-sql-${take(suffix, 8)}'
var contentSafetyName    = '${projectName}-cs-${take(suffix, 8)}'
var textAnalyticsName    = '${projectName}-ta-${take(suffix, 8)}'
var wafPolicyName        = toLower(replace('${projectName}waf', '-', ''))

// Lowercase image prefix for OCI compliance (image refs must be all-lowercase)
var imagePrefix          = toLower('${acr.properties.loginServer}/${projectName}')

// Placeholder image used on first deploy before CI/CD has pushed actual images.
// ACA validates the image ref at resource-creation time, so we need a real pullable image.
// App Service pulls lazily and tolerates missing images, so backendImage can always point at ACR.
var backendImage         = '${imagePrefix}-backend:latest'
var maintenanceImage     = containerImageOverride != '' ? containerImageOverride : '${imagePrefix}-maintenance:latest'

// Front Door CDN cache configurations (null = caching disabled, property omitted from ARM)
var staticCacheConfig = enableFdCaching ? {
  queryStringCachingBehavior: 'IgnoreQueryString'
  compressionSettings: {
    isCompressionEnabled: true
    contentTypesToCompress: [
      'text/html'
      'text/css'
      'application/javascript'
      'text/javascript'
      'application/json'
      'text/plain'
      'image/svg+xml'
    ]
  }
} : null

var imageCacheConfig = enableFdCaching ? {
  queryStringCachingBehavior: 'IgnoreQueryString'
  compressionSettings: {
    isCompressionEnabled: false
  }
} : null

var blobContainers = [
  'task-files'
  'task-assignee-files'
  'profile-images'
  'group-images'
  'default-images'
  'web'
  'assets'
  'staticfrontend'
]

// 1. User-Assigned Managed Identity

resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: '${projectName}-identity'
  location: location
  tags: tags
}

// 2. Log Analytics Workspaces (one per environment — keeps telemetry isolated)

resource logAnalyticsProd 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${projectName}-logs-prod'
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    workspaceCapping: {
      dailyQuotaGb: json('0.28')
    }
  }
}

resource logAnalyticsDev 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: '${projectName}-logs-dev'
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    workspaceCapping: {
      dailyQuotaGb: json('0.2')
    }
  }
}

// 3. Application Insights (prod + dev)

resource appInsightsProd 'Microsoft.Insights/components@2020-02-02' = {
  name: '${projectName}-ai-prod'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalyticsProd.id
  }
}

resource appInsightsDev 'Microsoft.Insights/components@2020-02-02' = {
  name: '${projectName}-ai-dev'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalyticsDev.id
  }
}

// 4. Storage Account + Blob Containers (private, no soft-delete)

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: storageAccountName
  location: location
  tags: tags
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    accessTier: 'Hot'
    supportsHttpsTrafficOnly: true
    minimumTlsVersion: 'TLS1_2'
    allowBlobPublicAccess: false
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storageAccount
  name: 'default'
  properties: {
    deleteRetentionPolicy: {
      enabled: false
    }
    containerDeleteRetentionPolicy: {
      enabled: false
    }
  }
}

resource blobContainerResources 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = [
  for containerName in blobContainers: {
    parent: blobService
    name: containerName
    properties: {
      publicAccess: 'None'
    }
  }
]

// 5. Azure SQL Server + Database (S1 = 20 DTU)

resource sqlServer 'Microsoft.Sql/servers@2023-08-01-preview' = {
  name: sqlServerName
  location: location
  tags: tags
  properties: {
    administratorLogin: sqlAdminLogin
    administratorLoginPassword: sqlAdminPassword
    minimalTlsVersion: '1.2'
    publicNetworkAccess: 'Enabled'
  }
}

resource sqlFirewallAllowAzure 'Microsoft.Sql/servers/firewallRules@2023-08-01-preview' = {
  parent: sqlServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource sqlDatabase 'Microsoft.Sql/servers/databases@2023-08-01-preview' = {
  parent: sqlServer
  name: sqlDatabaseName
  location: location
  tags: tags
  sku: {
    name: 'S1'
    tier: 'Standard'
  }
  properties: {
    collation: 'SQL_Latin1_General_CP1_CI_AS'
  }
}

// 6. Azure Managed Redis (Balanced_B0, 0.5 GB)

resource redisEnterprise 'Microsoft.Cache/redisEnterprise@2024-10-01' = {
  name: redisName
  location: location
  tags: tags
  sku: {
    name: 'Balanced_B0'
  }
}

resource redisDatabase 'Microsoft.Cache/redisEnterprise/databases@2024-10-01' = {
  parent: redisEnterprise
  name: 'default'
  properties: {
    clientProtocol: 'Encrypted'
    evictionPolicy: 'NoEviction'
    clusteringPolicy: 'EnterpriseCluster'
    port: 10000
  }
}

// 7. Key Vault + Secrets

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  tags: tags
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    tenantId: subscription().tenantId
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 7
    enabledForDeployment: false
    enabledForTemplateDeployment: false
    enabledForDiskEncryption: false
  }
}

// Secrets pulled from other resources

resource kvSecretDbUrl 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-AZURE-DB-URL'
  properties: {
    value: 'jdbc:sqlserver://${sqlServer.properties.fullyQualifiedDomainName}:1433;database=${sqlDatabaseName};encrypt=true;trustServerCertificate=false;loginTimeout=30;'
  }
}

resource kvSecretDbUser 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-AZURE-DB-USERNAME'
  properties: {
    value: sqlAdminLogin
  }
}

resource kvSecretDbPassword 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-AZURE-DB-PASSWORD'
  properties: {
    value: sqlAdminPassword
  }
}

resource kvSecretBlobEndpoint 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-BLOB-SERVICE-ENDPOINT'
  properties: {
    value: storageAccount.properties.primaryEndpoints.blob
  }
}

resource kvSecretRedisEndpoint 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-REDIS-ENDPOINT'
  properties: {
    value: '${redisEnterprise.properties.hostName}:10000'
  }
}

resource kvSecretRedisKey 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-REDIS-ACCESS-KEY'
  properties: {
    value: redisDatabase.listKeys().primaryKey
  }
}

resource kvSecretContentSafetyEndpoint 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-CONTENT-SAFETY-ENDPOINT'
  properties: {
    value: contentSafety.properties.endpoint
  }
}

resource kvSecretTextAnalyticsEndpoint 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-TEXT-ANALYTICS-ENDPOINT'
  properties: {
    value: textAnalytics.properties.endpoint
  }
}

resource kvSecretAcsEndpoint 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = if (enableEmailServices) {
  parent: keyVault
  name: 'TASKMANAGER-ACS-ENDPOINT'
  properties: {
    value: 'https://${acs!.properties.hostName}'
  }
}

resource kvSecretAcsAdminEndpoint 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = if (enableEmailServices) {
  parent: keyVault
  name: 'TASKMANAGER-ACS-ADMIN-ENDPOINT'
  properties: {
    value: 'https://${acsAdmin!.properties.hostName}'
  }
}

resource kvSecretAppInsightsProd 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-APP-INSIGHT-CONN-STR-PROD'
  properties: {
    value: appInsightsProd.properties.ConnectionString
  }
}

resource kvSecretAppInsightsDev 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'TASKMANAGER-APP-INSIGHT-CONN-STR-DEV'
  properties: {
    value: appInsightsDev.properties.ConnectionString
  }
}

// 8. Container Registry (Basic)

resource acr 'Microsoft.ContainerRegistry/registries@2023-11-01-preview' = {
  name: acrName
  location: location
  tags: tags
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: true
  }
}

// 9. Content Safety (image moderation) — West Europe

resource contentSafety 'Microsoft.CognitiveServices/accounts@2023-10-01-preview' = {
  name: contentSafetyName
  location: cognitiveServicesLocation
  tags: tags
  kind: 'ContentSafety'
  sku: {
    name: 'S0'
  }
  properties: {
    publicNetworkAccess: 'Enabled'
    restore: true
  }
}

// 9b. Text Analytics (Comment Intelligence — sentiment, key phrases, PII, summarisation) — West Europe

resource textAnalytics 'Microsoft.CognitiveServices/accounts@2023-10-01-preview' = {
  name: textAnalyticsName
  location: cognitiveServicesLocation
  tags: tags
  kind: 'TextAnalytics'
  sku: {
    name: 'S'
  }
  properties: {
    publicNetworkAccess: 'Enabled'
    restore: true
  }
}

// 10. Azure Communication Services (email) — Global
// Skipped for arena deployments — Spring NoOp email beans make ACS unnecessary.

resource emailService 'Microsoft.Communication/emailServices@2023-04-01' = if (enableEmailServices) {
  name: '${projectName}-email'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: 'Europe'
  }
}

resource emailDomain 'Microsoft.Communication/emailServices/domains@2023-04-01' = if (enableEmailServices) {
  parent: emailService
  name: 'AzureManagedDomain'
  location: 'global'
  tags: tags
  properties: {
    domainManagement: 'AzureManaged'
    userEngagementTracking: 'Disabled'
  }
}

resource acs 'Microsoft.Communication/communicationServices@2023-04-01' = if (enableEmailServices) {
  name: '${projectName}-acs'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: 'Europe'
    linkedDomains: [
      emailDomain.id
    ]
  }
}

// 10b. Azure Communication Services — Admin Alerts (separate quota pool)
// Isolates critical admin emails (CriticalExceptionAlerter, MaintenanceStalenessChecker)
// from user-facing email traffic so platform rate limits (100/hour) never block alerts.

resource emailServiceAdmin 'Microsoft.Communication/emailServices@2023-04-01' = if (enableEmailServices) {
  name: '${projectName}-email-admin'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: 'Europe'
  }
}

resource emailDomainAdmin 'Microsoft.Communication/emailServices/domains@2023-04-01' = if (enableEmailServices) {
  parent: emailServiceAdmin
  name: 'AzureManagedDomain'
  location: 'global'
  tags: tags
  properties: {
    domainManagement: 'AzureManaged'
    userEngagementTracking: 'Disabled'
  }
}

resource acsAdmin 'Microsoft.Communication/communicationServices@2023-04-01' = if (enableEmailServices) {
  name: '${projectName}-acs-admin'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: 'Europe'
    linkedDomains: [
      emailDomainAdmin.id
    ]
  }
}

// 11. RBAC Role Assignments

// KV Secrets User — lets the app read secrets
resource kvRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: keyVault
  name: guid(keyVault.id, managedIdentity.id, 'KeyVaultSecretsUser')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '4633458b-17de-408a-b874-0445c86b69e6')
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// Blob Data Contributor — read/write blobs
resource blobRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: storageAccount
  name: guid(storageAccount.id, managedIdentity.id, 'StorageBlobDataContributor')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// KV Secrets Officer — lets the deployer view and seed secrets via Portal / CLI
resource kvDeployerRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (deployerPrincipalId != '') {
  scope: keyVault
  name: guid(keyVault.id, deployerPrincipalId, 'KeyVaultSecretsOfficer')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'b86a8fe4-44ce-4948-aee5-eccb2c155cd7')
    principalId: deployerPrincipalId
    principalType: 'User'
  }
}

// Blob Data Contributor — lets the deployer push default images
resource blobDeployerRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (deployerPrincipalId != '') {
  scope: storageAccount
  name: guid(storageAccount.id, deployerPrincipalId, 'StorageBlobDataContributor')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
    principalId: deployerPrincipalId
    principalType: 'User'
  }
}

// Blob Data Contributor — lets CI/CD (GitHub Actions) deploy frontend and push blobs
resource blobCicdRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (cicdPrincipalId != '') {
  scope: storageAccount
  name: guid(storageAccount.id, cicdPrincipalId, 'StorageBlobDataContributor')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'ba92f5b4-2d11-453d-a403-e96b0029c9fe')
    principalId: cicdPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// Cognitive Services User — image moderation (Content Safety)
resource csRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: contentSafety
  name: guid(contentSafety.id, managedIdentity.id, 'CognitiveServicesUser')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'a97b65f3-24c7-4388-baec-2e87135dc908')
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// Cognitive Services User — comment analysis (Text Analytics / AI Language)
resource taRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: textAnalytics
  name: guid(textAnalytics.id, managedIdentity.id, 'CognitiveServicesUser')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'a97b65f3-24c7-4388-baec-2e87135dc908')
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// ACS Contributor — send emails via managed identity
resource acsRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (enableEmailServices) {
  scope: acs
  name: guid(acs.id, managedIdentity.id, 'Contributor')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'b24988ac-6180-42a0-ab88-20f7382dd24c')
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// ACS Admin Contributor — send admin alert emails via managed identity
resource acsAdminRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (enableEmailServices) {
  scope: acsAdmin
  name: guid(acsAdmin.id, managedIdentity.id, 'Contributor')
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', 'b24988ac-6180-42a0-ab88-20f7382dd24c')
    principalId: managedIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// 12. App Service Plan (B1 Linux, 2 instances)

resource appServicePlan 'Microsoft.Web/serverfarms@2023-12-01' = {
  name: appServicePlanName
  location: location
  tags: tags
  kind: 'linux'
  sku: {
    name: 'B1'
    tier: 'Basic'
    capacity: appServiceInstanceCount
  }
  properties: {
    reserved: true
  }
}

// 13. Web App (Linux container from ACR)

resource webApp 'Microsoft.Web/sites@2023-12-01' = {
  name: webAppName
  location: location
  tags: tags
  kind: 'app,linux,container'
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    clientAffinityEnabled: false
    keyVaultReferenceIdentity: managedIdentity.id
    siteConfig: {
      linuxFxVersion: 'DOCKER|${backendImage}'
      alwaysOn: true
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      http20Enabled: true
      ipSecurityRestrictions: [
        {
          ipAddress: 'AzureFrontDoor.Backend'
          action: 'Allow'
          tag: 'ServiceTag'
          priority: 100
          name: 'AllowFrontDoor'
          headers: {
            'X-Azure-FDID': [frontDoor.properties.frontDoorId]
          }
        }
      ]
      appSettings: [
        { name: 'MANAGED_IDENTITY_CLIENT_ID', value: managedIdentity.properties.clientId }
        { name: 'KEYVAULT_URI', value: keyVault.properties.vaultUri }
        { name: 'SPRING_PROFILES_ACTIVE', value: springProfilesActive }
        { name: 'DOCKER_REGISTRY_SERVER_URL', value: 'https://${acr.properties.loginServer}' }
        { name: 'DOCKER_REGISTRY_SERVER_USERNAME', value: acr.listCredentials().username }
        { name: 'DOCKER_REGISTRY_SERVER_PASSWORD', value: acr.listCredentials().passwords[0].value }
        { name: 'APP_PUBLIC_URL', value: useCustomDomain ? 'https://${customDomainHost}' : 'https://${fdEndpoint.properties.hostName}' }
        { name: 'WEBSITES_PORT', value: '8080' }
        { name: 'WEBSITES_CONTAINER_STOP_TIME_LIMIT', value: '30' }
        { name: 'MICROSOFT_PROVIDER_AUTHENTICATION_SECRET', value: '@Microsoft.KeyVault(VaultName=${keyVaultName};SecretName=TASKMANAGER-AUTH-CLIENT-SECRET)' }
      ]
    }
  }
}

// 13a. Easy Auth (Require Authentication)
// Part of the 3-layer defence-in-depth setup:
//   1. Service tag IP restriction  2. FDID header check  3. Bearer token via FD origin auth
// FD sends its MI token as Authorization header; Easy Auth validates audience + issuer.
// Spring Boot uses cookies for user sessions so there's no conflict.

resource webAppAuth 'Microsoft.Web/sites/config@2023-12-01' = {
  name: 'authsettingsV2'
  parent: webApp
  properties: {
    platform: {
      enabled: true
    }
    globalValidation: {
      requireAuthentication: true
      unauthenticatedClientAction: 'RedirectToLoginPage'
      redirectToProvider: 'azureActiveDirectory'
    }
    identityProviders: {
      azureActiveDirectory: {
        enabled: true
        registration: {
          clientId: authAppClientId
          clientSecretSettingName: 'MICROSOFT_PROVIDER_AUTHENTICATION_SECRET'
          openIdIssuer: 'https://sts.windows.net/${authAppTenantId}/v2.0'
        }
        validation: {
          allowedAudiences: [
            'api://${authAppClientId}'
          ]
          defaultAuthorizationPolicy: {
            allowedApplications: [
              authAppClientId
              managedIdentity.properties.clientId
            ]
          }
        }
      }
    }
    login: {
      tokenStore: {
        enabled: true
      }
    }
    httpSettings: {
      requireHttps: true
      forwardProxy: {
        convention: 'Standard'
      }
    }
  }
}

// 14. Container App Environment

resource containerAppEnv 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: '${projectName}-cae'
  location: location
  tags: tags
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalyticsProd.properties.customerId
        sharedKey: logAnalyticsProd.listKeys().primarySharedKey
      }
    }
  }
}

// 15a. Maintenance Job — Full (daily 03:15 UTC)
// Runs all cleanup: blobs (all containers), assets, inactive users, DB hygiene,
// group-creation reset, storage reconciliation, monthly budget reset, ACR prune.
// Offset to :15 so it never overlaps the :00/:30 task-blobs job.

resource maintenanceJobFull 'Microsoft.App/jobs@2024-03-01' = {
  name: 'maintenance-job-full'
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  properties: {
    environmentId: containerAppEnv.id
    configuration: {
      triggerType: 'Schedule'
      replicaTimeout: 1200
      replicaRetryLimit: 1
      scheduleTriggerConfig: {
        cronExpression: '15 3 * * *'
        parallelism: 1
        replicaCompletionCount: 1
      }
      registries: [
        {
          server: acr.properties.loginServer
          username: acr.listCredentials().username
          passwordSecretRef: 'acr-password'
        }
      ]
      secrets: [
        {
          name: 'acr-password'
          value: acr.listCredentials().passwords[0].value
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'maintenance'
          image: maintenanceImage
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
          env: [
            { name: 'MANAGED_IDENTITY_CLIENT_ID', value: managedIdentity.properties.clientId }
            { name: 'KEYVAULT_URI', value: keyVault.properties.vaultUri }
            { name: 'SPRING_PROFILES_ACTIVE', value: springProfilesActive }
            { name: 'MAINTENANCE_MODE', value: 'full' }
            { name: 'MAINTENANCE_FULL_INTERVAL_HOURS', value: '24' }
          ]
        }
      ]
    }
  }
}

// 15b. Maintenance Job — Task-blobs (every 30 min)
// Lightweight: scans only task-files and task-assignee-files containers for orphaned blobs.
// Runs at :00 and :30 of every hour. The full job runs at :15 so they never overlap.

resource maintenanceJobBlobs 'Microsoft.App/jobs@2024-03-01' = {
  name: 'maintenance-job-blobs'
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  properties: {
    environmentId: containerAppEnv.id
    configuration: {
      triggerType: 'Schedule'
      replicaTimeout: 600
      replicaRetryLimit: 1
      scheduleTriggerConfig: {
        cronExpression: '*/30 * * * *'
        parallelism: 1
        replicaCompletionCount: 1
      }
      registries: [
        {
          server: acr.properties.loginServer
          username: acr.listCredentials().username
          passwordSecretRef: 'acr-password'
        }
      ]
      secrets: [
        {
          name: 'acr-password'
          value: acr.listCredentials().passwords[0].value
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'maintenance'
          image: maintenanceImage
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            { name: 'MANAGED_IDENTITY_CLIENT_ID', value: managedIdentity.properties.clientId }
            { name: 'KEYVAULT_URI', value: keyVault.properties.vaultUri }
            { name: 'SPRING_PROFILES_ACTIVE', value: springProfilesActive }
            { name: 'MAINTENANCE_MODE', value: 'task-blobs' }
          ]
        }
      ]
    }
  }
}

// 17. WAF Policy

// Stress header passthrough — allows requests with both X-Stress-Key and X-Stress-Nonce.
// Deployed only for stress testing. The deployer generates the secrets and gives them
// to the tester — avoids IP whitelisting while keeping BlockAllTraffic armed.
var useStressHeaders = stressPassphraseKey != '' && stressPassphraseNonce != ''

var stressHeaderRule = useStressHeaders ? [
  {
    name: 'AllowStressHeaders'
    priority: 2
    ruleType: 'MatchRule'
    action: 'Allow'
    enabledState: 'Enabled'
    matchConditions: [
      {
        matchVariable: 'RequestHeader'
        selector: 'X-Stress-Key'
        operator: 'Equal'
        negateCondition: false
        matchValue: [stressPassphraseKey]
        transforms: []
      }
      {
        matchVariable: 'RequestHeader'
        selector: 'X-Stress-Nonce'
        operator: 'Equal'
        negateCondition: false
        matchValue: [stressPassphraseNonce]
        transforms: []
      }
    ]
  }
] : []

var baseWafRules = [
  {
    name: 'BlockInternalEndpoints'
    priority: 1
    ruleType: 'MatchRule'
    action: 'Block'
    enabledState: 'Enabled'
    matchConditions: [
      {
        matchVariable: 'RequestUri'
        operator: 'Contains'
        negateCondition: false
        matchValue: ['/actuator']
        transforms: ['Lowercase']
      }
    ]
  }
]

var blockAllRule = [
  {
    name: 'BlockAllTraffic'
    priority: 5
    ruleType: 'MatchRule'
    action: 'Block'
    enabledState: enableWafBlockAll ? 'Enabled' : 'Disabled'
    matchConditions: [
      {
        matchVariable: 'RequestUri'
        operator: 'BeginsWith'
        negateCondition: false
        matchValue: ['/']
        transforms: []
      }
    ]
  }
]

var rateLimitRules = [
  {
    name: 'RateLimitAuth'
    priority: 10
    ruleType: 'RateLimitRule'
    action: 'Block'
    enabledState: wafAuthRateLimit > 0 ? 'Enabled' : 'Disabled'
    rateLimitThreshold: wafAuthRateLimit > 0 ? wafAuthRateLimit : 1
    rateLimitDurationInMinutes: 5
    matchConditions: [
      {
        matchVariable: 'RequestUri'
        operator: 'Contains'
        negateCondition: false
        matchValue: ['/api/auth/']
        transforms: []
      }
    ]
  }
  {
    name: 'GlobalRateLimit'
    priority: 100
    ruleType: 'RateLimitRule'
    action: 'Block'
    enabledState: wafGlobalRateLimit > 0 ? 'Enabled' : 'Disabled'
    rateLimitThreshold: wafGlobalRateLimit > 0 ? wafGlobalRateLimit : 1
    rateLimitDurationInMinutes: 5
    matchConditions: [
      {
        matchVariable: 'RequestUri'
        operator: 'BeginsWith'
        negateCondition: false
        matchValue: ['/']
        transforms: []
      }
    ]
  }
]

resource wafPolicy 'Microsoft.Network/FrontDoorWebApplicationFirewallPolicies@2024-02-01' = {
  name: wafPolicyName
  location: 'global'
  tags: tags
  sku: {
    name: 'Standard_AzureFrontDoor'
  }
  properties: {
    policySettings: {
      enabledState: 'Enabled'
      mode: 'Prevention'
    }
    customRules: {
      rules: concat(baseWafRules, stressHeaderRule, blockAllRule, rateLimitRules)
    }
  }
}

// 18. Front Door Profile + Endpoint

resource frontDoor 'Microsoft.Cdn/profiles@2024-02-01' = {
  name: frontDoorName
  location: 'global'
  tags: tags
  sku: {
    name: 'Standard_AzureFrontDoor'
  }
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentity.id}': {}
    }
  }
  properties: {
    originResponseTimeoutSeconds: 60
  }
}

resource fdEndpoint 'Microsoft.Cdn/profiles/afdEndpoints@2024-02-01' = {
  parent: frontDoor
  name: '${projectName}-endpoint'
  location: 'global'
  properties: {
    enabledState: 'Enabled'
  }
}

// 19. Custom Domain (only if customDomainHost is provided)

resource fdCustomDomain 'Microsoft.Cdn/profiles/customDomains@2024-02-01' = if (useCustomDomain) {
  parent: frontDoor
  name: replace(customDomainHost, '.', '-')
  properties: {
    hostName: customDomainHost
    tlsSettings: {
      certificateType: 'ManagedCertificate'
      minimumTlsVersion: 'TLS12'
    }
  }
}

// 20. Origin Groups + Origins

// Backend API — uses preview API for origin auth (FD MI → Bearer → Easy Auth)
resource fdOriginGroupApi 'Microsoft.Cdn/profiles/originGroups@2025-09-01-preview' = {
  parent: frontDoor
  name: 'api-origin-group'
  properties: {
    loadBalancingSettings: {
      sampleSize: 4
      successfulSamplesRequired: 3
    }
    healthProbeSettings: {
      probePath: '/actuator/health'
      probeProtocol: 'Https'
      probeIntervalInSeconds: 30
      probeRequestType: 'HEAD'
    }
    authentication: {
      type: 'UserAssignedIdentity'
      userAssignedIdentity: {
        id: managedIdentity.id
      }
      scope: 'api://${authAppClientId}/.default'
    }
  }
}

resource fdOriginApi 'Microsoft.Cdn/profiles/originGroups/origins@2024-02-01' = {
  parent: fdOriginGroupApi
  name: 'api-origin'
  properties: {
    hostName: webApp.properties.defaultHostName
    httpPort: 80
    httpsPort: 443
    originHostHeader: webApp.properties.defaultHostName
    priority: 1
    weight: 1000
    enabledState: 'Enabled'
  }
}

// Blob Storage — FD authenticates via MI token; RBAC authorises the read access
resource fdOriginGroupBlob 'Microsoft.Cdn/profiles/originGroups@2025-09-01-preview' = {
  parent: frontDoor
  name: 'blob-origin-group'
  properties: {
    loadBalancingSettings: {
      sampleSize: 4
      successfulSamplesRequired: 3
    }
    authentication: {
      type: 'UserAssignedIdentity'
      userAssignedIdentity: {
        id: managedIdentity.id
      }
      scope: 'https://storage.azure.com/.default'
    }
  }
}

resource fdOriginBlob 'Microsoft.Cdn/profiles/originGroups/origins@2024-02-01' = {
  parent: fdOriginGroupBlob
  name: 'blob-origin'
  properties: {
    hostName: replace(replace(storageAccount.properties.primaryEndpoints.blob, 'https://', ''), '/', '')
    httpPort: 80
    httpsPort: 443
    originHostHeader: replace(replace(storageAccount.properties.primaryEndpoints.blob, 'https://', ''), '/', '')
    priority: 1
    weight: 1000
    enabledState: 'Enabled'
  }
}

// 21. Rule Sets + Rules

// Security headers

resource rsSecurityHeaders 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'SecurityHeaders'
}

resource ruleSecurityHeaders 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsSecurityHeaders
  name: 'SetHeaders'
  properties: {
    order: 1
    matchProcessingBehavior: 'Continue'
    conditions: []
    actions: [
      {
        name: 'ModifyResponseHeader'
        parameters: {
          typeName: 'DeliveryRuleHeaderActionParameters'
          headerAction: 'Overwrite'
          headerName: 'Strict-Transport-Security'
          value: 'max-age=31536000; includeSubDomains'
        }
      }
      {
        name: 'ModifyResponseHeader'
        parameters: {
          typeName: 'DeliveryRuleHeaderActionParameters'
          headerAction: 'Overwrite'
          headerName: 'Content-Security-Policy'
          value: 'default-src \'self\'; script-src \'self\'; style-src \'self\' \'unsafe-inline\'; img-src \'self\' blob: data:; font-src \'self\'; connect-src \'self\'; frame-ancestors \'none\''
        }
      }
      {
        name: 'ModifyResponseHeader'
        parameters: {
          typeName: 'DeliveryRuleHeaderActionParameters'
          headerAction: 'Overwrite'
          headerName: 'X-Frame-Options'
          value: 'DENY'
        }
      }
      {
        name: 'ModifyResponseHeader'
        parameters: {
          typeName: 'DeliveryRuleHeaderActionParameters'
          headerAction: 'Overwrite'
          headerName: 'X-Content-Type-Options'
          value: 'nosniff'
        }
      }
      {
        name: 'ModifyResponseHeader'
        parameters: {
          typeName: 'DeliveryRuleHeaderActionParameters'
          headerAction: 'Overwrite'
          headerName: 'Referrer-Policy'
          value: 'strict-origin-when-cross-origin'
        }
      }
      {
        name: 'ModifyResponseHeader'
        parameters: {
          typeName: 'DeliveryRuleHeaderActionParameters'
          headerAction: 'Overwrite'
          headerName: 'Permissions-Policy'
          value: 'camera=(), microphone=(), geolocation=(), payment=()'
        }
      }
    ]
  }
}

// Strip /api prefix

resource rsStripApi 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'StripApi'
}

resource ruleStripApi 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsStripApi
  name: 'RewriteApi'
  properties: {
    order: 1
    matchProcessingBehavior: 'Continue'
    conditions: []
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/'
          preserveUnmatchedPath: true
        }
      }
    ]
  }
}

// Rewrite to /staticfrontend/ container

resource rsRewriteStaticFrontend 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'RewriteStaticFrontend'
}

resource ruleRewriteStaticFrontend 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsRewriteStaticFrontend
  name: 'RewriteStaticFe'
  properties: {
    order: 1
    matchProcessingBehavior: 'Continue'
    conditions: []
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/staticfrontend/'
          preserveUnmatchedPath: true
        }
      }
    ]
  }
}

// Rewrite to /default-images/ container

resource rsStripBlobDefaultImages 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'StripBlobDefaultImages'
}

resource ruleStripBlobDefaultImages 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsStripBlobDefaultImages
  name: 'RewriteDefaultImg'
  properties: {
    order: 1
    matchProcessingBehavior: 'Continue'
    conditions: []
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/default-images/'
          preserveUnmatchedPath: true
        }
      }
    ]
  }
}

// Rewrite to /group-images/ container

resource rsStripBlobGroupImages 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'StripBlobGroupImages'
}

resource ruleStripBlobGroupImages 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsStripBlobGroupImages
  name: 'RewriteGroupImg'
  properties: {
    order: 1
    matchProcessingBehavior: 'Continue'
    conditions: []
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/group-images/'
          preserveUnmatchedPath: true
        }
      }
    ]
  }
}

// Rewrite to /profile-images/ container

resource rsStripBlobProfileImages 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'StripBlobProfileImages'
}

resource ruleStripBlobProfileImages 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsStripBlobProfileImages
  name: 'RewriteProfileImg'
  properties: {
    order: 1
    matchProcessingBehavior: 'Continue'
    conditions: []
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/profile-images/'
          preserveUnmatchedPath: true
        }
      }
    ]
  }
}

// SPA fallback (2 rules: static assets → /web/, everything else → /web/index.html)

resource rsWebSPAFallback 'Microsoft.Cdn/profiles/ruleSets@2024-02-01' = {
  parent: frontDoor
  name: 'WebSPAFallback'
}

// Static file extensions → /web/{path}
resource ruleWebStaticAssets 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsWebSPAFallback
  name: 'StaticAssets'
  properties: {
    order: 1
    matchProcessingBehavior: 'Stop'
    conditions: [
      {
        name: 'UrlFileExtension'
        parameters: {
          typeName: 'DeliveryRuleUrlFileExtensionMatchConditionParameters'
          operator: 'Equal'
          matchValues: ['html', 'png', 'jpg', 'jpeg', 'gif', 'svg', 'ico']
          negateCondition: false
          transforms: ['Lowercase']
        }
      }
    ]
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/web/'
          preserveUnmatchedPath: true
        }
      }
    ]
  }
}

// SPA routes → /web/index.html
resource ruleWebSPAFallback 'Microsoft.Cdn/profiles/ruleSets/rules@2024-02-01' = {
  parent: rsWebSPAFallback
  name: 'SPAFallback'
  properties: {
    order: 2
    matchProcessingBehavior: 'Stop'
    conditions: [
      {
        name: 'UrlFileExtension'
        parameters: {
          typeName: 'DeliveryRuleUrlFileExtensionMatchConditionParameters'
          operator: 'Equal'
          matchValues: ['html', 'png', 'jpg', 'jpeg', 'gif', 'svg', 'ico']
          negateCondition: true
          transforms: ['Lowercase']
        }
      }
    ]
    actions: [
      {
        name: 'UrlRewrite'
        parameters: {
          typeName: 'DeliveryRuleUrlRewriteActionParameters'
          sourcePattern: '/'
          destination: '/web/index.html'
          preserveUnmatchedPath: false
        }
      }
    ]
  }
}

// 22. Front Door Security Policy (WAF ↔ Endpoint)

resource fdSecurityPolicy 'Microsoft.Cdn/profiles/securityPolicies@2024-02-01' = {
  parent: frontDoor
  name: 'waf-association'
  properties: {
    parameters: {
      type: 'WebApplicationFirewall'
      wafPolicy: { id: wafPolicy.id }
      associations: [
        {
          domains: useCustomDomain ? [
            { id: fdEndpoint.id }
            { id: fdCustomDomain.id }
          ] : [
            { id: fdEndpoint.id }
          ]
          patternsToMatch: ['/*']
        }
      ]
    }
  }
}

// 23. Front Door Routes

// SPA catch-all → blob web container
resource routeSpa 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'taskmanager'
  properties: {
    originGroup: { id: fdOriginGroupBlob.id }
    patternsToMatch: ['/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    ruleSets: [
      { id: rsSecurityHeaders.id }
      { id: rsWebSPAFallback.id }
    ]
  }
  dependsOn: [fdOriginBlob]
}

// Backend API
resource routeBackend 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'taskmanager-backend'
  properties: {
    originGroup: { id: fdOriginGroupApi.id }
    patternsToMatch: ['/api/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    ruleSets: [
      { id: rsSecurityHeaders.id }
      { id: rsStripApi.id }
    ]
  }
  dependsOn: [fdOriginApi]
}

// Assets (Vite JS/CSS bundles) — path passes through
resource routeAssets 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'assets'
  properties: {
    originGroup: { id: fdOriginGroupBlob.id }
    patternsToMatch: ['/assets/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    cacheConfiguration: staticCacheConfig
    ruleSets: []
  }
  dependsOn: [fdOriginBlob]
}

// Static frontend
resource routeStaticFrontend 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'staticfrontend'
  properties: {
    originGroup: { id: fdOriginGroupBlob.id }
    originPath: '/'
    patternsToMatch: ['/static_frontend/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    cacheConfiguration: staticCacheConfig
    ruleSets: [
      { id: rsSecurityHeaders.id }
      { id: rsRewriteStaticFrontend.id }
    ]
  }
  dependsOn: [fdOriginBlob]
}

// Blob: profile images
resource routeBlobProfileImages 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'BlobProfileImages'
  properties: {
    originGroup: { id: fdOriginGroupBlob.id }
    originPath: '/'
    patternsToMatch: ['/blob/profile-images/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    cacheConfiguration: imageCacheConfig
    ruleSets: [
      { id: rsSecurityHeaders.id }
      { id: rsStripBlobProfileImages.id }
    ]
  }
  dependsOn: [fdOriginBlob]
}

// Blob: group images
resource routeBlobGroupImages 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'BlobGroupImages'
  properties: {
    originGroup: { id: fdOriginGroupBlob.id }
    originPath: '/'
    patternsToMatch: ['/blob/group-images/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    cacheConfiguration: imageCacheConfig
    ruleSets: [
      { id: rsSecurityHeaders.id }
      { id: rsStripBlobGroupImages.id }
    ]
  }
  dependsOn: [fdOriginBlob]
}

// Blob: default images
resource routeBlobDefaultImages 'Microsoft.Cdn/profiles/afdEndpoints/routes@2024-02-01' = {
  parent: fdEndpoint
  name: 'BlobDefaultImages'
  properties: {
    originGroup: { id: fdOriginGroupBlob.id }
    originPath: '/'
    patternsToMatch: ['/blob/default-images/*']
    supportedProtocols: ['Http', 'Https']
    httpsRedirect: 'Enabled'
    forwardingProtocol: 'HttpsOnly'
    linkToDefaultDomain: 'Enabled'
    customDomains: useCustomDomain ? [{ id: fdCustomDomain.id }] : []
    cacheConfiguration: imageCacheConfig
    ruleSets: [
      { id: rsSecurityHeaders.id }
      { id: rsStripBlobDefaultImages.id }
    ]
  }
  dependsOn: [fdOriginBlob]
}

// ─── 24. Observability: Alerts, Budget & Diagnostics (prod only) ────────────

// Action group: email recipient for all Azure-native alerts
resource actionGroupEmail 'Microsoft.Insights/actionGroups@2023-09-01-preview' = if (enableObservabilityAlerts && alertEmailAddress != '') {
  name: '${projectName}-alerts-email'
  location: 'global'
  tags: tags
  properties: {
    groupShortName: 'AlertEmail'
    enabled: true
    emailReceivers: [
      {
        name: 'AdminEmail'
        emailAddress: alertEmailAddress
        useCommonAlertSchema: true
      }
    ]
  }
}

// Monthly budget alert (resource-group scoped — deleted when RG is deleted)
resource budget 'Microsoft.Consumption/budgets@2023-11-01' = if (enableObservabilityAlerts && monthlyBudgetAmount > 0) {
  name: '${projectName}-monthly-budget'
  properties: {
    category: 'Cost'
    amount: monthlyBudgetAmount
    timeGrain: 'Monthly'
    timePeriod: {
      startDate: budgetStartDate
    }
    notifications: {
      actual80pct: {
        enabled: true
        operator: 'GreaterThanOrEqualTo'
        threshold: 80
        contactEmails: alertEmailAddress != '' ? [alertEmailAddress] : []
        thresholdType: 'Actual'
      }
      actual100pct: {
        enabled: true
        operator: 'GreaterThanOrEqualTo'
        threshold: 100
        contactEmails: alertEmailAddress != '' ? [alertEmailAddress] : []
        thresholdType: 'Actual'
      }
      forecasted120pct: {
        enabled: true
        operator: 'GreaterThanOrEqualTo'
        threshold: 120
        contactEmails: alertEmailAddress != '' ? [alertEmailAddress] : []
        thresholdType: 'Forecasted'
      }
    }
  }
}

// Front Door origin-health metric alert (30-min evaluation window → no false alarms during restarts)
resource originHealthAlert 'Microsoft.Insights/metricAlerts@2018-03-01' = if (enableObservabilityAlerts && alertEmailAddress != '') {
  name: '${projectName}-fd-origin-down'
  location: 'global'
  tags: tags
  properties: {
    description: 'Front Door origin health dropped below 100% for 30 minutes — backend may be unreachable.'
    severity: 1
    enabled: true
    scopes: [frontDoor.id]
    evaluationFrequency: 'PT5M'
    windowSize: 'PT30M'
    criteria: {
      'odata.type': 'Microsoft.Azure.Monitor.SingleResourceMultipleMetricCriteria'
      allOf: [
        {
          name: 'OriginHealthDrop'
          metricName: 'OriginHealthPercentage'
          metricNamespace: 'Microsoft.Cdn/profiles'
          operator: 'LessThan'
          threshold: 100
          timeAggregation: 'Average'
          criterionType: 'StaticThresholdCriterion'
        }
      ]
    }
    actions: [
      { actionGroupId: actionGroupEmail.id }
    ]
  }
}

// Front Door access-log diagnostic setting (includes WAF action data on Standard tier)
resource fdDiagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = if (enableObservabilityAlerts) {
  name: '${projectName}-fd-diagnostics'
  scope: frontDoor
  properties: {
    workspaceId: logAnalyticsProd.id
    logs: [
      {
        category: 'FrontDoorAccessLog'
        enabled: true
      }
      {
        category: 'FrontDoorHealthProbeLog'
        enabled: true
      }
    ]
    metrics: []
  }
}

// Outputs

output managedIdentityClientId string = managedIdentity.properties.clientId
output managedIdentityPrincipalId string = managedIdentity.properties.principalId
output keyVaultUri string = keyVault.properties.vaultUri
output acrLoginServer string = acr.properties.loginServer
output sqlServerFqdn string = sqlServer.properties.fullyQualifiedDomainName
output storageAccountName string = storageAccountName
output storageEndpoint string = storageAccount.properties.primaryEndpoints.blob
output storageBlobHostName string = replace(replace(storageAccount.properties.primaryEndpoints.blob, 'https://', ''), '/', '')
output appInsightsProdConnStr string = appInsightsProd.properties.ConnectionString
output appInsightsDevConnStr string = appInsightsDev.properties.ConnectionString
output webAppName string = webAppName
output webAppDefaultHostName string = webApp.properties.defaultHostName
output frontDoorProfileName string = frontDoorName
output frontDoorEndpointName string = fdEndpoint.name
output frontDoorEndpointHostName string = fdEndpoint.properties.hostName
