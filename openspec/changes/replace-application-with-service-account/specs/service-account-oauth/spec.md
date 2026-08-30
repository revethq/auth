## ADDED Requirements

### Requirement: Client Credentials Flow with Service Accounts
The OAuth 2.1 client_credentials flow SHALL resolve the requesting principal as a service account by looking up the service account by ID (the `client_id` in the token request is the service account's UUID). The flow uses the unified Credential resource (from `add-credentials-resource`) for secret validation and the service account's assigned scopes for token scope resolution.

#### Scenario: Successful client_credentials token request
- **WHEN** a token request is made with `grant_type=client_credentials`, a valid `client_id` matching a service account's ID, and a valid `client_secret`
- **THEN** the system resolves the service account by ID
- **AND** validates the credential via CredentialService
- **AND** issues an access token with scopes limited to the intersection of requested scopes and the service account's assigned scopes

#### Scenario: Unknown client_id in client_credentials request
- **WHEN** a token request is made with `grant_type=client_credentials` and a `client_id` that does not match any service account
- **THEN** the system returns an `invalid_client` error response

#### Scenario: Token includes service account identity
- **WHEN** an access token is issued for a service account via client_credentials
- **THEN** the token's `sub` claim contains the service account's ID
- **AND** the token's `client_id` claim contains the service account's ID

### Requirement: Authorization Server Discovery with Service Accounts
The `.well-known/openid-configuration` endpoint SHALL continue to advertise `client_credentials` as a supported grant type. The token endpoint behavior is unchanged from the caller's perspective.

#### Scenario: Discovery endpoint unchanged
- **WHEN** a GET request is made to `/.well-known/openid-configuration`
- **THEN** `grant_types_supported` includes `client_credentials`
- **AND** no Application-specific references appear in the response
