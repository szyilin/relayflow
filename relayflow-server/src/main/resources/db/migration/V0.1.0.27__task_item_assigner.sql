-- Task item assigner for「我分配的」quick view.

ALTER TABLE task_item
    ADD COLUMN assigner_id BIGINT;

CREATE INDEX idx_task_item_assigner
    ON task_item (tenant_id, assigner_id)
    WHERE deleted = 0 AND assigner_id IS NOT NULL;

COMMENT ON COLUMN task_item.assigner_id IS 'User who last assigned the task to someone else; null if never or self-assigned';
