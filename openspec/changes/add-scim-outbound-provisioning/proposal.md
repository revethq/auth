# Change: Add SCIM v2 Outbound Provisioning

## Why

Revet Auth needs to automatically propagate user and group lifecycle changes to downstream applications that implement SCIM v2 endpoints. This enables organizations to keep identity data synchronized across their application ecosystem without manual intervention, following the industry-standard approach used by Okta, Microsoft Entra ID, and other identity providers.

## What Changes

### New Domain Concepts
- **ScimApplication**: Configuration for a downstream SCIM service provider, linked to an Application for JWT-based authentication
- **ScimProvisioningLog**: Audit trail of outbound SCIM operations with status, request/response, and retry information
- **SCIM Scopes**: Standard OAuth scopes for SCIM access control (`scim:users:read`, `scim:users:write`, `scim:groups:read`, `scim:groups:write`)

### New Capabilities
- **User Provisioning**: Push user create, update, deactivate (active=false), and delete operations to configured downstream SCIM applications
- **Group Provisioning**: Push group create, update, and delete operations to downstream SCIM applications
- **Group Membership Sync**: Push member add/remove operations when users are added to or removed from groups
- **Attribute Mapping**: Map Revet Auth user/group attributes to SCIM schema attributes per downstream application
- **JWT-Based Authentication**: ScimApplications use an associated Application to generate JWTs; downstream consumers validate against the authorization server's JWKS
- **Automatic Application Provisioning**: Optionally auto-create an Application with SCIM scopes when creating a ScimApplication
- **SCIM Scope Validation**: Verify associated Applications have required SCIM scopes
- **Retry Logic**: Queue failed operations with configurable retry policy and exponential backoff

### Integration Points
- Hooks into existing EventRepository to trigger SCIM operations on User, Group, and GroupMember events
- New REST API endpoints for managing ScimApplication configurations
- Background job processing for async SCIM operation delivery
- Application and Scope entities extended for SCIM integration

## Impact

- **Affected specs**: New capability (no existing specs modified)
- **Affected code**:
  - `core/`: New domain models (ScimApplication, ScimProvisioningLog), service interfaces
  - `persistence/`: New JPA entities, repositories, service implementations
  - `web/`: New REST endpoints for ScimApplication CRUD, provisioning status
  - Event system: Subscribe to User/Group/GroupMember events
  - Scope/Application services: Extended for SCIM scope management
- **Database**: New tables for `scim_applications` and `scim_provisioning_logs`; new SCIM scopes in `scopes` table
- **External dependencies**: HTTP client for outbound SCIM calls (Quarkus REST Client)
