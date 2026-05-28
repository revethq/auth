## ADDED Requirements

### Requirement: PKCE Enforcement Audit
The audit SHALL verify that PKCE (Proof Key for Code Exchange) is mandatory for all authorization code grants per OAuth 2.1 (RFC 9126). The audit SHALL check that requests without `code_challenge` are rejected and that only `S256` is accepted as a challenge method.

**References**: ASVS 10.4.6, RFC 9700 §4.8

#### Scenario: PKCE not required on authorization requests
- **WHEN** the authorization endpoint is invoked without `code_challenge` or `code_challenge_method` parameters
- **THEN** document whether the request is accepted or rejected, and at what severity

#### Scenario: Plain PKCE method accepted
- **WHEN** the authorization endpoint is invoked with `code_challenge_method=plain`
- **THEN** document whether the plain method is accepted (OAuth 2.1 requires S256 only)

#### Scenario: Code verifier validation on token exchange
- **WHEN** a token request is made with an authorization code that had a `code_challenge`
- **THEN** verify that `code_verifier` is required and validated against the stored challenge

#### Scenario: PKCE downgrade attack
- **WHEN** an attacker strips the `code_challenge` parameter from an authorization request that a legitimate client would normally include
- **THEN** verify the authorization server rejects the request or that the token endpoint rejects code exchange without a verifier for clients that normally use PKCE (RFC 9700 §4.8)

### Requirement: Redirect URI Validation Audit
The audit SHALL verify that redirect URI validation prevents open redirect attacks. The audit SHALL check for exact string matching (per RFC 9700 §4.1), path traversal bypasses, fragment handling, and normalization attacks.

**References**: ASVS 10.4.1, RFC 9700 §4.1, §4.11

#### Scenario: Redirect URI strict matching
- **WHEN** a client registers redirect URIs
- **THEN** verify that the authorization endpoint performs exact string comparison (no wildcard, no substring matching) per ASVS 10.4.1

#### Scenario: URI normalization bypass
- **WHEN** redirect URIs with encoding variations (percent-encoding, case differences, trailing slashes, port normalization) are tested
- **THEN** document whether any normalization allows bypassing the registered URI check

#### Scenario: Unregistered redirect URI
- **WHEN** a redirect URI not in the client's registered list is provided
- **THEN** verify the request is rejected without redirecting to the attacker-controlled URI

#### Scenario: Path traversal in redirect URI
- **WHEN** redirect URIs containing `/../` path traversal sequences or multiple `redirect_uri` parameters (HTTP parameter pollution) are tested
- **THEN** document whether any bypass is possible

#### Scenario: Open redirect via registered URI
- **WHEN** registered redirect URIs are reviewed for open redirect potential (e.g., URIs that themselves perform further redirection)
- **THEN** document whether the authorization server's redirect could chain into an attacker-controlled destination (RFC 9700 §4.11)

### Requirement: Authorization Code Security Audit
The audit SHALL verify that authorization codes are single-use, short-lived, and bound to the requesting client. The audit SHALL check for code replay attacks and code injection.

**References**: ASVS 10.4.2, 10.4.3, RFC 9700 §4.5

#### Scenario: Authorization code single-use enforcement
- **WHEN** an authorization code is exchanged for tokens
- **THEN** verify the code is invalidated and cannot be reused; if reuse is detected, verify all tokens issued from that code are revoked (ASVS 10.4.2)

#### Scenario: Authorization code expiration
- **WHEN** the authorization code storage and lifecycle is reviewed
- **THEN** document the code TTL and whether expiration is enforced (maximum 10 minutes at L1/L2, 1 minute at L3 per ASVS 10.4.3)

#### Scenario: Authorization code client binding
- **WHEN** an authorization code is exchanged by a different client than the one that requested it
- **THEN** verify the exchange is rejected (RFC 9700 §4.5 authorization code injection)

### Requirement: State Parameter CSRF Protection Audit
The audit SHALL verify that the `state` parameter is properly handled to prevent CSRF attacks on the authorization flow.

**References**: RFC 9700 §4.7

#### Scenario: State parameter passthrough
- **WHEN** a `state` parameter is included in the authorization request
- **THEN** verify it is returned unmodified in the redirect response

#### Scenario: Missing state parameter
- **WHEN** no `state` parameter is provided
- **THEN** document whether the server enforces or recommends its use

### Requirement: Response Type and Grant Type Restriction Audit
The audit SHALL verify that only secure response types and grant types are supported per OAuth 2.1. The implicit grant (`response_type=token`) and Resource Owner Password Credentials grant are deprecated.

**References**: ASVS 10.4.4, 10.6.1, RFC 9700 §2.1.2, §2.4

#### Scenario: Implicit grant support
- **WHEN** the supported response types are reviewed
- **THEN** document whether `response_type=token` (implicit grant) is supported and flag as non-compliant with OAuth 2.1 if so (ASVS 10.4.4)

#### Scenario: Hybrid flow assessment
- **WHEN** `response_type=code id_token` is reviewed
- **THEN** document the security implications and whether tokens are exposed in the front-channel

#### Scenario: Response mode restriction
- **WHEN** the supported `response_mode` values are reviewed
- **THEN** verify the authorization server only allows `response_mode` values appropriate for each client's configuration (ASVS 10.6.1)

#### Scenario: ROPC grant availability
- **WHEN** the supported grant types are reviewed
- **THEN** verify that Resource Owner Password Credentials grant is not supported (RFC 9700 §2.4)

### Requirement: Mix-Up Attack Surface Audit
The audit SHALL assess the mix-up attack surface given that the deployment supports multiple authorization servers. Mix-up attacks exploit confusion between authorization server metadata when a client interacts with multiple servers.

**References**: ASVS 10.2.2, RFC 9700 §4.4

#### Scenario: Issuer identification in responses
- **WHEN** the authorization response and token response are reviewed
- **THEN** verify that the `iss` parameter is included to prevent mix-up attacks (per RFC 9207)

#### Scenario: Cross-server authorization code injection
- **WHEN** an authorization code from one authorization server is presented to another's token endpoint
- **THEN** verify the code is rejected due to server binding

### Requirement: OpenID Connect Discovery Audit
The audit SHALL verify that the `.well-known/openid-configuration` endpoint exposes accurate metadata and does not leak sensitive information.

#### Scenario: Discovery metadata accuracy
- **WHEN** the discovery endpoint response is reviewed
- **THEN** verify all advertised capabilities match actual implementation

#### Scenario: Sensitive information exposure
- **WHEN** the discovery endpoint response is reviewed
- **THEN** document any fields that could aid an attacker (internal URLs, debug info)

### Requirement: Clickjacking Protection Audit
The audit SHALL verify that the authorization and login pages are protected against clickjacking (UI redressing) attacks where an attacker frames the page in a transparent iframe.

**References**: RFC 9700 §4.16, OWASP WSTG-ATHZ-05

#### Scenario: X-Frame-Options header
- **WHEN** the HTTP response headers for the authorization/login endpoint are reviewed
- **THEN** verify that `X-Frame-Options: DENY` or `SAMEORIGIN` is set

#### Scenario: Content-Security-Policy frame-ancestors
- **WHEN** the HTTP response headers for the authorization/login endpoint are reviewed
- **THEN** verify that `Content-Security-Policy: frame-ancestors 'none'` or equivalent is set

### Requirement: Credential Leakage via Browser Mechanisms Audit
The audit SHALL verify that credentials (authorization codes, tokens) are not leaked through browser mechanisms such as Referer headers, browser history, or HTTP 307 redirects.

**References**: RFC 9700 §4.2, §4.3, §4.12

#### Scenario: Referer header leakage
- **WHEN** the authorization endpoint redirect responses are reviewed
- **THEN** verify that `Referrer-Policy` headers are set to prevent authorization codes from leaking to third-party resources loaded on the redirect target page

#### Scenario: Browser history exposure
- **WHEN** the authorization flow is reviewed
- **THEN** document whether authorization codes in URL query parameters persist in browser history and whether mitigation (e.g., `form_post` response mode) is available

#### Scenario: 307 redirect credential forwarding
- **WHEN** HTTP redirect responses from the authorization server are reviewed
- **THEN** verify that 302 (not 307/308) is used for redirects after credential submission, preventing POST body (containing passwords) from being forwarded to the redirect target (RFC 9700 §4.12)

### Requirement: Scope Upgrade Attack Audit
The audit SHALL verify that scopes cannot be escalated beyond what was originally authorized during any token exchange or refresh flow.

**References**: RFC 9700 §2.3

#### Scenario: Scope escalation during code exchange
- **WHEN** the token endpoint processes an authorization code exchange with a `scope` parameter broader than the original authorization
- **THEN** verify the server rejects the request or limits the issued token to the originally authorized scopes

#### Scenario: Scope escalation during refresh
- **WHEN** a refresh token request includes a broader `scope` than the original grant
- **THEN** verify the server rejects the request or limits the issued token to the original scopes

### Requirement: Consent Management Audit
The audit SHALL verify that user consent is properly managed for authorization grants.

**References**: ASVS 10.7.1, 10.7.2, 10.7.3

#### Scenario: User consent prompting
- **WHEN** the authorization flow is reviewed
- **THEN** document whether the user is prompted to consent to each authorization request and whether the scope/permissions being granted are clearly presented (ASVS 10.7.1, 10.7.2)

#### Scenario: Consent review and revocation
- **WHEN** the consent management capabilities are reviewed
- **THEN** document whether users can review and revoke previously granted consents (ASVS 10.7.3)

### Requirement: Host Header Poisoning Audit
The audit SHALL verify that the authorization server does not use the HTTP `Host` header to construct redirect URIs or issuer URLs in a way that an attacker could manipulate.

#### Scenario: Host header in redirect construction
- **WHEN** the authorization endpoint redirect logic is reviewed
- **THEN** verify that redirect URIs are constructed from registered client data, not from the incoming `Host` header

#### Scenario: Host header in issuer claim
- **WHEN** the JWT `iss` claim construction and OIDC discovery endpoint are reviewed
- **THEN** verify the issuer URL is derived from server configuration, not the `Host` header
