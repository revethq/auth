## ADDED Requirements

### Requirement: Password Hashing Audit
The audit SHALL review password hashing implementation for users, including algorithm choice, work factor, and migration strategy.

#### Scenario: BCrypt work factor
- **WHEN** the BCrypt configuration in `UserService.kt` is reviewed
- **THEN** document the work factor (cost parameter) and verify it meets current recommendations (minimum 10, recommended 12+)

#### Scenario: Password hash storage
- **WHEN** the User entity and database schema is reviewed
- **THEN** verify password hashes are stored securely and plaintext passwords are never persisted

#### Scenario: Password validation timing
- **WHEN** the password validation flow is reviewed
- **THEN** verify that BCrypt comparison is used (constant-time) and that authentication failures do not leak whether the username or password was incorrect

### Requirement: Client Secret Security Audit
The audit SHALL review application secret generation, storage, and validation.

#### Scenario: Secret generation entropy
- **WHEN** the application secret generation code is reviewed
- **THEN** verify secrets are generated with sufficient entropy from a cryptographically secure source

#### Scenario: Secret hash storage
- **WHEN** the ApplicationSecret entity and persistence is reviewed
- **THEN** verify secrets are stored as BCrypt hashes and plaintext secrets are only returned at creation time

#### Scenario: Secret comparison
- **WHEN** the client credentials validation flow is reviewed
- **THEN** verify BCrypt constant-time comparison is used to prevent timing attacks

### Requirement: Default Credentials Audit
The audit SHALL identify all default, hardcoded, or example credentials in the codebase and configuration.

#### Scenario: Database credentials
- **WHEN** `application.properties` and environment variable defaults are reviewed
- **THEN** document all default database passwords (e.g., `notsecure`) and verify they are overridable via environment variables

#### Scenario: Hardcoded secrets
- **WHEN** the codebase is searched for hardcoded credentials, API keys, or secrets
- **THEN** document any hardcoded sensitive values and their locations

#### Scenario: Example/test credentials
- **WHEN** test fixtures, example configurations, and documentation are reviewed
- **THEN** document any credentials that could be mistakenly used in deployment

### Requirement: Signing Key Generation Audit
The audit SHALL review RSA signing key generation for proper entropy and secure practices.

#### Scenario: Key generation algorithm
- **WHEN** the signing key generation code in `AuthorizationServerService.kt` is reviewed
- **THEN** verify RSA-2048 minimum, proper `KeyPairGenerator` initialization, and `SecureRandom` usage

#### Scenario: Private key storage
- **WHEN** the SigningKey entity and persistence is reviewed
- **THEN** verify private keys are stored securely (PEM format), document whether encryption at rest is applied, and assess exposure risk

#### Scenario: Key rotation capability
- **WHEN** the signing key lifecycle is reviewed
- **THEN** document whether key rotation is supported and whether old keys are properly deprecated via JWKS `kid` management
