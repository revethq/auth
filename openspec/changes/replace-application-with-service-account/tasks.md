## Prerequisites
- [ ] 0.1 `add-credentials-resource` is implemented (ApplicationSecret removed, Credential resource available)

## 1. Add IAM service-account dependencies
- [ ] 1.1 Add `com.revethq.iam:revet-service-account` to `core/build.gradle`
- [ ] 1.2 Add `com.revethq.iam:revet-service-account-persistence` to `persistence/build.gradle`
- [ ] 1.3 Verify dependencies resolve and compile

## 2. Create scope join table
- [ ] 2.1 Define a scope join table (e.g. `scopereference`) that links `revet_service_accounts.id` to `scope.id`, following the same pattern used by Client
- [ ] 2.2 Register IAM `ServiceAccountEntity` and related classes for native image reflection in `ReflectionConfiguration.kt`

## 3. Implement service layer
- [ ] 3.1 Create `ServiceAccountService` interface in `core/services/` with create, get, list, update, delete methods
- [ ] 3.2 Create `ServiceAccountService` implementation in `persistence/services/` delegating to IAM's `ServiceAccountService` for CRUD and adding scope + profile logic
- [ ] 3.3 Implement tenantId <-> authorizationServerId mapping in create and list operations
- [ ] 3.4 Implement profile creation with `ProfileType.SERVICE_ACCOUNT` and OIDC schema validation
- [ ] 3.5 Implement scope association on create and update

## 4. Create API layer
- [ ] 4.1 Create DTOs: `ServiceAccountRequest`, `ServiceAccountResponse`, `ServiceAccountsResponse` in `core/api/dto/`
- [ ] 4.2 Create `ServiceAccountsApi` JAX-RS interface in `core/api/interfaces/` at `/api/v1/service-accounts`
- [ ] 4.3 Create `ServiceAccounts` route handler in `web/api/routes/`
- [ ] 4.4 Create `ServiceAccountMapper` in `web/api/routes/mappers/`
- [ ] 4.5 Create `ValidServiceAccountRequest` validator in `web/api/validators/`

## 5. Update OAuth flows
- [ ] 5.1 Update `AuthorizationServer.kt` client_credentials branch to resolve service accounts by ID via `ServiceAccountService`
- [ ] 5.2 Update token `sub` and `client_id` claims to use service account ID
- [ ] 5.3 Update `AuthorizationServerService` to stop creating default applications on authorization server creation

## 6. Remove Application resource
- [ ] 6.1 Remove `core/domain/Application.kt`
- [ ] 6.2 Remove `core/api/interfaces/ApplicationsApi.kt`
- [ ] 6.3 Remove `core/api/dto/ApplicationRequest.kt`, `ApplicationResponse.kt`, `ApplicationsResponse.kt`
- [ ] 6.4 Remove `core/services/ApplicationService.kt`
- [ ] 6.5 Remove `persistence/entities/Application.kt`
- [ ] 6.6 Remove `persistence/entities/mappers/ApplicationMapper.kt`
- [ ] 6.7 Remove `persistence/repositories/ApplicationRepository.kt`
- [ ] 6.8 Remove `persistence/services/ApplicationService.kt`
- [ ] 6.9 Remove `web/api/routes/Applications.kt`
- [ ] 6.10 Remove `web/api/routes/mappers/ApplicationMapper.kt`
- [ ] 6.11 Remove `web/api/validators/ValidApplicationRequest.kt` and `ValidApplication.kt`
- [ ] 6.12 Remove Application references from `ReflectionConfiguration.kt`
- [ ] 6.13 Update `openspec/project.md` domain context table to replace Application with ServiceAccount

## 7. Testing
- [ ] 7.1 Write unit tests for ServiceAccountService (create, get, list, update, delete)
- [ ] 7.2 Write integration tests for `/api/v1/service-accounts` endpoints
- [ ] 7.3 Write integration test for client_credentials flow using a service account
- [ ] 7.4 Verify existing authorization_code flow tests still pass (no regression)

## 8. Data migration
- [ ] 8.1 Write SQL migration script to copy `application` rows into `revet_service_accounts`
- [ ] 8.2 Document migration steps in a migration note
