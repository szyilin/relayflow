-- Personal custom groups for「我负责的」(plan B). Per-user private; not shared.

CREATE TABLE task_mine_group (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    is_default      SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_mine_group PRIMARY KEY (id),
    CONSTRAINT ck_task_mine_group_default CHECK (is_default IN (0, 1))
);

-- At most one default group per user
CREATE UNIQUE INDEX uk_task_mine_group_default
    ON task_mine_group (tenant_id, user_id)
    WHERE deleted = 0 AND is_default = 1;

CREATE INDEX idx_task_mine_group_user
    ON task_mine_group (tenant_id, user_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_mine_group IS 'Personal mine groups (我负责的自定义分组)';
COMMENT ON COLUMN task_mine_group.user_id IS 'Owner user id';
COMMENT ON COLUMN task_mine_group.is_default IS '1 = default group; exactly one per user';

CREATE TABLE task_mine_group_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    group_id        BIGINT          NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_mine_group_item PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_mine_group_item_task
    ON task_mine_group_item (tenant_id, user_id, task_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_mine_group_item_group
    ON task_mine_group_item (tenant_id, user_id, group_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_mine_group_item IS 'Task membership in personal mine groups';
COMMENT ON COLUMN task_mine_group_item.group_id IS 'task_mine_group.id';
COMMENT ON COLUMN task_mine_group_item.task_id IS 'task_item.id';
