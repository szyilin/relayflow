-- Per-member unread @me flag for conversation list "[有人@我]" indicator.
ALTER TABLE im_conversation_member
    ADD COLUMN IF NOT EXISTS mention_unread SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN im_conversation_member.mention_unread IS
    '1 when this member has unread user @mention(s); cleared on mark read';
