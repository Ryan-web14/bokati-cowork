-- document: V4 added type_code NOT NULL as a legacy identifier but the
-- Document entity maps document_type_id (FK) instead and never sets type_code.
ALTER TABLE document ALTER COLUMN type_code DROP NOT NULL;