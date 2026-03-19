# ADR-008: Blob Storage with Front Door Origin Auth over SAS Tokens

**Date:** 2026-02-28  
**Status:** Accepted (supersedes initial SAS token approach)

## Context

User-uploaded files (profile images, task attachments) are stored in Azure
Blob Storage. The frontend needs to display images and allow file downloads.
Two access patterns were evaluated:

- **SAS tokens:** The backend generates short-lived Shared Access Signature
  URLs for each blob. The frontend fetches blobs directly from the Storage
  Account's public endpoint using these URLs. This requires the backend to
  generate tokens per file, manage token expiry, and exposes Storage Account
  URLs in the browser network tab.
- **Front Door Origin Auth:** Front Door is configured with a second origin
  group pointing to the Blob Storage account. Front Door authenticates to
  Blob Storage using its User-Assigned Managed Identity (RBAC role:
  `Storage Blob Data Reader`). The frontend accesses blobs through the same
  Front Door domain — requests to `/blob/*` are proxied to Storage with MI
  authentication.

The initial implementation used SAS tokens. During production hardening, it
was migrated to Origin Auth for security and architectural consistency.

## Decision

Use **Front Door Origin Auth with Managed Identity** for all blob access.

The frontend requests blobs through the Front Door CDN endpoint. Front Door
authenticates to Blob Storage using its MI token, validated via RBAC. No SAS
tokens are generated, no Storage Account URLs are exposed to clients, and blob
access benefits from the same CDN caching and WAF protection as API requests.

Downloads go through the backend API for budget enforcement — the backend
streams the blob with rate limiting, concurrency gating, ETag/304 fast-paths,
and download budget checks before any bytes leave the App Service.

Image display (thumbnails, profile pictures) goes through the CDN path for
performance — these are read-only, public within the application context, and
benefit from edge caching.

## Consequences

- **Positive:** No SAS token generation overhead. No token URLs in the
  browser. Single domain for all traffic (no CORS, no mixed-content).
- **Positive:** Blob access inherits CDN caching, WAF protection, and the
  same security headers as API traffic.
- **Positive:** Managed Identity is the sole authentication mechanism across
  all Azure services — consistent zero-credential model.
- **Negative:** Front Door CDN caching means image updates have a propagation
  delay until the cache is purged or expires. Mitigated by unique blob names —
  each upload generates a UUID-prefixed filename, so the CDN URL changes on
  every upload and caches are never stale.
- **Negative:** The Front Door origin auth feature is in preview. Accepted
  because the fallback (SAS tokens) is a known-good path if needed.
