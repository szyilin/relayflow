-- Foundation bootstrap (squashed V0.1.0.10–V0.1.0.33):
-- tenant/system/storage/IM/bot/calendar/task + seeds.
-- Convention: business tables include tenant_id; tenant-scoped uniques use UNIQUE (tenant_id, ...).

-- ---------------------------------------------------------------------------
-- sys_ : tenant / user / org / RBAC / menu / preference / contact
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
    signature       VARCHAR(120)    NOT NULL DEFAULT '',
    cover_file_id   VARCHAR(512),
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

COMMENT ON COLUMN sys_user.signature IS 'Personal signature shown on business card';
COMMENT ON COLUMN sys_user.cover_file_id IS 'Cover image file id (same scheme as avatar)';

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

CREATE TABLE sys_contact_remark (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    target_user_id  BIGINT          NOT NULL,
    remark_name     VARCHAR(64)     NOT NULL DEFAULT '',
    description     VARCHAR(500)    NOT NULL DEFAULT '',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_contact_remark PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_contact_remark_owner_target
    ON sys_contact_remark (tenant_id, owner_user_id, target_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_sys_contact_remark_owner
    ON sys_contact_remark (tenant_id, owner_user_id);

-- ---------------------------------------------------------------------------
-- infra_ : storage / file（notify 已退役，不建表）
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

-- ---------------------------------------------------------------------------
-- im_ : conversation / member / message / group / channel / bot
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
    bot_peer_bot_id     BIGINT,
    bot_peer_user_id    BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation PRIMARY KEY (id),
    CONSTRAINT ck_im_conversation_type CHECK (type IN ('direct', 'group', 'channel', 'bot_dm'))
);

CREATE INDEX idx_im_conversation_tenant_type ON im_conversation (tenant_id, type);
CREATE INDEX idx_im_conversation_tenant_last_msg ON im_conversation (tenant_id, last_msg_at DESC);
CREATE UNIQUE INDEX uk_im_conversation_direct_pair
    ON im_conversation (tenant_id, direct_peer_low, direct_peer_high)
    WHERE deleted = 0 AND type = 'direct' AND direct_peer_low IS NOT NULL AND direct_peer_high IS NOT NULL;
CREATE UNIQUE INDEX uk_im_conversation_bot_dm
    ON im_conversation (tenant_id, bot_peer_bot_id, bot_peer_user_id)
    WHERE deleted = 0
      AND type = 'bot_dm'
      AND bot_peer_bot_id IS NOT NULL
      AND bot_peer_user_id IS NOT NULL;

CREATE TABLE im_conversation_member (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    subject_type        VARCHAR(16)     NOT NULL DEFAULT 'user',
    subject_id          BIGINT          NOT NULL,
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
    CONSTRAINT ck_im_conversation_member_role CHECK (role IN ('owner', 'admin', 'member', 'subscriber')),
    CONSTRAINT ck_im_conversation_member_subject_type CHECK (subject_type IN ('user', 'bot'))
);

CREATE INDEX idx_im_conversation_member_tenant_subject
    ON im_conversation_member (tenant_id, subject_type, subject_id);
CREATE INDEX idx_im_conversation_member_tenant_conv ON im_conversation_member (tenant_id, conversation_id);
CREATE UNIQUE INDEX uk_im_conversation_member_subject
    ON im_conversation_member (tenant_id, conversation_id, subject_type, subject_id)
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
    CONSTRAINT ck_im_message_type CHECK (type IN ('text', 'image', 'file', 'system', 'card'))
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

CREATE TABLE im_bot (
    id                  BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    avatar_file_id      BIGINT,
    scope               VARCHAR(32)     NOT NULL,
    enable_policy       VARCHAR(32)     NOT NULL,
    handler_kind        VARCHAR(32)     NOT NULL DEFAULT 'noop',
    type                VARCHAR(32)     NOT NULL DEFAULT 'tenant',
    capabilities_json   TEXT,
    status              SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_bot PRIMARY KEY (id),
    CONSTRAINT ck_im_bot_scope CHECK (scope IN ('tenant', 'identity_fanout', 'installable')),
    CONSTRAINT ck_im_bot_enable_policy CHECK (enable_policy IN ('mandatory', 'default_on', 'opt_in', 'installable')),
    CONSTRAINT ck_im_bot_handler_kind CHECK (handler_kind IN ('platform', 'noop', 'webhook')),
    CONSTRAINT ck_im_bot_type CHECK (type IN ('system', 'tenant'))
);

CREATE UNIQUE INDEX uk_im_bot_code
    ON im_bot (code)
    WHERE deleted = 0;

CREATE TABLE im_bot_tenant_enablement (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    bot_id              BIGINT          NOT NULL,
    enabled             SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_bot_tenant_enablement PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_bot_tenant_enablement
    ON im_bot_tenant_enablement (tenant_id, bot_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_bot_tenant_enablement_tenant
    ON im_bot_tenant_enablement (tenant_id)
    WHERE deleted = 0;

CREATE TABLE im_bot_user_enablement (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    bot_id              BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_bot_user_enablement PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_bot_user_enablement
    ON im_bot_user_enablement (tenant_id, user_id, bot_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_bot_user_enablement_user
    ON im_bot_user_enablement (tenant_id, user_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- cal_ : calendar / event / share / recurrence
-- ---------------------------------------------------------------------------

CREATE TABLE cal_calendar (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    color           VARCHAR(32)     NOT NULL,
    description     VARCHAR(400),
    type            VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_calendar PRIMARY KEY (id),
    CONSTRAINT ck_cal_calendar_type CHECK (type IN ('PRIMARY', 'OWNED'))
);

CREATE UNIQUE INDEX uk_cal_calendar_primary
    ON cal_calendar (tenant_id, owner_user_id)
    WHERE deleted = 0 AND type = 'PRIMARY';

CREATE INDEX idx_cal_calendar_owner
    ON cal_calendar (tenant_id, owner_user_id)
    WHERE deleted = 0;

CREATE TABLE cal_event (
    id                      BIGINT          NOT NULL,
    tenant_id               BIGINT          NOT NULL,
    calendar_id             BIGINT          NOT NULL,
    title                   VARCHAR(200)    NOT NULL,
    description             VARCHAR(2000),
    start_time              TIMESTAMPTZ     NOT NULL,
    end_time                TIMESTAMPTZ     NOT NULL,
    all_day                 SMALLINT        NOT NULL DEFAULT 0,
    organizer_id            BIGINT          NOT NULL,
    remind_before_minutes   INT,
    all_day_remind_time     VARCHAR(8),
    status                  VARCHAR(16)     NOT NULL,
    rrule                   VARCHAR(512),
    creator                 BIGINT,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                 BIGINT,
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_event PRIMARY KEY (id),
    CONSTRAINT ck_cal_event_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_cal_event_all_day CHECK (all_day IN (0, 1))
);

CREATE INDEX idx_cal_event_calendar_time
    ON cal_event (tenant_id, calendar_id, start_time, end_time)
    WHERE deleted = 0;

CREATE INDEX idx_cal_event_organizer
    ON cal_event (tenant_id, organizer_id)
    WHERE deleted = 0;

COMMENT ON COLUMN cal_event.rrule IS 'RFC5545 RRULE text; null = single instance';

CREATE TABLE cal_attendee (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    event_id        BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    role            VARCHAR(16)     NOT NULL,
    response        VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_attendee PRIMARY KEY (id),
    CONSTRAINT ck_cal_attendee_role CHECK (role IN ('ORGANIZER', 'ATTENDEE')),
    CONSTRAINT ck_cal_attendee_response CHECK (response IN ('NEEDS_ACTION', 'ACCEPTED', 'DECLINED'))
);

CREATE UNIQUE INDEX uk_cal_attendee_event_user
    ON cal_attendee (event_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_cal_attendee_user
    ON cal_attendee (tenant_id, user_id)
    WHERE deleted = 0;

CREATE TABLE cal_calendar_share (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    calendar_id     BIGINT          NOT NULL,
    grantee_user_id BIGINT          NOT NULL,
    permission      VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_calendar_share PRIMARY KEY (id),
    CONSTRAINT ck_cal_calendar_share_permission CHECK (permission IN ('READ'))
);

CREATE UNIQUE INDEX uk_cal_calendar_share_grantee
    ON cal_calendar_share (tenant_id, calendar_id, grantee_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_cal_calendar_share_grantee
    ON cal_calendar_share (tenant_id, grantee_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_cal_calendar_share_calendar
    ON cal_calendar_share (tenant_id, calendar_id)
    WHERE deleted = 0;

CREATE TABLE cal_event_exception (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    master_event_id     BIGINT          NOT NULL,
    original_start      TIMESTAMPTZ     NOT NULL,
    cancelled           SMALLINT        NOT NULL DEFAULT 0,
    override_title      VARCHAR(200),
    override_start      TIMESTAMPTZ,
    override_end        TIMESTAMPTZ,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_event_exception PRIMARY KEY (id),
    CONSTRAINT ck_cal_event_exception_cancelled CHECK (cancelled IN (0, 1))
);

CREATE UNIQUE INDEX uk_cal_event_exception_master_start
    ON cal_event_exception (master_event_id, original_start)
    WHERE deleted = 0;

CREATE INDEX idx_cal_event_exception_master
    ON cal_event_exception (tenant_id, master_event_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- task_ : items / collab / lists / board / views / groups / custom fields
-- ---------------------------------------------------------------------------

CREATE TABLE task_item (
    id                      BIGINT          NOT NULL,
    tenant_id               BIGINT          NOT NULL,
    title                   VARCHAR(200)    NOT NULL,
    assignee_id             BIGINT          NOT NULL,
    assigner_id             BIGINT,
    creator_id              BIGINT          NOT NULL,
    due_time                TIMESTAMPTZ,
    start_time              TIMESTAMPTZ,
    description             TEXT,
    remind_before_minutes   INT,
    parent_id               BIGINT,
    list_id                 BIGINT,
    board_rank              INT,
    status                  VARCHAR(16)     NOT NULL,
    creator                 BIGINT,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                 BIGINT,
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item PRIMARY KEY (id),
    CONSTRAINT ck_task_item_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX idx_task_item_assignee
    ON task_item (tenant_id, assignee_id, status)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_parent
    ON task_item (tenant_id, parent_id)
    WHERE deleted = 0 AND parent_id IS NOT NULL;

CREATE INDEX idx_task_item_list
    ON task_item (tenant_id, list_id)
    WHERE deleted = 0 AND list_id IS NOT NULL;

CREATE INDEX idx_task_item_list_status_rank
    ON task_item (tenant_id, list_id, status, board_rank)
    WHERE deleted = 0 AND list_id IS NOT NULL AND parent_id IS NULL;

CREATE INDEX idx_task_item_assigner
    ON task_item (tenant_id, assigner_id)
    WHERE deleted = 0 AND assigner_id IS NOT NULL;

COMMENT ON COLUMN task_item.start_time IS 'Optional start time';
COMMENT ON COLUMN task_item.description IS 'Task description (plain text)';
COMMENT ON COLUMN task_item.remind_before_minutes IS 'Remind N minutes before due_time; NULL = system window';
COMMENT ON COLUMN task_item.parent_id IS 'Parent task id for one-level subtasks; NULL = root';
COMMENT ON COLUMN task_item.assigner_id IS 'User who last assigned the task to someone else; null if never or self-assigned';

CREATE TABLE task_follower (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_follower PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_follower_task_user
    ON task_follower (tenant_id, task_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_follower_user
    ON task_follower (tenant_id, user_id)
    WHERE deleted = 0;

COMMENT ON TABLE task_follower IS 'Task followers (我关注的)';
COMMENT ON COLUMN task_follower.task_id IS 'task_item.id';
COMMENT ON COLUMN task_follower.user_id IS 'Follower user id';

CREATE TABLE task_comment (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    author_id       BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_comment PRIMARY KEY (id)
);

CREATE INDEX idx_task_comment_task
    ON task_comment (tenant_id, task_id, create_time)
    WHERE deleted = 0;

COMMENT ON TABLE task_comment IS 'Task plain-text comments';
COMMENT ON COLUMN task_comment.author_id IS 'Comment author user id';

CREATE TABLE task_activity (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    task_title      VARCHAR(200)    NOT NULL,
    actor_id        BIGINT          NOT NULL,
    type            VARCHAR(32)     NOT NULL,
    summary         VARCHAR(500)    NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_activity PRIMARY KEY (id)
);

CREATE INDEX idx_task_activity_task
    ON task_activity (tenant_id, task_id, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_task_activity_create
    ON task_activity (tenant_id, create_time DESC)
    WHERE deleted = 0;

COMMENT ON TABLE task_activity IS 'Task activity / dynamics feed';
COMMENT ON COLUMN task_activity.type IS 'created|field_changed|subtask_*|follower_*|commented|assigned';
COMMENT ON COLUMN task_activity.summary IS 'Human-readable activity summary';

CREATE TABLE task_list (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(500),
    owner_id        BIGINT          NOT NULL,
    archived        SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list PRIMARY KEY (id)
);

CREATE INDEX idx_task_list_tenant_owner
    ON task_list (tenant_id, owner_id)
    WHERE deleted = 0;

CREATE TABLE task_list_member (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    role            VARCHAR(16)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_member PRIMARY KEY (id),
    CONSTRAINT ck_task_list_member_role CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER'))
);

CREATE UNIQUE INDEX uk_task_list_member_list_user
    ON task_list_member (tenant_id, list_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_member_user
    ON task_list_member (tenant_id, user_id)
    WHERE deleted = 0;

CREATE TABLE task_view_config (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    context_type    VARCHAR(32)     NOT NULL,
    context_id      BIGINT,
    owner_user_id   BIGINT,
    config_json     JSONB           NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_view_config PRIMARY KEY (id),
    CONSTRAINT ck_task_view_config_type CHECK (
        context_type IN (
            'MINE', 'FOLLOWING', 'ALL', 'CREATED', 'ASSIGNED_BY_ME', 'COMPLETED', 'LIST'
        )
    )
);

CREATE UNIQUE INDEX uk_task_view_config_personal
    ON task_view_config (tenant_id, context_type, owner_user_id)
    WHERE deleted = 0
      AND context_type <> 'LIST'
      AND owner_user_id IS NOT NULL
      AND context_id IS NULL;

CREATE UNIQUE INDEX uk_task_view_config_list
    ON task_view_config (tenant_id, context_type, context_id)
    WHERE deleted = 0
      AND context_type = 'LIST'
      AND context_id IS NOT NULL
      AND owner_user_id IS NULL;

COMMENT ON TABLE task_view_config IS 'Task view config: personal private or list shared default';
COMMENT ON COLUMN task_view_config.owner_user_id IS 'Owner for personal contexts; NULL for LIST shared default';
COMMENT ON COLUMN task_view_config.context_id IS 'listId when context_type=LIST';

CREATE TABLE task_item_assignee (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item_assignee PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_item_assignee_task_user
    ON task_item_assignee (tenant_id, task_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_assignee_user
    ON task_item_assignee (tenant_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_assignee_task
    ON task_item_assignee (tenant_id, task_id)
    WHERE deleted = 0;

COMMENT ON TABLE task_item_assignee IS 'Task multi-assignees (负责人集合)';
COMMENT ON COLUMN task_item_assignee.task_id IS 'task_item.id';
COMMENT ON COLUMN task_item_assignee.user_id IS 'Assignee user id';

CREATE TABLE task_mine_group (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    is_default      SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_mine_group PRIMARY KEY (id),
    CONSTRAINT ck_task_mine_group_default CHECK (is_default IN (0, 1))
);

CREATE UNIQUE INDEX uk_task_mine_group_default
    ON task_mine_group (tenant_id, user_id)
    WHERE deleted = 0 AND is_default = 1;

CREATE INDEX idx_task_mine_group_user
    ON task_mine_group (tenant_id, user_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_mine_group IS 'Personal mine groups (我负责的自定义分组)';
COMMENT ON COLUMN task_mine_group.user_id IS 'Owner user id';
COMMENT ON COLUMN task_mine_group.is_default IS '1 = default group; exactly one per user';

CREATE TABLE task_mine_group_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    group_id        BIGINT          NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_mine_group_item PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_mine_group_item_task
    ON task_mine_group_item (tenant_id, user_id, task_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_mine_group_item_group
    ON task_mine_group_item (tenant_id, user_id, group_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_mine_group_item IS 'Task membership in personal mine groups';
COMMENT ON COLUMN task_mine_group_item.group_id IS 'task_mine_group.id';
COMMENT ON COLUMN task_mine_group_item.task_id IS 'task_item.id';

CREATE TABLE task_list_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    group_id        BIGINT,
    rank            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_item PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_list_item_list_task
    ON task_list_item (tenant_id, list_id, task_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_item_task
    ON task_list_item (tenant_id, task_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_item_list
    ON task_list_item (tenant_id, list_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_item IS 'Task membership in lists (多清单)';
COMMENT ON COLUMN task_list_item.group_id IS 'Optional list-local group (P7)';
COMMENT ON COLUMN task_list_item.task_id IS 'Root task_item.id';

CREATE TABLE task_list_group (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    is_default      SMALLINT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_group PRIMARY KEY (id),
    CONSTRAINT ck_task_list_group_default CHECK (is_default IN (0, 1))
);

CREATE UNIQUE INDEX uk_task_list_group_default
    ON task_list_group (tenant_id, list_id)
    WHERE deleted = 0 AND is_default = 1;

CREATE INDEX idx_task_list_group_list
    ON task_list_group (tenant_id, list_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_group IS 'List-local task groups (清单内分组)';
COMMENT ON COLUMN task_list_group.is_default IS '1 = default group; exactly one per list';

CREATE TABLE task_list_field (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    list_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    field_key       VARCHAR(64)     NOT NULL,
    field_type      VARCHAR(32)     NOT NULL DEFAULT 'SINGLE_SELECT',
    rank            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_field PRIMARY KEY (id),
    CONSTRAINT ck_task_list_field_type CHECK (field_type IN ('SINGLE_SELECT'))
);

CREATE UNIQUE INDEX uk_task_list_field_key
    ON task_list_field (tenant_id, list_id, field_key)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_field_list
    ON task_list_field (tenant_id, list_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_field IS 'List-scoped custom field definition';
COMMENT ON COLUMN task_list_field.field_key IS 'Stable key; API groupBy uses custom:{id}';
COMMENT ON COLUMN task_list_field.field_type IS 'V1: SINGLE_SELECT only';

CREATE TABLE task_list_field_option (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    field_id        BIGINT          NOT NULL,
    value_key       VARCHAR(64)     NOT NULL,
    label           VARCHAR(64)     NOT NULL,
    rank            INT             NOT NULL DEFAULT 0,
    color           VARCHAR(32),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_list_field_option PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_list_field_option_key
    ON task_list_field_option (tenant_id, field_id, value_key)
    WHERE deleted = 0;

CREATE INDEX idx_task_list_field_option_field
    ON task_list_field_option (tenant_id, field_id, rank)
    WHERE deleted = 0;

COMMENT ON TABLE task_list_field_option IS 'Options for list custom single-select fields';
COMMENT ON COLUMN task_list_field_option.value_key IS 'Stable bucket key for groupBy / group-move';

CREATE TABLE task_item_field_value (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    item_id         BIGINT          NOT NULL,
    field_id        BIGINT          NOT NULL,
    option_id       BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_item_field_value PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_item_field_value
    ON task_item_field_value (tenant_id, item_id, field_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_item_field_value_field
    ON task_item_field_value (tenant_id, field_id)
    WHERE deleted = 0;

COMMENT ON TABLE task_item_field_value IS 'EAV values for task custom fields';
COMMENT ON COLUMN task_item_field_value.option_id IS 'NULL = empty / 无分组';

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

-- 系统 Bot（不含已退役的 invite-helper）+ 默认租户/管理员 enablement
INSERT INTO im_bot (id, code, name, description, scope, enable_policy, handler_kind, type, status, create_time, update_time)
VALUES
    (900001, 'org-assistant', '组织助手', '组织与角色、成员邀请等相关提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW()),
    (900003, 'task-bot', '任务助手', '任务到期与指派提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW()),
    (900004, 'approval-bot', '审批助手', '审批待办提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW()),
    (900005, 'account-security', '账号安全', '账号安全类提醒', 'identity_fanout', 'mandatory', 'noop', 'system', 1, NOW(), NOW()),
    (900006, 'calendar-bot', '日历助手', '日程邀约、变更与提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW());

INSERT INTO im_bot_tenant_enablement (id, tenant_id, bot_id, enabled, create_time, update_time)
VALUES
    (900101, 1, 900001, 1, NOW(), NOW()),
    (900103, 1, 900003, 1, NOW(), NOW()),
    (900104, 1, 900004, 1, NOW(), NOW()),
    (900105, 1, 900005, 1, NOW(), NOW()),
    (900106, 1, 900006, 1, NOW(), NOW());

INSERT INTO im_bot_user_enablement (id, tenant_id, user_id, bot_id, create_time, update_time)
VALUES
    (900201, 1, 1, 900001, NOW(), NOW()),
    (900203, 1, 1, 900003, NOW(), NOW()),
    (900204, 1, 1, 900004, NOW(), NOW()),
    (900205, 1, 1, 900005, NOW(), NOW()),
    (900206, 1, 1, 900006, NOW(), NOW());
