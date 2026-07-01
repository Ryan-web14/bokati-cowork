-- signature_path was added nullable in V7 but later constrained to NOT NULL.
-- System auto-signatures (e.g. contract signing on payment) have no file path.
ALTER TABLE document_signature
    ALTER COLUMN signature_path DROP NOT NULL;
