# Tasks: Add SCIM v2 Outbound Provisioning

## 1. Domain Models (`core` module)

- [x] 1.1 Create `ScimDeleteAction` enum in `core/src/main/kotlin/com/revethq/auth/core/scim/`
- [x] 1.2 Create `ScimOperation` enum in `core/src/main/kotlin/com/revethq/auth/core/scim/`
- [x] 1.3 Create `ScimProvisioningStatus` enum in `core/src/main/kotlin/com/revethq/auth/core/scim/`
- [x] 1.4 Create `RetryPolicy` data class in `core/src/main/kotlin/com/revethq/auth/core/scim/`
- [x] 1.5 Create `ScimApplication` domain model in `core/src/main/kotlin/com/revethq/auth/core/domain/`
- [x] 1.6 Create `ScimDeliveryStatus` domain model in `core/src/main/kotlin/com/revethq/auth/core/domain/`
- [x] 1.7 Create `ScimResourceMapping` domain model in `core/src/main/kotlin/com/revethq/auth/core/domain/`
- [x] 1.8 Create `ScimRelevantEvent` CDI event class in `core/src/main/kotlin/com/revethq/auth/core/scim/`
- [x] 1.9 Create `ScimApplicationService` interface in `core/src/main/kotlin/com/revethq/auth/core/services/`
- [x] 1.10 Create `ScimDeliveryService` interface in `core/src/main/kotlin/com/revethq/auth/core/services/`

## 2. SCIM Scopes (`core` + `persistence` modules)

- [x] 2.1 Define SCIM scope constants in `core/src/main/kotlin/com/revethq/auth/core/scim/ScimScopes.kt`
- [x] 2.2 Create `ScimScopeService` interface in `core/src/main/kotlin/com/revethq/auth/core/services/`
- [x] 2.3 Implement `ScimScopeService` in `persistence/src/main/kotlin/com/revethq/auth/persistence/services/`
- [x] 2.4 Add scope-to-operation mapping in `core/src/main/kotlin/com/revethq/auth/core/scim/ScimScopes.kt`

## 3. Event Processing Interface (`core` + `persistence` modules)

- [x] 3.1 Create `ScimEventProcessor` interface in `core/src/main/kotlin/com/revethq/auth/core/scim/`
- [x] 3.2 Create `ScimEventProcessorConfig` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/` (implemented via ScimProcessorStartup.kt)
- [x] 3.3 Implement `ScheduledScimEventProcessor` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/`
- [x] 3.4 Stub `CdiScimEventProcessor` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/`
- [x] 3.5 Stub `KafkaScimEventProcessor` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/`
- [x] 3.6 Stub `AmqpScimEventProcessor` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/`

## 4. CDI Event Integration (`persistence` module)

- [x] 4.1 Modify `EventRepository` to inject CDI `Event<ScimRelevantEvent>` in `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/EventRepository.kt`
- [x] 4.2 Fire `ScimRelevantEvent` after persisting User events (CREATE, UPDATE, DELETE)
- [x] 4.3 Fire `ScimRelevantEvent` after persisting Group events (CREATE, UPDATE, DELETE)
- [x] 4.4 Fire `ScimRelevantEvent` after persisting GroupMember events (CREATE, DELETE)

## 5. Database Schema (`persistence` module)

Note: Project uses Hibernate auto-DDL generation, not Flyway migrations.

- [x] 5.1 ~~Create Flyway migration~~ JPA entities define schema for `scim_applications`
- [x] 5.2 ~~Create Flyway migration~~ JPA entities define schema for `scim_delivery_status`
- [x] 5.3 ~~Create Flyway migration~~ JPA entities define schema for `scim_resource_mappings`
- [x] 5.4 Add indexes for status + nextRetryAt, event_id, scim_application_id (via JPA annotations)

## 6. Persistence Layer (`persistence` module)

- [x] 6.1 Create `ScimApplication` JPA entity in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/`
- [x] 6.2 Create `ScimDeliveryStatus` JPA entity in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/`
- [x] 6.3 Create `ScimResourceMapping` JPA entity in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/`
- [x] 6.4 Create `ScimApplicationMapper` in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/`
- [x] 6.5 Create `ScimDeliveryStatusMapper` in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/`
- [x] 6.6 Create `ScimResourceMappingMapper` in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/mappers/`
- [x] 6.7 Create `ScimApplicationRepository` in `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/`
- [x] 6.8 Create `ScimDeliveryStatusRepository` in `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/`:
  - `findPendingOrRetryable()` - status=PENDING or (status=RETRYING and nextRetryAt <= now)
  - `findByScimApplicationId()`
  - `findByEventId()`
- [x] 6.9 Create `ScimResourceMappingRepository` in `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/`
- [x] 6.10 Implement `ScimApplicationService` in `persistence/src/main/kotlin/com/revethq/auth/persistence/services/`
- [x] 6.11 Implement `ScimDeliveryService` in `persistence/src/main/kotlin/com/revethq/auth/persistence/services/`

## 7. Application Auto-Provisioning (`persistence` module)

- [x] 7.1 Extend `ApplicationService` interface in `core/src/main/kotlin/com/revethq/auth/core/services/ApplicationService.kt`
- [x] 7.2 Implement auto-create Application logic in `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScimApplicationService.kt`
- [x] 7.3 Implement scope validation logic in `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScimApplicationService.kt`
- [x] 7.4 Return Application credentials in creation response

## 8. JWT Token Generation

JWT generation for SCIM requests uses the existing `AuthorizationServerService.generateClientCredentialsAccessToken()` method rather than a dedicated `ScimTokenService`. This reuses the standard client credentials token flow.

- [x] 8.1 ~~Create `ScimTokenService` interface~~ Use `AuthorizationServerService.generateClientCredentialsAccessToken()` instead
- [x] 8.2 ~~Implement `ScimTokenService`~~ Not needed - reuse existing token generation
- [x] 8.3 Add token lifetime config property `revet.scim.token.lifetime` in `web/src/main/resources/application.properties`

## 9. SCIM HTTP Client (`persistence` module)

- [x] 9.1 Create `ScimClient` interface in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/client/`
- [x] 9.2 Implement `ScimClient` using Quarkus REST Client in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/client/`
- [x] 9.3 Implement JWT Bearer token authentication
- [x] 9.4 Create `ScimUserMapper` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/mappers/`
- [x] 9.5 Create `ScimGroupMapper` in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/mappers/`
- [x] 9.6 Implement attribute mapping logic using JSONPath expressions

## 10. Kotlin Coroutines Integration (`persistence` module)

- [x] 10.1 Add `kotlinx-coroutines-core` dependency to `persistence/build.gradle` if not present
- [x] 10.2 Implement parallel delivery in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/ScheduledScimEventProcessor.kt`:
  - Use `coroutineScope` and `async`
  - Use `SupervisorJob` for error isolation
- [x] 10.3 Implement backoff calculation in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/RetryPolicy.kt` (in core RetryPolicy)

## 11. SCIM Operations Implementation (`persistence` module)

All in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/operations/`:

- [x] 11.1 Implement `ScimUserOperations.createUser()` - POST /Users
- [x] 11.2 Implement `ScimUserOperations.updateUser()` - PUT /Users/{id}
- [x] 11.3 Implement `ScimUserOperations.deactivateUser()` - PATCH /Users/{id} (active=false)
- [x] 11.4 Implement `ScimUserOperations.deleteUser()` - DELETE /Users/{id}
- [x] 11.5 Implement `ScimGroupOperations.createGroup()` - POST /Groups
- [x] 11.6 Implement `ScimGroupOperations.updateGroup()` - PUT /Groups/{id}
- [x] 11.7 Implement `ScimGroupOperations.deleteGroup()` - DELETE /Groups/{id}
- [x] 11.8 Implement `ScimGroupOperations.addMember()` - PATCH /Groups/{id}
- [x] 11.9 Implement `ScimGroupOperations.removeMember()` - PATCH /Groups/{id}
- [x] 11.10 Implement `ScimResourceMappingService` for external ID tracking

## 12. Scheduled Processor Implementation (`persistence` module)

All in `persistence/src/main/kotlin/com/revethq/auth/persistence/scim/processors/ScheduledScimEventProcessor.kt`:

- [x] 12.1 Implement `pollAndProcess()`:
  - Query for PENDING or RETRYING (where nextRetryAt <= now) delivery statuses
  - Group by eventId
  - Process each delivery in parallel using coroutines
- [x] 12.2 Implement `processDelivery()`:
  - Generate fresh JWT
  - Execute SCIM HTTP request
  - Update status (SUCCESS, FAILED, RETRYING)
  - Store scimResourceId on success
  - Calculate nextRetryAt on failure
- [x] 12.3 Implement max retry exceeded handling (status → FAILED)

## 13. REST API (`core` + `web` modules)

DTOs in `core/src/main/kotlin/com/revethq/auth/core/api/dto/`:
- [x] 13.1 Create `ScimApplicationRequest` DTO (applicationId optional)
- [x] 13.2 Create `ScimApplicationResponse` DTO (includes Application credentials when auto-created)
- [x] 13.3 Create `ScimDeliveryStatusResponse` DTO

API interface in `core/src/main/kotlin/com/revethq/auth/core/api/`:
- [x] 13.4 Create `ScimApplicationApi` JAX-RS interface

Implementation in `web/src/main/kotlin/com/revethq/auth/web/scim/`:
- [x] 13.5 Implement `ScimApplicationResource` REST endpoints:
  - POST /scim-applications
  - GET /scim-applications?authorizationServerIds=...
  - GET /scim-applications/{scimApplicationId}
  - PUT /scim-applications/{scimApplicationId}
  - DELETE /scim-applications/{scimApplicationId}
- [x] 13.6 Implement delivery status query endpoint:
  - GET /scim-applications/{scimApplicationId}/deliveries

## 14. Testing

Unit tests:
- [x] 14.1 Unit tests for SCIM scope provisioning (`core/src/test/kotlin/.../scim/ScimScopesTest.kt`)
- [x] 14.2 Unit tests for Application auto-creation with scopes (`web/src/test/kotlin/.../scim/ScimApplicationServiceTest.kt`)
- [x] 14.3 Unit tests for scope validation on existing Application (`web/src/test/kotlin/.../scim/ScimApplicationServiceTest.kt`)
- [x] 14.4 ~~Unit tests for ScimTokenService~~ Not needed - uses `AuthorizationServerService.generateClientCredentialsAccessToken()`
- [x] 14.5 Unit tests for ScimUserMapper attribute mapping (`persistence/src/test/kotlin/.../scim/mappers/ScimUserMapperTest.kt`)
- [x] 14.6 Unit tests for ScimGroupMapper (`persistence/src/test/kotlin/.../scim/mappers/ScimGroupMapperTest.kt`)
- [x] 14.7 Unit tests for retry logic and backoff calculation (`core/src/test/kotlin/.../scim/RetryPolicyTest.kt`)
- [x] 14.8 Unit tests for parallel delivery with error isolation (`web/src/test/kotlin/.../scim/ParallelDeliveryTest.kt`)

Integration tests in `web/src/test/kotlin/com/revethq/auth/web/scim/`:
- [x] 14.9 Integration tests for ScimApplicationResource CRUD (`ScimApplicationResourceTest.kt`)
- [x] 14.10 Integration tests for CDI event firing on User/Group/GroupMember changes (`ScimEventIntegrationTest.kt`)
- [x] 14.11 Integration tests for SCIM HTTP operations with WireMock (`ScimProvisioningIntegrationTest.kt`)
- [x] 14.12 Integration tests for group membership sync (`ScimProvisioningIntegrationTest.kt`)

## 15. Configuration (`web` module)

- [x] 15.1 Add configuration properties to `web/src/main/resources/application.properties`:
  ```properties
  revet.scim.processor=scheduled
  revet.scim.scheduled.interval=5s
  revet.scim.token.lifetime=3600
  revet.scim.enabled=true
  ```
- [x] 15.2 Document configuration options in code comments

## Module Summary

| Module | New Packages | Components |
|--------|--------------|------------|
| `core` | `com.revethq.auth.core.scim` | Enums, interfaces, CDI event class |
| `core` | `com.revethq.auth.core.domain` | Domain models (ScimApplication, etc.) |
| `core` | `com.revethq.auth.core.services` | Service interfaces |
| `core` | `com.revethq.auth.core.api.dto` | Request/Response DTOs |
| `core` | `com.revethq.auth.core.api` | JAX-RS API interface |
| `persistence` | `com.revethq.auth.persistence.entities` | JPA entities |
| `persistence` | `com.revethq.auth.persistence.entities.mappers` | Entity mappers |
| `persistence` | `com.revethq.auth.persistence.repositories` | Panache repositories |
| `persistence` | `com.revethq.auth.persistence.services` | Service implementations |
| `persistence` | `com.revethq.auth.persistence.scim.processors` | Event processors |
| `persistence` | `com.revethq.auth.persistence.scim.client` | SCIM HTTP client |
| `persistence` | `com.revethq.auth.persistence.scim.mappers` | SCIM schema mappers |
| `persistence` | `com.revethq.auth.persistence.scim.operations` | SCIM operation handlers |
| `web` | `com.revethq.auth.web.scim` | REST resource implementations |

## Dependencies

- Tasks 2.x depend on Scope entity existing
- Tasks 3.x can start immediately (interface definition)
- Tasks 4.x depend on 1.8 (ScimRelevantEvent)
- Tasks 5.x can start after 1.x (schema matches domain models)
- Tasks 6.x depend on Tasks 1.x and 5.x
- Tasks 7.x depend on Tasks 2.x and 6.1-6.6
- Tasks 8.x depend on Tasks 6.x
- Tasks 9.x depend on Tasks 8.x
- Tasks 10.x can start after 1.x
- Tasks 11.x depend on Tasks 9.x
- Tasks 12.x depend on Tasks 3.3, 10.x, 11.x
- Tasks 13.x depend on Tasks 6.10, 7.x and can run in parallel with 9-12
- Tasks 14.x depend on all implementation tasks

## Parallelizable Work

- Tasks 1.x (domain models) can all be done in parallel
- Tasks 2.1-2.2 (scope definitions/interface) can be done in parallel with 1.x
- Tasks 3.1-3.2 (interface definition) can be done early
- Tasks 5.x (migrations) can be done in parallel after 1.x
- Tasks 6.1-6.6 (entities/mappers) can be done in parallel
- Tasks 10.x (coroutines) can be done in parallel with 9.x
- Tasks 13.1-13.4 (DTOs/interface) can be done in parallel with 9-12
