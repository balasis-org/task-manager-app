# KQL Queries for Azure Log Analytics / Application Insights

> **Where to run these**: Azure Portal → Application Insights → Logs (or Log Analytics workspace → Logs).  
> Copy-paste any query below directly into the KQL editor.  
> All queries assume structured JSON logging is enabled (LogstashEncoder in prod).

---

## Health & Availability

```kql
// All health check results from the last 24 hours — shows if any component went DOWN
requests
| where timestamp > ago(24h)
| where url endswith "/actuator/health"
| project timestamp, resultCode, duration, cloud_RoleInstance
| order by timestamp desc
```

```kql
// Failed health checks (non-200) — instances that returned unhealthy status
requests
| where timestamp > ago(24h)
| where url endswith "/actuator/health"
| where resultCode != "200"
| project timestamp, resultCode, duration, cloud_RoleInstance
| order by timestamp desc
```

---

## Errors & Exceptions

```kql
// All exceptions in the last 24 hours — grouped by type with count
exceptions
| where timestamp > ago(24h)
| summarize count() by type, outerMessage
| order by count_ desc
```

```kql
// Critical exceptions (CriticalBlobStorage, CriticalAuthIntegrity, CriticalInfrastructure)
traces
| where timestamp > ago(24h)
| where severityLevel >= 3
| where message contains "CRITICAL"
    or customDimensions.criticalType != ""
| project timestamp, message,
    criticalType = tostring(customDimensions.criticalType),
    instanceId = tostring(customDimensions.instanceId),
    totalOccurrences = tostring(customDimensions.totalOccurrences)
| order by timestamp desc
```

```kql
// All ERROR-level logs in the last hour — the go-to panic query
traces
| where timestamp > ago(1h)
| where severityLevel >= 3
| project timestamp, message,
    logger = tostring(customDimensions.logger_name),
    instanceId = tostring(customDimensions.instanceId)
| order by timestamp desc
```

```kql
// Exception stack traces — full details for the last 6 hours
exceptions
| where timestamp > ago(6h)
| project timestamp, type, outerMessage, innermostMessage, details
| order by timestamp desc
```

---

## Performance

```kql
// Slowest API endpoints in the last 24 hours (p50, p90, p99 latency)
requests
| where timestamp > ago(24h)
| where url !contains "/actuator/"
| summarize
    p50 = percentile(duration, 50),
    p90 = percentile(duration, 90),
    p99 = percentile(duration, 99),
    count = count()
    by name
| order by p99 desc
```

```kql
// Requests slower than 2 seconds in the last 6 hours
requests
| where timestamp > ago(6h)
| where duration > 2000
| project timestamp, name, duration, resultCode, cloud_RoleInstance
| order by duration desc
```

```kql
// Average response time per hour — trend over the last 7 days
requests
| where timestamp > ago(7d)
| where url !contains "/actuator/"
| summarize avgDuration = avg(duration) by bin(timestamp, 1h)
| render timechart
```

---

## Rate Limiting & Redis

```kql
// Rate limit exceeded events (HTTP 429 responses)
requests
| where timestamp > ago(24h)
| where resultCode == "429"
| summarize count() by bin(timestamp, 5m), cloud_RoleInstance
| render timechart
```

```kql
// Redis infrastructure failures — CriticalInfrastructureException logs
traces
| where timestamp > ago(24h)
| where message contains "Redis" and severityLevel >= 3
| project timestamp, message,
    logger = tostring(customDimensions.logger_name),
    instanceId = tostring(customDimensions.instanceId)
| order by timestamp desc
```

---

## Blob Storage

```kql
// Blob upload/download failures
traces
| where timestamp > ago(24h)
| where message contains "Blob" and severityLevel >= 3
| project timestamp, message,
    logger = tostring(customDimensions.logger_name)
| order by timestamp desc
```

```kql
// Blob storage dependency calls — latency and failures
dependencies
| where timestamp > ago(24h)
| where type == "Azure blob"
| summarize
    avgDuration = avg(duration),
    failureCount = countif(success == false),
    totalCount = count()
    by name
| order by failureCount desc
```

---

## Authentication

```kql
// Failed login attempts (401 responses)
requests
| where timestamp > ago(24h)
| where resultCode == "401"
| summarize count() by bin(timestamp, 15m)
| render timechart
```

```kql
// Auth-related error logs
traces
| where timestamp > ago(24h)
| where (message contains "auth" or message contains "JWT" or message contains "token")
    and severityLevel >= 3
| project timestamp, message,
    logger = tostring(customDimensions.logger_name)
| order by timestamp desc
```

---

## Database (HikariCP & SQL)

```kql
// SQL dependency calls — slow queries over 1 second
dependencies
| where timestamp > ago(24h)
| where type == "SQL"
| where duration > 1000
| project timestamp, name, duration, success, data
| order by duration desc
```

```kql
// HikariCP leak detection warnings
traces
| where timestamp > ago(24h)
| where message contains "leak detection"
| project timestamp, message,
    logger = tostring(customDimensions.logger_name)
| order by timestamp desc
```

---

## Traffic Overview

```kql
// Requests per minute — traffic pattern over the last 24 hours
requests
| where timestamp > ago(24h)
| summarize requestCount = count() by bin(timestamp, 1m)
| render timechart
```

```kql
// HTTP status code distribution — last 24 hours
requests
| where timestamp > ago(24h)
| summarize count() by resultCode
| order by count_ desc
| render piechart
```

```kql
// Top 10 most-called endpoints
requests
| where timestamp > ago(24h)
| where url !contains "/actuator/"
| summarize count() by name
| top 10 by count_
```

---

## Per-Instance Monitoring

```kql
// Request count per instance — check load balancing distribution
requests
| where timestamp > ago(24h)
| summarize count() by cloud_RoleInstance
| render piechart
```

```kql
// Error rate per instance — spot a single bad instance
requests
| where timestamp > ago(24h)
| summarize
    totalRequests = count(),
    failedRequests = countif(toint(resultCode) >= 500)
    by cloud_RoleInstance
| extend errorRate = round(100.0 * failedRequests / totalRequests, 2)
| order by errorRate desc
```

---

## Custom Metrics (Micrometer)

```kql
// Rate limit rejection count over time
customMetrics
| where timestamp > ago(24h)
| where name == "rate_limit_rejections_total"
| summarize sum(valueSum) by bin(timestamp, 5m)
| render timechart
```

```kql
// Blob upload count and failure rate
customMetrics
| where timestamp > ago(24h)
| where name startswith "blob_uploads"
| summarize sum(valueSum) by name, bin(timestamp, 1h)
| render timechart
```

```kql
// Active user sessions gauge
customMetrics
| where timestamp > ago(24h)
| where name == "active_sessions"
| summarize avg(value) by bin(timestamp, 5m)
| render timechart
```
