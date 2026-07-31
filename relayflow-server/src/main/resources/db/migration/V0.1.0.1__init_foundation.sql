-- Foundation bootstrap (squashed through V0.1.0.71 → V0.1.0.1):
-- tenant/system/storage/IM/bot/calendar/task/docs/contact/invite/geo
-- + rtc/meeting/agent + seeds.
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
    industry        VARCHAR(64),
    scale           VARCHAR(64),
    contact_name    VARCHAR(64),
    contact_email   VARCHAR(128),
    contact_mobile  VARCHAR(32),
    contact_landline VARCHAR(32),
    logo_file_id    BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_code ON sys_tenant (code) WHERE deleted = 0;

COMMENT ON COLUMN sys_tenant.industry IS 'Enterprise industry code';
COMMENT ON COLUMN sys_tenant.scale IS 'Enterprise scale code';
COMMENT ON COLUMN sys_tenant.contact_name IS 'Enterprise contact name';
COMMENT ON COLUMN sys_tenant.contact_email IS 'Enterprise contact email';
COMMENT ON COLUMN sys_tenant.contact_mobile IS 'Enterprise contact mobile';
COMMENT ON COLUMN sys_tenant.contact_landline IS 'Enterprise contact landline';
COMMENT ON COLUMN sys_tenant.logo_file_id IS 'Enterprise logo infra_file.id (nullable)';

CREATE TABLE sys_tenant_user (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    nickname        VARCHAR(64),
    avatar          VARCHAR(512),
    signature       VARCHAR(120)    NOT NULL DEFAULT '',
    cover_file_id   VARCHAR(512),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_user PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_user ON sys_tenant_user (tenant_id, user_id) WHERE deleted = 0;
CREATE INDEX idx_sys_tenant_user_user_id ON sys_tenant_user (user_id);

COMMENT ON COLUMN sys_tenant_user.nickname IS 'Display nickname within this tenant; null falls back to sys_user';
COMMENT ON COLUMN sys_tenant_user.avatar IS 'Avatar fileId within this tenant';
COMMENT ON COLUMN sys_tenant_user.signature IS 'Personal signature on business card within this tenant';
COMMENT ON COLUMN sys_tenant_user.cover_file_id IS 'Cover image fileId within this tenant';

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


CREATE TABLE sys_contact_external (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    target_user_id  BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_contact_external PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_contact_external_owner_target
    ON sys_contact_external (tenant_id, owner_user_id, target_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_sys_contact_external_owner
    ON sys_contact_external (tenant_id, owner_user_id);

COMMENT ON TABLE sys_contact_external IS 'Viewer-scoped external contacts in tenant address book';

CREATE TABLE sys_contact_request (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    from_user_id    BIGINT          NOT NULL,
    to_user_id      BIGINT          NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'pending',
    subtitle        VARCHAR(200)    NOT NULL DEFAULT '',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_contact_request PRIMARY KEY (id)
);

CREATE INDEX idx_sys_contact_request_to
    ON sys_contact_request (tenant_id, to_user_id)
    WHERE deleted = 0;

COMMENT ON TABLE sys_contact_request IS 'Add-contact requests addressed to a tenant member';
COMMENT ON COLUMN sys_contact_request.status IS 'pending | accepted | rejected';

CREATE TABLE sys_contact_star (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    target_user_id  BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_contact_star PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_contact_star_owner_target
    ON sys_contact_star (tenant_id, owner_user_id, target_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_sys_contact_star_owner
    ON sys_contact_star (tenant_id, owner_user_id);

COMMENT ON TABLE sys_contact_star IS 'Viewer-scoped starred contacts';

CREATE TABLE sys_tenant_invite_settings (
    id                          BIGINT          NOT NULL,
    tenant_id                   BIGINT          NOT NULL,
    invite_mode                 VARCHAR(32)     NOT NULL DEFAULT 'ADMIN_ONLY',
    non_admin_scope             VARCHAR(32)     NOT NULL DEFAULT 'INVITER_DEPT',
    non_admin_dept_ids          JSONB           NOT NULL DEFAULT '[]'::jsonb,
    non_admin_expire_days       INT             NOT NULL DEFAULT 3,
    non_admin_require_approval  BOOLEAN         NOT NULL DEFAULT FALSE,
    creator                     BIGINT,
    create_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                     BIGINT,
    update_time                 TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                     SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_invite_settings PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_tenant_invite_settings_tenant
    ON sys_tenant_invite_settings (tenant_id)
    WHERE deleted = 0;

COMMENT ON TABLE sys_tenant_invite_settings IS 'Per-tenant org invite mode and non-admin constraints';
COMMENT ON COLUMN sys_tenant_invite_settings.invite_mode IS 'OFF | ADMIN_ONLY | ALL_MEMBERS';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_scope IS 'INVITER_DEPT | DESIGNATED_DEPTS';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_dept_ids IS 'JSON array of dept id strings';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_expire_days IS 'Invite TTL days for non-admin inviters';
COMMENT ON COLUMN sys_tenant_invite_settings.non_admin_require_approval IS 'Whether non-admin invites need approval';

CREATE TABLE sys_org_invite (
    id                      BIGINT          NOT NULL,
    tenant_id               BIGINT          NOT NULL,
    channel                 VARCHAR(32)     NOT NULL,
    token                   VARCHAR(64),
    invite_code             VARCHAR(16),
    inviter_user_id         BIGINT          NOT NULL,
    target_mobile           VARCHAR(20),
    target_user_id          BIGINT,
    target_dept_id          BIGINT,
    target_role_ids         JSONB           NOT NULL DEFAULT '[]'::jsonb,
    status                  VARCHAR(32)     NOT NULL,
    expires_at              TIMESTAMPTZ,
    approved_by             BIGINT,
    approved_at             TIMESTAMPTZ,
    consumed_at             TIMESTAMPTZ,
    membership_user_id      BIGINT,
    creator                 BIGINT,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                 BIGINT,
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_org_invite PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_sys_org_invite_token
    ON sys_org_invite (token)
    WHERE deleted = 0 AND token IS NOT NULL;

CREATE UNIQUE INDEX uk_sys_org_invite_code
    ON sys_org_invite (invite_code)
    WHERE deleted = 0 AND invite_code IS NOT NULL AND status = 'ACTIVE';

CREATE INDEX idx_sys_org_invite_tenant_status
    ON sys_org_invite (tenant_id, status)
    WHERE deleted = 0;

CREATE INDEX idx_sys_org_invite_tenant_mobile_status
    ON sys_org_invite (tenant_id, target_mobile, status)
    WHERE deleted = 0 AND target_mobile IS NOT NULL;

COMMENT ON TABLE sys_org_invite IS 'Org invite process entity; membership written on consume';
COMMENT ON COLUMN sys_org_invite.channel IS 'SHARED | MOBILE (LINK/QR/CODE share SHARED row)';
COMMENT ON COLUMN sys_org_invite.status IS 'ACTIVE | PENDING_APPROVAL | CONSUMED | REVOKED | EXPIRED | REJECTED';

CREATE TABLE sys_tenant_address (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    types           JSONB           NOT NULL,
    region_id       BIGINT          NOT NULL,
    region_path     VARCHAR(512)    NOT NULL,
    detail          VARCHAR(512)    NOT NULL DEFAULT '',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ENABLED',
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_address PRIMARY KEY (id)
);

CREATE INDEX idx_sys_tenant_address_tenant ON sys_tenant_address (tenant_id) WHERE deleted = 0;
CREATE INDEX idx_sys_tenant_address_status ON sys_tenant_address (tenant_id, status) WHERE deleted = 0;

COMMENT ON TABLE sys_tenant_address IS 'Per-tenant enterprise addresses (job/interview/store types)';
COMMENT ON COLUMN sys_tenant_address.types IS 'JSON array of JOB | INTERVIEW | STORE';
COMMENT ON COLUMN sys_tenant_address.region_id IS 'infra_geo_region.id';
COMMENT ON COLUMN sys_tenant_address.status IS 'ENABLED | DISABLED';

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


CREATE TABLE infra_geo_region (
    id              BIGINT          NOT NULL,
    code            VARCHAR(32)     NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    level           VARCHAR(16)     NOT NULL,
    parent_id       BIGINT,
    sort            INT             NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_geo_region PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_infra_geo_region_code ON infra_geo_region (code) WHERE deleted = 0;
CREATE INDEX idx_infra_geo_region_parent ON infra_geo_region (parent_id) WHERE deleted = 0;
CREATE INDEX idx_infra_geo_region_level ON infra_geo_region (level) WHERE deleted = 0;

COMMENT ON TABLE infra_geo_region IS 'Platform China admin-division tree; shared across tenants';
COMMENT ON COLUMN infra_geo_region.level IS 'COUNTRY | PROVINCE | CITY | DISTRICT';
COMMENT ON COLUMN infra_geo_region.code IS 'Stable division code (e.g. GB/T 2260 style or CN)';

CREATE TABLE infra_geo_sync_meta (
    id              BIGINT          NOT NULL,
    source          VARCHAR(128)    NOT NULL,
    data_version    VARCHAR(64)     NOT NULL,
    status          VARCHAR(16)     NOT NULL,
    region_count    INT             NOT NULL DEFAULT 0,
    message         VARCHAR(512),
    synced_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_infra_geo_sync_meta PRIMARY KEY (id)
);

CREATE INDEX idx_infra_geo_sync_meta_synced ON infra_geo_sync_meta (synced_at DESC);

COMMENT ON TABLE infra_geo_sync_meta IS 'China geo sync run log (source + version)';

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
    badge_seq           BIGINT          NOT NULL DEFAULT 0,
    unread_count        INT             NOT NULL DEFAULT 0,
    mention_unread      SMALLINT        NOT NULL DEFAULT 0,
    join_time           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    mute_until          TIMESTAMPTZ,
    pinned              SMALLINT        NOT NULL DEFAULT 0,
    pinned_at           TIMESTAMPTZ,
    flagged             SMALLINT        NOT NULL DEFAULT 0,
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

COMMENT ON COLUMN im_conversation_member.mention_unread IS
    '1 when this member has unread user @mention(s); cleared on mark read';
COMMENT ON COLUMN im_conversation_member.badge_seq IS
    'Inbox badge watermark; unread_count = count(seq > badge_seq). Independent of read_seq.';
COMMENT ON COLUMN im_conversation_member.pinned_at IS
    'When pinned=1, pin time for sort; cleared on unpin.';
COMMENT ON COLUMN im_conversation_member.flagged IS
    '1 when member flagged this conversation (private).';
COMMENT ON COLUMN im_conversation_member.mute_until IS
    'Message mute until; NULL means not muted. Product mute toggle may set a far-future timestamp.';

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
    recalled_at         TIMESTAMPTZ,
    edited_at           TIMESTAMPTZ,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_message PRIMARY KEY (id),
    CONSTRAINT ck_im_message_sender_type CHECK (sender_type IN ('user', 'system', 'bot', 'app')),
    CONSTRAINT ck_im_message_type CHECK (type IN ('text', 'image', 'file', 'system', 'card', 'sticker'))
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
    searchable          BOOLEAN         NOT NULL DEFAULT FALSE,
    group_kind          VARCHAR(16)     NOT NULL DEFAULT 'NORMAL',
    dept_id             BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_group PRIMARY KEY (id),
    CONSTRAINT ck_im_group_kind CHECK (group_kind IN ('NORMAL', 'DEPT')),
    CONSTRAINT ck_im_group_dept CHECK (
        (group_kind = 'NORMAL' AND dept_id IS NULL)
        OR (group_kind = 'DEPT' AND dept_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_im_group_conversation
    ON im_group (tenant_id, conversation_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_group_searchable
    ON im_group (tenant_id)
    WHERE searchable = TRUE AND deleted = 0;

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
    owner_agent_id      BIGINT,
    owner_user_id       BIGINT,
    group_reply_mention_mode VARCHAR(16) NOT NULL DEFAULT 'trigger',
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
    CONSTRAINT ck_im_bot_type CHECK (type IN ('system', 'tenant')),
    CONSTRAINT ck_im_bot_group_reply_mention_mode CHECK (group_reply_mention_mode IN ('trigger', 'none'))
);

CREATE UNIQUE INDEX uk_im_bot_code
    ON im_bot (code)
    WHERE deleted = 0;

CREATE UNIQUE INDEX uk_im_bot_owner_agent
    ON im_bot (owner_agent_id)
    WHERE deleted = 0 AND owner_agent_id IS NOT NULL;

CREATE INDEX idx_im_bot_owner_user
    ON im_bot (owner_user_id)
    WHERE owner_user_id IS NOT NULL AND deleted = 0;

COMMENT ON COLUMN im_bot.owner_agent_id IS 'Bound AgentDef.id for type=tenant carrier bots; null for system bots';
COMMENT ON COLUMN im_bot.owner_user_id IS 'Owner user for agent carrier bots; null for system bots';
COMMENT ON COLUMN im_bot.group_reply_mention_mode IS
    'Group Bot Runtime reply mention: trigger=@trigger user; none=no auto mention (broadcast helpers)';

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


CREATE TABLE im_message_reaction (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    message_id          BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    emoji               VARCHAR(32)     NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_message_reaction PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_message_reaction_msg_user_emoji
    ON im_message_reaction (tenant_id, message_id, user_id, emoji)
    WHERE deleted = 0;

CREATE INDEX idx_im_message_reaction_msg
    ON im_message_reaction (tenant_id, message_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_message_reaction IS 'Per-user Unicode emoji reaction on an IM message';

CREATE TABLE im_sticker_pack (
    id                  BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    scope               VARCHAR(16)     NOT NULL DEFAULT 'system',
    sort                INT             NOT NULL DEFAULT 0,
    status              SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_sticker_pack PRIMARY KEY (id),
    CONSTRAINT ck_im_sticker_pack_scope CHECK (scope IN ('system', 'tenant'))
);

CREATE UNIQUE INDEX uk_im_sticker_pack_code
    ON im_sticker_pack (code)
    WHERE deleted = 0;

CREATE TABLE im_sticker (
    id                  BIGINT          NOT NULL,
    pack_id             BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    file_id             BIGINT,
    emoji               VARCHAR(32),
    sort                INT             NOT NULL DEFAULT 0,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_sticker PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_sticker_pack_code_item
    ON im_sticker (pack_id, code)
    WHERE deleted = 0;

CREATE INDEX idx_im_sticker_pack
    ON im_sticker (pack_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_sticker_pack IS 'IM sticker pack catalog (system/tenant)';
COMMENT ON TABLE im_sticker IS 'Sticker item - file_id optional when emoji glyph is used (V1 seed)';

CREATE TABLE im_user_sticker (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    file_id             BIGINT          NOT NULL,
    name                VARCHAR(128),
    sort                INT             NOT NULL DEFAULT 0,
    source_message_id   BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_user_sticker PRIMARY KEY (id)
);

CREATE INDEX idx_im_user_sticker_owner
    ON im_user_sticker (tenant_id, user_id, sort, id)
    WHERE deleted = 0;

CREATE UNIQUE INDEX uk_im_user_sticker_owner_file
    ON im_user_sticker (tenant_id, user_id, file_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_user_sticker IS 'User custom IM stickers (image/GIF)';

CREATE TABLE im_user_tag (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(64)     NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_user_tag PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_user_tag_tenant_user_name
    ON im_user_tag (tenant_id, user_id, name)
    WHERE deleted = 0;

CREATE INDEX idx_im_user_tag_tenant_user
    ON im_user_tag (tenant_id, user_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_user_tag IS 'Per-user private conversation tag library';

CREATE TABLE im_conversation_user_tag (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    conversation_id     BIGINT          NOT NULL,
    tag_id              BIGINT          NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_im_conversation_user_tag PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_im_conversation_user_tag
    ON im_conversation_user_tag (tenant_id, user_id, conversation_id, tag_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_conversation_user_tag_user_tag
    ON im_conversation_user_tag (tenant_id, user_id, tag_id)
    WHERE deleted = 0;

CREATE INDEX idx_im_conversation_user_tag_user_conv
    ON im_conversation_user_tag (tenant_id, user_id, conversation_id)
    WHERE deleted = 0;

COMMENT ON TABLE im_conversation_user_tag IS 'Per-user assignment of tags to conversations';

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
    CONSTRAINT ck_cal_calendar_type CHECK (type IN ('PRIMARY', 'OWNED', 'ALL_STAFF'))
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
    meeting_id              BIGINT,
    video_meeting_kind      VARCHAR(16)     NOT NULL DEFAULT 'NONE',
    external_meeting_url    VARCHAR(1000),
    creator                 BIGINT,
    create_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                 BIGINT,
    update_time             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_event PRIMARY KEY (id),
    CONSTRAINT ck_cal_event_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_cal_event_all_day CHECK (all_day IN (0, 1)),
    CONSTRAINT ck_cal_event_video_meeting_kind CHECK (video_meeting_kind IN ('NONE', 'RELAYFLOW', 'EXTERNAL'))
);

CREATE INDEX idx_cal_event_calendar_time
    ON cal_event (tenant_id, calendar_id, start_time, end_time)
    WHERE deleted = 0;

CREATE INDEX idx_cal_event_organizer
    ON cal_event (tenant_id, organizer_id)
    WHERE deleted = 0;

CREATE INDEX idx_cal_event_meeting
    ON cal_event (tenant_id, meeting_id)
    WHERE deleted = 0 AND meeting_id IS NOT NULL;

COMMENT ON COLUMN cal_event.rrule IS 'RFC5545 RRULE text; null = single instance';
COMMENT ON COLUMN cal_event.video_meeting_kind IS 'NONE | RELAYFLOW | EXTERNAL';
COMMENT ON COLUMN cal_event.external_meeting_url IS 'External meeting URL when kind=EXTERNAL';

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


CREATE TABLE cal_calendar_user_pref (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    calendar_id     BIGINT          NOT NULL,
    display_color   VARCHAR(32),
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_cal_calendar_user_pref PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_cal_calendar_user_pref
    ON cal_calendar_user_pref (tenant_id, user_id, calendar_id)
    WHERE deleted = 0;

CREATE INDEX idx_cal_calendar_user_pref_user
    ON cal_calendar_user_pref (tenant_id, user_id)
    WHERE deleted = 0;

COMMENT ON TABLE cal_calendar_user_pref IS 'Per-user calendar UI prefs; display_color null = use calendar default color';
COMMENT ON COLUMN cal_calendar_user_pref.display_color IS 'Local display color override; null clears override';

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
    CONSTRAINT ck_task_item_status CHECK (status IN ('TODO', 'DONE'))
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
-- doc_ : objects / library / drive / share / star / visit
-- ---------------------------------------------------------------------------

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
    storage_file_id     BIGINT,
    size_bytes          BIGINT,
    lifecycle           VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    deleted_at          TIMESTAMPTZ,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_object PRIMARY KEY (id),
    CONSTRAINT ck_doc_object_type CHECK (type IN ('RICH_DOC', 'FILE')),
    CONSTRAINT ck_doc_object_content_version CHECK (content_version >= 0),
    CONSTRAINT ck_doc_object_file_storage CHECK (
        (type = 'FILE' AND storage_file_id IS NOT NULL)
        OR (type = 'RICH_DOC' AND storage_file_id IS NULL)
    ),
    CONSTRAINT ck_doc_object_lifecycle CHECK (lifecycle IN ('ACTIVE', 'DELETED', 'PURGED'))
);

CREATE INDEX idx_doc_object_owner
    ON doc_object (tenant_id, owner_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_doc_object_owner_opened
    ON doc_object (tenant_id, owner_user_id, last_opened_at DESC NULLS LAST)
    WHERE deleted = 0;

CREATE INDEX idx_doc_object_storage_file
    ON doc_object (tenant_id, storage_file_id)
    WHERE deleted = 0 AND storage_file_id IS NOT NULL;

CREATE INDEX idx_doc_object_lifecycle
    ON doc_object (tenant_id, lifecycle, update_time DESC);

CREATE INDEX idx_doc_object_owner_lifecycle
    ON doc_object (tenant_id, owner_user_id, lifecycle);

COMMENT ON COLUMN doc_object.lifecycle IS 'ACTIVE | DELETED | PURGED';
COMMENT ON COLUMN doc_object.deleted_at IS 'When lifecycle became DELETED; used for 90-day restore window';
COMMENT ON COLUMN doc_object.size_bytes IS 'Drive FILE byte size mirrored from infra_file; null for RICH_DOC or not yet backfilled';

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

-- ---------------------------------------------------------------------------
-- DML : still-required seeds (final-state)
-- ---------------------------------------------------------------------------


CREATE TABLE doc_tenant_settings (
    tenant_id                       BIGINT          NOT NULL,
    member_share_enabled            BOOLEAN         NOT NULL DEFAULT TRUE,
    markdown_export_enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    drive_upload_enabled            BOOLEAN         NOT NULL DEFAULT TRUE,
    storage_quota_default_bytes     BIGINT,
    creator                         BIGINT,
    create_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater                         BIGINT,
    update_time                     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted                         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_tenant_settings PRIMARY KEY (tenant_id)
);

COMMENT ON TABLE doc_tenant_settings IS 'Tenant-level docs product settings (org policy)';
COMMENT ON COLUMN doc_tenant_settings.storage_quota_default_bytes IS
    'Default FILE storage quota in bytes for all members; NULL = unlimited';

CREATE TABLE doc_storage_quota_user_rule (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    quota_bytes     BIGINT,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_doc_storage_quota_user_rule PRIMARY KEY (id),
    CONSTRAINT uk_doc_storage_quota_user_rule UNIQUE (tenant_id, user_id)
);

COMMENT ON TABLE doc_storage_quota_user_rule IS
    'Per-user docs FILE storage quota override; quota_bytes NULL = unlimited for that user';

CREATE INDEX idx_doc_storage_quota_user_rule_tenant
    ON doc_storage_quota_user_rule (tenant_id);

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


-- ---------------------------------------------------------------------------
-- rtc_ / mtg_ : media session + meeting
-- ---------------------------------------------------------------------------

CREATE TABLE rtc_session (
    id              BIGINT       NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    purpose         VARCHAR(32)  NOT NULL,
    owner_type      VARCHAR(32)  NOT NULL,
    owner_id        BIGINT       NOT NULL,
    room_name       VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    participant_count INT        NOT NULL DEFAULT 0,
    creator         BIGINT,
    create_time     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_rtc_session PRIMARY KEY (id),
    CONSTRAINT uk_rtc_session_room UNIQUE (room_name),
    CONSTRAINT ck_rtc_session_status CHECK (status IN ('ACTIVE', 'ENDED'))
);

CREATE INDEX idx_rtc_session_owner
    ON rtc_session (tenant_id, owner_type, owner_id)
    WHERE deleted = 0;

CREATE INDEX idx_rtc_session_purpose
    ON rtc_session (tenant_id, purpose, status)
    WHERE deleted = 0;

CREATE UNIQUE INDEX uk_rtc_session_active_owner
    ON rtc_session (tenant_id, owner_type, owner_id, purpose)
    WHERE deleted = 0 AND status = 'ACTIVE';

CREATE TABLE mtg_meeting (
    id                  BIGINT       NOT NULL,
    tenant_id           BIGINT       NOT NULL,
    meeting_code        VARCHAR(16)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    host_user_id        BIGINT       NOT NULL,
    status              VARCHAR(16)  NOT NULL,
    scheduled_start     TIMESTAMPTZ,
    scheduled_end       TIMESTAMPTZ,
    rtc_session_id      BIGINT,
    calendar_event_id   BIGINT,
    creator             BIGINT,
    create_time         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_mtg_meeting PRIMARY KEY (id),
    CONSTRAINT uk_mtg_meeting_code UNIQUE (tenant_id, meeting_code),
    CONSTRAINT ck_mtg_meeting_status CHECK (status IN ('SCHEDULED', 'LIVE', 'ENDED'))
);

CREATE INDEX idx_mtg_meeting_host
    ON mtg_meeting (tenant_id, host_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_mtg_meeting_status
    ON mtg_meeting (tenant_id, status)
    WHERE deleted = 0;

CREATE TABLE mtg_participant (
    id              BIGINT       NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    meeting_id      BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    role            VARCHAR(16)  NOT NULL,
    joined_at       TIMESTAMPTZ,
    left_at         TIMESTAMPTZ,
    creator         BIGINT,
    create_time     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_mtg_participant PRIMARY KEY (id),
    CONSTRAINT uk_mtg_participant_user UNIQUE (meeting_id, user_id),
    CONSTRAINT ck_mtg_participant_role CHECK (role IN ('HOST', 'ATTENDEE'))
);

CREATE INDEX idx_mtg_participant_user
    ON mtg_participant (tenant_id, user_id)
    WHERE deleted = 0;

-- ---------------------------------------------------------------------------
-- agent_ : LLM connection / def / session / automation
-- ---------------------------------------------------------------------------

CREATE TABLE agent_llm_connection (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    provider_type       VARCHAR(32)     NOT NULL DEFAULT 'openai_compatible',
    base_url            VARCHAR(512)    NOT NULL,
    api_key_enc         VARCHAR(2048),
    model_name          VARCHAR(128)    NOT NULL,
    enabled             SMALLINT        NOT NULL DEFAULT 1,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_agent_llm_connection PRIMARY KEY (id),
    CONSTRAINT ck_agent_llm_connection_provider CHECK (provider_type IN ('openai_compatible')),
    CONSTRAINT ck_agent_llm_connection_enabled CHECK (enabled IN (0, 1))
);

CREATE UNIQUE INDEX uk_agent_llm_connection_tenant_code
    ON agent_llm_connection (tenant_id, code)
    WHERE deleted = 0;

CREATE INDEX idx_agent_llm_connection_tenant
    ON agent_llm_connection (tenant_id)
    WHERE deleted = 0;

COMMENT ON TABLE agent_llm_connection IS 'Tenant LLM provider connection; code is Agent modelRef';
COMMENT ON COLUMN agent_llm_connection.code IS 'Unique modelRef within tenant';
COMMENT ON COLUMN agent_llm_connection.api_key_enc IS 'AES-GCM encrypted API key';

CREATE TABLE agent_def (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    system_prompt       TEXT            NOT NULL DEFAULT '',
    model_ref           VARCHAR(64)     NOT NULL,
    skill_ids_json      JSONB           NOT NULL DEFAULT '[]'::jsonb,
    capability_profile_json JSONB       NOT NULL DEFAULT '{}'::jsonb,
    carrier_bot_id      BIGINT,
    status              VARCHAR(16)     NOT NULL DEFAULT 'active',
    owner_user_id       BIGINT          NOT NULL,
    visibility          VARCHAR(16)     NOT NULL DEFAULT 'private',
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_agent_def PRIMARY KEY (id),
    CONSTRAINT ck_agent_def_status CHECK (status IN ('active', 'disabled')),
    CONSTRAINT ck_agent_def_visibility CHECK (visibility IN ('private'))
);

CREATE INDEX idx_agent_def_tenant_owner
    ON agent_def (tenant_id, owner_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_agent_def_tenant_status
    ON agent_def (tenant_id, status)
    WHERE deleted = 0;

COMMENT ON TABLE agent_def IS 'Configurable Agent definition; model_ref -> agent_llm_connection.code';
COMMENT ON COLUMN agent_def.model_ref IS 'References agent_llm_connection.code within tenant';
COMMENT ON COLUMN agent_def.skill_ids_json IS 'JSON array of skill codes; empty until skill catalog';
COMMENT ON COLUMN agent_def.capability_profile_json IS
    'Capability profile JSON: {hotPacks:[], deferredDomains:[]}. Empty object = legacy skillIds-only hot path.';

CREATE TABLE agent_session (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    agent_id            BIGINT          NOT NULL,
    user_id             BIGINT          NOT NULL,
    title               VARCHAR(256),
    status              VARCHAR(16)     NOT NULL DEFAULT 'active',
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_agent_session PRIMARY KEY (id),
    CONSTRAINT ck_agent_session_status CHECK (status IN ('active', 'archived'))
);

CREATE INDEX idx_agent_session_user
    ON agent_session (tenant_id, user_id, update_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_agent_session_agent
    ON agent_session (tenant_id, agent_id)
    WHERE deleted = 0;

COMMENT ON TABLE agent_session IS 'Agent workspace chat session (not IM conversation)';

CREATE TABLE agent_session_message (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    session_id          BIGINT          NOT NULL,
    role                VARCHAR(16)     NOT NULL,
    content             TEXT            NOT NULL,
    parts_json          TEXT,
    seq                 INT             NOT NULL,
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_agent_session_message PRIMARY KEY (id),
    CONSTRAINT ck_agent_session_message_role CHECK (role IN ('user', 'assistant', 'system'))
);

CREATE UNIQUE INDEX uk_agent_session_message_seq
    ON agent_session_message (session_id, seq)
    WHERE deleted = 0;

CREATE INDEX idx_agent_session_message_session
    ON agent_session_message (tenant_id, session_id, seq)
    WHERE deleted = 0;

COMMENT ON TABLE agent_session_message IS 'Agent session turns; never written to im_message by this path';
COMMENT ON COLUMN agent_session_message.parts_json IS 'JSON array of message parts (text/reasoning/tool); null = legacy content-only';

CREATE TABLE agent_automation (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    owner_user_id       BIGINT          NOT NULL,
    agent_id            BIGINT          NOT NULL,
    title               VARCHAR(128)    NOT NULL,
    prompt              TEXT            NOT NULL,
    cron_expr           VARCHAR(64)     NOT NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'active',
    target_user_id      BIGINT          NOT NULL,
    next_run_at         TIMESTAMPTZ,
    last_run_at         TIMESTAMPTZ,
    last_status         VARCHAR(32),
    last_error          VARCHAR(512),
    creator             BIGINT,
    create_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater             BIGINT,
    update_time         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_agent_automation PRIMARY KEY (id),
    CONSTRAINT ck_agent_automation_status CHECK (status IN ('active', 'disabled'))
);

CREATE INDEX idx_agent_automation_owner
    ON agent_automation (tenant_id, owner_user_id)
    WHERE deleted = 0;

CREATE INDEX idx_agent_automation_due
    ON agent_automation (status, next_run_at)
    WHERE deleted = 0 AND status = 'active';

COMMENT ON TABLE agent_automation IS 'Cron automation: run Agent prompt and push via carrier bot to IM';
COMMENT ON COLUMN agent_automation.cron_expr IS 'Spring/Quartz 6-field cron';
COMMENT ON COLUMN agent_automation.target_user_id IS 'V1 bot_dm push target (usually owner)';


-- 默认租户（含 owner）
INSERT INTO sys_tenant (id, code, name, status, owner_user_id, create_time, update_time)
VALUES (1, 'RFDEMO7K9X2', '默认企业', 0, 1, NOW(), NOW());

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

INSERT INTO sys_role (id, tenant_id, parent_id, name, code, role_type, can_delegate, sort, status, create_time, update_time)
VALUES (100, 1, 0, '超级管理员', 'super_admin', 'SYSTEM', 1, 0, 0, NOW(), NOW());

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

-- Tenant-scoped display profile backfill from global sys_user
UPDATE sys_tenant_user tu
SET nickname = COALESCE(tu.nickname, NULLIF(TRIM(u.nickname), ''), u.username),
    avatar = COALESCE(tu.avatar, NULLIF(TRIM(u.avatar), '')),
    signature = CASE
        WHEN tu.signature IS NOT NULL AND tu.signature <> '' THEN tu.signature
        ELSE COALESCE(u.signature, '')
    END,
    cover_file_id = COALESCE(tu.cover_file_id, NULLIF(TRIM(u.cover_file_id), ''))
FROM sys_user u
WHERE tu.user_id = u.id
  AND tu.deleted = 0
  AND u.deleted = 0;


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


-- docs-bot seed
INSERT INTO im_bot (id, code, name, description, scope, enable_policy, handler_kind, type, status, create_time, update_time)
VALUES
    (900007, 'docs-bot', '文档助手', '文档分享与协作提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW());

INSERT INTO im_bot_tenant_enablement (id, tenant_id, bot_id, enabled, create_time, update_time)
VALUES
    (900107, 1, 900007, 1, NOW(), NOW());

INSERT INTO im_bot_user_enablement (id, tenant_id, user_id, bot_id, create_time, update_time)
VALUES
    (900207, 1, 1, 900007, NOW(), NOW());

-- geo permissions (template tenant 1)
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (2300, 1, 2000, '行政区划', 'infra:geo', 1, 3, 0, NOW(), NOW()),
    (2301, 1, 2300, '区划查询', 'infra:geo:query', 2, 1, 0, NOW(), NOW()),
    (2302, 1, 2300, '区划同步', 'infra:geo:sync', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (23001, 1, 100, 2300, NOW(), NOW()),
    (23002, 1, 100, 2301, NOW(), NOW()),
    (23003, 1, 100, 2302, NOW(), NOW());

-- enterprise profile permissions
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1600, 1, 1000, '企业信息', 'system:enterprise', 1, 6, 0, NOW(), NOW()),
    (1601, 1, 1600, '企业信息查询', 'system:enterprise:query', 2, 1, 0, NOW(), NOW()),
    (1602, 1, 1600, '企业信息更新', 'system:enterprise:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11600, 1, 100, 1600, NOW(), NOW()),
    (11601, 1, 100, 1601, NOW(), NOW()),
    (11602, 1, 100, 1602, NOW(), NOW());

-- IM product settings permissions
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


-- sticker catalog seed
INSERT INTO im_sticker_pack (id, code, name, scope, sort, status, create_time, update_time)
VALUES (910001, 'system_default', '系统表情', 'system', 0, 1, NOW(), NOW());

INSERT INTO im_sticker (id, pack_id, code, name, file_id, emoji, sort, create_time, update_time)
VALUES
    (910101, 910001, 'ok', 'OK', NULL, '👌', 1, NOW(), NOW()),
    (910102, 910001, 'clap', '鼓掌', NULL, '👏', 2, NOW(), NOW()),
    (910103, 910001, 'fire', '火', NULL, '🔥', 3, NOW(), NOW()),
    (910104, 910001, 'think', '思考', NULL, '🤔', 4, NOW(), NOW()),
    (910105, 910001, 'party', '庆祝', NULL, '🥳', 5, NOW(), NOW()),
    (910106, 910001, 'wave', '挥手', NULL, '👋', 6, NOW(), NOW());

-- Agent LLM permissions (parent under 基础设施 2000)
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (2400, 1, 2000, '模型设置', 'agent:llm', 1, 4, 0, NOW(), NOW()),
    (2401, 1, 2400, '模型查询', 'agent:llm:query', 2, 1, 0, NOW(), NOW()),
    (2402, 1, 2400, '模型更新', 'agent:llm:update', 2, 2, 0, NOW(), NOW()),
    (2403, 1, 2400, '模型测试连接', 'agent:llm:test', 2, 3, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (12400, 1, 100, 2400, NOW(), NOW()),
    (12401, 1, 100, 2401, NOW(), NOW()),
    (12402, 1, 100, 2402, NOW(), NOW()),
    (12403, 1, 100, 2403, NOW(), NOW());

-- Calendar product settings permissions under 产品设置 parent 1700
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

-- Docs product settings / files / storage-quota permissions under 产品设置 parent 1700
INSERT INTO sys_permission (id, tenant_id, parent_id, name, code, type, sort, status, create_time, update_time)
VALUES
    (1720, 1, 1700, '云文档产品设置', 'docs:product-settings', 1, 3, 0, NOW(), NOW()),
    (1721, 1, 1720, '云文档产品设置查询', 'docs:product-settings:query', 2, 1, 0, NOW(), NOW()),
    (1722, 1, 1720, '云文档产品设置更新', 'docs:product-settings:update', 2, 2, 0, NOW(), NOW()),
    (1730, 1, 1700, '云文档文件管理', 'docs:files', 1, 4, 0, NOW(), NOW()),
    (1731, 1, 1730, '云文档文件查询', 'docs:files:query', 2, 1, 0, NOW(), NOW()),
    (1732, 1, 1730, '云文档文件转移所有者', 'docs:files:transfer', 2, 2, 0, NOW(), NOW()),
    (1733, 1, 1730, '云文档文件删除', 'docs:files:delete', 2, 3, 0, NOW(), NOW()),
    (1734, 1, 1730, '云文档文件恢复', 'docs:files:restore', 2, 4, 0, NOW(), NOW()),
    (1735, 1, 1730, '云文档文件销毁', 'docs:files:purge', 2, 5, 0, NOW(), NOW()),
    (1740, 1, 1700, '云文档存储配额', 'docs:storage-quota', 1, 5, 0, NOW(), NOW()),
    (1741, 1, 1740, '云文档存储配额查询', 'docs:storage-quota:query', 2, 1, 0, NOW(), NOW()),
    (1742, 1, 1740, '云文档存储配额更新', 'docs:storage-quota:update', 2, 2, 0, NOW(), NOW());

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
VALUES
    (11720, 1, 100, 1720, NOW(), NOW()),
    (11721, 1, 100, 1721, NOW(), NOW()),
    (11722, 1, 100, 1722, NOW(), NOW()),
    (11730, 1, 100, 1730, NOW(), NOW()),
    (11731, 1, 100, 1731, NOW(), NOW()),
    (11732, 1, 100, 1732, NOW(), NOW()),
    (11733, 1, 100, 1733, NOW(), NOW()),
    (11734, 1, 100, 1734, NOW(), NOW()),
    (11735, 1, 100, 1735, NOW(), NOW()),
    (11740, 1, 100, 1740, NOW(), NOW()),
    (11741, 1, 100, 1741, NOW(), NOW()),
    (11742, 1, 100, 1742, NOW(), NOW());
