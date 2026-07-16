-- IM Bot catalog, enablement, bot_dm conversation type, and bot member subjects.
-- Design: im-bot-notify-foundation D2 / D3.

-- ---------------------------------------------------------------------------
-- Platform bot catalog (no tenant_id)
-- ---------------------------------------------------------------------------
CREATE TABLE im_bot (
    id                  BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    avatar_file_id      BIGINT,
    scope               VARCHAR(32)     NOT NULL,
    enable_policy       VARCHAR(32)     NOT NULL,
    handler_kind        VARCHAR(32)     NOT NULL DEFAULT 'noop',
    capabilities_json   TEXT,
    status              SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_bot PRIMARY KEY (id),
    CONSTRAINT ck_im_bot_scope CHECK (scope IN ('tenant', 'identity_fanout', 'installable')),
    CONSTRAINT ck_im_bot_enable_policy CHECK (enable_policy IN ('mandatory', 'default_on', 'opt_in', 'installable')),
    CONSTRAINT ck_im_bot_handler_kind CHECK (handler_kind IN ('platform', 'noop', 'webhook'))
);

CREATE UNIQUE INDEX uk_im_bot_code
    ON im_bot (code)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- Tenant enablement
-- ---------------------------------------------------------------------------
CREATE TABLE im_bot_tenant_enablement (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    bot_id              BIGINT          NOT NULL,
    enabled             SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_bot_tenant_enablement PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_bot_tenant_enablement
    ON im_bot_tenant_enablement (tenant_id, bot_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_bot_tenant_enablement_tenant
    ON im_bot_tenant_enablement (tenant_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- User enablement
-- ---------------------------------------------------------------------------
CREATE TABLE im_bot_user_enablement (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    bot_id              BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_bot_user_enablement PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_bot_user_enablement
    ON im_bot_user_enablement (tenant_id, user_id, bot_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_bot_user_enablement_user
    ON im_bot_user_enablement (tenant_id, user_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- Conversation: support bot_dm (+ peer columns for uniqueness, mirror direct)
-- ---------------------------------------------------------------------------
ALTER TABLE im_conversation
    DROP CONSTRAINT ck_im_conversation_type;

ALTER TABLE im_conversation
    ADD CONSTRAINT ck_im_conversation_type
    CHECK (type IN ('direct', 'group', 'channel', 'bot_dm'));

ALTER TABLE im_conversation
    ADD COLUMN bot_peer_bot_id  BIGINT,
    ADD COLUMN bot_peer_user_id BIGINT;

CREATE UNIQUE INDEX uk_im_conversation_bot_dm
    ON im_conversation (tenant_id, bot_peer_bot_id, bot_peer_user_id)
    WHERE deleted = 0
      AND type = 'bot_dm'
      AND bot_peer_bot_id IS NOT NULL
      AND bot_peer_user_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Members: subject_type + rename user_id -> subject_id (user | bot)
-- ---------------------------------------------------------------------------
ALTER TABLE im_conversation_member
    ADD COLUMN subject_type VARCHAR(16) NOT NULL DEFAULT 'user';

ALTER TABLE im_conversation_member
    ADD CONSTRAINT ck_im_conversation_member_subject_type
    CHECK (subject_type IN ('user', 'bot'));

DROP INDEX IF EXISTS uk_im_conversation_member_user;
DROP INDEX IF EXISTS idx_im_conversation_member_tenant_user;

ALTER TABLE im_conversation_member
    RENAME COLUMN user_id TO subject_id;

CREATE UNIQUE INDEX uk_im_conversation_member_subject
    ON im_conversation_member (tenant_id, conversation_id, subject_type, subject_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_conversation_member_tenant_subject
    ON im_conversation_member (tenant_id, subject_type, subject_id);
