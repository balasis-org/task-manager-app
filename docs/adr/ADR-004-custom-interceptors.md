# ADR-004: Custom Interceptors over Spring Security

**Date:** 2025-11-21  
**Status:** Accepted

## Context

The application requires authentication (JWT validation) and authorisation
(system roles + per-group roles) for every API request. Spring Security is the
standard framework for this in Spring Boot applications, providing filter
chains, `SecurityContext`, CSRF protection, and role-based access control.

Three concerns led to evaluating alternatives:

1. **Transparency.** Spring Security's filter chain is powerful but opaque —
   debugging authentication failures requires understanding the ordering and
   interaction of ~15 default filters. The application's auth logic is
   straightforward: extract a JWT from a cookie, validate it, and populate a
   request-scoped `CurrentUser` bean.

2. **Modular architecture alignment.** Spring Security's `SecurityContextHolder`
   uses thread-local state. Allowing this to propagate into the Engine layer
   (domain logic) would create a hidden dependency on the servlet container,
   violating the Hexagonal Architecture principle that domain logic has no
   dependency on delivery mechanisms.

3. **Group-scoped authorisation.** The RBAC model checks per-group roles
   (`GROUP_LEADER` in one group, `MEMBER` in another). Implementing this in
   Spring Security requires custom `PermissionEvaluator` or
   `AuthorizationManager` logic — more framework ceremony than a direct
   `EnumSet` comparison.

## Decision

Implement authentication and authorisation through **custom Spring MVC
`HandlerInterceptor` components**:

- `JwtInterceptor` — extracts the JWT from the cookie, validates the
  HMAC-SHA256 signature, and populates a request-scoped `CurrentUser` bean.
  ~135 lines, single responsibility, no implicit filter chains.
- `AuthorizationService` — checks per-group roles via `EnumSet` comparison
  in O(1). Called explicitly by controllers, not via annotations or AOP.

## Consequences

- **Positive:** The entire auth pipeline is visible in two classes. No implicit
  filter ordering, no thread-local state leaking across layers.
- **Positive:** `CurrentUser` is a standard Spring `@RequestScope` bean,
  injectable in the Context layer without coupling the Engine to servlet APIs.
- **Positive:** Group-scoped RBAC is a direct `EnumSet.contains()` call —
  simpler and faster than Spring Security's `PermissionEvaluator` abstraction.
- **Negative:** Spring Security's built-in CSRF, session fixation, and
  clickjacking protections are not inherited. Mitigated by: stateless JWT
  cookies (CSRF inapplicable for `SameSite=Strict`), `X-Frame-Options: DENY`
  at the CDN edge, and the absence of server-side sessions.
- **Negative:** OAuth 2.0 Resource Server auto-configuration cannot be used.
  The JWT validation logic must be maintained manually.
