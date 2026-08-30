# Change: Add Credentials resource

## Why
Authentication credentials are currently split across two unrelated patterns: passwords embedded on the User entity and ApplicationSecrets as a separate resource for applications. This makes it impossible to support additional credential types (API keys, TOTP, recovery codes) for users, and creates inconsistency between user and application credential management. A unified Credentials resource consolidates all credential management into a single API, decouples credentials from their owning entities, and enables extensible credential types for both users and applications.

## What Changes
- Add a new top-level `Credential` resource at `/api/v1/credentials`
- Support multiple credential types: `PASSWORD`, `API_KEY`, `TOTP`, `RECOVERY_CODE`
- Credentials can belong to either a user (`userId`) or an application (`applicationId`)
- Credentials optionally reference scopes (replacing the ApplicationSecret scope relationship)
- **BREAKING**: Remove `passwordHash` from the User entity and `password` from User DTOs
- **BREAKING**: Remove `setPassword` and `validatePassword` from `UserService`
- **BREAKING**: Remove the entire `ApplicationSecret` resource (domain model, entity, DTOs, API interface, routes, service methods, repository, mappers, validators)
- **BREAKING**: Remove `ApplicationSecret`-related methods from `ApplicationService`
- Add `CredentialService` with create, list, get, delete, and validate operations
- Update the OAuth 2.1 authorization code flow to validate passwords via `CredentialService`
- Update the client_credentials flow to validate application secrets via `CredentialService`
- Follow the existing ApplicationSecret pattern: plain values returned only on creation, only hashes stored

## Impact
- Affected specs: `credentials` (new capability)
- Affected code:
  - **New files:**
    - `core/domain/Credential.kt` — domain model
    - `core/domain/CredentialType.kt` — type enum
    - `core/domain/CredentialStatus.kt` — status enum
    - `core/api/interfaces/CredentialsApi.kt` — API interface
    - `core/api/dto/CredentialRequest.kt`, `CredentialResponse.kt`, `CredentialsResponse.kt` — DTOs
    - `core/services/CredentialService.kt` — service interface
    - `persistence/entities/Credential.kt` — JPA entity
    - `persistence/entities/mappers/CredentialMapper.kt` — entity mapper
    - `persistence/repositories/CredentialRepository.kt` — repository
    - `persistence/services/CredentialService.kt` — service implementation
    - `web/api/routes/Credentials.kt` — route handler
    - `web/api/routes/mappers/CredentialMapper.kt` — web mapper
  - **Modified files:**
    - `core/domain/User.kt` — remove `password` field
    - `persistence/entities/User.kt` — remove `passwordHash`
    - `persistence/entities/mappers/UserMapper.kt` — remove password mapping
    - `persistence/services/UserService.kt` — remove `setPassword`, `validatePassword`, stop handling password in `createUser`
    - `core/services/UserService.kt` — remove `setPassword`, `validatePassword`
    - `web/authorization/routes/AuthorizationServer.kt` — use `CredentialService.validate` for both login and client_credentials flows
  - **Removed files:**
    - `core/domain/ApplicationSecret.kt`
    - `core/api/interfaces/ApplicationSecretsApi.kt`
    - `core/api/dto/ApplicationSecretRequest.kt`, `ApplicationSecretResponse.kt`, `ApplicationSecretsResponse.kt`
    - `persistence/entities/ApplicationSecret.kt`
    - `persistence/entities/mappers/ApplicationSecretMapper.kt`
    - `persistence/repositories/ApplicationSecretRepository.kt`
    - `persistence/services/ApplicationService.kt` — remove secret-related methods
    - `web/api/routes/ApplicationSecrets.kt`
    - `web/api/routes/mappers/ApplicationMapper.kt` — remove secret-related mappers
    - `web/api/validators/ValidApplicationSecret.kt`
    - `web/api/validators/ValidApplicationSecretName.kt`
    - `web/api/validators/ValidApplicationSecretScopes.kt`
