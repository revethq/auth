## ADDED Requirements

### Requirement: Revet Table Namespace
All JPA entities in the auth project SHALL use explicit `@Table(name = "revet_...")` annotations with snake_case table names. This ensures a consistent namespace across all Revet projects and avoids collisions with PostgreSQL reserved words.

#### Scenario: All entities use revet_ prefix
- **WHEN** any JPA entity is defined in the persistence module
- **THEN** it MUST have an explicit `@Table(name = "revet_<snake_case_name>")` annotation
- **AND** the table name uses snake_case (e.g. `revet_authorization_servers`, not `revet_authorizationservers`)

#### Scenario: Join tables use revet_ prefix
- **WHEN** a `@JoinTable` is used for entity relationships (e.g. scope associations)
- **THEN** the join table name MUST use the `revet_` prefix (e.g. `revet_scope_references`)

#### Scenario: Index and constraint names are consistent
- **WHEN** explicit index or constraint names are defined on a table
- **THEN** the names SHOULD use the `idx_revet_` prefix for indexes and `uk_revet_` prefix for unique constraints

### Requirement: Migration Script
A SQL migration script SHALL be provided to rename existing tables in PostgreSQL from their current names to the new `revet_` prefixed names using `ALTER TABLE ... RENAME TO` statements.

#### Scenario: Migration renames all tables
- **WHEN** the migration script is executed against an existing database
- **THEN** all tables are renamed from their current names to the `revet_` prefixed names
- **AND** all data is preserved (ALTER TABLE RENAME is metadata-only, no data movement)

#### Scenario: Migration is idempotent
- **WHEN** the migration script is run against a database that already has `revet_` prefixed tables
- **THEN** it does not fail (uses `IF EXISTS` or equivalent guarding)
