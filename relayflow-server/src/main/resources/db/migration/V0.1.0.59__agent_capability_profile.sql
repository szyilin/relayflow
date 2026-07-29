-- AgentDef capability profile: hot packs + deferred domains (JSON), compatible with skill_ids_json

ALTER TABLE agent_def
    ADD COLUMN IF NOT EXISTS capability_profile_json JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMENT ON COLUMN agent_def.capability_profile_json IS
    'Capability profile JSON: {hotPacks:[], deferredDomains:[]}. Empty object = legacy skillIds-only hot path.';
