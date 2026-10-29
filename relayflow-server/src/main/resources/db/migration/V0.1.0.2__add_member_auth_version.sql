-- Existing memberships and legacy JWTs start at version zero.
ALTER TABLE sys_tenant_user ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN sys_tenant_user.auth_version IS 'Persistent member session version; increment on leaving ACTIVE';
