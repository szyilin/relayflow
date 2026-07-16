-- Bot catalog type: system (no subscription tables) | tenant (union of tenant/user enablement).
-- See openspec/changes/im-bot-reach-policy-v1/design.md D1.

ALTER TABLE im_bot
    ADD COLUMN IF NOT EXISTS type VARCHAR(32) NOT NULL DEFAULT 'tenant';

ALTER TABLE im_bot
    DROP CONSTRAINT IF EXISTS ck_im_bot_type;

ALTER TABLE im_bot
    ADD CONSTRAINT ck_im_bot_type CHECK (type IN ('system', 'tenant'));

-- Seeded platform assistants are system bots (deliver without enablement rows).
UPDATE im_bot
SET type = 'system',
    update_time = NOW()
WHERE code IN ('org-assistant', 'task-bot', 'approval-bot', 'account-security', 'invite-helper')
  AND deleted = 0;
