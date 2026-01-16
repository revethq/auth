# Integration Testing Capability

## ADDED Requirements

### Requirement: REST Resource Integration Tests

All REST API resources SHALL have integration tests verifying CRUD operations, error handling, and pagination.

#### Scenario: AuthorizationServers CRUD operations
- **Given** a running Quarkus application with DevServices PostgreSQL
- **When** CRUD operations are performed on `/authorizationServers`
- **Then** create returns 200 with the created entity
- **And** get by ID returns 200 for existing entities
- **And** get by ID returns 404 for non-existent entities
- **And** list returns 200 with paginated results
- **And** update returns 200 with updated entity
- **And** delete returns 204 and removes the entity

#### Scenario: Users CRUD operations with authorization server filtering
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/users`
- **Then** create returns 200 with the created user
- **And** list supports filtering by `authorizationServerIds`
- **And** get/update/delete handle 404 for non-existent users

#### Scenario: Applications CRUD operations
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/applications`
- **Then** create returns 200 with the created application
- **And** list supports filtering by `authorizationServerIds`
- **And** get/delete handle 404 for non-existent applications

#### Scenario: ApplicationSecrets CRUD operations
- **Given** an application exists
- **When** CRUD operations are performed on `/applicationSecrets`
- **Then** create returns 200 with the created secret
- **And** list supports filtering by `applicationIds`
- **And** get/delete handle 404 for non-existent secrets

#### Scenario: Clients CRUD operations
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/clients`
- **Then** create returns 200 with the created client
- **And** list supports filtering by `authorizationServerIds`
- **And** get/update/delete handle 404 for non-existent clients

#### Scenario: Groups CRUD operations
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/groups`
- **Then** create returns 200 with the created group
- **And** list supports filtering by `authorizationServerIds`
- **And** get/update/delete handle 404 for non-existent groups

#### Scenario: GroupMembers CRUD operations
- **Given** a group and user exist
- **When** CRUD operations are performed on group members
- **Then** add member returns 200
- **And** list members returns members of the group
- **And** remove member returns 204 and removes the membership

#### Scenario: Scopes CRUD operations
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/scopes`
- **Then** create returns 200 with the created scope
- **And** list supports filtering by `authorizationServerIds`
- **And** get/update/delete handle 404 for non-existent scopes

#### Scenario: Schemas CRUD operations
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/schemas`
- **Then** create returns 200 with the created schema
- **And** list supports filtering by `authorizationServerIds`
- **And** get/update/delete handle 404 for non-existent schemas

#### Scenario: Templates CRUD operations
- **Given** an authorization server exists
- **When** CRUD operations are performed on `/templates`
- **Then** create returns 200 with the created template
- **And** list supports filtering by `authorizationServerIds`
- **And** get/update/delete handle 404 for non-existent templates

### Requirement: NotFound Exception Handling

All NotFound exceptions SHALL be mapped to HTTP 404 responses with error details.

#### Scenario: Generic NotFound exception mapping
- **Given** a NotFound exception is thrown by a service
- **When** the exception propagates to the REST layer
- **Then** the response status is 404 NOT_FOUND
- **And** the response body contains an error message

#### Scenario: NotFound exceptions extend base class
- **Given** a NotFound exception class exists (e.g., `UserNotFound`)
- **When** the exception is created
- **Then** it extends the base `NotFoundException` class
- **And** includes a descriptive message

### Requirement: Test Infrastructure

Integration tests SHALL use Quarkus DevServices for database provisioning.

#### Scenario: DevServices PostgreSQL for tests
- **Given** integration tests are executed with `./gradlew test`
- **When** the test context starts
- **Then** Quarkus DevServices provisions a PostgreSQL container
- **And** the database schema is created fresh (`drop-and-create`)
- **And** tests can inject services for fixture setup
