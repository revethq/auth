# Design: SCIM v2 Outbound Provisioning

## Context

Revet Auth has an existing event system (EventRepository) that records User, Group, and GroupMember lifecycle events to the database. This provides a foundation for triggering outbound SCIM operations.

The SCIM 2.0 protocol (RFC 7643/7644) defines a RESTful API for identity management. When acting as a SCIM Client (outbound provisioning), Revet Auth must make HTTP requests to downstream SCIM Service Providers.

**Stakeholders**: Developers integrating Revet Auth with downstream SaaS applications.

**Constraints**:
- Must be compatible with Okta's SCIM implementation patterns
- Must not block the primary user/group operations if SCIM delivery fails
- Educational project - favor clarity over optimization
- Downstream consumers validate JWTs against the authorization server's JWKS

## Goals / Non-Goals

### Goals
- Push User lifecycle events (create, update, deactivate, delete) to downstream SCIM endpoints
- Push Group lifecycle events (create, update, delete) to downstream SCIM endpoints
- Push GroupMember changes (add/remove) to downstream SCIM endpoints
- Support per-application configuration (endpoint URL, attribute mapping)
- Use JWT-based authentication with existing Application credentials
- Automatically provision SCIM scopes and optionally create Applications
- Leverage existing Event system for audit (no duplication)
- Handle failures gracefully with retry logic
- Support pluggable event processing backends

### Non-Goals
- Inbound SCIM (Service Provider) - receiving provisioning requests
- Real-time synchronization guarantees (eventual consistency is acceptable)
- Bulk SCIM operations (RFC 7644 Section 3.7) - initial version uses individual requests
- SCIM schema discovery endpoints (ServiceProviderConfig, Schemas, ResourceTypes)
- Password synchronization to downstream apps
- Manual push endpoints (event-driven only)

## Decisions

### Decision 1: Pluggable Event Processing Interface

**What**: Define a `ScimEventProcessor` interface with multiple implementations. Default is `@Scheduled` polling; stubs provided for CDI Events, Kafka, and AMQP.

**Why**:
- Allows starting simple (polling) while enabling future scalability
- Educational value in showing different integration patterns
- No infrastructure dependencies for default implementation

**Interface**:
```kotlin
interface ScimEventProcessor {
    suspend fun processEvent(event: Event)
    fun start()
    fun stop()
}
```

**Implementations**:
| Implementation | Description | Status |
|----------------|-------------|--------|
| `ScheduledScimEventProcessor` | Quarkus `@Scheduled` polls for unprocessed events | Default |
| `CdiScimEventProcessor` | CDI `@Observes` for immediate processing + polling fallback | Stub |
| `KafkaScimEventProcessor` | Consumes from Kafka topic | Stub |
| `AmqpScimEventProcessor` | Consumes from AMQP queue | Stub |

**Selection**: Via configuration property `revet.scim.processor=scheduled|cdi|kafka|amqp`

### Decision 2: Event System Integration via CDI Events

**What**: Extend EventRepository to fire CDI events after persisting. ScimEventProcessor implementations can observe these events.

**Why**:
- Decouples SCIM processing from core event persistence
- Standard CDI pattern, no external dependencies
- Enables both synchronous observation and async processing

**Flow**:
```
UserService.createUser()
    → EventRepository.createUserProfileEvent()
        → persist Event to database
        → fire CDI Event: ScimRelevantEvent(event)
            → ScimEventProcessor.processEvent() [if CdiScimEventProcessor]
    → return User

[Async] ScheduledScimEventProcessor polls for unprocessed events
```

**CDI Event**:
```kotlin
data class ScimRelevantEvent(
    val event: Event,
    val resourceType: ResourceType
)
```

### Decision 3: Kotlin Coroutines for Parallel Delivery

**What**: When an event triggers SCIM delivery to multiple ScimApplications, use Kotlin coroutines to push in parallel.

**Why**:
- Kotlin coroutines already available in project
- Structured concurrency with proper error handling
- Non-blocking, efficient resource usage
- Cleaner than ExecutorService

**Implementation**:
```kotlin
suspend fun processEvent(event: Event) = coroutineScope {
    val scimApps = scimApplicationService.getEnabledForAuthServer(event.authorizationServerId)

    scimApps.map { scimApp ->
        async {
            try {
                deliverToScimApplication(event, scimApp)
            } catch (e: Exception) {
                // Log and create FAILED delivery status - don't fail other deliveries
                createFailedDeliveryStatus(event, scimApp, e)
            }
        }
    }.awaitAll()
}
```

**Error Isolation**: Each ScimApplication delivery is independent. One failure doesn't affect others.

### Decision 4: Simplified Audit via Event + ScimDeliveryStatus

**What**: Leverage existing Event table for audit. Only track delivery status in a separate lightweight table.

**Why**:
- Event already captures full resource state (no payload duplication)
- Consistent audit trail with other resources
- ScimDeliveryStatus only tracks what's SCIM-specific

**Removed**: `ScimProvisioningLog` with duplicated request/response bodies

**New Model**:
```kotlin
data class ScimDeliveryStatus(
    var id: UUID,
    var eventId: UUID,                    // FK to Event - contains full audit info
    var scimApplicationId: UUID,          // FK to ScimApplication
    var status: ScimDeliveryStatus,       // PENDING, IN_PROGRESS, SUCCESS, FAILED, RETRYING
    var scimResourceId: String?,          // External ID returned by downstream
    var httpStatus: Int?,                 // Response status code
    var retryCount: Int,
    var nextRetryAt: OffsetDateTime?,
    var lastError: String?,               // Brief error message
    var createdOn: OffsetDateTime,
    var completedOn: OffsetDateTime?
)
```

**Audit Query**: Join Event + ScimDeliveryStatus for full audit trail:
```sql
SELECT e.*, s.status, s.http_status, s.last_error
FROM events e
JOIN scim_delivery_status s ON s.event_id = e.id
WHERE s.scim_application_id = ?
```

### Decision 5: Per-AuthorizationServer SCIM Application Configuration

**What**: ScimApplication entities belong to an AuthorizationServer, allowing multi-tenant SCIM configurations.

**Why**:
- Matches existing domain model (Users, Groups, Clients all belong to AuthorizationServer)
- Different tenants may have different downstream applications

### Decision 6: SCIM Schema Attribute Mapping

**What**: Store attribute mappings as JSON in ScimApplication, mapping Revet Auth fields to SCIM attributes.

**Why**:
- Different downstream apps may require different attribute mappings
- SCIM schema is flexible; not all apps use the same attributes
- JSON storage aligns with existing patterns (Event.resource, Metadata)

**Default mapping** (OIDC UserInfo to SCIM Core User):
```json
{
  "userName": "$.username",
  "name.givenName": "$.profile.given_name",
  "name.familyName": "$.profile.family_name",
  "emails[0].value": "$.email",
  "active": "$.active"
}
```

### Decision 7: JWT-Based Authentication via Application

**What**: Each ScimApplication links to an Application entity. Outbound SCIM requests use JWTs generated with the Application's credentials. Downstream consumers validate JWTs against the authorization server's JWKS endpoint.

**Why**:
- Leverages existing Application/ApplicationSecret infrastructure
- No need to store external credentials - Revet Auth is the token issuer
- Downstream systems can validate tokens using standard JWKS validation
- Consistent with OAuth2 patterns (Client Credentials flow conceptually)

**Authentication flow**:
```
ScimApplication → Application → Generate JWT with SCIM scopes → Bearer token in HTTP request
                                     ↓
Downstream SCIM Server → Validate JWT against Authorization Server's JWKS
```

### Decision 8: SCIM OAuth Scopes

**What**: Define granular SCIM scopes following industry patterns, created automatically per authorization server.

**Scopes**:
| Scope | Description |
|-------|-------------|
| `scim:users:read` | Query user resources |
| `scim:users:write` | Create, update, delete users |
| `scim:groups:read` | Query group resources |
| `scim:groups:write` | Create, update, delete groups |

**Why**:
- Follows patterns from [Janssen](https://docs.jans.io/v1.1.4/admin/usermgmt/usermgmt-scim/)
- Enables fine-grained access control at the downstream consumer
- Uses colon separator (`scim:users:read`) consistent with common OAuth patterns

### Decision 9: Automatic Application Provisioning

**What**: When creating a ScimApplication, optionally auto-create an Application with SCIM scopes if an existing Application is not specified.

**Behavior**:
```
POST /scim-applications
  ├─ applicationId provided → Validate Application has required scopes
  └─ applicationId NOT provided → Create Application with name "{scimAppName} SCIM Client"
                                   Assign SCIM scopes based on enabledOperations
                                   Create ApplicationSecret
                                   Return credentials in response
```

### Decision 10: Deactivation vs Deletion

**What**: By default, user deletion in Revet Auth triggers `active=false` PATCH in downstream (Okta's behavior). Hard delete is configurable per ScimApplication.

**Why**:
- Okta's default behavior - maintains audit trail in downstream
- Some apps don't support DELETE, only deactivation
- Configurable for apps that require hard deletion

### Decision 11: Retry Mechanism

**What**: Exponential backoff with configurable max retries per ScimApplication.

**Default RetryPolicy**:
```kotlin
data class RetryPolicy(
    val maxRetries: Int = 5,
    val initialBackoffMs: Long = 1000,      // 1 second
    val maxBackoffMs: Long = 300000,        // 5 minutes
    val backoffMultiplier: Double = 2.0
)
```

**Backoff Calculation**:
```kotlin
fun calculateBackoff(retryCount: Int, policy: RetryPolicy): Long {
    val backoff = policy.initialBackoffMs * policy.backoffMultiplier.pow(retryCount)
    return minOf(backoff.toLong(), policy.maxBackoffMs)
}
```

**Retry Flow**:
```
Attempt 1 fails → status=RETRYING, nextRetryAt=now+1s, retryCount=1
Attempt 2 fails → status=RETRYING, nextRetryAt=now+2s, retryCount=2
Attempt 3 fails → status=RETRYING, nextRetryAt=now+4s, retryCount=3
Attempt 4 fails → status=RETRYING, nextRetryAt=now+8s, retryCount=4
Attempt 5 fails → status=RETRYING, nextRetryAt=now+16s, retryCount=5
Attempt 6 fails → status=FAILED (max retries exceeded)
```

## Data Model

### ScimApplication
```kotlin
data class ScimApplication(
    var id: UUID,
    var authorizationServerId: UUID,
    var applicationId: UUID,                   // Link to Application for JWT auth
    var name: String,                          // Human-readable name
    var baseUrl: String,                       // SCIM endpoint base URL
    var attributeMapping: Map<String, String>, // Revet → SCIM attribute mapping
    var enabledOperations: Set<ScimOperation>, // CREATE_USER, UPDATE_USER, etc.
    var deleteAction: ScimDeleteAction,        // DEACTIVATE (default), DELETE
    var retryPolicy: RetryPolicy,              // maxRetries, backoffMs
    var enabled: Boolean,
    var createdOn: OffsetDateTime,
    var updatedOn: OffsetDateTime
)
```

### ScimDeliveryStatus
```kotlin
data class ScimDeliveryStatus(
    var id: UUID,
    var eventId: UUID,                         // FK to Event (audit trail)
    var scimApplicationId: UUID,               // FK to ScimApplication
    var status: ScimProvisioningStatus,        // PENDING, IN_PROGRESS, SUCCESS, FAILED, RETRYING
    var scimResourceId: String?,               // External ID from downstream
    var httpStatus: Int?,                      // Response status code
    var retryCount: Int,
    var nextRetryAt: OffsetDateTime?,
    var lastError: String?,                    // Brief error message
    var createdOn: OffsetDateTime,
    var completedOn: OffsetDateTime?
)
```

### ScimResourceMapping
```kotlin
data class ScimResourceMapping(
    var id: UUID,
    var scimApplicationId: UUID,
    var localResourceType: ResourceType,
    var localResourceId: UUID,
    var scimResourceId: String,                // ID returned by downstream SCIM server
    var createdOn: OffsetDateTime
)
```

### Enums
```kotlin
enum class ScimDeleteAction { DEACTIVATE, DELETE }

enum class ScimOperation {
    CREATE_USER, UPDATE_USER, DEACTIVATE_USER, DELETE_USER,
    CREATE_GROUP, UPDATE_GROUP, DELETE_GROUP,
    ADD_GROUP_MEMBER, REMOVE_GROUP_MEMBER
}

enum class ScimProvisioningStatus { PENDING, IN_PROGRESS, SUCCESS, FAILED, RETRYING }
```

### SCIM Scopes (created in Scope table)
```kotlin
val SCIM_SCOPES = listOf(
    Scope(name = "scim:users:read", description = "Query SCIM user resources"),
    Scope(name = "scim:users:write", description = "Create, update, delete SCIM users"),
    Scope(name = "scim:groups:read", description = "Query SCIM group resources"),
    Scope(name = "scim:groups:write", description = "Create, update, delete SCIM groups")
)
```

## Event Processing Architecture

### Scheduled Processor (Default)

```kotlin
@ApplicationScoped
class ScheduledScimEventProcessor(
    private val scimDeliveryStatusRepository: ScimDeliveryStatusRepository,
    private val scimClient: ScimClient
) : ScimEventProcessor {

    @Scheduled(every = "5s")
    suspend fun pollAndProcess() = coroutineScope {
        val pending = scimDeliveryStatusRepository.findPendingOrRetryable()

        pending.groupBy { it.eventId }.forEach { (eventId, deliveries) ->
            deliveries.map { delivery ->
                async { processDelivery(delivery) }
            }.awaitAll()
        }
    }

    private suspend fun processDelivery(delivery: ScimDeliveryStatus) {
        // ... execute SCIM operation with fresh JWT
    }
}
```

### CDI Event Flow

```kotlin
// In EventRepository - fire CDI event after persist
@Inject
lateinit var scimEvent: Event<ScimRelevantEvent>

fun createUserProfileEvent(eventType: EventType, user: User, profile: Profile) {
    val event = // ... create and persist event

    if (eventType in listOf(CREATE, UPDATE, DELETE)) {
        scimEvent.fire(ScimRelevantEvent(event, ResourceType.USER))
    }
}

// CDI Observer (when using CdiScimEventProcessor)
@ApplicationScoped
class CdiScimEventProcessor : ScimEventProcessor {

    fun onScimEvent(@Observes event: ScimRelevantEvent) {
        // Immediately queue for processing
        runBlocking { processEvent(event.event) }
    }
}
```

## SCIM Protocol Mapping

| Revet Event | SCIM Operation | HTTP Method | Endpoint | Required Scope |
|-------------|----------------|-------------|----------|----------------|
| User CREATE | Create user | POST | /Users | `scim:users:write` |
| User UPDATE | Replace/patch user | PUT or PATCH | /Users/{id} | `scim:users:write` |
| User DELETE | Deactivate or delete | PATCH or DELETE | /Users/{id} | `scim:users:write` |
| Group CREATE | Create group | POST | /Groups | `scim:groups:write` |
| Group UPDATE | Replace/patch group | PUT or PATCH | /Groups/{id} | `scim:groups:write` |
| Group DELETE | Delete group | DELETE | /Groups/{id} | `scim:groups:write` |
| GroupMember CREATE | Add member | PATCH | /Groups/{id} | `scim:groups:write` |
| GroupMember DELETE | Remove member | PATCH | /Groups/{id} | `scim:groups:write` |

## JWT Token Format

Outbound SCIM requests include a JWT Bearer token with:

```json
{
  "iss": "https://auth.example.com",
  "sub": "{applicationId}",
  "aud": "{scimApplication.baseUrl}",
  "iat": 1234567890,
  "exp": 1234571490,
  "scope": "scim:users:write scim:groups:write",
  "client_id": "{applicationId}"
}
```

Downstream consumers validate:
1. Signature against JWKS at `{issuer}/.well-known/jwks.json`
2. Issuer matches expected authorization server
3. Expiration is in the future
4. Scopes include required permissions

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Downstream SCIM server unavailable | Retry with exponential backoff; don't block primary operations |
| Attribute mapping errors | Validate mapping on ScimApplication save; log detailed errors |
| Rate limiting by downstream | Configurable rate limiting per ScimApplication (future) |
| Event ordering issues | Process events in order per resource using event timestamps |
| JWT expiration during retry | Generate fresh JWT for each retry attempt |
| Application deleted while ScimApplication exists | Cascade delete or prevent Application deletion |
| Coroutine cancellation | Use SupervisorJob to prevent cascade failures |

## Migration Plan

1. Add CDI event firing to EventRepository for User/Group/GroupMember events
2. Add new database tables (`scim_applications`, `scim_delivery_status`, `scim_resource_mappings`)
3. Deploy code changes (no breaking changes to existing functionality)
4. SCIM scopes are created on-demand when first ScimApplication is configured per authorization server
5. Enable SCIM provisioning by creating ScimApplication configurations (optionally auto-creates Application)
6. No rollback concerns - new tables can be dropped without affecting existing functionality

## Configuration

```properties
# Event processor implementation (default: scheduled)
revet.scim.processor=scheduled

# Polling interval for scheduled processor
revet.scim.scheduled.interval=5s

# Default JWT lifetime
revet.scim.token.lifetime=3600

# Enable/disable SCIM processing globally
revet.scim.enabled=true
```

## Open Questions

1. **Rate limiting**: Should we implement configurable rate limiting per downstream application?
   - Recommendation: Start without, add if needed based on usage

2. **Bulk operations**: Should we support SCIM Bulk operations (RFC 7644 Section 3.7) for initial sync?
   - Recommendation: Defer to future enhancement; individual operations sufficient for MVP

3. **Token lifetime**: What should be the default JWT lifetime for SCIM tokens?
   - Recommendation: 1 hour (3600s), configurable per ScimApplication
