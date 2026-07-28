-- RTC media session metadata (LiveKit room mapping). Media itself is not stored here.

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
