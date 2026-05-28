# Credential & Secret Management -- Security Audit Findings

## Summary

| Severity      | Count |
|---------------|-------|
| Critical      | 2     |
| High          | 1     |
| Medium        | 3     |
| Low           | 2     |
| Informational | 3     |
| **Total**     | **11**|

---

### Finding: CRED-01 Application Secret Uses SHA-256 Instead of BCrypt -- Vulnerable to Offline Brute Force

- **Severity**: Critical
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt:161-163`
- **Description**: Application (client) secrets are hashed using SHA-256 (`MessageDigest.getInstance("SHA-256")`) rather than BCrypt. SHA-256 is a fast, non-salted hash function designed for data integrity, not credential storage. A modern GPU can compute billions of SHA-256 hashes per second.
  ```kotlin
  val messageDigest = MessageDigest.getInstance("SHA-256")
  val secret = BigInteger(1, SecureRandom().generateSeed(120)).toString(16)
  val secretHash = BigInteger(1, messageDigest.digest(secret.toByteArray())).toString(16)
  ```
- **Risk**: If an attacker gains read access to the database, they can brute-force all stored client secret hashes orders of magnitude faster than if BCrypt were used. Since client secrets grant access via client_credentials flow, this is a critical authorization bypass path. The SHA-256 hashes are also unsalted, enabling rainbow table attacks.
- **Recommendation**: Replace SHA-256 with BCrypt (`BcryptUtil.bcryptHash()` already available in the project). Use `BcryptUtil.matches()` for validation.
- **References**: OWASP Password Storage Cheat Sheet, RFC 6819 Section 5.1.4.1.3

---

### Finding: CRED-02 Client Secret Validation Vulnerable to Timing Attack

- **Severity**: Critical
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt:228-232`
- **Description**: The `isApplicationSecretValid` method computes a SHA-256 hash and compares using Kotlin's `==` operator (`secretHash == applicationSecretOpt.get().applicationSecretHash`), which delegates to `String.equals()` — a non-constant-time comparison that returns `false` at the first mismatched character.
- **Risk**: An attacker can determine the correct hash byte-by-byte by measuring response time variations, reducing the search space from exponential to linear. Compounded by CRED-01 (SHA-256 is fast, so hash comparison is the dominant time signal).
- **Recommendation**: Switch to BCrypt for secret storage and validation (see CRED-01). If SHA-256 must be retained temporarily, use `MessageDigest.isEqual()` for constant-time comparison.
- **References**: OWASP Testing Guide: Testing for Timing Attacks, RFC 6749 Section 10.1, CWE-208

---

### Finding: CRED-03 BCrypt Work Factor Uses Default (10) Instead of Recommended Minimum (12)

- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/UserService.kt:208`
- **Description**: `BcryptUtil.bcryptHash(password)` uses the default work factor of 10. Current OWASP recommendations suggest a minimum cost of 12 for 2024+ deployments. A TODO on line 207 acknowledges this should be configurable.
- **Risk**: Cost 10 means ~1024 iterations; cost 12 means ~4096 — approximately 4x more resistance to offline brute force. As GPU/ASIC capabilities increase, cost 10 becomes progressively easier to crack.
- **Recommendation**: Explicitly set cost to at least 12: `BcryptUtil.bcryptHash(password, 12)`. Make configurable per authorization server. Implement re-hash on next successful login for existing hashes.
- **References**: OWASP Password Storage Cheat Sheet (cost 10 minimum, 12+ preferred), NIST SP 800-63B Section 5.1.1.2

---

### Finding: CRED-04 Default Database Password "notsecure" in Configuration and Docker Compose

- **Severity**: Medium
- **Location**: `persistence/src/main/resources/application.properties:3`, `web/src/main/resources/application.properties:4`, `docker-compose.yaml:17`, `docker-compose.local.yaml:19`, `docker-compose.dev.yaml:10`
- **Description**: Default database passwords are hardcoded across multiple files. The two application.properties files have inconsistent defaults ("notsecure" vs "auth"). Docker-compose files hardcode passwords with no environment variable override.
- **Risk**: If deployed without setting `CB_AUTH_DATABASE_PASSWORD`, the database is accessible with known trivial passwords.
- **Recommendation**: Remove default values from application.properties (fail fast). Make docker-compose passwords reference `.env` files. Harmonize the inconsistent defaults.
- **References**: OWASP A07:2021, CWE-798

---

### Finding: CRED-05 Private Signing Keys Stored Unencrypted in Database

- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/SigningKey.kt:42-43`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:106-110`
- **Description**: RSA private signing keys are stored as plaintext PEM strings in the `SigningKey` entity's `privateKey` column. No encryption-at-rest is applied at the application level.
- **Risk**: Database compromise exposes all RSA private keys in cleartext, allowing an attacker to forge arbitrary JWT access tokens for any authorization server.
- **Recommendation**: Encrypt private keys at rest using envelope encryption. Consider using a dedicated KMS (AWS KMS, HashiCorp Vault). At minimum, ensure database-level encryption at rest.
- **References**: RFC 7517 Section 8, OWASP Cryptographic Storage Cheat Sheet

---

### Finding: CRED-06 No Signing Key Rotation Mechanism

- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:94-119`
- **Description**: Signing keys are generated only during authorization server creation. No API endpoint, scheduled task, or management function exists to create new keys, deprecate old keys, or remove expired keys from JWKS. No concept of "active" vs "retired" key exists.
- **Risk**: A compromised signing key remains in use indefinitely with no mechanism to retire it without recreating the entire authorization server.
- **Recommendation**: Add key lifecycle management: create new keys, add status field (ACTIVE/DEPRECATED/REVOKED), only use ACTIVE keys for signing, include DEPRECATED in JWKS for grace period.
- **References**: RFC 7517 Section 4.5, NIST SP 800-57, RFC 9700 Section 3.5

---

### Finding: CRED-07 Signing Key Selection Uses java.util.Random Instead of SecureRandom

- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:389`
- **Description**: Uses `java.util.Random()` (non-cryptographic PRNG) to select signing key. The `drop()` approach also creates selection bias.
- **Risk**: Low practical risk since key choice is not secret, but establishes a pattern of using non-cryptographic random in security code.
- **Recommendation**: Replace with `SecureRandom()`. When key rotation is implemented, select the active key deterministically.
- **References**: CWE-330

---

### Finding: CRED-08 Password Hash Mapped to Domain Object "password" Field

- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/UserMapper.kt:36`
- **Description**: `UserMapper.from()` maps `passwordHash` to domain `User.password` field. The naming is misleading and the hash is unnecessarily propagated through the domain layer. API responses correctly exclude it via `UserResponse` DTO.
- **Risk**: Misleading field name could confuse developers. If the domain object is ever logged or serialized without care, the hash would be exposed.
- **Recommendation**: Rename to `passwordHash`. Consider not mapping the hash to the domain object at all.
- **References**: CWE-200

---

### Finding: CRED-09 RSA Key Generation Does Not Explicitly Specify SecureRandom Provider

- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:96-97`
- **Description**: `KeyPairGenerator.initialize(2048)` uses default `SecureRandom` internally (adequate on most platforms) but does not explicitly verify the entropy source. RSA-2048 meets current minimum requirements.
- **Risk**: On standard JVMs, the default is cryptographically adequate. In containerized environments with limited entropy, may be less predictable.
- **Recommendation**: Consider explicitly passing `SecureRandom()`. Consider increasing key size to 3072 for protection beyond 2030 per NIST SP 800-57.
- **References**: NIST SP 800-57 Part 1, RFC 7518 Section 3.3

---

### Finding: CRED-10 Application Secret Entropy Generation Has Variable Output Length

- **Severity**: Informational
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt:162`
- **Description**: `BigInteger(1, SecureRandom().generateSeed(120)).toString(16)` generates 960 bits of entropy (sufficient) but `BigInteger` drops leading zero bytes, producing variable-length hex strings.
- **Risk**: Cosmetic concern; entropy is sufficient. Variable length could confuse validation or logging.
- **Recommendation**: Use `Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)` for consistent-length output.
- **References**: RFC 6749 Section 2.3.1

---

### Finding: CRED-11 Password Validation Correctly Uses Generic Error Messages but Leaks Timing Information

- **Severity**: Informational
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:100-124`
- **Description**: Authentication correctly uses generic "Invalid username or password" for all failure cases. However, when a user is not found, the BCrypt comparison is skipped (fast path), while valid users always trigger BCrypt comparison (slow path). This timing difference could allow user enumeration.
- **Risk**: Timing side-channel for user enumeration. Generic error messages are correct.
- **Recommendation**: Add a dummy BCrypt comparison when user is not found so both paths take approximately the same time.
- **References**: OWASP Authentication Cheat Sheet, CWE-204, RFC 6819 Section 4.1.1
