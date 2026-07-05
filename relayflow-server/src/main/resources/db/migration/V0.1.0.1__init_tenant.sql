-- Tenant metadata tables and default seed (V1 single-tenant mode).
-- Convention for later migrations:
--   - All sys_/infra_/im_ business tables MUST include tenant_id BIGINT NOT NULL.
--   - Tenant-scoped unique constraints: UNIQUE (tenant_id, ...).

CREATE TABLE sys_tenant (
    id              BIGINT          NOT NULL,
    code            VARCHAR(64)     NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    status          SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_code ON sys_tenant (code) WHERE deleted = 0;

CREATE TABLE sys_tenant_user (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_user PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_user ON sys_tenant_user (tenant_id, user_id) WHERE deleted = 0;
CREATE INDEX idx_sys_tenant_user_user_id ON sys_tenant_user (user_id);

INSERT INTO sys_tenant (id, code, name, status, create_time, update_time)
VALUES (1, 'default', '默认企业', 0, NOW(), NOW());
