# MyTeamTasks

[![Backend CI/CD](https://github.com/balasis-org/task-manager-app/actions/workflows/backend-ci-cd.yml/badge.svg)](https://github.com/balasis-org/task-manager-app/actions/workflows/backend-ci-cd.yml)
[![Frontend CI/CD](https://github.com/balasis-org/task-manager-app/actions/workflows/frontend-ci-cd.yml/badge.svg)](https://github.com/balasis-org/task-manager-app/actions/workflows/frontend-ci-cd.yml)
[![Maintenance CI/CD](https://github.com/balasis-org/task-manager-app/actions/workflows/maintenance-ci-cd.yml/badge.svg)](https://github.com/balasis-org/task-manager-app/actions/workflows/maintenance-ci-cd.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](backend/pom.xml)
[![React](https://img.shields.io/badge/React-19-61DAFB)](frontend/package.json)
[![Azure](https://img.shields.io/badge/Azure-16%20PaaS%20resources-0078D4)](#architecture)
[![OWASP](https://img.shields.io/badge/OWASP-41%20assertions%20passed-2ea44f)](#security-testing)

BSc thesis project — John Balasis · CSY22117  
AthensTech College · Academic collaboration with CITY College, University of York Europe Campus
---

[[[Image here: hero screenshot — group dashboard or task board view, dark mode, showing real UI with tasks and members]]]

a containerised, full-stack group task management platform deployed on 16 Azure PaaS resources. built as a BSc thesis to prove that enterprise-grade security, AI-assisted workflows, and operational automation can work on a student budget (~$108–128/month baseline — stress-tested and validated; scaling levers documented in §6.5 of the thesis). not a demo. not a prototype. nine OWASP attack simulations run 41 assertions against the live system — 100% pass rate. the FREE tier generates exactly $0 variable Azure cost. every cost-generating Azure API call is gated by an atomic SQL check before it executes.


## highlights

| | | |
|:--|:--|:--|
| **16** Azure PaaS resources — one Bicep template | **41** OWASP security assertions — 100% pass | **$0** variable cost on the FREE tier |
| **7** independent security layers | **18** JPA entities, **75+** custom queries | **<$0.002** per maintenance job run |
| **9** automated k6 attack simulations | **AES-256-GCM** encrypted client-side cache | **AI** content moderation + comment intelligence |


## what it does

- **groups** — create groups, invite members by email, assign roles (GROUP_LEADER, TASK_MANAGER, REVIEWER, MEMBER, GUEST) each with distinct permissions enforced server-side
- **tasks** — full lifecycle with status transitions, assignees, file attachments per role (creator and assignee quotas enforced independently), comments, and review submissions
- **files** — upload images and documents, server-side resize (3 MB photos → ~20 KB thumbnails), AI content moderation via Azure Content Safety, 64 KB streamed downloads with ETag/304 fast-paths that bypass all downstream cost
- **real-time** — smart polling with four-phase idle degradation (ACTIVE 30s → ACTIVE-LONG 60s → SLOW 60s → STOP), presence tracking piggybacked on change-detection calls, differential sync reducing polling bandwidth by ~99.5% vs naive full-fetch
- **encrypted cache** — AES-256-GCM with PBKDF2-derived keys (100k iterations) in localStorage, stale-while-revalidate pattern, automatic key rotation every 7 days, multi-user eviction (up to 3 users per device)
- **email** — group invitations and critical system alerts via Azure Communication Services, fire-and-forget async delivery
- **subscriptions** — five tiers with atomic SQL budget enforcement (`UPDATE ... WHERE budget >= cost`) — if the budget is exceeded, the Azure API is never invoked. no distributed locks, no race conditions
- **comment intelligence** — TEAMS_PRO tier gets AI-powered comment analysis (sentiment, key phrases, PII detection, summarisation) via Azure AI Language, with an 8,000 credit/month budget and async outbox-drainer architecture
- **admin panel** — system-wide user management, plan assignment, searchable paginated tables with debounced input, admin role assignable only via Key Vault secret

[[[Image here: 3-4 panel collage — group list view, task detail with files, admin panel, and maybe the login/landing page]]]


## security

the security architecture covers seven independent layers. no single failure compromises the system.

**authentication** — Azure AD (Entra ID) via OAuth 2.0 Authorization Code Grant. the backend is a confidential client (BFF pattern): the client secret never leaves Key Vault, the frontend never holds any Azure AD token. after login, the backend issues a 15-minute HMAC-SHA256 JWT and a 24-hour refresh token — both stored in `httpOnly; Secure; SameSite=Strict` cookies. refresh token rotation invalidates stolen tokens on legitimate use. on logout, tokens are deleted server-side.

**rate limiting** — three independent layers:
1. WAF edge (per-IP, blocks volumetric floods before reaching the backend)
2. Redis Bucket4j (per-user, 40 req/min + 420 req/15 min sliding window, **fail-closed** — if Redis is down, all requests are rejected)
3. domain-specific limiters (download concurrency gate, re-download guard, image change burst limiter)

**input sanitisation** — five layers: `SanitizingRequestBodyAdvice` (global HTML/script stripping), `InputSanitizer` (12 categories of Unicode invisible characters), `StringSanitizer` (blob key allowlisting, path traversal prevention), `@ValidEnum` (enum set validation), and 100% parameterised JPQL (zero native SQL).

**RBAC** — two-level: system roles (USER, ADMIN) and per-group roles (GROUP_LEADER, TASK_MANAGER, REVIEWER, MEMBER, GUEST). checked via `EnumSet` comparison in O(1). admin escalation is architecturally impossible — `ADMIN` is assigned only via a Key Vault secret at login.

**origin protection** — Front Door guards both origins: the web app has three-layer verification (AzureFrontDoor.Backend service tag + X-Azure-FDID header + Front Door's MI Bearer token validated by Easy Auth). the blob origin uses a separate MI-authenticated RBAC path.

**data protection** — Managed Identity is the single credential for SQL, Blob, and Key Vault (zero passwords in config). 20 Key Vault secrets fetched once at startup. six security headers enforced at the CDN edge (HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy).


## security testing

nine k6 scripts simulate real attack vectors against the running system, mapped to the OWASP Top 10 (2021). **41 individual assertions, 100% pass rate.**

| script | OWASP | what it tests |
|---|---|---|
| 01-auth-bypass | A07 | anonymous access, forged JWT, expired JWT, garbage refresh token |
| 02-rate-limit-hammer | A04 | 60 rapid requests — confirms Bucket4j activates within 40 with Retry-After |
| 03-input-abuse | A03 | blank fields, oversized inputs, malformed JSON, out-of-range values |
| 04-unauthorized-access | A01 | outsider tries to access another user's group resources |
| 05-upload-header-pollution | A03/A04 | spoofed Content-Type, GIF format, oversized file, path traversal filename |
| 06-idor-resource-tampering | A01 | fabricated file IDs, cross-user task deletion, phantom group access |
| 07-xss-html-injection | A03 | script tags, img/onerror, svg/onload, Unicode-escaped payloads |
| 08-session-manipulation | A07 | session fixation, token replay, CRLF injection, mutated JWT |
| 09-privilege-escalation | A01 | MEMBER attempts GROUP_LEADER-only operations |

three stress tests validate the B1 tier under concurrent load: polling load (30 VUs, 2s intervals) and presence storm (50 VUs, 30s intervals). these run against a dedicated arena environment deployed from the same Bicep template — production defences intact, no risk to real users.


## cost engineering

total Azure cost: ~$108–128/month (initial baseline for current SKU selection — validated by k6 stress tests under aggressive concurrency scenarios; metric-based scaling triggers and first upgrade levers are documented in the thesis §6.5). cost isn't an afterthought — it's a first-class architectural concern that shaped 12 major design decisions.

every cost-generating API call (upload, download, email, image scan, AI analysis) is gated by an atomic SQL check before the Azure API is invoked:

```sql
UPDATE users SET usedX = usedX + :cost WHERE usedX + :cost <= :budget
```

if the WHERE clause matches zero rows, the operation is rejected — no distributed locks, no race conditions, just database row-level serialisation.

| resource | SKU | ~monthly |
|---|---|---|
| App Service (×2 instances) | B1 Linux | $27 |
| Azure SQL | S1 (20 DTU) | $30 |
| Front Door + WAF | Standard | $38 |
| Redis | Enterprise B0 | $11 |
| ACR | Basic | $5 |
| everything else (Key Vault, ACS, Content Safety, AI Language, Blob, monitoring) | various | ~$2–17 |

the FREE tier generates exactly $0 in variable Azure cost (email and scan limits are zero, storage is minimal, downloads capped at 500 MB/month). paid tier budgets cap maximum variable cost per user. seven automated maintenance services prevent unbounded growth.


## architecture

[[[Image here: high-level system architecture diagram — React SPA → Azure Front Door (CDN + WAF) → two origin paths: (1) Blob Storage for static assets, (2) App Service for /api/* → backend connects to Azure SQL, Redis, Blob Storage, Content Safety, AI Language (Text Analytics), ACS, Key Vault → Container App Jobs for maintenance → all via Managed Identity]]]

the system follows a three-tier architecture with Azure Front Door as the single entry point. static assets (the React SPA) are served from Blob Storage via CDN — zero App Service compute for the UI. API calls (`/api/*`) are proxied to the backend. WAF rules fire before any request reaches application code. one external domain = no CORS, no mixed-content, no split DNS.

**backend** — Spring Boot 3.5 / Java 21, structured as a 10-module Maven reactor. the codebase enforces compile-time separation between engine modules (domain logic — entities, repositories, services, Azure adapters — no HTTP awareness) and context modules (HTTP delivery — controllers, interceptors, DTOs, exception handler). if a controller class imports something from the Redis adapter, the build fails. this is dependency enforcement via Maven's module graph, not convention. the request pipeline is fixed and auditable: WAF → rate limiting → JWT validation → XSS sanitisation → controller → authorisation check → business validation → service → parameterised JPQL.

**frontend** — React 19 with the React Compiler (automatic memoisation), Vite 7, ~74 JSX files across 15 routes and 50+ reusable components. five Context providers in dependency order. dark mode via CSS custom properties with pre-hydration flash prevention.

**database** — Azure SQL (S1, 20 DTU) with 18 JPA entities, 75+ custom `@Query` methods, nine Flyway migrations. every association-loading query uses `LEFT JOIN FETCH` (1 SQL statement instead of 201 for N+1 problems). H2 compatibility mode for zero-cost local development.

**maintenance** — two Azure Container App Jobs (billed per-execution, <$0.002/run): a daily full sweep (orphan blob cleanup, inactive user anonymisation, expired invite cleanup, budget reconciliation, counter resets, ACR image pruning) and a 30-minute blob-only scan for fast orphan detection.


## stack

| layer | technology |
|---|---|
| backend | Java 21, Spring Boot 3.5.7, 10-module Maven reactor |
| frontend | React 19 + React Compiler, Vite 7, vanilla CSS custom properties |
| database | Azure SQL (MSSQL) + Flyway migrations · H2/docker-mssql for local dev |
| caching | Azure Managed Redis (Balanced B0) — 7 services with disjoint key namespaces |
| storage | Azure Blob Storage (Standard_LRS) — 5 app containers + 3 frontend containers |
| CDN / edge | Azure Front Door (Standard) + custom WAF policy |
| auth | Azure AD (Entra ID) · OAuth 2.0 Authorization Code Grant · HMAC-SHA256 JWT in httpOnly cookies |
| email | Azure Communication Services (Managed Identity) |
| moderation | Azure AI Content Safety (S0) — fail-open policy, budget-gated per tier |
| comment intelligence | Azure AI Language (Text Analytics S) — sentiment, key phrases, PII, summarisation. TEAMS_PRO only, 8,000 credits/month |
| secrets | Azure Key Vault — single trust root, Managed Identity, startup-only fetch |
| registry | Azure Container Registry (Basic) — auto-pruned to 2 tags per repo |
| monitoring | Application Insights + OpenTelemetry (10% sampling, polling traces dropped) · 7 Micrometer metrics · structured JSON logging |
| CI/CD | GitHub Actions (4 workflows) · Bicep IaC · GitHub Environments |
| testing | k6 — 9 OWASP attack scripts (41 assertions) + 3 stress tests |


## CI/CD

four GitHub Actions workflows:

- **deploy-infra** — manually triggered. provisions all 16 Azure resources via Bicep and writes generated names (ACR, web app, storage account, Front Door endpoint) back to GitHub Environment variables using the GH CLI. the three deployment workflows reference these dynamically — zero hardcoded resource names. cross-platform PowerShell scripts serve as a manual fallback.
- **backend-ci-cd** — triggers on pushes to main touching `backend/**` or `shared/**`. builds + tests the Spring Boot app, pushes a Docker image to ACR with a timestamp tag (`yyyyMMddHHmmss`), updates the App Service container.
- **frontend-ci-cd** — triggers on pushes to main touching `frontend/**`. builds the React app, uploads static files to Blob Storage (`$web`), purges the Front Door cache.
- **maintenance-ci-cd** — triggers on pushes to main touching `maintenance/**`. builds the maintenance jar, pushes to ACR, updates both ACA cron jobs.

path-based filtering ensures unrelated changes never trigger unnecessary builds. timestamp tags enable precise rollback without rebuild. all credentials live in GitHub encrypted Secrets, scoped to minimum permissions.


## monitoring

- OpenTelemetry distributed tracing → Application Insights (10% base sampling, polling traces dropped by custom `PollingEndpointSampler`)
- 7 custom Micrometer metrics via Spring AOP (rate limit rejections, blob ops, auth attempts, critical exceptions)
- structured JSON logging (`LogstashEncoder`) with MDC context fields, queryable via KQL
- 13 pre-built KQL diagnostic queries across 8 categories
- `CriticalExceptionAlerter` — admin email with exponential backoff (1 min → 4h cap) and instance-ID correlation
- `MaintenanceStalenessChecker` — alerts if the cleanup job hasn't completed in 12 hours
- custom health indicators for Blob Storage and Redis feeding into App Service / Front Door routing


## local development

the full Azure topology is replicated locally using Docker containers — zero cloud costs during development.

| Azure service | local replacement | compose file |
|---|---|---|
| Azure SQL | SQL Server in Docker | `backend-db-compose.yml` |
| Blob Storage | Azurite emulator | `backend-blob-compose.yml` |
| Redis | Redis on tmpfs | `backend-redis-compose.yml` |
| ACS (email) | MailHog SMTP capture | `backend-email-compose.yml` |

Spring profiles switch between local and Azure adapters. identical business logic runs in both environments — only authentication and infrastructure adapter beans change.

see **[local development guide](docs/local-development.md)** for full setup instructions.


## project layout

```
backend/                        spring boot 10-module reactor
  engine/                         domain: entities, services, azure adapters, monitoring
  context/                        HTTP: controllers, interceptors, DTOs, security filters
frontend/                       react 19 SPA (vite)
shared/                         shared enums (backend + maintenance)
maintenance/                    scheduled cleanup jobs (container app jobs)
k6/                             k6 attack simulations + stress tests
.github/workflows/              4 workflows: infra, backend, frontend, maintenance
infrastructure/                 Bicep templates, param files, fallback PS scripts
infrastructure/manual-setup/    one-time Azure setup + post-deployment
```


## guides

- **[Local development](docs/local-development.md)** — run the full stack locally with Docker
- **[Production deployment](infrastructure/README.MD)** — Bicep provisioning, GitHub Environments setup, CI/CD configuration
- **[Azure manual setup](infrastructure/manual-setup/)** — service principal, OAuth 2.0 app registration, post-deployment Key Vault setup
- **[Architecture Decision Records](docs/adr/)** — 10 ADRs documenting major technical decisions with alternatives and trade-offs
- **[Versioning](docs/versioning.md)** — milestone changelog from v0.1.0 through current release


---

*16 Azure resources · 10 Maven modules · 75+ REST endpoints · 16 JPA entities · 10 Flyway migrations · 5 subscription tiers · 9 OWASP attack scripts · 41 security assertions · $108–128/month*
