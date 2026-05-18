-- Bloc 1 : unité de mesure sur les lignes
ALTER TABLE billing_document_line
    ADD COLUMN IF NOT EXISTS unit VARCHAR(30);

-- Bloc 3 : référence de paiement, instructions et coordonnées bancaires sur le document
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS payment_reference    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payment_instructions TEXT,
    ADD COLUMN IF NOT EXISTS bank_details_json    JSONB;