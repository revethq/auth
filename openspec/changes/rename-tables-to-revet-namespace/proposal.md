# Change: Rename all tables to revet_ namespace

## Why
The auth project's database tables use inconsistent naming: some use implicit JPA names (e.g. `client`, `scope`, `authorizationserver`), some use snake_case (e.g. `refresh_tokens`, `group_members`), and some use a `scim_` prefix. Moving all tables under a `revet_` prefix establishes a consistent namespace, avoids collisions with PostgreSQL reserved words (e.g. `user`, `group`), and aligns with the IAM library's convention (e.g. `revet_service_accounts`, `revet_users`).

## What Changes
All JPA entities in the persistence module get explicit `@Table(name = "revet_...")` annotations with snake_case names. Column names within tables are not changed. The join table `scopereference` is also renamed.

**Table renames:**

| Entity | Current table | New table |
|--------|--------------|-----------|
| AuthorizationServer | `authorizationserver` | `revet_authorization_servers` |
| Client | `client` | `revet_clients` |
| ClientCode | `clientcode` | `revet_client_codes` |
| Event | `event` | `revet_events` |
| Group | `groups` | `revet_groups` |
| GroupMember | `group_members` | `revet_group_members` |
| IdentityProvider | `identityprovider` | `revet_identity_providers` |
| Profile | `profile` | `revet_profiles` |
| RefreshToken | `refresh_tokens` | `revet_refresh_tokens` |
| Schema | `schema` | `revet_schemas` |
| Scope | `scope` | `revet_scopes` |
| ScopeReference | `scopereference` | `revet_scope_references` |
| SigningKey | `signingkey` | `revet_signing_keys` |
| Template | `template` | `revet_templates` |
| User | `users` | `revet_users` |
| Application | `application` | `revet_applications` |
| ApplicationSecret | `applicationsecret` | `revet_application_secrets` |
| ScimApplication | `scim_application` | `revet_scim_applications` |
| ScimDeliveryStatus | `scim_delivery_status` | `revet_scim_delivery_statuses` |
| ScimResourceMapping | `scim_resource_mapping` | `revet_scim_resource_mappings` |

## Impact
- Affected specs: `table-namespace` (new)
- Affected code:
  - **Modified files** (all in `persistence/src/main/kotlin/com/revethq/auth/persistence/entities/`):
    - `AuthorizationServer.kt` — add `@Table(name = "revet_authorization_servers")`
    - `Client.kt` — add `@Table(name = "revet_clients")`, update `@JoinTable` scopereference reference
    - `ClientCode.kt` — add `@Table(name = "revet_client_codes")`
    - `Event.kt` — add `@Table(name = "revet_events")`
    - `Group.kt` — update `@Table(name = "revet_groups")`
    - `GroupMember.kt` — update `@Table(name = "revet_group_members")`
    - `IdentityProvider.kt` — add `@Table(name = "revet_identity_providers")`
    - `Profile.kt` — add `@Table(name = "revet_profiles")`
    - `RefreshToken.kt` — update `@Table(name = "revet_refresh_tokens")`
    - `Schema.kt` — add `@Table(name = "revet_schemas")`
    - `Scope.kt` — add `@Table(name = "revet_scopes")`
    - `ScopeReference.kt` — update `@Table(name = "revet_scope_references")`
    - `SigningKey.kt` — add `@Table(name = "revet_signing_keys")`
    - `Template.kt` — add `@Table(name = "revet_templates")`
    - `User.kt` — update `@Table(name = "revet_users")`
    - `Application.kt` — add `@Table(name = "revet_applications")`
    - `ApplicationSecret.kt` — add `@Table(name = "revet_application_secrets")`, update `@JoinTable` scopereference reference
    - `ScimApplication.kt` — update `@Table(name = "revet_scim_applications")`, update index names
    - `ScimDeliveryStatus.kt` — update `@Table(name = "revet_scim_delivery_statuses")`, update index names
    - `ScimResourceMapping.kt` — update `@Table(name = "revet_scim_resource_mappings")`, update index/constraint names
