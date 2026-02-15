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

---

## Notes
- This document is backend-focused; tags include frontend/infra work as well.
- `v0.1.0` is a lightweight tag (points directly to a commit). Later tags are annotated and therefore have a separate tag object SHA.