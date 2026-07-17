-- Calendar domain: calendars, events, attendees + calendar-bot seed.

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

-- Platform calendar assistant (system bot: deliver without enablement).
INSERT INTO im_bot (id, code, name, description, scope, enable_policy, handler_kind, type, status, create_time, update_time)
VALUES
    (900006, 'calendar-bot', '日历助手', '日程邀约、变更与提醒', 'tenant', 'default_on', 'noop', 'system', 1, NOW(), NOW());

INSERT INTO im_bot_tenant_enablement (id, tenant_id, bot_id, enabled, create_time, update_time)
VALUES
    (900106, 1, 900006, 1, NOW(), NOW());

INSERT INTO im_bot_user_enablement (id, tenant_id, user_id, bot_id, create_time, update_time)
VALUES
    (900206, 1, 1, 900006, NOW(), NOW());
