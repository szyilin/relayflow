-- Per-department docs FILE storage quota overrides.

CREATE TABLE doc_storage_quota_dept_rule (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    dept_id         BIGINT          NOT NULL,
    quota_bytes     BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_storage_quota_dept_rule PRIMARY KEY (id),
    CONSTRAINT uk_doc_storage_quota_dept_rule UNIQUE (tenant_id, dept_id)
);

COMMENT ON TABLE doc_storage_quota_dept_rule IS
    'Per-dept docs FILE storage quota; applies to members whose primary dept matches when no user rule; quota_bytes NULL = unlimited';

CREATE INDEX idx_doc_storage_quota_dept_rule_tenant
    ON doc_storage_quota_dept_rule (tenant_id);
