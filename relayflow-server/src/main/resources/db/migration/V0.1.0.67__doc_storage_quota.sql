-- Docs storage quota (Wave A2): default + per-user rules + permission seeds.

ALTER TABLE doc_tenant_settings
    ADD COLUMN storage_quota_default_bytes BIGINT;

COMMENT ON COLUMN doc_tenant_settings.storage_quota_default_bytes IS
    'Default FILE storage quota in bytes for all members; NULL = unlimited';

CREATE TABLE doc_storage_quota_user_rule (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    quota_bytes     BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_storage_quota_user_rule PRIMARY KEY (id),
    CONSTRAINT uk_doc_storage_quota_user_rule UNIQUE (tenant_id, user_id)
);

COMMENT ON TABLE doc_storage_quota_user_rule IS
    'Per-user docs FILE storage quota override; quota_bytes NULL = unlimited for that user';

CREATE INDEX idx_doc_storage_quota_user_rule_tenant
    ON doc_storage_quota_user_rule (tenant_id);

-- Permissions under 产品设置 parent 1700
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1740, 1, 1700, '云文档存储配额', 'docs:storage-quota', 1, 5, 0, NOW(), NOW()),
    (1741, 1, 1740, '云文档存储配额查询', 'docs:storage-quota:query', 2, 1, 0, NOW(), NOW()),
    (1742, 1, 1740, '云文档存储配额更新', 'docs:storage-quota:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11740, 1, 100, 1740, NOW(), NOW()),
    (11741, 1, 100, 1741, NOW(), NOW()),
    (11742, 1, 100, 1742, NOW(), NOW());
