-- Per-user custom stickers (image/GIF favorites).

CREATE TABLE im_user_sticker (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    file_id             BIGINT          NOT NULL,
    name                VARCHAR(128),
    sort                INT             NOT NULL DEFAULT 0,
    source_message_id   BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_user_sticker PRIMARY KEY (id)
);

CREATE INDEX idx_im_user_sticker_owner
    ON im_user_sticker (tenant_id, user_id, sort, id)
    WHERE deleted = 0;

CREATE UNIQUE INDEX uk_im_user_sticker_owner_file
    ON im_user_sticker (tenant_id, user_id, file_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_user_sticker IS 'User custom IM stickers (image/GIF)';
