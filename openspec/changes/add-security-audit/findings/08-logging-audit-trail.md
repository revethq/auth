# Logging & Audit Trail - Security Audit Findings

**Audit Date**: 2026-05-21
**Auditor**: Claude Opus 4.6 (Automated Security Audit)
**Scope**: Application logging practices, audit event system, sensitive data handling in logs and events

## Summary

| Severity       | Count |
|----------------|-------|
| Critical       | 1     |
| High           | 4     |
| Medium         | 5     |
| Low            | 3     |
| Informational  | 3     |
| **Total**      | **16** |

---

### Finding: LAT-001 Username Logged on Failed Authentication
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:103,108`
- **Description**: When a user provides invalid credentials, the username is logged at ERROR level via `LOG.error("User not found: $username")`. This occurs on both lines 103 and 108. Usernames are personally identifiable information and logging them in cleartext can expose user identities in log aggregation systems.
- **Risk**: If logs are accessed by unauthorized parties (log injection, exposed log aggregation, shared log infrastructure), attacker can enumerate valid usernames by correlating login attempts. In environments subject to GDPR or similar regulations, logging PII without controls is a compliance violation.
- **Recommendation**: Replace the username in log messages with a hashed or truncated form, or log only the authorization server ID and a generic failure message. Log at WARN/INFO level instead of ERROR. Include source IP for security monitoring.
- **References**: OWASP Logging Cheat Sheet, CWE-532

---

### Finding: LAT-002 Authorization Code Stored in Event System via toString()
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt:118-131`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:106`
- **Description**: When a client code (authorization code) is created, `eventRepository.createClientCodeEvent(mutableClientCode, EventType.CREATE)` is called. The `createClientCodeEvent` method calls `convertToMap(clientCode)` which calls `obj?.toString()` on the `ClientCode` data class. Since `ClientCode` is a Kotlin data class, `toString()` outputs all fields including the `code` field (the raw authorization code), `codeChallenge`, `nonce`, and `state`. This data is persisted in the `resource` JSON column of the `Event` database table.
- **Risk**: The authorization code is stored in plaintext in the events table. If the events table is accessed by an attacker (SQL injection, database breach, or backup compromise), they can retrieve valid authorization codes and exchange them for access tokens.
- **Recommendation**: Exclude sensitive fields (`code`, `codeChallenge`, `codeChallengeMethod`) from event serialization. Either override `toString()` on `ClientCode` to exclude these fields, or use a dedicated event serialization method that explicitly selects only safe fields (e.g., `id`, `clientId`, `authorizationServerId`, `createdOn`).
- **References**: RFC 6749 Section 4.1.2, OWASP Logging Cheat Sheet

---

### Finding: LAT-003 SigningKey Private Key Potentially Exposed via toString() in Events
- **Severity**: Critical
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt:87-101`
- **Description**: The `createSigningKeyEvent` method calls `convertToMap(signingKey)` which invokes `signingKey.toString()`. The `SigningKey` data class has a `privateKey` field. While the code on line 94-95 does `resource.remove("privateKey")`, the `convertToMap` function stores everything under a `"data"` key as a single `toString()` string, not as individual map keys. The `resource.remove("privateKey")` call removes the key `"privateKey"` from the top-level map returned by `convertToMap`, but the private key is already embedded within the `"data"` string value (which is the full `toString()` output). The removal has no effect on the actual data stored.
- **Risk**: RSA private signing keys are stored in plaintext in the events table's JSON `resource` column. If the events table or database backups are compromised, an attacker gains the ability to forge any JWT token issued by this authorization server, effectively bypassing all authentication and authorization.
- **Recommendation**: The `convertToMap` function must be rewritten to perform proper field-level serialization rather than using `toString()`. Sensitive fields like `privateKey` must be excluded before serialization. Alternatively, do not store signing key material in events at all -- only store the key ID, key type, and public key.
- **References**: OWASP Cryptographic Storage Cheat Sheet, RFC 7517 Section 4

---

### Finding: LAT-004 User Data Stored in Events via Unsafe toString() Serialization
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt:178-197`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/UserService.kt:129`
- **Description**: The `createUserProfileEvent` method calls `convertToMap(pair.left)` where `pair.left` is a `User` domain object. The `convertToMap` function calls `toString()` on the entire User object. The `toString()` serialization pattern is inherently unsafe because any future field additions to the User class will automatically be included in event storage without review. Additionally, the profile data (`pair.right?.profile`) is also stored -- profile data may contain PII (email, name, address).
- **Risk**: PII and potentially sensitive user metadata are stored in the events table without field-level filtering. Any future field additions to domain objects automatically leak into events.
- **Recommendation**: Replace `convertToMap` with explicit field selection for all domain objects. Create allow-lists of fields that are safe to store in audit events.
- **References**: CWE-532, OWASP Logging Cheat Sheet

---

### Finding: LAT-005 Debug Logging Left in Production Code
- **Severity**: Low
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:159`
- **Description**: The line `LOG.error("We are about to call into the client service now")` is clearly a debug statement left from development that is logged at ERROR level. This pollutes production error logs with non-error information.
- **Risk**: Log noise reduces the signal-to-noise ratio of production logs, making incident response slower. Using ERROR level for debug messages may trigger false alerts in monitoring systems.
- **Recommendation**: Remove this debug log statement entirely, or change it to `LOG.debug()` or `LOG.trace()` with a meaningful message.
- **References**: OWASP Logging Cheat Sheet

---

### Finding: LAT-006 Debug Logging Left in ClientService
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:62`
- **Description**: The line `LOG.error("We are going to try and create the client code now.")` is another debug statement logged at ERROR level. The logger is also instantiated locally inside the method instead of using a companion object pattern.
- **Risk**: Same as LAT-005: log noise and false alerting.
- **Recommendation**: Remove this debug log statement. If logging is needed for this operation, use `LOG.debug()` at the class companion object level.
- **References**: OWASP Logging Cheat Sheet

---

### Finding: LAT-007 println Used in Validator (Bypasses Logging Framework)
- **Severity**: Medium
- **Location**: `core/src/main/kotlin/com/revethq/auth/core/authorization/validators/ValidAuthorizationServer.kt:70`
- **Description**: The validator uses `println("An unexpected error occurred while validating the Authorization Server ID of $value: ${e.message}")` to output error information. This bypasses the logging framework (no log level, no structured logging, no log rotation or filtering). Additionally, the error is silently swallowed -- the method returns `true` after catching the exception, meaning validation passes even when an unexpected error occurs.
- **Risk**: Output to stdout/println is not captured by standard log management tools. The silent swallowing of exceptions means an unexpected database error could cause the validator to approve invalid authorization server IDs.
- **Recommendation**: Replace `println` with proper logger usage (`LOG.error()`). Fix the logic bug: the method should return `false` on unexpected exceptions, not `true`.
- **References**: CWE-778 (Insufficient Logging), OWASP Logging Cheat Sheet

---

### Finding: LAT-008 System.err.println Used in ClientCodeMapper
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/ClientCodeMapper.kt:38`
- **Description**: The `ClientCodeMapper.from` method uses `System.err.println("Failed to load scope with ID: $scopeId - ${e.message}")` to log scope loading failures. This bypasses the logging framework and outputs directly to stderr.
- **Risk**: Error output is not captured by structured logging infrastructure. The error is silently swallowed, potentially hiding data integrity issues.
- **Recommendation**: Replace `System.err.println` with proper structured logging via `LOG.warn()` or `LOG.error()`.
- **References**: CWE-778 (Insufficient Logging)

---

### Finding: LAT-009 No Login Attempt Audit Trail
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:80-183`
- **Description**: The `createAuthorizationCode` method handles the entire login flow -- receiving username/password, validating credentials, and issuing authorization codes. However, there is no audit event created for login attempts (successful or failed). The only logging is ad-hoc `LOG.error()` calls. Specifically:
  - **Failed logins**: No event recorded when a user provides wrong credentials (line 122), when a user is not found (lines 103, 108), or when authorization server mismatch occurs (line 112).
  - **Successful logins**: No event recorded when a user successfully authenticates and an authorization code is issued (line 160-183).
  - **No source IP or client identification**: None of the log messages include the source IP address, user agent, or client ID.
- **Risk**: Without login audit trails, it is impossible to detect brute force attacks, credential stuffing, account compromise, or perform forensic analysis after a security incident.
- **Recommendation**: Create new `EventType` values for authentication events (e.g., `LOGIN_SUCCESS`, `LOGIN_FAILURE`). Record events for every authentication attempt with: timestamp, username (hashed), authorization server ID, client ID, source IP, user agent, and failure reason.
- **References**: OWASP Authentication Cheat Sheet, NIST SP 800-53 AU-2, RFC 6749 Section 10.13

---

### Finding: LAT-010 No Token Grant Audit Trail
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:374-491`
- **Description**: The `postToken` method handles all token grant types (client_credentials, authorization_code, refresh_token) but creates no audit events for any token grant operation. Token issuance is a critical security event that should be tracked.
- **Risk**: Without token grant auditing, there is no way to trace which tokens were issued, to whom, when, or with what scopes. This makes it impossible to investigate token misuse or detect anomalous token issuance patterns.
- **Recommendation**: Create audit events for each token grant with: grant type, client ID, subject, scopes granted, authorization server ID, timestamp, and token expiration. Do not log the token value itself.
- **References**: OWASP Logging Cheat Sheet, RFC 6749 Section 10

---

### Finding: LAT-011 No Token Revocation Audit Trail and Missing Revocation Endpoint
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:248-251,456-458`
- **Description**: Token revocation occurs in two places: (1) during token rotation in `refreshAccessToken` where the old refresh token is revoked, and (2) via `revokeAllUserRefreshTokens`. Neither operation creates an audit event. Additionally, the `.well-known` configuration advertises a `revocationEndpoint` but no actual revocation endpoint implementation exists.
- **Risk**: Without revocation auditing, there is no record of when or why tokens were revoked. The advertised-but-missing revocation endpoint may mislead clients.
- **Recommendation**: (1) Implement the revocation endpoint per RFC 7009. (2) Create audit events for all token revocation operations.
- **References**: RFC 7009, OWASP Logging Cheat Sheet

---

### Finding: LAT-012 SQL Logging Configuration Risk
- **Severity**: Informational
- **Location**: `persistence/src/main/resources/application.properties:10`
- **Description**: The Hibernate SQL logging is currently commented out: `#quarkus.hibernate-orm.log.sql=${CB_AUTH_LOG_SQL:true}`. The default value in the environment variable fallback is `true`, meaning if someone uncomments this line, all SQL queries will be logged, potentially including sensitive data.
- **Risk**: If SQL logging is enabled, queries containing sensitive data may appear in logs.
- **Recommendation**: Change the default value to `false`: `quarkus.hibernate-orm.log.sql=${CB_AUTH_LOG_SQL:false}`. Add a comment warning about the sensitivity of SQL log output.
- **References**: OWASP Logging Cheat Sheet, CWE-532

---

### Finding: LAT-013 Event System Lacks Immutability Guarantees
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/Event.kt:13-26`, `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt:47`
- **Description**: The `Event` entity is a standard JPA entity extending `PanacheRepositoryBase<Event, UUID>` which provides full CRUD operations including `delete()`, `update()`, and `persist()` (for updates). There are no database-level constraints preventing modification or deletion of events.
- **Risk**: An attacker who gains code execution or a malicious insider could delete or modify audit events to cover their tracks. The events table can be truncated via the repository's inherited `deleteAll()` method.
- **Recommendation**: (1) Make the Event entity append-only at the database level using PostgreSQL rules or triggers. (2) Create a read-only repository interface for events. (3) Consider using a separate database user with INSERT-only permissions for the events table.
- **References**: NIST SP 800-92, CWE-779

---

### Finding: LAT-014 Event System Uses toString() Serialization -- Unreliable and Potentially Sensitive
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt:264-271`
- **Description**: The `convertToMap` function used by all event creation methods stores event data as `obj?.toString()`. This approach has multiple problems: (1) `toString()` output is not a stable serialization format. (2) For Kotlin data classes, `toString()` includes all fields, including sensitive ones. (3) The resulting event data is not queryable or parseable. (4) Different domain objects may have inconsistent formats.
- **Risk**: Audit data stored via `toString()` cannot be reliably queried for forensic analysis. Sensitive fields from any domain object are automatically included without review.
- **Recommendation**: Replace `convertToMap` with proper JSON serialization using explicit field selection. Define a serialization allow-list for each domain type. Use a proper serialization library with `@JsonIgnore` annotations on sensitive fields.
- **References**: CWE-532, OWASP Logging Cheat Sheet

---

### Finding: LAT-015 No Administrative Action Audit Differentiation
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt` (entire file)
- **Description**: The event system tracks CRUD operations on most resources, but the `EventType` enum only has three values: `CREATE`, `UPDATE`, `DELETE`. There is no distinction between administrative actions and system-generated actions. Events also lack: (1) who performed the action (no actor/principal field), (2) the source of the action (no IP address, no request context), (3) whether the action was performed via API or internally.
- **Risk**: Without actor identification, it is impossible to determine who performed administrative changes. This significantly limits the forensic value of the audit trail.
- **Recommendation**: (1) Add `actorId`, `actorType`, `sourceIp`, and `requestId` fields to the `Event` entity. (2) Expand `EventType` to include security-relevant event types. (3) Propagate request context through to event creation methods.
- **References**: NIST SP 800-53 AU-3, OWASP Logging Cheat Sheet

---

### Finding: LAT-016 Event Persistence Not Guaranteed on Failure
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:166-191`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:307-332`
- **Description**: In `ClientService.createClient`, event creation happens within manually managed transactions (`QuarkusTransaction.begin/commit`). The event is created in a separate transaction from the client creation, meaning if the second transaction fails, the client is created but the audit event is lost.
- **Risk**: Audit events can be silently dropped if the second transaction fails, creating gaps in the audit trail.
- **Recommendation**: Ensure all audit events are created within the same transaction as the action they record. Alternatively, use an outbox pattern where events are written to a local outbox table in the same transaction and asynchronously processed.
- **References**: NIST SP 800-92

---

## Missing Security-Relevant Event Types

The following security-relevant actions are **not tracked** by the event system:

| Action | Status |
|--------|--------|
| Successful login | NOT TRACKED |
| Failed login | NOT TRACKED |
| Token issuance (all grant types) | NOT TRACKED |
| Token revocation | NOT TRACKED |
| Token refresh | NOT TRACKED |
| Password change/set | NOT TRACKED |
| Application secret creation | NOT TRACKED |
| Application secret deletion | NOT TRACKED |
| SCIM outbound provisioning events | NOT TRACKED |
| JWT validation failures | NOT TRACKED |
| Authorization code exchange | NOT TRACKED |

## Tracked Event Types (via EventRepository)

| Resource Type | CREATE | UPDATE | DELETE |
|---------------|--------|--------|--------|
| AUTHORIZATION_SERVER | Yes | Yes | Yes |
| CLIENT | Yes | Yes | Yes |
| CLIENT_CODE | Yes | No | No |
| USER (with Profile) | Yes | Yes | Yes |
| APPLICATION (with Profile) | Yes | No | Yes |
| SCOPE | Yes | No | Yes |
| SCHEMA | Yes | No | Yes |
| TEMPLATE | Yes | No | Yes |
| SIGNING_KEY | Yes | No | No |
| GROUP | Yes | Yes | Yes |
| GROUP_MEMBER | Yes | No | Yes |
