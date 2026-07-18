-- Per-context task view configuration (personal private / list shared default).

CREATE TABLE task_view_config (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    context_type    VARCHAR(32)     NOT NULL,
    context_id      BIGINT,
    owner_user_id   BIGINT,
    config_json     JSONB           NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_view_config PRIMARY KEY (id),
    CONSTRAINT ck_task_view_config_type CHECK (
        context_type IN (
            'MINE', 'FOLLOWING', 'ALL', 'CREATED', 'ASSIGNED_BY_ME', 'COMPLETED', 'LIST'
        )
    )
);

-- Personal contexts: one row per user per type
CREATE UNIQUE INDEX uk_task_view_config_personal
    ON task_view_config (tenant_id, context_type, owner_user_id)
    WHERE deleted = 0
      AND context_type <> 'LIST'
      AND owner_user_id IS NOT NULL
      AND context_id IS NULL;

-- List shared default: one row per list
CREATE UNIQUE INDEX uk_task_view_config_list
    ON task_view_config (tenant_id, context_type, context_id)
    WHERE deleted = 0
      AND context_type = 'LIST'
      AND context_id IS NOT NULL
      AND owner_user_id IS NULL;

COMMENT ON TABLE task_view_config IS 'Task view config: personal private or list shared default';
COMMENT ON COLUMN task_view_config.owner_user_id IS 'Owner for personal contexts; NULL for LIST shared default';
COMMENT ON COLUMN task_view_config.context_id IS 'listId when context_type=LIST';
