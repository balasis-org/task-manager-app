# ADR-006: Bicep over Terraform

**Date:** 2026-03-11  
**Status:** Accepted

## Context

The application's 24 Azure PaaS resources must be provisioned reproducibly
across three environments (production, arena-security, arena-stress) from a
single template. Two Infrastructure-as-Code tools were evaluated:

- **Terraform:** Multi-cloud orchestration, mature ecosystem, HCL syntax.
  Requires a state backend (Azure Storage Account or Terraform Cloud) for
  tracking resource state. Provider versioning adds another dependency.
- **Bicep:** Azure-native IaC compiled to ARM templates. Stateless — Azure
  Resource Manager is inherently idempotent, so no external state management
  is required. Ships with every Azure CLI installation.

## Decision

Use **Bicep** for all infrastructure provisioning.

A single `main.bicep` file (~1,350 lines) provisions all 24 resources.
Environment-specific configuration is driven by `.bicepparam` files
(`main.bicepparam`, `main-arena-security.bicepparam`,
`main-arena-stress.bicepparam`). The `deploy-infra` GitHub Actions workflow
invokes `az deployment` and writes generated resource names back to GitHub
Environment variables via the GH CLI.

Three categories of manual setup fall outside Bicep's scope — Entra ID App
Registrations, Key Vault secrets derived from those registrations, and default
Blob Storage images — because they use the Microsoft Graph API, not the ARM
API that Bicep targets. Automating them was rejected because it would require
elevated Graph permissions (`Application.ReadWrite.All`) and `az ad app create`
is not idempotent.

## Consequences

- **Positive:** No state backend to manage, no state lock contention, no state
  file drift. ARM handles idempotency natively.
- **Positive:** `bicepparam` files provide clean per-environment overrides.
  Deploying a new arena environment is a single workflow dispatch with a
  different parameter file.
- **Positive:** Zero additional tooling — Bicep ships with the Azure CLI
  already required for deployment.
- **Negative:** Azure-only. If multi-cloud deployment were required, Terraform
  would be necessary. This does not apply to a single-provider project.
- **Negative:** Some resources (Entra ID App Registrations, Graph API
  operations) are outside Bicep's scope, requiring manual setup guides.
