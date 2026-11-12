-- Encaissement au guichet : monnaie rendue, paiements hors plateforme (cheque, virement,
-- mobile money hors plateforme), annulation et relance d'une intention de paiement.

ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS tendered_amount NUMERIC(19, 4);
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS change_amount NUMERIC(19, 4);
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS cash_session_number VARCHAR(100);
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS bank_name VARCHAR(180);
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS payer_name VARCHAR(180);
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS payer_phone VARCHAR(40);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_cash_session
    ON payment_transaction (cash_session_number)
    WHERE cash_session_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_transaction_method_reference
    ON payment_transaction (payment_method, provider_reference);

ALTER TABLE payment_intent ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE payment_intent ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(180);
ALTER TABLE payment_intent ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);
ALTER TABLE payment_intent ADD COLUMN IF NOT EXISTS relaunch_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payment_intent ADD COLUMN IF NOT EXISTS last_relaunched_at TIMESTAMPTZ;
ALTER TABLE payment_intent ADD COLUMN IF NOT EXISTS last_relaunched_by VARCHAR(180);
