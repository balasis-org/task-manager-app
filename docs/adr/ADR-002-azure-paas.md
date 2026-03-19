# ADR-002: Azure PaaS over IaaS and FaaS

**Date:** 2025-10-27  
**Status:** Accepted

## Context

The application requires managed container hosting, a relational database,
blob storage, CDN/WAF edge routing, caching, email delivery, AI content
moderation, and secret management. These must be operated by a single developer
within a ~$108–128/month baseline budget (validated by stress tests; scaling levers documented in the thesis §6.5).

Three cloud service models were considered:

- **IaaS (Azure VMs):** Full control, but requires manual OS patching, load
  balancer configuration, TLS certificate management, and security hardening —
  unsustainable for a solo operator.
- **FaaS (Azure Functions):** Event-driven per-invocation billing, but
  Consumption tier cold starts degrade user experience for an always-on web
  application, and the 5-minute execution timeout is insufficient for
  maintenance workloads.
- **PaaS (Azure App Service):** Managed container hosting with built-in TLS,
  custom domains, health probes, and ACR-based deployment. Predictable monthly
  cost on B1 tier.

Azure was selected over AWS and GCP for three reasons: the Azure for Students
programme ($100 credits, no credit card required), Front Door's integrated
CDN/WAF/routing as a single managed resource, and Managed Identity providing
passwordless authentication to all services (SQL, Blob, Key Vault, Redis).

## Decision

Deploy on **Azure PaaS** using App Service B1 (Linux, 2 instances) as the
primary compute, with Container App Jobs for scheduled maintenance workloads.
All 16 resources are provisioned from a single Bicep template.

Azure-specific SDK usage is isolated to infrastructure adapter modules within
the Engine layer. Domain logic uses framework-standard abstractions (Spring
Boot, JPA, REST) to limit vendor coupling.

## Consequences

- **Positive:** Managed TLS, OS patching, and health-based routing eliminate
  operational overhead. Managed Identity removes all credential storage.
  Predictable B1 pricing (~$27/month for 2 instances) fits the budget.
- **Positive:** Container App Jobs provide near-zero-cost (<$0.002/run)
  scheduled maintenance without consuming App Service resources.
- **Negative:** Vendor coupling to Azure-specific services (Front Door, ACS,
  Content Safety, AI Language). Mitigated by isolating Azure SDK calls to
  dedicated adapter classes behind service interfaces.
- **Negative:** B1 tier has a connection ceiling and limited CPU (1 vCPU,
  1.75 GB RAM per instance), requiring query optimisation and careful
  connection pool sizing within the 20 DTU SQL envelope.
