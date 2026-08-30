-- Rename all tables to revet_ namespace.
-- ALTER TABLE ... RENAME TO is metadata-only (no data movement).
-- Guarded with IF EXISTS for idempotency.

ALTER TABLE IF EXISTS authorizationserver RENAME TO revet_authorization_servers;
ALTER TABLE IF EXISTS client RENAME TO revet_clients;
ALTER TABLE IF EXISTS clientcode RENAME TO revet_client_codes;
ALTER TABLE IF EXISTS event RENAME TO revet_events;
ALTER TABLE IF EXISTS groups RENAME TO revet_groups;
ALTER TABLE IF EXISTS group_members RENAME TO revet_group_members;
ALTER TABLE IF EXISTS identityprovider RENAME TO revet_identity_providers;
ALTER TABLE IF EXISTS profile RENAME TO revet_profiles;
ALTER TABLE IF EXISTS refresh_tokens RENAME TO revet_refresh_tokens;
ALTER TABLE IF EXISTS schema RENAME TO revet_schemas;
ALTER TABLE IF EXISTS scope RENAME TO revet_scopes;
ALTER TABLE IF EXISTS scopereference RENAME TO revet_scope_references;
ALTER TABLE IF EXISTS signingkey RENAME TO revet_signing_keys;
ALTER TABLE IF EXISTS template RENAME TO revet_templates;
ALTER TABLE IF EXISTS users RENAME TO revet_users;
ALTER TABLE IF EXISTS application RENAME TO revet_applications;
ALTER TABLE IF EXISTS applicationsecret RENAME TO revet_application_secrets;
ALTER TABLE IF EXISTS scim_application RENAME TO revet_scim_applications;
ALTER TABLE IF EXISTS scim_delivery_status RENAME TO revet_scim_delivery_statuses;
ALTER TABLE IF EXISTS scim_resource_mapping RENAME TO revet_scim_resource_mappings;

-- Rename indexes on SCIM tables
ALTER INDEX IF EXISTS idx_scim_app_auth_server RENAME TO idx_revet_scim_app_auth_server;
ALTER INDEX IF EXISTS idx_scim_app_application RENAME TO idx_revet_scim_app_application;
ALTER INDEX IF EXISTS idx_scim_app_enabled RENAME TO idx_revet_scim_app_enabled;
ALTER INDEX IF EXISTS idx_scim_delivery_event RENAME TO idx_revet_scim_delivery_event;
ALTER INDEX IF EXISTS idx_scim_delivery_app RENAME TO idx_revet_scim_delivery_app;
ALTER INDEX IF EXISTS idx_scim_delivery_status_retry RENAME TO idx_revet_scim_delivery_status_retry;
ALTER INDEX IF EXISTS idx_scim_delivery_created RENAME TO idx_revet_scim_delivery_created;
ALTER INDEX IF EXISTS idx_scim_mapping_app RENAME TO idx_revet_scim_mapping_app;
ALTER INDEX IF EXISTS idx_scim_mapping_local RENAME TO idx_revet_scim_mapping_local;

-- Rename unique constraints on SCIM tables
ALTER INDEX IF EXISTS uk_scim_mapping_local_resource RENAME TO uk_revet_scim_mapping_local_resource;
