# OAuth 2.1 / OIDC Protocol Compliance - Security Audit Findings

**Audit Date**: 2026-05-21
**Auditor**: Claude Opus 4.6 (Automated Security Audit)
**Scope**: OAuth 2.1 / OIDC protocol compliance of the Revet Auth authorization server

## Summary

| Severity      | Count |
|---------------|-------|
| Critical      | 3     |
| High          | 5     |
| Medium        | 6     |
| Low           | 4     |
| Informational | 4     |
| **Total**     | **22**|

---

## Critical Findings (3)

### Finding: PKCE-001 PKCE Not Enforced on Authorization Requests
- **Severity**: Critical
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:265-358` (`initiateAuthorization`) and lines 80-184 (`createAuthorizationCode`)
- **Description**: The authorization endpoint accepts requests without `code_challenge` or `code_challenge_method`. The `initiateAuthorization` method validates `clientId`, `redirectUri`, and `scope`, but never checks for `code_challenge`. The `createAuthorizationCode` method stores whatever `codeChallenge` value is provided (including null) without requiring it. OAuth 2.1 (RFC 9700 Section 4.8) mandates PKCE for all authorization code grants.
- **Risk**: Without mandatory PKCE, the authorization code grant is vulnerable to authorization code interception attacks. An attacker who intercepts the code can exchange it for tokens without needing the code verifier.
- **Recommendation**: Add validation in `initiateAuthorization` to reject requests without `code_challenge`. Return `invalid_request` error. Validate `code_challenge_method` is exactly `S256`.
- **References**: RFC 9700 Section 4.8, ASVS 10.4.6, RFC 7636

### Finding: PKCE-002 Code Verifier Not Validated During Token Exchange
- **Severity**: Critical
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:441-469` (authorization code branch in `postToken`)
- **Description**: The token endpoint's authorization code exchange retrieves the `ClientCode` but never validates `code_verifier` against the stored `code_challenge`. The `accessTokenRequest.codeVerifier` parameter (defined in `AccessTokenRequest.kt:49`) is completely ignored. Only `redirectUri` matching is performed (line 445).
- **Risk**: PKCE is completely ineffective. An attacker who obtains an authorization code can exchange it without the code verifier.
- **Recommendation**: Retrieve stored `code_challenge` and `code_challenge_method` from `ClientCode`. Require `code_verifier`, compute `BASE64URL(SHA256(code_verifier))`, compare to stored challenge. Reject on mismatch.
- **References**: RFC 9700 Section 4.8, RFC 7636 Section 4.6, ASVS 10.4.6

### Finding: CODE-001 Authorization Code Not Invalidated After Use
- **Severity**: Critical
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:442`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:147-152`, `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/ClientCodeRepository.kt:30-32`
- **Description**: When an authorization code is exchanged for tokens (line 442), the code is retrieved but never deleted or marked as used. The `ClientCodeRepository` only has `findByCode` with no delete or invalidation method. The `ClientCode` entity has no `isUsed` field. The same code can be reused indefinitely.
- **Risk**: Authorization code replay attacks. An attacker who obtains a code can exchange it repeatedly.
- **Recommendation**: Delete or invalidate the code immediately after successful exchange. Add `deleteByCode` to `ClientCodeRepository`. Implement reuse detection to revoke all tokens from a replayed code.
- **References**: RFC 9700 Section 4.5, ASVS 10.4.2

---

## High Findings (5)

### Finding: REDIR-001 Redirect URI Normalization Bypass
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:500-535` (`normalizeUri`), lines 308-317 (URI comparison)
- **Description**: Redirect URI comparison uses `normalizeUri` instead of exact string matching. The function lowercases scheme/host, removes default ports, and normalizes paths. RFC 9700 Section 4.1 requires exact string comparison. Bypass vectors include port normalization (`https://example.com:443/callback` matching `https://example.com/callback`), case normalization, and potential `URI.create()` path resolution.
- **Risk**: Crafted redirect URIs may normalize to match registered URIs but point elsewhere, enabling authorization code theft.
- **Recommendation**: Replace with exact string comparison: `allowedUri.toString() == redirectUri`.
- **References**: RFC 9700 Section 4.1, ASVS 10.4.1

### Finding: RESP-001 Implicit Grant (response_type=token) Advertised
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:217`, `core/src/main/kotlin/com/revethq/auth/core/authorization/dto/WellKnown.kt:71`
- **Description**: The well-known endpoint advertises `response_type=token` (implicit grant). OAuth 2.1 (RFC 9700 Section 2.1.2) deprecates the implicit grant.
- **Risk**: Clients may use implicit flow, exposing tokens in URL fragments, browser history, and Referer headers.
- **Recommendation**: Remove `TOKEN` from `responseTypesSupported`. Only advertise `code`.
- **References**: RFC 9700 Section 2.1.2, ASVS 10.4.4

### Finding: RESP-002 Hybrid Flow (response_type=code id_token) Advertised
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:216`, `core/src/main/kotlin/com/revethq/auth/core/authorization/dto/WellKnown.kt:72`
- **Description**: The well-known endpoint advertises `response_type=code id_token` (hybrid flow), exposing ID tokens in the front-channel.
- **Risk**: ID token exposed in URL fragment accessible via browser history, Referer headers, and JavaScript.
- **Recommendation**: Remove `CODE_ID_TOKEN` from `responseTypesSupported`.
- **References**: RFC 9700 Section 2.1.2, ASVS 10.6.1

### Finding: CSRF-001 Missing CSRF Protection on Login Form
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:93`, `persistence/src/main/resources/templates/login.html:1145`
- **Description**: Line 93 has `// TODO: Check the CSRF token`. The login form has no CSRF token hidden field. No CSRF protection exists.
- **Risk**: Login CSRF attacks, session fixation, or forced authorization flow completion.
- **Recommendation**: Generate CSRF token, embed as hidden form field, validate on submission.
- **References**: RFC 9700 Section 4.7, OWASP CSRF Prevention

### Finding: CODE-002 Authorization Code Has No Expiration
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/ClientCode.kt`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:59-109,147-152`
- **Description**: `ClientCode` entity has no `expiresAt` field. The `findByCode` query has no time-based filtering. Codes never expire. ASVS 10.4.3 requires max 10 minutes (L1/L2) or 1 minute (L3).
- **Risk**: Unlimited window for code replay. Combined with CODE-001, creates persistent replay vulnerability.
- **Recommendation**: Add `expiresAt` field. Set to `createdOn.plusMinutes(1)`. Add time filter to queries.
- **References**: ASVS 10.4.3, RFC 6749 Section 4.1.2

---

## Medium Findings (6)

### Finding: CODE-003 Authorization Code Client Binding Not Validated
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:441-469`
- **Description**: During token exchange, `clientCode.clientId` (line 450) is not compared against `requestClientId` (line 404). A different client could exchange another client's code.
- **Risk**: Authorization code injection -- code issued to Client A exchanged by Client B.
- **Recommendation**: Add: `if (clientCode.clientId != requestClientId) throw BadRequestException("Client ID mismatch")`
- **References**: RFC 9700 Section 4.5, RFC 6749 Section 4.1.3

### Finding: SCOPE-001 No Scope Validation During Refresh
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:471-489`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:230-304`
- **Description**: The `scope` parameter in refresh requests is silently ignored. Scopes always come from stored token. Per RFC 9700 Section 2.3, broader scope requests should be explicitly rejected.
- **Risk**: Clients receive no error feedback. Violates RFC requirements for explicit scope handling.
- **Recommendation**: Validate requested scopes are a subset of original grant. Return `invalid_scope` if broader.
- **References**: RFC 9700 Section 2.3, RFC 6749 Section 6

### Finding: HEADER-001 No Clickjacking Protection
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServerRoutes.kt:69-84`, `web/src/main/resources/application.properties`
- **Description**: No `X-Frame-Options` or `Content-Security-Policy: frame-ancestors` headers anywhere in the codebase.
- **Risk**: Login page can be framed for clickjacking attacks.
- **Recommendation**: Add `X-Frame-Options: DENY` and `Content-Security-Policy: frame-ancestors 'none'`.
- **References**: RFC 9700 Section 4.16, OWASP WSTG-ATHZ-05

### Finding: HEADER-002 No Referrer-Policy Header
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:183`
- **Description**: 302 redirect response includes authorization code in URL but no `Referrer-Policy` header. Codes leak via `Referer` to third-party resources.
- **Risk**: Authorization codes leak through Referer header to third parties.
- **Recommendation**: Add `.header("Referrer-Policy", "no-referrer")` to redirect responses.
- **References**: RFC 9700 Section 4.2

### Finding: MIXUP-001 No Issuer (iss) in Authorization Response
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:162-183`
- **Description**: Authorization response redirect does not include `iss` parameter per RFC 9207. Multiple authorization servers are supported via `authorizationServerId`.
- **Risk**: Mix-up attacks where codes from one server are sent to another's token endpoint.
- **Recommendation**: Add `&iss=<issuer_url>` to the redirect URL.
- **References**: RFC 9207, RFC 9700 Section 4.4, ASVS 10.2.2

### Finding: CORS-001 Overly Permissive CORS Configuration
- **Severity**: Medium
- **Location**: `web/src/main/resources/application.properties:8`
- **Description**: `quarkus.http.cors.origins=/.*/` matches any origin, combined with `access-control-allow-credentials=true`. Also `quarkus.tls.trust-all=true`.
- **Risk**: Any website can make credentialed cross-origin requests to all endpoints.
- **Recommendation**: Restrict CORS origins. Remove `trust-all` from production.
- **References**: OWASP CORS Misconfiguration

---

## Low Findings (4)

### Finding: STATE-001 State Parameter Not Enforced
- **Severity**: Low
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:265-358`
- **Description**: `state` is optional and passed through correctly when present, but not enforced.
- **Recommendation**: Consider logging warnings when absent. Document as recommended.
- **References**: RFC 9700 Section 4.7

### Finding: TOKEN-001 ID Token Identical to Access Token
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:545`
- **Description**: `accessToken.idToken = token` with TODO comment. Same JWT for both. Missing OIDC claims (`auth_time`, `azp`, `at_hash`).
- **Recommendation**: Generate separate ID token with proper OIDC claims and client-specific audience.
- **References**: OpenID Connect Core 1.0 Section 2

### Finding: DISC-001 Revocation Endpoint Advertised But Not Implemented
- **Severity**: Low
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:210`
- **Description**: Well-known advertises `/revocation/` but no endpoint handler exists. Inaccurate metadata.
- **Recommendation**: Implement the endpoint or remove from well-known response.
- **References**: RFC 7009

### Finding: NONCE-001 Nonce Returned in Authorization Response URL
- **Severity**: Low
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:168-170`
- **Description**: Nonce is non-standardly included in the redirect URL query string. It should only be in the ID token.
- **Recommendation**: Remove nonce from redirect URL.
- **References**: OpenID Connect Core 1.0 Section 3.1.2.1

---

## Informational Findings (4)

### Finding: CONSENT-001 No User Consent Mechanism
- **Severity**: Informational
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:80-184`
- **Description**: No consent screen between authentication and code issuance. No consent entity/service/UI.
- **References**: ASVS 10.7.1, 10.7.2, 10.7.3

### Finding: RESP-003 Response Type Parameter Not Validated
- **Severity**: Informational
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:265-358`
- **Description**: `responseType` is accepted but never validated. Any value proceeds without error.
- **References**: RFC 9700 Section 2.1.2, ASVS 10.6.1

### Finding: SUBJ-001 Incorrect Subject Claim in Refresh Token Flow
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:280`
- **Description**: `sub` claim set to `storedToken.clientId` instead of `storedToken.userId` after refresh. Changes user identity on refresh.
- **Recommendation**: Use `storedToken.userId!!.toString()` for `sub` claim.
- **References**: OpenID Connect Core 1.0 Section 2

### Finding: HOST-001 Issuer URL from Database (Positive)
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:526`
- **Description**: Issuer URL derived from database-stored `serverUrl`, not HTTP `Host` header. Correctly prevents host header poisoning.
- **References**: OWASP Host Header Injection

---

## Additional Notes

- **302 redirects** (not 307/308) are correctly used at line 183, preventing POST body forwarding.
- **ROPC grant** is not supported (only `client_credentials`, `authorization_code`, `refresh_token` in `GrantTypeEnum`), which is OAuth 2.1 compliant.
- **Scope escalation during code exchange** is prevented since scopes come from stored `ClientCode`, not the token request.
- **PKCE plain method** and **PKCE downgrade** are moot since PKCE is not enforced at all.
