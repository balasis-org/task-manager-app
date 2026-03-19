# ADR-010: k6 over JMeter and Gatling

**Date:** 2026-03-11  
**Status:** Accepted

## Context

The application requires two categories of automated testing against the live
Azure deployment:

1. **Security attack simulations** — scripted scenarios that attempt
   authentication bypass, injection, privilege escalation, IDOR, and session
   manipulation against the running system, mapped to the OWASP Top 10.
2. **Stress tests** — concurrent load scenarios (polling, presence tracking,
   file downloads) to validate the B1 tier's performance under pressure.

Three tools were evaluated:

- **JMeter:** Mature, GUI-based, widely used. XML test plans are verbose and
  difficult to version control. Scripting complex multi-step scenarios (create
  user → create group → upload file → attempt IDOR) requires Groovy/BeanShell
  embedded in XML — poor developer experience.
- **Gatling:** Scala-based DSL, good for load testing. Less suited to the
  assertion-heavy security testing pattern where each request needs multiple
  `check()` assertions against status codes, headers, and response bodies.
- **k6:** JavaScript-based, scriptable, CLI-first, purpose-built for both
  load testing and scripted HTTP scenarios. Test scripts are plain `.js`
  files — diffable, reviewable, version-controlled like application code.

## Decision

Use **k6** for all automated testing against live deployments.

Nine security attack scripts test 41 individual assertions across OWASP Top 10
categories. Three stress test scripts validate performance under concurrent
load. All scripts share a common configuration module (`config.js`) and
HTTP helper library (`http-helpers.js`) for authentication and request
construction.

Tests execute against dedicated **arena environments** deployed from the same
Bicep template as production:
- `arena-security` — mirrors production defences exactly
- `arena-stress` — disables WAF rate limits, overrides plan budgets for
  throughput testing

Both arenas include a `DevAuthController` for JWT issuance without Azure AD
and a `DataLoader` for test user provisioning, enabling k6 scripts to operate
without Azure AD credentials or source code access.

## Consequences

- **Positive:** Test scripts are plain JavaScript — readable, diffable, and
  reviewable in pull requests like any other code.
- **Positive:** k6's `check()` API maps naturally to security assertions
  ("status is 401", "body contains 'Forbidden'", "Retry-After header present").
- **Positive:** Single tool for both security and performance testing reduces
  the learning curve and infrastructure requirements.
- **Negative:** k6 does not support browser-based testing (Selenium/Playwright
  scenarios). Not needed — all tests target the REST API directly, which is
  the security boundary.
- **Negative:** k6 OSS lacks a built-in dashboard. Console output and
  structured JSON logs are sufficient for evaluation evidence.
