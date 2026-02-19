# Task Manager

**Thesis project** — John Balasis · CSY22117 · AthensTech College (York University base)

---

a production-oriented group task management platform built as a thesis submission. the goal was never to just "make it work for the demo" — every layer was designed with Azure PaaS, real security patterns, and maintainability in mind.

## what it is

a full-stack web application where users can create groups, manage tasks collaboratively, assign roles, upload files, review task submissions, and communicate via comments. it supports an invitation system, group events log, admin back-office, and email notifications — all running behind Azure infrastructure.

## stack

- **Backend** — Spring Boot 3.5.7, Java 21, 12-module Maven reactor
- **Frontend** — React 19.2, Vite 7.3, nginx (Dockerized SPA)
- **Database** — Azure SQL (MSSQL), Flyway for schema migration, Hibernate for dev
- **Infrastructure** — Azure Web App for Containers, Azure Container Registry, Azure Front Door + CDN

## architecture

the backend is structured as a multi-module Maven project with a clean separation between context (controllers, DTOs, interceptors), engine (services, repositories, validators, infrastructure integrations), and contracts (shared enums). every HTTP request passes through a fixed pipeline: rate limiting → JWT validation → XSS sanitization → controller → authorization check → business validation → service.

the frontend is a React SPA with an encrypted localStorage cache (AES-256-GCM via Web Crypto API), a context-based group state manager with differential sync, and a tiered smart-polling system that backs off progressively based on user activity.

## security

a lot of effort went into this:

- **authentication** — Azure AD (Entra ID) OAuth 2.0 authorization code flow, producing two HTTP-only `Secure SameSite=Strict` cookies: a 10-minute JWT and a 24-hour refresh token with full rotation (each use issues a new token, old one immediately invalidated)
- **rate limiting** — Redis-backed distributed token-bucket via Bucket4j + Lettuce. dual window: 40 req/min and 420 req/15min per IP, enforced before auth, fails-open if Redis is unavailable
- **XSS sanitization** — all inbound request DTOs are automatically sanitized through `SanitizingRequestBodyAdvice`, stripping HTML/scripts and invisible Unicode characters (with intentional preservation of ZWJ/ZWNJ for emoji)
- **content safety** — every image upload passes through Azure AI Content Safety (hate/violence/self-harm/sexual analysis, threshold < 3/6)
- **encrypted frontend cache** — group data stored in localStorage only after AES-256-GCM encryption with a per-user key derived via PBKDF2. the raw key lives only in memory — never written to storage
- **RBAC** — two-level: system roles (USER/ADMIN) and per-group roles (GROUP_LEADER, TASK_MANAGER, REVIEWER, MEMBER, GUEST), enforced at both authorization service and business validator layers
- **startup gate** — a `StartupBlockingFilter` returns HTTP 503 until two internal bootstrap gates are cleared (default images seeded + dev data loaded). no request leaks through during cold start

## notable features

- **differential polling** — instead of re-fetching the whole group on every poll, the client sends a `lastSeen` timestamp and the server returns only what changed (updated tasks, deleted task IDs, member changes). uses `DeletedTask` tombstones for deletion tracking
- **smart poll hook** — tiered backoff: 30s when active, 60s after mild idle, stops entirely after 15min with a visible stale indicator
- **plan limits** — capacity enforcement baked in at service level (max users, groups per user, tasks per group)
- **admin panel** — full back-office with paginated search over users, groups, tasks, comments, file downloads
- **Flyway baseline** — `V1__baseline.sql` captures the full schema for migration testing. a separate `dev-flyway-mssql` profile spins against an isolated MSSQL container (port 1434) with `ddl-auto: validate` to verify migrations before merging to prod
- **maintenance module** — standalone Spring Boot app for scheduled orphan blob cleanup and inactive user pruning, with its own CICD pipeline
- **Azure Key Vault** — all secrets (JWT key, DB connection, API keys, Redis credentials) fetched from Key Vault in prod via Managed Identity — nothing hardcoded
- **Azure Blob Storage** — 5-container architecture (profile images, group images, task files, assignee files, default avatars), public containers served via CDN, private containers gated behind authenticated API endpoints
- **OpenTelemetry + Application Insights** — distributed tracing, dependency tracking, and log correlation via the additive `OpenTelemetry` Spring profile

## project layout

```
task-manager-app/
├── task-manager-backend/       ← Spring Boot multi-module reactor
├── task-manager-frontend/      ← Vite + React SPA
├── task-manager-contracts/     ← shared enums (backend + maintenance)
├── task-manager-maintenance/   ← scheduled cleanup jobs
└── docs/                       ← versioning log, this file, thesis docs
```

## docs

- `versioning.md` — milestone changelog anchored to git tags (v0.1.0 → current)
- `PROJECT_DOCUMENTATION.md` — deep-dive reference covering every layer of the system

---

*this is a thesis submission for CSY22117 at AthensTech College. the codebase is intentionally built beyond the scope of a typical academic project — the aim was to get as close to a real production deployment as possible within the constraints of the work.*
