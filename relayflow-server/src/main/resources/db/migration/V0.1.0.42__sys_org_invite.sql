-- Org invite process entity (consume writes membership; C-class no join seed for shared).

CREATE TABLE sys_org_invite (
    id                      BIGINT          NOT NULL,
    tenant_id               BIGINT          NOT NULL,
    channel                 VARCHAR(32)     NOT NULL,
    token                   VARCHAR(64),
    invite_code             VARCHAR(16),
    inviter_user_id         BIGINT          NOT NULL,
    target_mobile           VARCHAR(20),
    target_user_id          BIGINT,
    target_dept_id          BIGINT,
    target_role_ids         JSONB           NOT NULL DEFAULT '[]'::jsonb,
    status                  VARCHAR(32)     NOT NULL,
    expires_at              TIMESTAMPTZ,
    approved_by             BIGINT,
    approved_at             TIMESTAMPTZ,
    consumed_at             TIMESTAMPTZ,
    membership_user_id      BIGINT,
    creator                 BIGINT,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                 BIGINT,
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_org_invite PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_org_invite_token
    ON sys_org_invite (token)
    WHERE deleted = 0 AND token IS NOT NULL;

CREATE UNIQUE INDEX uk_sys_org_invite_code
    ON sys_org_invite (invite_code)
    WHERE deleted = 0 AND invite_code IS NOT NULL AND status = 'ACTIVE';

CREATE INDEX idx_sys_org_invite_tenant_status
    ON sys_org_invite (tenant_id, status)
    WHERE deleted = 0;

CREATE INDEX idx_sys_org_invite_tenant_mobile_status
    ON sys_org_invite (tenant_id, target_mobile, status)
    WHERE deleted = 0 AND target_mobile IS NOT NULL;

COMMENT ON TABLE sys_org_invite IS 'Org invite process entity; membership written on consume';
COMMENT ON COLUMN sys_org_invite.channel IS 'SHARED | MOBILE (LINK/QR/CODE share SHARED row)';
COMMENT ON COLUMN sys_org_invite.status IS 'ACTIVE | PENDING_APPROVAL | CONSUMED | REVOKED | EXPIRED | REJECTED';
