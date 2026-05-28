# Change: Add Comprehensive Security Audit

## Why

Revet Auth is an OAuth 2.1 / OIDC authorization server handling sensitive authentication and authorization flows. While marked as educational, it should still demonstrate security best practices. A structured audit across OWASP, OAuth/OIDC-specific threats, Quarkus platform practices, and multi-tenancy isolation will identify vulnerabilities, misconfigurations, and deviations from standards before the codebase is used as a reference or extended further.

## What Changes

This is a **findings-only** audit phase. No code changes are produced — each area generates a documented set of findings with severity ratings and remediation recommendations. A follow-up proposal will implement prioritized fixes.

- **OAuth 2.1 / OIDC Protocol Compliance**: Audit authorization code flow, PKCE enforcement, redirect URI validation, state parameter, response types, and mix-up attack surface
- **Token Security**: Audit JWT signing, refresh token rotation, expiration enforcement, audience/issuer validation, and token storage
- **Credential & Secret Management**: Audit password hashing, client secret storage, default credentials, and key generation entropy
- **OWASP Web API Security**: Audit for injection, broken access control, security misconfiguration, rate limiting, error handling, and SSRF
- **Multi-Tenancy Isolation**: Audit cross-tenant data access, signing key scoping, and scope boundary enforcement
- **Quarkus & JVM Best Practices**: Audit dependency vulnerabilities, Hibernate ORM patterns, CDI scoping, native build config, and secrets management
- **SCIM Provisioning Security**: Audit outbound request authentication, URL validation (SSRF), and delivery retry security
- **Logging & Audit Trail**: Audit for sensitive data in logs, audit completeness, and event system integrity

## Impact

- Affected specs: none (new capabilities added)
- Affected code: all modules — `core/`, `persistence/`, `web/`
- Key files under audit:
  - `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt` — OAuth/OIDC endpoints
  - `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt` — token generation, signing, validation
  - `persistence/src/main/kotlin/com/revethq/auth/persistence/services/UserService.kt` — password handling
  - `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt` — client secret management
  - `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/` — SCIM provisioning
  - `web/src/main/resources/application.properties` — CORS, TLS, SCIM config
  - `persistence/src/main/resources/application.properties` — database config, default credentials

## Approach

Each of the 8 audit areas is an independent capability spec with its own task list. Agents can work on any area in parallel — there are no ordering dependencies between areas.

### Finding Format

Each finding SHALL be documented as:

```
### Finding: [SHORT-ID] [Title]
- **Severity**: Critical / High / Medium / Low / Informational
- **Location**: file:line or configuration reference
- **Description**: What was found
- **Risk**: What could be exploited and impact
- **Recommendation**: How to remediate
- **References**: Relevant RFC, OWASP, or CVE links
```

### Severity Definitions

| Severity | Criteria |
|----------|----------|
| Critical | Exploitable vulnerability allowing unauthorized access, data breach, or full system compromise |
| High | Significant vulnerability requiring specific conditions to exploit, or missing critical security control |
| Medium | Security weakness that increases attack surface or deviates from best practices with moderate risk |
| Low | Minor security concern with limited exploitability or impact |
| Informational | Observation or best-practice recommendation with no direct security risk |
