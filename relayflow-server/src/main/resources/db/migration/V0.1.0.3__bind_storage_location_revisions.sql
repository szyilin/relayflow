CREATE TABLE infra_storage_config_revision (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL CHECK (source IN ('bootstrap', 'tenant')),
    location_key VARCHAR(64) NOT NULL,
    config_json TEXT NOT NULL,
    creator BIGINT,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updater BIGINT,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, location_key)
);
COMMENT ON COLUMN infra_storage_config_revision.config_json IS 'Immutable location metadata and AES-GCM encrypted secret; credentials rotate explicitly';
ALTER TABLE infra_storage_provider ADD COLUMN storage_config_revision_id BIGINT,
    ADD COLUMN active_storage_config_revision_id BIGINT;
ALTER TABLE infra_file ADD COLUMN storage_config_revision_id BIGINT;
ALTER TABLE infra_file_upload_session ADD COLUMN storage_config_revision_id BIGINT;
ALTER TABLE infra_storage_provider ADD CONSTRAINT fk_provider_draft_location
    FOREIGN KEY (tenant_id, storage_config_revision_id) REFERENCES infra_storage_config_revision(tenant_id, id),
    ADD CONSTRAINT fk_provider_active_location FOREIGN KEY (tenant_id, active_storage_config_revision_id)
    REFERENCES infra_storage_config_revision(tenant_id, id);
ALTER TABLE infra_file ADD CONSTRAINT fk_file_location FOREIGN KEY (tenant_id, storage_config_revision_id)
    REFERENCES infra_storage_config_revision(tenant_id, id);
ALTER TABLE infra_file_upload_session ADD CONSTRAINT fk_upload_location FOREIGN KEY (tenant_id, storage_config_revision_id)
    REFERENCES infra_storage_config_revision(tenant_id, id);
CREATE INDEX idx_file_location ON infra_file(tenant_id, storage_config_revision_id);
CREATE INDEX idx_upload_location ON infra_file_upload_session(tenant_id, storage_config_revision_id, status, expires_at);
CREATE INDEX idx_provider_draft_location ON infra_storage_provider(tenant_id, storage_config_revision_id);
CREATE INDEX idx_provider_active_location ON infra_storage_provider(tenant_id, active_storage_config_revision_id);
