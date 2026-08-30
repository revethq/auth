## 1. Rename entity tables
- [x] 1.1 `AuthorizationServer.kt` — add `@Table(name = "revet_authorization_servers")`
- [x] 1.2 `Client.kt` — add `@Table(name = "revet_clients")`
- [x] 1.3 `ClientCode.kt` — add `@Table(name = "revet_client_codes")`
- [x] 1.4 `Event.kt` — add `@Table(name = "revet_events")`
- [x] 1.5 `Group.kt` — update to `@Table(name = "revet_groups")`
- [x] 1.6 `GroupMember.kt` — update to `@Table(name = "revet_group_members")`
- [x] 1.7 `IdentityProvider.kt` — add `@Table(name = "revet_identity_providers")`
- [x] 1.8 `Profile.kt` — add `@Table(name = "revet_profiles")`
- [x] 1.9 `RefreshToken.kt` — update to `@Table(name = "revet_refresh_tokens")`
- [x] 1.10 `Schema.kt` — add `@Table(name = "revet_schemas")`
- [x] 1.11 `Scope.kt` — add `@Table(name = "revet_scopes")`
- [x] 1.12 `SigningKey.kt` — add `@Table(name = "revet_signing_keys")`
- [x] 1.13 `Template.kt` — add `@Table(name = "revet_templates")`
- [x] 1.14 `User.kt` — update to `@Table(name = "revet_users")`
- [x] 1.15 `Application.kt` — add `@Table(name = "revet_applications")`
- [x] 1.16 `ApplicationSecret.kt` — add `@Table(name = "revet_application_secrets")`

## 2. Rename join tables
- [x] 2.1 `ScopeReference.kt` — update to `@Table(name = "revet_scope_references")`
- [x] 2.2 Update `@JoinTable(name = "scopereference")` references in `Client.kt`, `Application.kt`, and `ApplicationSecret.kt` to `revet_scope_references`

## 3. Rename SCIM tables
- [x] 3.1 `ScimApplication.kt` — update to `@Table(name = "revet_scim_applications")`, update index names
- [x] 3.2 `ScimDeliveryStatus.kt` — update to `@Table(name = "revet_scim_delivery_statuses")`, update index names
- [x] 3.3 `ScimResourceMapping.kt` — update to `@Table(name = "revet_scim_resource_mappings")`, update index/constraint names

## 4. Write SQL migration script
- [x] 4.1 Write `ALTER TABLE ... RENAME TO` statements for all 20 tables, guarded with `ALTER TABLE IF EXISTS` for idempotency
- [x] 4.2 Place the migration script in `persistence/src/main/resources/db/migration/V1__rename_tables_to_revet_namespace.sql`

## 5. Verify
- [x] 5.1 All three submodules (`core`, `persistence`, `web`) compile successfully
- [ ] 5.2 Confirm existing tests pass with the new table names
