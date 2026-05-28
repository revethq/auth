## ADDED Requirements

### Requirement: SCIM Outbound Authentication Audit
The audit SHALL review how the SCIM client authenticates to downstream SCIM providers.

#### Scenario: JWT token generation for SCIM
- **WHEN** the SCIM client's authentication mechanism is reviewed
- **THEN** verify that JWT tokens generated for SCIM calls use appropriate scopes, audience, and expiration

#### Scenario: Bearer token transmission
- **WHEN** the SCIM HTTP client is reviewed
- **THEN** verify tokens are transmitted via Authorization header over HTTPS and not exposed in URLs or logs

#### Scenario: Token lifetime
- **WHEN** the SCIM token configuration (`revet.scim.token.lifetime=3600`) is reviewed
- **THEN** assess whether the token lifetime is appropriate and whether tokens are cached or regenerated per request

### Requirement: SCIM URL Validation Audit
The audit SHALL review SCIM application URL handling for SSRF and URL manipulation vulnerabilities.

#### Scenario: Base URL validation
- **WHEN** SCIM application creation/update is reviewed
- **THEN** verify that `baseUrl` is validated (scheme, host, port) and internal/private network addresses are rejected

#### Scenario: URL construction safety
- **WHEN** SCIM endpoint URL construction (base URL + resource paths) is reviewed
- **THEN** verify that path traversal or injection in resource paths cannot alter the target URL

#### Scenario: DNS rebinding
- **WHEN** the SCIM HTTP client connection process is reviewed
- **THEN** document whether DNS resolution results are cached and whether DNS rebinding could redirect SCIM calls to internal hosts

### Requirement: SCIM Delivery Security Audit
The audit SHALL review the SCIM event delivery pipeline for data exposure and integrity concerns.

#### Scenario: Sensitive data in SCIM payloads
- **WHEN** the SCIM resource mapping and payload construction is reviewed
- **THEN** verify that sensitive fields (passwords, secrets) are not included in outbound SCIM payloads

#### Scenario: Retry payload safety
- **WHEN** the retry mechanism for failed SCIM deliveries is reviewed
- **THEN** verify that retry payloads do not accumulate or expose credentials, and that failed deliveries are properly bounded

#### Scenario: Event processor isolation
- **WHEN** the various SCIM event processors (scheduled, CDI, Kafka, AMQP) are reviewed
- **THEN** document the security characteristics of each processor type and whether message channels are authenticated
