-- Docs share / star / personal recent visit + docs-bot seed.

CREATE TABLE doc_share (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    object_id           BIGINT          NOT NULL,
    grantee_user_id     BIGINT          NOT NULL,
    permission          VARCHAR(16)     NOT NULL,
    granted_by_user_id  BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_share PRIMARY KEY (id),
    CONSTRAINT ck_doc_share_permission CHECK (permission IN ('VIEW', 'EDIT'))
);

CREATE UNIQUE INDEX uk_doc_share_object_grantee
    ON doc_share (tenant_id, object_id, grantee_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_share_grantee
    ON doc_share (tenant_id, grantee_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_share_object
    ON doc_share (tenant_id, object_id)
    WHERE deleted = 0;

CREATE TABLE doc_star (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    object_id           BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_star PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_doc_star_user_object
    ON doc_star (tenant_id, user_id, object_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_star_user
    ON doc_star (tenant_id, user_id, create_time DESC)
    WHERE deleted = 0;

CREATE TABLE doc_user_visit (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    object_id           BIGINT          NOT NULL,
    last_opened_at      TIMESTAMPTZ     NOT NULL,
    title_snapshot      VARCHAR(200)    NOT NULL DEFAULT '',
    source              VARCHAR(16),
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_user_visit PRIMARY KEY (id),
    CONSTRAINT ck_doc_user_visit_source CHECK (source IS NULL OR source IN ('OWNER', 'SHARED'))
);

CREATE UNIQUE INDEX uk_doc_user_visit_user_object
    ON doc_user_visit (tenant_id, user_id, object_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_user_visit_user_opened
    ON doc_user_visit (tenant_id, user_id, last_opened_at DESC)
    WHERE deleted = 0;

-- Backfill owner visits from existing last_opened_at (id = object id for uniqueness in seed window).
INSERT INTO doc_user_visit (
    id, tenant_id, user_id, object_id, last_opened_at, title_snapshot, source,
    creator, create_time, updater, update_time, deleted
)
SELECT
    o.id,
    o.tenant_id,
    o.owner_user_id,
    o.id,
    o.last_opened_at,
    COALESCE(o.title, ''),
    'OWNER',
    o.owner_user_id,
    NOW(),
    o.owner_user_id,
    NOW(),
    0
FROM doc_object o
WHERE o.deleted = 0
  AND o.last_opened_at IS NOT NULL
  AND o.type = 'RICH_DOC';

INSERT INTO im_bot (id, code, name, description, scope, enable_policy, handler_kind, type, status, create_time, update_time)
VALUES
    (900007, 'docs-bot', '文档助手', '文档分享与协作提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW());

INSERT INTO im_bot_tenant_enablement (id, tenant_id, bot_id, enabled, create_time, update_time)
VALUES
    (900107, 1, 900007, 1, NOW(), NOW());

INSERT INTO im_bot_user_enablement (id, tenant_id, user_id, bot_id, create_time, update_time)
VALUES
    (900207, 1, 1, 900007, NOW(), NOW());
