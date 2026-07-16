-- Seed system bots + default tenant enablement for tenant_id=1.
-- Codes are stable; IDs are fixed for deterministic local/dev environments.

INSERT INTO im_bot (id, code, name, description, scope, enable_policy, handler_kind, status, create_time, update_time)
VALUES
    (900001, 'org-assistant', '组织助手', '组织与角色等相关提醒', 'tenant', 'default_on', 'noop', 1, NOW(), NOW()),
    (900002, 'invite-helper', '邀请助手', '跨企业成员邀请提醒', 'identity_fanout', 'mandatory', 'noop', 1, NOW(), NOW()),
    (900003, 'task-bot', '任务助手', '任务到期与指派提醒', 'tenant', 'default_on', 'noop', 1, NOW(), NOW()),
    (900004, 'approval-bot', '审批助手', '审批待办提醒', 'tenant', 'default_on', 'noop', 1, NOW(), NOW()),
    (900005, 'account-security', '账号安全', '账号安全类提醒', 'identity_fanout', 'mandatory', 'noop', 1, NOW(), NOW());

-- Default tenant (V1 tenant_id=1): enable all seeded system bots.
INSERT INTO im_bot_tenant_enablement (id, tenant_id, bot_id, enabled, create_time, update_time)
VALUES
    (900101, 1, 900001, 1, NOW(), NOW()),
    (900102, 1, 900002, 1, NOW(), NOW()),
    (900103, 1, 900003, 1, NOW(), NOW()),
    (900104, 1, 900004, 1, NOW(), NOW()),
    (900105, 1, 900005, 1, NOW(), NOW());

-- Bootstrap admin (user_id=1) user enablement for mandatory/default_on bots already tenant-enabled.
INSERT INTO im_bot_user_enablement (id, tenant_id, user_id, bot_id, create_time, update_time)
VALUES
    (900201, 1, 1, 900001, NOW(), NOW()),
    (900202, 1, 1, 900002, NOW(), NOW()),
    (900203, 1, 1, 900003, NOW(), NOW()),
    (900204, 1, 1, 900004, NOW(), NOW()),
    (900205, 1, 1, 900005, NOW(), NOW());
