-- Agent automation (cron → AgentRuntime → carrier ImBotApi push)

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
