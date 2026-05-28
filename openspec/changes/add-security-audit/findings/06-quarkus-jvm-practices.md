# Quarkus & JVM Best Practices - Security Audit Findings

**Audit Date**: 2026-05-21
**Auditor**: Claude Opus 4.6 (Automated Security Audit)
**Scope**: Quarkus framework configuration, JVM security patterns, dependency management, and deployment practices

## Summary

| Severity      | Count |
|---------------|-------|
| Critical      | 1     |
| High          | 5     |
| Medium        | 6     |
| Low           | 4     |
| Informational | 3     |
| **Total**     | **19**|

---

### Finding: QJP-01 CORS Allows All Origins
- **Severity**: Critical
- **Location**: `web/src/main/resources/application.properties:8`
- **Description**: The regex pattern `/.*/` combined with `access-control-allow-credentials=true` lets any website make credentialed requests to the server, enabling session riding and token theft.
- **Risk**: Any website can make authenticated cross-origin requests to all API endpoints, including the token endpoint and management API.
- **Recommendation**: Replace `/.*/` with specific allowed origins. If API is same-origin only, disable CORS entirely.
- **References**: OWASP CORS Misconfiguration, CWE-942

---

### Finding: QJP-02 TLS Trust-All Enabled Globally
- **Severity**: High
- **Location**: `web/src/main/resources/application.properties:12`
- **Description**: `quarkus.tls.trust-all=true` disables certificate verification for all outbound TLS connections in all environments, enabling MITM attacks on SCIM provisioning and IdP federation calls.
- **Risk**: Man-in-the-middle attacks can intercept all outbound HTTPS traffic including SCIM Bearer tokens and user provisioning data.
- **Recommendation**: Remove from production. Use `%dev.quarkus.tls.trust-all=true` for dev only. Configure dedicated TLS contexts with specific trust stores for self-signed endpoints.
- **References**: CWE-295 (Improper Certificate Validation), OWASP Transport Layer Security Cheat Sheet

---

### Finding: QJP-03 Swagger UI Exposed in Production
- **Severity**: High
- **Location**: `web/src/main/resources/application.properties:15`
- **Description**: `quarkus.swagger-ui.always-include=true` exposes the full API specification in production at `/q/swagger-ui/`, providing attackers a detailed map of every endpoint, parameter, and data model.
- **Risk**: Full API surface area disclosure. Combined with no authentication on management endpoints, provides a complete interactive attack console.
- **Recommendation**: Remove `quarkus.swagger-ui.always-include=true`. Use `%dev.quarkus.swagger-ui.always-include=true` instead.
- **References**: CWE-200, OWASP API8:2023

---

### Finding: QJP-04 Hibernate Schema Auto-Update in Production
- **Severity**: High
- **Location**: `persistence/src/main/resources/application.properties:7`
- **Description**: `quarkus.hibernate-orm.schema-management.strategy=update` without profile restriction means Hibernate modifies the production schema at startup, risking data loss, unintended column drops, and deployment failures.
- **Risk**: Automatic schema changes in production can corrupt data. Schema modifications are not versioned or reviewable. Failed migrations can leave the database in an inconsistent state.
- **Recommendation**: Set to `none` or `validate` for production. Use `%dev.quarkus.hibernate-orm.schema-management.strategy=update` for development. Use Flyway or Liquibase for production schema management.
- **References**: OWASP API8:2023, CWE-1188

---

### Finding: QJP-05 Database Credentials Have Insecure Defaults
- **Severity**: High
- **Location**: `persistence/src/main/resources/application.properties:2-3`
- **Description**: Password defaults to `notsecure` which will be used if the `CB_AUTH_DATABASE_PASSWORD` environment variable is not set in production. The two `application.properties` files also have inconsistent defaults (`notsecure` vs `auth`).
- **Risk**: If deployed without explicitly setting database credentials, the database is accessible with known trivial passwords.
- **Recommendation**: Remove default values from application.properties (fail fast on missing env vars). Harmonize the inconsistent defaults. Make docker-compose passwords reference `.env` files.
- **References**: CWE-798, OWASP A07:2021

---

### Finding: QJP-06 IdentityProvider Entity Stores Client Secret in Plaintext
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/IdentityProvider.kt:50`
- **Description**: The `clientSecret` field for upstream IdP authentication is stored as plaintext in the database with no encryption at rest at the application level.
- **Risk**: Database compromise exposes all upstream IdP client secrets, allowing an attacker to impersonate the authorization server to upstream identity providers.
- **Recommendation**: Encrypt client secrets at rest using envelope encryption or a KMS. At minimum, ensure database-level encryption at rest is enabled.
- **References**: OWASP Cryptographic Storage Cheat Sheet, CWE-312

---

### Finding: QJP-07 SigningKey Entity Stores Private Key as Plaintext
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/SigningKey.kt:42`
- **Description**: RSA private keys used to sign all JWTs are stored as plaintext TEXT columns. Database compromise exposes all signing keys, enabling an attacker to forge arbitrary access tokens for any authorization server.
- **Risk**: Complete authorization bypass if database is compromised. All tokens can be forged.
- **Recommendation**: Encrypt private keys at rest using envelope encryption. Consider using a dedicated KMS (AWS KMS, HashiCorp Vault). At minimum, ensure database-level encryption at rest.
- **References**: RFC 7517 Section 8, OWASP Cryptographic Storage Cheat Sheet

---

### Finding: QJP-08 Password Hash Leaked to Events Table
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/UserMapper.kt:36`
- **Description**: The `UserMapper.from()` maps `passwordHash` to the domain `User.password` field. The `EventRepository` serializes this via `toString()` (Kotlin data class), writing the bcrypt hash to the event table's JSON column. The hash is stored indefinitely without cleanup.
- **Risk**: Password hashes stored in the event log. If event data is accessed through backup compromise or unauthorized database access, password hashes are exposed.
- **Recommendation**: Strip password field before serialization. Create a `toSafeMap()` method with explicit non-sensitive field selection. Override `toString()` on User to exclude password.
- **References**: OWASP Logging Cheat Sheet, CWE-532

---

### Finding: QJP-09 ApplicationSecretHash Flows Through to Events
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/ApplicationSecretMapper.kt:31`
- **Description**: The unsalted SHA-256 hash of application secrets flows through the domain object and is serialized into the events table via `toString()`.
- **Risk**: Application secret hashes exposed in event storage. Since SHA-256 hashes are unsalted, they are vulnerable to rainbow table attacks.
- **Recommendation**: Exclude secret hashes from event serialization. Use explicit field selection for event data.
- **References**: CWE-532

---

### Finding: QJP-10 Application Secrets Hashed with Unsalted SHA-256
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt:162-163`
- **Description**: Application secrets use `MessageDigest("SHA-256")` without salt, while user passwords correctly use `BcryptUtil.bcryptHash()`. SHA-256 is a fast hash that can be computed at billions of hashes per second on modern GPUs. The hashes are also unsalted, enabling rainbow table attacks.
- **Risk**: If an attacker gains read access to the database, they can brute-force all stored client secret hashes orders of magnitude faster than if BCrypt were used.
- **Recommendation**: Replace SHA-256 with BCrypt (`BcryptUtil.bcryptHash()` already available in the project). Use `BcryptUtil.matches()` for validation.
- **References**: OWASP Password Storage Cheat Sheet, RFC 6819 Section 5.1.4.1.3

---

### Finding: QJP-11 Eager FetchType on Multiple Entity Relationships
- **Severity**: Medium
- **Location**: 5 locations across `AuthorizationServer.kt`, `Application.kt`, `ApplicationSecret.kt`, `Client.kt`, `Scope.kt`
- **Description**: Bidirectional eager loading between `AuthorizationServer` and `Scope` creates cascading fetches. Loading an authorization server eagerly loads all its scopes, and each scope may reference back to the authorization server. Combined with in-memory pagination, this creates N+1 query patterns and memory pressure.
- **Risk**: Denial of service via memory exhaustion. An authorization server with many scopes could cause excessive memory usage on every request that touches the authorization server entity.
- **Recommendation**: Switch to `FetchType.LAZY` for all collection relationships. Use `JOIN FETCH` in specific queries where eager loading is needed.
- **References**: Quarkus Hibernate ORM Best Practices

---

### Finding: QJP-12 In-Memory Pagination Loads All Records
- **Severity**: Medium
- **Location**: 8 service files (UserService, ClientService, ScopeService, ApplicationService, GroupService, GroupMemberService, TemplateService, SchemaService)
- **Description**: Most list methods call `listAll()` then `subList()`, loading all records into memory before paginating. Only `ApplicationService.getApplications()` uses proper database-level pagination via Panache's `.page()` method.
- **Risk**: Memory exhaustion and slow responses as data grows. A tenant with thousands of users will cause all users across all tenants to be loaded into memory on every list request.
- **Recommendation**: Use Panache's built-in pagination: `find("query").page(Page.of(pageNumber, pageSize)).list()`.
- **References**: Quarkus Panache Documentation, CWE-400

---

### Finding: QJP-13 IdentityProviderService Missing CDI Scope Annotation
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/IdentityProviderService.kt:29`
- **Description**: Missing `@ApplicationScoped` and `@Transactional` annotations, inconsistent with all other services in the project.
- **Risk**: Without `@ApplicationScoped`, the bean may not be properly managed by CDI. Without `@Transactional`, database operations may not have proper transaction boundaries.
- **Recommendation**: Add `@ApplicationScoped` and `@Transactional` annotations to match the pattern used by all other services.
- **References**: Quarkus CDI Reference

---

### Finding: QJP-14 java.util.Random for Key Selection
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:389`
- **Description**: Uses predictable `java.util.Random()` instead of `SecureRandom` for signing key selection. The `drop(Random().nextInt(keys.size)).firstOrNull()` expression also produces a biased distribution.
- **Risk**: Low practical risk since key choice is not secret, but establishes a pattern of using non-cryptographic random in security code.
- **Recommendation**: Replace with `SecureRandom()`. When key rotation is implemented, select the active key deterministically.
- **References**: CWE-330

---

### Finding: QJP-15 No Production Profile Separation
- **Severity**: Low
- **Location**: Both `application.properties` files
- **Description**: No Quarkus profile prefixes (`%prod.`, `%dev.`) are used, so all development-oriented insecure settings (trust-all TLS, Swagger UI, schema auto-update, default passwords) apply in production.
- **Risk**: Development settings in production weaken security posture across the board.
- **Recommendation**: Use Quarkus profile prefixes for all environment-specific settings. Create separate `application-prod.properties` or use `%prod.` / `%dev.` prefixes consistently.
- **References**: Quarkus Configuration Reference, CWE-1188

---

### Finding: QJP-16 Missing DELETE Method in CORS
- **Severity**: Low
- **Location**: `web/src/main/resources/application.properties:11`
- **Description**: CORS allows only `GET,PUT,POST` but the API has DELETE and PATCH endpoints, indicating the CORS config was not carefully reviewed against the actual API surface.
- **Risk**: Low -- browsers will block DELETE/PATCH cross-origin requests. However, the inconsistency indicates the CORS configuration was not systematically reviewed.
- **Recommendation**: Review and align CORS methods with actual API methods used. Since CORS should be restricted anyway (see QJP-01), address this as part of the CORS overhaul.
- **References**: MDN CORS Documentation

---

### Finding: QJP-17 Native Build Reflection Configuration is Minimal and Appropriate (Positive)
- **Severity**: Informational
- **Location**: Native build configuration files
- **Description**: Only domain/DTO classes are registered for reflection. No entities with sensitive fields are exposed through native image reflection configuration.
- **Risk**: None. This is a positive finding.
- **Recommendation**: No changes needed. Maintain this minimal approach.
- **References**: Quarkus Native Image Reference

---

### Finding: QJP-18 CDI Scope Correctness -- Services Are Properly Scoped (Positive)
- **Severity**: Informational
- **Location**: All service files
- **Description**: All `@ApplicationScoped` services use immutable constructor injection with no mutable per-request state. This is the correct pattern for Quarkus CDI beans.
- **Risk**: None. This is a positive finding.
- **Recommendation**: No changes needed.
- **References**: Quarkus CDI Reference

---

### Finding: QJP-19 Entity-to-Response Mapping Architecture is Sound (Positive)
- **Severity**: Informational
- **Location**: All mapper files
- **Description**: Two-layer mapping (entity -> domain -> DTO) prevents JPA entities from reaching HTTP responses. Sensitive fields are stripped by web-layer mappers. This is a defense-in-depth architecture.
- **Risk**: None. This is a positive finding.
- **Recommendation**: No changes needed. Consider adding `@JsonbTransient` to sensitive domain fields as additional defense-in-depth.
- **References**: OWASP Secure Coding Practices
