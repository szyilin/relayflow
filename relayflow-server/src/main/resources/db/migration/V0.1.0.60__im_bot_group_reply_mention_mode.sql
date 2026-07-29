-- Bot Runtime group replies: platform @trigger user vs broadcast (none).
ALTER TABLE im_bot
    ADD COLUMN IF NOT EXISTS group_reply_mention_mode VARCHAR(16) NOT NULL DEFAULT 'trigger';

ALTER TABLE im_bot
    DROP CONSTRAINT IF EXISTS ck_im_bot_group_reply_mention_mode;

ALTER TABLE im_bot
    ADD CONSTRAINT ck_im_bot_group_reply_mention_mode
        CHECK (group_reply_mention_mode IN ('trigger', 'none'));

COMMENT ON COLUMN im_bot.group_reply_mention_mode IS
    'Group Bot Runtime reply mention: trigger=@trigger user; none=no auto mention (broadcast helpers)';
