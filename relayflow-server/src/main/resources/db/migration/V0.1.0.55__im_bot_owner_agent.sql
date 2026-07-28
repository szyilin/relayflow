-- Agent carrier bot binding on im_bot (im-bot-agent-carrier-v1).

ALTER TABLE im_bot
    ADD COLUMN IF NOT EXISTS owner_agent_id BIGINT;

COMMENT ON COLUMN im_bot.owner_agent_id IS 'Bound AgentDef.id for type=tenant carrier bots; null for system bots';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_bot_owner_agent
    ON im_bot (owner_agent_id)
    WHERE deleted = 0 AND owner_agent_id IS NOT NULL;
