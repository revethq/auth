# OWASP Web API Security Audit Findings

**Audit Date**: 2026-05-21
**Scope**: OWASP Web API Security review of OAuth 2.1 / OIDC authorization server
**Spec**: `openspec/changes/add-security-audit/specs/owasp-web-api/spec.md`

## Summary

| Severity      | Count |
|---------------|-------|
| Critical      | 2     |
| High          | 4     |
| Medium        | 5     |
| Low           | 3     |
| Informational | 3     |
| **Total**     | **17**|

---

## Injection Vulnerability Audit

### Finding: INJ-01 Qute Template Injection via Admin-Controlled Templates
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServerRoutes.kt:75`
- **Description**: The login screen rendering decodes a Base64-encoded template from the database and passes it directly to `Qute.fmt()` for rendering. The template content is stored by administrators via the management API (the `Templates` CRUD endpoint). `Qute.fmt()` interprets Qute template expressions (e.g., `{#eval}`, `{inject:bean.method()}`). Since the management API has no authentication (see BFLA-01), anyone can create or update a template containing malicious Qute expressions.
  ```kotlin
  var quteTemplate = Qute.fmt(String(Base64.getDecoder().decode(template.template)))
  quteTemplate = quteTemplate.data("error", errorMessage)
  return Response.ok().entity(quteTemplate.render()).build()
  ```
- **Risk**: If an attacker can modify the login template (trivial given no auth on the management API), they can inject arbitrary Qute expressions. Depending on the Qute configuration and available beans, this could lead to server-side code execution, information disclosure, or denial of service. The `errorMessage` parameter is passed via `.data("error", errorMessage)` which is properly parameterized, but the template body itself is the attack surface.
- **Recommendation**: (1) Authenticate and authorize the Templates management API. (2) Sanitize or validate template content to ensure only safe Qute constructs are used -- consider using a strict allowlist of Qute features. (3) Consider using `Qute.fmt()` with `contentType(ContentType.TEXT_HTML)` and ensuring Qute's built-in HTML escaping is active.
- **References**: OWASP Server-Side Template Injection (SSTI), CWE-94 (Improper Control of Generation of Code)

### Finding: INJ-02 HQL/JPQL Injection -- All Repositories Use Parameterized Queries (No Issue Found)
- **Severity**: Informational
- **Location**: All 19 repository files in `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/`
- **Description**: All Panache repository queries across the codebase use positional parameter binding (`?1`, `?2`, etc.) rather than string concatenation. No string concatenation of user input into query predicates was found.
- **Risk**: None. This is a positive finding.
- **Recommendation**: No changes required. Maintain this pattern.
- **References**: OWASP Injection Prevention Cheat Sheet, CWE-89

### Finding: INJ-03 JSON Deserialization -- Default JSON-B Behavior (Low Risk)
- **Severity**: Informational
- **Location**: All API interfaces in `core/src/main/kotlin/com/revethq/auth/core/api/interfaces/` using `@Consumes(MediaType.APPLICATION_JSON)`
- **Description**: The application uses Quarkus's default JSON-B for request deserialization. By default, JSON-B ignores unknown properties during deserialization, which is safe. No custom JSON-B configuration enabling polymorphic deserialization or risky behaviors was found.
- **Risk**: Low. The default behavior is safe.
- **Recommendation**: No immediate changes. Consider adding explicit `@JsonbProperty` annotations to DTOs as defensive measure.
- **References**: CWE-502 (Deserialization of Untrusted Data)

---

## Broken Access Control Audit

### Finding: BFLA-01 All Management API Endpoints Lack Authentication and Authorization
- **Severity**: Critical
- **Location**: All route files in `web/src/main/kotlin/com/revethq/auth/web/api/routes/` and `web/src/main/kotlin/com/revethq/auth/web/scim/ScimApplicationResource.kt`; all API interfaces in `core/src/main/kotlin/com/revethq/auth/core/api/interfaces/`
- **Description**: A comprehensive search for security annotations (`@RolesAllowed`, `@Authenticated`, `@PermitAll`, `@DenyAll`, `SecurityContext`, `@SecurityRequirement`) across the entire codebase returned zero matches. No Quarkus security configuration (`quarkus.security.*`, `quarkus.http.auth.*`, `quarkus.oidc.*`) was found in any `application.properties` file. This means every single management API endpoint is completely unauthenticated and unauthorized:
  - `POST/GET/PUT/DELETE /authorizationServers`
  - `POST/GET/PUT/DELETE /users` (including password management)
  - `POST/GET/PUT/DELETE /clients`
  - `POST/GET/DELETE /applications`
  - `POST/GET/DELETE /applicationSecrets` (credentials)
  - `POST/GET/PUT/DELETE /groups`
  - `POST/GET/DELETE /groups/{groupId}/members`
  - `POST/GET/PUT/DELETE /schemas`
  - `POST/GET/PUT/DELETE /scopes`
  - `POST/GET/PUT/DELETE /templates`
  - `POST/GET/PUT/DELETE /scim-applications`
- **Risk**: Any unauthenticated attacker with network access can perform full CRUD operations on all resources. This enables: (1) Creating rogue authorization servers and users, (2) Exfiltrating all user data, client secrets, and application secrets, (3) Modifying OAuth clients' redirect URIs to redirect authorization codes to attacker-controlled servers, (4) Deleting all configurations (denial of service), (5) Modifying login templates for credential theft, (6) Creating SCIM applications pointing to attacker-controlled servers for data exfiltration.
- **Recommendation**: Implement authentication and authorization on all management API endpoints: (1) Add `@Authenticated` or `@RolesAllowed("admin")` annotations, (2) Configure Quarkus OIDC or HTTP auth, (3) Consider a separate management API port.
- **References**: OWASP API2:2023 (Broken Authentication), OWASP API5:2023 (Broken Function Level Authorization), CWE-306 (Missing Authentication for Critical Function)

### Finding: BFLA-02 No Resource Ownership Validation on CRUD Operations
- **Severity**: Critical
- **Location**: All route files in `web/src/main/kotlin/com/revethq/auth/web/api/routes/`
- **Description**: CRUD operations on tenant-scoped resources do not validate that the requesting entity has access to the target resource's authorization server. The `list*` endpoints accept optional `authorizationServerIds` filter parameters, but these are optional -- omitting them returns all resources across all tenants.
- **Risk**: Even with authentication, without ownership validation any authenticated user could access, modify, or delete resources belonging to other tenants/authorization servers (horizontal privilege escalation).
- **Recommendation**: (1) Enforce mandatory authorization server context on all operations, (2) Validate that the authenticated principal has access to the target resource's authorization server, (3) Make `authorizationServerIds` required on list endpoints or derive from authenticated context.
- **References**: OWASP API1:2023 (Broken Object Level Authorization), CWE-639 (Authorization Bypass Through User-Controlled Key)

---

## Security Misconfiguration Audit

### Finding: CORS-01 Wildcard CORS Origin with Credentials Enabled
- **Severity**: High
- **Location**: `web/src/main/resources/application.properties:7-10`
- **Description**: CORS configuration allows any origin (`/.*/`) with credentials enabled (`access-control-allow-credentials=true`). Any website can make authenticated cross-origin requests. Also, `DELETE` method is missing from `quarkus.http.cors.methods` while DELETE endpoints exist.
- **Risk**: Cross-origin data theft; any website can make API calls reading responses with any credentials sent along.
- **Recommendation**: Replace `/.*/` with specific allowed origins. If API is same-origin only, disable CORS.
- **References**: OWASP API8:2023 (Security Misconfiguration), CWE-942, RFC 6749 Section 10.12

### Finding: TLS-01 TLS Certificate Verification Disabled Globally
- **Severity**: High
- **Location**: `web/src/main/resources/application.properties:12`
- **Description**: `quarkus.tls.trust-all=true` disables TLS certificate verification for all outbound HTTPS connections, directly affecting the SCIM client and any other HTTP client.
- **Risk**: Man-in-the-middle attacks can intercept, read, and modify outbound SCIM provisioning requests including Bearer tokens and user/group data.
- **Recommendation**: Remove for production. Use `%dev.quarkus.tls.trust-all=true` for dev only. Configure custom truststores for specific self-signed endpoints.
- **References**: CWE-295 (Improper Certificate Validation), OWASP API8:2023

### Finding: TLS-02 TLS-Terminating Reverse Proxy Credential Exposure Risk
- **Severity**: Medium
- **Location**: `web/src/main/resources/application.properties:1`, `Dockerfile:16`
- **Description**: The application listens on port 5000 over plaintext HTTP. No application-level TLS is configured. In typical deployments with TLS-terminating reverse proxies, credentials travel in cleartext between the proxy and the application.
- **Risk**: OAuth credentials (passwords, client secrets, tokens) may be exposed on internal network segments.
- **Recommendation**: Enable application-level TLS, or ensure proxy-to-app communication is over a secure channel. Document deployment security requirements.
- **References**: RFC 9700 Section 4.13, CWE-319

### Finding: SWAGGER-01 Swagger UI Enabled in Production
- **Severity**: Medium
- **Location**: `web/src/main/resources/application.properties:15`
- **Description**: `quarkus.swagger-ui.always-include=true` exposes the full OpenAPI specification and interactive Swagger UI in production at `/q/swagger-ui/`.
- **Risk**: Attackers can discover and map all API endpoints and data models. Combined with no authentication, provides a complete interactive attack surface.
- **Recommendation**: Remove `quarkus.swagger-ui.always-include=true`; use `%dev.quarkus.swagger-ui.always-include=true` instead.
- **References**: OWASP API8:2023, CWE-200

### Finding: DEV-01 Development Settings in Production Configuration
- **Severity**: Low
- **Location**: `persistence/src/main/resources/application.properties:3,7,9`
- **Description**: Several development-oriented settings: (1) Default password `notsecure`, (2) `quarkus.hibernate-orm.schema-management.strategy=update` auto-modifies production schema, (3) `quarkus.live-reload.instrumentation=true`.
- **Risk**: Insecure defaults, potential data corruption, performance impact.
- **Recommendation**: Use `%dev.` profile prefixes. Set schema strategy to `none` or `validate` for production. Remove insecure default credentials.
- **References**: OWASP API8:2023, CWE-1188

### Finding: HDR-01 Missing Security Response Headers
- **Severity**: Medium
- **Location**: `web/src/main/resources/application.properties` (missing configuration)
- **Description**: No security response headers are configured: `Strict-Transport-Security`, `X-Content-Type-Options`, `Cache-Control: no-store` on token responses, `Pragma: no-cache`, `X-Frame-Options`, `Content-Security-Policy`. No `quarkus.http.header.*` configuration or `ContainerResponseFilter` implementations were found.
- **Risk**: Token response caching, clickjacking on login form, MIME-sniffing attacks.
- **Recommendation**: Configure security headers via `quarkus.http.header.*` properties and add a `ContainerResponseFilter` for token-specific `Cache-Control: no-store`.
- **References**: OWASP Secure Headers Project, RFC 6749 Section 5.1, RFC 9700 Section 4.16

---

## Rate Limiting Audit

### Finding: RATE-01 No Rate Limiting on Login Endpoint
- **Severity**: High
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:80-184`
- **Description**: The authorization endpoint POST accepts credentials with no rate limiting, account lockout, or brute-force protection. Each attempt directly queries the database and performs bcrypt comparison without throttling.
- **Risk**: Unlimited brute-force attacks against user passwords.
- **Recommendation**: Implement rate limiting, progressive delays, temporary account lockout, and CAPTCHA after repeated failures.
- **References**: OWASP API4:2023, CWE-307, NIST SP 800-63B Section 5.2.2

### Finding: RATE-02 No Rate Limiting on Token Endpoint
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:374-491`
- **Description**: The token endpoint has no rate limiting, allowing unlimited client secret brute-force attempts via the `client_credentials` grant type.
- **Risk**: Client secret brute-force attacks.
- **Recommendation**: Implement per-client_id rate limiting and temporary lockout.
- **References**: OWASP API4:2023, CWE-307

### Finding: RATE-03 No Rate Limiting on Management API Endpoints
- **Severity**: Low
- **Location**: All route files in `web/src/main/kotlin/com/revethq/auth/web/api/routes/`
- **Description**: No rate limiting or throttling on any management API endpoint.
- **Risk**: Denial of service, rapid enumeration, database exhaustion. Amplified by lack of authentication.
- **Recommendation**: Implement API rate limiting via Quarkus extensions or reverse proxy.
- **References**: OWASP API4:2023

---

## Error Handling and Information Leakage Audit

### Finding: ERR-01 IllegalArgumentException Mapper Exposes Internal Error Messages
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/api/exceptions/IllegalArgumentExceptionMapper.kt:9-11`
- **Description**: Returns raw `exception.message` directly in HTTP response body, potentially exposing internal details.
- **Risk**: Internal implementation details exposed to attackers for reconnaissance.
- **Recommendation**: Return generic error messages; log full exception details server-side.
- **References**: CWE-209

### Finding: ERR-02 Missing Generic Exception Mapper
- **Severity**: Low
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/api/exceptions/` (only 2 mappers)
- **Description**: Only `IllegalArgumentExceptionMapper` and `ScimApplicationNotFoundExceptionMapper` exist. No catch-all mapper for `Exception` or `RuntimeException`. Unhandled exceptions may expose stack traces.
- **Risk**: Stack traces and internal details from unhandled exceptions could reach clients.
- **Recommendation**: Implement a generic `ExceptionMapper<Exception>` returning sanitized responses.
- **References**: CWE-209, OWASP API8:2023

### Finding: ERR-03 Login Error Messages -- Properly Non-Enumerating
- **Severity**: Informational
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:100-123`
- **Description**: Login failure consistently returns `"Invalid username or password"` for all cases (user not found, wrong server, wrong password). However, there is a minor timing side-channel: when user is not found, bcrypt comparison is skipped.
- **Risk**: Low. User-facing messages are correct. Timing side-channel possible but hard to exploit.
- **Recommendation**: Perform dummy bcrypt comparison when user not found to eliminate timing difference.
- **References**: CWE-204

### Finding: ERR-04 Username Logged in Error Messages
- **Severity**: Low
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:103,108`
- **Description**: `LOG.error("User not found: $username")` logs usernames at ERROR level on failed logins.
- **Risk**: Usernames exposed in logs if log systems are compromised.
- **Recommendation**: Log at WARN/INFO level. Include source IP. Consider partial redaction.
- **References**: CWE-532

---

## SSRF Prevention Audit

### Finding: SSRF-01 No URL Validation on SCIM Application Base URL
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/client/ScimClient.kt:72,96,120,143,165` and `web/src/main/kotlin/com/revethq/auth/web/scim/ScimApplicationResource.kt:44-53`
- **Description**: The SCIM client makes outbound HTTP requests to URLs from the `baseUrl` field of SCIM application entities, set via the unauthenticated management API. No URL validation prevents requests to internal networks, localhost, or cloud metadata endpoints (e.g., `http://169.254.169.254/`). Combined with `quarkus.tls.trust-all=true`, the client connects to any endpoint without certificate verification.
- **Risk**: SSRF allowing internal network scanning, cloud metadata credential theft, internal service probing, and data exfiltration via attacker-controlled SCIM endpoints.
- **Recommendation**: (1) Validate URLs to block private IPs, loopback, link-local, metadata endpoints, non-HTTP schemes. (2) Use DNS resolution validation against rebinding. (3) Consider domain allowlists. (4) Authenticate the management API.
- **References**: OWASP API7:2023 (SSRF), CWE-918

### Finding: SSRF-02 Redirect URI -- No Server-Side Requests (Positive)
- **Severity**: Informational
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:162-183`
- **Description**: Redirect URIs are correctly used only in HTTP 302 redirects, not for server-side requests.
- **Risk**: None. Correct behavior.
- **Recommendation**: No changes required.
- **References**: RFC 6749 Section 3.1.2

---

## Additional Finding

### Finding: CSRF-01 Missing CSRF Token Validation on Authorization Endpoint
- **Severity**: Medium
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:93`
- **Description**: Explicit TODO comment: `// TODO: Check the CSRF token`. The login form POST accepts credentials without CSRF token validation.
- **Risk**: CSRF attacks against the login form.
- **Recommendation**: Implement CSRF token generation and validation using Quarkus CSRF extension.
- **References**: CWE-352, OWASP CSRF Prevention Cheat Sheet, RFC 9700 Section 4.7
