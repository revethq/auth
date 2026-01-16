# Proposal: Add Integration Tests for All Resources

## Summary

Add comprehensive integration tests for all REST API resources in Revet Auth using Quarkus DevServices and RestAssured. Currently, only `ScimApplicationResource` has integration tests. This proposal covers adding tests for the remaining 10 resource endpoints.

## Problem Statement

The project lacks integration test coverage for most REST API endpoints. Without integration tests:
- Regressions can go undetected during refactoring
- API contract compliance is not verified
- Edge cases and error handling are not validated
- New contributors have no reference for expected behavior

## Proposed Solution

Create integration tests for all REST resources using the existing test infrastructure:
- **Quarkus DevServices** for PostgreSQL (already configured)
- **RestAssured** for HTTP testing (already configured)
- **@QuarkusTest** annotation for integration test context

## Scope

### Resources to Test (10 total)

| Resource | API Interface | Endpoints |
|----------|---------------|-----------|
| AuthorizationServers | `AuthorizationServersApi` | CRUD (5) |
| Users | `UsersApi` | CRUD (5) |
| Applications | `ApplicationsApi` | CRUD (4) |
| ApplicationSecrets | `ApplicationSecretsApi` | CRUD |
| Clients | `ClientsApi` | CRUD |
| Groups | `GroupsApi` | CRUD |
| GroupMembers | `GroupMembersApi` | CRUD |
| Scopes | `ScopesApi` | CRUD |
| Schemas | `SchemasApi` | CRUD |
| Templates | `TemplatesApi` | CRUD |

### Already Tested (1)
- ScimApplicationResource - 11 tests

### Out of Scope
- OAuth/OIDC flow tests (AuthorizationServer authorization endpoints)
- Performance/load testing
- Security/penetration testing

## Success Criteria

1. All resources have integration tests covering:
   - Create (POST) - success and validation errors
   - Read single (GET by ID) - success and 404
   - Read list (GET) - with pagination
   - Update (PUT) - success and 404
   - Delete (DELETE) - success and 404
2. All tests pass with `./gradlew test`
3. Tests use DevServices for isolated database per test run

## Dependencies

- Existing test infrastructure (DevServices, RestAssured)
- NotFound exception mappers for untested resources

## Risks

- **Entity relationship constraints**: Tests may uncover bugs similar to the Scope `@OneToOne` issue fixed during SCIM testing
- **Transaction management**: Services using manual transactions may conflict with test transactions
- **Missing exception mappers**: NotFound exceptions may not return proper 404 responses

## Alternatives Considered

1. **Mock-based unit tests**: Rejected - doesn't validate actual HTTP behavior and database interactions
2. **Testcontainers explicit**: Rejected - DevServices is simpler and already configured
3. **Contract testing (Pact)**: Deferred - useful for API consumers but overkill for initial coverage
