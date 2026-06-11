-- #3 Inbound email deduplication: track the Microsoft Graph Message-ID
-- to prevent double-processing if the worker restarts mid-run.
ALTER TABLE ticket_message ADD COLUMN IF NOT EXISTS external_message_id VARCHAR(512);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ticket_msg_ext_id
    ON ticket_message(external_message_id)
    WHERE external_message_id IS NOT NULL;