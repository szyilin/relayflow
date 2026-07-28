-- Enterprise profile columns on sys_tenant + tenant addresses + permissions.

ALTER TABLE sys_tenant
    ADD COLUMN IF NOT EXISTS industry VARCHAR(64),
    ADD COLUMN IF NOT EXISTS scale VARCHAR(64),
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(64),
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(128),
    ADD COLUMN IF NOT EXISTS contact_mobile VARCHAR(32),
    ADD COLUMN IF NOT EXISTS contact_landline VARCHAR(32);

COMMENT ON COLUMN sys_tenant.industry IS 'Enterprise industry code';
COMMENT ON COLUMN sys_tenant.scale IS 'Enterprise scale code';
COMMENT ON COLUMN sys_tenant.contact_name IS 'Enterprise contact name';
COMMENT ON COLUMN sys_tenant.contact_email IS 'Enterprise contact email';
COMMENT ON COLUMN sys_tenant.contact_mobile IS 'Enterprise contact mobile';
COMMENT ON COLUMN sys_tenant.contact_landline IS 'Enterprise contact landline';

CREATE TABLE sys_tenant_address (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    types           JSONB           NOT NULL,
    region_id       BIGINT          NOT NULL,
    region_path     VARCHAR(512)    NOT NULL,
    detail          VARCHAR(512)    NOT NULL DEFAULT '',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ENABLED',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_address PRIMARY KEY (id)
);

CREATE INDEX idx_sys_tenant_address_tenant ON sys_tenant_address (tenant_id) WHERE deleted = 0;
CREATE INDEX idx_sys_tenant_address_status ON sys_tenant_address (tenant_id, status) WHERE deleted = 0;

COMMENT ON TABLE sys_tenant_address IS 'Per-tenant enterprise addresses (job/interview/store types)';
COMMENT ON COLUMN sys_tenant_address.types IS 'JSON array of JOB | INTERVIEW | STORE';
COMMENT ON COLUMN sys_tenant_address.region_id IS 'infra_geo_region.id';
COMMENT ON COLUMN sys_tenant_address.status IS 'ENABLED | DISABLED';

-- Permissions under 系统管理 (parent 1000)
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1600, 1, 1000, '企业信息', 'system:enterprise', 1, 6, 0, NOW(), NOW()),
    (1601, 1, 1600, '企业信息查询', 'system:enterprise:query', 2, 1, 0, NOW(), NOW()),
    (1602, 1, 1600, '企业信息更新', 'system:enterprise:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11600, 1, 100, 1600, NOW(), NOW()),
    (11601, 1, 100, 1601, NOW(), NOW()),
    (11602, 1, 100, 1602, NOW(), NOW());
