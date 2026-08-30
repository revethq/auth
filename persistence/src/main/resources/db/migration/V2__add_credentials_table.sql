-- Add unified credentials table and migrate data from users.passwordHash
-- and revet_application_secrets.

-- 1. Create the revet_credentials table
CREATE TABLE IF NOT EXISTS revet_credentials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    userId          UUID,
    applicationId   UUID,
    authorizationServerId UUID,
    type            VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    name            VARCHAR(255),
    credentialHash  TEXT NOT NULL,
    expiresIn       INTEGER,
    createdOn       TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updatedOn       TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_revet_credentials_user ON revet_credentials (userId);
CREATE INDEX IF NOT EXISTS idx_revet_credentials_application ON revet_credentials (applicationId);
CREATE INDEX IF NOT EXISTS idx_revet_credentials_auth_server ON revet_credentials (authorizationServerId);
CREATE INDEX IF NOT EXISTS idx_revet_credentials_type_status ON revet_credentials (type, status);

-- 2. Migrate passwords from revet_users to revet_credentials (type PASSWORD)
INSERT INTO revet_credentials (id, userId, authorizationServerId, type, status, name, credentialHash, createdOn, updatedOn)
SELECT gen_random_uuid(), u.id, u.authorizationServerId, 'PASSWORD', 'ACTIVE', 'Password', u.passwordHash, u.createdOn, u.updatedOn
FROM revet_users u
WHERE u.passwordHash IS NOT NULL;

-- 3. Migrate application secrets to revet_credentials (type API_KEY)
-- Note: revet_application_secrets has no updatedOn column, use createdOn for both.
INSERT INTO revet_credentials (id, userId, applicationId, authorizationServerId, type, status, name, credentialHash, expiresIn, createdOn, updatedOn)
SELECT s.id, NULL, s.applicationId, s.authorizationServerId, 'API_KEY', 'ACTIVE', s.name, s.applicationSecretHash, s.expiresIn, s.createdOn, s.createdOn
FROM revet_application_secrets s;

-- 4. Update scope references from APPLICATION_SECRET (ordinal 2) to CREDENTIAL (ordinal 4)
-- ScopeReferenceType is stored as ordinal integers (no @Enumerated(STRING)).
-- Ordinals: AUTHORIZATION_SERVER=0, APPLICATION=1, APPLICATION_SECRET=2, CLIENT=3, CREDENTIAL=4
UPDATE revet_scope_references
SET scopeReferenceType = 4
WHERE scopeReferenceType = 2;

-- 5. Drop passwordHash column from revet_users
ALTER TABLE revet_users DROP COLUMN IF EXISTS passwordHash;

-- 6. Drop the revet_application_secrets table
DROP TABLE IF EXISTS revet_application_secrets;
