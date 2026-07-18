-- List-local groups (D7 / plan C). group_id on task_list_item points here.

CREATE TABLE task_list_group (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    is_default      SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_group PRIMARY KEY (id),
    CONSTRAINT ck_task_list_group_default CHECK (is_default IN (0, 1))
);

CREATE UNIQUE INDEX uk_task_list_group_default
    ON task_list_group (tenant_id, list_id)
    WHERE deleted = 0 AND is_default = 1;

CREATE INDEX idx_task_list_group_list
    ON task_list_group (tenant_id, list_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_group IS 'List-local task groups (清单内分组)';
COMMENT ON COLUMN task_list_group.is_default IS '1 = default group; exactly one per list';

-- Default group per existing list (reuse list id as row id for backfill)
INSERT INTO task_list_group (
    id, tenant_id, list_id, name, rank, is_default,
    creator, create_time, updater, update_time, deleted
)
SELECT
    l.id,
    l.tenant_id,
    l.id,
    '默认',
    0,
    1,
    l.creator,
    COALESCE(l.create_time, NOW()),
    l.updater,
    COALESCE(l.update_time, NOW()),
    0
FROM task_list l
WHERE l.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM task_list_group g
    WHERE g.tenant_id = l.tenant_id
      AND g.list_id = l.id
      AND g.is_default = 1
      AND g.deleted = 0
  );

-- Point existing memberships at default group
UPDATE task_list_item i
SET group_id = g.id
FROM task_list_group g
WHERE i.deleted = 0
  AND i.group_id IS NULL
  AND g.list_id = i.list_id
  AND g.is_default = 1
  AND g.deleted = 0
  AND g.tenant_id = i.tenant_id;
