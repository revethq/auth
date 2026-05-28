## ADDED Requirements

### Requirement: Injection Vulnerability Audit
The audit SHALL review all data input paths for SQL injection, HQL injection, template injection, and other injection vectors.

#### Scenario: HQL/JPQL injection via Panache
- **WHEN** all Panache repository queries are reviewed
- **THEN** verify parameterized queries are used and no string concatenation builds query predicates from user input

#### Scenario: Template injection via Qute
- **WHEN** the HTML template rendering code (login screens) is reviewed
- **THEN** verify user-supplied data is properly escaped and that template injection cannot execute arbitrary code

#### Scenario: JSON deserialization
- **WHEN** JSON-B deserialization of request bodies is reviewed
- **THEN** verify that unexpected fields are ignored and that deserialization does not trigger side effects

### Requirement: Broken Access Control Audit
The audit SHALL review all API endpoints for proper authorization enforcement.

**References**: OWASP API Security API2:2023 (Broken Authentication), API5:2023 (Broken Function Level Authorization)

#### Scenario: Admin API authentication
- **WHEN** the management/CRUD endpoints (authorization servers, users, clients, applications, etc.) are reviewed
- **THEN** document whether these endpoints require authentication and authorization, or are openly accessible

#### Scenario: Resource ownership validation
- **WHEN** CRUD operations on tenant-scoped resources are reviewed
- **THEN** verify that operations validate the requesting entity has access to the target authorization server's resources

#### Scenario: Horizontal privilege escalation
- **WHEN** API endpoints accepting resource IDs are reviewed
- **THEN** verify that users/applications cannot access resources belonging to other entities by guessing or manipulating IDs

### Requirement: Security Misconfiguration Audit
The audit SHALL review all security-related configuration for deviations from production-safe defaults.

#### Scenario: CORS configuration
- **WHEN** the CORS settings in `application.properties` are reviewed
- **THEN** document the current origin policy (`/.*/` — allows all origins) and assess the risk for credential-bearing requests

#### Scenario: TLS configuration
- **WHEN** the TLS settings are reviewed
- **THEN** document `quarkus.tls.trust-all=true` and assess the impact on SCIM outbound calls and any other HTTPS client connections

#### Scenario: TLS terminating reverse proxy
- **WHEN** the deployment architecture and TLS configuration is reviewed
- **THEN** document whether credentials could be exposed between a TLS-terminating reverse proxy and the application backend if communication is over plaintext HTTP (RFC 9700 §4.13)

#### Scenario: Swagger UI exposure
- **WHEN** the OpenAPI/Swagger UI configuration is reviewed
- **THEN** document whether Swagger UI is enabled in production builds and assess information disclosure risk

#### Scenario: Debug/development settings
- **WHEN** all `application.properties` files are reviewed
- **THEN** document any development-only settings that should be disabled in production (SQL logging, dev profiles, etc.)

#### Scenario: Security headers
- **WHEN** HTTP response headers across all endpoints are reviewed
- **THEN** document presence/absence of security headers: `Strict-Transport-Security`, `X-Content-Type-Options`, `Cache-Control` (for responses containing tokens), `Pragma`

### Requirement: Rate Limiting Audit
The audit SHALL assess rate limiting and brute-force protection across all authentication-related endpoints.

#### Scenario: Login endpoint rate limiting
- **WHEN** the authorization endpoint (POST) handling user credentials is reviewed
- **THEN** document whether rate limiting or account lockout is implemented to prevent brute-force attacks

#### Scenario: Token endpoint rate limiting
- **WHEN** the token endpoint is reviewed
- **THEN** document whether rate limiting is implemented to prevent credential stuffing against client credentials

#### Scenario: API endpoint rate limiting
- **WHEN** the management API endpoints are reviewed
- **THEN** document whether any rate limiting or throttling is in place

### Requirement: Error Handling and Information Leakage Audit
The audit SHALL review error responses for information disclosure that could aid attackers.

#### Scenario: Stack trace exposure
- **WHEN** error handling and exception mappers are reviewed
- **THEN** verify that stack traces and internal implementation details are not exposed in HTTP responses

#### Scenario: Authentication error messages
- **WHEN** login failure responses are reviewed
- **THEN** verify that error messages do not differentiate between "user not found" and "wrong password" (user enumeration)

#### Scenario: OAuth error responses
- **WHEN** OAuth error responses are reviewed
- **THEN** verify they comply with RFC 6749 Section 5.2 and do not leak additional information

### Requirement: SSRF Prevention Audit
The audit SHALL review all server-side HTTP requests for SSRF (Server-Side Request Forgery) vulnerabilities.

#### Scenario: SCIM outbound URL validation
- **WHEN** the SCIM application `baseUrl` configuration and HTTP client calls are reviewed
- **THEN** document whether URLs are validated to prevent requests to internal networks, localhost, or metadata endpoints

#### Scenario: Redirect URI as SSRF vector
- **WHEN** the redirect URI handling is reviewed
- **THEN** verify the server does not make server-side requests to redirect URIs (should only redirect the user-agent)
