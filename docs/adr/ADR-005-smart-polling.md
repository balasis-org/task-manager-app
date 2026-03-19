# ADR-005: Smart Polling over WebSockets

**Date:** 2026-02-15  
**Status:** Accepted

## Context

The application requires near-real-time updates for group task boards:
new tasks, status changes, file uploads, comments, and member presence.

Three approaches were considered:

- **WebSockets:** Full-duplex persistent connections providing instant push
  notifications. However, Azure Front Door Standard does not support WebSocket
  proxying — upgrading to Front Door Premium (~$335/month) would triple the
  edge infrastructure cost from ~$38/month.
- **Server-Sent Events (SSE):** Unidirectional push from server to client.
  Simpler than WebSockets but still requires holding persistent connections,
  which compete with the B1 tier's limited connection pool.
- **Smart polling:** The client polls at adaptive intervals, with the server
  returning only changes since the last request (differential sync).

## Decision

Implement a **four-phase smart polling architecture** with differential sync:

1. **ACTIVE** (30s) — user is actively interacting
2. **ACTIVE-LONG** (60s) — user is present but idle for a short period
3. **SLOW** (60s) — user has been idle longer
4. **STOP** — 15 minutes of inactivity; polling halts entirely with a stale
   indicator in the UI

Polling responses carry only the delta since the client's last known state.
The server compares timestamps and returns created/updated/deleted entities,
reducing payload size by ~99.5% compared to naive full-fetch polling.

Presence tracking is piggybacked on change-detection calls — no additional
API requests needed. The server tracks last-seen timestamps in Redis sorted
sets, and the polling response includes an updated presence list.

## Consequences

- **Positive:** Works through Azure Front Door Standard with no infrastructure
  changes. No persistent connections consuming the B1 connection pool.
- **Positive:** Differential sync minimises bandwidth and server load. A
  typical polling response is <1 KB when nothing has changed.
- **Positive:** Four-phase degradation means inactive tabs generate zero
  traffic after 15 minutes.
- **Negative:** Worst-case update latency is 30 seconds (ACTIVE phase).
  Acceptable for a task management application where sub-second updates are
  not required.
- **Negative:** The differential sync logic in `GroupServiceImpl` (~1,600
  lines) is complex — it must track deletions (via `DeletedTask` tombstones),
  compute membership changes, and merge presence data into the response.
