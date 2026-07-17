-- Calendar V1.1: whole-calendar share + RRULE / exceptions.

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

ALTER TABLE cal_event
    ADD COLUMN rrule VARCHAR(512);

COMMENT ON COLUMN cal_event.rrule IS 'RFC5545 RRULE text; null = single instance';

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
