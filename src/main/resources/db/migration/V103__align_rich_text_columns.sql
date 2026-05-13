ALTER TABLE support_ticket
    ALTER COLUMN description TYPE TEXT;

ALTER TABLE ticket_message
    ALTER COLUMN message TYPE TEXT;

ALTER TABLE support_quick_reply
    ALTER COLUMN body TYPE TEXT;

ALTER TABLE crm_lead_activity
    ALTER COLUMN notes TYPE TEXT;

ALTER TABLE task_item
    ALTER COLUMN description TYPE TEXT;

ALTER TABLE task_comment
    ALTER COLUMN comment TYPE TEXT;
