-- Contact enrich: external contacts, friend requests, starred contacts.

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
