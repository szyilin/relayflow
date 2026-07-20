-- Docs domain: document objects + personal library page tree (no doc_embed in V1).

CREATE TABLE doc_object (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    type                VARCHAR(32)     NOT NULL,
    title               VARCHAR(200)    NOT NULL,
    body                JSONB           NOT NULL DEFAULT '{"type":"doc","content":[]}'::jsonb,
    body_format         VARCHAR(32)     NOT NULL DEFAULT 'tiptap_json_v1',
    content_version     INT             NOT NULL DEFAULT 0,
    owner_user_id       BIGINT          NOT NULL,
    last_opened_at      TIMESTAMPTZ,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_object PRIMARY KEY (id),
    CONSTRAINT ck_doc_object_type CHECK (type IN ('RICH_DOC')),
    CONSTRAINT ck_doc_object_content_version CHECK (content_version >= 0)
);

CREATE INDEX idx_doc_object_owner
    ON doc_object (tenant_id, owner_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_object_owner_opened
    ON doc_object (tenant_id, owner_user_id, last_opened_at DESC NULLS LAST)
    WHERE deleted = 0;

CREATE TABLE doc_library_node (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    owner_user_id       BIGINT          NOT NULL,
    parent_id           BIGINT,
    object_id           BIGINT          NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_library_node PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_doc_library_node_object
    ON doc_library_node (object_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_library_node_owner_parent
    ON doc_library_node (tenant_id, owner_user_id, parent_id, sort_order)
    WHERE deleted = 0;
