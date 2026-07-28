-- Enterprise logo + migrate seed tenant code off readable word "default".

ALTER TABLE sys_tenant
    ADD COLUMN IF NOT EXISTS logo_file_id BIGINT;

COMMENT ON COLUMN sys_tenant.logo_file_id IS 'Enterprise logo infra_file.id (nullable)';

-- Opaque demo code (Feishu-style); unique index uk_sys_tenant_code already exists.
UPDATE sys_tenant
SET code = 'RFDEMO7K9X2',
    update_time = NOW()
WHERE code = 'default'
  AND deleted = 0;
