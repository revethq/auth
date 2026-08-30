## Context
The auth project defines an `Application` entity for system-to-system (client_credentials) OAuth flows. The IAM library already provides a `ServiceAccount` domain model with persistence and web modules. Replacing Application with ServiceAccount unifies the identity model across Revet projects and eliminates redundant code.

This change depends on `add-credentials-resource`, which migrates ApplicationSecret into a unified Credential resource. With credentials handled separately, the Application entity's remaining responsibilities are: identity (name, clientId), tenant association (authorizationServerId), scope assignment, and profile data.

## Goals / Non-Goals
- **Goals:**
  - Replace Application with IAM ServiceAccount as the identity for client_credentials flows
  - Import `revet-service-account` and `revet-service-account-persistence` as library dependencies
  - Use the IAM `revet_service_accounts` table directly with no wrapping entity
  - Preserve profile/schema validation for service accounts
  - Map ServiceAccount.tenantId to authorizationServerId

- **Non-Goals:**
  - Changing the client_credentials OAuth flow itself (only the identity lookup changes)
  - Implementing IAM permission evaluation (out of scope)
  - Modifying the Credential resource (handled by `add-credentials-resource`)
  - Database migration tooling (Hibernate schema update handles dev; production migration is manual)

## Decisions

### 1. Import IAM modules as dependencies
- **Decision:** Add `revet-service-account` (domain) to core and `revet-service-account-persistence` (JPA + repository) to persistence
- **Rationale:** Reuses existing, tested code. The IAM ServiceAccount already uses `com.revethq.core.Metadata`, matching auth's patterns
- **Alternative:** Define a new ServiceAccount in auth. Rejected because it duplicates the IAM model and prevents cross-system identity correlation via URNs

### 2. Use IAM table directly, no wrapping entity
- **Decision:** Use the IAM `ServiceAccountEntity` and `revet_service_accounts` table as-is. Auth-specific concerns (scopes, profiles) are handled via join tables and existing Profile entities that reference the service account ID.
- **Rationale:** Avoids additional schema complexity. The IAM entity already has all the fields needed for identity (id, name, description, tenantId, metadata, timestamps).

### 3. Service account ID as OAuth client_id
- **Decision:** Use `serviceAccount.id.toString()` as the OAuth `client_id` in the client_credentials flow. No separate `clientId` field is needed.
- **Rationale:** The current Application entity already uses `clientId = id.toString()`. The service account UUID provides the same unique identifier function. This avoids needing an extra column or metadata key for a value that's already available.

### 4. tenantId maps to authorizationServerId
- **Decision:** When creating a service account, set `tenantId = authorizationServerId.toString()`. When listing/filtering, convert back.
- **Rationale:** AuthorizationServer is the tenant boundary in auth. The IAM tenantId is a String, so UUID.toString() works directly.

### 5. Scope association
- **Decision:** Create a scope join table (`revet_service_account_scopes` or reuse the existing `scopereference` pattern) that references `revet_service_accounts.id` and `scope.id` directly.
- **Rationale:** Scopes are auth-specific and not part of the IAM model. A join table is the same pattern used by Client and the former Application entity.

### 6. Profile association
- **Decision:** Reuse the existing Profile entity and ProfileService. Service accounts get profiles the same way Applications do today: a Profile row with `profileType = SERVICE_ACCOUNT` linked by the service account ID.
- **Rationale:** Preserves schema validation behavior and keeps profile management consistent with users.

## Risks / Trade-offs
- **IAM library version coupling** — Auth depends on the IAM service-account module version. Mitigation: pin to a specific version and update intentionally.
- **Database schema change** — The `application` table is replaced by the IAM `revet_service_accounts` table. Mitigation: provide a migration SQL script in the change tasks.
- **Existing data** — Any existing Application rows need migration to ServiceAccount rows. Mitigation: document a migration query in tasks.
- **Scope join table** — Auth needs to define a join table that references an IAM-owned table. This is a cross-module schema dependency but is acceptable since auth owns its database.

## Migration Plan
1. Implement `add-credentials-resource` first (removes ApplicationSecret dependency)
2. Add IAM service-account dependencies
3. `revet_service_accounts` table created via Hibernate schema update in dev
4. Create scope join table for service accounts
5. Implement ServiceAccountService delegating to IAM service + auth concerns
6. Add API routes and DTOs
7. Update client_credentials flow to use ServiceAccountService
8. Remove Application entity, service, routes, and related code
9. Provide SQL migration script for existing Application data
