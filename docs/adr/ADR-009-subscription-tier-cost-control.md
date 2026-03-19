# ADR-009: Five-Tier Subscription Model as Cost Control

**Date:** 2026-02-19  
**Status:** Accepted

## Context

Every user action that consumes Azure resources has a marginal cost: blob
storage (Standard_LRS at-rest), blob download egress, email delivery via ACS,
image moderation via Content Safety, and AI analysis via AI Language. Without
application-level controls, a single user could generate unbounded Azure
charges.

Traditional approaches to cost control — infrastructure-level quotas, WAF rate
limits, or API gateway throttling — operate at the request level, not the
resource level. They can prevent volumetric abuse but cannot enforce business
rules like "this user has 500 MB of storage remaining" or "this user has 200
emails left this month."

## Decision

Implement a **five-tier subscription system** (FREE, STUDENT, ORGANIZER, TEAM,
TEAMS_PRO) as a first-class cost-control mechanism, with a `PlanLimits` matrix
governing thirteen numerical limits per tier.

Every cost-generating API call is gated by an **atomic SQL budget check**
before the Azure API is invoked:

```sql
UPDATE users SET usedX = usedX + :cost WHERE usedX + :cost <= :budget
```

If the WHERE clause matches zero rows, the operation is rejected — the Azure
API is never called. No distributed locks, no race conditions — database
row-level serialisation provides the atomicity guarantee.

Five budget types follow this pattern: storage, downloads, email, Content
Safety scans, and AI analysis credits. The FREE tier deliberately zeroes all
cost-generating allowances, producing ~$0 variable Azure cost per user.

The **owner-bears-cost** attribution model assigns every metered operation to
the user who initiated it, not the group that benefits. This prevents
cost-amplification where multiple free collaborators could drain a single
paid user's budget.

## Consequences

- **Positive:** Worst-case variable cost per user is deterministic and bounded
  by their tier's limits. No user can exceed their tier's cost ceiling
  regardless of behaviour.
- **Positive:** The atomic SQL check is a single row-level operation — no
  distributed coordination, no Redis dependency for budget enforcement.
- **Positive:** The FREE tier generates exactly $0 variable Azure cost, making
  it a pure loss leader for conversion with negligible infrastructure cost
  (~$0.02/month per user).
- **Negative:** Tier enforcement adds a SQL query to every cost-generating API
  path. Mitigated by the queries being single-row updates with indexed
  primary keys — sub-millisecond execution.
- **Negative:** The owner-bears-cost model means group leaders on paid tiers
  pay for operations they initiate on behalf of the group, even if group
  members are on free tiers. This is intentional — it simplifies attribution
  and prevents cost-amplification vectors.
