ALTER TABLE inventory_purchase_order
    ADD COLUMN IF NOT EXISTS approval_level VARCHAR(40) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS total_amount BIGINT;

ALTER TABLE goods_receipt
    ADD COLUMN IF NOT EXISTS invoice_document_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS delivery_note_document_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS proof_document_code VARCHAR(120);

ALTER TABLE goods_receipt_line
    ADD COLUMN IF NOT EXISTS rejected_quantity NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS quality_accepted BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS backorder_quantity NUMERIC(19, 4);
