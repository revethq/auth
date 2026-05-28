## Context

Revet Auth has an existing integration test pattern using `@QuarkusTest`, RestAssured, and DevServices PostgreSQL. The SCIM tests demonstrate the established pattern: inject services for fixture setup in `@BeforeEach`, use RestAssured for HTTP assertions, and use WireMock for external service simulation. This test suite follows the same patterns for the OAuth/OIDC endpoints.

## Goals / Non-Goals

**Goals:**
- Automated regression tests for OAuth 2.1 / OIDC security properties
- Tests that exercise actual HTTP endpoints (not mocked service calls)
- Coverage of OWASP ASVS v5.0 V10 authorization server requirements (L1/L2)
- Coverage of RFC 9700 attack mitigations that can be tested via HTTP
- Coverage of OWASP WSTG-ATHZ-05 authorization server test cases
- Tests that can run in CI via `./gradlew test`

**Non-Goals:**
- Penetration testing or fuzzing (out of scope for unit/integration tests)
- Tests requiring a running browser (no Selenium/Playwright)
- Performance or load testing
- Tests for features not yet implemented (e.g., DPoP, mTLS)
- Fixing issues found by tests — that's for the audit fix phase

## Decisions

### Decision: RestAssured integration tests in web module
Tests live in `web/src/test/kotlin/com/revethq/auth/web/authorization/` alongside the existing SCIM tests. They use `@QuarkusTest` with DevServices for a real database.

**Alternatives considered:**
- Service-layer unit tests — rejected because they don't exercise HTTP behavior (headers, status codes, redirects, content types)
- External test harness (e.g., Python/requests) — rejected because it adds a separate test runtime and can't use DevServices

### Decision: Test fixture setup via injected services
Tests create authorization servers, clients, users, applications, and scopes programmatically via injected service classes in `@BeforeEach`, following the established SCIM test pattern.

### Decision: Follow-redirect disabled for redirect tests
RestAssured's default follow-redirect behavior must be disabled (`redirects().follow(false)`) for authorization endpoint tests so we can assert on the redirect response itself (status code, Location header, absence of tokens in URL).

### Decision: Two test capabilities (OAuth/OIDC + OWASP)
Separating OAuth protocol tests from generic OWASP web tests keeps each test class focused and allows parallel agent work.

## Test Infrastructure

```
web/src/test/kotlin/com/revethq/auth/web/authorization/
├── AuthorizationEndpointSecurityTest.kt    # GET /authorization/ validation
├── AuthorizationCodeFlowSecurityTest.kt    # POST /authorization/ + code exchange
├── TokenEndpointSecurityTest.kt            # POST /token/ all grant types
├── PkceSecurityTest.kt                     # PKCE enforcement and downgrade
├── RedirectUriSecurityTest.kt              # Redirect URI validation
├── RefreshTokenSecurityTest.kt             # Refresh token lifecycle
├── JwksEndpointSecurityTest.kt             # JWKS response validation
├── WellKnownEndpointSecurityTest.kt        # Discovery metadata validation
├── UserInfoEndpointSecurityTest.kt         # Bearer token validation
├── SecurityHeadersTest.kt                  # HTTP security headers across endpoints
└── OAuthErrorHandlingTest.kt               # Error response compliance
```

### Test Fixture Dependencies

```
AuthorizationServer (root — created in @BeforeEach)
├── Client (with registered redirect URIs and scopes)
├── User (with password hash for login)
├── Application + ApplicationSecret (for client_credentials)
└── Scopes (openid, email, profile, offline_access)
```

## Risks / Trade-offs

- **Tests may fail immediately** — Some tests are designed to verify security properties that may not be implemented yet (e.g., PKCE required, authorization code single-use). These failures are the point — they document what needs fixing.
- **Test coupling to redirect behavior** — Authorization flow tests depend on RestAssured not following redirects, which requires explicit configuration.
- **Login screen rendering** — The POST /authorization/ endpoint returns an HTML login screen on failure, which is harder to assert on than JSON. Tests should check status codes and Location headers rather than HTML content.

## Open Questions

- Should tests that are expected to fail (documenting missing security controls) be marked with `@Disabled("Pending fix")` or left as failing tests?
