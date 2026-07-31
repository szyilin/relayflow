-- Dual watermark (badge_seq), pin timestamp, flag; user-private conversation tags.

ALTER TABLE im_conversation_member
    ADD COLUMN IF NOT EXISTS badge_seq BIGINT NOT NULL DEFAULT 0;

ALTER TABLE im_conversation_member
    ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMPTZ;

ALTER TABLE im_conversation_member
    ADD COLUMN IF NOT EXISTS flagged SMALLINT NOT NULL DEFAULT 0;

-- Backfill: inbox watermark starts equal to peer-visible read watermark.
UPDATE im_conversation_member
SET badge_seq = COALESCE(read_seq, 0)
WHERE badge_seq = 0 AND COALESCE(read_seq, 0) > 0;

COMMENT ON COLUMN im_conversation_member.badge_seq IS
    'Inbox badge watermark; unread_count = count(seq > badge_seq). Independent of read_seq.';
COMMENT ON COLUMN im_conversation_member.pinned_at IS
    'When pinned=1, pin time for sort; cleared on unpin.';
COMMENT ON COLUMN im_conversation_member.flagged IS
    '1 when member flagged this conversation (private).';
COMMENT ON COLUMN im_conversation_member.mute_until IS
    'Message mute until; NULL means not muted. Product mute toggle may set a far-future timestamp.';

CREATE TABLE IF NOT EXISTS im_user_tag (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_user_tag PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_user_tag_tenant_user_name
    ON im_user_tag (tenant_id, user_id, name)
    WHERE deleted = 0;

CREATE INDEX idx_im_user_tag_tenant_user
    ON im_user_tag (tenant_id, user_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_user_tag IS 'Per-user private conversation tag library';

CREATE TABLE IF NOT EXISTS im_conversation_user_tag (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    tag_id              BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation_user_tag PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_conversation_user_tag
    ON im_conversation_user_tag (tenant_id, user_id, conversation_id, tag_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_conversation_user_tag_user_tag
    ON im_conversation_user_tag (tenant_id, user_id, tag_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_conversation_user_tag_user_conv
    ON im_conversation_user_tag (tenant_id, user_id, conversation_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_conversation_user_tag IS 'Per-user assignment of tags to conversations';
