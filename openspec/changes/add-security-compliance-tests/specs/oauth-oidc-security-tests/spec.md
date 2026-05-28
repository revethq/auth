## ADDED Requirements

### Requirement: PKCE Enforcement Tests
The test suite SHALL verify PKCE enforcement on the authorization and token endpoints per OAuth 2.1 and RFC 9700 §4.8.

**References**: ASVS 10.4.6, RFC 9700 §4.8, OWASP WSTG-ATHZ-05

#### Scenario: Authorization request without code_challenge is rejected
- **WHEN** a GET request to `/{id}/authorization/` omits the `code_challenge` parameter
- **THEN** the response rejects the request or the resulting authorization code cannot be exchanged without a `code_verifier`

#### Scenario: Plain challenge method is rejected
- **WHEN** a GET request to `/{id}/authorization/` includes `code_challenge_method=plain`
- **THEN** the response rejects the request with an appropriate error

#### Scenario: Token exchange without code_verifier fails
- **WHEN** an authorization code was issued with a `code_challenge` and the token request omits `code_verifier`
- **THEN** the token endpoint returns an error

#### Scenario: Token exchange with wrong code_verifier fails
- **WHEN** a token request includes a `code_verifier` that does not match the stored `code_challenge`
- **THEN** the token endpoint returns an error

#### Scenario: Token exchange with correct code_verifier succeeds
- **WHEN** a token request includes a `code_verifier` that matches the `code_challenge` (S256)
- **THEN** the token endpoint returns a valid access token

### Requirement: Redirect URI Validation Tests
The test suite SHALL verify redirect URI validation per ASVS 10.4.1 (exact string matching) and RFC 9700 §4.1.

**References**: ASVS 10.4.1, RFC 9700 §4.1, §4.11

#### Scenario: Exact registered redirect URI succeeds
- **WHEN** a GET request to `/{id}/authorization/` includes a `redirect_uri` that exactly matches a registered URI
- **THEN** the authorization flow proceeds (login screen rendered)

#### Scenario: Unregistered redirect URI is rejected without redirect
- **WHEN** a GET request includes a `redirect_uri` not in the client's registered list
- **THEN** the server returns an error response directly (does not redirect to the unregistered URI)

#### Scenario: Redirect URI with path traversal is rejected
- **WHEN** a GET request includes a `redirect_uri` containing `/../` path traversal
- **THEN** the server rejects the request

#### Scenario: Redirect URI with added path is rejected
- **WHEN** a registered URI is `https://example.com/callback` and the request uses `https://example.com/callback/evil`
- **THEN** the server rejects the request (no prefix matching)

#### Scenario: Redirect URI with different port is rejected
- **WHEN** the registered URI uses port 443 and the request uses a non-default port
- **THEN** the server rejects the request unless normalization is exact

#### Scenario: Missing redirect URI is rejected
- **WHEN** a GET request omits the `redirect_uri` parameter
- **THEN** the server returns a 400 error response

### Requirement: Authorization Code Lifecycle Tests
The test suite SHALL verify authorization code security properties per ASVS 10.4.2, 10.4.3, and RFC 9700 §4.5.

**References**: ASVS 10.4.2, 10.4.3, RFC 9700 §4.5

#### Scenario: Authorization code single-use enforcement
- **WHEN** a valid authorization code is exchanged for tokens at the token endpoint
- **AND** the same code is submitted a second time
- **THEN** the second request fails

#### Scenario: Authorization code reuse triggers token revocation
- **WHEN** a valid authorization code is exchanged for tokens
- **AND** the same code is submitted again
- **THEN** any tokens issued from the first exchange are revoked (ASVS 10.4.2)

#### Scenario: Authorization code bound to client
- **WHEN** an authorization code issued for client A is submitted by client B at the token endpoint
- **THEN** the token endpoint rejects the request

#### Scenario: Authorization code bound to redirect URI
- **WHEN** an authorization code is exchanged with a different `redirect_uri` than the one used in the authorization request
- **THEN** the token endpoint rejects the request

#### Scenario: Expired authorization code is rejected
- **WHEN** an authorization code older than the configured TTL is submitted to the token endpoint
- **THEN** the token endpoint rejects the request

### Requirement: State Parameter Tests
The test suite SHALL verify state parameter handling for CSRF protection per RFC 9700 §4.7.

**References**: RFC 9700 §4.7

#### Scenario: State parameter returned in redirect
- **WHEN** a `state` parameter is included in the authorization request and the user authenticates successfully
- **THEN** the redirect includes the same `state` value unmodified

#### Scenario: State parameter preserved on error redirect
- **WHEN** a `state` parameter is included and the authorization fails due to invalid scope
- **THEN** the error redirect includes the `state` value

### Requirement: Grant Type Restriction Tests
The test suite SHALL verify that only supported grant types are accepted and deprecated grants are rejected per ASVS 10.4.4.

**References**: ASVS 10.4.4, RFC 9700 §2.1.2, §2.4

#### Scenario: Authorization code grant succeeds
- **WHEN** a token request with `grant_type=authorization_code` and valid code is submitted
- **THEN** the token endpoint returns a valid access token

#### Scenario: Client credentials grant succeeds
- **WHEN** a token request with `grant_type=client_credentials` and valid credentials is submitted
- **THEN** the token endpoint returns a valid access token

#### Scenario: Refresh token grant succeeds
- **WHEN** a token request with `grant_type=refresh_token` and valid refresh token is submitted
- **THEN** the token endpoint returns a new access token and refresh token

#### Scenario: Unsupported grant type is rejected
- **WHEN** a token request with `grant_type=password` (ROPC) is submitted
- **THEN** the token endpoint returns an error

#### Scenario: Missing grant type is rejected
- **WHEN** a token request without a `grant_type` parameter is submitted
- **THEN** the token endpoint returns a 400 error

### Requirement: Token Endpoint Client Authentication Tests
The test suite SHALL verify that client authentication is enforced on the token endpoint per ASVS 10.4.10.

**References**: ASVS 10.4.10

#### Scenario: Client credentials with invalid secret fails
- **WHEN** a token request with `grant_type=client_credentials` and an incorrect `client_secret` is submitted
- **THEN** the token endpoint rejects the request

#### Scenario: Client credentials with missing client_id fails
- **WHEN** a token request with `grant_type=client_credentials` but no `client_id` is submitted
- **THEN** the token endpoint returns a 400 error

#### Scenario: Client credentials from wrong authorization server fails
- **WHEN** an application's credentials from authorization server A are submitted to authorization server B's token endpoint
- **THEN** the token endpoint rejects the request

### Requirement: Refresh Token Lifecycle Tests
The test suite SHALL verify refresh token rotation, reuse detection, and scope binding per ASVS 10.4.5, 10.4.8, 10.4.9, and RFC 9700 §4.14.

**References**: ASVS 10.4.5, 10.4.8, 10.4.9, RFC 9700 §4.14

#### Scenario: Refresh token rotation issues new token
- **WHEN** a valid refresh token is exchanged for new tokens
- **THEN** the response includes a new, different refresh token

#### Scenario: Old refresh token is revoked after rotation
- **WHEN** a refresh token is exchanged and a new one is issued
- **AND** the old refresh token is submitted again
- **THEN** the token endpoint rejects the old token

#### Scenario: Refresh token reuse revokes token family
- **WHEN** a revoked refresh token is submitted (indicating potential theft)
- **THEN** all tokens in the family are revoked (RFC 9700 §4.14)

#### Scenario: Refresh token scope cannot be escalated
- **WHEN** a refresh token request includes a broader `scope` than the original grant
- **THEN** the token endpoint either rejects the request or limits the scope to the original grant

#### Scenario: Refresh token bound to client
- **WHEN** a refresh token issued to client A is submitted by client B
- **THEN** the token endpoint rejects the request

### Requirement: JWT Access Token Validation Tests
The test suite SHALL verify JWT structure, claims, and signing per ASVS 10.3.1, 10.3.2, 10.3.3.

**References**: ASVS 10.3.1, 10.3.2, 10.3.3

#### Scenario: Access token has required claims
- **WHEN** a valid access token is obtained from the token endpoint
- **THEN** the decoded JWT contains `iss`, `aud`, `sub`, `exp`, `scope` claims

#### Scenario: Issuer claim matches authorization server
- **WHEN** an access token is obtained from authorization server A
- **THEN** the `iss` claim matches authorization server A's configured issuer URL

#### Scenario: Audience claim is set
- **WHEN** an access token is obtained
- **THEN** the `aud` claim matches the authorization server's configured audience

#### Scenario: Token is signed with RS256
- **WHEN** an access token JWT header is inspected
- **THEN** the `alg` is `RS256` and the `kid` matches a key in the JWKS endpoint

#### Scenario: Token is verifiable via JWKS
- **WHEN** an access token is obtained and the JWKS endpoint is queried
- **THEN** the token signature can be verified using the public key from JWKS matching the `kid`

#### Scenario: Expired token is rejected at userinfo
- **WHEN** an expired or malformed bearer token is sent to the `/userinfo/` endpoint
- **THEN** the endpoint rejects the request

### Requirement: Well-Known Discovery Endpoint Tests
The test suite SHALL verify that the OIDC discovery endpoint returns accurate, secure metadata.

#### Scenario: Discovery endpoint returns valid metadata
- **WHEN** a GET request is made to `/{id}/.well-known/openid-configuration/`
- **THEN** the response contains `issuer`, `authorization_endpoint`, `token_endpoint`, `jwks_uri`, and `response_types_supported`

#### Scenario: Discovery response_types_supported is accurate
- **WHEN** the discovery endpoint `response_types_supported` is reviewed
- **THEN** document whether `token` (implicit) is listed (OAuth 2.1 non-compliance if so)

#### Scenario: Discovery code_challenge_methods_supported lists S256
- **WHEN** the discovery endpoint is queried
- **THEN** `code_challenge_methods_supported` includes `S256`

### Requirement: Scope Enforcement Tests
The test suite SHALL verify that scopes are properly validated and cannot be escalated during any flow.

**References**: ASVS 10.4.11, RFC 9700 §2.3

#### Scenario: Invalid scope triggers error redirect
- **WHEN** the authorization endpoint receives a request with a scope not registered for the authorization server
- **THEN** the server redirects to the client with an `invalid_scope` error

#### Scenario: Token endpoint limits scopes to authorization
- **WHEN** a client_credentials token request includes scopes not assigned to the application secret
- **THEN** the returned token only contains the intersection of requested and allowed scopes

#### Scenario: Cross-tenant scope rejected
- **WHEN** a scope registered to authorization server A is requested against authorization server B
- **THEN** the scope is not included in the resulting token

### Requirement: Multi-Tenant Authorization Isolation Tests
The test suite SHALL verify that authorization artifacts cannot cross tenant boundaries.

#### Scenario: Authorization code from server A rejected at server B token endpoint
- **WHEN** an authorization code issued under authorization server A is submitted to server B's token endpoint
- **THEN** the token endpoint rejects the request

#### Scenario: Client from server A rejected at server B authorization endpoint
- **WHEN** a client registered under authorization server A initiates authorization at server B
- **THEN** the authorization endpoint rejects the request

#### Scenario: JWKS endpoint returns only tenant keys
- **WHEN** the JWKS endpoint for authorization server A is queried
- **THEN** only keys belonging to server A are returned (no keys from server B)

#### Scenario: Token from server A rejected at server B userinfo
- **WHEN** an access token issued by authorization server A is presented to server B's userinfo endpoint
- **THEN** the endpoint rejects the token
