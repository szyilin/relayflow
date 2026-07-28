-- Public/searchable groups, dept group kind, message recall/edit metadata.

ALTER TABLE im_group
    ADD COLUMN searchable BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN group_kind VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN dept_id BIGINT NULL;

ALTER TABLE im_group
    ADD CONSTRAINT ck_im_group_kind CHECK (group_kind IN ('NORMAL', 'DEPT')),
    ADD CONSTRAINT ck_im_group_dept CHECK (
        (group_kind = 'NORMAL' AND dept_id IS NULL)
        OR (group_kind = 'DEPT' AND dept_id IS NOT NULL)
    );

CREATE INDEX idx_im_group_searchable
    ON im_group (tenant_id)
    WHERE searchable = TRUE AND deleted = 0;

ALTER TABLE im_message
    ADD COLUMN recalled_at TIMESTAMPTZ NULL,
    ADD COLUMN edited_at TIMESTAMPTZ NULL;
