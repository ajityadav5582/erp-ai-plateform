# Security Review — Authentication & Authorization

**Scope:** `platform/identity`, `libraries/common-security`, business services (`inventory`, `pos`, `sales`, `procurement`), API Gateway, frontend auth handling.

**Reviewer:** Senior Java Full-Stack review (automated + manual).

---

## 1. Executive summary

The platform has a reasonably well-structured, stateless JWT-based auth model:
- Identity Service issues short-lived access tokens (15 min) + rotating opaque refresh tokens.
- Downstream services validate the shared HMAC JWT via `PlatformJwtAuthenticationFilter` and enforce permissions through a `PermissionAspect`.
- Tenant isolation is carried in the token; company/fiscal-year are request-scoped headers.

However, several **high/medium** issues were identified. A subset of the low-risk ones has been fixed in code (see §6); the rest are recommendations.

---

## 2. Findings

### CRITICAL

| # | Area | Issue | Impact |
|---|------|-------|--------|
| C1 | Secrets | Default JWT secret `jwt-secret-key-change-me-in-production-32b` is committed as a fallback in every `application.yml` and in `PlatformJwtProperties`. Anyone can forge tokens if env is unset. | Token forgery / full account takeover |
| C2 | Gateway | Gateway performs **no authentication** and forwards `X-Company-Id` / `X-Fiscal-Year-Id` headers verbatim. Services trust these headers. If a service is reachable directly (or headers are spoofed), a user can read/write another company's data (IDOR). | Cross-tenant / cross-company data breach |
| C3 | Frontend | Access & refresh tokens are stored in **JS-readable cookies** (`document.cookie`), not `httpOnly`. Any XSS steals both tokens. | Session hijacking |
| C4 | Identity | `/api/v1/identity/auth/register` and `/onboarding` are `permitAll`. Registration accepts an arbitrary `tenantId`, allowing user creation in any tenant and tenant spam. | Unauthorized account creation / tenant abuse |

### HIGH

| # | Area | Issue | Impact |
|---|------|-------|--------|
| H1 | Crypto | HS256 symmetric secret is shared by every service. Compromise of any service leaks the signing key → token forgery for all. | Lateral movement |
| H2 | Authorization | `TokenAuthenticatedUser.hasPermission()` (and `hasAllPermissions`) hard-code a bypass for roles `SUPER_ADMIN` / `TENANT_ADMIN` / `OWNER`. Fine-grained permission changes cannot revoke these. | Over-privileged roles |
| H3 | Identity | `JwtAuthenticationFilter` previously set Spring Security authorities to `List.of()`, so `@PreAuthorize`/`hasRole` checks could never pass; only the custom aspect worked. (Fixed.) | Inconsistent enforcement |
| H4 | Identity | `SecurityConfig` enabled `httpBasic`, allowing an alternate (weaker) auth path. (Fixed.) | Auth bypass surface |
| H5 | Password reset | `forgotPassword` returns the reset token in the HTTP response (documented as "should be emailed"). Token leakage via logs/proxies. | Account takeover |
| H6 | Refresh tokens | Need to verify reuse-detection / family revocation on refresh-token reuse. Review `AuthServiceImpl.refresh`. | Replay attacks |
| H7 | Company scoping | `RequestContext.setCompanyId(header)` is never validated against the user's accessible companies. | IDOR |

### MEDIUM

| # | Area | Issue |
|---|------|-------|
| M1 | Rate limiting | No rate limiting on `/login`, `/refresh`, `/forgot-password`. Account lockout exists but is per-account only. |
| M2 | Token lifetime | Refresh token 7 days with no absolute session cap / idle timeout. |
| M3 | CORS | Services disable CORS (gateway is authority) — verify gateway CORS allow-list is strict (no `*` with credentials). |
| M4 | Logging | `log.debug("Rejected access token: {}", e.getMessage())` may log token parse details; ensure no full token is logged anywhere. |
| M5 | Input validation | `@Valid` on controllers present; verify all DTOs have constraints. |
| M6 | SQL/JPQL | Native queries in repositories — verify parameterized (no string concat). |
| M7 | Multi-tenancy | `TenantContext`/`RequestContext` are ThreadLocals cleared in `finally` — good. But async/Kafka consumers must propagate context manually. |
| M8 | Frontend | Tokens in cookies without `httpOnly`; `Secure` only in production. |

### LOW

| # | Area | Issue |
|---|------|-------|
| L1 | `PermissionAspect` (common-security) and identity's own `PermissionAspect`/`RequirePermission` are two parallel systems — consolidate. |
| L2 | `HasPermission`/`HasRole` annotations in common-security are unused (aspect keys off `RequiresPermission`/`RequiresRole`). Dead code. |
| L3 | `PlatformJwtProperties` lacked validation (fixed). |
| L4 | Identity filter did not clear `SecurityContextHolder` in `finally` (fixed). |

---

## 3. Recommendations (priority order)

1. **Remove insecure JWT secret defaults** from all `application.yml` and require `JWT_SECRET` (fail fast). Use a secrets manager in prod.
2. **Migrate JWT signing to RS256** (asymmetric) so downstream services only need the public key.
3. **Move tokens to `httpOnly`, `Secure`, `SameSite=Strict` cookies** set by the backend; frontend reads only via API. Or keep access token in memory only.
4. **Validate company/fiscal-year selection server-side**: store the user's accessible company IDs and reject `X-Company-Id` not in that set.
5. **Restrict registration/onboarding**: require admin auth or a signed invitation; rate-limit.
6. **Do not return reset tokens in responses**; deliver via email/SMS only.
7. **Add rate limiting** (Bucket4j / gateway) on auth endpoints.
8. **Refresh-token reuse detection**: on reuse of a revoked refresh token, revoke the whole family.
9. **Remove HTTP Basic** and any alternate auth paths (done).
10. **Consolidate authorization**: single aspect + annotations; remove dead `HasPermission`/`HasRole` or wire them.
11. **Network isolation**: services must not be directly reachable; only gateway exposed.
12. **Audit logging**: log auth events (login success/fail, refresh, logout, permission denial) with correlation id.

---

## 4. What was fixed in code

- `libraries/common-security/.../PlatformJwtProperties.java`: removed insecure default secret; added `@Validated` + `@NotBlank`/`@Size(min=32)`.
- `platform/identity/.../SecurityConfig.java`: removed `httpBasic`.
- `platform/identity/.../JwtAuthenticationFilter.java`: populate Spring Security authorities from token claims; clear `SecurityContextHolder` in `finally`.
- Chunk 2 — Secrets hardening: removed `${JWT_SECRET:...default}` fallback from all service `application.yml`; added `JWT_SECRET`/`JWT_ISSUER` to `.env.example`; added `JWT_SECRET` to each service `bootRun` env.
- Chunk 3 — Refresh-token reuse detection: on presentation of a revoked refresh token, revoke all active tokens for the user/tenant and throw.
- Chunk 4 (partial) — Header hardening: `PlatformJwtAuthenticationFilter` now sanitizes `X-Company-Id` / `X-Fiscal-Year-Id` to positive numeric values only.
- Chunk 5 — Rate limiting: added `RateLimitFilter` to identity service (10 req/min/IP on login/refresh/forgot-password).

---

## 5. Residual risk

Items C2, C3, C4, H1, H2, H5, H6, H7, M1 remain open and require design-level changes beyond a single PR. Track as separate tickets.
