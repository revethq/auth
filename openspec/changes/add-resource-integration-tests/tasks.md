# Tasks: Add Integration Tests for All Resources

## 1. Exception Handling Infrastructure

- [ ] 1.1 Create base `NotFoundException` class in `core/src/main/kotlin/com/revethq/auth/core/exceptions/`
- [ ] 1.2 Update existing NotFound exceptions to extend `NotFoundException`:
  - `AuthorizationServerNotFound`
  - `UserNotFound`
  - `ApplicationNotFound`
  - `ApplicationSecretNotFound`
  - `ClientNotFound`
  - `GroupNotFound`
  - `GroupMemberNotFound`
  - `ScopeNotFound`
  - `SchemaNotFound`
  - `TemplateNotFound`
  - `ProfileNotFound`
- [ ] 1.3 Create generic `NotFoundExceptionMapper` in `web/src/main/kotlin/com/revethq/auth/web/api/exceptions/`
- [ ] 1.4 Remove redundant `ScimApplicationNotFoundExceptionMapper` (will be covered by generic mapper)

## 2. Authorization Servers Integration Tests

- [ ] 2.1 Create `AuthorizationServersTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 2.2 Implement tests:
  - POST creates authorization server
  - POST returns 400 for invalid request (missing name)
  - GET retrieves authorization server by ID
  - GET returns 404 for non-existent authorization server
  - GET lists authorization servers with pagination
  - PUT updates authorization server
  - PUT returns 404 for non-existent authorization server
  - DELETE removes authorization server
- [ ] 2.3 Run tests and verify all pass

## 3. Users Integration Tests

- [ ] 3.1 Create `UsersTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 3.2 Implement tests:
  - POST creates user
  - POST returns 400 for invalid request
  - GET retrieves user by ID
  - GET returns 404 for non-existent user
  - GET lists users with authorizationServerIds filter
  - PUT updates user
  - PUT returns 404 for non-existent user
  - DELETE removes user
- [ ] 3.3 Run tests and verify all pass

## 4. Applications Integration Tests

- [ ] 4.1 Create `ApplicationsTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 4.2 Implement tests:
  - POST creates application
  - POST returns 400 for invalid request
  - GET retrieves application by ID
  - GET returns 404 for non-existent application
  - GET lists applications with authorizationServerIds filter
  - DELETE removes application
- [ ] 4.3 Run tests and verify all pass

## 5. Application Secrets Integration Tests

- [ ] 5.1 Create `ApplicationSecretsTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 5.2 Implement tests:
  - POST creates application secret
  - GET retrieves application secret by ID
  - GET returns 404 for non-existent secret
  - GET lists application secrets by applicationIds
  - DELETE removes application secret
- [ ] 5.3 Run tests and verify all pass

## 6. Clients Integration Tests

- [ ] 6.1 Create `ClientsTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 6.2 Implement tests:
  - POST creates client
  - POST returns 400 for invalid request
  - GET retrieves client by ID
  - GET returns 404 for non-existent client
  - GET lists clients with authorizationServerIds filter
  - PUT updates client
  - DELETE removes client
- [ ] 6.3 Run tests and verify all pass

## 7. Groups Integration Tests

- [ ] 7.1 Create `GroupsTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 7.2 Implement tests:
  - POST creates group
  - POST returns 400 for invalid request
  - GET retrieves group by ID
  - GET returns 404 for non-existent group
  - GET lists groups with authorizationServerIds filter
  - PUT updates group
  - DELETE removes group
- [ ] 7.3 Run tests and verify all pass

## 8. Group Members Integration Tests

- [ ] 8.1 Create `GroupMembersTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 8.2 Implement tests:
  - POST adds member to group
  - POST returns 400 for invalid request
  - GET retrieves group member
  - GET returns 404 for non-existent member
  - GET lists group members
  - DELETE removes member from group
- [ ] 8.3 Run tests and verify all pass

## 9. Scopes Integration Tests

- [ ] 9.1 Create `ScopesTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 9.2 Implement tests:
  - POST creates scope
  - POST returns 400 for invalid request
  - GET retrieves scope by ID
  - GET returns 404 for non-existent scope
  - GET lists scopes with authorizationServerIds filter
  - PUT updates scope
  - DELETE removes scope
- [ ] 9.3 Run tests and verify all pass

## 10. Schemas Integration Tests

- [ ] 10.1 Create `SchemasTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 10.2 Implement tests:
  - POST creates schema
  - POST returns 400 for invalid request
  - GET retrieves schema by ID
  - GET returns 404 for non-existent schema
  - GET lists schemas with authorizationServerIds filter
  - PUT updates schema
  - DELETE removes schema
- [ ] 10.3 Run tests and verify all pass

## 11. Templates Integration Tests

- [ ] 11.1 Create `TemplatesTest.kt` in `web/src/test/kotlin/com/revethq/auth/web/api/routes/`
- [ ] 11.2 Implement tests:
  - POST creates template
  - POST returns 400 for invalid request
  - GET retrieves template by ID
  - GET returns 404 for non-existent template
  - GET lists templates with authorizationServerIds filter
  - PUT updates template
  - DELETE removes template
- [ ] 11.3 Run tests and verify all pass

## 12. Final Validation

- [ ] 12.1 Run full test suite: `./gradlew test`
- [ ] 12.2 Verify all tests pass
- [ ] 12.3 Update tasks.md with completion status

## Dependencies

- Section 1 (Exception Infrastructure) must complete before Sections 2-11
- Sections 2-11 can be parallelized as they test independent resources
- Section 2 (AuthorizationServers) is recommended first as it's the root entity

## Test Count Summary

| Section | Tests |
|---------|-------|
| AuthorizationServers | ~8 |
| Users | ~8 |
| Applications | ~6 |
| ApplicationSecrets | ~5 |
| Clients | ~7 |
| Groups | ~7 |
| GroupMembers | ~6 |
| Scopes | ~7 |
| Schemas | ~7 |
| Templates | ~7 |
| **Total New** | **~68** |
| Existing (ScimApplications) | 11 |
| **Grand Total** | **~79** |
