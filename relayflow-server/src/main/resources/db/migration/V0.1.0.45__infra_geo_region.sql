-- Platform geo regions (no tenant_id) + sync metadata + permission for cascade API.

CREATE TABLE infra_geo_region (
    id              BIGINT          NOT NULL,
    code            VARCHAR(32)     NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    level           VARCHAR(16)     NOT NULL,
    parent_id       BIGINT,
    sort            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_geo_region PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_infra_geo_region_code ON infra_geo_region (code) WHERE deleted = 0;
CREATE INDEX idx_infra_geo_region_parent ON infra_geo_region (parent_id) WHERE deleted = 0;
CREATE INDEX idx_infra_geo_region_level ON infra_geo_region (level) WHERE deleted = 0;

COMMENT ON TABLE infra_geo_region IS 'Platform China admin-division tree; shared across tenants';
COMMENT ON COLUMN infra_geo_region.level IS 'COUNTRY | PROVINCE | CITY | DISTRICT';
COMMENT ON COLUMN infra_geo_region.code IS 'Stable division code (e.g. GB/T 2260 style or CN)';

CREATE TABLE infra_geo_sync_meta (
    id              BIGINT          NOT NULL,
    source          VARCHAR(128)    NOT NULL,
    data_version    VARCHAR(64)     NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    region_count    INT             NOT NULL DEFAULT 0,
    message         VARCHAR(512),
    synced_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_geo_sync_meta PRIMARY KEY (id)
);

CREATE INDEX idx_infra_geo_sync_meta_synced ON infra_geo_sync_meta (synced_at DESC);

COMMENT ON TABLE infra_geo_sync_meta IS 'China geo sync run log (source + version)';

-- Permissions (template tenant 1; new tenants copy via bootstrap)
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (2300, 1, 2000, '行政区划', 'infra:geo', 1, 3, 0, NOW(), NOW()),
    (2301, 1, 2300, '区划查询', 'infra:geo:query', 2, 1, 0, NOW(), NOW()),
    (2302, 1, 2300, '区划同步', 'infra:geo:sync', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (23001, 1, 100, 2300, NOW(), NOW()),
    (23002, 1, 100, 2301, NOW(), NOW()),
    (23003, 1, 100, 2302, NOW(), NOW());
