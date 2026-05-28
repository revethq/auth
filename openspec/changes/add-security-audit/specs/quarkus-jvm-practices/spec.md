## ADDED Requirements

### Requirement: Dependency Vulnerability Audit
The audit SHALL review all project dependencies for known security vulnerabilities (CVEs).

#### Scenario: Direct dependency CVE check
- **WHEN** the Gradle dependency tree is analyzed against known vulnerability databases
- **THEN** document all dependencies with known CVEs, their severity, and available patched versions

#### Scenario: Transitive dependency CVE check
- **WHEN** transitive dependencies are reviewed
- **THEN** document any transitive dependencies with known vulnerabilities that are not addressed by direct dependency updates

#### Scenario: Outdated security libraries
- **WHEN** security-critical libraries (jose4j, smallrye-jwt, spring-security-core, Quarkus platform) are reviewed
- **THEN** document current versions vs latest available and any security-relevant changes in newer versions

### Requirement: Hibernate ORM Security Audit
The audit SHALL review Hibernate ORM usage for security-relevant patterns and anti-patterns.

#### Scenario: Entity exposure in API responses
- **WHEN** the data flow from JPA entities to API responses is reviewed
- **THEN** verify that JPA entities are not directly serialized (which could expose internal fields like password hashes, private keys)

#### Scenario: Lazy loading and N+1 patterns
- **WHEN** entity relationships and fetch strategies are reviewed
- **THEN** document any lazy loading patterns that could be exploited for denial-of-service via crafted API requests triggering excessive queries

#### Scenario: Schema management strategy
- **WHEN** the Hibernate schema management configuration is reviewed
- **THEN** document `hibernate-orm.schema-management.strategy=update` and assess the risk of automatic schema migration in production

### Requirement: CDI Scope Correctness Audit
The audit SHALL review CDI bean scopes for security implications of shared mutable state.

#### Scenario: Singleton services with request state
- **WHEN** `@ApplicationScoped` and `@Singleton` services are reviewed
- **THEN** verify that no singleton-scoped beans hold per-request mutable state that could leak between requests or tenants

#### Scenario: Request-scoped security context
- **WHEN** the tenant context and user context propagation is reviewed
- **THEN** verify that security-relevant context (current authorization server, authenticated user) uses appropriate CDI scopes

### Requirement: Native Build Security Audit
The audit SHALL review GraalVM native image configuration for security implications.

#### Scenario: Reflection configuration
- **WHEN** the `ReflectionConfiguration.kt` and native image settings are reviewed
- **THEN** document which classes are registered for reflection and assess whether internal classes are unnecessarily exposed

#### Scenario: Native image security features
- **WHEN** the native build configuration is reviewed
- **THEN** document whether GraalVM security features (serialization filtering, resource inclusion) are properly configured

### Requirement: Configuration Secrets Management Audit
The audit SHALL review how configuration secrets are handled across environments.

#### Scenario: Environment variable security
- **WHEN** all configuration properties with default values are reviewed
- **THEN** document which secrets have insecure defaults and verify environment variable override mechanism works correctly

#### Scenario: Production profile separation
- **WHEN** Quarkus profile configuration is reviewed
- **THEN** document whether production-specific security settings exist and whether development defaults could leak into production
