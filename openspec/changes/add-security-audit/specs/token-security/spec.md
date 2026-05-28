## ADDED Requirements

### Requirement: JWT Signing Security Audit
The audit SHALL review JWT signing practices including algorithm selection, key strength, key rotation, and `kid` header usage.

#### Scenario: Signing algorithm review
- **WHEN** the JWT signing configuration is reviewed
- **THEN** verify RS256 (RSA-SHA256) is used and that weaker algorithms (none, HS256 with public key) are not accepted during validation

#### Scenario: RSA key strength
- **WHEN** the signing key generation code is reviewed (`AuthorizationServerService.kt`)
- **THEN** verify RSA-2048 minimum key length and proper `SecureRandom` usage

#### Scenario: Key ID header
- **WHEN** JWT tokens are generated
- **THEN** verify the `kid` header is set and matches a key in the JWKS endpoint

#### Scenario: Algorithm confusion attack
- **WHEN** the JWT validation code (`jose4j` JwtConsumerBuilder) is reviewed
- **THEN** verify the expected algorithm is explicitly set and symmetric/asymmetric confusion is prevented

### Requirement: Refresh Token Security Audit
The audit SHALL review refresh token generation, storage, rotation, and revocation practices.

**References**: ASVS 10.4.5, 10.4.8, 10.4.9, RFC 9700 §4.14

#### Scenario: Refresh token entropy
- **WHEN** the refresh token generation code is reviewed
- **THEN** verify sufficient entropy (minimum 128 bits) from a cryptographically secure random source

#### Scenario: Refresh token storage
- **WHEN** the refresh token persistence is reviewed
- **THEN** verify tokens are stored as hashes (not plaintext) and evaluate the hashing algorithm choice (SHA-256 vs bcrypt/argon2)

#### Scenario: Refresh token rotation
- **WHEN** a refresh token is used to obtain new tokens
- **THEN** verify the old token is immediately revoked and a new token is issued (rotation) per ASVS 10.4.5

#### Scenario: Refresh token reuse detection
- **WHEN** a revoked refresh token is presented
- **THEN** document whether the system detects reuse and revokes the entire token family (indicating compromise) per RFC 9700 §4.14

#### Scenario: Refresh token scope binding
- **WHEN** refresh tokens are reviewed
- **THEN** verify tokens are bound to specific scopes and cannot be used to escalate privileges

#### Scenario: Refresh token absolute expiration
- **WHEN** the refresh token lifecycle is reviewed
- **THEN** verify refresh tokens have an absolute expiration (not just sliding expiration) to limit the window of compromise (ASVS 10.4.8)

#### Scenario: Refresh token revocation capability
- **WHEN** the token revocation capabilities are reviewed
- **THEN** verify that refresh tokens can be revoked by an authorized user or administrator (ASVS 10.4.9)

### Requirement: Token Expiration Audit
The audit SHALL verify that token expiration is properly enforced for all token types.

#### Scenario: Access token expiration
- **WHEN** the access token generation and validation code is reviewed
- **THEN** verify expiration claims are set and enforced, and document the configurable expiration values

#### Scenario: Refresh token expiration
- **WHEN** the refresh token lifecycle is reviewed
- **THEN** verify refresh tokens have expiration and document whether absolute lifetime limits are enforced

#### Scenario: Authorization code expiration
- **WHEN** the authorization code lifecycle is reviewed
- **THEN** verify codes expire within a reasonable window (maximum 10 minutes at L1/L2, 1 minute at L3 per ASVS 10.4.3)

### Requirement: Token Claim Validation Audit
The audit SHALL verify that JWT claims are properly set during generation and validated during consumption.

**References**: ASVS 10.3.1, 10.3.2, 10.3.3

#### Scenario: Issuer claim
- **WHEN** JWT tokens are generated and validated
- **THEN** verify the `iss` claim is set correctly per authorization server and validated on consumption

#### Scenario: Audience claim
- **WHEN** JWT tokens are generated and validated
- **THEN** verify the `aud` claim is set and validated to prevent token misuse across audiences (ASVS 10.3.1)

#### Scenario: Subject claim
- **WHEN** JWT tokens are generated
- **THEN** verify the `sub` claim correctly identifies the authenticated entity and cannot be reassigned to other users (ASVS 10.3.3)

#### Scenario: Delegated authorization claims
- **WHEN** JWT access tokens are reviewed
- **THEN** verify that claims define the delegated authorization scope so resource servers can enforce authorization decisions (ASVS 10.3.2)

### Requirement: Token Leakage Prevention Audit
The audit SHALL check for token exposure through logs, error messages, URLs, and HTTP headers.

**References**: ASVS 10.1.1, RFC 9700 §4.2, §4.3, §4.9

#### Scenario: Tokens in server logs
- **WHEN** logging configuration and log statements are reviewed
- **THEN** document any instances where access tokens, refresh tokens, or authorization codes appear in logs

#### Scenario: Tokens in error responses
- **WHEN** error handling code is reviewed
- **THEN** verify that token values are not included in error messages returned to clients

#### Scenario: Tokens in URLs
- **WHEN** the authorization flow is reviewed
- **THEN** verify that tokens are not transmitted via URL query parameters (except authorization codes in redirects, which is expected)

#### Scenario: Token distribution minimization
- **WHEN** the token flow across components is reviewed
- **THEN** verify tokens are only sent to components that strictly need them (ASVS 10.1.1)

### Requirement: Sender-Constrained Token Audit
The audit SHALL assess whether the authorization server supports sender-constrained (Proof-of-Possession) tokens to prevent stolen token misuse.

**References**: ASVS 10.3.5, 10.4.14, RFC 9700 §4.10

#### Scenario: DPoP support assessment
- **WHEN** the token endpoint and resource server integration is reviewed
- **THEN** document whether DPoP (Demonstration of Proof-of-Possession, RFC 9449) is supported or feasible to add

#### Scenario: mTLS token binding assessment
- **WHEN** the TLS configuration and token endpoint is reviewed
- **THEN** document whether mutual TLS certificate-bound tokens (RFC 8705) are supported or feasible to add

#### Scenario: Stolen token misuse risk
- **WHEN** the current bearer token implementation is reviewed without sender-constraining
- **THEN** document the risk level of token theft and replay, and recommend mitigation strategy

### Requirement: Confidential Client Backchannel Authentication Audit
The audit SHALL verify that confidential clients are properly authenticated on backchannel requests to the authorization server (token endpoint, revocation endpoint).

**References**: ASVS 10.4.10, 10.4.16

#### Scenario: Token endpoint client authentication
- **WHEN** the token endpoint handling for confidential clients is reviewed
- **THEN** verify that client authentication is required and enforced for all backchannel token requests (ASVS 10.4.10)

#### Scenario: Client authentication method strength
- **WHEN** the supported client authentication methods are reviewed
- **THEN** document what methods are supported (client_secret_post, client_secret_basic, private_key_jwt, client_secret_jwt) and assess strength (ASVS 10.4.16 recommends mTLS or private_key_jwt at L3)
