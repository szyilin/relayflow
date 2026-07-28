-- Agent LLM connections (OpenAI-compatible) + permission seeds.

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

-- Permissions (parent under 基础设施 2000; 2100=storage, 2200=file, 2300=geo → use 2400)
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
