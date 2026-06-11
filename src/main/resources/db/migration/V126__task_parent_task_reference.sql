ALTER TABLE task_item
    ADD COLUMN IF NOT EXISTS parent_task_id BIGINT REFERENCES task_item(id);

CREATE INDEX IF NOT EXISTS idx_task_item_parent_task ON task_item(parent_task_id);
