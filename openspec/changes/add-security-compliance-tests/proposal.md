# Change: Add Security Compliance Test Suite

## Why

The project has no automated tests exercising the OAuth 2.1 / OIDC security properties of the authorization server endpoints. The existing integration tests cover CRUD operations on management resources (SCIM), but none test the authorization flow, token endpoint, redirect URI validation, PKCE enforcement, or other security-critical behaviors. Without these tests, regressions in security controls go undetected.

This test suite complements the `add-security-audit` findings-only proposal by providing ongoing automated verification of OWASP and OAuth/OIDC compliance properties.

## What Changes

Add integration tests using the existing `@QuarkusTest` + RestAssured infrastructure, organized into two capabilities:

- **OAuth 2.1 / OIDC Security Tests**: Tests exercising the authorization endpoint (`GET/POST /{id}/authorization/`), token endpoint (`POST /{id}/token/`), JWKS endpoint, well-known endpoint, and userinfo endpoint for protocol compliance per RFC 9700, OWASP ASVS v5.0 V10, and OWASP WSTG-ATHZ-05.

- **OWASP Web Security Tests**: Tests verifying HTTP security headers, error handling (no info leakage), CORS behavior, and response properties across all endpoints.

No changes to application code are required — this is a test-only proposal. However, test failures will surface issues that should be fixed (either in this proposal or via the `add-security-audit` fix phase).

## Impact

- Affected specs: none (new capabilities)
- Affected code: `web/src/test/` only — new test classes
- Related changes: `add-security-audit` (findings will inform which tests are highest priority), `add-resource-integration-tests` (shared test infrastructure)
- Key endpoints under test:
  - `GET /{authorizationServerId}/authorization/` — authorization initiation
  - `POST /{authorizationServerId}/authorization/` — credential submission and code generation
  - `POST /{authorizationServerId}/token/` — token exchange (all grant types)
  - `GET /{authorizationServerId}/jwks/` — key set
  - `GET /{authorizationServerId}/.well-known/openid-configuration/` — discovery
  - `GET /{authorizationServerId}/userinfo/` — user info with bearer token
