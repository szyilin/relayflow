-- Denormalized FILE size for admin list + quota metering (no cross-domain JOIN).
ALTER TABLE doc_object
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT NULL;

COMMENT ON COLUMN doc_object.size_bytes IS 'Drive FILE byte size mirrored from infra_file; null for RICH_DOC or not yet backfilled';
