-- Agent carrier ownership for group-embeddable bot catalog.
ALTER TABLE im_bot
    ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;

COMMENT ON COLUMN im_bot.owner_user_id IS 'Owner user for agent carrier bots; null for system bots';

CREATE INDEX IF NOT EXISTS idx_im_bot_owner_user
    ON im_bot (owner_user_id)
    WHERE owner_user_id IS NOT NULL AND deleted = 0;

-- Prefer agent_def.owner_user_id when carrier is bound.
UPDATE im_bot b
SET owner_user_id = a.owner_user_id,
    update_time = NOW()
FROM agent_def a
WHERE b.owner_agent_id = a.id
  AND b.owner_user_id IS NULL
  AND b.deleted = 0
  AND a.deleted = 0;

-- Fallback: earliest user enablement row for the bot.
UPDATE im_bot b
SET owner_user_id = e.user_id,
    update_time = NOW()
FROM (
    SELECT DISTINCT ON (bot_id) bot_id, user_id
    FROM im_bot_user_enablement
    WHERE deleted = 0
    ORDER BY bot_id, id ASC
) e
WHERE b.id = e.bot_id
  AND b.owner_agent_id IS NOT NULL
  AND b.owner_user_id IS NULL
  AND b.deleted = 0;
