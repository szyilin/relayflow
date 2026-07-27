-- Tenant-level org invite global settings (C-class: lazy upsert, no join seed).

CREATE TABLE sys_tenant_invite_settings (
    id                          BIGINT          NOT NULL,
    tenant_id                   BIGINT          NOT NULL,
    invite_mode                 VARCHAR(32)     NOT NULL DEFAULT 'ADMIN_ONLY',
    non_admin_scope             VARCHAR(32)     NOT NULL DEFAULT 'INVITER_DEPT',
    non_admin_dept_ids          JSONB           NOT NULL DEFAULT '[]'::jsonb,
    non_admin_expire_days       INT             NOT NULL DEFAULT 3,
    non_admin_require_approval  BOOLEAN         NOT NULL DEFAULT FALSE,
    creator                     BIGINT,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                     BIGINT,
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                     SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_invite_settings PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_invite_settings_tenant
    ON sys_tenant_invite_settings (tenant_id)
    WHERE deleted = 0;

COMMENT ON TABLE sys_tenant_invite_settings IS 'Per-tenant org invite mode and non-admin constraints';
COMMENT ON COLUMN sys_tenant_invite_settings.invite_mode IS 'OFF | ADMIN_ONLY | ALL_MEMBERS';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_scope IS 'INVITER_DEPT | DESIGNATED_DEPTS';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_dept_ids IS 'JSON array of dept id strings';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_expire_days IS 'Invite TTL days for non-admin inviters';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_require_approval IS 'Whether non-admin invites need approval';
