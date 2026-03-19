# ADR-001: Modular Monolith over Microservices

**Date:** 2025-10-27  
**Status:** Accepted

## Context

The application manages collaborative task workflows with 18 JPA entities,
six-level join chains for authorisation checks, and atomic budget enforcement
queries (`UPDATE ... WHERE budget >= cost`). These characteristics demand
transactional consistency across entity boundaries.

The system is built and operated by a single developer on a ~$108–128/month
Azure baseline budget (stress-tested; scaling levers documented in thesis §6.5). Each additional independently deployed service would require its
own CI/CD pipeline, monitoring configuration, health checks, and
inter-service communication, increasing both operational cost and cognitive
overhead.

Microservices solve large-organisation problems — independent team deployability,
polyglot persistence, per-service scaling — none of which apply here. Fowler's
"MonolithFirst" guidance recommends starting with a monolith and decomposing
only when the domain boundaries are well understood and the team structure
demands it.

## Decision

Adopt a **modular monolith** structured as a 10-module Maven reactor with
compile-time dependency enforcement:

- **Engine modules** (domain logic) — entities, repositories, services, and
  Azure infrastructure adapters. No dependency on Spring MVC.
- **Context modules** (HTTP delivery) — controllers, interceptors, DTOs,
  exception handlers. Cannot access infrastructure adapters directly.
- **Shared module** — enums consumed by both the backend and the maintenance
  deployable.

Module boundaries are enforced by Maven's dependency graph: a controller
importing a Redis adapter class causes a build failure. This is stricter than
ArchUnit-based runtime tests (the reason Spring Modulith was not adopted).

The architecture follows Cockburn's Hexagonal Architecture principle — domain
logic has no dependency on delivery mechanisms — while preserving a clear
microservices decomposition path along existing module boundaries.

## Consequences

- **Positive:** Single deployable JAR simplifies CI/CD, monitoring, and local
  development. Transactional consistency is maintained without distributed
  coordination. Compile-time enforcement prevents architectural erosion.
- **Positive:** The module boundaries (Engine/Context split) map directly to
  a future microservices decomposition if needed.
- **Negative:** All modules scale together — a CPU-intensive operation in one
  module affects the entire application. Mitigated by offloading heavy
  maintenance work to a separate Container App Job.
- **Negative:** A single deployable means a change in any module triggers a
  full redeployment. Acceptable for the current team size and deployment
  frequency.
