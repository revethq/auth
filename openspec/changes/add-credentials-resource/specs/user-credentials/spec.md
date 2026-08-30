## ADDED Requirements

### Requirement: Credential Resource
The system SHALL provide a top-level `Credential` resource representing an authentication credential. Each credential SHALL have an `id`, `authorizationServerId`, `type`, `status`, optional `name`, optional `expiresIn`, optional `scopes`, and timestamps (`createdOn`, `updatedOn`). Each credential SHALL belong to exactly one owner: either a user (via `userId`) or an application (via `applicationId`).

#### Scenario: Credential with user owner
- **WHEN** a credential is created with a `userId`
- **THEN** it SHALL be associated with that user and `applicationId` SHALL be null

#### Scenario: Credential with application owner
- **WHEN** a credential is created with an `applicationId`
- **THEN** it SHALL be associated with that application and `userId` SHALL be null

#### Scenario: Credential requires exactly one owner
- **WHEN** a credential is created with both `userId` and `applicationId` set, or with neither set
- **THEN** the system SHALL return a 400 error

### Requirement: Credential Types
The system SHALL support the following credential types via the `CredentialType` enum: `PASSWORD`, `API_KEY`, `TOTP`, `RECOVERY_CODE`.

#### Scenario: Valid credential types
- **WHEN** a credential is created with type `PASSWORD`, `API_KEY`, `TOTP`, or `RECOVERY_CODE`
- **THEN** the credential SHALL be accepted

#### Scenario: Invalid credential type
- **WHEN** a credential is created with an unrecognized type
- **THEN** the system SHALL return a 400 error

### Requirement: Credential Status
The system SHALL support the following credential statuses via the `CredentialStatus` enum: `ACTIVE`, `REVOKED`. New credentials SHALL default to `ACTIVE`.

#### Scenario: Default status on creation
- **WHEN** a credential is created
- **THEN** its status SHALL be `ACTIVE`

### Requirement: Create Credential
The system SHALL provide a `POST /api/v1/credentials` endpoint that accepts a `CredentialRequest` containing `authorizationServerId`, `type`, `value` (the plain credential), optional `userId`, optional `applicationId`, optional `name`, optional `scopes` (list of scope UUIDs), and optional `expiresIn`. The system SHALL hash the plain value before storage and return the plain value exactly once in the creation response.

#### Scenario: Create a password credential
- **WHEN** a POST request is made with type `PASSWORD` and a plain password value
- **THEN** the system SHALL hash the password using bcrypt, store only the hash, and return a response containing the credential metadata and the plain value

#### Scenario: Create an API key credential
- **WHEN** a POST request is made with type `API_KEY`
- **THEN** the system SHALL hash the value using SHA-256, store only the hash, and return the plain value in the response

#### Scenario: Create credential with scopes
- **WHEN** a POST request includes a list of scope UUIDs
- **THEN** the system SHALL associate those scopes with the credential via the scope reference table

#### Scenario: Plain value not returned after creation
- **WHEN** a GET request is made for an existing credential
- **THEN** the response SHALL NOT include the plain credential value

### Requirement: Single Active Password
The system SHALL enforce that only one credential of type `PASSWORD` with status `ACTIVE` exists per user per authorization server. Creating a new `PASSWORD` credential SHALL automatically set any existing active password credential for that user to `REVOKED`.

#### Scenario: New password replaces old
- **WHEN** a user already has an active PASSWORD credential and a new PASSWORD credential is created
- **THEN** the existing PASSWORD credential SHALL be set to status `REVOKED` and the new one SHALL be `ACTIVE`

### Requirement: Get Credential
The system SHALL provide a `GET /api/v1/credentials/{credentialId}` endpoint that returns credential metadata (id, userId, applicationId, type, status, name, scopes, timestamps) without the plain value or hash.

#### Scenario: Get credential by ID
- **WHEN** a GET request is made with a valid credential ID
- **THEN** the system SHALL return the credential metadata without the plain value

#### Scenario: Get non-existent credential
- **WHEN** a GET request is made with a non-existent credential ID
- **THEN** the system SHALL return a 404 error

### Requirement: List Credentials
The system SHALL provide a `GET /api/v1/credentials` endpoint that accepts optional `userId`, `applicationId`, and `type` query parameters for filtering, and standard `limit`/`offset` pagination.

#### Scenario: List credentials by user
- **WHEN** a GET request is made with a `userId` query parameter
- **THEN** the system SHALL return all credentials belonging to that user

#### Scenario: List credentials by application
- **WHEN** a GET request is made with an `applicationId` query parameter
- **THEN** the system SHALL return all credentials belonging to that application

#### Scenario: List credentials by type
- **WHEN** a GET request is made with a `type` query parameter
- **THEN** the system SHALL return only credentials of the specified type

### Requirement: Delete Credential
The system SHALL provide a `DELETE /api/v1/credentials/{credentialId}` endpoint that permanently removes a credential.

#### Scenario: Delete credential
- **WHEN** a DELETE request is made with a valid credential ID
- **THEN** the credential SHALL be permanently removed and the system SHALL return 204

#### Scenario: Delete non-existent credential
- **WHEN** a DELETE request is made with a non-existent credential ID
- **THEN** the system SHALL return a 404 error

### Requirement: Validate Credential
The `CredentialService` SHALL provide a `validate(credentialId: UUID, value: String): Boolean` method that verifies a plain credential value against the stored hash. For `PASSWORD` type credentials it SHALL use bcrypt comparison; for all other types it SHALL use SHA-256 comparison. The method SHALL return `false` if the credential does not exist, is not `ACTIVE`, or the value does not match.

#### Scenario: Valid password
- **WHEN** `validate` is called with a correct password for an active PASSWORD credential
- **THEN** the method SHALL return `true`

#### Scenario: Invalid password
- **WHEN** `validate` is called with an incorrect password
- **THEN** the method SHALL return `false`

#### Scenario: Revoked credential
- **WHEN** `validate` is called for a credential with status `REVOKED`
- **THEN** the method SHALL return `false`

### Requirement: Find Active Credential By User
The `CredentialService` SHALL provide a method to find the active credential of a given type for a user within an authorization server. This supports the OAuth authorization code flow where the system needs to locate a user's password credential for validation.

#### Scenario: Find active password for user
- **WHEN** `findActiveByUserAndType` is called with a userId, authorizationServerId, and type PASSWORD
- **THEN** the method SHALL return the active PASSWORD credential for that user, or null if none exists

### Requirement: Remove Password from User
The system SHALL NOT store password data on the User entity. The `password` field SHALL be removed from the `User` domain model, the `passwordHash` column SHALL be removed from the `users` database table, and the `setPassword`/`validatePassword` methods SHALL be removed from `UserService`.

#### Scenario: User creation without password
- **WHEN** a user is created via `POST /api/v1/users`
- **THEN** the request body SHALL NOT accept a `password` field; credentials SHALL be managed via the Credentials API

### Requirement: Remove ApplicationSecret Resource
The system SHALL remove the entire `ApplicationSecret` resource stack: domain model, JPA entity, DTOs, API interface (`/api/v1/applicationSecrets`), route handler, repository, mappers, validators, and related service methods on `ApplicationService`. Application credentials SHALL be managed via the Credentials API using type `API_KEY` with `applicationId` set.

#### Scenario: Application secret replaced by credential
- **WHEN** an application needs a secret for the client_credentials flow
- **THEN** the secret SHALL be created via `POST /api/v1/credentials` with `applicationId` set, type `API_KEY`, and the desired scopes

### Requirement: OAuth Login Uses Credential Service
The OAuth 2.1 authorization code flow SHALL validate user passwords using `CredentialService` instead of `UserService.validatePassword`. The system SHALL find the user's active PASSWORD credential and validate the provided password against it.

#### Scenario: Login with password credential
- **WHEN** a user submits credentials during the OAuth authorization code flow
- **THEN** the system SHALL find the active PASSWORD credential for that user and authorization server, then validate the password against it

### Requirement: Client Credentials Flow Uses Credential Service
The OAuth 2.1 client_credentials flow SHALL validate application secrets using `CredentialService` instead of `ApplicationService.isApplicationSecretValid`. The credential ID replaces the application secret ID in the request. Scope filtering SHALL use the credential's associated scopes.

#### Scenario: Client credentials with credential
- **WHEN** a client_credentials token request is made with a credential ID and secret value
- **THEN** the system SHALL validate the secret via `CredentialService.validate` and filter requested scopes to those assigned to the credential
