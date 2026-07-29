-- Per-user calendar display color override (does not mutate cal_calendar.color).

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
