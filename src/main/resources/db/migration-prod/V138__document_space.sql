-- Document space: logical grouping for GED separation (KYC, CONTRACT, FINANCIAL, ASSET, ADMINISTRATIVE, GENERIC)
ALTER TABLE document
    ADD COLUMN space               VARCHAR(50) NOT NULL DEFAULT 'GENERIC',
    ADD COLUMN space_reference_code VARCHAR(100);

CREATE INDEX idx_document_space ON document (space);
