## ADDED Requirements

### Requirement: Service Account CRUD
The system SHALL provide a REST API at `/api/v1/service-accounts` for creating, reading, updating, listing, and deleting service accounts. Service accounts use the IAM `ServiceAccount` domain model and `revet_service_accounts` table directly, with auth-specific scope assignment and profile association.

#### Scenario: Create a service account
- **WHEN** a POST request is made to `/api/v1/service-accounts` with a valid name, authorizationServerId, and optional description
- **THEN** a new service account is created with a generated UUID, tenantId set to the authorizationServerId, and timestamps set to the current time
- **AND** the response includes the service account details with status 201

#### Scenario: Create a service account with a profile
- **WHEN** a POST request is made to `/api/v1/service-accounts` with a profile object
- **THEN** a Profile is created with profileType `SERVICE_ACCOUNT` linked to the service account ID
- **AND** the profile is validated against the authorization server's OIDC schema

#### Scenario: Create a service account with scopes
- **WHEN** a POST request is made to `/api/v1/service-accounts` with a list of scope IDs
- **THEN** the service account is associated with the specified scopes via a join table
- **AND** only scopes belonging to the same authorization server are accepted

#### Scenario: Get a service account by ID
- **WHEN** a GET request is made to `/api/v1/service-accounts/{serviceAccountId}`
- **THEN** the service account is returned with its profile and scope associations
- **AND** the response status is 200

#### Scenario: Get a non-existent service account
- **WHEN** a GET request is made to `/api/v1/service-accounts/{serviceAccountId}` with an unknown ID
- **THEN** a 404 response is returned

#### Scenario: List service accounts
- **WHEN** a GET request is made to `/api/v1/service-accounts` with optional `authorizationServerIds`, `limit`, and `offset` query parameters
- **THEN** a paginated list of service accounts is returned filtered by the specified authorization server IDs

#### Scenario: Update a service account
- **WHEN** a PUT request is made to `/api/v1/service-accounts/{serviceAccountId}` with updated fields
- **THEN** the service account's name, description, profile, and scopes are updated
- **AND** the updatedOn timestamp is set to the current time

#### Scenario: Delete a service account
- **WHEN** a DELETE request is made to `/api/v1/service-accounts/{serviceAccountId}`
- **THEN** the service account, its profile, and its scope associations are deleted
- **AND** the response status is 204

#### Scenario: Delete a non-existent service account
- **WHEN** a DELETE request is made to `/api/v1/service-accounts/{serviceAccountId}` with an unknown ID
- **THEN** a 404 response is returned

### Requirement: Service Account tenantId Mapping
The system SHALL map `ServiceAccount.tenantId` to the authorization server's ID. All service account queries filtered by authorizationServerId SHALL use tenantId for the underlying IAM lookup.

#### Scenario: tenantId set on creation
- **WHEN** a service account is created with authorizationServerId `abc-123`
- **THEN** the IAM ServiceAccount's tenantId is set to `"abc-123"`

#### Scenario: Filtering by authorizationServerId
- **WHEN** service accounts are listed with authorizationServerIds filter
- **THEN** the query filters by tenantId matching the provided authorization server IDs

## REMOVED Requirements

### Requirement: Application CRUD
**Reason**: The Application resource is replaced by Service Account. All application management endpoints at `/api/v1/applications` are removed.
**Migration**: Existing Application data should be migrated to the `revet_service_accounts` table. The Application's `id`, `name`, `authorizationServerId` (as tenantId), and `metadata` map directly to ServiceAccount fields.
