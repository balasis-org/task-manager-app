# Versioning Log

Tracks project milestones anchored to git tags.
Earlier versions focused mostly on backend work, but from v0.5 onwards the scope covers the full stack.

## Versioning Scheme
- **0.x.y** — pre-release / development
    - x = milestone increment
    - y = patch or small fix
- **1.0.0** — first production-ready release

---

## Versions

### v0.1.0 — Initial backend baseline
- Date: 2025-10-27
- Tag target (commit): `151288ed375cc4c71f6e2239ddec92ede1b16dac`
- Notes:
    - Initial backend baseline, project structure set up.
    - First version bump to `0.1.0-SNAPSHOT`.

### v0.2.0 — Azure SQL + Key Vault wiring (backend containerization)
- Date: 2025-11-07
- Tag object: `02ac703d0670449748d649d8fa2b40c822196ef5` (annotated)
- Tag target (commit): `33d2c02b1a22c47d72f79e7d4dc1ceff833a881c`
- Highlights:
    - Docker setup and compose files for Azure-like deployments.
    - MSSQL / Azure SQL profiles added (`application-dev-mssql.yml`, `application-prod-azuresql.yml`, etc).
    - Azure Key Vault integration for SQL credentials.
    - Dev bootstrapping cleanup, DataLoader defaults tuned.

### v0.3.0 — Azure integrations (Auth/Blob/Email/Observability) + CI
- Date: 2025-11-21
- Tag object: `6c26e835855c6167fab662cb06136858145bb9df` (annotated)
- Tag target (commit): `b77b91f3bd0d723bb16d6aa11686388d8a4ce7f5`
- Highlights:
    - Azure AD auth flow (OAuth2), dev-tested + auth wiring refactored.
    - Blob storage integration (Azure + local Azurite).
    - Email integration — prod via ACS, dev via SMTP/mailhog.
    - OpenTelemetry plumbing and API response-time aspect.
    - Backend CI + GitHub Action for deploys.

### v0.4.0 — Collaboration/workflow APIs + invitation UX primitives
- Date: 2026-02-15
- Tag object: `00a25fac6474de785945e842dbbc6721456f7ced` (annotated)
- Tag target (commit): `d08fd4b6703cfeb3bae48796bf2054677376eae9`
- Highlights:
    - Group/task collaboration APIs expanded — task preview DTOs, comments CRUD, review flow, to-be-reviewed workflow, events listing.
    - Invitations and membership management strengthened:
        - Role choice + status flow improvements.
        - Pending-only "my invites" semantics, inviter-only cancel.
        - Seen/unseen via invite timestamps + 'mark seen on fetch'.
        - Membership search, removal (self-remove included), role-change/swap logic.
    - File download APIs for task files + assignee files. Blob service refined for multiple containers.
    - Blob garbage-collector groundwork, content-safety hooks for uploads.
    - Refresh-token support + JWT/auth hardening.

### v0.5.0 — Full React SPA + admin panel + rate limiting + Flyway baseline
- Date: 2026-02-19
- Tag object: `b044998d8313494b8a4b8494ea5c5d14e1210053` (annotated)
- Tag target (commit): `d28ad184624004f5ec0b6be909295173989aac3b`
- Highlights:

    **Frontend — complete SPA built from scratch (replaces the old frontend-demo):**
    - Vite 7.3 + React 19.2 SPA. Pages: Dashboard, Task, Comments, Settings, AdminPanel, Invitations, Login, AuthCallback, AboutUs, TermsOfService, CookiePolicy.
    - `GroupProvider` with AES-256-GCM encrypted localStorage cache — group data encrypted with a per-user backend-issued `cacheKey` held only in memory.
    - `useSmartPoll` — tiered backoff polling (30s/60s active, 60s mild idle, stops at 15 min + stale indicator).
    - `apiClient.js` — centralized fetch wrapper, auto-retry on 401, 429 handling, file upload + blob download.
    - Components: `DashboardTopBar`, `FilterPanel`, `TaskTable`, popup suite (NewGroup, GroupSettings, NewTask, InviteToGroup, GroupEvents).
    - `ToastContext` notification system.
    - Layout with tiered invitation polling (60s / 3min / 45min by idle duration).
    - Protected routes, nginx SPA config, Docker + dev Dockerfile.

    **Backend — admin, rate limiting, startup hardening, XSS sanitisation:**
    - `AdminController` + `AdminService`: full admin CRUD over users, groups, tasks, comments with paginated search. Complete DTO layer + `AdminOutboundMapper`.
    - Redis-backed rate limiting via Bucket4j + Lettuce. Dual-window (40 req/min + 420 req/15min), interceptor runs after JWT, dev/prod configs. Fails-closed when Redis is down.
    - `StartupGate` + `StartupBlockingFilter` — two-gate startup, returns 503 until both gates clear.
    - `InputSanitizer` + `SanitizingRequestBodyAdvice` — auto XSS sanitization of all inbound DTOs (strips HTML, trims invisible Unicode, preserves ZWJ/ZWNJ).
    - `DeletedTask` tombstone entity for the differential refresh endpoint.
    - `GroupRefreshDto` with minimized JSON keys + delta refresh logic in `GroupServiceImpl`.
    - `PlanLimits` — enforces max users, groups per user, tasks per group.
    - `User` model expanded: `cacheKey`, invite code, `isOrg`, email notifications flag.
    - Mini dropdown DTOs and `TaskPreviewDto` refactors for payload minimisation.
    - `DataLoader` overhaul with richer seed data.
    - Repository expansions — delta queries, admin queries, soft-delete lookups.
    - `DevAuthController` + `AuthService` hardening: refresh token rotation, `@Profile` annotations extended.

    **Flyway migration baseline:**
    - `V1__baseline.sql` — full schema, 13 tables, constraint names, indexes, FKs.
    - `application-dev-flyway-mssql.yml` — `ddl-auto: validate` + Flyway for testing migrations locally before prod.
    - `backend-flyway-db-compose.yml` — isolated MSSQL container (port 1434) for Flyway testing.

    **CI/CD and maintenance:**
    - `maintenance-ci-cd.yml` GitHub Action for the maintenance module.
    - `frontend-ci-cd.yml` reworked for Vite SPA.
    - Maintenance module refactored into a Spring Boot app (`MaintenanceCore`, `MaintenanceRunner`), new Dockerfile.
    - Old vanilla HTML/JS frontend-demo removed entirely.

### v0.6.0 — Production hardening, observability and architecture stabilization
- Date: 2026-02-28
- Tag object: `121f6ec402e24247ee0ce733fe20e3e0fa5661d8` (annotated)
- Tag target (commit): `2473588616b552f92bccd51d64e238d60750c592`
- Highlights :

    **Architecture pivots:**
    - Blob access migrated from SAS tokens to Origin Auth. Frontend got a new `BlobSasContext`, backend `BlobStorageService` rewritten. Old `blob-base.jsx` / `api-base.jsx` removed.
    - Frontend de-containerized — Dockerfile, nginx configs and compose removed. SPA now deployed as static assets to Azure Blob Storage behind Front Door.
    - Rate limiting switched from IP-based to userId-based keying (Redis). Dev/prod configs split out properly. New `ServiceOverloadedException` for 429s.

    **Monitoring overhaul:**
    - `CriticalExceptionAlerter` — categorized alerting for auth, blob and infrastructure failures.
    - `LayerProfilingAspect` + `ProfilingContext` — per-layer performance profiling.
    - `ApplicationMetrics` — custom Micrometer gauges/counters for business-level observability.
    - `PollingEndpointSampler` — OTel sampler that suppresses noise from polling endpoints.
    - Health indicators for Blob Storage and Redis. `logback-spring.xml` structured logging.
    - `kql-queries.md` — KQL reference for Application Insights queries.
    - Old `HibernateStatisticsService` removed (superseded by the new profiling stack).

    **Backend hardening:**
    - Auth flow reworked — `AuthService` + `JwtService` tightened (refresh token rotation, controller flow simplified).
    - `GroupServiceImpl` differential refresh optimised, repository queries refined across the board.
    - `V2__add_unique_constraints.sql` Flyway migration + `V1__baseline.sql` schema corrections.
    - Critical exception hierarchy added: `CriticalException` base with auth, blob and infra subtypes.
    - `StringSanitizer` utility, `RequestLoggingFilter` for request tracing, `StartupBlockingFilter` refinements.

    **Frontend decomposition:**
    - Monolithic page components broken down into focused sub-components across task, admin, comments, dashboard, group settings, invitations, settings and topbar/layout domains.
    - Each sub-component got its own CSS module.
    - New shared components: `AppShield`, `DefaultImagePicker`, `MemberDetailPopup`, `NotFound`.
    - New utilities — `formatDate.js`, `userImg.js`, `useDebounce` hook.
    - `GroupProvider` reworked for the blob pivot (encrypted cache + differential sync adjustments).

    **CI/CD restructure:**
    - GitHub Actions bumped from v3 to v4 (checkout, setup-java, upload/download-artifact).
    - Workflows split — `pull_request` triggers CI checks (required status checks on PRs), `push` to main triggers CD deploy.
    - Frontend pipeline got a CDN purge step to match the local bat.
    - Branch protection enabled: `test_backend`, `test_frontend`, `test_maintenance` as required checks.

    **Maintenance:**
    - New `AssetCleanerService` for orphan blob cleanup.
    - `BlobCleanerService` and `MaintenanceRepository` reworked. User cleanup days logic fixed.

---

## Notes
- `v0.1.0` is a lightweight tag (points directly at a commit). Later tags are annotated and have a separate tag object SHA.
- From v0.5.0 onward the scope covers full-stack changes, not just backend.
