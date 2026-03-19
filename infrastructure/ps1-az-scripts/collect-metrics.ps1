# collect-metrics.ps1 — Queries Azure Monitor platform metrics for a resource group.
# These metrics are FREE (93-day retention) and never touch Log Analytics.
#
# Usage:
#   pwsh -File collect-metrics.ps1 -ResourceGroup MyTeamTasksV2 -HoursBack 1
#   pwsh -File collect-metrics.ps1 -ResourceGroup MyTeamTasksV2 -StartTime 2026-03-19T10:00:00Z -EndTime 2026-03-19T11:00:00Z
#   pwsh -File collect-metrics.ps1 -ResourceGroup MyTeamTasksV2 -HoursBack 168   # last 7 days

param(
    [Parameter(Mandatory)][string]$ResourceGroup,
    [string]$StartTime,
    [string]$EndTime,
    [int]$HoursBack = 1,
    [string]$OutputFile
)

$ErrorActionPreference = 'Stop'

# ── Time window ──────────────────────────────────────────────
if (-not $StartTime) {
    $end   = [DateTime]::UtcNow
    $start = $end.AddHours(-$HoursBack)
    $StartTime = $start.ToString('yyyy-MM-ddTHH:mm:ssZ')
    $EndTime   = $end.ToString('yyyy-MM-ddTHH:mm:ssZ')
}

Write-Host "Collecting metrics for $ResourceGroup"
Write-Host "  Window: $StartTime → $EndTime"
Write-Host ""

# ── Discover resources by type ───────────────────────────────
function Find-Resource($type) {
    $r = az resource list -g $ResourceGroup --resource-type $type --query "[0].id" -o tsv 2>$null
    return $r
}

$sqlDbId      = az resource list -g $ResourceGroup --resource-type "Microsoft.Sql/servers/databases" --query "[?!contains(name,'master')].id | [0]" -o tsv 2>$null
$appPlanId    = Find-Resource "Microsoft.Web/serverfarms"
$webAppId     = Find-Resource "Microsoft.Web/sites"
$redisId      = Find-Resource "Microsoft.Cache/redisEnterprise"
$frontDoorId  = Find-Resource "Microsoft.Cdn/profiles"
$storageId    = Find-Resource "Microsoft.Storage/storageAccounts"

# ── Query helper ─────────────────────────────────────────────
function Get-Metric($resourceId, $metricNames, $aggregation) {
    if (-not $resourceId) { return $null }
    $raw = az monitor metrics list `
        --resource $resourceId `
        --metric $metricNames `
        --start-time $StartTime `
        --end-time $EndTime `
        --aggregation $aggregation `
        --interval PT5M `
        -o json 2>$null
    if ($raw) { return $raw | ConvertFrom-Json }
    return $null
}

function Extract-Values($metricsResult, $metricName, $aggregation) {
    if (-not $metricsResult) { return @{ avg = 'N/A'; max = 'N/A'; last = 'N/A' } }
    $series = $metricsResult.value | Where-Object { $_.name.value -eq $metricName }
    if (-not $series -or -not $series.timeseries -or $series.timeseries.Count -eq 0) {
        return @{ avg = 'N/A'; max = 'N/A'; last = 'N/A' }
    }
    $points = $series.timeseries[0].data | Where-Object { $_.$aggregation -ne $null }
    if ($points.Count -eq 0) { return @{ avg = 'N/A'; max = 'N/A'; last = 'N/A' } }
    $values = $points | ForEach-Object { $_.$aggregation }
    $avg = ($values | Measure-Object -Average).Average
    $max = ($values | Measure-Object -Maximum).Maximum
    $last = $values[-1]
    return @{ avg = [math]::Round($avg, 2); max = [math]::Round($max, 2); last = [math]::Round($last, 2) }
}

# ── Collect ──────────────────────────────────────────────────
$results = [ordered]@{}

# SQL Database
if ($sqlDbId) {
    $sqlMetrics = Get-Metric $sqlDbId "dtu_consumption_percent,storage_percent" "average,maximum"
    $results['sql_dtu_percent']     = Extract-Values $sqlMetrics 'dtu_consumption_percent' 'average'
    $results['sql_storage_percent'] = Extract-Values $sqlMetrics 'storage_percent' 'average'
}

# App Service Plan
if ($appPlanId) {
    $planMetrics = Get-Metric $appPlanId "CpuPercentage,MemoryPercentage" "average,maximum"
    $results['plan_cpu_percent']    = Extract-Values $planMetrics 'CpuPercentage' 'average'
    $results['plan_memory_percent'] = Extract-Values $planMetrics 'MemoryPercentage' 'average'
}

# Web App
if ($webAppId) {
    $appMetrics = Get-Metric $webAppId "Http5xx,Requests" "total"
    $results['app_http_5xx']   = Extract-Values $appMetrics 'Http5xx' 'total'
    $results['app_requests']   = Extract-Values $appMetrics 'Requests' 'total'
}

# Redis Enterprise
if ($redisId) {
    $redisMetrics = Get-Metric $redisId "usedmemorypercentage,operationsPerSecond,connectedclients" "average,maximum"
    $results['redis_memory_percent'] = Extract-Values $redisMetrics 'usedmemorypercentage' 'average'
    $results['redis_ops_per_sec']    = Extract-Values $redisMetrics 'operationsPerSecond' 'average'
    $results['redis_connections']    = Extract-Values $redisMetrics 'connectedclients' 'maximum'
}

# Front Door
if ($frontDoorId) {
    $fdMetrics = Get-Metric $frontDoorId "TotalLatency,RequestCount,OriginHealthPercentage" "average,total"
    $results['fd_latency_ms']       = Extract-Values $fdMetrics 'TotalLatency' 'average'
    $results['fd_request_count']    = Extract-Values $fdMetrics 'RequestCount' 'total'
    $results['fd_origin_health']    = Extract-Values $fdMetrics 'OriginHealthPercentage' 'average'
}

# Storage Account
if ($storageId) {
    $storeMetrics = Get-Metric $storageId "Transactions,Egress" "total"
    $results['storage_transactions'] = Extract-Values $storeMetrics 'Transactions' 'total'
    $results['storage_egress_bytes'] = Extract-Values $storeMetrics 'Egress' 'total'
}

# ── Output ───────────────────────────────────────────────────
Write-Host "┌────────────────────────────┬──────────┬──────────┬──────────┐"
Write-Host "│ Metric                     │ Avg      │ Max      │ Last     │"
Write-Host "├────────────────────────────┼──────────┼──────────┼──────────┤"

foreach ($key in $results.Keys) {
    $v = $results[$key]
    $label = $key.PadRight(26)
    $a = "$($v.avg)".PadRight(8)
    $m = "$($v.max)".PadRight(8)
    $l = "$($v.last)".PadRight(8)
    Write-Host "│ $label │ $a │ $m │ $l │"
}

Write-Host "└────────────────────────────┴──────────┴──────────┴──────────┘"

# JSON output
$jsonPayload = @{
    resourceGroup = $ResourceGroup
    startTime     = $StartTime
    endTime       = $EndTime
    metrics       = $results
} | ConvertTo-Json -Depth 5

if ($OutputFile) {
    $jsonPayload | Out-File -FilePath $OutputFile -Encoding utf8
    Write-Host "`nJSON written to: $OutputFile"
} else {
    Write-Host "`nJSON output:"
    Write-Host $jsonPayload
}
