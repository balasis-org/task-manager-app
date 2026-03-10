# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.x.x (current) | Yes |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly.

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, email the maintainer directly at **giovani1994a@gmail.com** with:

1. A description of the vulnerability
2. Steps to reproduce the issue
3. Potential impact assessment
4. Any suggested fixes (optional)

You should receive an acknowledgement within 48 hours. Fixes for confirmed vulnerabilities will be prioritised and released as patch versions.

## Security Architecture

This project implements a seven-layer defence-in-depth security architecture:

1. **Edge** — Azure Front Door WAF with custom rules (IP rate limiting, internal endpoint blocking)
2. **Transport** — HTTPS/HSTS enforced, six security headers at CDN edge
3. **Origin authentication** — Front Door Managed Identity tokens validated by Easy Auth + X-Azure-FDID + IP restriction
4. **Application rate limiting** — Redis Bucket4j (per-user, fail-closed) at 40 req/min + 420 req/15 min sliding windows
5. **Input sanitisation** — five-layer pipeline: `SanitizingRequestBodyAdvice`, `InputSanitizer`, `StringSanitizer`, `@ValidEnum`, parameterised JPQL
6. **Authorisation** — two-level RBAC (system roles + per-group roles) via `EnumSet` comparison
7. **Data protection** — Managed Identity for all Azure services (zero stored credentials), HMAC-SHA256 JWT in httpOnly/Secure/SameSite=Strict cookies

## Automated Security Testing

Nine k6 attack simulations validate eight OWASP Top 10 (2021) categories with 41 individual assertions at 100% pass rate:

- Authentication bypass (A07)
- Rate limit evasion (A04)
- Input abuse and injection (A03)
- Unauthorised resource access (A01)
- File upload manipulation (A03/A04)
- IDOR resource tampering (A01)
- XSS / HTML injection (A03)
- Session manipulation (A07)
- Privilege escalation (A01)

## Dependency Management

Dependencies are monitored via [Dependabot](.github/dependabot.yml) for:
- Maven (backend + maintenance)
- npm (frontend)
- GitHub Actions (workflows)
