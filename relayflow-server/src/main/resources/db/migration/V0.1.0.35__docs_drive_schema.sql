-- Docs drive: personal folders + placements; FILE type on doc_object.

ALTER TABLE doc_object
    ADD COLUMN storage_file_id BIGINT;

ALTER TABLE doc_object
    DROP CONSTRAINT ck_doc_object_type;

ALTER TABLE doc_object
    ADD CONSTRAINT ck_doc_object_type CHECK (type IN ('RICH_DOC', 'FILE'));

ALTER TABLE doc_object
    ADD CONSTRAINT ck_doc_object_file_storage CHECK (
        (type = 'FILE' AND storage_file_id IS NOT NULL)
        OR (type = 'RICH_DOC' AND storage_file_id IS NULL)
    );

CREATE INDEX idx_doc_object_storage_file
    ON doc_object (tenant_id, storage_file_id)
    WHERE deleted = 0 AND storage_file_id IS NOT NULL;

CREATE TABLE doc_drive_folder (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    owner_user_id       BIGINT          NOT NULL,
    parent_id           BIGINT,
    name                VARCHAR(200)    NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_drive_folder PRIMARY KEY (id)
);

CREATE INDEX idx_doc_drive_folder_owner_parent
    ON doc_drive_folder (tenant_id, owner_user_id, parent_id, sort_order)
    WHERE deleted = 0;

CREATE TABLE doc_drive_item (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    owner_user_id       BIGINT          NOT NULL,
    folder_id           BIGINT,
    object_id           BIGINT          NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_drive_item PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_doc_drive_item_object
    ON doc_drive_item (object_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_drive_item_owner_folder
    ON doc_drive_item (tenant_id, owner_user_id, folder_id, sort_order)
    WHERE deleted = 0;
