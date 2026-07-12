-- org-member-dept-default: align canonical root dept name with tenant; backfill primary dept for members without one.
-- Canonical root = lowest id per tenant where parent_id = 0 (handles legacy duplicate roots).

UPDATE sys_dept d
SET name = t.name,
    update_time = NOW()
FROM sys_tenant t
WHERE d.tenant_id = t.id
  AND d.parent_id = 0
  AND d.deleted = 0
  AND t.deleted = 0
  AND d.id = (
      SELECT MIN(d2.id)
      FROM sys_dept d2
      WHERE d2.tenant_id = d.tenant_id
        AND d2.parent_id = 0
        AND d2.deleted = 0
  );

INSERT INTO sys_user_dept (id, tenant_id, user_id, dept_id, primary_flag, create_time, update_time)
SELECT
    7100000000000000000 + tu.id,
    tu.tenant_id,
    tu.user_id,
    root.id,
    1,
    NOW(),
    NOW()
FROM sys_tenant_user tu
INNER JOIN LATERAL (
    SELECT d.id
    FROM sys_dept d
    WHERE d.tenant_id = tu.tenant_id
      AND d.parent_id = 0
      AND d.deleted = 0
    ORDER BY d.id
    LIMIT 1
) root ON TRUE
WHERE tu.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_dept ud
      WHERE ud.tenant_id = tu.tenant_id
        AND ud.user_id = tu.user_id
        AND ud.deleted = 0
  );
