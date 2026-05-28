# Multi-Tenancy Isolation Security Audit Findings

**Audit Date**: 2026-05-21
**Auditor**: Claude Opus 4.6 (Automated Security Audit)
**Scope**: Multi-tenant isolation boundaries across all authorization server operations

## Summary

| Severity | Count |
|----------|-------|
| Critical | 1     |
| High     | 8     |
| Medium   | 5     |
| Low      | 2     |
| Informational | 2 |
| **Total** | **18** |

---

### Finding: MTI-01 Authorization Code Cross-Tenant Exchange
- **Severity**: Critical
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:441-469`
- **Description**: When exchanging an authorization code for a token at the token endpoint (`postToken`, `AUTHORIZATION_CODE` grant type), the `clientService.getClientCode()` retrieves the client code by its code string alone without verifying that the code's `authorizationServerId` matches the `authorizationServerId` from the URL path. The code is looked up globally via `ClientCodeRepository.findByCode(code)` (line 30 of `ClientCodeRepository.kt`), which has no `authorizationServerId` filter. While the authorization code is created with the correct `authorizationServerId`, a code issued by server A could be exchanged at server B's token endpoint. The resulting token would be signed by server B's key, with server B's issuer/audience, but would carry the userId and scopes from the code that was originally issued by server A.
- **Risk**: An attacker who obtains an authorization code for tenant A could exchange it at tenant B's token endpoint. This produces a valid access token signed by tenant B, but referencing a userId that may not exist in tenant B or, worse, maps to a different user in tenant B. This is a full cross-tenant authentication bypass.
- **Recommendation**: After retrieving the `ClientCode`, verify that `clientCode.authorizationServerId == authorizationServerId` before proceeding. Add an `authorizationServerId` parameter to the `getClientCode` method or add a post-retrieval check:
  ```kotlin
  if (clientCode.authorizationServerId != authorizationServerId) {
      throw BadRequestException("Authorization code does not belong to this authorization server")
  }
  ```
- **References**: RFC 6749 Section 4.1.3, OWASP Authorization Testing

---

### Finding: MTI-02 User Lookup by Username Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/UserService.kt:165-176`, `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/UserRepository.kt:30-32`
- **Description**: The `getUser(username: String)` method queries users by username globally across all authorization servers. The repository method `findByUsername` uses `find("username = ?1", username)` with no `authorizationServerId` filter. If two authorization servers have users with the same username, this method returns an arbitrary one (the first result). In the OAuth authorization flow (`AuthorizationServer.kt:100-101`), this method is used to find the user during login, and then the authorization server check happens afterward (line 111). However, `findByUsername` uses `firstResultOptional()`, which means the check on line 111 could fail for a legitimate user if another tenant has the same username and happens to be returned first.
- **Risk**: In the login flow, a user from a different authorization server could be returned, causing the subsequent `authorizationServerId` check to fail even for legitimate users (denial of service), or if an attacker creates a user with a matching username on a different tenant, the wrong user record could be found. While the authorization server check at line 111 catches this, it relies on application-level defense rather than database-level scoping.
- **Recommendation**: Add `authorizationServerId` as a parameter to the `getUser(username: String)` method. Update `UserRepository.findByUsername` to include the authorization server filter:
  ```kotlin
  fun findByUsernameAndAuthorizationServerId(username: String, authorizationServerId: UUID): Optional<User> {
      return find("username = ?1 and authorizationServerId = ?2", username, authorizationServerId).firstResultOptional()
  }
  ```
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-03 User Get/Update/Delete by ID Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/UserService.kt:151-161` (getUser by ID), `180-203` (updateUser), `134-147` (deleteUser)
- **Description**: The `getUser(userId: UUID)`, `updateUser(userId, ...)`, and `deleteUser(userId)` methods look up users solely by their primary key (UUID) without verifying the user belongs to the requesting authorization server. The API routes in `web/src/main/kotlin/com/revethq/auth/web/api/routes/Users.kt` also do not pass or validate any `authorizationServerId`. An API caller can get, update, or delete any user across any tenant by knowing or guessing the user's UUID.
- **Risk**: Cross-tenant data access. An administrator of tenant A can read, modify, or delete users belonging to tenant B. Password changes, profile modifications, and user deletion can all be performed cross-tenant.
- **Recommendation**: Add `authorizationServerId` as a required parameter for all single-entity user operations. Modify the repository query to include `authorizationServerId` in the lookup, or add a post-retrieval check that the user's `authorizationServerId` matches the expected tenant.
- **References**: OWASP IDOR

---

### Finding: MTI-04 Client Get/Update/Delete by ID Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ClientService.kt:112-118` (deleteClient), `122-134` (updateClient), `138-143` (getClient by clientId string)
- **Description**: The `deleteClient(clientId: UUID)`, `updateClient(clientId, ...)`, and `getClient(clientId: String)` methods look up clients solely by their primary key or client ID string, without any authorization server scoping. The `ClientRepository.findByClientId(clientId: String)` (line 34-36 of `ClientRepository.kt`) queries globally. The API routes in `Clients.kt` pass only the client ID, not the authorization server ID.
- **Risk**: Cross-tenant client management. An attacker can modify redirect URIs for a client belonging to a different tenant, enabling OAuth redirect attacks. Deleting a cross-tenant client causes denial of service.
- **Recommendation**: All client CRUD operations should require and validate the `authorizationServerId`. The `findByClientId` repository method should include `authorizationServerId` in the query.
- **References**: CWE-639 (Authorization Bypass Through User-Controlled Key)

---

### Finding: MTI-05 Application Get/Delete by ID Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt:70-83` (deleteApplication), `87-97` (getApplication)
- **Description**: The `deleteApplication(applicationId: UUID)` and `getApplication(applicationId: UUID)` methods look up applications solely by their primary key without verifying the application belongs to the requesting authorization server. The API routes in `Applications.kt` similarly do not pass or check the authorization server ID for single-entity operations.
- **Risk**: Cross-tenant application management. An attacker can view or delete applications belonging to different tenants, causing data exposure or denial of service.
- **Recommendation**: Add `authorizationServerId` scoping to all single-entity application operations, either as a query filter or as a post-retrieval verification.
- **References**: CWE-639 (Authorization Bypass Through User-Controlled Key)

---

### Finding: MTI-06 Scope Get/Delete by ID Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScopeService.kt:65-71` (deleteScope), `75-80` (getScope)
- **Description**: The `deleteScope(scopeId: UUID)` and `getScope(scopeId: UUID)` methods use `findByIdOptional(scopeId)` without any authorization server scoping. An API caller can delete or read scopes belonging to any tenant.
- **Risk**: An attacker could delete scopes belonging to another authorization server, causing token generation failures (denial of service) or altering the authorization model of a different tenant.
- **Recommendation**: Add `authorizationServerId` verification to scope get and delete operations.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-07 Group Get/Update/Delete by ID Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/GroupService.kt:67-77` (deleteGroup), `81-86` (getGroup), `90-103` (updateGroup)
- **Description**: Group operations (get, update, delete) look up groups by primary key without any authorization server scoping. The same pattern exists for `GroupMemberService` (`GroupMemberService.kt:70-77` deleteGroupMember, `81-86` getGroupMember).
- **Risk**: Cross-tenant group manipulation. Groups and group memberships from one tenant can be read, modified, or deleted by another tenant.
- **Recommendation**: Add `authorizationServerId` scoping to all single-entity group operations.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-08 Template and Schema Get/Update/Delete by ID Not Scoped to Authorization Server
- **Severity**: High
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/TemplateService.kt:66-72` (deleteTemplate), `76-81` (getTemplate), `85-96` (updateTemplate); `persistence/src/main/kotlin/com/revethq/auth/persistence/services/SchemaService.kt:110-116` (deleteSchema), `120-125` (getSchema), `147-158` (updateSchema)
- **Description**: Template and schema single-entity operations (get, update, delete) use only the entity's primary key without verifying it belongs to the requesting authorization server.
- **Risk**: Cross-tenant template or schema manipulation. An attacker could modify the login template of a different tenant (injecting malicious HTML/JavaScript), or delete schemas to break user validation for another tenant.
- **Recommendation**: Add `authorizationServerId` scoping to all template and schema single-entity operations.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-09 List Endpoints Return All Tenants When No Authorization Server Filter Provided
- **Severity**: Medium
- **Location**: Multiple services: `UserService.kt:93-101`, `ClientService.kt:155-163`, `ScopeService.kt:42-51`, `ApplicationService.kt:200-212`, `GroupService.kt:44-53`, `GroupMemberService.kt:42-57`, `TemplateService.kt:42-51`, `SchemaService.kt:128-137`, `ScimApplicationService.kt:63-73`
- **Description**: All list endpoints follow a pattern where if the `authorizationServerIds` list is empty, they call `listAll()` or `findAll()`, returning entities from ALL authorization servers without any tenant filtering. The API routes pass the filter as an optional query parameter (e.g., `Users.kt:66-79` uses `authorizationServerIds ?: emptyList()`), so omitting the parameter returns all data.
- **Risk**: Any authenticated API caller can enumerate and access data across all tenants by simply not providing the `authorizationServerIds` query parameter. This is an information disclosure vulnerability that exposes the full multi-tenant dataset.
- **Recommendation**: List endpoints should require at least one `authorizationServerId` filter and never fall through to `listAll()`. Alternatively, the current user's tenant should be determined from their authentication context and enforced server-side.
- **References**: CWE-200 (Exposure of Sensitive Information)

---

### Finding: MTI-10 ApplicationSecret Delete/List Not Scoped to Authorization Server
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ApplicationService.kt:191-196` (deleteApplicationSecret), `144-152` (getApplicationSecrets); `web/src/main/kotlin/com/revethq/auth/web/api/routes/ApplicationSecrets.kt:53-55,58-66`
- **Description**: The `deleteApplicationSecret(secretId: UUID)` method looks up the secret by primary key without any authorization server check. The `getApplicationSecrets(applicationId: List<UUID>)` method, when given an empty list, calls `applicationSecretRepository.findAll().list()` (line 146), returning all secrets across all tenants. The `getApplicationSecret(applicationSecretId: String)` method at line 239 also lacks tenant scoping.
- **Risk**: Cross-tenant secret enumeration and deletion. While the actual secret value is hashed, the metadata (application ID, scopes, creation date) is exposed. Deleting a cross-tenant secret breaks client credential authentication for that tenant.
- **Recommendation**: Add `authorizationServerId` verification to all application secret operations. The list endpoint should never return all secrets without a tenant filter.
- **References**: CWE-639 (Authorization Bypass Through User-Controlled Key)

---

### Finding: MTI-11 IdentityProvider Operations Completely Lack Authorization Server Scoping
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/IdentityProviderService.kt:43-64`
- **Description**: All `IdentityProviderService` methods (get, create, update, delete) operate solely on the entity's primary key with no authorization server scoping whatsoever. The mapper functions (`from` and `to`) are also stub implementations returning empty objects. There is no `authorizationServerId` checking anywhere in this service.
- **Risk**: Cross-tenant identity provider manipulation. While this service appears to be in an early/incomplete state, any identity provider can be accessed, modified, or deleted by any tenant.
- **Recommendation**: Add `authorizationServerId` to all identity provider operations. Complete the mapper implementations and ensure tenant scoping is included.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-12 ScimApplication Get/Update/Delete Not Scoped to Authorization Server
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/ScimApplicationService.kt:82-86` (getScimApplication), `142-170` (updateScimApplication), `173-184` (deleteScimApplication)
- **Description**: The `getScimApplication`, `updateScimApplication`, and `deleteScimApplication` methods retrieve SCIM application entities by their primary key without checking that they belong to the requesting tenant's authorization server.
- **Risk**: Cross-tenant SCIM configuration manipulation. An attacker could modify the SCIM provisioning configuration of a different tenant, redirecting user provisioning data to an attacker-controlled endpoint.
- **Recommendation**: Add `authorizationServerId` verification to all SCIM application single-entity operations.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-13 Profile Lookup by ResourceId Not Scoped to Authorization Server
- **Severity**: Medium
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/ProfileRepository.kt:35-37`, `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:162-164,190-192`
- **Description**: The `ProfileRepository.findByResourceId(resourceId: UUID)` method queries profiles solely by the `resource` foreign key, without any `authorizationServerId` filter. This is used in `generateClientCredentialsAccessToken` (line 162) and `generateAuthorizationCodeFlowAccessToken` (line 190) to retrieve profile data that gets embedded into JWT claims. Although the `authorizationServerId` is used to select the signing key and build the issuer claim, the profile data itself is fetched without tenant verification.
- **Risk**: If a resource ID collision or reuse occurs, profile data from a different tenant could be embedded into a JWT. In practice, UUIDs make collisions unlikely, but the absence of tenant scoping at the query level is a defense-in-depth gap.
- **Recommendation**: Add `authorizationServerId` to the profile lookup query used during token generation.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-14 Refresh Token Lookup Not Scoped by Authorization Server at Query Level
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/RefreshTokenRepository.kt:32-37`
- **Description**: The `findByTokenHash(tokenHash: String)` method queries the refresh token table by token hash alone, without including `authorizationServerId` in the query. While the `AuthorizationServerService.refreshAccessToken` method (line 244) performs an application-level check after retrieval (`storedToken.authorizationServerId != authorizationServerId`), the database query itself is not scoped.
- **Risk**: The application-level check at line 244 provides effective protection. However, querying without tenant scoping at the database level means the database does work for all tenants and a timing side-channel could theoretically reveal whether a token hash exists in a different tenant. The risk is low because the application-level check correctly prevents cross-tenant token usage.
- **Recommendation**: Include `authorizationServerId` in the `findByTokenHash` query for defense-in-depth.
- **References**: CWE-208 (Observable Timing Discrepancy)

---

### Finding: MTI-15 Refresh Token Revocation Not Scoped by Authorization Server
- **Severity**: Low
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:456-458`, `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/RefreshTokenRepository.kt:46-50`
- **Description**: The `revokeAllUserRefreshTokens(userId: UUID)` method and its backing repository method `revokeAllUserTokens(userId: UUID)` revoke tokens for a user across all authorization servers, not just the current tenant. The query is `"userId = ?2 and isRevoked = false"` with no `authorizationServerId` filter.
- **Risk**: If a user ID exists in multiple authorization servers (unlikely with UUIDs but possible via cross-tenant user creation), revoking tokens for one tenant would also revoke tokens for the other. This could cause unexpected session termination across tenants.
- **Recommendation**: Add `authorizationServerId` to the revocation query.
- **References**: CWE-284 (Improper Access Control)

---

### Finding: MTI-16 ValidAuthorizationServer Validator Swallows Unexpected Exceptions
- **Severity**: Informational
- **Location**: `core/src/main/kotlin/com/revethq/auth/core/authorization/validators/ValidAuthorizationServer.kt:69-71`
- **Description**: In the core `ValidAuthorizationServer` validator, the catch block for general `Exception` (line 69-71) prints a message to stdout using `println` but returns `true` (falls through to line 72), meaning that if an unexpected error occurs (database connectivity issue, timeout, etc.), the validation passes and the request proceeds with a potentially non-existent or unverified authorization server ID.
- **Risk**: During transient infrastructure failures, requests with invalid or non-existent authorization server IDs could bypass validation and proceed, potentially causing null pointer exceptions or unexpected behavior deeper in the processing pipeline.
- **Recommendation**: The catch block for non-`AuthorizationServerNotFound` exceptions should return `false` rather than falling through to `true`. Also, replace `println` with proper logger usage.
- **References**: CWE-754 (Improper Check for Unusual or Exceptional Conditions)

---

### Finding: MTI-17 UserInfo Endpoint Does Not Verify User Belongs to Authorization Server
- **Severity**: Informational
- **Location**: `web/src/main/kotlin/com/revethq/auth/web/authorization/routes/AuthorizationServer.kt:229-239`
- **Description**: The `getUserInfo` endpoint validates the JWT against the authorization server (which includes issuer and audience checks), then extracts the `sub` claim and looks up the user by UUID. However, it does not verify that the user's `authorizationServerId` matches the authorization server from the URL path. In practice, a JWT minted by server A could not pass validation against server B because the signing keys and issuer differ, so this is a defense-in-depth gap rather than an exploitable vulnerability.
- **Risk**: Very low. The JWT validation already ensures the token was signed by the correct authorization server's key and has the correct issuer. The additional user-to-tenant check would be pure defense-in-depth.
- **Recommendation**: Add a check that the user returned by `getUser` has an `authorizationServerId` matching the path parameter, for defense-in-depth.
- **References**: OpenID Connect Core Section 5.3

---

### Finding: MTI-18 JWKS and Signing Key Selection Are Properly Scoped (Positive Finding)
- **Severity**: Informational (Positive)
- **Location**: `persistence/src/main/kotlin/com/revethq/auth/persistence/services/AuthorizationServerService.kt:144-148,386-393,397-435`; `persistence/src/main/kotlin/com/revethq/auth/persistence/repositories/SigningKeyRepository.kt:29-35`
- **Description**: The JWKS endpoint, token signing key selection, and JWT validation are all properly scoped to the authorization server. `getJwksForAuthorizationServer` queries signing keys using `findAllByAuthorizationServerId`. `validateJwtForAuthorizationServer` builds the JWT consumer with the expected issuer and validates against only that server's keys using `findByIdAndAuthorizationServerId`. Additionally, the refresh token flow correctly validates `authorizationServerId` at the application level (line 244) and client validation during OAuth flows properly checks `authorizationServerId` (lines 64, 111, 131, 293).
- **Risk**: None. This is a positive finding confirming correct implementation.
- **Recommendation**: No changes needed.
- **References**: RFC 7517 (JSON Web Key)

---

## Overall Assessment

The most critical architectural issue is a **systemic lack of `authorizationServerId` scoping on single-entity operations** (get, update, delete by primary key). While list operations generally support filtering by authorization server IDs, they default to returning all tenants' data when the filter is omitted.

The OAuth authorization flow has good tenant isolation in several areas -- the client is verified against the authorization server during authorization initiation (MTI-18), signing keys are properly scoped, and refresh tokens are validated against the correct tenant at the application level. However, the authorization code exchange (MTI-01) is a critical gap that could allow cross-tenant token generation.

The root cause of most findings is that the management API (CRUD operations on users, clients, applications, scopes, groups, templates, schemas) was designed without mandatory tenant scoping on individual entity operations. The `authorizationServerId` is treated as optional metadata rather than a mandatory security boundary.

**Priority remediation order:**
1. **MTI-01** (Critical): Add authorization server validation to authorization code exchange
2. **MTI-03 through MTI-08** (High): Add tenant scoping to all single-entity CRUD operations
3. **MTI-02** (High): Scope username lookup to the authorization server
4. **MTI-09** (Medium): Require authorization server filter on all list endpoints
5. Remaining Medium/Low findings for defense-in-depth
