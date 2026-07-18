-- Board: three-state status + column rank.

ALTER TABLE task_item DROP CONSTRAINT IF EXISTS ck_task_item_status;

ALTER TABLE task_item
    ADD CONSTRAINT ck_task_item_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));

ALTER TABLE task_item
    ADD COLUMN board_rank INT;

CREATE INDEX idx_task_item_list_status_rank
    ON task_item (tenant_id, list_id, status, board_rank)
    WHERE deleted = 0 AND list_id IS NOT NULL AND parent_id IS NULL;
