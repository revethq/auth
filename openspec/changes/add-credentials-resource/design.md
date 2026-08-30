## Context
Authentication credentials are currently managed in two separate patterns:
1. **User passwords** — `passwordHash` stored directly on the `users` table, managed via `UserService.setPassword`/`validatePassword`
2. **Application secrets** — `ApplicationSecret` as a dedicated resource with its own entity, repository, DTOs, API (`/api/v1/applicationSecrets`), and scope relationships

This split prevents credential lifecycle management for users and creates inconsistency. The new `Credential` resource unifies both patterns into a single extensible model.

## Goals / Non-Goals
- Goals:
  - Unify user password and application secret management into a single Credential resource
  - Support extensible credential types (`PASSWORD`, `API_KEY`, `TOTP`, `RECOVERY_CODE`)
  - Support optional scope restrictions on credentials (carrying forward the ApplicationSecret scope feature)
  - Enable credential lifecycle management (create, revoke, expire)
  - Maintain backwards-compatible OAuth 2.1 flow behavior (authorization code + client_credentials)
- Non-Goals:
  - TOTP verification logic (type is stored but verification is out of scope)
  - Recovery code generation flows
  - Multi-factor authentication enforcement policies
  - Rate limiting or brute-force protection on credential validation

## Decisions

### Ownership Model
- A Credential has two optional owner fields: `userId` (UUID) and `applicationId` (UUID)
- Exactly one of the two MUST be set on each credential
- This avoids a generic `ownerId`/`ownerType` pattern that loses type safety and complicates queries
- Alternatives considered: single `ownerId` + `ownerType` enum. Rejected because it loses foreign key integrity and requires runtime type checks.

### Credential Types
- `CredentialType` enum: `PASSWORD`, `API_KEY`, `TOTP`, `RECOVERY_CODE`
- Application secrets use `API_KEY` type — they are conceptually the same as API keys (a random secret used for authentication)
- This eliminates a redundant `APPLICATION_SECRET` type that would be functionally identical to `API_KEY`
- Hashing strategy is determined by type: `PASSWORD` uses bcrypt, all others use SHA-256

### Scope Relationship
- Credentials have an optional many-to-many relationship with Scopes (via `scopereference` table)
- This carries forward the ApplicationSecret scope filtering behavior
- During `client_credentials` flow, requested scopes are intersected with the credential's assigned scopes
- For `PASSWORD` credentials, scopes are typically empty
- The `ScopeReferenceType` enum gets a new value: `CREDENTIAL` (replacing `APPLICATION_SECRET`)

### API Design
- Top-level path: `POST/GET /api/v1/credentials`, `GET/DELETE /api/v1/credentials/{credentialId}`
- No PUT/PATCH — credentials are immutable once created. To rotate, create a new credential and delete the old one.
- List endpoint filters by `userId`, `applicationId`, and optionally `type`
- Create response includes the plain value exactly once; subsequent GETs never return it
- `CredentialRequest` includes: `userId` (optional), `applicationId` (optional), `authorizationServerId`, `type`, `value` (plain credential), `name` (optional), `scopes` (optional list of scope UUIDs), `expiresIn` (optional)

### Password Constraint
- Only one ACTIVE credential of type `PASSWORD` is allowed per user per authorization server
- Creating a new `PASSWORD` credential automatically revokes any existing active password credential for that user
- No such constraint for `API_KEY` — multiple active keys are allowed

### Client Credentials Flow Migration
- Currently: `AuthorizationServer.kt` calls `applicationService.isApplicationSecretValid(authServerId, secretId, secret)` then `applicationService.getApplicationSecret(secretId)` for scopes
- After: calls `credentialService.validate(credentialId, value)` and `credentialService.getCredential(credentialId)` for scopes
- The `clientId` in the client_credentials request maps to the credential ID (same as current behavior where it maps to the application secret ID)

### Migration
- Remove `passwordHash` from the `users` table
- Remove the `application_secret` table
- Add a `credentials` table with columns: `id`, `user_id`, `application_id`, `authorization_server_id`, `type`, `status`, `name`, `credential_hash`, `expires_in`, `created_on`, `updated_on`
- Migrate existing password hashes from `users` and application secret hashes from `application_secret` into `credentials`
- Update `scopereference` table to use `CREDENTIAL` type instead of `APPLICATION_SECRET`

## Risks / Trade-offs
- **Breaking change** for API consumers using `/api/v1/applicationSecrets` or setting passwords via User create/update. Mitigation: document migration path clearly.
- **Data migration** for existing passwords and application secrets. Mitigation: SQL migration script handles both in a single transaction.
- **Scope reference migration** — existing `APPLICATION_SECRET` scope references need to point to new credential IDs. Mitigation: migration script maps old secret IDs to new credential IDs.

## Migration Plan
1. Create the `credentials` table
2. Migrate existing `passwordHash` values from `users` into `credentials` as type `PASSWORD`
3. Migrate existing application secrets from `application_secret` into `credentials` as type `API_KEY`
4. Update `scopereference` entries from type `APPLICATION_SECRET` to `CREDENTIAL`, mapping to new credential IDs
5. Drop the `passwordHash` column from `users`
6. Drop the `application_secret` table
7. Update application code (remove ApplicationSecret stack, remove password from User, add CredentialService, update OAuth flows)

## Open Questions
- Should there be a bulk-create endpoint for recovery codes (e.g., generate 10 at once)?
