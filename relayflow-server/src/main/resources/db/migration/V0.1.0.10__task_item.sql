-- Task domain: personal task items for workspace /app/tasks (V1 assignee = creator).

CREATE TABLE task_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    assignee_id     BIGINT          NOT NULL,
    creator_id      BIGINT          NOT NULL,
    due_time        TIMESTAMPTZ,
    status          VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item PRIMARY KEY (id),
    CONSTRAINT ck_task_item_status CHECK (status IN ('TODO', 'DONE'))
);

CREATE INDEX idx_task_item_assignee
    ON task_item (tenant_id, assignee_id, status)
    WHERE deleted = 0;
