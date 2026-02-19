# Backend Versioning Log

This file tracks backend milestones using the repository git tags as anchors.
The tags themselves are repo-wide, but the notes below focus on **backend** changes.

## Versioning Scheme
- **0.x.y**: Pre-release / development
    - $x$ = iteration / milestone increment
    - $y$ = patch / small fix increment
- **1.0.0**: first production-ready backend release

---

## Versions

### v0.1.0 — Initial backend baseline
- Date: 2025-10-27
- Tag target (commit): `151288ed375cc4c71f6e2239ddec92ede1b16dac`
- Notes:
    - Establishes the initial backend baseline and project structure.
    - Marks the first version bump to `0.1.0-SNAPSHOT`.

### v0.2.0 — Azure SQL + Key Vault wiring (backend containerization)
- Date: 2025-11-07
- Tag object: `02ac703d0670449748d649d8fa2b40c822196ef5` (annotated)
- Tag target (commit): `33d2c02b1a22c47d72f79e7d4dc1ceff833a881c`
- Backend highlights:
    - Adds Docker setup and compose files aimed at Azure-like deployments.
    - Introduces MSSQL/Azure SQL profiles and configuration (`application-dev-mssql.yml`, `application-prod-azuresql.yml`, etc).
    - Implements Azure Key Vault integration for retrieving Azure SQL credentials.
    - Refactors dev bootstrapping/profile usage and tunes DataLoader defaults/logging.

### v0.3.0 — Azure integrations (Auth/Blob/Email/Observability) + CI
- Date: 2025-11-21
- Tag object: `6c26e835855c6167fab662cb06136858145bb9df` (annotated)
- Tag target (commit): `b77b91f3bd0d723bb16d6aa11686388d8a4ce7f5`
- Backend highlights:
    - Adds Azure AD auth flow (OAuth2) work (dev-tested) and refactors auth wiring.
    - Adds blob storage integration for Azure + local Azurite.
    - Adds email integration for prod (ACS) and dev (SMTP/mailhog).
    - Introduces OpenTelemetry plumbing and API response-time aspect.
    - Adds backend CI basics and a GitHub Action for backend deploys.

### v0.4.0 — Collaboration/workflow APIs + invitation UX primitives
- Date: 2026-02-15
- Tag object: `00a25fac6474de785945e842dbbc6721456f7ced` (annotated)
- Tag target (commit): `d08fd4b6703cfeb3bae48796bf2054677376eae9`
- Backend highlights:
    - Expands group/task collaboration APIs (task preview DTOs, comments CRUD, review flow, to-be-reviewed workflow, events listing).
    - Strengthens invitations and membership management:
        - Invitation role choice + status flow improvements.
        - Pending-only “my invites” semantics + inviter-only cancel endpoint.
        - Seen/unseen primitives via invite timestamps + “mark seen on fetch” behavior.
        - Membership search, removal (including self-remove), and role-change/swap logic.
    - Adds file download APIs for task files + assignee files and refines blob service to support multiple containers.
    - Adds blob garbage-collector groundwork and content-safety hooks for uploads.
    - Adds refresh-token support and JWT/auth hardening.

### v0.5.0 — Full React SPA + admin panel + rate limiting + Flyway baseline
- Date: 2026-02-19
- Tag object: `b044998d8313494b8a4b8494ea5c5d14e1210053` (annotated)
- Tag target (commit): `d28ad184624004f5ec0b6be909295173989aac3b`
- Highlights:

    **Frontend — complete SPA built from scratch (replaces old frontend-demo):**
    - Vite 7.3 + React 19.2 SPA with full page set: Dashboard, Task, Comments, Settings, AdminPanel, Invitations, Login, AuthCallback, AboutUs, TermsOfService, CookiePolicy.
    - `GroupProvider` context with AES-256-GCM encrypted localStorage cache (`cacheCrypto.js`) — group detail encrypted with per-user backend-issued `cacheKey`, held only in memory.
    - `useSmartPoll` hook — tiered backoff polling (30s/60s active, 60s mild idle, stops at 15 min + stale indicator).
    - `apiClient.js` — centralized fetch wrapper with auto-retry on 401, 429 handling, file upload and blob download support.
    - Full component set: `DashboardTopBar`, `FilterPanel`, `TaskTable`, popup suite (NewGroup, GroupSettings, NewTask, InviteToGroup, GroupEvents).
    - Toast notification system (`ToastContext`).
    - Layout with tiered invitation polling (60s/3min/45min based on idle duration).
    - Protected routes, SPA nginx config, Docker + dev Dockerfile.

    **Backend — admin, rate limiting, startup hardening, XSS sanitization:**
    - `AdminController` + `AdminService` (302 lines): full admin CRUD over users, groups, tasks, comments with paginated search. Complete admin DTO layer (7 outbound resource types) and `AdminOutboundMapper`.
    - Redis-backed distributed rate limiting: `RateLimitService` interface, `RedisRateLimitService` (Bucket4j + Lettuce, dual-window: 40 req/min + 420 req/15min), `RateLimitInterceptor` (runs before JWT), dev/prod configs. Fails-open if Redis unavailable.
    - `StartupGate` + `StartupBlockingFilter`: two-gate startup (images ready + data ready), returns HTTP 503 until both gates are open.
    - `InputSanitizer` + `SanitizingRequestBodyAdvice`: automatic XSS sanitization of all `BaseInboundResource` fields (strips HTML, trims invisible Unicode, preserves ZWJ/ZWNJ).
    - `DeletedTask` entity + tombstone repository: soft-delete tracking required for the differential refresh endpoint to report deletions to clients.
    - `GroupRefreshDto` with minimized JSON keys + full differential refresh logic in `GroupServiceImpl` (delta endpoint `GET /groups/{id}/refresh?lastSeen=`).
    - `PlanLimits` utility: enforces max users (100), max groups per user, max tasks per group.
    - `User` model expansion: `cacheKey`, invite code, `isOrg`, allow email notifications.
    - Mini dropdown DTOs: `UserMiniForDropdownResource`, `GroupMiniForDropdownResource`, `TaskPreviewDto` + `TaskPreviewParticipantDto` refactors for payload minimization.
    - `DataLoader` overhaul: richer seed data covering all roles and task states.
    - Repository expansions across `TaskRepository`, `UserRepository`, `GroupInvitationRepository`, `TaskCommentRepository`, `DeletedTaskRepository` — delta queries, admin queries, soft-delete lookups.
    - `DevAuthController` + `AuthService` hardening: refresh token rotation, `@Profile` annotations extended throughout infrastructure beans.

    **Flyway migration baseline:**
    - `V1__baseline.sql` — complete schema with 13 tables, clean constraint names, all indexes and FK constraints.
    - `application-dev-flyway-mssql.yml` profile (`ddl-auto: validate` + Flyway enabled) for testing migrations locally before prod deploy.
    - `backend-flyway-db-compose.yml` — second isolated MSSQL container (port 1434) for Flyway testing.

    **CI/CD and maintenance:**
    - `maintenance-ci-cd.yml` GitHub Action added for the maintenance module.
    - `frontend-ci-cd.yml` fully reworked for the new Vite SPA structure.
    - Maintenance module refactored into a proper Spring Boot app (`MaintenanceCore`, `MaintenanceRunner`), new Dockerfile.
    - Old `task-manager-frontend-demo` (vanilla HTML/JS) removed; replaced entirely by the new React SPA.

---

## Notes
- This document is backend-focused; tags include frontend/infra work as well.
- `v0.1.0` is a lightweight tag (points directly to a commit). Later tags are annotated and therefore have a separate tag object SHA.