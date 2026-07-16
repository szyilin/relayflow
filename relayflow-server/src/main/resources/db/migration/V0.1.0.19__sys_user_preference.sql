-- Enterprise-scoped user workspace preferences (C-class: lazy upsert, no join seed).

CREATE TABLE sys_user_preference (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    settings        JSONB           NOT NULL DEFAULT '{}'::jsonb,
    schema_version  INT             NOT NULL DEFAULT 1,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_user_preference PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_user_preference_tenant_user
    ON sys_user_preference (tenant_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_sys_user_preference_tenant
    ON sys_user_preference (tenant_id);
