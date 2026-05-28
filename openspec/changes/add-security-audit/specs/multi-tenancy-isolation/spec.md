## ADDED Requirements

### Requirement: Cross-Tenant Data Access Audit
The audit SHALL verify that data belonging to one authorization server cannot be accessed or modified through another authorization server's context.

#### Scenario: User isolation
- **WHEN** user CRUD and query operations are reviewed
- **THEN** verify that all user queries are scoped to the requesting authorization server and cross-tenant user access is impossible

#### Scenario: Client isolation
- **WHEN** client and application CRUD operations are reviewed
- **THEN** verify that clients/applications are scoped to their authorization server and cannot be accessed cross-tenant

#### Scenario: Authorization code isolation
- **WHEN** authorization code creation and exchange is reviewed
- **THEN** verify codes are bound to a specific authorization server and cannot be exchanged against a different server's token endpoint

#### Scenario: Refresh token isolation
- **WHEN** refresh token creation and usage is reviewed
- **THEN** verify tokens are bound to a specific authorization server and cannot be used cross-tenant

### Requirement: Signing Key Isolation Audit
The audit SHALL verify that each authorization server's signing keys are properly scoped and cannot be used by or leaked to other tenants.

#### Scenario: Key scoping in JWKS
- **WHEN** the JWKS endpoint implementation is reviewed
- **THEN** verify that each authorization server's JWKS endpoint only returns its own keys

#### Scenario: Token signing key selection
- **WHEN** the JWT signing process is reviewed
- **THEN** verify that tokens are signed with the correct authorization server's key and cross-tenant key usage is impossible

### Requirement: Scope Boundary Audit
The audit SHALL verify that OAuth scopes are properly isolated between authorization servers.

#### Scenario: Scope definition isolation
- **WHEN** scope CRUD operations are reviewed
- **THEN** verify scopes are scoped to their authorization server and cannot be referenced cross-tenant

#### Scenario: Token scope enforcement
- **WHEN** the token generation process is reviewed
- **THEN** verify that tokens can only contain scopes registered to the issuing authorization server

### Requirement: Tenant Identifier Validation Audit
The audit SHALL verify that the authorization server identifier in URL paths is properly validated and cannot be manipulated.

#### Scenario: Path parameter validation
- **WHEN** the `{authorizationServerId}` path parameter handling is reviewed across all tenant-scoped endpoints
- **THEN** verify the ID is validated as a valid UUID and that non-existent server IDs return appropriate errors without leaking information

#### Scenario: Tenant context propagation
- **WHEN** the request processing pipeline is reviewed
- **THEN** verify that the tenant context (authorization server ID) is consistently used throughout the request lifecycle and cannot be overridden mid-request
