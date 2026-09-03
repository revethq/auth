## Prerequisites
- [x] 0.1 `add-credentials-resource` is implemented (ApplicationSecret removed, Credential resource available)

## 1. Add IAM service-account dependencies
- [x] 1.1 Add `com.revethq.iam:revet-service-account` to `core/build.gradle`
- [x] 1.2 Add `com.revethq.iam:revet-service-account-persistence` to `persistence/build.gradle`
- [x] 1.3 Verify dependencies resolve and compile

## 2. Create scope join table
- [x] 2.1 Added `SERVICE_ACCOUNT` to `ScopeReferenceType` enum for scope join via `revet_scope_references` table
- [x] 2.2 Register IAM `ServiceAccount` class for native image reflection in `ReflectionConfiguration.kt`

## 3. Implement service layer
- [x] 3.1 Create `ServiceAccountService` interface in `core/services/` with create, get, list, update, delete methods
- [x] 3.2 Create `ServiceAccountService` implementation in `persistence/services/` using IAM's `ServiceAccountRepository` directly and adding scope + profile logic
- [x] 3.3 Implement tenantId <-> authorizationServerId mapping in create and list operations
- [x] 3.4 Implement profile creation with `ProfileType.ServiceAccount` and OIDC schema validation
- [x] 3.5 Implement scope association on create and update

## 4. Create API layer
- [x] 4.1 Create DTOs: `ServiceAccountRequest`, `ServiceAccountResponse`, `ServiceAccountsResponse` in `core/api/dto/`
- [x] 4.2 Create `ServiceAccountsApi` JAX-RS interface in `core/api/interfaces/` at `/api/v1/service-accounts`
- [x] 4.3 Create `ServiceAccounts` route handler in `web/api/routes/`
- [x] 4.4 Create `ServiceAccountMapper` in `web/api/routes/mappers/`
- [ ] 4.5 Create `ValidServiceAccountRequest` validator in `web/api/validators/` (deferred — not needed for MVP)

## 5. Update OAuth flows
- [x] 5.1 Updated `AuthorizationServerService.generateClientCredentialsAccessToken` parameter from `applicationId` to `principalId`
- [x] 5.2 Token `sub` and `client_id` claims already use `credential.principalId` (service account ID) correctly
- [x] 5.3 No default application creation exists — no change needed

## 6. Remove Application resource
- [x] 6.1 Remove `core/domain/Application.kt`
- [x] 6.2 Remove `core/api/interfaces/ApplicationsApi.kt`
- [x] 6.3 Remove `core/api/dto/ApplicationRequest.kt`, `ApplicationResponse.kt`, `ApplicationsResponse.kt`
- [x] 6.4 Remove `core/services/ApplicationService.kt`
- [x] 6.5 Remove `persistence/entities/Application.kt`
- [x] 6.6 Remove `persistence/entities/mappers/ApplicationMapper.kt`
- [x] 6.7 Remove `persistence/repositories/ApplicationRepository.kt`
- [x] 6.8 Remove `persistence/services/ApplicationService.kt`
- [x] 6.9 Remove `web/api/routes/Applications.kt`
- [x] 6.10 Remove `web/api/routes/mappers/ApplicationMapper.kt`
- [x] 6.11 Remove `web/api/validators/ValidApplicationRequest.kt`, `ValidApplication.kt`, and related validators
- [x] 6.12 Removed Application references from `EventRepository.kt` and `ProfileMapper.kt`
- [ ] 6.13 Update `openspec/project.md` domain context table to replace Application with ServiceAccount

## 7. Testing
- [ ] 7.1 Write unit tests for ServiceAccountService (create, get, list, update, delete)
- [ ] 7.2 Write integration tests for `/api/v1/service-accounts` endpoints
- [ ] 7.3 Write integration test for client_credentials flow using a service account
- [ ] 7.4 Verify existing authorization_code flow tests still pass (no regression)

## 8. Data migration
- [ ] 8.1 Write SQL migration script to copy `application` rows into `revet_service_accounts`
- [ ] 8.2 Document migration steps in a migration note

## Additional changes made
- Renamed Credential `userId`/`applicationId` to single `principalId` field (per user feedback)
- Removed `PrincipalType` enum — credential type is sufficient for filtering
- Updated `ScimApplication` domain/entity/DTOs: `applicationId` → `serviceAccountId`, `autoCreateApplication` → `autoCreateServiceAccount`
- Updated `ScimApplicationService` to create ServiceAccounts instead of Applications for SCIM
- Updated `ScimScopeService` to validate scopes via `ScopeReference` table instead of `ApplicationRepository`
- Updated `ScimTokenService` to use `serviceAccountId` instead of `applicationId`
- Updated all SCIM tests (`ScimApplicationServiceTest`, `ScimApplicationResourceTest`, `ScimProvisioningIntegrationTest`)
- Added `ServiceAccountNotFoundExceptionMapper` for proper 404 responses
- Added `SERVICE_ACCOUNT` to `ResourceType` enum
- Added `quarkus-hibernate-orm-panache-kotlin` dependency for IAM Kotlin Panache compatibility
