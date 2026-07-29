-- Message reactions, sticker packs, and sticker message type.

ALTER TABLE im_message DROP CONSTRAINT ck_im_message_type;
ALTER TABLE im_message
    ADD CONSTRAINT ck_im_message_type CHECK (type IN ('text', 'image', 'file', 'system', 'card', 'sticker'));

CREATE TABLE im_message_reaction (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    message_id          BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    emoji               VARCHAR(32)     NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_message_reaction PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_message_reaction_msg_user_emoji
    ON im_message_reaction (tenant_id, message_id, user_id, emoji)
    WHERE deleted = 0;

CREATE INDEX idx_im_message_reaction_msg
    ON im_message_reaction (tenant_id, message_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_message_reaction IS 'Per-user Unicode emoji reaction on an IM message';

-- Platform catalog (ignored by tenant line); tenant custom packs deferred.
CREATE TABLE im_sticker_pack (
    id                  BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    scope               VARCHAR(16)     NOT NULL DEFAULT 'system',
    sort                INT             NOT NULL DEFAULT 0,
    status              SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_sticker_pack PRIMARY KEY (id),
    CONSTRAINT ck_im_sticker_pack_scope CHECK (scope IN ('system', 'tenant'))
);

CREATE UNIQUE INDEX uk_im_sticker_pack_code
    ON im_sticker_pack (code)
    WHERE deleted = 0;

CREATE TABLE im_sticker (
    id                  BIGINT          NOT NULL,
    pack_id             BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    file_id             BIGINT,
    emoji               VARCHAR(32),
    sort                INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_sticker PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_sticker_pack_code_item
    ON im_sticker (pack_id, code)
    WHERE deleted = 0;

CREATE INDEX idx_im_sticker_pack
    ON im_sticker (pack_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_sticker_pack IS 'IM sticker pack catalog (system/tenant)';
COMMENT ON TABLE im_sticker IS 'Sticker item - file_id optional when emoji glyph is used (V1 seed)';

INSERT INTO im_sticker_pack (id, code, name, scope, sort, status, create_time, update_time)
VALUES (910001, 'system_default', '系统表情', 'system', 0, 1, NOW(), NOW());

INSERT INTO im_sticker (id, pack_id, code, name, file_id, emoji, sort, create_time, update_time)
VALUES
    (910101, 910001, 'ok', 'OK', NULL, '👌', 1, NOW(), NOW()),
    (910102, 910001, 'clap', '鼓掌', NULL, '👏', 2, NOW(), NOW()),
    (910103, 910001, 'fire', '火', NULL, '🔥', 3, NOW(), NOW()),
    (910104, 910001, 'think', '思考', NULL, '🤔', 4, NOW(), NOW()),
    (910105, 910001, 'party', '庆祝', NULL, '🥳', 5, NOW(), NOW()),
    (910106, 910001, 'wave', '挥手', NULL, '👋', 6, NOW(), NOW());
