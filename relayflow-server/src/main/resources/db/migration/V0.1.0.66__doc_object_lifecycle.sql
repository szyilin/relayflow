-- Docs object lifecycle for admin file ops (Wave A1) + permission seeds.

ALTER TABLE doc_object
    ADD COLUMN lifecycle VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN deleted_at TIMESTAMPTZ;

COMMENT ON COLUMN doc_object.lifecycle IS 'ACTIVE | DELETED | PURGED';
COMMENT ON COLUMN doc_object.deleted_at IS 'When lifecycle became DELETED; used for 90-day restore window';

-- Backfill: soft-deleted rows → DELETED
UPDATE doc_object
SET lifecycle = 'DELETED',
    deleted_at = COALESCE(update_time, NOW())
WHERE deleted = 1
  AND lifecycle = 'ACTIVE';

ALTER TABLE doc_object
    ADD CONSTRAINT ck_doc_object_lifecycle
        CHECK (lifecycle IN ('ACTIVE', 'DELETED', 'PURGED'));

CREATE INDEX idx_doc_object_lifecycle
    ON doc_object (tenant_id, lifecycle, update_time DESC);

CREATE INDEX idx_doc_object_owner_lifecycle
    ON doc_object (tenant_id, owner_user_id, lifecycle);

-- Permissions under 产品设置 parent 1700
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1730, 1, 1700, '云文档文件管理', 'docs:files', 1, 4, 0, NOW(), NOW()),
    (1731, 1, 1730, '云文档文件查询', 'docs:files:query', 2, 1, 0, NOW(), NOW()),
    (1732, 1, 1730, '云文档文件转移所有者', 'docs:files:transfer', 2, 2, 0, NOW(), NOW()),
    (1733, 1, 1730, '云文档文件删除', 'docs:files:delete', 2, 3, 0, NOW(), NOW()),
    (1734, 1, 1730, '云文档文件恢复', 'docs:files:restore', 2, 4, 0, NOW(), NOW()),
    (1735, 1, 1730, '云文档文件销毁', 'docs:files:purge', 2, 5, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11730, 1, 100, 1730, NOW(), NOW()),
    (11731, 1, 100, 1731, NOW(), NOW()),
    (11732, 1, 100, 1732, NOW(), NOW()),
    (11733, 1, 100, 1733, NOW(), NOW()),
    (11734, 1, 100, 1734, NOW(), NOW()),
    (11735, 1, 100, 1735, NOW(), NOW());
