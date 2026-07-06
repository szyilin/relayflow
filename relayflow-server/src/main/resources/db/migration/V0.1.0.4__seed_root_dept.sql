-- Seed root department for default tenant when none exists.
INSERT INTO sys_dept (id, tenant_id, parent_id, name, sort, status, create_time, update_time)
SELECT 1, 1, 0, '总部', 0, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dept WHERE tenant_id = 1 AND deleted = 0
);
