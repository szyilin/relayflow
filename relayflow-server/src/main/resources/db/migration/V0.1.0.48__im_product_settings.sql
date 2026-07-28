-- IM tenant product settings (org-level) + group size rule sets + permission seeds.

CREATE TABLE im_tenant_settings (
    tenant_id                       BIGINT          NOT NULL,
    group_member_limit_default      INT             NOT NULL DEFAULT 50,
    allow_public_group              BOOLEAN         NOT NULL DEFAULT TRUE,
    dept_group_leave_policy         VARCHAR(16)     NOT NULL DEFAULT 'ALLOW',
    member_export_policy            VARCHAR(32)     NOT NULL DEFAULT 'DENY_ALL',
    member_export_designated_json   JSONB,
    recall_enabled                  BOOLEAN         NOT NULL DEFAULT TRUE,
    recall_within_hours             INT             NOT NULL DEFAULT 24,
    edit_enabled                    BOOLEAN         NOT NULL DEFAULT TRUE,
    edit_within_hours               INT             NOT NULL DEFAULT 24,
    creator                         BIGINT,
    create_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                         BIGINT,
    update_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_tenant_settings PRIMARY KEY (tenant_id),
    CONSTRAINT ck_im_tenant_settings_leave CHECK (dept_group_leave_policy IN ('ALLOW', 'DENY')),
    CONSTRAINT ck_im_tenant_settings_export CHECK (member_export_policy IN ('ALLOW_OWNER_ADMIN', 'DENY_ALL', 'ALLOW_DESIGNATED')),
    CONSTRAINT ck_im_tenant_settings_limit CHECK (group_member_limit_default >= 2)
);

COMMENT ON TABLE im_tenant_settings IS 'Tenant-level IM product settings (org policy)';

CREATE TABLE im_group_size_user_rule (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    subject_type        VARCHAR(16)     NOT NULL,
    subject_id          BIGINT          NOT NULL,
    member_limit        INT             NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_group_size_user_rule PRIMARY KEY (id),
    CONSTRAINT ck_im_group_size_user_rule_subject CHECK (subject_type IN ('DEPT', 'USER')),
    CONSTRAINT ck_im_group_size_user_rule_limit CHECK (member_limit >= 2)
);

CREATE INDEX idx_im_group_size_user_rule_tenant ON im_group_size_user_rule (tenant_id) WHERE deleted = 0;

CREATE TABLE im_group_size_group_rule (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    member_limit        INT             NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_group_size_group_rule PRIMARY KEY (id),
    CONSTRAINT ck_im_group_size_group_rule_limit CHECK (member_limit >= 2)
);

CREATE UNIQUE INDEX uk_im_group_size_group_rule_conv
    ON im_group_size_group_rule (tenant_id, conversation_id)
    WHERE deleted = 0;

-- Permissions under 系统管理 (parent 1000); product settings are admin-facing
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1700, 1, 1000, '产品设置', 'im:product-settings', 1, 7, 0, NOW(), NOW()),
    (1701, 1, 1700, '产品设置查询', 'im:product-settings:query', 2, 1, 0, NOW(), NOW()),
    (1702, 1, 1700, '产品设置更新', 'im:product-settings:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11700, 1, 100, 1700, NOW(), NOW()),
    (11701, 1, 100, 1701, NOW(), NOW()),
    (11702, 1, 100, 1702, NOW(), NOW());
