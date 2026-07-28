-- Agent definition + workspace session (isolated from im_message)

CREATE TABLE agent_def (
    id                  BIGINT          NOT NULL,
    tenant_id           BIGINT          NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(512),
    system_prompt       TEXT            NOT NULL DEFAULT '',
    model_ref           VARCHAR(64)     NOT NULL,
    skill_ids_json      JSONB           NOT NULL DEFAULT '[]'::jsonb,
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
