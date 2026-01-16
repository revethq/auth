# SCIM Outbound Provisioning

Provides SCIM v2 client capabilities for provisioning users and groups to downstream applications.

## ADDED Requirements

### Requirement: SCIM Scopes

The system SHALL define standard OAuth scopes for SCIM access control that downstream consumers can use to validate permissions.

The following scopes SHALL be available:
- `scim:users:read` - Query SCIM user resources
- `scim:users:write` - Create, update, delete SCIM users
- `scim:groups:read` - Query SCIM group resources
- `scim:groups:write` - Create, update, delete SCIM groups

#### Scenario: SCIM scopes created on first SCIM application
- **WHEN** the first SCIM application is created for an authorization server
- **AND** the SCIM scopes do not exist for that authorization server
- **THEN** the system SHALL create all four SCIM scopes
- **AND** the scopes SHALL be available for assignment to Applications

#### Scenario: SCIM scopes already exist
- **WHEN** a SCIM application is created for an authorization server
- **AND** the SCIM scopes already exist for that authorization server
- **THEN** the system SHALL NOT create duplicate scopes

---

### Requirement: SCIM Application Configuration

The system SHALL allow administrators to configure downstream SCIM v2 service provider applications per authorization server.

Each SCIM application configuration MUST include:
- A unique name within the authorization server
- A base URL for the SCIM endpoint
- An associated Application for JWT-based authentication
- Attribute mapping from Revet Auth to SCIM schema
- Enabled operations (which lifecycle events trigger provisioning)
- Delete action preference (deactivate vs hard delete)
- Retry policy configuration

#### Scenario: Create SCIM application with existing Application
- **WHEN** an administrator creates a SCIM application with an existing Application ID
- **AND** the Application has the required SCIM scopes for the enabled operations
- **THEN** the system SHALL store the configuration linked to the Application
- **AND** the configuration SHALL be associated with the specified authorization server

#### Scenario: Create SCIM application with existing Application missing scopes
- **WHEN** an administrator creates a SCIM application with an existing Application ID
- **AND** the Application does NOT have the required SCIM scopes for the enabled operations
- **THEN** the system SHALL reject the request with an error
- **AND** the error SHALL indicate which scopes are missing

#### Scenario: Create SCIM application with auto-created Application
- **WHEN** an administrator creates a SCIM application without specifying an Application ID
- **THEN** the system SHALL create a new Application with name "{scimAppName} SCIM Client"
- **AND** the system SHALL assign SCIM scopes based on the enabled operations
- **AND** the system SHALL create an ApplicationSecret for the new Application
- **AND** the response SHALL include the Application credentials

#### Scenario: Update SCIM application configuration
- **WHEN** an administrator updates a SCIM application configuration
- **THEN** the system SHALL apply the new configuration to subsequent provisioning operations
- **AND** in-flight operations SHALL complete with the previous configuration

#### Scenario: Delete SCIM application configuration
- **WHEN** an administrator deletes a SCIM application configuration
- **THEN** the system SHALL stop provisioning to that application
- **AND** pending provisioning operations for that application SHALL be cancelled

---

### Requirement: JWT-Based Authentication

The system SHALL authenticate outbound SCIM requests using JWTs issued by the authorization server, allowing downstream consumers to validate tokens against the JWKS endpoint.

#### Scenario: JWT generated for SCIM request
- **WHEN** the system sends a SCIM request to a downstream application
- **THEN** the system SHALL generate a JWT signed with the authorization server's signing key
- **AND** the JWT SHALL include the Application ID as the subject and client_id
- **AND** the JWT SHALL include the SCIM scopes based on the enabled operations
- **AND** the JWT SHALL include the SCIM application's base URL as the audience

#### Scenario: Fresh JWT for each retry
- **WHEN** a SCIM operation is retried after a failure
- **THEN** the system SHALL generate a fresh JWT for the retry attempt
- **AND** the new JWT SHALL have a new expiration time

#### Scenario: Downstream validation against JWKS
- **WHEN** a downstream consumer receives a SCIM request with a JWT Bearer token
- **THEN** the downstream consumer can validate the JWT signature against the authorization server's JWKS endpoint
- **AND** the downstream consumer can verify the scopes in the token match the required permissions

---

### Requirement: Event-Driven Processing

The system SHALL process SCIM operations via event-driven architecture, triggered by lifecycle events from the existing Event system.

#### Scenario: CDI event fired on resource change
- **WHEN** a User, Group, or GroupMember event is persisted to the Event table
- **THEN** the system SHALL fire a CDI event to notify SCIM processors
- **AND** the CDI event SHALL contain the Event and resource type

#### Scenario: Pluggable event processor
- **WHEN** the system is configured with an event processor type
- **THEN** the system SHALL use the configured processor implementation
- **AND** the default processor SHALL be a scheduled poller

#### Scenario: Multiple processor implementations available
- **WHEN** the system starts
- **THEN** the following processor implementations SHALL be available:
  - Scheduled polling (default)
  - CDI Events with polling fallback (stub)
  - Kafka consumer (stub)
  - AMQP consumer (stub)

---

### Requirement: Parallel Delivery

The system SHALL deliver SCIM operations to multiple downstream applications in parallel using Kotlin coroutines.

#### Scenario: Event triggers multiple deliveries
- **WHEN** a lifecycle event occurs
- **AND** multiple SCIM applications are configured for the authorization server
- **THEN** the system SHALL create a delivery status entry for each SCIM application
- **AND** deliveries SHALL be processed in parallel using coroutines

#### Scenario: Delivery failure isolation
- **WHEN** a SCIM delivery to one application fails
- **THEN** the failure SHALL NOT affect deliveries to other applications
- **AND** each delivery SHALL be independently retried

---

### Requirement: User Provisioning

The system SHALL provision user lifecycle events to configured downstream SCIM applications.

#### Scenario: User creation triggers SCIM POST
- **WHEN** a user is created in Revet Auth
- **AND** a SCIM application is configured with CREATE_USER enabled
- **THEN** the system SHALL send a POST request to the downstream /Users endpoint
- **AND** the request body SHALL contain the user attributes mapped to SCIM schema
- **AND** the system SHALL store the returned SCIM resource ID for future operations

#### Scenario: User update triggers SCIM PUT or PATCH
- **WHEN** a user's attributes are updated in Revet Auth
- **AND** a SCIM application is configured with UPDATE_USER enabled
- **THEN** the system SHALL send a PUT or PATCH request to /Users/{scimId}
- **AND** the request body SHALL contain the updated attributes mapped to SCIM schema

#### Scenario: User deletion triggers deactivation by default
- **WHEN** a user is deleted in Revet Auth
- **AND** a SCIM application is configured with DELETE_USER enabled
- **AND** the delete action is set to DEACTIVATE
- **THEN** the system SHALL send a PATCH request to /Users/{scimId}
- **AND** the request body SHALL set the active attribute to false

#### Scenario: User deletion triggers hard delete when configured
- **WHEN** a user is deleted in Revet Auth
- **AND** a SCIM application is configured with DELETE_USER enabled
- **AND** the delete action is set to DELETE
- **THEN** the system SHALL send a DELETE request to /Users/{scimId}

---

### Requirement: Group Provisioning

The system SHALL provision group lifecycle events to configured downstream SCIM applications.

#### Scenario: Group creation triggers SCIM POST
- **WHEN** a group is created in Revet Auth
- **AND** a SCIM application is configured with CREATE_GROUP enabled
- **THEN** the system SHALL send a POST request to the downstream /Groups endpoint
- **AND** the request body SHALL contain the group attributes mapped to SCIM schema
- **AND** the system SHALL store the returned SCIM resource ID for future operations

#### Scenario: Group update triggers SCIM PUT or PATCH
- **WHEN** a group's attributes are updated in Revet Auth
- **AND** a SCIM application is configured with UPDATE_GROUP enabled
- **THEN** the system SHALL send a PUT or PATCH request to /Groups/{scimId}
- **AND** the request body SHALL contain the updated attributes mapped to SCIM schema

#### Scenario: Group deletion triggers SCIM DELETE
- **WHEN** a group is deleted in Revet Auth
- **AND** a SCIM application is configured with DELETE_GROUP enabled
- **THEN** the system SHALL send a DELETE request to /Groups/{scimId}

---

### Requirement: Group Membership Provisioning

The system SHALL provision group membership changes to configured downstream SCIM applications using SCIM PATCH operations.

#### Scenario: Adding user to group triggers member add
- **WHEN** a user is added to a group in Revet Auth
- **AND** a SCIM application is configured with ADD_GROUP_MEMBER enabled
- **AND** both the user and group have been provisioned to the SCIM application
- **THEN** the system SHALL send a PATCH request to /Groups/{scimId}
- **AND** the request body SHALL contain an add operation for the members attribute

#### Scenario: Removing user from group triggers member remove
- **WHEN** a user is removed from a group in Revet Auth
- **AND** a SCIM application is configured with REMOVE_GROUP_MEMBER enabled
- **AND** both the user and group have been provisioned to the SCIM application
- **THEN** the system SHALL send a PATCH request to /Groups/{scimId}
- **AND** the request body SHALL contain a remove operation for the members attribute

#### Scenario: Membership change skipped for unprovisioned resources
- **WHEN** a user is added to or removed from a group
- **AND** either the user or group has not been provisioned to the SCIM application
- **THEN** the system SHALL NOT send a membership change request
- **AND** the system SHALL log that the operation was skipped

---

### Requirement: Attribute Mapping

The system SHALL support configurable attribute mapping from Revet Auth user and group models to SCIM schema.

#### Scenario: Default user attribute mapping
- **WHEN** no custom attribute mapping is configured for a SCIM application
- **THEN** the system SHALL use default mappings:
  - username → userName
  - email → emails[0].value
  - profile.given_name → name.givenName
  - profile.family_name → name.familyName

#### Scenario: Custom attribute mapping
- **WHEN** a custom attribute mapping is configured for a SCIM application
- **THEN** the system SHALL use the custom mapping to construct SCIM request bodies
- **AND** source values SHALL be extracted using JSONPath expressions

#### Scenario: Default group attribute mapping
- **WHEN** no custom group attribute mapping is configured
- **THEN** the system SHALL use default mappings:
  - displayName → displayName
  - externalId → externalId

---

### Requirement: Reliable Delivery

The system SHALL guarantee at-least-once delivery of SCIM operations using delivery status tracking.

#### Scenario: Delivery status created for each operation
- **WHEN** a lifecycle event triggers a SCIM operation
- **THEN** the system SHALL create a delivery status entry referencing the Event
- **AND** the delivery status SHALL be set to PENDING
- **AND** the primary operation (user/group create/update/delete) SHALL complete successfully regardless of SCIM delivery status

#### Scenario: Failed operation retried with backoff
- **WHEN** a SCIM operation fails due to network error or downstream unavailability
- **THEN** the system SHALL update the delivery status to RETRYING
- **AND** the system SHALL calculate the next retry time using exponential backoff
- **AND** the retry count SHALL be incremented

#### Scenario: Operation marked failed after max retries
- **WHEN** a SCIM operation has been retried the maximum number of times
- **AND** the operation still fails
- **THEN** the system SHALL update the delivery status to FAILED
- **AND** the error message SHALL be recorded

#### Scenario: Successful operation recorded
- **WHEN** a SCIM operation completes successfully
- **THEN** the system SHALL update the delivery status to SUCCESS
- **AND** the HTTP response status SHALL be recorded
- **AND** the SCIM resource ID from downstream SHALL be stored

---

### Requirement: Delivery Status Audit

The system SHALL provide visibility into SCIM delivery status by leveraging the existing Event table for audit data and a lightweight delivery status table for SCIM-specific tracking.

#### Scenario: Query delivery status by application
- **WHEN** an administrator queries delivery statuses for a SCIM application
- **THEN** the system SHALL return statuses filtered by the specified application
- **AND** statuses SHALL include the referenced Event for full audit details

#### Scenario: Query delivery status by status
- **WHEN** an administrator queries delivery statuses with a status filter
- **THEN** the system SHALL return only statuses matching the specified filter

#### Scenario: Query delivery status by resource
- **WHEN** an administrator queries delivery statuses for a specific user or group
- **THEN** the system SHALL return all delivery statuses for events related to that resource
