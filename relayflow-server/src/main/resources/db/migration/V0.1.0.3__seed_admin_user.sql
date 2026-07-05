-- Dev bootstrap: default super admin for V1 single-tenant mode.
-- Credentials: admin / admin123 — change password after first login in non-dev environments.

INSERT INTO sys_user (id, username, password, nickname, create_time, update_time)
VALUES (
    1,
    'admin',
    '$2b$10$Pe7vdtU2gx0Jy/FpgchTDO6TTpg481EIlN8JIqsaUhukEOaKQ9NVa',
    '管理员',
    NOW(),
    NOW()
);

INSERT INTO sys_tenant_user (id, tenant_id, user_id, status, create_time, update_time)
VALUES (1, 1, 1, 'ACTIVE', NOW(), NOW());

INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time, update_time)
VALUES (1, 1, 1, 100, NOW(), NOW());

UPDATE sys_tenant
SET owner_user_id = 1,
    update_time = NOW()
WHERE id = 1;
