ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS receipt_issued_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transaction_receipt_number
    ON payment_transaction(receipt_number)
    WHERE receipt_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_transaction_receipt_number
    ON payment_transaction(receipt_number);

CREATE INDEX IF NOT EXISTS idx_billing_document_payable_lookup
    ON billing_document(document_type, status, customer_type, customer_code, balance_due);

CREATE INDEX IF NOT EXISTS idx_billing_document_line_source_lookup
    ON billing_document_line(source_type, source_code);
