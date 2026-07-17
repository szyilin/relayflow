-- Task lists (containers) + membership; optional task_item.list_id affiliation.

CREATE TABLE task_list (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    owner_id        BIGINT          NOT NULL,
    archived        SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list PRIMARY KEY (id)
);

CREATE INDEX idx_task_list_tenant_owner
    ON task_list (tenant_id, owner_id)
    WHERE deleted = 0;

CREATE TABLE task_list_member (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    role            VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_member PRIMARY KEY (id),
    CONSTRAINT ck_task_list_member_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

CREATE UNIQUE INDEX uk_task_list_member_list_user
    ON task_list_member (tenant_id, list_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_member_user
    ON task_list_member (tenant_id, user_id)
    WHERE deleted = 0;

ALTER TABLE task_item
    ADD COLUMN list_id BIGINT;

CREATE INDEX idx_task_item_list
    ON task_item (tenant_id, list_id)
    WHERE deleted = 0 AND list_id IS NOT NULL;
