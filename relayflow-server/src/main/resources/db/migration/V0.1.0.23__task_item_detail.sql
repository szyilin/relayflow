-- Task detail P0: start/description/remind/parent for workspace task detail panel.

ALTER TABLE task_item
    ADD COLUMN IF NOT EXISTS start_time TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS remind_before_minutes INT,
    ADD COLUMN IF NOT EXISTS parent_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_task_item_parent
    ON task_item (tenant_id, parent_id)
    WHERE deleted = 0 AND parent_id IS NOT NULL;

COMMENT ON COLUMN task_item.start_time IS 'Optional start time';
COMMENT ON COLUMN task_item.description IS 'Task description (plain text)';
COMMENT ON COLUMN task_item.remind_before_minutes IS 'Remind N minutes before due_time; NULL = system window';
COMMENT ON COLUMN task_item.parent_id IS 'Parent task id for one-level subtasks; NULL = root';
