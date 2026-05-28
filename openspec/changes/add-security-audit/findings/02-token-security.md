# Token Security Audit Findings

**Audit Date**: 2026-05-21
**Auditor**: Claude Opus 4.6 (Automated Security Audit)
**Scope**: JWT signing, refresh tokens, token expiration, claim validation, leakage prevention, sender-constrained tokens, client authentication

## Summary

| Severity      | Count |
|---------------|-------|
| Critical      | 2     |
| High          | 5     |
| Medium        | 5     |
| Low           | 3     |
| Informational | 4     |
| **Total**     | **19**|

---

## JWT Signing Security

### Finding: TS-01 RSA Key Generation Missing Explicit SecureRandom
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:96-97`
- **Description**: The `KeyPairGenerator` is initialized with a key size of 2048 bits but does not explicitly pass a `SecureRandom` instance to `generator.initialize()`. While the JDK default `KeyPairGenerator.initialize(int)` does use a system-provided secure random source internally, explicitly specifying `SecureRandom()` is a defense-in-depth best practice that makes the security intent clear and avoids any platform-specific deviations.
- **Risk**: In practice the risk is negligible on standard JVMs, as the default provider uses a secure PRNG. However, on non-standard or constrained JVM implementations, the default could theoretically fall back to a weaker source.
- **Recommendation**: Pass an explicit `SecureRandom` instance: `generator.initialize(2048, SecureRandom())`.
- **References**: NIST SP 800-57, Java Security Standard Algorithm Names documentation

### Finding: TS-02 Insecure Random Used for Signing Key Selection
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:389`
- **Description**: The `getSigningKeysForAuthorizationServer` method uses `java.util.Random()` (imported at line 73) instead of `java.security.SecureRandom` to select which signing key to use from the available keys. `java.util.Random` is a linear congruential generator with predictable output. Additionally, a new `Random()` instance is created on every call, which means the seed is based on `System.nanoTime()`, making the selection pattern potentially predictable. The expression `drop(Random().nextInt(keys.size)).firstOrNull()` also produces a biased distribution -- it does not select uniformly.
- **Risk**: An attacker who can observe multiple JWT `kid` headers over time could predict which signing key will be selected next. While this alone does not compromise the signing keys, it reduces the security benefit of having multiple keys and could inform targeted attacks against a specific key.
- **Recommendation**: Replace `Random()` with `SecureRandom()` or, better yet, use a deterministic key selection strategy (e.g., always use the most recently created key for signing, and only maintain older keys for verification).
- **References**: CWE-330 (Use of Insufficiently Random Values)

### Finding: TS-03 No Key Rotation Mechanism
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt` (entire file)
- **Description**: Signing keys are generated once during authorization server creation (line 318) and there is no mechanism for key rotation. There is no endpoint, scheduled job, or administrative function to rotate signing keys. The `SigningKey` entity (at `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/SigningKey.kt`) has no expiration or status field to mark keys as active/retired.
- **Risk**: If a signing key is compromised, there is no way to rotate to a new key without recreating the entire authorization server. Long-lived keys increase the window of exposure if a key is compromised.
- **Recommendation**: Implement a key rotation mechanism that: (1) generates a new signing key, (2) marks the old key as "verification only," (3) retains old keys in JWKS for a grace period to allow existing tokens to be verified, and (4) eventually removes retired keys.
- **References**: NIST SP 800-57 Part 1 (Key Management), RFC 7517 Section 4.5

### Finding: TS-04 JWK Exponent Hardcoded, Overwriting Computed Value
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:127,135`
- **Description**: The `buildJwk` method computes the exponent `e` from the actual public key at line 127, but then immediately overwrites it with the hardcoded string `"AQAB"` at line 135. While `"AQAB"` is the base64url encoding of 65537 (the standard RSA public exponent), this hardcoding creates dead code and means that if a key were ever generated with a non-standard exponent, the JWKS would advertise the wrong value.
- **Risk**: Low -- this is a correctness issue rather than a direct security vulnerability. The standard public exponent 65537 is expected for RSA keys.
- **Recommendation**: Either remove the dead code at line 127 (since the value is overwritten) or remove the hardcoded value at line 135 and use the computed value.
- **References**: RFC 7518 Section 6.3.1

### Finding: TS-05 Algorithm Confusion Prevention is Properly Implemented
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:410-414`
- **Description**: The JWT validation code correctly uses `AlgorithmConstraints` with `ConstraintType.PERMIT` and only allows `AlgorithmIdentifiers.RSA_USING_SHA256`. This effectively prevents algorithm confusion attacks where an attacker might try to use `"none"` or switch to a symmetric algorithm like HS256 with the public key.
- **Risk**: None -- this is correctly implemented.
- **Recommendation**: No action needed. This is a positive finding.
- **References**: CVE-2015-9235, RFC 7518

---

## Refresh Token Security

### Finding: TS-06 Refresh Token Has Sufficient Entropy
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:437-441`
- **Description**: Refresh tokens are generated using `SecureRandom` with 32 bytes (256 bits) of entropy, well above the 128-bit minimum. The tokens are base64url-encoded without padding, producing opaque tokens.
- **Risk**: None -- this is correctly implemented.
- **Recommendation**: No action needed.
- **References**: RFC 9700 Section 4.14

### Finding: TS-07 Refresh Tokens Stored as SHA-256 Hashes (Acceptable but Not Ideal)
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:444-447`
- **Description**: Refresh tokens are hashed with SHA-256 before storage, which prevents plaintext token exposure from database compromise. However, SHA-256 is a fast hash, and while the 256-bit entropy of the tokens makes brute-force infeasible, a slower hash like bcrypt or argon2id would provide additional protection against preimage attacks if token entropy were ever reduced.
- **Risk**: With the current 256-bit entropy, this is not practically exploitable. If token entropy were ever reduced, the fast SHA-256 hash could become brute-forceable.
- **Recommendation**: SHA-256 is acceptable given the high entropy. For defense-in-depth, consider HMAC-SHA256 with a server-side secret key.
- **References**: OWASP Token Storage Cheat Sheet, ASVS 10.4.5

### Finding: TS-08 Refresh Token Rotation Implemented but Missing Reuse Detection
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:248-251` and `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/RefreshTokenRepository.kt:32-37`
- **Description**: Token rotation is correctly implemented: when a refresh token is used, the old token is immediately revoked (line 249) and a new token is issued (lines 287-300). However, there is no token family tracking or reuse detection. The `RefreshTokenRepository.findByTokenHash` query filters for `isRevoked = false`, so a revoked token simply returns `Optional.empty()` and results in a generic "Invalid or expired refresh token" error. There is no mechanism to detect that a **previously valid but now revoked** token was presented, which would indicate token theft.
- **Risk**: If an attacker steals a refresh token and uses it before the legitimate client does, the legitimate client's subsequent use of the now-revoked token will fail silently. The attacker's new token family continues to work. RFC 9700 Section 4.14 recommends that reuse of a revoked token should trigger revocation of the entire token family.
- **Recommendation**: Add a `familyId` (or `parentTokenId`) column to the `RefreshToken` entity. When a revoked token is presented, look up its family and revoke all tokens in that family.
- **References**: RFC 9700 Section 4.14, ASVS 10.4.5

### Finding: TS-09 Refresh Token Subject Claim Set to Client ID Instead of User ID
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:280`
- **Description**: In the `refreshAccessToken` method, the `sub` claim of the new access token is set to `storedToken.clientId!!` (line 280) instead of the user's ID. Compare this with `generateAuthorizationCodeFlowAccessToken` at line 198 where `sub` is correctly set to the `subject` parameter (which is `codeUserId.toString()`). This means that after a token refresh, the access token's `sub` claim identifies the OAuth client rather than the authenticated user.
- **Risk**: (1) Resource servers relying on the `sub` claim will receive the client ID instead of the user ID, potentially granting access to wrong resources. (2) The UserInfo endpoint uses `jwtClaims["sub"]` to look up the user profile, which will fail or return wrong data after a refresh. (3) Audit logs based on `sub` will attribute actions to the client rather than the user.
- **Recommendation**: Change line 280 from `additionalClaims["sub"] = storedToken.clientId!!` to `additionalClaims["sub"] = storedToken.userId.toString()`.
- **References**: RFC 7519 Section 4.1.2, ASVS 10.3.3

### Finding: TS-10 No Absolute Expiration Limit on Refresh Token Chains
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:296`
- **Description**: Each time a refresh token is rotated, the new token receives a fresh 30-day expiration (`OffsetDateTime.now().plusDays(30)`). This creates a sliding expiration window -- as long as the refresh token is used within 30 days, the user session can be extended indefinitely without re-authentication. There is no absolute lifetime limit on the token chain.
- **Risk**: A compromised refresh token can maintain access indefinitely if rotated regularly. ASVS 10.4.8 requires that token-based sessions have absolute timeouts.
- **Recommendation**: Track the original grant time (e.g., store `originalGrantedAt` in the RefreshToken entity) and enforce an absolute maximum lifetime (e.g., 90 days from original grant).
- **References**: ASVS 10.4.8, RFC 9700 Section 4.14

### Finding: TS-11 Refresh Token Revocation API Exists but No External Endpoint
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:456-458` and `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/RefreshTokenRepository.kt:39-44`
- **Description**: The service layer has methods `revokeAllUserRefreshTokens` and the repository has `revokeToken` and `revokeAllUserTokens`. The `.well-known` configuration advertises a `revocation_endpoint`. However, reviewing the `AuthorizationServerApi` interface, there is no implemented revocation endpoint handler. The revocation endpoint URL is advertised but not functional.
- **Risk**: Clients and administrators cannot revoke tokens through the standard OAuth 2.0 revocation flow (RFC 7009). Only internal service calls can perform revocation.
- **Recommendation**: Implement the token revocation endpoint per RFC 7009, accepting a token and token_type_hint, authenticating the client, and revoking the specified token.
- **References**: RFC 7009 (OAuth 2.0 Token Revocation), ASVS 10.4.9

---

## Token Expiration

### Finding: TS-12 Authorization Codes Have No Expiration
- **Severity**: Critical
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/ClientCode.kt` and `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/ClientCodeRepository.kt:30-33`
- **Description**: The `ClientCode` entity has a `createdOn` field but no `expiresAt` field. The `ClientCodeRepository.findByCode` method retrieves codes without any time-based filtering. Authorization codes never expire and can be used at any time after creation. There is also no mechanism to invalidate a code after it has been exchanged for tokens (the code is not deleted or marked as used after the token exchange at lines 441-469 of `AuthorizationServer.kt`).
- **Risk**: An attacker who intercepts an authorization code has an unlimited window to exchange it for tokens. OAuth 2.1 and RFC 9700 require authorization codes to expire quickly (ASVS 10.4.3 specifies maximum 10 minutes at L1/L2, 1 minute at L3). Additionally, the same code can be used multiple times, violating RFC 6749 Section 4.1.2 which states that authorization codes MUST be single-use.
- **Recommendation**: (1) Add an `expiresAt` field to `ClientCode` entity, set to `createdOn + 60 seconds` (or at most 10 minutes). (2) Filter by expiration in `findByCode`. (3) Delete or mark the code as used immediately after token exchange. (4) If a code is presented twice, revoke all tokens issued from that code.
- **References**: RFC 6749 Section 4.1.2, RFC 9700 Section 4.5, ASVS 10.4.3

### Finding: TS-13 PKCE Code Verifier Not Validated at Token Exchange
- **Severity**: Critical
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:441-469`
- **Description**: The `codeVerifier` parameter is accepted at the token endpoint (lines 381, 398) and the `codeChallenge`/`codeChallengeMethod` are stored with the authorization code (lines 153-154). However, at the token exchange (lines 441-469), the `codeVerifier` is never validated against the stored `codeChallenge`. The code verifier is completely ignored. The `.well-known` configuration advertises S256 as a supported code challenge method (line 219), giving clients the false impression that PKCE is enforced.
- **Risk**: PKCE is the primary defense against authorization code interception attacks in OAuth 2.1. Without verification, an attacker who intercepts an authorization code can exchange it for tokens without possessing the code verifier. This completely defeats PKCE, which is mandatory in OAuth 2.1.
- **Recommendation**: At the token exchange: (1) If the authorization code was issued with a `code_challenge`, require the `code_verifier` parameter. (2) Compute `BASE64URL(SHA256(code_verifier))` and compare it to the stored `code_challenge`. (3) Reject the request if they do not match. (4) Consider making PKCE mandatory for all public clients.
- **References**: RFC 7636 (PKCE), RFC 9700 Section 4.5, OAuth 2.1 draft Section 4.1.2

### Finding: TS-14 Access Token Expiration Properly Set
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:171,204,282,528,565`
- **Description**: Access tokens have configurable expiration via `authorizationCodeTokenExpiration` and `clientCredentialsTokenExpiration` fields on the AuthorizationServer entity, defaulting to 3600 seconds (1 hour). The `exp` claim is set correctly. JWT validation requires expiration via `setRequireExpirationTime()` (line 405).
- **Risk**: None -- expiration is correctly enforced. 1 hour is on the longer side; 5-15 minutes is more common for high-security deployments.
- **Recommendation**: Consider documenting recommended expiration values and allowing shorter expirations for sensitive applications.
- **References**: RFC 9700 Section 4.13

---

## Token Claim Validation

### Finding: TS-15 Audience Validation Contradictory Configuration
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:407,409`
- **Description**: The JWT validation code calls both `.setSkipDefaultAudienceValidation()` (line 407) and `.setExpectedAudience(authorizationServer.audience)` (line 409). In jose4j, `setSkipDefaultAudienceValidation()` disables the built-in audience validation that would otherwise require the `aud` claim to be present. If `audience` on the AuthorizationServer entity is null, `setExpectedAudience(null)` may effectively skip audience validation entirely.
- **Risk**: If the `audience` field is null or empty, audience validation may be silently bypassed, allowing tokens intended for one resource server to be accepted by another (ASVS 10.3.1).
- **Recommendation**: (1) Remove `setSkipDefaultAudienceValidation()` and rely solely on `setExpectedAudience()`. (2) Validate that `authorizationServer.audience` is non-null and non-empty before proceeding. (3) Add a NOT NULL constraint on the audience column.
- **References**: ASVS 10.3.1, RFC 7519 Section 4.1.3

### Finding: TS-16 ID Token Identical to Access Token
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:545`
- **Description**: The access token and ID token are set to the same JWT value: `accessToken.idToken = token` with the comment `// TODO: ID token should be separate from access token`. The ID token in OIDC is intended for client consumption to verify user authentication, while the access token is intended for resource server consumption. They should have different audiences, and the ID token should contain an `at_hash` claim.
- **Risk**: (1) The access token contains user profile claims which may leak sensitive user information to resource servers. (2) The ID token has the wrong audience (resource server instead of client_id). (3) A resource server receiving the access token can impersonate the user to other services.
- **Recommendation**: Generate separate JWTs for access token and ID token. The ID token should have the client_id as audience, include `at_hash`, and contain only identity claims.
- **References**: OIDC Core Section 2, Section 3.1.3.3, ASVS 10.3.2

### Finding: TS-17 User Profile Claims Included Directly in Access Token
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:540,577`
- **Description**: Both `buildAccessTokenResponse` and `buildClientCredentialsAccessToken` iterate over `profile.profile` and include all profile key-value pairs directly as JWT claims. Whatever arbitrary data is in the user/application profile is included in the unencrypted access token.
- **Risk**: (1) Sensitive user data is exposed in access tokens transmitted to multiple resource servers. (2) Profile data is not filtered before inclusion. (3) Violates data minimization principles (ASVS 10.3.2, RFC 9700 Section 4.9).
- **Recommendation**: Remove profile claims from access tokens. Provide user information only through the UserInfo endpoint or the ID token. If specific claims are needed in the access token, use an explicit allowlist.
- **References**: ASVS 10.3.2, RFC 9700 Section 4.9, OIDC Core Section 5.4

---

## Token Leakage Prevention

### Finding: TS-18 JWT Validation Error Messages May Leak Internal Details
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:429-430`
- **Description**: When JWT validation fails, the jose4j `InvalidJwtException` message is passed directly to the client via `NotAuthorized(e.message ?: "Invalid JWT")`. These messages contain detailed information about why validation failed, including expected vs. actual claim values, algorithm details, and key information.
- **Risk**: An attacker probing the token validation endpoint can learn specific details about expected issuers, audiences, algorithms, and timing constraints.
- **Recommendation**: Log the detailed error message at DEBUG level for troubleshooting, but return only a generic error to clients: `throw NotAuthorized("Invalid or expired token")`.
- **References**: OWASP Testing Guide, CWE-209 (Information Exposure Through Error Message)

### Finding: TS-19 No Token Leakage in Logs (Positive Finding)
- **Severity**: Informational
- **Location**: All audited files
- **Description**: A search for logging statements containing token values found no instances where access tokens, refresh tokens, or their hashes are logged. Log statements in the authorization flow reference only usernames and authorization server IDs, not token values.
- **Risk**: None -- this is correctly implemented.
- **Recommendation**: No action needed.
- **References**: RFC 9700 Section 4.2, ASVS 10.1.1

---

## Sender-Constrained Tokens

### Finding: TS-20 No DPoP or mTLS Token Binding Support
- **Severity**: Medium (for L1/L2), would be High for L3
- **Location**: System-wide
- **Description**: The authorization server issues bearer tokens without any sender-constraining mechanism. There is no support for DPoP (RFC 9449) or mutual TLS certificate-bound tokens (RFC 8705). A codebase search for "DPoP", "mTLS", "proof-of-possession", and "sender-constrain" returned no results.
- **Risk**: Bearer tokens are susceptible to theft and replay. If stolen, they can be used by any party until expiration.
- **Recommendation**: Near-term: keep access token lifetimes short (5-15 minutes). Medium-term: implement DPoP support. High-security: implement mTLS certificate-bound tokens.
- **References**: RFC 9449 (DPoP), RFC 8705 (mTLS), ASVS 10.3.5, 10.4.14, RFC 9700 Section 4.10

---

## Confidential Client Backchannel Authentication

### Finding: TS-21 Client Authentication Only via client_secret_post
- **Severity**: Medium (for L1/L2), would be High for L3
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:406-439`
- **Description**: Only the `CLIENT_CREDENTIALS` grant type validates client credentials (lines 408-415); the `AUTHORIZATION_CODE` and `REFRESH_TOKEN` grant types do not authenticate the client at all. The `.well-known` configuration at line 212 only advertises `client_secret_post`. No support for `client_secret_basic`, `private_key_jwt`, or `client_secret_jwt`.
- **Risk**: (1) For authorization code flow, any party with the code can exchange it for tokens without client authentication. (2) For refresh token flow, only client_id is checked (line 239), not client_secret. (3) Combined with TS-13 (no PKCE), authorization code interception is trivially exploitable.
- **Recommendation**: (1) Require client authentication for confidential clients on all grant types. (2) Add support for `private_key_jwt` for higher security (ASVS 10.4.16 L3).
- **References**: ASVS 10.4.10, 10.4.16, RFC 9700 Section 4.10, RFC 6749 Section 2.3

### Finding: TS-22 Authorization Code Flow Missing Client Authentication
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:441-469`
- **Description**: When exchanging an authorization code for tokens, the token endpoint does not validate the client secret. The `clientId` is required (line 404), but no `clientSecret` verification occurs for the `AUTHORIZATION_CODE` grant type. Only `redirectUri` matching is checked (line 445).
- **Risk**: Any party that obtains an authorization code and knows the redirect URI can exchange it for tokens. RFC 6749 Section 4.1.3 requires client authentication at the token endpoint for confidential clients.
- **Recommendation**: Add client authentication for the authorization code grant type. Verify client_secret against registered credentials before issuing tokens.
- **References**: RFC 6749 Section 4.1.3, ASVS 10.4.10

---

## Additional Findings

### Finding: TS-23 Authorization Code Not Invalidated After Use
- **Severity**: High (related to TS-12)
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:441-469`
- **Description**: After an authorization code is exchanged for tokens, the code is not deleted or marked as used. The `getClientCode` method at `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:147-152` simply looks up the code with no subsequent invalidation. The same authorization code can be exchanged for tokens multiple times.
- **Risk**: An attacker who intercepts an authorization code can use it even after the legitimate client has already exchanged it. Per RFC 6749 Section 4.1.2: "If an authorization code is used more than once, the authorization server MUST deny the request and SHOULD revoke all tokens previously issued based on that authorization code."
- **Recommendation**: Delete the authorization code immediately after successful token exchange. If a code is presented a second time, revoke all tokens that were issued from that code.
- **References**: RFC 6749 Section 4.1.2, RFC 9700 Section 4.5

---

## Priority Remediation Order

The following is the recommended order for addressing these findings, based on severity and exploitability:

1. **TS-13** (Critical) -- Implement PKCE verification at token exchange
2. **TS-12** (Critical) -- Add authorization code expiration and single-use enforcement
3. **TS-23** (High) -- Invalidate authorization codes after use
4. **TS-22** (High) -- Add client authentication for authorization code grant
5. **TS-09** (High) -- Fix `sub` claim in refresh token flow (bug)
6. **TS-16** (High) -- Separate ID token from access token
7. **TS-17** (High) -- Remove profile claims from access tokens
8. **TS-15** (High) -- Fix audience validation configuration
9. **TS-08** (High) -- Add refresh token reuse detection with family revocation
10. **TS-10** (Medium) -- Add absolute expiration for refresh token chains
11. **TS-11** (Medium) -- Implement revocation endpoint
12. **TS-18** (Medium) -- Sanitize JWT validation error messages
13. **TS-02** (Medium) -- Replace `java.util.Random` with `SecureRandom`
14. **TS-03** (Medium) -- Implement key rotation mechanism
15. **TS-21/TS-20** (Medium) -- Enhance client authentication methods and consider DPoP
