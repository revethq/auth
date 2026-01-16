# Design: Integration Tests for All Resources

## Test Architecture

### Test Infrastructure

```
web/src/test/
├── kotlin/com/revethq/auth/web/
│   ├── api/routes/                    # Resource integration tests
│   │   ├── AuthorizationServersTest.kt
│   │   ├── UsersTest.kt
│   │   ├── ApplicationsTest.kt
│   │   ├── ApplicationSecretsTest.kt
│   │   ├── ClientsTest.kt
│   │   ├── GroupsTest.kt
│   │   ├── GroupMembersTest.kt
│   │   ├── ScopesTest.kt
│   │   ├── SchemasTest.kt
│   │   └── TemplatesTest.kt
│   └── scim/
│       └── ScimApplicationResourceTest.kt  # Already exists
└── resources/
    └── application.properties         # DevServices config (already exists)
```

### Test Class Structure

Each test class follows a consistent pattern based on the existing `ScimApplicationResourceTest`:

```kotlin
@QuarkusTest
class <Resource>Test {

    @Inject
    lateinit var authorizationServerService: AuthorizationServerService  // For fixtures

    private var authServerId: UUID? = null

    @BeforeEach
    fun setUp() {
        // Create prerequisite entities (e.g., authorization server)
    }

    // CRUD Tests
    @Test fun `POST creates <entity>`()
    @Test fun `POST returns 400 for invalid request`()
    @Test fun `GET retrieves <entity> by ID`()
    @Test fun `GET returns 404 for non-existent <entity>`()
    @Test fun `GET lists <entities> with pagination`()
    @Test fun `PUT updates <entity>`()
    @Test fun `PUT returns 404 for non-existent <entity>`()
    @Test fun `DELETE removes <entity>`()
    @Test fun `DELETE returns 404 for non-existent <entity>`()

    // Helper method for creating test entities
    private fun create<Entity>(name: String): String { ... }
}
```

## Entity Dependencies

Many resources have parent-child relationships requiring fixture setup:

```
AuthorizationServer (root)
├── User
├── Application
│   └── ApplicationSecret
├── Client
├── Group
│   └── GroupMember (User + Group)
├── Scope
├── Schema
└── Template
```

### Test Execution Order

Tests within a class should be independent. Use `@BeforeEach` to create required fixtures:

1. **AuthorizationServersTest** - No dependencies (root entity)
2. **UsersTest** - Requires AuthorizationServer
3. **ApplicationsTest** - Requires AuthorizationServer
4. **ApplicationSecretsTest** - Requires Application (→ AuthorizationServer)
5. **ClientsTest** - Requires AuthorizationServer
6. **GroupsTest** - Requires AuthorizationServer
7. **GroupMembersTest** - Requires Group + User (→ AuthorizationServer)
8. **ScopesTest** - Requires AuthorizationServer
9. **SchemasTest** - Requires AuthorizationServer
10. **TemplatesTest** - Requires AuthorizationServer

## Exception Handling

### Required Exception Mappers

For proper 404 responses, each NotFound exception needs a mapper:

| Exception | Mapper Required |
|-----------|-----------------|
| `AuthorizationServerNotFound` | Yes |
| `UserNotFound` | Yes |
| `ApplicationNotFound` | Yes |
| `ApplicationSecretNotFound` | Yes |
| `ClientNotFound` | Yes |
| `GroupNotFound` | Yes |
| `GroupMemberNotFound` | Yes |
| `ScopeNotFound` | Yes |
| `SchemaNotFound` | Yes |
| `TemplateNotFound` | Yes |
| `ScimApplicationNotFound` | Already exists |

### Generic NotFound Mapper Option

Instead of individual mappers, create a base exception class:

```kotlin
// core module
abstract class NotFoundException(message: String) : RuntimeException(message)

class UserNotFound : NotFoundException("The User was not found")
// ... other NotFound exceptions extend NotFoundException

// web module
@Provider
class NotFoundExceptionMapper : ExceptionMapper<NotFoundException> {
    override fun toResponse(exception: NotFoundException): Response {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to exception.message))
            .build()
    }
}
```

## Test Data Management

### Approach: Fresh Data Per Test

- Use `@BeforeEach` to create fresh fixtures
- DevServices provides clean database via `drop-and-create`
- No explicit cleanup needed (database recreated per test run)

### Unique Naming

Include UUID in entity names to avoid conflicts:

```kotlin
val authServer = AuthorizationServer(
    name = "Test Auth Server ${UUID.randomUUID()}",
    ...
)
```

## Performance Considerations

### Test Parallelization

- Tests within the same `@QuarkusTest` class run sequentially
- Multiple test classes could run in parallel with Gradle configuration
- Initial implementation: sequential execution (safer)

### Database Reuse

DevServices reuses the PostgreSQL container across test runs when:
```properties
testcontainers.reuse.enable=true  # in ~/.testcontainers.properties
```

Not required but improves local test iteration speed.

## Validation Coverage

Each resource test should validate:

1. **Happy Path**: CRUD operations succeed
2. **Not Found**: 404 for non-existent entities
3. **Validation**: 400 for invalid requests (missing required fields)
4. **Pagination**: List endpoints respect limit/offset
5. **Filtering**: List endpoints filter by authorizationServerId where applicable
