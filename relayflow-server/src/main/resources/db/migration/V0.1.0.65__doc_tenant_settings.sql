-- Docs tenant product settings (org policy Wave B) + permission seeds.

CREATE TABLE doc_tenant_settings (
    tenant_id                   BIGINT          NOT NULL,
    member_share_enabled        BOOLEAN         NOT NULL DEFAULT TRUE,
    markdown_export_enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    drive_upload_enabled        BOOLEAN         NOT NULL DEFAULT TRUE,
    creator                     BIGINT,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                     BIGINT,
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                     SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_tenant_settings PRIMARY KEY (tenant_id)
);

COMMENT ON TABLE doc_tenant_settings IS 'Tenant-level docs product settings (org policy)';

-- Permissions under 产品设置 parent 1700
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1720, 1, 1700, '云文档产品设置', 'docs:product-settings', 1, 3, 0, NOW(), NOW()),
    (1721, 1, 1720, '云文档产品设置查询', 'docs:product-settings:query', 2, 1, 0, NOW(), NOW()),
    (1722, 1, 1720, '云文档产品设置更新', 'docs:product-settings:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11720, 1, 100, 1720, NOW(), NOW()),
    (11721, 1, 100, 1721, NOW(), NOW()),
    (11722, 1, 100, 1722, NOW(), NOW());
