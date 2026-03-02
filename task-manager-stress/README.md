# task-manager-stress

Load and stress testing for myteamtasks, using [k6](https://k6.io/) (Grafana).

## Prerequisites

| Tool | Install | Verify |
|------|---------|--------|
| **k6** | `winget install GrafanaLabs.k6` | `k6 version` |
| **Backend** | Docker compose or local Spring Boot | `curl http://localhost:8080/actuator/health` |

k6 is a standalone Go binary — no Node.js / npm needed.

## Architecture

```
task-manager-stress/
    k6/
        config.js                    # Base URL, user lists
        scripts/
            helpers.js               # Shared login/poll/presence helpers
            presence-storm.js        # 50 VUs polling at real intervals (30s)
            polling-load.js          # 30 VUs polling aggressively (2s)
```

## Seed Data

The backend `DataLoader` (profile `DataLoader`) creates:

- **12 core users** (alice, bob, carol, …, oscar) in Group A and B
- **38 stress users** (stress01–stress38) — all members of **Stress Test Group C**
- **Stress Test Group C** has all 50 users (led by Alice)

If using Flyway, the DataLoader seeds on first run.  Subsequent runs detect
existing data and skip.  To re-seed: drop/recreate the database.

## Running

### 1. Presence Storm (realistic polling)

Simulates 50 users each polling every 30 seconds (identical to the frontend).
After a few cycles, the presence overlay should show ~50 users online.

```powershell
cd task-manager-stress/k6
k6 run scripts/presence-storm.js
```

Override settings:

```powershell
# Only 20 users, 1 minute
k6 run -e VUS=20 -e DURATION=1m scripts/presence-storm.js

# Against Azure backend (direct App Service URL — IP-whitelisted)
k6 run -e BASE_URL=https://myteamtasks-backend.azurewebsites.net scripts/presence-storm.js
```

### 2. Polling Load Test (stress)

Pushes each VU to poll every 2 seconds (~30 req/min per user).
Expects some 429 (rate-limited) responses and verifies the rate limiter works.

```powershell
cd task-manager-stress/k6
k6 run scripts/polling-load.js
```

### 3. Watch the UI in parallel

While k6 runs, open a browser and log in as **Alice Dev** (Group C leader).
Navigate to the dashboard and select "Stress Test Group C".
The presence bar should fill with online avatars.

## Metrics

k6 outputs a summary table at the end of each run.  Custom metrics:

| Metric | Type | Script | Meaning |
|--------|------|--------|---------|
| `presence_online_count` | Gauge | presence-storm | How many users the /presence endpoint reports |
| `presence_latency_ms` | Trend | presence-storm | p50/p95/p99 of /presence calls |
| `heartbeat_ok` | Counter | presence-storm | Successful has-changed calls |
| `heartbeat_fail` | Counter | presence-storm | Failed has-changed calls |
| `rate_limited_429` | Counter | polling-load | Times Bucket4j returned 429 |
| `rate_limit_pct` | Rate | polling-load | Percentage of requests that were rate-limited |

## Testing Against Production (Azure)

**Do NOT block the public app** — instead, deploy a separate **test App Service** or use the
**App Service's direct URL** with a temporary IP access restriction:

```powershell
# Temporarily allow your IP to hit the App Service directly (bypasses Front Door)
az webapp config access-restriction add `
    --resource-group myteamtasks-rg `
    --name myteamtasks-backend `
    --rule-name StressTest `
    --priority 100 `
    --ip-address YOUR.PUBLIC.IP/32

# Run k6 against the direct URL
k6 run -e BASE_URL=https://myteamtasks-backend.azurewebsites.net scripts/presence-storm.js

# Clean up
az webapp config access-restriction remove `
    --resource-group myteamtasks-rg `
    --name myteamtasks-backend `
    --rule-name StressTest
```

For professional production testing, the recommended approach is a dedicated
test App Service + database (same SKU as production) that can be spun up on
demand and torn down after tests.
