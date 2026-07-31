-- Foundation bootstrap (squashed V0.1.0.1–V0.1.0.10): tenant, system, storage, IM, notify, task + seeds.
-- Convention: business tables include tenant_id; tenant-scoped uniques use UNIQUE (tenant_id, ...).

-- ---------------------------------------------------------------------------
-- sys_ : tenant / user / org / RBAC / menu
-- ---------------------------------------------------------------------------

CREATE TABLE sys_tenant (
    id              BIGINT          NOT NULL,
    code            VARCHAR(64)     NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    status          SMALLINT        NOT NULL DEFAULT 0,
    owner_user_id   BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_code ON sys_tenant (code) WHERE deleted = 0;

CREATE TABLE sys_tenant_user (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_user PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_user ON sys_tenant_user (tenant_id, user_id) WHERE deleted = 0;
CREATE INDEX idx_sys_tenant_user_user_id ON sys_tenant_user (user_id);

CREATE TABLE sys_user (
    id              BIGINT          NOT NULL,
    username        VARCHAR(64)     NOT NULL,
    password        VARCHAR(100)    NOT NULL,
    nickname        VARCHAR(64)     NOT NULL DEFAULT '',
    mobile          VARCHAR(20),
    email           VARCHAR(128),
    avatar          VARCHAR(512),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_user PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_user_mobile ON sys_user (mobile) WHERE deleted = 0 AND mobile IS NOT NULL;
CREATE UNIQUE INDEX uk_sys_user_email ON sys_user (email) WHERE deleted = 0 AND email IS NOT NULL;

CREATE TABLE sys_dept (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    name            VARCHAR(64)     NOT NULL,
    sort            INT             NOT NULL DEFAULT 0,
    leader_user_id  BIGINT,
    status          SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_dept PRIMARY KEY (id)
);

CREATE INDEX idx_sys_dept_tenant_parent ON sys_dept (tenant_id, parent_id);

CREATE TABLE sys_user_dept (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    dept_id         BIGINT          NOT NULL,
    primary_flag    SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_user_dept PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_user_dept ON sys_user_dept (tenant_id, user_id, dept_id) WHERE deleted = 0;
CREATE INDEX idx_sys_user_dept_user ON sys_user_dept (tenant_id, user_id);

CREATE TABLE sys_role (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    name            VARCHAR(64)     NOT NULL,
    code            VARCHAR(64),
    role_type       VARCHAR(16)     NOT NULL DEFAULT 'CUSTOM',
    data_scope      VARCHAR(32)     NOT NULL DEFAULT 'SELF',
    can_delegate    SMALLINT        NOT NULL DEFAULT 0,
    sort            INT             NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 0,
    remark          VARCHAR(512),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_role PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_role_code ON sys_role (tenant_id, code) WHERE deleted = 0 AND code IS NOT NULL;
CREATE INDEX idx_sys_role_tenant_parent ON sys_role (tenant_id, parent_id);

CREATE TABLE sys_user_role (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (tenant_id, user_id, role_id) WHERE deleted = 0;

CREATE TABLE sys_permission (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    name            VARCHAR(64)     NOT NULL,
    code            VARCHAR(128)    NOT NULL,
    type            SMALLINT        NOT NULL DEFAULT 1,
    sort            INT             NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_permission PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_permission_code ON sys_permission (tenant_id, code) WHERE deleted = 0;
CREATE INDEX idx_sys_permission_tenant_parent ON sys_permission (tenant_id, parent_id);

CREATE TABLE sys_role_permission (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    permission_id   BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_role_permission PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_role_permission ON sys_role_permission (tenant_id, role_id, permission_id) WHERE deleted = 0;

CREATE TABLE sys_role_dept (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    dept_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_role_dept PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_role_dept ON sys_role_dept (tenant_id, role_id, dept_id) WHERE deleted = 0;

CREATE TABLE sys_menu (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    parent_id       BIGINT          NOT NULL DEFAULT 0,
    name            VARCHAR(64)     NOT NULL,
    type            SMALLINT        NOT NULL DEFAULT 1,
    path            VARCHAR(256),
    component       VARCHAR(256),
    permission_id   BIGINT,
    icon            VARCHAR(64),
    sort            INT             NOT NULL DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 0,
    visible         SMALLINT        NOT NULL DEFAULT 1,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_menu PRIMARY KEY (id)
);

CREATE INDEX idx_sys_menu_tenant_parent ON sys_menu (tenant_id, parent_id);

-- ---------------------------------------------------------------------------
-- infra_ : storage / file / notify
-- ---------------------------------------------------------------------------

CREATE TABLE infra_storage_provider (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    provider        VARCHAR(32)     NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'active',
    is_default      SMALLINT        NOT NULL DEFAULT 0,
    config_json     TEXT            NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_storage_provider PRIMARY KEY (id)
);

CREATE INDEX idx_infra_storage_provider_tenant ON infra_storage_provider (tenant_id);
CREATE UNIQUE INDEX uk_infra_storage_provider_tenant_provider
    ON infra_storage_provider (tenant_id, provider) WHERE deleted = 0;

CREATE TABLE infra_file (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    provider        VARCHAR(32)     NOT NULL,
    storage_uri     VARCHAR(512)    NOT NULL,
    object_key      VARCHAR(512)    NOT NULL,
    original_name   VARCHAR(256)    NOT NULL,
    mime_type       VARCHAR(128)    NOT NULL DEFAULT '',
    size            BIGINT          NOT NULL DEFAULT 0,
    sha256          VARCHAR(64),
    access_level    VARCHAR(16)     NOT NULL DEFAULT 'private',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_file PRIMARY KEY (id)
);

CREATE INDEX idx_infra_file_tenant_provider ON infra_file (tenant_id, provider);
CREATE INDEX idx_infra_file_tenant_create ON infra_file (tenant_id, create_time);

CREATE TABLE infra_file_upload_session (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'pending',
    provider        VARCHAR(32)     NOT NULL,
    object_key      VARCHAR(512)    NOT NULL,
    original_name   VARCHAR(256)    NOT NULL,
    mime_type       VARCHAR(128)    NOT NULL DEFAULT '',
    size            BIGINT          NOT NULL DEFAULT 0,
    access_level    VARCHAR(16)     NOT NULL DEFAULT 'private',
    expires_at      TIMESTAMPTZ     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_file_upload_session PRIMARY KEY (id)
);

CREATE INDEX idx_infra_file_upload_session_tenant_status
    ON infra_file_upload_session (tenant_id, status);
CREATE INDEX idx_infra_file_upload_session_expires
    ON infra_file_upload_session (expires_at) WHERE deleted = 0 AND status = 'pending';

CREATE TABLE infra_file_binding (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    file_id         BIGINT          NOT NULL,
    biz_type        VARCHAR(64)     NOT NULL,
    biz_id          BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_file_binding PRIMARY KEY (id)
);

CREATE INDEX idx_infra_file_binding_file ON infra_file_binding (tenant_id, file_id);
CREATE UNIQUE INDEX uk_infra_file_binding_biz
    ON infra_file_binding (tenant_id, biz_type, biz_id, file_id) WHERE deleted = 0;

CREATE TABLE infra_notify (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT,
    mobile          VARCHAR(20),
    type            VARCHAR(32)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    body            VARCHAR(500),
    payload_json    JSONB,
    read_flag       SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_notify PRIMARY KEY (id)
);

CREATE INDEX idx_infra_notify_user
    ON infra_notify (tenant_id, user_id, read_flag)
    WHERE deleted = 0;

CREATE INDEX idx_infra_notify_mobile
    ON infra_notify (mobile, read_flag)
    WHERE deleted = 0 AND user_id IS NULL;

-- ---------------------------------------------------------------------------
-- im_ : conversation / member / message / group / channel
-- ---------------------------------------------------------------------------

CREATE TABLE im_conversation (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    type                VARCHAR(16)     NOT NULL,
    title               VARCHAR(128),
    avatar_file_id      BIGINT,
    last_msg_id         BIGINT,
    last_msg_at         TIMESTAMPTZ,
    last_msg_preview    VARCHAR(512),
    settings_json       TEXT,
    direct_peer_low     BIGINT,
    direct_peer_high    BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation PRIMARY KEY (id),
    CONSTRAINT ck_im_conversation_type CHECK (type IN ('direct', 'group', 'channel'))
);

CREATE INDEX idx_im_conversation_tenant_type ON im_conversation (tenant_id, type);
CREATE INDEX idx_im_conversation_tenant_last_msg ON im_conversation (tenant_id, last_msg_at DESC);
CREATE UNIQUE INDEX uk_im_conversation_direct_pair
    ON im_conversation (tenant_id, direct_peer_low, direct_peer_high)
    WHERE deleted = 0 AND type = 'direct' AND direct_peer_low IS NOT NULL AND direct_peer_high IS NOT NULL;

CREATE TABLE im_conversation_member (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    role                VARCHAR(16)     NOT NULL DEFAULT 'member',
    read_seq            BIGINT          NOT NULL DEFAULT 0,
    unread_count        INT             NOT NULL DEFAULT 0,
    join_time           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    mute_until          TIMESTAMPTZ,
    pinned              SMALLINT        NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation_member PRIMARY KEY (id),
    CONSTRAINT ck_im_conversation_member_role CHECK (role IN ('owner', 'admin', 'member', 'subscriber'))
);

CREATE INDEX idx_im_conversation_member_tenant_user ON im_conversation_member (tenant_id, user_id);
CREATE INDEX idx_im_conversation_member_tenant_conv ON im_conversation_member (tenant_id, conversation_id);
CREATE UNIQUE INDEX uk_im_conversation_member_user
    ON im_conversation_member (tenant_id, conversation_id, user_id)
    WHERE deleted = 0;

CREATE TABLE im_message (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    sender_id           BIGINT          NOT NULL,
    sender_type         VARCHAR(16)     NOT NULL DEFAULT 'user',
    type                VARCHAR(16)     NOT NULL DEFAULT 'text',
    content_json        TEXT            NOT NULL,
    client_msg_id       VARCHAR(64),
    seq                 BIGINT          NOT NULL,
    reply_to_msg_id     BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_message PRIMARY KEY (id),
    CONSTRAINT ck_im_message_sender_type CHECK (sender_type IN ('user', 'system', 'bot', 'app')),
    CONSTRAINT ck_im_message_type CHECK (type IN ('text', 'image', 'file', 'system'))
);

CREATE INDEX idx_im_message_tenant_conv_seq ON im_message (tenant_id, conversation_id, seq);
CREATE INDEX idx_im_message_tenant_conv_time ON im_message (tenant_id, conversation_id, create_time);
CREATE UNIQUE INDEX uk_im_message_client_msg
    ON im_message (tenant_id, client_msg_id)
    WHERE deleted = 0 AND client_msg_id IS NOT NULL;

CREATE TABLE im_group (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    owner_user_id       BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_group PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_group_conversation
    ON im_group (tenant_id, conversation_id)
    WHERE deleted = 0;

CREATE TABLE im_channel (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    post_permission     VARCHAR(32)     NOT NULL DEFAULT 'admin_only',
    owner_user_id       BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_channel PRIMARY KEY (id),
    CONSTRAINT ck_im_channel_post_permission CHECK (post_permission IN ('admin_only', 'all_subscribers'))
);

CREATE UNIQUE INDEX uk_im_channel_conversation
    ON im_channel (tenant_id, conversation_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- task_ : personal task items
-- ---------------------------------------------------------------------------

CREATE TABLE task_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    assignee_id     BIGINT          NOT NULL,
    creator_id      BIGINT          NOT NULL,
    due_time        TIMESTAMPTZ,
    status          VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item PRIMARY KEY (id),
    CONSTRAINT ck_task_item_status CHECK (status IN ('TODO', 'DONE'))
);

CREATE INDEX idx_task_item_assignee
    ON task_item (tenant_id, assignee_id, status)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- DML : still-required seeds (final-state)
-- ---------------------------------------------------------------------------

-- 默认租户（含 owner）
INSERT INTO sys_tenant (id, code, name, status, owner_user_id, create_time, update_time)
VALUES (1, 'default', '默认企业', 0, 1, NOW(), NOW());

-- 系统权限树 + 超管角色
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1000, 1, 0, '系统管理', 'system', 1, 0, 0, NOW(), NOW()),
    (1100, 1, 1000, '用户管理', 'system:user', 1, 1, 0, NOW(), NOW()),
    (1101, 1, 1100, '用户查询', 'system:user:query', 2, 1, 0, NOW(), NOW()),
    (1102, 1, 1100, '用户列表', 'system:user:list', 2, 2, 0, NOW(), NOW()),
    (1103, 1, 1100, '用户创建', 'system:user:create', 2, 3, 0, NOW(), NOW()),
    (1104, 1, 1100, '用户更新', 'system:user:update', 2, 4, 0, NOW(), NOW()),
    (1105, 1, 1100, '用户删除', 'system:user:delete', 2, 5, 0, NOW(), NOW()),
    (1200, 1, 1000, '部门管理', 'system:dept', 1, 2, 0, NOW(), NOW()),
    (1201, 1, 1200, '部门查询', 'system:dept:query', 2, 1, 0, NOW(), NOW()),
    (1202, 1, 1200, '部门列表', 'system:dept:list', 2, 2, 0, NOW(), NOW()),
    (1203, 1, 1200, '部门创建', 'system:dept:create', 2, 3, 0, NOW(), NOW()),
    (1204, 1, 1200, '部门更新', 'system:dept:update', 2, 4, 0, NOW(), NOW()),
    (1205, 1, 1200, '部门删除', 'system:dept:delete', 2, 5, 0, NOW(), NOW()),
    (1300, 1, 1000, '角色管理', 'system:role', 1, 3, 0, NOW(), NOW()),
    (1301, 1, 1300, '角色查询', 'system:role:query', 2, 1, 0, NOW(), NOW()),
    (1302, 1, 1300, '角色列表', 'system:role:list', 2, 2, 0, NOW(), NOW()),
    (1303, 1, 1300, '角色创建', 'system:role:create', 2, 3, 0, NOW(), NOW()),
    (1304, 1, 1300, '角色更新', 'system:role:update', 2, 4, 0, NOW(), NOW()),
    (1305, 1, 1300, '角色删除', 'system:role:delete', 2, 5, 0, NOW(), NOW()),
    (1400, 1, 1000, '菜单管理', 'system:menu', 1, 4, 0, NOW(), NOW()),
    (1401, 1, 1400, '菜单查询', 'system:menu:query', 2, 1, 0, NOW(), NOW()),
    (1402, 1, 1400, '菜单列表', 'system:menu:list', 2, 2, 0, NOW(), NOW()),
    (1403, 1, 1400, '菜单创建', 'system:menu:create', 2, 3, 0, NOW(), NOW()),
    (1404, 1, 1400, '菜单更新', 'system:menu:update', 2, 4, 0, NOW(), NOW()),
    (1405, 1, 1400, '菜单删除', 'system:menu:delete', 2, 5, 0, NOW(), NOW()),
    (1500, 1, 1000, '认证', 'system:auth', 1, 5, 0, NOW(), NOW()),
    (1501, 1, 1500, '登录', 'system:auth:login', 2, 1, 0, NOW(), NOW());

INSERT INTO sys_role (id, tenant_id, parent_id, name, code, role_type, data_scope, can_delegate, sort, status, create_time, update_time)
VALUES (100, 1, 0, '超级管理员', 'super_admin', 'SYSTEM', 'ALL', 1, 0, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
SELECT
    10000 + id,
    1,
    100,
    id,
    NOW(),
    NOW()
FROM sys_permission
WHERE tenant_id = 1 AND deleted = 0;

-- Dev bootstrap admin：手机号登录 19988888888 / 123456
INSERT INTO sys_user (id, username, password, nickname, mobile, create_time, update_time)
VALUES (
    1,
    '19988888888',
    '$2a$10$1.CaH9sDhy1/.pjBfztKSuaYXNgNrYz2Y9Y4Ds8dRkn1u/MuPI0lO',
    '管理员',
    '19988888888',
    NOW(),
    NOW()
);

INSERT INTO sys_tenant_user (id, tenant_id, user_id, status, create_time, update_time)
VALUES (1, 1, 1, 'ACTIVE', NOW(), NOW());

INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time, update_time)
VALUES (1, 1, 1, 100, NOW(), NOW());

-- 根部门（名与默认租户对齐）
INSERT INTO sys_dept (id, tenant_id, parent_id, name, sort, status, create_time, update_time)
SELECT 1, 1, 0, '默认企业', 0, 0, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dept WHERE tenant_id = 1 AND deleted = 0
);

-- 根部门名与租户名对齐；为无主部门成员回填主部门
UPDATE sys_dept d
SET name = t.name,
    update_time = NOW()
FROM sys_tenant t
WHERE d.tenant_id = t.id
  AND d.parent_id = 0
  AND d.deleted = 0
  AND t.deleted = 0
  AND d.id = (
      SELECT MIN(d2.id)
      FROM sys_dept d2
      WHERE d2.tenant_id = d.tenant_id
        AND d2.parent_id = 0
        AND d2.deleted = 0
  );

INSERT INTO sys_user_dept (id, tenant_id, user_id, dept_id, primary_flag, create_time, update_time)
SELECT
    7100000000000000000 + tu.id,
    tu.tenant_id,
    tu.user_id,
    root.id,
    1,
    NOW(),
    NOW()
FROM sys_tenant_user tu
INNER JOIN LATERAL (
    SELECT d.id
    FROM sys_dept d
    WHERE d.tenant_id = tu.tenant_id
      AND d.parent_id = 0
      AND d.deleted = 0
    ORDER BY d.id
    LIMIT 1
) root ON TRUE
WHERE tu.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_dept ud
      WHERE ud.tenant_id = tu.tenant_id
        AND ud.user_id = tu.user_id
        AND ud.deleted = 0
  );

-- infra 权限树 + 超管绑定
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (2000, 1, 0, '基础设施', 'infra', 1, 10, 0, NOW(), NOW()),
    (2100, 1, 2000, '存储设置', 'infra:storage', 1, 1, 0, NOW(), NOW()),
    (2101, 1, 2100, '存储查询', 'infra:storage:query', 2, 1, 0, NOW(), NOW()),
    (2102, 1, 2100, '存储更新', 'infra:storage:update', 2, 2, 0, NOW(), NOW()),
    (2103, 1, 2100, '存储测试连接', 'infra:storage:test', 2, 3, 0, NOW(), NOW()),
    (2200, 1, 2000, '文件管理', 'infra:file', 1, 2, 0, NOW(), NOW()),
    (2201, 1, 2200, '文件列表', 'infra:file:list', 2, 1, 0, NOW(), NOW()),
    (2202, 1, 2200, '文件上传', 'infra:file:upload', 2, 2, 0, NOW(), NOW()),
    (2203, 1, 2200, '文件下载', 'infra:file:download', 2, 3, 0, NOW(), NOW()),
    (2204, 1, 2200, '文件删除', 'infra:file:delete', 2, 4, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
SELECT
    20000 + id,
    1,
    100,
    id,
    NOW(),
    NOW()
FROM sys_permission
WHERE tenant_id = 1 AND id >= 2000 AND id < 3000 AND deleted = 0;
