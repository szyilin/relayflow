-- Collapse task status to binary: TODO (incomplete) | DONE (complete).
-- Migrate any IN_PROGRESS rows/config references, then tighten CHECK.

UPDATE task_item
SET status = 'TODO'
WHERE status = 'IN_PROGRESS';

-- Strip IN_PROGRESS from saved view-config JSON filter values (arrays).
-- config_json is JSONB: cast to text for replace/LIKE, then cast back.
UPDATE task_view_config
SET config_json = replace(config_json::text, ',"IN_PROGRESS"', '')::jsonb
WHERE config_json::text LIKE '%IN_PROGRESS%';

UPDATE task_view_config
SET config_json = replace(config_json::text, '"IN_PROGRESS",', '')::jsonb
WHERE config_json::text LIKE '%IN_PROGRESS%';

UPDATE task_view_config
SET config_json = replace(config_json::text, '"IN_PROGRESS"', '"TODO"')::jsonb
WHERE config_json::text LIKE '%IN_PROGRESS%';

ALTER TABLE task_item DROP CONSTRAINT IF EXISTS ck_task_item_status;

ALTER TABLE task_item
    ADD CONSTRAINT ck_task_item_status
    CHECK (status IN ('TODO', 'DONE'));
