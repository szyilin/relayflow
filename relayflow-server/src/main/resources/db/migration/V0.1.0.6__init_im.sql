-- IM domain: conversations, members, messages, group/channel metadata.
-- Architecture: im-platform-foundation design §D5.

-- 6.1 Unified conversation shell (direct | group | channel)
CREATE TABLE im_conversation (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    type                VARCHAR(16)     NOT NULL,
    title               VARCHAR(128),
    avatar_file_id      BIGINT,
    last_msg_id         BIGINT,
    last_msg_at         TIMESTAMPTZ,
    last_msg_preview    VARCHAR(512),
    settings_json       TEXT,
    direct_peer_low     BIGINT,
    direct_peer_high    BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation PRIMARY KEY (id),
    CONSTRAINT ck_im_conversation_type CHECK (type IN ('direct', 'group', 'channel'))
);

CREATE INDEX idx_im_conversation_tenant_type ON im_conversation (tenant_id, type);
CREATE INDEX idx_im_conversation_tenant_last_msg ON im_conversation (tenant_id, last_msg_at DESC);
CREATE UNIQUE INDEX uk_im_conversation_direct_pair
    ON im_conversation (tenant_id, direct_peer_low, direct_peer_high)
    WHERE deleted = 0 AND type = 'direct' AND direct_peer_low IS NOT NULL AND direct_peer_high IS NOT NULL;

-- 6.2 Conversation membership and read watermark
CREATE TABLE im_conversation_member (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    role                VARCHAR(16)     NOT NULL DEFAULT 'member',
    read_seq            BIGINT          NOT NULL DEFAULT 0,
    unread_count        INT             NOT NULL DEFAULT 0,
    join_time           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    mute_until          TIMESTAMPTZ,
    pinned              SMALLINT        NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation_member PRIMARY KEY (id),
    CONSTRAINT ck_im_conversation_member_role CHECK (role IN ('owner', 'admin', 'member', 'subscriber'))
);

CREATE INDEX idx_im_conversation_member_tenant_user ON im_conversation_member (tenant_id, user_id);
CREATE INDEX idx_im_conversation_member_tenant_conv ON im_conversation_member (tenant_id, conversation_id);
CREATE UNIQUE INDEX uk_im_conversation_member_user
    ON im_conversation_member (tenant_id, conversation_id, user_id)
    WHERE deleted = 0;

-- 6.3 Messages (per-conversation seq, client idempotency)
CREATE TABLE im_message (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    sender_id           BIGINT          NOT NULL,
    sender_type         VARCHAR(16)     NOT NULL DEFAULT 'user',
    type                VARCHAR(16)     NOT NULL DEFAULT 'text',
    content_json        TEXT            NOT NULL,
    client_msg_id       VARCHAR(64),
    seq                 BIGINT          NOT NULL,
    reply_to_msg_id     BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_message PRIMARY KEY (id),
    CONSTRAINT ck_im_message_sender_type CHECK (sender_type IN ('user', 'system', 'bot', 'app')),
    CONSTRAINT ck_im_message_type CHECK (type IN ('text', 'image', 'file', 'system'))
);

CREATE INDEX idx_im_message_tenant_conv_seq ON im_message (tenant_id, conversation_id, seq);
CREATE INDEX idx_im_message_tenant_conv_time ON im_message (tenant_id, conversation_id, create_time);
CREATE UNIQUE INDEX uk_im_message_client_msg
    ON im_message (tenant_id, client_msg_id)
    WHERE deleted = 0 AND client_msg_id IS NOT NULL;

-- 6.4 Group metadata (1:1 with im_conversation where type=group)
CREATE TABLE im_group (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    owner_user_id       BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_group PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_group_conversation
    ON im_group (tenant_id, conversation_id)
    WHERE deleted = 0;

-- 6.5 Channel metadata (1:1 with im_conversation where type=channel)
CREATE TABLE im_channel (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    post_permission     VARCHAR(32)     NOT NULL DEFAULT 'admin_only',
    owner_user_id       BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_channel PRIMARY KEY (id),
    CONSTRAINT ck_im_channel_post_permission CHECK (post_permission IN ('admin_only', 'all_subscribers'))
);

CREATE UNIQUE INDEX uk_im_channel_conversation
    ON im_channel (tenant_id, conversation_id)
    WHERE deleted = 0;
