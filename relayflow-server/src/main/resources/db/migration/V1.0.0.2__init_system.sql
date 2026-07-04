-- System domain: global user account, org structure, RBAC, menu.
-- Extends tenant metadata from V1.0.0.1.

-- 2.1 Member lifecycle status on tenant membership
ALTER TABLE sys_tenant_user
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

-- 2.2 Optional tenant owner marker
ALTER TABLE sys_tenant
    ADD COLUMN owner_user_id BIGINT;

-- 2.3 Global user account (no tenant_id, no lifecycle status)
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

-- 2.4 Department tree and user-department membership
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

-- 2.5 Roles with hierarchy and data scope
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

-- 2.6 Permission tree and role bindings
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

-- 2.7 Admin menu (UI navigation, optional permission link)
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

-- 2.8 Seed: minimal system permission tree + super_admin role
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
