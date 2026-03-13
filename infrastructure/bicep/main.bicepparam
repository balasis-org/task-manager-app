using './main.bicep'

// Core
param location = 'italynorth'
param contentSafetyLocation = 'westeurope'
param projectName = 'MyTeamTasksV2'
param tags = {
  project: 'MyTeamTasksV2'
  managedBy: 'bicep'
}

// SQL
param sqlAdminLogin = 'myteamtasksV2admin'
param sqlAdminPassword = readEnvironmentVariable('SQL_ADMIN_PASSWORD', '')
param sqlDatabaseName = 'myteamtasksV2db'

// App Service
param appServicePlanName = 'myteamtasksV2-plan'
param appServiceInstanceCount = 2

// Easy Auth
param authAppClientId = '7b429825-f7cf-4971-b9b7-36c491c8ed3e'
param authAppTenantId = '9cce5f41-4493-4fac-936e-f4f739db8315'

param customDomainHost = 'www.myteamtasks.net'
