-- Multi-assignee: junction table + backfill from task_item.assignee_id.

CREATE TABLE task_item_assignee (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item_assignee PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_item_assignee_task_user
    ON task_item_assignee (tenant_id, task_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_assignee_user
    ON task_item_assignee (tenant_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_assignee_task
    ON task_item_assignee (tenant_id, task_id)
    WHERE deleted = 0;

COMMENT ON TABLE task_item_assignee IS 'Task multi-assignees (负责人集合)';
COMMENT ON COLUMN task_item_assignee.task_id IS 'task_item.id';
COMMENT ON COLUMN task_item_assignee.user_id IS 'Assignee user id';

-- Backfill: one row per existing non-null assignee_id (1:1 → reuse task id as row id)
INSERT INTO task_item_assignee (
    id, tenant_id, task_id, user_id, creator, create_time, updater, update_time, deleted
)
SELECT
    t.id,
    t.tenant_id,
    t.id,
    t.assignee_id,
    t.creator,
    COALESCE(t.create_time, NOW()),
    t.updater,
    COALESCE(t.update_time, NOW()),
    0
FROM task_item t
WHERE t.deleted = 0
  AND t.assignee_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM task_item_assignee a
      WHERE a.tenant_id = t.tenant_id
        AND a.task_id = t.id
        AND a.user_id = t.assignee_id
        AND a.deleted = 0
  );
