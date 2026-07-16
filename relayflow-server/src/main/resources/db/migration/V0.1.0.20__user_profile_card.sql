-- Profile card fields + viewer-scoped contact remarks.

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS signature VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS cover_file_id VARCHAR(512);

COMMENT ON COLUMN sys_user.signature IS 'Personal signature shown on business card';
COMMENT ON COLUMN sys_user.cover_file_id IS 'Cover image file id (same scheme as avatar)';

CREATE TABLE sys_contact_remark (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    target_user_id  BIGINT          NOT NULL,
    remark_name     VARCHAR(64)     NOT NULL DEFAULT '',
    description     VARCHAR(500)    NOT NULL DEFAULT '',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_contact_remark PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_contact_remark_owner_target
    ON sys_contact_remark (tenant_id, owner_user_id, target_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_sys_contact_remark_owner
    ON sys_contact_remark (tenant_id, owner_user_id);
