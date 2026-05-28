## 1. Test Infrastructure Setup
- [ ] 1.1 Create shared test fixture helper for OAuth/OIDC tests — method to create an authorization server, client (with redirect URIs), user (with password), application + secret, and default scopes in `@BeforeEach`
- [ ] 1.2 Create helper method to execute a full authorization code flow (login → code → token exchange) for use in downstream tests
- [ ] 1.3 Verify RestAssured redirect-following is disabled in security tests (`redirects().follow(false).and().redirects().max(0)`)

## 2. PKCE Enforcement Tests
- [ ] 2.1 Create `PkceSecurityTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/authorization/`
- [ ] 2.2 Implement tests: authorization without `code_challenge`, `plain` method rejected, token exchange without `code_verifier`, wrong `code_verifier`, correct `code_verifier` succeeds (ASVS 10.4.6, RFC 9700 §4.8)
- [ ] 2.3 Run tests and document any failures as expected security gaps

## 3. Redirect URI Validation Tests
- [ ] 3.1 Create `RedirectUriSecurityTest.kt`
- [ ] 3.2 Implement tests: exact match succeeds, unregistered URI rejected (no redirect), path traversal rejected, added path suffix rejected, different port rejected, missing URI rejected (ASVS 10.4.1, RFC 9700 §4.1)
- [ ] 3.3 Run tests and document any failures

## 4. Authorization Code Lifecycle Tests
- [ ] 4.1 Create `AuthorizationCodeFlowSecurityTest.kt`
- [ ] 4.2 Implement tests: single-use enforcement, reuse triggers revocation, client binding, redirect URI binding, expired code rejected (ASVS 10.4.2, 10.4.3, RFC 9700 §4.5)
- [ ] 4.3 Implement state parameter tests: state returned in success redirect, state preserved on error redirect (RFC 9700 §4.7)
- [ ] 4.4 Run tests and document any failures

## 5. Grant Type and Token Endpoint Tests
- [ ] 5.1 Create `TokenEndpointSecurityTest.kt`
- [ ] 5.2 Implement grant type tests: authorization_code succeeds, client_credentials succeeds, refresh_token succeeds, `password` (ROPC) rejected, missing grant_type rejected (ASVS 10.4.4)
- [ ] 5.3 Implement client authentication tests: invalid secret fails, missing client_id fails, cross-server credentials fail (ASVS 10.4.10)
- [ ] 5.4 Implement input validation tests: malformed grant_type, missing required parameters, non-UUID server ID, non-existent server ID
- [ ] 5.5 Run tests and document any failures

## 6. Refresh Token Lifecycle Tests
- [ ] 6.1 Create `RefreshTokenSecurityTest.kt`
- [ ] 6.2 Implement tests: rotation issues new token, old token revoked, reuse revokes family, scope escalation rejected, client binding (ASVS 10.4.5, 10.4.8, 10.4.9, RFC 9700 §4.14)
- [ ] 6.3 Run tests and document any failures

## 7. JWT and JWKS Validation Tests
- [ ] 7.1 Create `JwtSecurityTest.kt`
- [ ] 7.2 Implement JWT claim tests: required claims present (`iss`, `aud`, `sub`, `exp`, `scope`), issuer matches server, audience set, `alg` is RS256, `kid` matches JWKS (ASVS 10.3.1, 10.3.2, 10.3.3)
- [ ] 7.3 Implement JWKS tests: endpoint returns valid key set, keys match token `kid`, cross-tenant key isolation
- [ ] 7.4 Run tests and document any failures

## 8. Scope Enforcement Tests
- [ ] 8.1 Create `ScopeSecurityTest.kt`
- [ ] 8.2 Implement tests: invalid scope triggers error redirect, client_credentials scope limited to application assignment, cross-tenant scope rejected (ASVS 10.4.11, RFC 9700 §2.3)
- [ ] 8.3 Run tests and document any failures

## 9. Multi-Tenant Isolation Tests
- [ ] 9.1 Create `MultiTenantSecurityTest.kt`
- [ ] 9.2 Implement tests: cross-server code exchange rejected, cross-server client rejected, JWKS returns only tenant keys, cross-server token rejected at userinfo
- [ ] 9.3 Run tests and document any failures

## 10. Well-Known and Discovery Tests
- [ ] 10.1 Create `WellKnownEndpointSecurityTest.kt`
- [ ] 10.2 Implement tests: valid metadata returned, response_types_supported accuracy (flag `token`), code_challenge_methods_supported includes S256
- [ ] 10.3 Run tests and document any failures

## 11. Security Headers Tests
- [ ] 11.1 Create `SecurityHeadersTest.kt`
- [ ] 11.2 Implement tests: clickjacking headers on authorization endpoint, `Cache-Control: no-store` on token responses, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` on redirects (RFC 9700 §4.2, §4.16)
- [ ] 11.3 Run tests and document any failures

## 12. Redirect Security Tests
- [ ] 12.1 Create `RedirectSecurityTest.kt`
- [ ] 12.2 Implement tests: redirect uses 302 (not 307), code in query not fragment, no tokens in redirect URL (RFC 9700 §4.12)
- [ ] 12.3 Run tests and document any failures

## 13. Error Handling and Information Leakage Tests
- [ ] 13.1 Create `OAuthErrorHandlingTest.kt`
- [ ] 13.2 Implement tests: token endpoint error format (RFC 6749 §5.2), login failure no user enumeration, invalid client no existence leakage, no stack traces in errors
- [ ] 13.3 Run tests and document any failures

## 14. CORS Security Tests
- [ ] 14.1 Create `CorsSecurityTest.kt`
- [ ] 14.2 Implement tests: preflight request handling, cross-origin credential policy
- [ ] 14.3 Run tests and document any failures

## 15. UserInfo Endpoint Security Tests
- [ ] 15.1 Create `UserInfoSecurityTest.kt`
- [ ] 15.2 Implement tests: missing Authorization rejected, malformed token rejected, cross-server token rejected, valid token returns profile
- [ ] 15.3 Run tests and document any failures

## 16. Final Validation
- [ ] 16.1 Run full test suite: `./gradlew test`
- [ ] 16.2 Categorize test results: passing (security control verified), failing (security gap), erroring (test infrastructure issue)
- [ ] 16.3 Document summary of security coverage and gaps found

## Dependencies

- Task 1 (infrastructure) blocks tasks 2-15
- Tasks 2-15 can be parallelized across agents
- This proposal does not depend on `add-security-audit` but findings from both should be cross-referenced
- The `add-resource-integration-tests` proposal provides complementary CRUD tests but is not a prerequisite
