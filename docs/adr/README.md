# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) documenting the significant
technical decisions made during the design and development of MyTeamTasks.

Each ADR follows [Michael Nygard's template](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions):

- **Status** — Accepted, Superseded, or Deprecated
- **Context** — The forces at play, including technical, political, and project-specific constraints
- **Decision** — What was decided and why
- **Consequences** — The resulting context after the decision, both positive and negative

## Index

| # | Decision | Status |
|---|----------|--------|
| [001](ADR-001-modular-monolith.md) | Modular monolith over microservices | Accepted |
| [002](ADR-002-azure-paas.md) | Azure PaaS over IaaS and FaaS | Accepted |
| [003](ADR-003-bff-oauth.md) | BFF OAuth 2.0 over client-side tokens | Accepted |
| [004](ADR-004-custom-interceptors.md) | Custom interceptors over Spring Security | Accepted |
| [005](ADR-005-smart-polling.md) | Smart polling over WebSockets | Accepted |
| [006](ADR-006-bicep-over-terraform.md) | Bicep over Terraform | Accepted |
| [007](ADR-007-redis-unified-cache.md) | Redis as unified cache and coordination layer | Accepted |
| [008](ADR-008-blob-origin-auth.md) | Blob Storage with Front Door Origin Auth over SAS tokens | Accepted |
| [009](ADR-009-subscription-tier-cost-control.md) | Five-tier subscription model as cost control | Accepted |
| [010](ADR-010-k6-testing.md) | k6 over JMeter and Gatling | Accepted |
