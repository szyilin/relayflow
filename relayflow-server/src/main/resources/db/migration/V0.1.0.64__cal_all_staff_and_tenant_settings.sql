-- All-staff calendars, org settings thin set, product-settings permissions.

ALTER TABLE cal_calendar DROP CONSTRAINT IF EXISTS ck_cal_calendar_type;
ALTER TABLE cal_calendar
    ADD CONSTRAINT ck_cal_calendar_type CHECK (type IN ('PRIMARY', 'OWNED', 'ALL_STAFF'));

CREATE TABLE cal_all_staff_scope (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    calendar_id     BIGINT          NOT NULL,
    scope_type      VARCHAR(16)     NOT NULL,
    scope_id        BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_all_staff_scope PRIMARY KEY (id),
    CONSTRAINT ck_cal_all_staff_scope_type CHECK (scope_type IN ('ALL', 'USER', 'DEPT'))
);

CREATE INDEX idx_cal_all_staff_scope_calendar
    ON cal_all_staff_scope (tenant_id, calendar_id)
    WHERE deleted = 0;

COMMENT ON TABLE cal_all_staff_scope IS 'Visibility scope for ALL_STAFF calendars; ALL = whole tenant';
COMMENT ON COLUMN cal_all_staff_scope.scope_id IS 'USER/DEPT target id; null when scope_type=ALL';

CREATE TABLE cal_tenant_settings (
    tenant_id                           BIGINT          NOT NULL,
    allow_member_create_owned_calendar  SMALLINT        NOT NULL DEFAULT 1,
    creator                             BIGINT,
    create_time                         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                             BIGINT,
    update_time                         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_tenant_settings PRIMARY KEY (tenant_id),
    CONSTRAINT ck_cal_tenant_settings_allow_owned CHECK (allow_member_create_owned_calendar IN (0, 1))
);

COMMENT ON TABLE cal_tenant_settings IS 'Per-tenant calendar org product settings';

-- Permissions under 产品设置 parent 1700
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1710, 1, 1700, '日历产品设置', 'calendar:product-settings', 1, 2, 0, NOW(), NOW()),
    (1711, 1, 1710, '日历产品设置查询', 'calendar:product-settings:query', 2, 1, 0, NOW(), NOW()),
    (1712, 1, 1710, '日历产品设置更新', 'calendar:product-settings:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11710, 1, 100, 1710, NOW(), NOW()),
    (11711, 1, 100, 1711, NOW(), NOW()),
    (11712, 1, 100, 1712, NOW(), NOW());
