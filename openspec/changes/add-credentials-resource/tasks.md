## 1. Core Domain & Interfaces
- [x] 1.1 Create `CredentialType` enum (`PASSWORD`, `API_KEY`, `TOTP`, `RECOVERY_CODE`)
- [x] 1.2 Create `CredentialStatus` enum (`ACTIVE`, `REVOKED`)
- [x] 1.3 Create `Credential` domain model in `core/domain/`
- [x] 1.4 Create `CredentialRequest` and `CredentialResponse` DTOs in `core/api/dto/`
- [x] 1.5 Create `CredentialsResponse` list wrapper DTO
- [x] 1.6 Create `CredentialService` interface in `core/services/`
- [x] 1.7 Create `CredentialsApi` JAX-RS interface in `core/api/interfaces/`
- [x] 1.8 Remove `password` field from `User` domain model
- [x] 1.9 Remove `setPassword` and `validatePassword` from `UserService` interface
- [x] 1.10 Remove `ApplicationSecret` domain model
- [x] 1.11 Remove `ApplicationSecretRequest`, `ApplicationSecretResponse`, `ApplicationSecretsResponse` DTOs
- [x] 1.12 Remove `ApplicationSecretsApi` interface
- [x] 1.13 Remove application-secret methods from `ApplicationService` interface (`getApplicationSecrets`, `createApplicationSecret`, `deleteApplicationSecret`, `isApplicationSecretValid`, `getApplicationSecret`)

## 2. Persistence Layer
- [x] 2.1 Create `Credential` JPA entity in `persistence/entities/`
- [x] 2.2 Create `CredentialMapper` in `persistence/entities/mappers/`
- [x] 2.3 Create `CredentialRepository` in `persistence/repositories/`
- [x] 2.4 Create `CredentialService` implementation in `persistence/services/`
- [x] 2.5 Remove `passwordHash` from User entity
- [x] 2.6 Remove `setPassword` and `validatePassword` from `UserService` implementation
- [x] 2.7 Update `UserMapper` to remove password mapping
- [x] 2.8 Update `createUser` in `UserService` to stop handling password
- [x] 2.9 Remove `ApplicationSecret` entity
- [x] 2.10 Remove `ApplicationSecretMapper`
- [x] 2.11 Remove `ApplicationSecretRepository`
- [x] 2.12 Remove application-secret methods from `ApplicationService` implementation
- [x] 2.13 Update `ScopeReferenceType` to replace `APPLICATION_SECRET` with `CREDENTIAL`

## 3. Web Layer
- [x] 3.1 Create web `CredentialMapper` in `web/api/routes/mappers/`
- [x] 3.2 Create `Credentials` route handler in `web/api/routes/`
- [x] 3.3 Remove `ApplicationSecrets` route handler
- [x] 3.4 Remove application-secret mappers from `ApplicationMapper`
- [x] 3.5 Remove `ValidApplicationSecret`, `ValidApplicationSecretName`, `ValidApplicationSecretScopes` validators
- [x] 3.6 Update `UserMapper` in web layer to remove password from request mapping
- [x] 3.7 Update `AuthorizationServer.kt` — use `CredentialService.validate` for login (authorization code flow)
- [x] 3.8 Update `AuthorizationServer.kt` — use `CredentialService` for client_credentials flow

## 4. Database Migration
- [x] 4.1 Create migration: add `credentials` table
- [x] 4.2 Create migration: migrate `passwordHash` from `users` to `credentials` (type PASSWORD)
- [x] 4.3 Create migration: migrate `application_secret` rows to `credentials` (type API_KEY)
- [x] 4.4 Create migration: update `scopereference` entries from APPLICATION_SECRET to CREDENTIAL
- [x] 4.5 Create migration: drop `passwordHash` from `users`
- [x] 4.6 Create migration: drop `application_secret` table

## 5. Testing
- [ ] 5.1 Test credential CRUD for user-owned credentials (PASSWORD, API_KEY)
- [ ] 5.2 Test credential CRUD for application-owned credentials (API_KEY with scopes)
- [ ] 5.3 Test password validation via CredentialService
- [ ] 5.4 Test single-active-password constraint (new password revokes old)
- [ ] 5.5 Test OAuth authorization code flow with CredentialService
- [ ] 5.6 Test client_credentials flow with CredentialService (scope filtering)
- [ ] 5.7 Test that plain credential value is only returned on creation
- [ ] 5.8 Test ownership validation (exactly one of userId/applicationId required)
