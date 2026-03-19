# ADR-007: Redis as Unified Cache and Coordination Layer

**Date:** 2026-02-19  
**Status:** Accepted

## Context

The application requires several distributed coordination primitives across
its two App Service instances:

1. **Rate limiting** — per-user sliding window counters (Bucket4j)
2. **Presence tracking** — sorted sets with last-seen timestamps
3. **Download budget enforcement** — re-download guard (3 re-downloads per
   8-hour window per file per user)
4. **Burst limiting** — image change limiter (5 changes per 5-minute window)
5. **Distributed locks** — email outbox drainer and image moderation drainer
   (single-instance-at-a-time execution across the two App Service instances)

Three caching options were considered:

- **In-memory (ConcurrentHashMap / Caffeine):** Zero cost, but state is lost
  on restart and not shared across instances. Rate limiting and presence
  tracking would be per-instance, not per-user.
- **Memcached:** Distributed, but lacks sorted sets (needed for presence) and
  atomic counter operations (needed for rate limiting).
- **Redis:** Supports all required data structures — atomic counters, sorted
  sets, key expiry, and distributed locks (via `SET NX`) — in a single
  managed instance.

## Decision

Use **Azure Managed Redis (Balanced B0)** as the single coordination layer
for all six concerns, with disjoint key namespaces to prevent collisions.

Rate limiting and download budget enforcement operate as **fail-closed** — if
Redis is unavailable, requests are rejected (HTTP 503). This ensures a Redis
outage cannot cause unbounded cost exposure from unmetered API calls.

Presence tracking and convenience features operate as **fail-open** — a Redis
outage degrades the user experience (stale presence, no burst limiting) but
does not block core functionality.

## Consequences

- **Positive:** One ~$11/month managed instance serves six distinct concerns.
  No additional infrastructure for locks, presence, or rate limiting.
- **Positive:** Fail-closed/fail-open asymmetry ensures cost-generating
  operations are always gated, while non-critical features degrade gracefully.
- **Positive:** Managed Redis provides automatic patching, monitoring, and
  replication without operational overhead.
- **Negative:** Single point of failure for rate limiting — a Redis outage
  causes all API requests to return 503. Acceptable given the B0 tier's SLA
  and the alternative (unmetered access during outages) being worse.
- **Negative:** B0 tier has limited throughput (~256 MB, no clustering).
  Sufficient for the current user base; the scaling roadmap triggers a
  B0 → B1 upgrade when memory utilisation exceeds 80%.
