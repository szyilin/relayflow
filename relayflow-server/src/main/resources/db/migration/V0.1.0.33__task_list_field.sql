-- List-scoped custom single-select fields (P8 / D12 EAV).

CREATE TABLE task_list_field (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    field_key       VARCHAR(64)     NOT NULL,
    field_type      VARCHAR(32)     NOT NULL DEFAULT 'SINGLE_SELECT',
    rank            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_field PRIMARY KEY (id),
    CONSTRAINT ck_task_list_field_type CHECK (field_type IN ('SINGLE_SELECT'))
);

CREATE UNIQUE INDEX uk_task_list_field_key
    ON task_list_field (tenant_id, list_id, field_key)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_field_list
    ON task_list_field (tenant_id, list_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_field IS 'List-scoped custom field definition';
COMMENT ON COLUMN task_list_field.field_key IS 'Stable key; API groupBy uses custom:{id}';
COMMENT ON COLUMN task_list_field.field_type IS 'V1: SINGLE_SELECT only';

CREATE TABLE task_list_field_option (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    field_id        BIGINT          NOT NULL,
    value_key       VARCHAR(64)     NOT NULL,
    label           VARCHAR(64)     NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    color           VARCHAR(32),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_field_option PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_list_field_option_key
    ON task_list_field_option (tenant_id, field_id, value_key)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_field_option_field
    ON task_list_field_option (tenant_id, field_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_field_option IS 'Options for list custom single-select fields';
COMMENT ON COLUMN task_list_field_option.value_key IS 'Stable bucket key for groupBy / group-move';

CREATE TABLE task_item_field_value (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    item_id         BIGINT          NOT NULL,
    field_id        BIGINT          NOT NULL,
    option_id       BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item_field_value PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_item_field_value
    ON task_item_field_value (tenant_id, item_id, field_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_field_value_field
    ON task_item_field_value (tenant_id, field_id)
    WHERE deleted = 0;

COMMENT ON TABLE task_item_field_value IS 'EAV values for task custom fields';
COMMENT ON COLUMN task_item_field_value.option_id IS 'NULL = empty / 无分组';
