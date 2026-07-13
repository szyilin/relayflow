-- Notify inbox V2: optional dedupe key for idempotent unread refresh (e.g. task:123).

ALTER TABLE infra_notify
    ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_infra_notify_dedupe
    ON infra_notify (tenant_id, user_id, type, dedupe_key, read_flag)
    WHERE deleted = 0 AND dedupe_key IS NOT NULL;
