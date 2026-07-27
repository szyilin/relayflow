-- Backfill MOBILE org invites from historical NOT_JOINED memberships (0.x redesign).

INSERT INTO sys_org_invite (
    id,
    tenant_id,
    channel,
    token,
    invite_code,
    inviter_user_id,
    target_mobile,
    target_user_id,
    target_dept_id,
    target_role_ids,
    status,
    expires_at,
    membership_user_id,
    creator,
    create_time,
    updater,
    update_time,
    deleted
)
SELECT
    (9100000000000000000 + tu.id % 100000000000) AS id,
    tu.tenant_id,
    'MOBILE' AS channel,
    NULL AS token,
    NULL AS invite_code,
    COALESCE(tu.creator, 1) AS inviter_user_id,
    u.mobile AS target_mobile,
    tu.user_id AS target_user_id,
    NULL AS target_dept_id,
    '[]'::jsonb AS target_role_ids,
    'ACTIVE' AS status,
    NOW() + INTERVAL '30 days' AS expires_at,
    NULL AS membership_user_id,
    tu.creator,
    COALESCE(tu.create_time, NOW()),
    tu.updater,
    COALESCE(tu.update_time, NOW()),
    0
FROM sys_tenant_user tu
JOIN sys_user u ON u.id = tu.user_id AND u.deleted = 0
WHERE tu.deleted = 0
  AND tu.status = 'NOT_JOINED'
  AND u.mobile IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_org_invite oi
      WHERE oi.deleted = 0
        AND oi.tenant_id = tu.tenant_id
        AND oi.channel = 'MOBILE'
        AND oi.target_user_id = tu.user_id
        AND oi.status IN ('ACTIVE', 'PENDING_APPROVAL')
  );
