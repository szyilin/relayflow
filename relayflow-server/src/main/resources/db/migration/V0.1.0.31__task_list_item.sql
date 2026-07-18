-- Multi-list membership (D6). Backfill from task_item.list_id; junction is source of truth.

CREATE TABLE task_list_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    group_id        BIGINT,
    rank            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_item PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_list_item_list_task
    ON task_list_item (tenant_id, list_id, task_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_item_task
    ON task_list_item (tenant_id, task_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_item_list
    ON task_list_item (tenant_id, list_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_item IS 'Task membership in lists (多清单)';
COMMENT ON COLUMN task_list_item.group_id IS 'Optional list-local group (P7)';
COMMENT ON COLUMN task_list_item.task_id IS 'Root task_item.id';

-- Backfill: one membership per existing non-null list_id
INSERT INTO task_list_item (
    id, tenant_id, list_id, task_id, group_id, rank,
    creator, create_time, updater, update_time, deleted
)
SELECT
    t.id,
    t.tenant_id,
    t.list_id,
    t.id,
    NULL,
    0,
    t.creator,
    COALESCE(t.create_time, NOW()),
    t.updater,
    COALESCE(t.update_time, NOW()),
    0
FROM task_item t
WHERE t.deleted = 0
  AND t.list_id IS NOT NULL
  AND t.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM task_list_item i
    WHERE i.tenant_id = t.tenant_id
      AND i.list_id = t.list_id
      AND i.task_id = t.id
      AND i.deleted = 0
  );
