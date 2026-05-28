## ADDED Requirements

### Requirement: Security Headers Tests
The test suite SHALL verify that security-relevant HTTP headers are present on authorization server responses.

**References**: RFC 9700 §4.16, OWASP WSTG-ATHZ-05

#### Scenario: Authorization endpoint has anti-clickjacking headers
- **WHEN** a GET request to `/{id}/authorization/` returns the login page
- **THEN** the response includes `X-Frame-Options: DENY` (or `SAMEORIGIN`) and/or `Content-Security-Policy` with `frame-ancestors 'none'`

#### Scenario: Token endpoint sets cache-control
- **WHEN** the token endpoint returns an access token response
- **THEN** the response includes `Cache-Control: no-store` and `Pragma: no-cache` to prevent token caching (per RFC 6749 §5.1)

#### Scenario: All endpoints set X-Content-Type-Options
- **WHEN** any endpoint returns a response
- **THEN** the response includes `X-Content-Type-Options: nosniff`

#### Scenario: Redirect responses set Referrer-Policy
- **WHEN** the authorization endpoint redirects with an authorization code
- **THEN** the response includes `Referrer-Policy: no-referrer` to prevent code leakage via Referer header (RFC 9700 §4.2)

### Requirement: Authorization Redirect Security Tests
The test suite SHALL verify that authorization redirects use secure HTTP methods and do not leak credentials.

**References**: RFC 9700 §4.12

#### Scenario: Redirect uses 302 not 307
- **WHEN** the authorization endpoint redirects the user after successful authentication
- **THEN** the HTTP status code is 302 (not 307 or 308), preventing POST body forwarding to the redirect target

#### Scenario: Authorization code in query not fragment
- **WHEN** the authorization endpoint redirects with an authorization code
- **THEN** the code is in the URL query string (`?code=...`), not in the fragment (`#code=...`)

#### Scenario: No tokens in redirect URL
- **WHEN** the authorization code flow completes and the redirect is issued
- **THEN** the redirect URL does not contain `access_token` or `refresh_token` parameters

### Requirement: OAuth Error Response Compliance Tests
The test suite SHALL verify that error responses follow RFC 6749 §5.2 format and do not leak information.

**References**: RFC 6749 §5.2, OWASP WSTG-ATHZ-05

#### Scenario: Token endpoint error response format
- **WHEN** the token endpoint rejects a request
- **THEN** the response body contains `error` and optionally `error_description` per RFC 6749 §5.2

#### Scenario: Login failure does not enumerate users
- **WHEN** authentication fails for an invalid username vs a valid username with wrong password
- **THEN** both cases return the same error message (no user enumeration)

#### Scenario: Invalid client does not leak existence
- **WHEN** the authorization endpoint receives an invalid `client_id`
- **THEN** the error response does not distinguish between "client does not exist" and "client not authorized"

#### Scenario: Error responses do not contain stack traces
- **WHEN** any endpoint encounters an internal error
- **THEN** the response does not include Java stack traces, class names, or internal implementation details

### Requirement: CORS Security Tests
The test suite SHALL verify CORS configuration is appropriate for an authorization server.

#### Scenario: Preflight request handling
- **WHEN** an OPTIONS preflight request is sent to the token endpoint with `Origin: https://attacker.com`
- **THEN** document the `Access-Control-Allow-Origin` header value and whether credentials are allowed

#### Scenario: CORS on authorization endpoint
- **WHEN** a cross-origin request is sent to the authorization endpoint
- **THEN** document whether the response allows the origin and assess the security impact

### Requirement: Token Endpoint Input Validation Tests
The test suite SHALL verify that the token endpoint properly validates all input parameters and rejects malformed requests.

#### Scenario: Malformed grant_type rejected
- **WHEN** the token endpoint receives an unrecognized `grant_type` value
- **THEN** the endpoint returns a 400 error with `unsupported_grant_type`

#### Scenario: Missing required parameters rejected
- **WHEN** the token endpoint receives a request missing required parameters for the given grant type (e.g., `code` for authorization_code)
- **THEN** the endpoint returns a 400 error

#### Scenario: Non-UUID authorization server ID rejected
- **WHEN** a request is sent with a non-UUID `authorizationServerId` path parameter
- **THEN** the endpoint returns an appropriate error (400 or 404)

#### Scenario: Non-existent authorization server ID rejected
- **WHEN** a request is sent with a valid UUID that does not match any authorization server
- **THEN** the endpoint returns an appropriate error without leaking server-side details

### Requirement: UserInfo Endpoint Security Tests
The test suite SHALL verify that the userinfo endpoint properly authenticates and authorizes requests.

#### Scenario: Missing Authorization header rejected
- **WHEN** a GET request to `/{id}/userinfo/` omits the Authorization header
- **THEN** the endpoint returns 401

#### Scenario: Malformed bearer token rejected
- **WHEN** the Authorization header contains a non-JWT value or truncated token
- **THEN** the endpoint returns 401 without leaking validation details

#### Scenario: Token from wrong authorization server rejected
- **WHEN** a valid token from authorization server A is presented to server B's userinfo endpoint
- **THEN** the endpoint returns 401

#### Scenario: Valid token returns user profile
- **WHEN** a valid bearer token is presented to the correct authorization server's userinfo endpoint
- **THEN** the endpoint returns the user's profile claims
