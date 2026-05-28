# SCIM Provisioning Security Audit Findings

## Summary

| Severity      | Count |
|---------------|-------|
| Critical      | 1     |
| High          | 2     |
| Medium        | 4     |
| Low           | 3     |
| Informational | 2     |
| **Total**     | **12**|

---

### Finding: SCIM-01 No Base URL Validation -- SSRF via SCIM Application Configuration
- **Severity**: Critical
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScimApplicationService.kt:122-131` (create) and `:155-157` (update)
- **Description**: The `baseUrl` field on SCIM application configurations is accepted and stored without any validation of scheme, host, or port. No allowlist/denylist enforcement. The `ScimClient` at `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/client/ScimClient.kt:178-186` constructs URLs via simple string concatenation in `buildUrl()` and passes directly to `URI.create()`. The `ScimApplicationRequest` DTO only has `@NotBlank` on `baseUrl`.
- **Risk**: SSRF. An attacker with API access can configure a SCIM application to point to internal services (cloud metadata endpoints, internal databases, admin panels). The server sends authenticated HTTP requests with Bearer tokens to those targets on every user/group provisioning event. The 5-second polling interval means requests are sent automatically and repeatedly.
- **Recommendation**: Validate URLs: HTTPS only, resolve hostname and reject private/reserved IP ranges, reject cloud metadata hostnames, consider domain allowlists, validate at connection time to defeat DNS rebinding.
- **References**: OWASP SSRF Prevention Cheat Sheet, CWE-918

---

### Finding: SCIM-02 TLS Certificate Validation Globally Disabled
- **Severity**: High
- **Location**: `web/src/main/resources/application.properties:12`
- **Description**: `quarkus.tls.trust-all=true` is in production configuration (not behind a dev profile). Affects all Quarkus-managed HTTP clients and may affect the default SSLContext. Outbound SCIM HTTPS connections may not verify TLS certificates.
- **Risk**: Man-in-the-middle attacks can intercept SCIM traffic, capturing Bearer tokens and user provisioning data.
- **Recommendation**: Remove from production config. Use `%dev.quarkus.tls.trust-all=true` for dev only. Configure dedicated TLS contexts with specific trust stores.
- **References**: OWASP Transport Layer Security Cheat Sheet, CWE-295

---

### Finding: SCIM-03 User Password Hash Potentially Persisted in Event Resource Data
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt:178-196`
- **Description**: When user events are created, the entire `User` object is serialized via `convertToMap(pair.left)` which calls `obj?.toString()`. Kotlin data classes generate `toString()` that includes ALL fields, including `password`. The password hash is stored in the `Event.resource` JSONB column.
- **Risk**: Password hashes stored in the event log. If serialization approach changes, or event data is accessed through other means, password hashes are exposed. Stored indefinitely without cleanup.
- **Recommendation**: Strip password field before serialization. Create a `toSafeMap()` method with explicit non-sensitive field selection. Override `toString()` on User to exclude password.
- **References**: OWASP Logging Cheat Sheet, CWE-532

---

### Finding: SCIM-04 No URL Path Traversal Protection in URL Construction
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/client/ScimClient.kt:178-186`
- **Description**: `buildUrl` constructs URLs via `"$base/$path/$resourceId"`. The `resourceId` comes from external SCIM server responses. A malicious downstream server could return a crafted `id` containing path traversal sequences (e.g., `../../admin/delete`), and that ID is stored in the database and reused in future PUT/PATCH/DELETE operations.
- **Risk**: A compromised downstream SCIM server could redirect requests to unintended endpoints via crafted resource IDs.
- **Recommendation**: URL-encode `resourceId` in `buildUrl()`. Validate constructed URL host matches original `baseUrl`. Validate resource IDs don't contain path separators or query strings.
- **References**: CWE-22, OWASP Path Traversal

---

### Finding: SCIM-05 SCIM Token Lifetime of 3600 Seconds with No Per-Request Generation Guarantee
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScimTokenService.kt:42-43`, `web/src/main/resources/application.properties:53`
- **Description**: Token lifetime is 3600 seconds (1 hour) while SCIM HTTP timeout is 30 seconds. Fresh tokens are generated per delivery (good), but the long lifetime means intercepted tokens are replayable for up to 1 hour.
- **Risk**: Leaked Bearer tokens have a wide replay window against downstream SCIM providers.
- **Recommendation**: Reduce lifetime to 60-120 seconds. Consider adding `jti` claim for one-time-use enforcement.
- **References**: RFC 9068, OWASP JWT Security

---

### Finding: SCIM-06 SCIM Filter Injection in Group Member Remove Operation
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/mappers/ScimGroupMapper.kt:97-108`
- **Description**: `mapToRemoveMemberPatch` interpolates `memberScimId` directly into a SCIM filter: `"members[value eq \"$memberScimId\"]"`. The ID originates from previous downstream SCIM server responses. A malicious server could return IDs containing SCIM filter syntax.
- **Risk**: Filter injection could cause remove-member PATCH to target unintended group members.
- **Recommendation**: Sanitize `memberScimId` by escaping special characters. Validate stored SCIM resource IDs conform to expected format. Consider using a SCIM filter builder library.
- **References**: RFC 7644 Section 3.5.2, CWE-943

---

### Finding: SCIM-07 No DNS Rebinding Protection
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/client/ScimClient.kt:59-61`
- **Description**: `ScimClient` uses `java.net.http.HttpClient` with standard DNS resolution. No IP pinning or re-validation against private ranges. Even with URL validation at config time, an attacker's domain could initially resolve to a public IP then change to an internal IP. The 5-second polling creates new connections with fresh DNS lookups.
- **Risk**: DNS rebinding bypasses URL validation. Combined with `quarkus.tls.trust-all=true`, a serious SSRF bypass vector.
- **Recommendation**: Implement connection-time IP validation. Use custom DNS resolver enforcing IP restrictions. Cache and compare DNS results.
- **References**: OWASP SSRF Prevention, CWE-350

---

### Finding: SCIM-08 SCIM Management API Has No Authentication or Authorization
- **Severity**: Low
- **Location**: `core/src/main/kotlin/com/revethq/auth/core/api/interfaces/ScimApplicationsApi.kt:47-141`, `web/src/main/kotlin/com/revethq/auth/web/scim/ScimApplicationResource.kt:38-133`
- **Description**: `/scim-applications` CRUD has no authentication annotations. Consistent with the broader pattern (covered in OWASP audit BFLA-01) but particularly concerning for SCIM due to SSRF risk.
- **Risk**: Unauthenticated attacker could create SCIM applications pointing to attacker-controlled servers or internal addresses.
- **Recommendation**: Add authentication/authorization. Restrict to administrator roles. Add audit logging for configuration changes.
- **References**: OWASP API Security - Broken Object Level Authorization, CWE-306

---

### Finding: SCIM-09 Delivery Retry Does Not Bound Concurrent In-Progress Deliveries
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/ScheduledScimEventProcessor.kt:110-143`
- **Description**: `pollAndProcess()` fetches all pending/retryable deliveries and processes them in parallel with unbounded coroutines. No concurrency limit. The `runBlocking` call doesn't await async results, creating fire-and-forget that may cause duplicate processing on next poll cycle (every 5 seconds).
- **Risk**: Thread pool/connection/memory exhaustion. Duplicate delivery processing.
- **Recommendation**: Add concurrency semaphore. Properly await async results. Skip already-in-progress deliveries in query.
- **References**: CWE-400

---

### Finding: SCIM-10 JWT Audience Set to baseUrl Which May Be Attacker-Controlled
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScimTokenService.kt:72`
- **Description**: SCIM JWT `aud` claim is set to `scimApplication.baseUrl ?: ""`. The empty string fallback for null baseUrl produces semantically invalid JWTs.
- **Risk**: Low direct risk since token is only sent to configured `baseUrl`. Empty audience is technically invalid.
- **Recommendation**: Validate baseUrl is not null before generating token. Ensure audience is always a valid URL.
- **References**: RFC 7519 Section 4.1.3

---

### Finding: SCIM-11 SCIM Payloads Do Not Include Password Data (Positive Finding)
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/mappers/ScimUserMapper.kt:97-124`
- **Description**: SCIM user mapper only maps `username`, `id`, `email`, and profile name fields. No password or credential data is included in outbound SCIM payloads.
- **Risk**: None. Positive finding.
- **Recommendation**: Add unit test asserting password fields are never included as a regression guard.
- **References**: RFC 7643 Section 9

---

### Finding: SCIM-12 Stub Event Processors Lack Security Documentation
- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/KafkaScimEventProcessor.kt`, `AmqpScimEventProcessor.kt`, `CdiScimEventProcessor.kt`
- **Description**: Three alternative processors exist as stubs (throw `UnsupportedOperationException`). None document security requirements for their respective message channels. `CdiScimEventProcessor` uses non-atomic `var running` instead of `AtomicBoolean`.
- **Risk**: No immediate risk (stubs). When implemented, missing security docs could lead to insecure deployments.
- **Recommendation**: Add security requirement comments. Fix `CdiScimEventProcessor.running` to use `AtomicBoolean`.
- **References**: OWASP Microservices Security Cheat Sheet
