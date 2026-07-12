-- In-app notification inbox (member invite, future approval/task types).

CREATE TABLE infra_notify (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT,
    mobile          VARCHAR(20),
    type            VARCHAR(32)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    body            VARCHAR(500),
    payload_json    JSONB,
    read_flag       SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_notify PRIMARY KEY (id)
);

CREATE INDEX idx_infra_notify_user
    ON infra_notify (tenant_id, user_id, read_flag)
    WHERE deleted = 0;

CREATE INDEX idx_infra_notify_mobile
    ON infra_notify (mobile, read_flag)
    WHERE deleted = 0 AND user_id IS NULL;
