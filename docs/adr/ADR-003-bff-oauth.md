# ADR-003: BFF OAuth 2.0 over Client-Side Tokens

**Date:** 2025-11-21  
**Status:** Accepted

## Context

The application authenticates users via Azure AD (Entra ID) using the OAuth 2.0
Authorization Code Grant. Two architectural patterns were considered for token
management:

- **Client-side token handling:** The SPA receives and stores Azure AD tokens
  directly (via MSAL.js), attaching them to API requests as Bearer tokens.
  This exposes tokens to XSS attacks in browser storage and requires the SPA
  to manage token refresh, expiry, and revocation.
- **Backend-for-Frontend (BFF) pattern:** The backend acts as a confidential
  client — the client secret never leaves Key Vault, and the frontend never
  holds any Azure AD token. After login, the backend issues its own
  application-level JWT and refresh token.

The BFF pattern aligns with current OAuth 2.0 security best practices for
browser-based applications, which recommend against storing access tokens in
the browser.

## Decision

Adopt the **BFF pattern**: the backend is the OAuth 2.0 confidential client.
After successful authentication with Azure AD, the backend issues:

- A **15-minute HMAC-SHA256 JWT** for API authentication
- A **24-hour refresh token** for session continuity

Both are stored in `httpOnly; Secure; SameSite=Strict` cookies — inaccessible
to JavaScript. Refresh token rotation invalidates the previous token on each
use, limiting the window for stolen token replay. On logout, tokens are deleted
server-side.

The frontend has zero knowledge of Azure AD tokens, client secrets, or
authentication internals.

## Consequences

- **Positive:** Azure AD client secret never leaves Key Vault; no tokens in
  browser-accessible storage; `httpOnly` cookies eliminate XSS token theft.
- **Positive:** Refresh token rotation provides automatic stolen-token
  detection — a legitimate refresh invalidates any replayed token.
- **Positive:** All authentication complexity is centralised in the backend,
  simplifying the frontend to a redirect-based flow.
- **Negative:** Every API request carries cookies, adding ~200 bytes per
  request. Negligible for this application's request volume.
- **Negative:** The backend must handle token refresh logic and rotation state,
  adding complexity to `AuthService` and `JwtService`.
