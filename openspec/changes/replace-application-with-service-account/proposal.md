# Change: Replace Application with Service Account from IAM

## Why
The Application resource is a custom domain model that duplicates the service principal concept already provided by the IAM library's `revet-service-account` module. Replacing Application with ServiceAccount aligns auth with the shared IAM identity model, enables cross-system permission evaluation via URNs, and reduces domain-specific code that must be maintained in the auth project.

## What Changes
- **BREAKING**: Remove the `Application` domain model, JPA entity, DTOs, API interface (`/api/v1/applications`), route handler, repository, mapper, validators, and service
- **BREAKING**: Remove the `ApplicationService` interface and implementation (secret-related methods are already migrated by `add-credentials-resource`)
- Add `com.revethq.iam:revet-service-account` as a dependency in the `core` module
- Add `com.revethq.iam:revet-service-account-persistence` as a dependency in the `persistence` module
- Use the IAM `ServiceAccountEntity` and `revet_service_accounts` table directly — no wrapping entity
- Add a new `ServiceAccountsApi` interface at `/api/v1/service-accounts` with create, get, list, update, and delete operations
- Add a `ServiceAccountService` in auth that delegates to the IAM service and adds auth-specific concerns: scope assignment and profile association
- Use the service account's UUID `id` as the OAuth `client_id` in the client_credentials flow (matching current Application behavior where `clientId = id.toString()`)
- Map `ServiceAccount.tenantId` to `authorizationServerId` to maintain the authorization server tenant boundary
- Service accounts support optional profile data with schema validation (same as Application today)
- Scope association uses a join table referencing `revet_service_accounts.id` directly
- Update the client_credentials flow to resolve service accounts by ID instead of application ID
- Update `AuthorizationServerService` to stop creating default applications on authorization server creation

## Impact
- Affected specs: `service-account-management` (new), `service-account-oauth` (new)
- Depends on: `add-credentials-resource` (credentials are managed via the unified Credential resource, not ApplicationSecret)
- Affected code:
  - **New dependencies:**
    - `core/build.gradle` — add `com.revethq.iam:revet-service-account`
    - `persistence/build.gradle` — add `com.revethq.iam:revet-service-account-persistence`
  - **New files:**
    - `core/api/interfaces/ServiceAccountsApi.kt` — JAX-RS API interface
    - `core/api/dto/ServiceAccountRequest.kt`, `ServiceAccountResponse.kt`, `ServiceAccountsResponse.kt` — DTOs
    - `core/services/ServiceAccountService.kt` — service interface
    - `persistence/services/ServiceAccountService.kt` — service implementation (delegates to IAM service, adds scope + profile logic)
    - `web/api/routes/ServiceAccounts.kt` — route handler
    - `web/api/routes/mappers/ServiceAccountMapper.kt` — web mapper
    - `web/api/validators/ValidServiceAccountRequest.kt` — request validation
  - **Modified files:**
    - `web/authorization/routes/AuthorizationServer.kt` — resolve service accounts by ID for client_credentials flow
    - `persistence/services/AuthorizationServerService.kt` — remove application creation on authorization server setup
    - `web/config/ReflectionConfiguration.kt` — register IAM classes for native image
  - **Removed files:**
    - `core/domain/Application.kt`
    - `core/api/interfaces/ApplicationsApi.kt`
    - `core/api/dto/ApplicationRequest.kt`, `ApplicationResponse.kt`, `ApplicationsResponse.kt`
    - `core/services/ApplicationService.kt`
    - `persistence/entities/Application.kt`
    - `persistence/entities/mappers/ApplicationMapper.kt`
    - `persistence/repositories/ApplicationRepository.kt`
    - `persistence/services/ApplicationService.kt`
    - `web/api/routes/Applications.kt`
    - `web/api/routes/mappers/ApplicationMapper.kt`
    - `web/api/validators/ValidApplicationRequest.kt`
    - `web/api/validators/ValidApplication.kt`
