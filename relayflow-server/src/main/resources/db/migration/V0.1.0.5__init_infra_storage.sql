-- Infra domain: tenant storage providers, file metadata, upload sessions, business bindings.
-- Seeds infra:storage:* and infra:file:* permissions for default tenant.

-- 5.1 Tenant-configurable object storage providers
CREATE TABLE infra_storage_provider (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    provider        VARCHAR(32)     NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'active',
    is_default      SMALLINT        NOT NULL DEFAULT 0,
    config_json     TEXT            NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_storage_provider PRIMARY KEY (id)
);

CREATE INDEX idx_infra_storage_provider_tenant ON infra_storage_provider (tenant_id);
CREATE UNIQUE INDEX uk_infra_storage_provider_tenant_provider
    ON infra_storage_provider (tenant_id, provider) WHERE deleted = 0;

-- 5.2 File metadata (confirmed uploads)
CREATE TABLE infra_file (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    provider        VARCHAR(32)     NOT NULL,
    storage_uri     VARCHAR(512)    NOT NULL,
    object_key      VARCHAR(512)    NOT NULL,
    original_name   VARCHAR(256)    NOT NULL,
    mime_type       VARCHAR(128)    NOT NULL DEFAULT '',
    size            BIGINT          NOT NULL DEFAULT 0,
    sha256          VARCHAR(64),
    access_level    VARCHAR(16)     NOT NULL DEFAULT 'private',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_file PRIMARY KEY (id)
);

CREATE INDEX idx_infra_file_tenant_provider ON infra_file (tenant_id, provider);
CREATE INDEX idx_infra_file_tenant_create ON infra_file (tenant_id, create_time);

-- 5.3 Direct-upload sessions (pending / confirmed / expired)
CREATE TABLE infra_file_upload_session (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'pending',
    provider        VARCHAR(32)     NOT NULL,
    object_key      VARCHAR(512)    NOT NULL,
    original_name   VARCHAR(256)    NOT NULL,
    mime_type       VARCHAR(128)    NOT NULL DEFAULT '',
    size            BIGINT          NOT NULL DEFAULT 0,
    access_level    VARCHAR(16)     NOT NULL DEFAULT 'private',
    expires_at      TIMESTAMPTZ     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_file_upload_session PRIMARY KEY (id)
);

CREATE INDEX idx_infra_file_upload_session_tenant_status
    ON infra_file_upload_session (tenant_id, status);
CREATE INDEX idx_infra_file_upload_session_expires
    ON infra_file_upload_session (expires_at) WHERE deleted = 0 AND status = 'pending';

-- 5.4 File-to-business entity bindings
CREATE TABLE infra_file_binding (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    file_id         BIGINT          NOT NULL,
    biz_type        VARCHAR(64)     NOT NULL,
    biz_id          BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_file_binding PRIMARY KEY (id)
);

CREATE INDEX idx_infra_file_binding_file ON infra_file_binding (tenant_id, file_id);
CREATE UNIQUE INDEX uk_infra_file_binding_biz
    ON infra_file_binding (tenant_id, biz_type, biz_id, file_id) WHERE deleted = 0;

-- 5.5 Seed: infra permission tree + super_admin bindings
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (2000, 1, 0, '基础设施', 'infra', 1, 10, 0, NOW(), NOW()),
    (2100, 1, 2000, '存储设置', 'infra:storage', 1, 1, 0, NOW(), NOW()),
    (2101, 1, 2100, '存储查询', 'infra:storage:query', 2, 1, 0, NOW(), NOW()),
    (2102, 1, 2100, '存储更新', 'infra:storage:update', 2, 2, 0, NOW(), NOW()),
    (2103, 1, 2100, '存储测试连接', 'infra:storage:test', 2, 3, 0, NOW(), NOW()),
    (2200, 1, 2000, '文件管理', 'infra:file', 1, 2, 0, NOW(), NOW()),
    (2201, 1, 2200, '文件列表', 'infra:file:list', 2, 1, 0, NOW(), NOW()),
    (2202, 1, 2200, '文件上传', 'infra:file:upload', 2, 2, 0, NOW(), NOW()),
    (2203, 1, 2200, '文件下载', 'infra:file:download', 2, 3, 0, NOW(), NOW()),
    (2204, 1, 2200, '文件删除', 'infra:file:delete', 2, 4, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
SELECT
    20000 + id,
    1,
    100,
    id,
    NOW(),
    NOW()
FROM sys_permission
WHERE tenant_id = 1 AND id >= 2000 AND id < 3000 AND deleted = 0;
