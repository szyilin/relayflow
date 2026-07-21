-- Tenant-scoped member profile (nickname/avatar/signature/cover).
-- Account credentials stay on sys_user; display fields follow (tenant_id, user_id).

ALTER TABLE sys_tenant_user
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(64),
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(512),
    ADD COLUMN IF NOT EXISTS signature VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS cover_file_id VARCHAR(512);

COMMENT ON COLUMN sys_tenant_user.nickname IS 'Display nickname within this tenant; null falls back to sys_user';
COMMENT ON COLUMN sys_tenant_user.avatar IS 'Avatar fileId within this tenant';
COMMENT ON COLUMN sys_tenant_user.signature IS 'Personal signature on business card within this tenant';
COMMENT ON COLUMN sys_tenant_user.cover_file_id IS 'Cover image fileId within this tenant';

-- Backfill existing ACTIVE (and any) memberships from global sys_user once.
UPDATE sys_tenant_user tu
SET nickname = COALESCE(tu.nickname, NULLIF(TRIM(u.nickname), ''), u.username),
    avatar = COALESCE(tu.avatar, NULLIF(TRIM(u.avatar), '')),
    signature = CASE
        WHEN tu.signature IS NOT NULL AND tu.signature <> '' THEN tu.signature
        ELSE COALESCE(u.signature, '')
    END,
    cover_file_id = COALESCE(tu.cover_file_id, NULLIF(TRIM(u.cover_file_id), ''))
FROM sys_user u
WHERE tu.user_id = u.id
  AND tu.deleted = 0
  AND u.deleted = 0;
