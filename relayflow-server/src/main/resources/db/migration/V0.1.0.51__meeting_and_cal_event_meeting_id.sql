-- Meeting product tables + calendar weak link (opaque meeting_id).

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

ALTER TABLE cal_event
    ADD COLUMN meeting_id BIGINT NULL;

CREATE INDEX idx_cal_event_meeting
    ON cal_event (tenant_id, meeting_id)
    WHERE deleted = 0 AND meeting_id IS NOT NULL;
